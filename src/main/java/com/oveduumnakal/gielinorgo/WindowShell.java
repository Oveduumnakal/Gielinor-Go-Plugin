/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The state, input handling and painting of the painted window's shell, with no RuneLite types so it paints into a
 * {@code BufferedImage} in a test. It owns whether the window is open, the active tab, the hover, the key focus and
 * the {@link Animations} table, and lays out the six tab headers, the Close button, a placeholder page per tab and the
 * footer (Reset window, the style button, the size button and the key hints). The pages themselves come later and plug
 * in through {@link PageActions}. The game thread owns it: painting reads one immutable {@link WindowSnapshot}, and
 * the mouse and key events arrive here already carried over from the UI thread. Hit rectangles are the final ones;
 * only a sliding row refuses clicks, and no page has one yet.
 *
 * <p>Closing keeps a text box's text as a draft and runs nothing half-finished: a modal that is open when the window
 * closes is shut without calling back.
 */
final class WindowShell
{
	/** The id of the Close button's hit. */
	static final String ID_CLOSE = "btn:close";

	/** The id of the Reset window button's hit. */
	static final String ID_RESET = "btn:reset";

	/** The id of the style button's hit. */
	static final String ID_THEME = "btn:theme";

	/** The id of the window size button's hit. */
	static final String ID_SIZE = "btn:size";

	/** The id of the Change log button's hit. */
	static final String ID_CHANGELOG = "btn:changelog";

	/** The id of the Feedback button's hit. */
	static final String ID_FEEDBACK = "btn:feedback";

	/** The first focus order a page's hits may use; the tabs and the Close button come before it. */
	static final int PAGE_ORDER = 10;

	/** The focus order of the first footer button; a page's hits come before it. */
	static final int FOOTER_ORDER = 100;

	/** The hint line while the window has focus and nothing is open over it. */
	static final String HINT_WINDOW = "Left/Right Tabs   Up/Down Select   Enter Press   Esc Back";

	/** The hint line while a menu is open. */
	static final String HINT_MENU = "Up/Down Move   Enter Choose   Esc Close";

	/** The hint line while a dialog is open. */
	static final String HINT_MODAL = "Left/Right Pick   Enter Confirm   Esc Cancel";

	/** The hint line while a text box has the keys. */
	static final String HINT_FIELD = "Type   Enter Save   Esc Cancel";

	/** The hint line while the window has no focus. */
	static final String HINT_UNFOCUSED = "Click the window to use keys";

	private static final int CLOSE_ORDER = 6;
	private static final String TAB_BAR = "tabBar";
	private static final String PREVIEW_NOTE = "This window is a preview: the pages come soon.";

	private final Host host;
	private final WindowFocus focus = new WindowFocus();
	private final Animations animations = new Animations();
	private final ContextMenu menu = new ContextMenu();
	private final Tooltip tooltip = new Tooltip();
	private final StillLayers layers = new StillLayers();
	private volatile WindowSnapshot snapshot = WindowSnapshot.EMPTY;
	private PageActions pages = PageActions.NONE;
	private boolean open;
	private boolean visible;
	private ShellMotion.Phase phase = ShellMotion.Phase.OPEN;
	private long phaseMs;
	private WindowTab tab = WindowTab.PARTY;
	private long tabMs;
	private List<Hit> hits = Collections.emptyList();
	private WindowLayout layout;
	private WindowStyle style;
	private String pressedId;
	private String shakeId = "";
	private long shakeMs;
	private Modal modal;
	private ChangelogPage changelog;
	private TextBox field;
	private String draft = "";
	private int seenDoneTasks;

	/** What the shell asks of the plugin: the settings it reads and the things its footer buttons do. */
	interface Host
	{
		/** @return the style to paint in */
		WindowTheme theme();

		/** @return how much the window moves */
		MotionMode motion();

		/** @return whether the footer shows key hints */
		boolean keyHints();

		/** @return whether the window's keyboard is on */
		boolean keyboard();

		/** @return the words on the size button, such as {@code Size: Medium} */
		String sizeLabel();

		/** Puts the window back to its default size and place. */
		void resetWindow();

		/** Switches to the other style and saves the choice. */
		void flipTheme();

		/** Moves on to the next window size preset. */
		void nextSize();

		/**
		 * Shows a pop-up notice.
		 *
		 * @param message what to say
		 */
		void notice(NoticeMessage message);

