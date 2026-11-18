/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * The Storage page: a scrolling list with a row for every stored creature that passes the filters, in the chosen
 * order, a Select button that turns the rows into tick boxes, a Tick all shown button and a Release selected button
 * for the ticked ones. A click on a row moves the creature to the team; a right click or Enter opens its menu. The
 * Sort and Filter buttons and the filter chips come from {@link StorageControls}; the arranged list comes from
 * {@link StorageView}. A row keeps its real place in storage whatever its position on screen, so ids, ticks and
 * picks never change. The page only lays out, paints and declares hits; what a click does lives in
 * {@link StoragePageFlow}. Holds no RuneLite types.
 */
final class StoragePage
{
	/** The id of the Select (or Done) button. */
	static final String ID_SELECT = "storage:select";

	/** The id of the Release selected button. */
	static final String ID_RELEASE = "storage:release";

	/** The id of the Sort button. */
	static final String ID_SORT = "storage:sort";

	/** The id of the Filter button. */
	static final String ID_FILTER = "storage:filter";

	/** The id of the Tick all shown button. */
	static final String ID_TICK_ALL = "storage:tickall";

	/** The id of the Clear chip, which removes every filter. */
	static final String ID_CLEAR = "storage:clear";

	/** The start of the id of a filter chip's label; its place in the query follows. */
	static final String CHIP_PREFIX = "storage:chip:";

	/** The start of the id of a filter chip's cross; its place in the query follows. */
	static final String CHIPX_PREFIX = "storage:chipx:";

	/** The start of the id of a stored creature's row; its place in storage follows. */
	static final String ROW_PREFIX = "storage:row:";

	/** The text shown while nothing is stored. */
	static final String EMPTY_TEXT = "Storage is empty. Creatures go here when your team is full.";

	/** The text shown while filters hide every stored creature. */
	static final String NO_MATCH_TEXT = "No stored creature matches. Clear the filters to see them all.";

	/** The hint over the list while rows are clicked to move. */
	static final String HINT = "Click a creature to move it to your team. Right-click for more.";

	/** The hint over the list while rows are ticked. */
	static final String SELECT_HINT = "Tick the creatures to release, then press Release selected.";

	private static final double ROW_UNITS = 72;
	private static final String FILTER_TIP = "Show only some creatures. Click a filter chip to show or hide it.";
	private static final String SORT_TIP = "Sort the list. Box order keeps the order the creatures were stored in.";
	private static final int NOT_ON_TEAM = -1;

	private final PageText.Sink sink;
	private final PortraitCache pictures;
	private final ScrollList list = new ScrollList();
	private final TreeSet<Integer> ticked = new TreeSet<>();
	private boolean selecting;
	private List<CreatureView> shown;
	private StorageQuery query = StorageQuery.NONE;
	private List<StorageView.Entry> entries = new ArrayList<>();
	private List<CreatureView> source;
	private StorageQuery arranged;
	private StorageControls.Layout controls;
	private Font chipFont;
	private FontMetrics chipMetrics;
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

	/** @return the sort and filters; they outlive the window closing, so they last until RuneLite restarts */
	StorageQuery query()
	{
		return query;
	}

	/** @return the places in storage of the creatures shown, in display order */
	int[] shownPlaces()
	{
		return StorageView.places(entries);
	}

	/** @return the Sort button's box, for opening its menu below it */
	Rectangle sortBounds()
	{
		return controls == null ? new Rectangle() : controls.sort();
	}

