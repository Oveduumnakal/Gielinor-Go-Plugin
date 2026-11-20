/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The right-click menu of a row, and the menu Enter or Space opens on a highlighted row. It holds a list of items,
 * each with a label, an enabled state, a reason shown beside a disabled one (such as "Coming with the tracker") and
 * an action. One highlight is shared by the pointer and the keys: moving the pointer over an enabled item highlights
 * it, Up and Down move it (wrapping, skipping disabled items), Enter or Space runs it, Esc or Backspace closes the
 * menu. While open the menu consumes every click: a click on an enabled item runs it and closes the menu, a click
 * on a disabled item does nothing, and a click anywhere else closes the menu and is still consumed, so it never
 * reaches the row beneath. The menu scales out from the click point over 100 ms in full motion; its hit rectangles
 * are the final ones from the first frame. Actions run on the caller's thread: the caller wraps them for the game
 * thread. Holds no RuneLite types.
 */
final class ContextMenu
{
	private List<Item> items = Collections.emptyList();
	private boolean open;
	private int highlight = -1;
	private Point anchor = new Point();
	private Rectangle bounds = new Rectangle();
	private List<Rectangle> boxes = Collections.emptyList();
	private int itemHeight;
	private long openedMs;
	private long lastMs;
	private Rectangle canvasSeen = new Rectangle();
	private WindowStyle styleSeen;
	private FontMetrics fmSeen;
	private ContextMenu child;
	private int childIndex = -1;
	private boolean childKeys;

	/** One entry of the menu. */
	static final class Item
	{
		private final String label;
		private final boolean enabled;
		private final String reason;
		private final Runnable action;
		private final boolean separator;
		private final boolean opens;
		private final List<Item> children;

		private Item(String label, boolean enabled, String reason, Runnable action, boolean separator, boolean opens)
		{
			this(label, enabled, reason, action, separator, opens, null);
		}

		private Item(String label, boolean enabled, String reason, Runnable action, boolean separator, boolean opens,
			List<Item> children)
		{
			this.children = children;
			this.label = label;
			this.enabled = enabled;
			this.reason = reason == null ? "" : reason;
			this.action = action;
			this.separator = separator;
			this.opens = opens;
		}

		/**
		 * @param label the text
		 * @param action what to do when it is chosen
		 * @return an enabled item
		 */
		static Item of(String label, Runnable action)
		{
			return new Item(label, true, null, action, false, false);
		}

		/**
		 * @param label the text
		 * @param reason why it cannot be chosen, shown beside it; may be empty
		 * @return a dim item that does nothing
		 */
		static Item disabled(String label, String reason)
		{
			return new Item(label, false, reason, null, false, false);
		}

		/**
		 * @param label the text
		 * @param detail a short text drawn at the right edge, such as the odds of a jar; may be empty
		 * @param action what to do when it is chosen
		 * @return an enabled item with a detail beside it
		 */
		static Item detailed(String label, String detail, Runnable action)
		{
			return new Item(label, true, detail, action, false, false);
		}

		/**
		 * An item that opens a nested list. It is drawn with an arrow, and choosing it runs the action without
		 * closing the menu: the action is what shows the list.
		 *
		 * @param label the text
		 * @param action what to do when it is chosen
		 * @return an enabled item that keeps the menu open
		 */
		static Item opener(String label, Runnable action)
		{
			return new Item(label, true, null, action, false, true);
		}

		/**
		 * An item with a submenu. It is drawn with an arrow; hovering it, or pressing Right or Enter on it, opens the
		 * list beside the menu, and choosing something in that list closes both.
		 *
		 * @param label the text
		 * @param items the submenu's items
		 * @return an enabled item that shows a list
		 */
		static Item submenu(String label, List<Item> items)
		{
			return new Item(label, true, null, null, false, true, Collections.unmodifiableList(new ArrayList<>(items)));
		}

		/** @return the submenu's items, or null when this item has none */
		List<Item> children()
		{
			return children;
		}

		/**
		 * @param label the text, drawn dim, such as the name of what the menu is about
		 * @return a row that cannot be chosen and is skipped by the keys
		 */
		static Item header(String label)
		{
			return new Item(label, false, null, null, false, false);
		}

		/** @return a thin line between two groups of items; it cannot be chosen and the keys skip it */
		static Item line()
		{
			return new Item("", false, null, null, true, false);
		}

		/** @return whether this is the thin line made by {@link #line()} */
		boolean isLine()
		{
			return separator;
		}