		/** @return whether the Change log button reads What's New and stands out, until the player opens it */
		default boolean whatsNew()
		{
			return false;
		}

		/** Records that the player opened the change log, so the What's New mark goes away. */
		default void whatsNewSeen()
		{
			return;
		}

		/**
		 * Opens an address in the player's browser.
		 *
		 * @param url the address
		 */
		default void openLink(String url)
		{
			return;
		}
	}

	/** What the pages do for the shell, set by the page code that arrives with the Party and Storage pages. */
	interface PageActions
	{
		/** Pages that do nothing, for a window that only has its shell. */
		PageActions NONE = new PageActions()
		{
			@Override
			public List<Hit> hits(WindowTab tab, Rectangle body)
			{
				return Collections.emptyList();
			}

			@Override
			public void press(Hit hit)
			{
				return;
			}

			@Override
			public void menu(Hit hit, int x, int y, boolean keyboard)
			{
				return;
			}

			@Override
			public void wheel(int x, int y, int rotation)
			{
				return;
			}

			@Override
			public int depth()
			{
				return 0;
			}

			@Override
			public void back()
			{
				return;
			}
		};

		/**
		 * Declares what the page on a tab has to hit, in paint order.
		 *
		 * @param tab the tab shown
		 * @param body the page area in window pixels
		 * @return the page's hits, with focus orders from {@link #PAGE_ORDER} up to {@link #FOOTER_ORDER}
		 */
		List<Hit> hits(WindowTab tab, Rectangle body);

		/**
		 * A hit of the page was pressed: a left click, or Enter on a button.
		 *
		 * @param hit the hit
		 */
		void press(Hit hit);

		/**
		 * A row's menu was asked for.
		 *
		 * @param hit the row
		 * @param x where to open it, in window pixels
		 * @param y where to open it, in window pixels
		 * @param keyboard whether a key asked, so the first item is highlighted
		 */
		void menu(Hit hit, int x, int y, boolean keyboard);

		/**
		 * The wheel turned over the window.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 * @param rotation the wheel's clicks
		 */
		void wheel(int x, int y, int rotation);

		/**
		 * A button went down on the page with no menu or dialog open, so a scroll list can take the press.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 * @return whether the press landed on a list's scroll bar, so it is not a click on what lies under it
		 */
		default boolean pointerPressed(int x, int y)
		{
			return false;
		}

		/**
		 * The pointer moved with a button down that went down on the page.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 */
		default void pointerDragged(int x, int y)
		{
			return;
		}

		/**
		 * The button that went down on the page came up.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 * @return whether the press became a scroll, so the release is not a click
		 */
		default boolean pointerReleased(int x, int y)
		{
			return false;
		}

		/** @return how many pages deep the window is, 0 on a tab's own page */
		int depth();

		/** Goes back one page. */
		void back();

		/**
		 * Called at the start of every paint, before the page declares its hits, with what the page lays itself out
		 * by.
		 *
		 * @param style the style, which has the window scale
		 * @param mode the motion mode
		 * @param nowMs the time in milliseconds
		 */
		default void frame(WindowStyle style, MotionMode mode, long nowMs)
		{
			return;
		}

		/**
		 * Called when the window opens.
		 *
		 * @param nowMs the time in milliseconds
		 */
		default void opened(long nowMs)
		{
			return;
		}

		/** Called when the window closes, so the pages forget what they were showing. */
		default void closed()
		{
			return;
		}

		/**
		 * Paints the page of a tab. When the page paints nothing, the shell paints its placeholder.
		 *
		 * @param g the graphics, with (0, 0) at the window's corner
		 * @param tab the tab shown
		 * @param ctx what the frame needs
		 * @return whether the page painted itself
		 */
		default boolean paint(Graphics2D g, WindowTab tab, PageContext ctx)
		{
			return false;
		}

		/**
		 * Offers a key to the page before the shell uses it, when no menu or dialog is open.
		 *
		 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
		 * @param modifiers the modifiers
		 * @param nowMs the time in milliseconds
		 * @return whether the page used the key
		 */
		default boolean key(int keyCode, int modifiers, long nowMs)
		{
			return false;
		}
	}

	/**
	 * Creates the shell, closed.
	 *
	 * @param host the settings and footer actions
	 */
	WindowShell(Host host)
	{
		this.host = host;
	}

