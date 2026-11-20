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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The Collection page: the Creature Log. The left panel holds the record (caught, seen), the next log reward and
 * today's spawns; the right panel holds the filter button, a search box that narrows the list by name, the species
 * list in a {@link ScrollList} and a strip with the chosen species' details. Only the rows in view are painted, the
 * filtered list is rebuilt only when the log, the filter, the search text or the day's picks change, and unseen
 * species show as ??? with no menu. The list scrolls with the wheel, the bar and the keys. Holds no RuneLite types;
 * the wiki is opened through a callback.
 */
final class CollectionPage
{
	/** The start of every hit id of this page. */
	static final String PREFIX = "collection:";

	/** The id of the filter button. */
	static final String ID_FILTER = PREFIX + "filter";

	/** The id of the search box. */
	static final String ID_SEARCH = PREFIX + "search";

	/** The most characters the search box holds. */
	static final int SEARCH_LENGTH = 24;

	/** The id of the details strip. */
	static final String ID_DETAIL = PREFIX + "detail";

	/** The label of the menu item that opens the wiki. */
	static final String OPEN_WIKI = "Open wiki";

	private static final String ROW_PREFIX = PREFIX + "row:";
	private static final double GAP_UNITS = 8;
	private static final double HEADING_UNITS = 26;
	private static final double LINE_UNITS = 26;
	private static final double ROW_UNITS = 34;
	private static final double TEXT_UNITS = 24;
	private static final double LEFT_SHARE = 0.4;
	private static final double UNSEEN_ALPHA = 0.6;
	private static final int MIN_ROWS = 3;
	private static final int TODAY_ROWS = 5;

	private final PageText.Sink sink;
	private final ScrollList list = new ScrollList();
	private final List<LogEntry> shown = new ArrayList<>();
	private final TextBox search = new TextBox(SEARCH_LENGTH, text -> text, TextBox.system());
	private CollectionFilter filter = CollectionFilter.ALL;
	private List<LogEntry> source;
	private CollectionFilter filtered;
	private String searched = "";
	private long now;
	private Set<String> featured = Collections.emptySet();
	private String selectedKey;
	private int selected = -1;
	private Rectangle left = new Rectangle();
	private Rectangle right = new Rectangle();
	private Rectangle record = new Rectangle();
	private Rectangle recordBar = new Rectangle();
	private Rectangle reward = new Rectangle();
	private Rectangle todayHeading = new Rectangle();
	private Rectangle[] todayRows = new Rectangle[0];
	private Rectangle event = new Rectangle();
	private Rectangle night = new Rectangle();
	private Rectangle filterButton = new Rectangle();
	private Rectangle count = new Rectangle();
	private Rectangle searchBox = new Rectangle();
	private Rectangle detail = new Rectangle();

	/**
	 * Creates the page.
	 *
	 * @param sink hears what text is drawn, for tests
	 */
	CollectionPage(PageText.Sink sink)
	{
		this.sink = sink;
		search.placeholder("Search species");
		search.onSubmit(text -> search.focus(false, now));
		search.onCancel(() ->
		{
			search.setText("");
			search.focus(false, now);
		});
	}

	/** @return the search box, whose text narrows the list by species name */
	TextBox searchBox()
	{
		return search;
	}

	/** @return whether the search box has the keys, so typing goes to it and not to the window */
	boolean searchFocused()
	{
		return search.focused();
	}

	/**
	 * Takes the keys away from the search box. Its text stays.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void blurSearch(long nowMs)
	{
		search.focus(false, nowMs);
	}

	/**
	 * Gives a key to the search box.
	 *
	 * @param keyCode the key
	 * @param modifiers the modifiers
	 * @param nowMs the time in milliseconds
	 * @return whether the box used it
	 */
	boolean searchKey(int keyCode, int modifiers, long nowMs)
	{
		return search.onKeyPressed(keyCode, modifiers, nowMs);
	}

