/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Paints the Bag page and lists its hits: the trainer box, the jars to pick from, the healing items, and the eggs
 * with their progress. A click on a jar selects it for throwing; a click on the title changes it when more than one is
 * earned; the rest are for the tooltips. Holds no RuneLite types.
 */
final class BagPainter
{
	/** The id of the trainer title's hit. */
	static final String ID_TITLE = "bag:title";

	/** The start of a jar hit's id, followed by its place in the list of jars shown. */
	static final String JAR_PREFIX = "bag:jar:";

	private static final String ITEM_PREFIX = "bag:item:";
	private static final String EGG_PREFIX = "bag:egg:";

	/** The start of an egg's pin button's id, followed by the egg's place in the list. */
	static final String PIN_PREFIX = "bag:pin:";

	private static final double ICON_SHARE = 0.72;

	/** The smallest share of a cell's height its picture shrinks to before the count takes the small font. */
	private static final double ICON_FLOOR = 0.4;

	/** The smallest gap around a cell's picture and count, in pixels. */
	private static final int MIN_CELL_PAD = 2;

	/** The room kept clear between a count and its cell's slanted right side, in layout units. */
	private static final double COUNT_MARGIN_UNITS = 8;

	/** The least of that room, in pixels. */
	private static final int MIN_COUNT_MARGIN = 4;

	/** The smallest picture a very long count may leave, in pixels. */
	private static final int MIN_ICON = 8;

	/** The gap around a cell's picture when a very long count leaves no room for more, in pixels. */
	private static final int SQUEEZED_PAD = 1;
	private static final double DIM = 0.4;

	private final PageText.Sink sink;

	/**
	 * Creates the painter.
	 *
	 * @param sink hears what text is drawn, for tests
	 */
	BagPainter(PageText.Sink sink)
	{
		this.sink = sink;
	}

	/**
	 * @param place a place in the list of jars shown
	 * @return the id of that jar's hit
	 */
	static String jarId(int place)
	{
		return JAR_PREFIX + place;
	}