	/**
	 * Sets the page actions.
	 *
	 * @param actions what the pages do
	 */
	void pages(PageActions actions)
	{
		pages = actions == null ? PageActions.NONE : actions;
	}

	/**
	 * Hands over a new snapshot to paint from. The game thread builds one when the panel's state changes and calls
	 * this. Tasks that were already done when the save was read, or that a daily reset took back, are not news, so
	 * they never count as completed since the player last looked.
	 *
	 * @param next the snapshot
	 */
	void publish(WindowSnapshot next)
	{
		seenDoneTasks = snapshot.loaded() ? Math.min(seenDoneTasks, next.doneTasks()) : next.doneTasks();
		snapshot = next;
	}

	/** @return the snapshot the next paint reads */
	WindowSnapshot snapshot()
	{
		return snapshot;
	}

	/** @return whether the window is open and takes input; a window fading out is not */
	boolean isOpen()
	{
		return open;
	}

	/** @return whether the window is still painted, which includes the fade out */
	boolean isVisible()
	{
		return visible;
	}

	/** @return the tab shown */
	WindowTab tab()
	{
		return tab;
	}

	/** @return the change log page while it is open, else null */
	ChangelogPage changelogPage()
	{
		return changelog;
	}

	/** @return the key focus and the shared highlight */
	WindowFocus focus()
	{
		return focus;
	}

	/** @return the table of running animations */
	Animations animations()
	{
		return animations;
	}

	/** @return the hits as laid out by the last paint, in paint order, at their final places */
	List<Hit> hits()
	{
		return hits;
	}

	/** @return the text typed into a text box when the window last closed with one open, empty for none */
	String draft()
	{
		return draft;
	}

	/** @return the draft, and forgets it so the next open starts clean */
	String takeDraft()
	{
		String kept = draft;
		draft = "";
		return kept;
	}

	/**
	 * Opens the window with key focus, on the tab it was last on. The plugin checks {@link WindowRules#mayOpen}
	 * first.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void open(long nowMs)
	{
		if (open)
			return;

		open = true;
		visible = true;
		phase = ShellMotion.Phase.OPEN;
		phaseMs = nowMs;
		tabMs = nowMs;
		focus.focus(true);
		if (tab.gated() && !snapshot.storageEnabled())
			tab = WindowTab.PARTY;

		focus.highlightKey(tab.hitId());
		seenDoneTasks = snapshot.doneTasks();
		pages.opened(nowMs);
	}

	/**
	 * Shows a tab, as a click on its header does. A greyed tab cannot be shown.
	 *
	 * @param next the tab
	 * @param nowMs the time in milliseconds
	 */
	void showTab(WindowTab next, long nowMs)
	{
		if (!open || next.gated() && !snapshot.storageEnabled())
			return;

		selectTab(next, nowMs);
		focus.highlightKey(tab.hitId());
	}

	/**
	 * Closes the window. A menu is closed and a dialog is shut without calling back; the text in its box is kept as
	 * the draft.
	 *
	 * @param nowMs the time in milliseconds
	 * @param how how it closes: by the player, or by combat or a battle, which is a quick fade
	 */
	void close(long nowMs, ShellMotion.Phase how)
	{
		if (!open)
			return;

		open = false;
		phase = how;
		phaseMs = nowMs;
		menu.close();
		if (modalOpen())
		{
			draft = field == null ? "" : field.text();
			modal.close();
		}

		modal = null;
		field = null;
		changelog = null;
		tooltip.clear();
		pressedId = null;
		focus.focus(false);
		focus.hover(null);
		animations.clear();
		pages.closed();
	}

	/**
	 * Opens the right-click menu of a row.
	 *
	 * @param items the items, Summary first
	 * @param x where to open it, in window pixels
	 * @param y where to open it, in window pixels
	 * @param keyboard whether a key asked, so the first enabled item is highlighted
	 * @param nowMs the time in milliseconds
	 */
	void openMenu(List<ContextMenu.Item> items, int x, int y, boolean keyboard, long nowMs)
	{
		if (!open || style == null)
			return;

		FontMetrics fm = metrics(ContextMenu.font(style));
		menu.open(items, x, y, layout.window, style, fm, keyboard, nowMs);
	}

