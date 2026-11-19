/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * The Battle page: the friends in the party, the Rival, the City Champions and the Elite challenge in one scrolling
 * column of cards, as the old Battle tab had them. The column scrolls with the wheel, its bar, a drag on it and the
 * Page Up, Page Down, Home and End keys (Up and Down keep moving between buttons); only the buttons that are in view
 * are hits. The words and the gating come from {@link BattleModel}, the geometry from {@link BattleLayout}. Pressing
 * a button runs one of the old panel's actions ({@link PanelActions}); Forfeit asks first in a painted dialog and
 * Trade runs {@link BattleTrade}. Holds no RuneLite types.
 */
final class BattlePage
{
	/** The title of the Forfeit confirmation. */
	static final String FORFEIT_TITLE = "Forfeit";

	/** The button that keeps the Elite run going. */
	static final String FORFEIT_NO = "Keep going";

	/** The button that gives the run up. */
	static final String FORFEIT_YES = "Forfeit";

	private static final double LINE_UNITS = 30;

	private final WindowShell shell;
	private final PanelActions actions;
	private final LongSupplier clock;
	private final PageText.Sink sink;
	private final ScrollList list = new ScrollList();
	private Rectangle viewport = new Rectangle();
	private BattleLayout laid;
	private List<BattleBlock> blocks = new ArrayList<>();
	private PanelState lastPanel;
	private FriendsState lastFriends;
	private int lastWidth = -1;
	private double lastScale = -1;
	private WindowTheme lastTheme;

	/**
	 * Creates the page.
	 *
	 * @param shell opens the page's dialogs and menus
	 * @param actions what a button does to the game
	 * @param clock the time in milliseconds
	 * @param sink hears what text is drawn, for tests
	 */
	BattlePage(WindowShell shell, PanelActions actions, LongSupplier clock, PageText.Sink sink)
	{
		this.shell = shell;
		this.actions = actions;
		this.clock = clock;
		this.sink = sink;
	}

	/** @return the scrolling column, whose offset and bar the tests drive */
	ScrollList list()
	{
		return list;
	}

	/** @return the blocks the page shows now, for tests */
	List<BattleBlock> blocks()
	{
		return blocks;
	}

	/** @return the column's layout, for tests */
	BattleLayout layoutNow()
	{
		return laid;
	}

	/**
	 * Works out where everything sits. Call it each frame before painting and before the hits are asked for.
	 *
	 * @param body the page area
	 * @param style the style
	 * @param snapshot what the game shows
	 */
	void layout(Rectangle body, WindowStyle style, WindowSnapshot snapshot)
	{
		int pad = style.metrics().pad;
		viewport = new Rectangle(body.x + pad / 2, body.y + pad / 2, Math.max(0, body.width - pad),
			Math.max(0, body.height - pad));
		int bar = Math.max(4, style.px(ScrollList.BAR_UNITS));
		int width = Math.max(1, viewport.width - bar - pad / 2);
		PanelState panel = snapshot.state();
		FriendsState friends = snapshot.friends();
		boolean same = panel == lastPanel && friends == lastFriends && width == lastWidth
			&& style.scale() == lastScale && style.theme() == lastTheme;
		if (!same || laid == null)
		{
			blocks = BattleModel.build(panel, friends);
			laid = BattleLayout.of(blocks, width, style, fonts(style));
			lastPanel = panel;
			lastFriends = friends;
			lastWidth = width;
			lastScale = style.scale();
			lastTheme = style.theme();
		}

		int line = style.px(LINE_UNITS);
		list.layout(viewport, line, (laid.height() + line - 1) / line, style);
	}

