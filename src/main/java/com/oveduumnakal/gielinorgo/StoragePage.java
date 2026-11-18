/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * The Storage page: a scrolling list with a row for every stored creature, a Select button that turns the rows into
 * tick boxes, and a Release selected button for the ticked ones. A click on a row moves the creature to the team; a
 * right click or Enter opens its menu. The page only lays out, paints and declares hits; what a click does lives in
 * {@link StoragePageFlow}. The row list is a {@link ScrollList}, so a later Box view or a sort and filter bar can sit
 * beside it without touching the rows. Holds no RuneLite types.
 */
final class StoragePage
{
	/** The id of the Select (or Done) button. */
	static final String ID_SELECT = "storage:select";

	/** The id of the Release selected button. */
	static final String ID_RELEASE = "storage:release";

	/** The start of the id of a stored creature's row; its place in storage follows. */
	static final String ROW_PREFIX = "storage:row:";

	/** The text shown while nothing is stored. */
	static final String EMPTY_TEXT = "Storage is empty. Creatures go here when your team is full.";

	/** The hint over the list while rows are clicked to move. */
	static final String HINT = "Click a creature to move it to your team. Right-click for more.";

	/** The hint over the list while rows are ticked. */
	static final String SELECT_HINT = "Tick the creatures to release, then press Release selected.";

	private static final double ROW_UNITS = 72;
	private static final double SELECT_UNITS = 90;
	private static final double RELEASE_UNITS = 190;
	private static final double HINT_UNITS = 28;
	private static final int NOT_ON_TEAM = -1;

	private final PageText.Sink sink;
	private final PortraitCache pictures;
	private final ScrollList list = new ScrollList();
	private final TreeSet<Integer> ticked = new TreeSet<>();
	private boolean selecting;
	private List<CreatureView> shown;
	private Rectangle heading = new Rectangle();
	private Rectangle select = new Rectangle();
	private Rectangle release = new Rectangle();
	private Rectangle hint = new Rectangle();
	private Rectangle panel = new Rectangle();

	/**
	 * Creates the page.
	 *
	 * @param pictures where creature pictures are
	 * @param sink hears what text is drawn, for tests
	 */
	StoragePage(PortraitCache pictures, PageText.Sink sink)
	{
		this.pictures = pictures;
		this.sink = sink;
	}

	/** @return the creature list, whose offset and bar the tests drive */
	ScrollList list()
	{
		return list;
	}

	/** @return whether the rows are tick boxes */
	boolean selecting()
	{
		return selecting;
	}

	/** @return the ticked places in storage, lowest first */
	TreeSet<Integer> ticked()
	{
		return ticked;
	}

	/**
	 * Turns Select mode on or off. Either way the ticks are cleared.
	 *
	 * @param on whether the rows become tick boxes
	 */
	void select(boolean on)
	{
		selecting = on;
		ticked.clear();
	}

	/**
	 * Ticks or unticks a place.
	 *
	 * @param place the place in storage
	 */
	void toggle(int place)
	{
		if (!ticked.remove(place))
			ticked.add(place);
	}

	/** Forgets Select mode and the ticks, as when the window closes. */
	void reset()
	{
		selecting = false;
		ticked.clear();
		shown = null;
	}

	/**
	 * @param id a hit's id
	 * @return the place in storage a row's id names, or -1 when it is not a row's id
	 */
	static int place(String id)
	{
		if (id == null || !id.startsWith(ROW_PREFIX))
			return NOT_ON_TEAM;

		try
		{
			return Integer.parseInt(id.substring(ROW_PREFIX.length()));
		}
		catch (NumberFormatException e)
		{
			return NOT_ON_TEAM;
		}
	}

	/**
	 * @param place a place in storage
	 * @return the id of that row's hit
	 */
	static String rowId(int place)
	{
		return ROW_PREFIX + place;
	}