	/**
	 * Opens a dialog over the window, such as the Rename box or the Release confirmation.
	 *
	 * @param dialog the dialog
	 * @param box its text box, or null when it has none; its text is kept as the draft if the window closes
	 * @param nowMs the time in milliseconds
	 */
	void openModal(Modal dialog, TextBox box, long nowMs)
	{
		if (!open)
			return;

		menu.close();
		modal = dialog;
		field = box;
		dialog.open(nowMs);
	}

	/** @return which part of the window has the keys, ignoring whether the window has focus */
	WindowKeyRules.KeyMode keyMode()
	{
		if (!open)
			return WindowKeyRules.KeyMode.NONE;

		if (modalOpen())
		{
			if (field == null)
				return WindowKeyRules.KeyMode.MODAL;

			return field.focused() ? WindowKeyRules.KeyMode.FIELD : WindowKeyRules.KeyMode.NONE;
		}

		return menu.isOpen() ? WindowKeyRules.KeyMode.MENU : WindowKeyRules.KeyMode.WINDOW;
	}

	/**
	 * Builds the view the UI thread reads.
	 *
	 * @param bounds where the window is on the canvas
	 * @return the view
	 */
	WindowView view(Rectangle bounds)
	{
		return new WindowView(open, bounds, hits, keyMode(), focus.focused());
	}

	/**
	 * @return whether something waits for the player, which the toggle shows as a pulsing dot; nothing is waiting
	 *         while the window is open
	 */
	boolean waiting()
	{
		return !open && snapshot.waiting(seenDoneTasks);
	}

	/**
	 * Handles a key going down. The key listener has already decided the window takes it.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link KeyEvent#getModifiersEx}
	 * @param nowMs the time in milliseconds
	 */
	void onKey(int keyCode, int modifiers, long nowMs)
	{
		if (!open)
			return;

		if (modalOpen())
		{
			modal.onKeyPressed(keyCode, modifiers, nowMs);
			return;
		}

		if (menu.isOpen())
		{
			menu.onKeyPressed(keyCode);
			return;
		}

		if (pages.key(keyCode, modifiers, nowMs))
			return;

		switch (keyCode)
		{
			case KeyEvent.VK_LEFT:
				stepTab(-1, nowMs);
				break;
			case KeyEvent.VK_RIGHT:
				stepTab(1, nowMs);
				break;
			case KeyEvent.VK_UP:
				focus.move(-1);
				break;
			case KeyEvent.VK_DOWN:
				focus.move(1);
				break;
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_SPACE:
				activate(nowMs);
				break;
			case KeyEvent.VK_ESCAPE:
				back(true, nowMs);
				break;
			case KeyEvent.VK_BACK_SPACE:
				back(false, nowMs);
				break;
			default:
				break;
		}
	}

	/**
	 * Handles a character typed into a text box.
	 *
	 * @param c the character
	 * @param modifiers the modifiers
	 * @param nowMs the time in milliseconds
	 */
	void onTyped(char c, int modifiers, long nowMs)
	{
		if (open && modalOpen())
			modal.onKeyTyped(c, modifiers, nowMs);
	}

	/**
	 * The pointer moved over the window.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @param nowMs the time in milliseconds
	 */
	void onMoved(int x, int y, long nowMs)
	{
		if (!open)
			return;

		tooltip.clear();
		if (modalOpen())
		{
			modal.onMouseMoved(x, y);
			return;
		}

		if (menu.onMouseMoved(x, y))
			return;

		Hit hit = hitAt(x, y);
		focus.hover(hit);
		if (hit != null)
			tooltip.hover(hit.id(), hit.tip(), x, y, nowMs);
	}

	/** The pointer left the window, so nothing it was over is hovered any more. */
	void onLeft()
	{
		tooltip.clear();
		if (!open)
			return;

		if (modalOpen())
			modal.onMouseMoved(-1, -1);

		if (!focus.keyboardLed())
			focus.hover(null);
	}

	/**
	 * A mouse button went down inside the window. A click inside gives the window key focus.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @param button the button, as {@link MouseEvent#getButton}
	 * @param modifiers the modifiers
	 * @param nowMs the time in milliseconds
	 */
	void onPressed(int x, int y, int button, int modifiers, long nowMs)
	{
		if (!open)
			return;

		focus.focus(true);
		tooltip.clear();
		if (modalOpen())
		{
			modal.onMousePressed(x, y, button, modifiers, nowMs);
			return;
		}

		if (menu.isOpen())
		{
			menu.onMousePressed(x, y, button);
			return;
		}

		if (changelog != null ? changelog.pressed(x, y) : pages.pointerPressed(x, y))
		{
			pressedId = null;
			return;
		}

		Hit hit = hitAt(x, y);
		pressedId = hit != null && hit.enabled() ? hit.id() : null;
	}