	private static BattleLayout.Fonts fonts(WindowStyle style)
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			return new BattleLayout.Fonts(g.getFontMetrics(style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS)),
				g.getFontMetrics(style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS)),
				g.getFontMetrics(style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS)));
		}
		finally
		{
			g.dispose();
		}
	}

	private Rectangle onScreen(Rectangle box)
	{
		return new Rectangle(viewport.x + box.x, viewport.y - list.offset() + box.y, box.width, box.height);
	}

	/**
	 * Lists the page's hits: every button in view, and the tooltips of cards that have no button.
	 *
	 * @return the hits
	 */
	List<Hit> hits()
	{
		List<Hit> hits = new ArrayList<>();
		int order = WindowShell.PAGE_ORDER;
		Rectangle clip = list.viewport();
		for (BattleLayout.Placed one : laid.placed())
		{
			BattleBlock block = one.block;
			if (block.kind() != BattleBlock.Kind.CARD)
				continue;

			Rectangle card = onScreen(one.box).intersection(clip);
			if (!block.tip().isEmpty() && !card.isEmpty())
				hits.add(new Hit(block.id(), Hit.Kind.AREA, card, -1, true, block.tip()));

			for (int i = 0; i < block.items().size(); i++)
			{
				BattleBlock.Item item = block.items().get(i);
				List<Rectangle> boxes = one.buttons.get(i);
				for (int b = 0; b < item.buttons().size(); b++)
				{
					BattleBlock.Btn button = item.buttons().get(b);
					Rectangle box = onScreen(boxes.get(b)).intersection(clip);
					if (!box.isEmpty())
					{
						hits.add(new Hit(button.id(), Hit.Kind.BUTTON, box, order++, button.enabled(),
							button.tip()));
					}
				}
			}
		}

		return hits;
	}

	/**
	 * Paints the page.
	 *
	 * @param g the graphics, with (0, 0) at the window's corner
	 * @param ctx what the frame needs
	 */
	void paint(Graphics2D g, PageContext ctx)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Font bold = st.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		Shape saved = g.getClip();
		Rectangle clip = list.viewport();
		g.clipRect(clip.x, clip.y, clip.width, clip.height);
		for (BattleLayout.Placed one : laid.placed())
		{
			if (!onScreen(one.box).intersects(clip))
				continue;

			BattleBlock block = one.block;
			if (block.kind() == BattleBlock.Kind.HEADING)
			{
				text.draw(g, block.text(), onScreen(one.title), small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
			}
			else if (block.kind() == BattleBlock.Kind.NOTE)
			{
				text.drawWrapped(g, block.text(), onScreen(one.title), small, ThemeColor.SUBTEXT);
			}
			else
			{
				card(g, ctx, text, one, new Font[] {bold, regular, small});
			}
		}

		g.setClip(saved);
		list.paintBar(g, st);
	}

	private void card(Graphics2D g, PageContext ctx, PageText text, BattleLayout.Placed one, Font[] fonts)
	{
		WindowStyle st = ctx.style;
		BattleBlock block = one.block;
		Rectangle box = onScreen(one.box);
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		Rectangle stripe = onScreen(one.stripe);
		g.setColor(st.color(block.stripe()));
		Area bar = new Area(st.panelShape(new Rectangle2D.Double(box.x, box.y, box.width, box.height)));
		bar.intersect(new Area(stripe));
		g.fill(bar);
		Rectangle clip = list.viewport();
		Rectangle title = onScreen(one.title);
		if (title.intersects(clip))
		{
			text.draw(g, block.text(), title, fonts[0], ThemeColor.TEXT, PageText.Align.LEFT);
			text.draw(g, block.status(), onScreen(one.status), fonts[2], block.statusColor(),
				PageText.Align.RIGHT);
		}

		for (int i = 0; i < block.items().size(); i++)
		{
			BattleBlock.Item item = block.items().get(i);
			Rectangle row = onScreen(one.items.get(i));
			if (!row.intersects(clip))
				continue;

			if (!item.isButtons())
			{
				text.drawWrapped(g, item.text(), row, item.small() ? fonts[2] : fonts[1], item.color());
				continue;
			}

			for (int b = 0; b < item.buttons().size(); b++)
			{
				BattleBlock.Btn button = item.buttons().get(b);
				Rectangle at = onScreen(one.buttons.get(i).get(b));
				st.button(g, new Rectangle2D.Double(at.x, at.y, at.width, at.height), button.label(), true,
					!button.enabled(), ctx.hoverOf(button.id()), ctx.pressOf(button.id()));
			}
		}
	}

	/**
	 * A button of the page was pressed.
	 *
	 * @param hit the button's hit
	 * @param panel what the game shows
	 * @param friends the friends in the party
	 */
	void press(Hit hit, PanelState panel, FriendsState friends)
	{
		String id = hit.id();
		if (!hit.enabled())
			return;

		if (id.startsWith(BattleModel.CHAMPION_PREFIX))
		{
			actions.challenge(id.substring(BattleModel.CHAMPION_PREFIX.length()));
			return;
		}

		switch (id)
		{
			case BattleModel.ID_ELITE_CHALLENGE:
				actions.eliteChallenge();
				return;
			case BattleModel.ID_ELITE_CONTINUE:
				actions.eliteContinue();
				return;
			case BattleModel.ID_ELITE_FORFEIT:
				confirmForfeit();
				return;
			case BattleModel.ID_RIVAL:
				actions.challenge(RivalRules.ID);
				return;
			default:
				break;
		}

		pressFriend(hit, panel, friends);
	}

	private void pressFriend(Hit hit, PanelState panel, FriendsState friends)
	{
		String id = hit.id();
		long who = BattleModel.memberOf(id, BattleModel.FRIEND_CHALLENGE_PREFIX);
		if (who >= 0)
		{
			actions.challengeFriend(who);
			return;
		}

		who = BattleModel.memberOf(id, BattleModel.ACCEPT_PREFIX);
		if (who >= 0)
		{
			actions.answerTrade(who, BattleModel.offerOf(id), true);
			return;
		}

		who = BattleModel.memberOf(id, BattleModel.DECLINE_PREFIX);
		if (who >= 0)
		{
			actions.answerTrade(who, BattleModel.offerOf(id), false);
			return;
		}

		who = BattleModel.memberOf(id, BattleModel.FRIEND_TRADE_PREFIX);
		FriendView friend = who < 0 ? null : friendOf(friends, who);
		if (friend != null)
		{
			Rectangle b = hit.bounds();
			new BattleTrade(shell, actions, clock, panel, friend, b.x + b.width / 2, b.y + b.height / 2).start();
		}
	}

	private static FriendView friendOf(FriendsState friends, long memberId)
	{
		for (FriendView friend : friends.friends())
		{
			if (friend.memberId() == memberId)
				return friend;
		}

		return null;
	}

	private void confirmForfeit()
	{
		String message = "Give up the Elite run? Your team rests again and the next attempt begins at trainer 1.";
		Modal ask = new Modal(FORFEIT_TITLE, message, Arrays.asList(FORFEIT_NO, FORFEIT_YES), 0, choice ->
		{
			if (choice == 1)
				actions.eliteForfeit();
		}, () ->
		{
		});
		shell.openModal(ask, null, clock.getAsLong());
	}

	/**
	 * The wheel turned over the page.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @param rotation the wheel's clicks
	 */
	void wheel(int x, int y, int rotation)
	{
		list.onWheel(x, y, rotation);
	}

	/**
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @return whether the press landed on the scroll bar
	 */
	boolean pressed(int x, int y)
	{
		boolean onBar = list.scrollable() && list.trackBounds().contains(x, y);
		boolean took = list.onPress(x, y);
		return onBar && took;
	}

	/**
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 */
	void dragged(int x, int y)
	{
		list.onDrag(x, y);
	}

	/** @return whether the press became a scroll, so the release is not a click */
	boolean released()
	{
		return list.onRelease();
	}

	/**
	 * Scrolls with a key.
	 *
	 * @param keyCode the key
	 * @return whether the key was a scrolling key
	 */
	boolean key(int keyCode)
	{
		int page = Math.max(1, list.viewport().height - list.rowHeight());
		switch (keyCode)
		{
			case KeyEvent.VK_PAGE_UP:
				list.scrollBy(-page);
				return true;
			case KeyEvent.VK_PAGE_DOWN:
				list.scrollBy(page);
				return true;
			case KeyEvent.VK_HOME:
				list.setOffset(0);
				return true;
			case KeyEvent.VK_END:
				list.setOffset(list.maxOffset());
				return true;
			default:
				return false;
		}
	}
}