	/** @return the Filter button's box, for opening its menu below it */
	Rectangle filterBounds()
	{
		return controls == null ? new Rectangle() : controls.filter();
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

	/**
	 * Ticks every creature that is shown now, which a filter may have cut down.
	 */
	void tickAllShown()
	{
		for (StorageView.Entry entry : entries)
			ticked.add(entry.place());
	}

	/**
	 * Sets the sort and filters, scrolls back to the top and drops the ticks of creatures that are no longer shown.
	 *
	 * @param next the new query
	 * @param storage the stored creatures now
	 */
	void setQuery(StorageQuery next, List<CreatureView> storage)
	{
		query = next;
		list.setOffset(0);
		arrange(storage);
	}

	/** Forgets Select mode and the ticks, as when the window closes. The sort and filters stay. */
	void reset()
	{
		selecting = false;
		ticked.clear();
		shown = null;
		source = null;
	}

	/**
	 * @param id a hit's id
	 * @return the place in the query a filter chip's label id names, or -1
	 */
	static int chip(String id)
	{
		return indexAfter(id, CHIP_PREFIX);
	}

	/**
	 * @param id a hit's id
	 * @return the place in the query a filter chip's cross id names, or -1
	 */
	static int chipRemove(String id)
	{
		return indexAfter(id, CHIPX_PREFIX);
	}

	private static int indexAfter(String id, String prefix)
	{
		if (id == null || !id.startsWith(prefix))
			return NOT_ON_TEAM;

		try
		{
			return Integer.parseInt(id.substring(prefix.length()));
		}
		catch (NumberFormatException e)
		{
			return NOT_ON_TEAM;
		}
	}

	/**
	 * @param id a hit's id
	 * @return the place in storage a row's id names, or -1 when it is not a row's id
	 */
	static int place(String id)
	{
		return indexAfter(id, ROW_PREFIX);
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

		if (source != storage || arranged != query)
			arrange(storage);

		WindowStyle.Metrics m = style.metrics();
		panel = new Rectangle(body);
		controls = StorageControls.layout(body, style, chipMetrics(style), selecting, false, query);
		int top = controls.listTop();
		Rectangle viewport = new Rectangle(body.x + m.pad / 2, top, body.width - m.pad,
			Math.max(0, body.y + body.height - top - m.pad / 2));
		list.layout(viewport, style.px(ROW_UNITS), entries.size(), style);
	}

	private FontMetrics chipMetrics(WindowStyle style)
	{
		Font font = style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		if (chipMetrics == null || !font.equals(chipFont))
		{
			chipMetrics = WindowPages.metrics(font);
			chipFont = font;
		}

		return chipMetrics;
	}

	private void arrange(List<CreatureView> storage)
	{
		entries = StorageView.arrange(storage, query);
		source = storage;
		arranged = query;
		List<Integer> places = new ArrayList<>();
		for (int place : StorageView.places(entries))
			places.add(place);

		ticked.retainAll(places);
	}

	/**
	 * Lists the page's hits: the buttons and chips, then every row in view.
	 *
	 * @param firstOrder the focus order of the first hit
	 * @return the hits
	 */
	List<Hit> hits(int firstOrder)
	{
		int order = firstOrder;
		List<Hit> hits = new ArrayList<>();
		String selectTip = selecting ? "Stop selecting and go back to moving creatures"
			: "Tick several creatures to release them together";
		hits.add(new Hit(ID_SELECT, Hit.Kind.BUTTON, controls.select(), order++, true, selectTip));
		hits.add(new Hit(ID_FILTER, Hit.Kind.BUTTON, controls.filter(), order++, true, FILTER_TIP));
		hits.add(new Hit(ID_SORT, Hit.Kind.BUTTON, controls.sort(), order++, true, sortTip()));
		if (selecting)
		{
			String tickTip = entries.isEmpty() ? "No creature is shown to tick"
				: "Tick every creature you can see. Creatures a filter hides stay unticked.";
			hits.add(new Hit(ID_TICK_ALL, Hit.Kind.BUTTON, controls.tickAll(), order++, !entries.isEmpty(), tickTip));
			String releaseTip = ticked.isEmpty() ? "Tick the creatures to release first"
				: "Release the ticked creatures. You are asked first.";
			hits.add(new Hit(ID_RELEASE, Hit.Kind.BUTTON, controls.release(), order++, !ticked.isEmpty(),
				releaseTip));
		}

		for (StorageControls.Chip chip : controls.chips())
		{
			List<StorageQuery.Criterion> criteria = query.criteria();
			boolean hidden = criteria.get(chip.index()).hidden();
			String tip = hidden ? "Hiding creatures that match. Click to show only them."
				: "Showing only creatures that match. Click to hide them instead.";
			hits.add(new Hit(CHIP_PREFIX + chip.index(), Hit.Kind.BUTTON, chip.label(), order++, true, tip));
			hits.add(new Hit(CHIPX_PREFIX + chip.index(), Hit.Kind.BUTTON, chip.remove(), order++, true,
				"Remove this filter"));
		}

		if (!controls.clear().isEmpty())
			hits.add(new Hit(ID_CLEAR, Hit.Kind.BUTTON, controls.clear(), order++, true, "Remove every filter"));

		for (int i = list.firstVisible(); i <= list.lastVisible() && i < entries.size(); i++)
		{
			Rectangle box = list.rowBounds(i);
			StorageView.Entry entry = entries.get(i);
			if (box != null)
				hits.add(new Hit(rowId(entry.place()), Hit.Kind.ROW, box, order++, true, tooltip(entry.view())));
		}

		return hits;
	}

	private String sortTip()
	{
		StorageQuery.Sort sort = query.sort();
		String words = sort.directionWords(query.descending());
		return SORT_TIP + (words.isEmpty() ? "" : "\nNow: " + sort.label() + ", " + words + ".");
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
		int total = storage.size();
		String title = query.active() ? "Showing " + entries.size() + " of " + total
			: "Stored  " + total + " / " + SaveData.STORAGE_SIZE;
		text.draw(g, title, controls.heading(), regular, ThemeColor.TEXT, PageText.Align.LEFT);
		st.button(g, rect(controls.select()), selecting ? "Done" : "Select", false, false, ctx.hoverOf(ID_SELECT),
			ctx.pressOf(ID_SELECT));
		st.button(g, rect(controls.filter()), query.active() ? "Filter (" + query.criteria().size() + ")" : "Filter",
			query.active(), false, ctx.hoverOf(ID_FILTER), ctx.pressOf(ID_FILTER));
		st.button(g, rect(controls.sort()), "Sort: " + query.sort().label(), false, false, ctx.hoverOf(ID_SORT),
			ctx.pressOf(ID_SORT));
		if (selecting)
		{
			st.button(g, rect(controls.tickAll()), "Tick all shown", false, entries.isEmpty(),
				ctx.hoverOf(ID_TICK_ALL), ctx.pressOf(ID_TICK_ALL));
			st.button(g, rect(controls.release()), "Release selected (" + ticked.size() + ")", true,
				ticked.isEmpty(), ctx.hoverOf(ID_RELEASE), ctx.pressOf(ID_RELEASE));
		}

		if (query.active())
			paintChips(g, ctx, text, small);

		if (storage.isEmpty())
		{
			text.drawWrapped(g, EMPTY_TEXT, list.viewport(), small, ThemeColor.SUBTEXT);
			return;
		}

		if (!query.active())
			text.draw(g, selecting ? SELECT_HINT : HINT, controls.strip(), small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);

		if (entries.isEmpty())
		{
			text.drawWrapped(g, NO_MATCH_TEXT, list.viewport(), small, ThemeColor.SUBTEXT);
			return;
		}

		list.paint(g, st, (gr, index, box) -> row(gr, ctx, text, entries.get(index).view(),
			entries.get(index).place(), box, small));
	}

	private void paintChips(Graphics2D g, PageContext ctx, PageText text, Font small)
	{
		WindowStyle st = ctx.style;
		for (StorageControls.Chip chip : controls.chips())
		{
			StorageQuery.Criterion each = query.criteria().get(chip.index());
			String labelId = CHIP_PREFIX + chip.index();
			String crossId = CHIPX_PREFIX + chip.index();
			st.button(g, rect(chip.label()), each.label(), !each.hidden(), false, ctx.hoverOf(labelId),
				ctx.pressOf(labelId));
			st.button(g, rect(chip.remove()), "x", false, false, ctx.hoverOf(crossId), ctx.pressOf(crossId));
		}

		if (controls.moreCount() > 0 && !controls.more().isEmpty())
			text.draw(g, StorageControls.moreText(controls.moreCount()), controls.more(), small,
				ThemeColor.SUBTEXT, PageText.Align.LEFT);

		if (!controls.clear().isEmpty())
			st.button(g, rect(controls.clear()), "Clear", false, false, ctx.hoverOf(ID_CLEAR),
				ctx.pressOf(ID_CLEAR));
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
	 * A navigation key was pressed while a stored creature's row is highlighted: finds the row to highlight next and
	 * scrolls it into view. Up on the first shown row and Down on the last are left to the window, which moves on to
	 * the buttons or wraps.
	 *
	 * @param keyCode the key
	 * @param highlightedId the id of the highlighted hit, or null
	 * @return the place in storage of the row to highlight, or -1 when the page does not use the key
	 */
	int keyTarget(int keyCode, String highlightedId)
	{
		int at = displayIndex(place(highlightedId));
		if (at < 0)
			return NOT_ON_TEAM;

		int last = entries.size() - 1;
		int page = Math.max(1, list.viewport().height / Math.max(1, list.rowHeight()) - 1);
		int to;
		switch (keyCode)
		{
			case KeyEvent.VK_UP:
				to = at - 1;
				break;
			case KeyEvent.VK_DOWN:
				to = at + 1;
				break;
			case KeyEvent.VK_PAGE_UP:
				to = at - page;
				break;
			case KeyEvent.VK_PAGE_DOWN:
				to = at + page;
				break;
			case KeyEvent.VK_HOME:
				to = 0;
				break;
			case KeyEvent.VK_END:
				to = last;
				break;
			default:
				return NOT_ON_TEAM;
		}

		boolean step = keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;
		if (step && (to < 0 || to > last))
			return NOT_ON_TEAM;

		int target = Math.max(0, Math.min(last, to));
		list.ensureVisible(target);
		return entries.get(target).place();
	}

	private int displayIndex(int place)
	{
		if (place < 0)
			return NOT_ON_TEAM;

		for (int i = 0; i < entries.size(); i++)
		{
			if (entries.get(i).place() == place)
				return i;
		}

		return NOT_ON_TEAM;
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