	/**
	 * A button that went down inside the window came up. A left click on a hit presses it; a right click on a row
	 * opens its menu; a click on a disabled tab shakes it and says why.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @param button the button
	 * @param nowMs the time in milliseconds
	 */
	void onReleased(int x, int y, int button, long nowMs)
	{
		String down = pressedId;
		pressedId = null;
		if (!open)
			return;

		if (modalOpen())
		{
			modal.onMouseReleased(x, y);
			return;
		}

		if (changelog != null ? changelog.released(x, y, host::openLink) : pages.pointerReleased(x, y))
			return;

		Hit hit = hitAt(x, y);
		if (hit == null)
			return;

		if (!hit.enabled())
		{
			lockedClick(hit, nowMs);
			return;
		}

		if (!hit.id().equals(down))
			return;

		if (button == MouseEvent.BUTTON1)
			perform(hit, nowMs);
		else if (button == MouseEvent.BUTTON3 && hit.kind() == Hit.Kind.ROW)
			pages.menu(hit, x, y, false);
	}

	/**
	 * The pointer moved with a button down that went down inside the window.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 */
	void onDragged(int x, int y)
	{
		if (!open)
			return;

		if (modalOpen())
			modal.onMouseDragged(x);
		else if (!menu.isOpen() && changelog != null)
			changelog.dragged(x, y);
		else if (!menu.isOpen())
			pages.pointerDragged(x, y);
	}

	/**
	 * The wheel turned over the window.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @param rotation the wheel's clicks
	 */
	void onWheel(int x, int y, int rotation)
	{
		if (open && !modalOpen() && !menu.isOpen() && changelog != null)
			changelog.wheel(x, y, rotation);
		else if (open && !modalOpen() && !menu.isOpen())
			pages.wheel(x, y, rotation);
	}