		/** @return whether choosing it shows a nested list and keeps the menu open */
		boolean opens()
		{
			return opens;
		}

		/** @return the text */
		String label()
		{
			return label;
		}

		/** @return whether it can be chosen */
		boolean enabled()
		{
			return enabled;
		}

		/** @return why it cannot be chosen, or an empty text */
		String reason()
		{
			return reason;
		}
	}

	/**
	 * @param style the style
	 * @return the font the menu uses, a whole multiple of the regular font's native size
	 */
	static Font font(WindowStyle style)
	{
		return style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
	}

	/**
	 * Opens the menu at a point.
	 *
	 * @param list the items, in order
	 * @param x the click's x, where the menu's corner goes
	 * @param y the click's y
	 * @param canvas the canvas bounds; the menu flips to the other side of the point, and is kept inside
	 * @param style the style
	 * @param fm the measures of {@link #font}
	 * @param keyboard whether a key opened it: the first enabled item (Summary) is then highlighted, otherwise
	 *        nothing is until the pointer or a key moves
	 * @param nowMs the time in milliseconds
	 */
	void open(List<Item> list, int x, int y, Rectangle canvas, WindowStyle style, FontMetrics fm, boolean keyboard,
		long nowMs)
	{
		items = Collections.unmodifiableList(new ArrayList<>(list));
		open = !items.isEmpty();
		anchor = new Point(x, y);
		openedMs = nowMs;
		lastMs = nowMs;
		child = null;
		childIndex = -1;
		childKeys = false;
		highlight = keyboard ? next(-1, 1) : -1;
		layout(canvas, style, fm);
	}

	/** Closes the menu. */
	void close()
	{
		open = false;
		highlight = -1;
		closeChild();
	}

	private void closeChild()
	{
		child = null;
		childIndex = -1;
		childKeys = false;
	}

	/** @return the open submenu, or null when none shows */
	ContextMenu submenu()
	{
		return child != null && child.open ? child : null;
	}

	/** @return whether the menu is open */
	boolean isOpen()
	{
		return open;
	}

	/** @return the items, in order; empty when never opened */
	List<Item> items()
	{
		return items;
	}

	/** @return the index of the highlighted item, or -1 for none */
	int highlight()
	{
		return highlight;
	}

	/** @return the point the menu was opened at */
	Point anchor()
	{
		return new Point(anchor);
	}

	/**
	 * Works the boxes out again, for a new scale or canvas. Painting calls it each frame.
	 *
	 * @param canvas the canvas bounds
	 * @param style the style
	 * @param fm the measures of {@link #font}
	 */
	void layout(Rectangle canvas, WindowStyle style, FontMetrics fm)
	{
		canvasSeen = canvas;
		styleSeen = style;
		fmSeen = fm;
		int pad = style.metrics().pad;
		itemHeight = fm.getHeight() + pad / 2;
		Dimension size = size(style, fm);
		bounds = Tooltip.place(size, anchor, canvas, 0);
		List<Rectangle> list = new ArrayList<>();
		for (int i = 0; i < items.size(); i++)
			list.add(new Rectangle(bounds.x, bounds.y + pad / 2 + i * itemHeight, bounds.width, itemHeight));

		boxes = list;
		if (child != null)
			placeChild();
	}

	private void openChild(int at, boolean keyboard)
	{
		if (child != null && childIndex == at)
		{
			if (keyboard && !childKeys)
			{
				childKeys = true;
				child.highlight = child.next(-1, 1);
			}

			return;
		}

		child = new ContextMenu();
		childIndex = at;
		childKeys = keyboard;
		child.open(items.get(at).children, 0, 0, canvasSeen, styleSeen, fmSeen, keyboard, lastMs);
		placeChild();
	}

	private void placeChild()
	{
		Rectangle row = boxes.get(childIndex);
		Dimension size = child.size(styleSeen, fmSeen);
		int x = bounds.x + bounds.width;
		if (x + size.width > canvasSeen.x + canvasSeen.width)
			x = bounds.x - size.width;

		child.anchorAt(x, row.y - styleSeen.metrics().pad / 2, canvasSeen, styleSeen, fmSeen);
	}

	private void syncChild(int at)
	{
		if (items.get(at).children != null)
			openChild(at, false);
		else
			closeChild();
	}