	/**
	 * @param id a hit's id
	 * @return the place in the list of eggs that a pin button's id names, or -1 when it is not an egg pin's
	 */
	static int pinPlace(String id)
	{
		if (id == null || !id.startsWith(PIN_PREFIX))
			return -1;

		try
		{
			return Integer.parseInt(id.substring(PIN_PREFIX.length()));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/**
	 * @param id a hit's id
	 * @return the place in the list of jars shown that the id names, or -1 when it is not a jar's
	 */
	static int jarPlace(String id)
	{
		if (id == null || !id.startsWith(JAR_PREFIX))
			return -1;

		try
		{
			return Integer.parseInt(id.substring(JAR_PREFIX.length()));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/**
	 * Lists the page's hits in focus order.
	 *
	 * @param layout where everything sits
	 * @param style the style the page was laid out in
	 * @param state what the page shows
	 * @return the hits: the title, every jar, every egg's pin button, then the areas that only carry tooltips
	 */
	List<Hit> hits(BagLayout layout, WindowStyle style, PanelState state)
	{
		List<Hit> hits = new ArrayList<>();
		int order = WindowShell.PAGE_ORDER;
		boolean switchable = state.titles().size() > 1;
		hits.add(new Hit(ID_TITLE, Hit.Kind.BUTTON, layout.titleLine, switchable ? order++ : -1, switchable,
			switchable ? "Click to change title" : "Earn a title to show it here"));
		List<JarType> shown = BagText.jarsShown(state);
		for (int i = 0; i < shown.size() && i < layout.jarCells.length; i++)
			hits.add(new Hit(jarId(i), Hit.Kind.BUTTON, layout.jarCells[i], order++, true,
				BagText.jarTip(shown.get(i))));

		int used = state.tracker().count();
		for (int i = 0; i < layout.eggRows.length; i++)
		{
			EggView egg = state.eggs().get(i);
			Rectangle pin = PinButton.bounds(style, layout.eggRows[i], layout.eggLine);
			hits.add(PinButton.hit(PIN_PREFIX + i, pin, order++, egg.pinned(),
				PinButton.refusal(egg.pinned(), false, used)));
		}

		ItemType[] types = ItemType.values();
		for (int i = 0; i < types.length; i++)
			hits.add(new Hit(ITEM_PREFIX + i, Hit.Kind.AREA, layout.itemCells[i], -1, true,
				BagText.itemTip(types[i], count(state.items(), types[i]))));

		for (int i = 0; i < layout.eggRows.length; i++)
			hits.add(new Hit(EGG_PREFIX + i, Hit.Kind.AREA,
				PinButton.rest(style, layout.eggRows[i], layout.eggLine), -1, true,
				BagText.eggTip(state.eggs().get(i))));

		hits.add(new Hit("bag:steps", Hit.Kind.AREA, layout.stepsLine, -1, true,
			state.starters().isEmpty() ? BagText.stepsTip(state.stepProgress()) : GoGame.STARTER_FIRST_TEXT));
		hits.add(new Hit("bag:level", Hit.Kind.AREA, layout.trainerBar, -1, true,
			Math.round(state.trainerProgress() * 100) + "% to the next trainer level"));
		return hits;
	}

	/**
	 * Paints the page.
	 *
	 * @param g the graphics, with (0, 0) at the window's corner
	 * @param ctx what the frame needs
	 * @param layout where everything sits
	 * @param state what the page shows
	 */
	void paint(Graphics2D g, PageContext ctx, BagLayout layout, PanelState state)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		st.panel(g, rect(layout.trainer), WindowDepth.Pane.BAG_TRAINER);
		st.panel(g, rect(layout.jars), WindowDepth.Pane.BAG_JARS);
		st.panel(g, rect(layout.items), WindowDepth.Pane.BAG_HEALING_ITEMS);
		st.panel(g, rect(layout.eggs), WindowDepth.Pane.BAG_EGGS);

		trainer(g, ctx, text, layout, state, regular, small);
		text.draw(g, "Throw", layout.jarsHeading, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
		jars(g, ctx, text, layout, state, regular);
		text.draw(g, "Healing items", layout.itemsHeading, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
		items(g, ctx, text, layout, state, regular);
		text.draw(g, "Eggs", layout.eggsHeading, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
		eggs(g, ctx, text, layout, state, regular, small);
	}

	private void trainer(Graphics2D g, PageContext ctx, PageText text, BagLayout layout, PanelState state,
		Font regular, Font small)
	{
		WindowStyle st = ctx.style;
		boolean titled = state.shownTitle() != null;
		String name = BagText.titleName(state.shownTitle()) + " Lv " + state.trainerLevel();
		text.draw(g, name, layout.titleLine, regular, titled ? ThemeColor.GOLD : ThemeColor.TEXT,
			PageText.Align.LEFT);
		if (ctx.focused && ID_TITLE.equals(ctx.highlight))
			st.focusRing(g, st.outline(WindowStyle.Kind.CHIP, rect(layout.titleLine)));

		int won = state.battlesWon();
		text.draw(g, won + (won == 1 ? " battle won" : " battles won"), layout.winsLine, small, ThemeColor.SUBTEXT,
			PageText.Align.RIGHT);
		st.xpBar(g, rect(layout.trainerBar), state.trainerProgress());
		text.draw(g, "Next jar " + state.stepProgress() + " / " + GoGame.TILES_PER_JAR, layout.stepsLine, small,
			ThemeColor.SUBTEXT, PageText.Align.LEFT);
		st.bar(g, rect(layout.stepsBar), state.stepProgress() / (double) GoGame.TILES_PER_JAR, ThemeColor.JAR_FILL,
			ThemeColor.XP_TRACK);
	}

	private void jars(Graphics2D g, PageContext ctx, PageText text, BagLayout layout, PanelState state, Font regular)
	{
		List<JarType> shown = BagText.jarsShown(state);
		int cells = Math.min(shown.size(), layout.jarCells.length);
		if (cells == 0)
			return;

		int most = 0;
		for (int i = 0; i < cells; i++)
			most = Math.max(most, count(state.jars(), shown.get(i)));

		CountFit fit = fit(g, ctx.style, layout.jarCells[0], String.valueOf(most), regular);
		for (int i = 0; i < cells; i++)
		{
			JarType type = shown.get(i);
			String id = jarId(i);
			boolean selected = type == state.selectedJar();
			int count = count(state.jars(), type);
			cell(g, ctx, text, layout.jarCells[i], id, selected, JarIcons.source(type, JarIcons.pixels(g, fit.size)),
				String.valueOf(count),
				count > 0 || selected, fit);
		}
	}

	private void items(Graphics2D g, PageContext ctx, PageText text, BagLayout layout, PanelState state, Font regular)
	{
		ItemType[] types = ItemType.values();
		int most = 0;
		for (ItemType type : types)
			most = Math.max(most, count(state.items(), type));

		CountFit fit = fit(g, ctx.style, layout.itemCells[0], String.valueOf(most), regular);
		for (int i = 0; i < types.length; i++)
		{
			int count = count(state.items(), types[i]);
			cell(g, ctx, text, layout.itemCells[i], ITEM_PREFIX + i, false,
				ItemIcons.source(types[i], JarIcons.pixels(g, fit.size)),
				String.valueOf(count), count > 0, fit);
		}
	}

	/**
	 * Sizes a grid of cells so its longest count is never cut: the pictures shrink to make room, past their smallest
	 * size the counts take the small font, then the gaps close up. Every cell of a grid gets the same answer.
	 *
	 * @param g the graphics
	 * @param st the style
	 * @param box one cell of the grid
	 * @param widest the longest count in the grid
	 * @param regular the font the counts are drawn in when there is room
	 * @return the picture size and the font
	 */
	private static CountFit fit(Graphics2D g, WindowStyle st, Rectangle box, String widest, Font regular)
	{
		int full = (int) Math.round(box.height * ICON_SHARE);
		int floor = (int) Math.round(box.height * ICON_FLOOR);
		int pad = Math.max(MIN_CELL_PAD, (box.height - full) / 2);
		int right = st.metrics().slant / 2 + Math.max(MIN_COUNT_MARGIN, st.px(COUNT_MARGIN_UNITS));
		int room = box.width - st.metrics().slant / 2 - 2 * pad - right;
		Font font = regular;
		if (room - g.getFontMetrics(font).stringWidth(widest) < floor)
			font = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);

		int least = floor;
		if (room - g.getFontMetrics(font).stringWidth(widest) < floor)
		{
			pad = SQUEEZED_PAD;
			right = st.metrics().slant / 2 + MIN_COUNT_MARGIN;
			room = box.width - st.metrics().slant / 2 - 2 * pad - right;
			least = Math.min(MIN_ICON, floor);
		}

		int size = Math.max(least, Math.min(full, room - g.getFontMetrics(font).stringWidth(widest)));
		return new CountFit(size, pad, right, font);
	}

	private void cell(Graphics2D g, PageContext ctx, PageText text, Rectangle box, String id, boolean selected,
		BufferedImage icon, String label, boolean bright, CountFit fit)
	{
		WindowStyle st = ctx.style;
		boolean pressable = id.startsWith(JAR_PREFIX);
		Shape shape = st.row(g, rect(box), selected, pressable ? ctx.hoverOf(id) : 0, pressable ? ctx.pressOf(id) : 0);
		if (ctx.focused && id.equals(ctx.highlight))
			st.focusRing(g, shape);

		int size = fit.size;
		int pad = fit.pad;
		Composite saved = g.getComposite();
		if (!bright)
			g.setComposite(AlphaComposite.SrcOver.derive((float) DIM));

		CrispImages.draw(g, icon, box.x + pad + st.metrics().slant / 2, box.y + (box.height - size) / 2.0, size);
		g.setComposite(saved);
		int textX = box.x + pad + st.metrics().slant / 2 + size + pad;
		Rectangle words = new Rectangle(textX, box.y, Math.max(0, box.x + box.width - textX - fit.right), box.height);
		ThemeColor ink = bright ? WindowStyle.rowText(selected) : ThemeColor.SUBTEXT;
		text.draw(g, label, words, fit.font, ink, PageText.Align.LEFT);
	}

	private void eggs(Graphics2D g, PageContext ctx, PageText text, BagLayout layout, PanelState state, Font regular,
		Font small)
	{
		WindowStyle st = ctx.style;
		List<EggView> eggs = state.eggs();
		if (eggs.isEmpty())
		{
			text.draw(g, "No eggs yet", new Rectangle(layout.eggsHeading.x, layout.eggsHeading.y
				+ layout.eggsHeading.height, layout.eggsHeading.width, layout.eggsHeading.height), small,
				ThemeColor.SUBTEXT, PageText.Align.LEFT);
			return;
		}

		int used = state.tracker().count();
		for (int i = 0; i < layout.eggRows.length; i++)
		{
			EggView egg = eggs.get(i);
			Rectangle row = PinButton.rest(st, layout.eggRows[i], layout.eggLine);
			int half = layout.eggLine;
			text.draw(g, egg.label(), new Rectangle(row.x, row.y, row.width, half), small, ThemeColor.TEXT,
				PageText.Align.LEFT);
			String steps = egg.tilesLeft() <= 0 ? "Ready" : egg.tilesLeft() + " steps";
			text.draw(g, steps, new Rectangle(row.x, row.y, row.width, half), small, ThemeColor.SUBTEXT,
				PageText.Align.RIGHT);
			Rectangle bar = new Rectangle(row.x, row.y + half + 1, row.width, Math.max(3, st.metrics().bar / 2));
			st.xpBar(g, rect(bar), egg.progress());
			PinButton.paint(g, ctx, PinButton.bounds(st, layout.eggRows[i], layout.eggLine), PIN_PREFIX + i,
				egg.pinned(), PinButton.refusal(egg.pinned(), false, used) == null);
		}

		if (layout.eggsMore.width > 0)
			text.draw(g, "+" + (eggs.size() - layout.eggRows.length) + " more", layout.eggsMore, small,
				ThemeColor.SUBTEXT, PageText.Align.LEFT);
	}

	private static <T> int count(Map<T, Integer> owned, T key)
	{
		Integer found = owned.get(key);
		return found == null ? 0 : found;
	}

	private static Rectangle2D rect(Rectangle r)
	{
		return new Rectangle2D.Double(r.x, r.y, r.width, r.height);
	}

	/** How big a grid of cells draws its pictures and counts. */
	private static final class CountFit
	{
		private final int size;
		private final int pad;
		private final int right;
		private final Font font;

		private CountFit(int size, int pad, int right, Font font)
		{
			this.size = size;
			this.pad = pad;
			this.right = right;
			this.font = font;
		}
	}
}