	/**
	 * Takes the keyboard away from the window, as a click in the world, chat typing or leaving the game does. A text
	 * box keeps its text.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void focusLost(long nowMs)
	{
		focus.focus(false);
		if (field != null)
			field.focus(false, nowMs);
	}

	/**
	 * Paints the window. Nothing is painted, and false is returned, when it is closed and has finished fading out.
	 *
	 * @param g the graphics, with (0, 0) at the window's top-left corner
	 * @param width the window width in pixels
	 * @param height the window height in pixels
	 * @param nowMs the time in milliseconds
	 * @return whether anything was painted
	 */
	boolean paint(Graphics2D g, int width, int height, long nowMs)
	{
		if (!visible)
			return false;

		MotionMode mode = host.motion();
		ShellMotion.Frame frame = ShellMotion.frame(mode, phase, nowMs - phaseMs);
		if (!open && frame.done)
		{
			visible = false;
			layers.clear();
			return false;
		}

		layout = WindowLayout.of(width, height);
		style = WindowStyle.of(host.theme(), layout.scale);
		WindowSnapshot shown = snapshot;
		if (tab.gated() && !shown.storageEnabled())
			selectTab(WindowTab.PARTY, nowMs);

		if (open)
			seenDoneTasks = shown.doneTasks();

		pages.frame(style, mode, nowMs);
		publishHits(shown);
		RenderingHints savedHints = g.getRenderingHints();
		Stroke savedStroke = g.getStroke();
		Color savedColor = g.getColor();
		Font savedFont = g.getFont();
		WindowStyle.prepare(g);
		AffineTransform savedTransform = g.getTransform();
		Composite savedComposite = g.getComposite();
		g.translate(width / 2.0, height / 2.0);
		g.scale(frame.scale, frame.scale);
		g.translate(-width / 2.0, -height / 2.0);
		g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0.0, Math.min(1.0, frame.alpha))));
		try
		{
			paintFrame(g, shown, mode, nowMs);
			Rectangle canvas = layout.window;
			menu.paint(g, style, canvas, mode, nowMs);
			if (modal != null)
				modal.paint(g, style, canvas, mode, nowMs);

			tooltip.paint(g, style, canvas, nowMs);
		}
		finally
		{
			g.setTransform(savedTransform);
			g.setComposite(savedComposite);
			g.setRenderingHints(savedHints);
			g.setStroke(savedStroke);
			g.setColor(savedColor);
			g.setFont(savedFont);
		}

		animations.prune(nowMs);
		return true;
	}

	private void paintFrame(Graphics2D g, WindowSnapshot shown, MotionMode mode, long nowMs)
	{
		WindowLayout lay = layout;
		WindowStyle st = style;
		layers.draw(g, 0, 0, "frame", lay.window.width, lay.window.height, lay.scale, st.theme(), 0, layer ->
		{
			WindowStyle.prepare(layer);
			st.window(layer, lay.window);
			st.header(layer, rect(lay.header));
		});
		paintTabs(g, shown, mode, nowMs);
		paintPage(g, shown, mode, nowMs);
		paintFooter(g);
	}

	private void paintTabs(Graphics2D g, WindowSnapshot shown, MotionMode mode, long nowMs)
	{
		WindowTab[] all = WindowTab.values();
		for (int i = 0; i < all.length; i++)
		{
			WindowTab t = all[i];
			boolean disabled = t.gated() && !shown.storageEnabled();
			double shake = t.hitId().equals(shakeId)
				? ShellMotion.shake(mode, nowMs - shakeMs, style.px(WindowTimes.SHAKE_UNITS)) : 0.0;
			Rectangle2D box = rect(layout.tabs[i]);
			box.setRect(box.getX() + shake, box.getY(), box.getWidth(), box.getHeight());
			style.tab(g, box, t.label(), t == tab, disabled, hoverOf(t.hitId()));
			ringIf(g, t.hitId(), Slant.shape(box, style.metrics().tabSlant));
		}

		Rectangle active = layout.tabs[tab.ordinal()];
		double barX = animations.value(TAB_BAR, nowMs, active.x);
		int barH = Math.max(2, style.px(3));
		g.setColor(style.color(ThemeColor.ACCENT));
		int barW = active.width - (int) Math.min(style.metrics().tabSlant, active.width / 2.0);
		g.fillRect((int) Math.round(barX), active.y + active.height + barH / 2, barW, barH);
		Rectangle2D close = rect(layout.close);
		style.button(g, close, "Close", false, false, hoverOf(ID_CLOSE), pressOf(ID_CLOSE));
		ringIf(g, ID_CLOSE, Slant.shape(close, style.metrics().chipSlant));
	}

	private void paintPage(Graphics2D g, WindowSnapshot shown, MotionMode mode, long nowMs)
	{
		Rectangle body = layout.body;
		long fadeMs = mode.fadeMs(WindowTimes.TAB);
		double t = fadeMs <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, (double) (nowMs - tabMs) / fadeMs));
		double eased = Easing.OUT_CUBIC.apply(t);
		int lift = mode.moves() ? (int) Math.round(style.px(WindowTimes.PAGE_MOVE_UNITS) * (1.0 - eased)) : 0;
		Composite saved = g.getComposite();
		float own = ((AlphaComposite) saved).getAlpha();
		g.setComposite(AlphaComposite.SrcOver.derive((float) (own * eased)));
		WindowTab page = tab;
		WindowStyle st = style;
		AffineTransform savedTransform = g.getTransform();
		g.translate(0, lift);
		PageContext ctx = new PageContext(st, mode, nowMs, shown, body, focus.highlight(), focus.focused(),
			pressedId);
		if (changelog != null)
		{
			changelog.paint(g, ctx);
		}
		else if (!pages.paint(g, page, ctx))
		{
			g.setTransform(savedTransform);
			layers.draw(g, body.x, body.y + lift, "page:" + page.name(), body.width, body.height, layout.scale,
				st.theme(), shown.version(), layer -> paintPlaceholder(layer, st, page, shown, body.width,
					body.height));
		}

		g.setTransform(savedTransform);
		g.setComposite(saved);
	}

	private static void paintPlaceholder(Graphics2D g, WindowStyle st, WindowTab page, WindowSnapshot shown, int w,
		int h)
	{
		WindowStyle.prepare(g);
		st.panel(g, new Rectangle2D.Double(0, 0, w, h));
		int pad = st.metrics().pad;
		Font title = st.font(WindowFonts.Face.BOLD, WindowFonts.TITLE_UNITS);
		FontMetrics tm = g.getFontMetrics(title);
		int y = pad;
		st.text(g, page.label(), pad, y + tm.getAscent(), title, ThemeColor.TEXT);
		y += tm.getHeight() + pad;
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		FontMetrics rm = g.getFontMetrics(regular);
		String text = page.placeholder() + "\n" + info(page, shown) + "\n" + PREVIEW_NOTE;
		int room = Math.max(rm.getHeight(), w - 2 * pad);
		TextLayout.plain(text, rm, room, 0).paint(g, st, pad, y, ThemeColor.SUBTEXT);
	}

	private static String info(WindowTab page, WindowSnapshot shown)
	{
		switch (page)
		{
			case PARTY:
				return "On your team now: " + shown.teamSize() + " of 6.";
			case STORAGE:
				return "In storage now: " + shown.storedCount() + ".";
			case TASKS:
				return "Tasks done today: " + shown.doneTasks() + ".";
			default:
				return "";
		}
	}

	private void paintFooter(Graphics2D g)
	{
		Font small = style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics fm = g.getFontMetrics(small);
		String hint = WindowStyle.ellipsize(hintText(), fm, layout.hints.width);
		int baseline = layout.hints.y + (layout.hints.height + fm.getAscent() - fm.getDescent()) / 2;
		style.text(g, hint, layout.hints.x, baseline, small, ThemeColor.SUBTEXT);
		footerButton(g, layout.reset, ID_RESET, "Reset window");
		footerButton(g, layout.theme, ID_THEME, host.theme().toString());
		footerButton(g, layout.size, ID_SIZE, host.sizeLabel());
		footerButton(g, layout.changelog, ID_CHANGELOG, changelogLabel(), host.whatsNew());
		footerButton(g, layout.feedback, ID_FEEDBACK, "Feedback");
	}

	private String changelogLabel()
	{
		return host.whatsNew() ? "What's New" : "Change log";
	}

	private void footerButton(Graphics2D g, Rectangle box, String id, String label)
	{
		footerButton(g, box, id, label, false);
	}

	private void footerButton(Graphics2D g, Rectangle box, String id, String label, boolean primary)
	{
		Rectangle2D shape = rect(box);
		style.button(g, shape, label, primary, false, hoverOf(id), pressOf(id));
		ringIf(g, id, Slant.shape(shape, style.metrics().chipSlant));
	}

	private String hintText()
	{
		if (!host.keyHints() || !host.keyboard())
			return "";

		if (!focus.focused())
			return HINT_UNFOCUSED;

		switch (keyMode())
		{
			case FIELD:
				return HINT_FIELD;
			case MODAL:
				return HINT_MODAL;
			case MENU:
				return HINT_MENU;
			default:
				return HINT_WINDOW;
		}
	}

	private void publishHits(WindowSnapshot shown)
	{
		List<Hit> list = new ArrayList<>();
		WindowTab[] all = WindowTab.values();
		for (int i = 0; i < all.length; i++)
		{
			boolean enabled = !all[i].gated() || shown.storageEnabled();
			list.add(new Hit(all[i].hitId(), Hit.Kind.TAB, layout.tabs[i], i, enabled,
				enabled ? "" : TeamSwapRule.REFUSAL));
		}

		list.add(new Hit(ID_CLOSE, Hit.Kind.BUTTON, layout.close, CLOSE_ORDER, true, "Close the window (Esc)"));
		list.addAll(changelog != null ? changelog.hits(style, layout.body) : pages.hits(tab, layout.body));
		list.add(new Hit(ID_RESET, Hit.Kind.BUTTON, layout.reset, FOOTER_ORDER, true,
			"Put the window back to its normal size and place"));
		list.add(new Hit(ID_THEME, Hit.Kind.BUTTON, layout.theme, FOOTER_ORDER + 1, true,
			"Switch between the two window styles"));
		list.add(new Hit(ID_SIZE, Hit.Kind.BUTTON, layout.size, FOOTER_ORDER + 2, true, "Change the window size"));
		list.add(new Hit(ID_CHANGELOG, Hit.Kind.BUTTON, layout.changelog, FOOTER_ORDER + 3, true,
			host.whatsNew() ? "See what changed in this version" : "Read the change log"));
		list.add(new Hit(ID_FEEDBACK, Hit.Kind.BUTTON, layout.feedback, FOOTER_ORDER + 4, true,
			"Report a bug or request a feature"));
		hits = Collections.unmodifiableList(list);
		focus.setHits(hits);
	}

	private double hoverOf(String id)
	{
		return id.equals(focus.highlight()) ? 1.0 : 0.0;
	}

	private double pressOf(String id)
	{
		return id.equals(pressedId) ? 1.0 : 0.0;
	}

	private void ringIf(Graphics2D g, String id, Shape shape)
	{
		if (focus.focused() && id.equals(focus.highlight()))
			style.focusRing(g, shape);
	}

	private void stepTab(int delta, long nowMs)
	{
		WindowTab[] all = WindowTab.values();
		boolean[] enabled = new boolean[all.length];
		for (int i = 0; i < all.length; i++)
			enabled[i] = !all[i].gated() || snapshot.storageEnabled();

		int to = WindowFocus.nextTab(enabled, tab.ordinal(), delta);
		selectTab(all[to], nowMs);
		focus.highlightKey(tab.hitId());
	}

	private void selectTab(WindowTab next, long nowMs)
	{
		changelog = null;
		if (next == tab)
			return;

		MotionMode mode = host.motion();
		if (layout != null)
		{
			double from = layout.tabs[tab.ordinal()].x;
			double to = layout.tabs[next.ordinal()].x;
			long ms = mode.moveMs(WindowTimes.TAB);
			if (ms > 0)
				animations.start(TAB_BAR, Tween.of(from, to, nowMs, ms, Easing.OUT_CUBIC), false);
		}

		tab = next;
		tabMs = nowMs;
	}

	private void activate(long nowMs)
	{
		Hit hit = focus.highlighted();
		switch (focus.activate())
		{
			case PRESS:
				perform(hit, nowMs);
				break;
			case MENU:
				Rectangle b = hit.bounds();
				pages.menu(hit, b.x + b.width / 2, b.y + b.height / 2, true);
				break;
			default:
				break;
		}
	}

	private void back(boolean closes, long nowMs)
	{
		int depth = pages.depth() + (changelog != null ? 1 : 0);
		switch (WindowFocus.back(menu.isOpen(), depth, closes))
		{
			case MENU:
				menu.close();
				break;
			case PAGE:
				if (changelog != null)
					changelog = null;
				else
					pages.back();

				break;
			case CLOSE:
				close(nowMs, ShellMotion.Phase.CLOSE);
				break;
			default:
				break;
		}
	}

	private void perform(Hit hit, long nowMs)
	{
		WindowTab chosen = WindowTab.fromHitId(hit.id());
		if (chosen != null)
		{
			selectTab(chosen, nowMs);
			return;
		}

		switch (hit.id())
		{
			case ID_CLOSE:
				close(nowMs, ShellMotion.Phase.CLOSE);
				break;
			case ID_RESET:
				host.resetWindow();
				break;
			case ID_THEME:
				host.flipTheme();
				break;
			case ID_SIZE:
				host.nextSize();
				break;
			case ID_CHANGELOG:
				host.whatsNewSeen();
				changelog = new ChangelogPage(Changelog.load());
				break;
			case ID_FEEDBACK:
				openFeedback(nowMs);
				break;
			case ChangelogPage.ID_BACK:
				changelog = null;
				break;
			default:
				pages.press(hit);
				break;
		}
	}

	private void openFeedback(long nowMs)
	{
		String version = Changelog.load().currentVersion();
		Rectangle b = layout.feedback;
		openMenu(FeedbackLinks.menu(host::openLink, version), b.x + b.width, b.y, focus.keyboardLed(), nowMs);
	}

	private void lockedClick(Hit hit, long nowMs)
	{
		WindowTab locked = WindowTab.fromHitId(hit.id());
		if (locked == null || !locked.gated())
			return;

		shakeId = hit.id();
		shakeMs = nowMs;
		host.notice(NoticeMessage.STORAGE_LOCKED);
	}

	private Hit hitAt(int x, int y)
	{
		for (int i = hits.size() - 1; i >= 0; i--)
			if (hits.get(i).contains(x, y))
				return hits.get(i);

		return null;
	}

	private boolean modalOpen()
	{
		if (modal != null && !modal.isOpen())
		{
			modal = null;
			field = null;
		}

		return modal != null;
	}

	private static Rectangle2D rect(Rectangle r)
	{
		return new Rectangle2D.Double(r.x, r.y, r.width, r.height);
	}

	private static FontMetrics metrics(Font font)
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			return g.getFontMetrics(font);
		}
		finally
		{
			g.dispose();
		}
	}
}