	/**
	 * Measures the menu as it would be laid out now, without placing it.
	 *
	 * @param style the style
	 * @param fm the measures of {@link #font}
	 * @return the width and height of the menu's box
	 */
	Dimension size(WindowStyle style, FontMetrics fm)
	{
		int pad = style.metrics().pad;
		int rowHeight = fm.getHeight() + pad / 2;
		int gap = 2 * pad;
		int widest = 0;
		for (Item item : items)
		{
			int w = fm.stringWidth(item.label) + (item.reason.isEmpty() ? 0 : gap + fm.stringWidth(item.reason));
			if (item.opens)
				w += gap + arrowWidth(fm);

			widest = Math.max(widest, w);
		}

		return new Dimension(widest + 2 * pad, items.size() * rowHeight + pad);
	}

	/**
	 * Moves an open menu to a new anchor and works its boxes out again. A nested list uses it to sit beside the menu
	 * that opened it.
	 *
	 * @param x the anchor's x
	 * @param y the anchor's y
	 * @param canvas the canvas bounds
	 * @param style the style
	 * @param fm the measures of {@link #font}
	 */
	void anchorAt(int x, int y, Rectangle canvas, WindowStyle style, FontMetrics fm)
	{
		anchor = new Point(x, y);
		layout(canvas, style, fm);
	}

	/** @return the menu's box, by its final place */
	Rectangle bounds()
	{
		return new Rectangle(bounds);
	}

	/**
	 * @param index the item
	 * @return the item's box, by its final place, or null when there is no such item
	 */
	Rectangle itemBounds(int index)
	{
		return index < 0 || index >= boxes.size() ? null : new Rectangle(boxes.get(index));
	}

	/**
	 * Finds the item under a point.
	 *
	 * @param x the point's x
	 * @param y the point's y
	 * @return the item's index, or -1 when the point is not on an item
	 */
	int itemAt(int x, int y)
	{
		for (int i = 0; i < boxes.size(); i++)
			if (boxes.get(i).contains(x, y))
				return i;

		return -1;
	}

	/**
	 * Moves the highlight, wrapping and skipping disabled items. With no enabled item nothing moves.
	 *
	 * @param delta 1 for down, -1 for up
	 */
	void move(int delta)
	{
		int step = delta < 0 ? -1 : 1;
		int at = next(highlight < 0 && step < 0 ? 0 : highlight, step);
		if (at >= 0)
			highlight = at;

		if (child != null && childIndex != highlight)
			closeChild();
	}

	private int next(int from, int step)
	{
		int n = items.size();
		for (int i = 1; i <= n; i++)
		{
			int at = Math.floorMod(from + step * i, n);
			if (items.get(at).enabled)
				return at;
		}

		return -1;
	}

	/**
	 * Runs the highlighted item and closes the menu; on an item with a submenu it opens the list instead.
	 *
	 * @return whether an item ran; false when nothing enabled is highlighted
	 */
	boolean activate()
	{
		return activate(true);
	}

	private boolean activate(boolean keyboard)
	{
		if (!open || highlight < 0 || !items.get(highlight).enabled)
			return false;

		Item item = items.get(highlight);
		if (item.children != null)
		{
			openChild(highlight, keyboard);
			return true;
		}

		Runnable action = item.action;
		if (!item.opens)
			close();

		if (action != null)
			action.run();

		return true;
	}

	/**
	 * Handles the pointer moving: it highlights an enabled item under it.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether the pointer is over the menu, so the caller does not hover what lies beneath
	 */
	boolean onMouseMoved(int x, int y)
	{
		if (!open)
			return false;

		if (child != null && child.open && child.bounds.contains(x, y))
		{
			child.onMouseMoved(x, y);
			return true;
		}

		int at = itemAt(x, y);
		if (at >= 0 && items.get(at).enabled)
		{
			highlight = at;
			syncChild(at);
		}

		return bounds.contains(x, y);
	}

	/**
	 * Handles a mouse button going down.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @param button the button, as {@link MouseEvent#getButton}
	 * @return whether the click was consumed: always while the menu is open
	 */
	boolean onMousePressed(int x, int y, int button)
	{
		if (!open)
			return false;

		if (child != null && child.open && child.bounds.contains(x, y))
		{
			child.onMousePressed(x, y, button);
			if (!child.open)
				close();

			return true;
		}

		int at = itemAt(x, y);
		if (at < 0)
		{
			close();
			return true;
		}

		if (button == MouseEvent.BUTTON1 && items.get(at).enabled)
		{
			highlight = at;
			activate(false);
		}

		return true;
	}