	/**
	 * Gives a typed character to the search box.
	 *
	 * @param c the character
	 * @param modifiers the modifiers
	 * @param nowMs the time in milliseconds
	 */
	void searchTyped(char c, int modifiers, long nowMs)
	{
		search.onKeyTyped(c, modifiers, nowMs);
	}

	/** @return the species list, whose offset and bar the tests drive */
	ScrollList list()
	{
		return list;
	}

	/** @return the filter now on */
	CollectionFilter filter()
	{
		return filter;
	}

	/** @return how many species the list shows under the filter */
	int shownCount()
	{
		return shown.size();
	}

	/** @return the chosen species, or null when none is chosen or it is filtered out */
	LogEntry selectedEntry()
	{
		return selected >= 0 && selected < shown.size() ? shown.get(selected) : null;
	}

	/**
	 * @param index a place in the shown list
	 * @return its hit id
	 */
	static String rowId(int index)
	{
		return ROW_PREFIX + index;
	}

	/**
	 * @param id a hit's id
	 * @return the place in the shown list it names, or -1 when it is not a species row's id
	 */
	static int rowIndex(String id)
	{
		if (id == null || !id.startsWith(ROW_PREFIX))
			return -1;

		try
		{
			return Integer.parseInt(id.substring(ROW_PREFIX.length()));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/**
	 * Works out where everything sits. Call it each frame before painting and before the hits are asked for.
	 *
	 * @param body the page area
	 * @param style the style
	 * @param panel what the page shows
	 */
	void layout(Rectangle body, WindowStyle style, PanelState panel)
	{
		sync(panel);
		int pad = style.metrics().pad;
		int gap = style.px(GAP_UNITS);
		int leftW = (int) Math.round((body.width - gap) * LEFT_SHARE);
		left = new Rectangle(body.x, body.y, leftW, body.height);
		right = new Rectangle(body.x + leftW + gap, body.y, body.width - leftW - gap, body.height);
		layoutLeft(style, panel);
		int line = style.px(LINE_UNITS);
		int x = right.x + pad;
		int w = right.width - 2 * pad;
		int y = right.y + pad / 2;
		int buttonW = Math.min(w, Math.max(w / 2, style.px(120)));
		filterButton = new Rectangle(x, y, buttonW, line);
		count = new Rectangle(x + buttonW + pad, y, Math.max(0, w - buttonW - pad), line);
		int searchY = y + line + gap / 2;
		searchBox = new Rectangle(x, searchY, w, line);
		int top = searchY + line + gap / 2;
		int bottom = right.y + right.height - pad / 2;
		int rowH = style.px(ROW_UNITS);
		int detailH = pad + 3 * line;
		boolean room = bottom - top - detailH >= MIN_ROWS * rowH;
		boolean showDetail = selectedEntry() != null && CollectionFilter.known(selectedEntry()) && room;
		detail = showDetail ? new Rectangle(x, bottom - detailH, w, detailH) : new Rectangle();
		int listBottom = showDetail ? detail.y - gap / 2 : bottom;
		list.layout(new Rectangle(right.x + pad / 2, top, right.width - pad, Math.max(0, listBottom - top)), rowH,
			shown.size(), style);
	}

	private void layoutLeft(WindowStyle style, PanelState panel)
	{
		int pad = style.metrics().pad;
		int gap = style.px(GAP_UNITS);
		int line = style.px(LINE_UNITS);
		int x = left.x + pad;
		int w = left.width - 2 * pad;
		int limit = left.y + left.height - pad / 2;
		int y = left.y + pad / 2;
		record = new Rectangle(x, y, w, line);
		y += line;
		recordBar = new Rectangle(x, y, w, Math.max(3, style.metrics().bar));
		y += recordBar.height + gap;
		reward = rewardText(panel).isEmpty() ? new Rectangle() : new Rectangle(x, y, w, line);
		y += reward.isEmpty() ? 0 : line + gap / 2;
		TodayView today = panel.today();
		todayHeading = new Rectangle();
		todayRows = new Rectangle[0];
		event = new Rectangle();
		night = new Rectangle();
		if (!today.active())
			return;

		todayHeading = fit(x, y, w, style.px(HEADING_UNITS), limit);
		y += style.px(HEADING_UNITS);
		int picks = Math.min(TODAY_ROWS, today.featured().size());
		todayRows = new Rectangle[picks];
		for (int i = 0; i < picks; i++)
		{
			todayRows[i] = fit(x, y, w, line, limit);
			y += line;
		}

		event = today.event() == null ? new Rectangle() : fit(x, y, w, line, limit);
		y += today.event() == null ? 0 : line;
		night = today.night() ? fit(x, y, w, line, limit) : new Rectangle();
	}

	private static Rectangle fit(int x, int y, int w, int h, int limit)
	{
		return y + h <= limit ? new Rectangle(x, y, w, h) : new Rectangle();
	}

	/**
	 * Rebuilds the shown list when the log, the filter or the day's picks changed. The chosen species stays chosen
	 * while it stays in the list.
	 */
	private void sync(PanelState panel)
	{
		Set<String> keys = panel.today().featuredKeys();
		String typed = search.text().trim();
		if (panel.log() == source && filter == filtered && keys.equals(featured) && typed.equals(searched))
			return;

		if (!typed.equals(searched))
			list.setOffset(0);

		searched = typed;
		source = panel.log();
		filtered = filter;
		featured = keys;
		shown.clear();
		selected = -1;
		for (LogEntry entry : source)
		{
			if (filter.matches(entry, featured) && CollectionFilter.matchesText(entry, searched))
				shown.add(entry);
		}

		if (selectedKey == null)
			return;

		for (int i = 0; i < shown.size(); i++)
		{
			if (Species.keyFor(shown.get(i).name()).equals(selectedKey))
			{
				selected = i;
				return;
			}
		}
	}

	/**
	 * Lists the page's hits: the filter button, the search box, every visible species row and the detail strip.
	 *
	 * @return the hits
	 */
	List<Hit> hits()
	{
		List<Hit> hits = new ArrayList<>();
		CollectionFilter after = filter.next();
		String next = after.label().toLowerCase(Locale.ROOT);
		hits.add(new Hit(ID_FILTER, Hit.Kind.BUTTON, filterButton, WindowShell.PAGE_ORDER, true,
			"Show " + next + " species"));
		hits.add(new Hit(ID_SEARCH, Hit.Kind.BUTTON, searchBox, WindowShell.PAGE_ORDER, true,
			"Search species by name"));
		int last = Math.min(list.lastVisible(), shown.size() - 1);
		for (int i = list.firstVisible(); i <= last; i++)
		{
			Rectangle box = list.rowBounds(i);
			if (box != null)
				hits.add(new Hit(rowId(i), Hit.Kind.ROW, box, -1, true, tip(shown.get(i))));
		}

		LogEntry entry = selectedEntry();
		if (!detail.isEmpty() && entry != null)
			hits.add(new Hit(ID_DETAIL, Hit.Kind.AREA, detail, -1, true, detailTip(entry)));

		return hits;
	}

	/**
	 * @param entry a species
	 * @return the tooltip of its row: level, style and rarity, and a note for a quest-only or gilded one; empty while
	 *         the species is unknown
	 */
	static String tip(LogEntry entry)
	{
		if (!CollectionFilter.known(entry))
			return "";

		String tip = "Lv " + entry.speciesLevel() + " · " + entry.type().label() + " · "
			+ entry.rarity().label();
		if (entry.retired())
			tip += " · quest only, no longer catchable, not in the total";

		return entry.gilded() ? tip + " · gilded owned" : tip;
	}

	/**
	 * @param entry a known species
	 * @return the details strip's tooltip: the whole examine text and the evolution line
	 */
	static String detailTip(LogEntry entry)
	{
		StringBuilder text = new StringBuilder(entry.name());
		if (entry.examine() != null)
			text.append('\n').append(entry.examine());

		if (!entry.lineText().isEmpty())
			text.append('\n').append(entry.lineText());

		return text.toString();
	}

	/**
	 * @param panel what the page shows
	 * @return the next log reward as one line, or an empty text when the player has no milestones
	 */
	static String rewardText(PanelState panel)
	{
		MilestoneView view = panel.milestones();
		if (view.rows().isEmpty())
			return "";

		if (view.nextLabel() == null)
			return "All log rewards earned";

		for (MilestoneView.Row row : view.rows())
		{
			if (!row.paid() && row.label().equals(view.nextLabel()))
				return "Next reward: " + row.reward() + " at " + view.nextLabel();
		}

		return "Next: " + view.nextLabel();
	}

	/**
	 * Handles a press on one of the page's hits.
	 *
	 * @param id the hit's id
	 */
	void press(String id)
	{
		if (ID_FILTER.equals(id))
		{
			filter = filter.next();
			list.setOffset(0);
			return;
		}

		if (ID_SEARCH.equals(id))
		{
			search.focus(true, now);
			return;
		}

		int index = rowIndex(id);
		if (index >= 0 && index < shown.size())
			choose(index);
	}

	private void choose(int index)
	{
		selected = index;
		selectedKey = Species.keyFor(shown.get(index).name());
		list.ensureVisible(index);
	}

	/**
	 * Gives the items of a species' menu.
	 *
	 * @param entry the species
	 * @param openWiki called with the species name when Open wiki is chosen
	 * @return the items, or an empty list for a species the player has not met
	 */
	static List<ContextMenu.Item> menuItems(LogEntry entry, Consumer<String> openWiki)
	{
		if (!CollectionFilter.known(entry))
			return Collections.emptyList();

		return Collections.singletonList(ContextMenu.Item.of(OPEN_WIKI, () -> openWiki.accept(entry.name())));
	}

	/**
	 * @param id a hit's id
	 * @return the species whose row it is, or null
	 */
	LogEntry entryOf(String id)
	{
		int index = rowIndex(id);
		if (index < 0 || index >= shown.size())
			return null;

		choose(index);
		return shown.get(index);
	}

	/** @return the middle of the chosen row's visible part, for a menu opened from the keys, or null for none */
	Rectangle chosenRowBounds()
	{
		return selected < 0 ? null : list.rowBounds(selected);
	}

	/**
	 * A key was pressed over the page. Up, Down, Page Up, Page Down, Home and End move the chosen species and keep
	 * it in view.
	 *
	 * @param keyCode the key
	 * @return whether the page used it
	 */
	boolean key(int keyCode)
	{
		if (shown.isEmpty())
			return false;

		int page = Math.max(1, list.viewport().height / Math.max(1, list.rowHeight()) - 1);
		int from = selected < 0 ? 0 : selected;
		int to;
		switch (keyCode)
		{
			case KeyEvent.VK_UP:
				to = selected < 0 ? 0 : from - 1;
				break;
			case KeyEvent.VK_DOWN:
				to = selected < 0 ? 0 : from + 1;
				break;
			case KeyEvent.VK_PAGE_UP:
				to = from - page;
				break;
			case KeyEvent.VK_PAGE_DOWN:
				to = from + page;
				break;
			case KeyEvent.VK_HOME:
				to = 0;
				break;
			case KeyEvent.VK_END:
				to = shown.size() - 1;
				break;
			default:
				return false;
		}

		choose(Math.max(0, Math.min(shown.size() - 1, to)));
		return true;
	}

	/**
	 * Paints the page.
	 *
	 * @param g the graphics, with (0, 0) at the window's corner
	 * @param ctx what the frame needs
	 * @param panel what the page shows
	 */
	void paint(Graphics2D g, PageContext ctx, PanelState panel)
	{
		WindowStyle st = ctx.style;
		now = ctx.now;
		PageText text = new PageText(st, sink);
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		st.panel(g, rect(left));
		st.panel(g, rect(right));
		paintLeft(g, st, text, panel, regular, small);
		paintFilter(g, ctx, text, small);
		paintSearch(g, ctx);
		list.paint(g, st, (gr, index, box) -> row(gr, ctx, text, index, box, small));
		LogEntry entry = selectedEntry();
		if (!detail.isEmpty() && entry != null)
			paintDetail(g, st, text, entry, regular, small);
	}

	private void paintLeft(Graphics2D g, WindowStyle st, PageText text, PanelState panel, Font regular, Font small)
	{
		int seenW = Math.min(record.width / 2, g.getFontMetrics(small).stringWidth("Seen " + panel.seenSpecies())
			+ st.metrics().pad);
		text.draw(g, "Caught " + Milestones.count(panel.caughtSpecies()) + " / "
			+ Milestones.count(panel.totalSpecies()), new Rectangle(record.x, record.y, record.width - seenW,
			record.height), regular, ThemeColor.TEXT, PageText.Align.LEFT);
		text.draw(g, "Seen " + Milestones.count(panel.seenSpecies()), new Rectangle(record.x + record.width - seenW,
			record.y, seenW, record.height), small, ThemeColor.SUBTEXT, PageText.Align.RIGHT);
		st.bar(g, rect(recordBar), (double) panel.caughtSpecies() / Math.max(1, panel.totalSpecies()),
			ThemeColor.XP_FILL, ThemeColor.HP_TRACK);
		if (!reward.isEmpty())
			text.draw(g, rewardText(panel), reward, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);

		TodayView today = panel.today();
		if (!today.active() || todayHeading.isEmpty())
			return;

		text.draw(g, "Today · resets in " + today.resetText(), todayHeading, small, ThemeColor.SUBTEXT,
			PageText.Align.LEFT);
		for (int i = 0; i < todayRows.length; i++)
		{
			if (todayRows[i].isEmpty())
				continue;

			TodayView.Pick pick = today.featured().get(i);
			Rectangle r = todayRows[i];
			int swatch = swatch(g, st, r, pick.rarity());
			int labelW = Math.min(r.width / 2, g.getFontMetrics(small).stringWidth(pick.rarity().label())
				+ st.metrics().pad);
			text.draw(g, pick.name(), new Rectangle(r.x + swatch, r.y, r.width - swatch - labelW, r.height), small,
				ThemeColor.TEXT, PageText.Align.LEFT);
			text.draw(g, pick.rarity().label(), new Rectangle(r.x + r.width - labelW, r.y, labelW, r.height), small,
				ThemeColor.SUBTEXT, PageText.Align.RIGHT);
		}

		if (!event.isEmpty())
			text.draw(g, today.event(), event, small, ThemeColor.ACCENT, PageText.Align.LEFT);

		if (!night.isEmpty())
			text.draw(g, today.nightLine(), night, small, ThemeColor.ACCENT, PageText.Align.LEFT);
	}

	private static int swatch(Graphics2D g, WindowStyle st, Rectangle row, Rarity rarity)
	{
		int size = Math.max(4, st.px(8));
		g.setColor(rarity.color());
		g.fillRect(row.x, row.y + (row.height - size) / 2, size, size);
		return size + st.metrics().pad / 2 + 1;
	}

	private void paintFilter(Graphics2D g, PageContext ctx, PageText text, Font small)
	{
		WindowStyle st = ctx.style;
		Shape shape = st.row(g, rect(filterButton), false, ctx.hoverOf(ID_FILTER), ctx.pressOf(ID_FILTER));
		if (ctx.focused && ID_FILTER.equals(ctx.highlight))
			st.focusRing(g, shape);

		int pad = st.metrics().pad;
		text.draw(g, "Show: " + filter.label(), new Rectangle(filterButton.x + pad, filterButton.y,
			filterButton.width - 2 * pad, filterButton.height), small, ThemeColor.ROW_TEXT, PageText.Align.CENTER);
		text.draw(g, Milestones.count(shown.size()) + " species", count, small, ThemeColor.SUBTEXT,
			PageText.Align.RIGHT);
	}

	private void paintSearch(Graphics2D g, PageContext ctx)
	{
		search.paint(g, ctx.style, searchBox, ctx.mode, ctx.now);
		if (!search.focused() && ctx.focused && ID_SEARCH.equals(ctx.highlight))
			ctx.style.focusRing(g, ctx.style.outline(WindowStyle.Kind.FIELD, rect(searchBox)));
	}

	private void row(Graphics2D g, PageContext ctx, PageText text, int index, Rectangle box, Font small)
	{
		WindowStyle st = ctx.style;
		LogEntry entry = shown.get(index);
		String id = rowId(index);
		boolean chosen = index == selected;
		Rectangle inner = new Rectangle(box.x + 1, box.y + 1, box.width - 2, box.height - 2);
		Shape shape = st.row(g, rect(inner), chosen, ctx.hoverOf(id), ctx.pressOf(id));
		if (chosen && ctx.focused)
			st.focusRing(g, shape);

		int pad = st.metrics().pad;
		int textH = Math.min(inner.height, st.px(TEXT_UNITS));
		Rectangle line = new Rectangle(inner.x + pad, inner.y + (inner.height - textH) / 2, inner.width - 2 * pad,
			textH);
		if (!list.viewport().contains(line))
			return;

		boolean known = CollectionFilter.known(entry);
		int swatchW = known ? swatch(g, st, line, entry.rarity()) : 0;
		String right = entry.caught() ? (entry.retired() ? "quest only " : "") + "×" + entry.caughtCount()
			: entry.seen() ? "seen" : entry.rarity().label();
		int rightW = Math.min(line.width / 2, g.getFontMetrics(small).stringWidth(right) + pad);
		boolean star = featured.contains(Species.keyFor(entry.name()));
		String name = (star ? "* " : "") + (known ? entry.name() : "???");
		Composite saved = g.getComposite();
		if (!known)
			g.setComposite(AlphaComposite.SrcOver.derive((float) UNSEEN_ALPHA));

		text.draw(g, name, new Rectangle(line.x + swatchW, line.y, line.width - swatchW - rightW, line.height),
			small, ThemeColor.ROW_TEXT, PageText.Align.LEFT);
		text.draw(g, right, new Rectangle(line.x + line.width - rightW, line.y, rightW, line.height), small,
			entry.gilded() ? ThemeColor.GOLD : ThemeColor.ROW_TEXT, PageText.Align.RIGHT);
		g.setComposite(saved);
	}

	private void paintDetail(Graphics2D g, WindowStyle st, PageText text, LogEntry entry, Font regular, Font small)
	{
		st.panel(g, rect(detail));
		int pad = st.metrics().pad;
		int line = st.px(LINE_UNITS);
		int x = detail.x + pad;
		int w = detail.width - 2 * pad;
		int y = detail.y + pad / 2;
		String caught = entry.recordText();
		int recordW = Math.min(w / 2, g.getFontMetrics(small).stringWidth(caught) + pad);
		text.draw(g, entry.name(), new Rectangle(x, y, w - recordW, line), regular, ThemeColor.TEXT,
			PageText.Align.LEFT);
		text.draw(g, caught, new Rectangle(x + w - recordW, y, recordW, line), small,
			entry.gilded() ? ThemeColor.GOLD : ThemeColor.SUBTEXT, PageText.Align.RIGHT);
		if (entry.examine() != null)
			text.draw(g, entry.examine(), new Rectangle(x, y + line, w, line), small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);

		if (!entry.lineText().isEmpty())
			text.draw(g, entry.lineText(), new Rectangle(x, y + 2 * line, w, line), small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);
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
	 * @return whether the press landed on the search box or the list's scroll bar
	 */
	boolean pressed(int x, int y)
	{
		if (search.onMousePressed(x, y, MouseEvent.BUTTON1, 0, now))
			return true;

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
		search.onMouseDragged(x);
		list.onDrag(x, y);
	}

	/** @return whether the press became a scroll, so the release is not a click */
	boolean released()
	{
		search.onMouseReleased();
		return list.onRelease();
	}

	private static Rectangle2D rect(Rectangle r)
	{
		return new Rectangle2D.Double(r.x, r.y, r.width, r.height);
	}
}