	/**
	 * Works out where everything sits and drops ticks whose creature changed. Call it each frame before painting and
	 * before the hits are asked for.
	 *
	 * @param body the page area
	 * @param style the style
	 * @param storage the stored creatures
	 */
	void layout(Rectangle body, WindowStyle style, List<CreatureView> storage)
	{
		if (!storage.equals(shown))
		{
			ticked.retainAll(StorageTicks.kept(shown, storage, ticked));
			shown = storage;
		}

		WindowStyle.Metrics m = style.metrics();
		int pad = m.pad;
		int chip = m.chipHeight;
		panel = new Rectangle(body);
		int selectW = style.px(SELECT_UNITS);
		int releaseW = selecting ? style.px(RELEASE_UNITS) : 0;
		int y = body.y + pad;
		int right = body.x + body.width - pad;
		select = new Rectangle(right - selectW, y, selectW, chip);
		release = selecting ? new Rectangle(select.x - pad / 2 - releaseW, y, releaseW, chip) : new Rectangle();
		int used = selectW + (selecting ? pad / 2 + releaseW : 0) + pad / 2;
		heading = new Rectangle(body.x + pad, y, Math.max(0, body.width - 2 * pad - used), chip);
		int line = style.px(HINT_UNITS);
		hint = new Rectangle(body.x + pad, y + chip + pad / 2, body.width - 2 * pad, line);
		int top = hint.y + line + pad / 2;
		Rectangle viewport = new Rectangle(body.x + pad / 2, top, body.width - pad,
			Math.max(0, body.y + body.height - top - pad / 2));
		list.layout(viewport, style.px(ROW_UNITS), storage.size(), style);
	}

	/**
	 * Lists the page's hits: the buttons, then every row in view.
	 *
	 * @param storage the stored creatures
	 * @param firstOrder the focus order of the first hit
	 * @return the hits
	 */
	List<Hit> hits(List<CreatureView> storage, int firstOrder)
	{
		int order = firstOrder;
		List<Hit> hits = new ArrayList<>();
		String selectTip = selecting ? "Stop selecting and go back to moving creatures"
			: "Tick several creatures to release them together";
		hits.add(new Hit(ID_SELECT, Hit.Kind.BUTTON, select, order++, true, selectTip));
		if (selecting)
		{
			String releaseTip = ticked.isEmpty() ? "Tick the creatures to release first"
				: "Release the ticked creatures. You are asked first.";
			hits.add(new Hit(ID_RELEASE, Hit.Kind.BUTTON, release, order++, !ticked.isEmpty(), releaseTip));
		}

		for (int i = list.firstVisible(); i <= list.lastVisible() && i < storage.size(); i++)
		{
			Rectangle box = list.rowBounds(i);
			if (box != null)
				hits.add(new Hit(rowId(i), Hit.Kind.ROW, box, order++, true, tooltip(storage.get(i))));
		}

		return hits;
	}

	/**
	 * Words a stored creature's tooltip.
	 *
	 * @param view the creature
	 * @return the lines
	 */
	static String tooltip(CreatureView view)
	{
		RowModel row = RowModel.of(view, NOT_ON_TEAM);
		StringBuilder text = new StringBuilder(row.name());
		if (row.nicknamed())
			text.append(" (")
				.append(row.species())
				.append(')');

		text.append('\n')
			.append(view.type().label())
			.append(" - ")
			.append(view.rarity().label());
		if (view.gilded())
			text.append(" - Gilded");

		text.append('\n').append(view.rating().label()).append(" · ").append(view.temperamentLabel())
			.append(": ").append(view.temperamentEffect());
		text.append('\n').append(view.rollsLine());
		text.append("\nClick to move to your team");
		text.append("\nReleasing pays ")
			.append(ReleaseRules.xp(view))
			.append(" XP to your lead");
		return text.append("\nRight-click to rename or release").toString();
	}

	/**
	 * Paints the page.
	 *
	 * @param g the graphics, with (0, 0) at the window's corner
	 * @param ctx what the frame needs
	 * @param storage the stored creatures
	 */
	void paint(Graphics2D g, PageContext ctx, List<CreatureView> storage)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		st.panel(g, rect(panel));
		text.draw(g, "Stored  " + storage.size() + " / " + SaveData.STORAGE_SIZE, heading, regular,
			ThemeColor.TEXT, PageText.Align.LEFT);
		st.button(g, rect(select), selecting ? "Done" : "Select", false, false, ctx.hoverOf(ID_SELECT),
			ctx.pressOf(ID_SELECT));
		if (selecting)
		{
			st.button(g, rect(release), "Release selected (" + ticked.size() + ")", true, ticked.isEmpty(),
				ctx.hoverOf(ID_RELEASE), ctx.pressOf(ID_RELEASE));
		}