	/**
	 * Tells whether the menu uses a key, so a key listener can consume its pressed, typed and released events.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @return whether the menu is open and the key is Up, Down, Enter, Space, Esc or Backspace, or Right on an
	 *         item with a submenu, or Left while a submenu shows
	 */
	boolean uses(int keyCode)
	{
		return open && (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_ENTER
			|| keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE
			|| keyCode == KeyEvent.VK_RIGHT && highlight >= 0 && items.get(highlight).children != null
			|| keyCode == KeyEvent.VK_LEFT && child != null);
	}

	/**
	 * Handles a key going down.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @return whether the key was used and must be consumed
	 */
	boolean onKeyPressed(int keyCode)
	{
		if (!uses(keyCode))
			return false;

		if (child != null && child.open && childKeys)
		{
			if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE)
			{
				closeChild();
				return true;
			}

			child.onKeyPressed(keyCode);
			if (!child.open)
				close();

			return true;
		}

		switch (keyCode)
		{
			case KeyEvent.VK_RIGHT:
				openChild(highlight, true);
				break;
			case KeyEvent.VK_LEFT:
				closeChild();
				break;
			case KeyEvent.VK_UP:
				move(-1);
				break;
			case KeyEvent.VK_DOWN:
				move(1);
				break;
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_SPACE:
				activate();
				break;
			default:
				close();
				break;
		}

		return true;
	}

	/**
	 * Paints the menu: scaled out from the click point and fading in while it opens, then plain.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param canvas the canvas bounds
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 */
	void paint(Graphics2D g, WindowStyle style, Rectangle canvas, MotionMode mode, long nowMs)
	{
		if (!open)
			return;

		lastMs = nowMs;
		Font font = font(style);
		FontMetrics fm = g.getFontMetrics(font);
		layout(canvas, style, fm);
		AffineTransform saved = g.getTransform();
		Composite savedComposite = g.getComposite();
		long scaleMs = mode.moveMs(WindowTimes.MENU);
		long fadeMs = mode.fadeMs(WindowTimes.MENU);
		double scale = 1.0;
		if (scaleMs > 0)
			scale = 0.85 + 0.15 * Easing.OUT_CUBIC.apply((double) (nowMs - openedMs) / scaleMs);

		if (fadeMs > 0)
		{
			float alpha = (float) Easing.OUT_CUBIC.apply((double) (nowMs - openedMs) / fadeMs);
			g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
		}

		g.translate(anchor.x, anchor.y);
		g.scale(scale, scale);
		g.translate(-anchor.x, -anchor.y);
		style.popup(g, new Rectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height), "menu",
			ThemeColor.PANEL_BOX, ThemeColor.PANEL_BORDER, false, Readability.BODY);
		int pad = style.metrics().pad;
		for (int i = 0; i < items.size(); i++)
		{
			Item item = items.get(i);
			Rectangle box = boxes.get(i);
			if (i == highlight)
			{
				style.select(g, style.outline(WindowStyle.Kind.BUTTON, new Rectangle2D.Double(
					box.x + style.metrics().border, box.y, box.width - 2 * style.metrics().border, box.height)));
			}

			if (item.separator)
			{
				g.setColor(style.color(ThemeColor.ROW_BORDER));
				g.fillRect(box.x + pad, box.y + box.height / 2, box.width - 2 * pad, 1);
				continue;
			}

			int baseline = box.y + (box.height + fm.getAscent() - fm.getDescent()) / 2;
			style.text(g, item.label, box.x + pad, baseline, font,
				item.enabled ? ThemeColor.TEXT : ThemeColor.TAB_DISABLED_TEXT);
			if (!item.reason.isEmpty())
				style.text(g, item.reason, box.x + box.width - pad - fm.stringWidth(item.reason), baseline, font,
					ThemeColor.SUBTEXT);

			if (item.opens)
				paintArrow(g, style, box, fm, pad);
		}

		g.setTransform(saved);
		g.setComposite(savedComposite);
		if (child != null && child.open)
			child.paint(g, style, canvas, mode, nowMs);
	}

	private static int arrowWidth(FontMetrics fm)
	{
		return Math.max(4, fm.getHeight() / 3);
	}

	private static void paintArrow(Graphics2D g, WindowStyle style, Rectangle box, FontMetrics fm, int pad)
	{
		int w = arrowWidth(fm);
		int h = 2 * w;
		int x = box.x + box.width - pad - w;
		int y = box.y + (box.height - h) / 2;
		g.setColor(style.color(ThemeColor.TEXT));
		g.fillPolygon(new int[] {x, x + w, x}, new int[] {y, y + h / 2, y + h}, 3);
	}
}