		if (storage.isEmpty())
		{
			text.drawWrapped(g, EMPTY_TEXT, list.viewport(), small, ThemeColor.SUBTEXT);
			return;
		}

		text.draw(g, selecting ? SELECT_HINT : HINT, hint, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
		list.paint(g, st, (gr, index, box) -> row(gr, ctx, text, storage.get(index), index, box, small));
	}

	private void row(Graphics2D g, PageContext ctx, PageText text, CreatureView view, int place, Rectangle box,
		Font small)
	{
		WindowStyle st = ctx.style;
		WindowStyle.Metrics m = st.metrics();
		RowModel model = RowModel.of(view, NOT_ON_TEAM);
		String id = rowId(place);
		int gap = Math.max(2, m.pad / 3);
		Rectangle inner = new Rectangle(box.x + m.pad / 2, box.y + gap / 2, box.width - m.pad, box.height - gap);
		Composite saved = PageContext.fade(g, model.dim());
		st.row(g, rect(inner), false, ctx.hoverOf(id), ctx.pressOf(id));
		int x = inner.x + m.slant;
		int right = inner.x + inner.width - m.slant;
		int face = inner.height - m.pad;
		if (selecting)
		{
			int side = Math.max(8, face / 2);
			tickBox(g, st, new Rectangle(x, inner.y + (inner.height - side) / 2, side, side), ticked.contains(place));
			x += side + m.pad;
		}

		Rectangle portrait = new Rectangle(x, inner.y + (inner.height - face) / 2, face, face);
		PortraitFrame.paint(g, st, pictures, model, rect(portrait), m.slant / 2.0, 0, ctx.now, ctx.mode);
		x += face + m.pad;
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		int nameH = g.getFontMetrics(regular).getHeight();
		int infoH = g.getFontMetrics(small).getHeight();
		int barH = Math.max(2, m.bar / 2);
		int top = inner.y + Math.max(0, (inner.height - nameH - infoH - barH - m.pad / 4) / 2);
		String level = model.levelText();
		int levelW = Math.min((right - x) / 3, g.getFontMetrics(small).stringWidth(level) + m.pad);
		Rectangle nameBox = new Rectangle(x, top, Math.max(0, right - x - levelW - m.pad / 2), nameH);
		Rectangle levelBox = new Rectangle(right - levelW, top, levelW, nameH);
		Rectangle info = new Rectangle(x, top + nameH, Math.max(0, right - x), infoH);
		ThemeColor ink = WindowStyle.rowText(false);
		String name = (view.gilded() ? "Gilded " : "") + model.name();
		String detail = view.rarity().label() + " · " + view.type().label() + tag(model);
		if (list.viewport().contains(nameBox))
		{
			text.drawName(g, name, nameBox, ink);
			text.draw(g, level, levelBox, small, ink, PageText.Align.RIGHT);
		}

		if (list.viewport().contains(info))
			text.draw(g, detail, info, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);

		st.hpBar(g, new Rectangle2D.Double(x, top + nameH + infoH + m.pad / 4.0, right - x, barH), model.hp());
		g.setComposite(saved);
	}

	private static String tag(RowModel model)
	{
		String word = PartyPainter.tag(model);
		return word.isEmpty() ? "" : " · " + word;
	}

	private static void tickBox(Graphics2D g, WindowStyle st, Rectangle box, boolean on)
	{
		g.setColor(st.color(ThemeColor.HP_TRACK));
		g.fill(box);
		g.setColor(st.color(ThemeColor.PANEL_BORDER));
		g.setStroke(new BasicStroke(Math.max(1f, st.metrics().border)));
		g.draw(box);
		if (!on)
			return;

		g.setColor(st.color(ThemeColor.GOLD));
		g.setStroke(new BasicStroke(Math.max(2f, st.metrics().border + 1f)));
		g.drawLine(box.x + box.width / 5, box.y + box.height / 2, box.x + box.width * 2 / 5,
			box.y + box.height * 4 / 5);
		g.drawLine(box.x + box.width * 2 / 5, box.y + box.height * 4 / 5, box.x + box.width * 4 / 5,
			box.y + box.height / 5);
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
	 * @return whether the press landed on the list's scroll bar
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

	private static Rectangle2D rect(Rectangle r)
	{
		return new Rectangle2D.Double(r.x, r.y, r.width, r.height);
	}
}
