/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The Tasks page: the day's tasks with the time to their reset, the weekly task, the streak, and the species
 * milestones in a scrolling list. The rows are only for reading and their tooltips; the milestone list scrolls with the
 * wheel and its bar. Holds no RuneLite types.
 */
final class TasksPage
{
	private static final double GAP_UNITS = 8;
	private static final double HEADING_UNITS = 30;
	private static final double TASK_UNITS = 54;
	private static final double MILESTONE_UNITS = 70;
	private static final double LINE_UNITS = 30;
	private static final double LEFT_SHARE = 0.5;
	private static final String TASK_PREFIX = "tasks:row:";
	private static final String MILESTONE_PREFIX = "tasks:milestone:";

	/** The id of the streak line's hit. */
	static final String ID_STREAK = "tasks:streak";

	/** The start of a task pin button's id, followed by the task's place in the rows. */
	static final String PIN_PREFIX = "tasks:pin:";

	private final PageText.Sink sink;
	private final ScrollList list = new ScrollList();
	private Rectangle left = new Rectangle();
	private Rectangle right = new Rectangle();
	private Rectangle[] taskRows = new Rectangle[0];
	private Rectangle[] headings = new Rectangle[0];
	private Rectangle divider = new Rectangle();
	private Rectangle streak = new Rectangle();
	private Rectangle nextLine = new Rectangle();
	private Rectangle nextBar = new Rectangle();
	private Rectangle milestoneHeading = new Rectangle();

	/**
	 * Creates the page.
	 *
	 * @param sink hears what text is drawn, for tests
	 */
	TasksPage(PageText.Sink sink)
	{
		this.sink = sink;
	}

	/** @return the line between the Daily and the Weekly tasks, empty when there is no weekly task */
	Rectangle divider()
	{
		return divider;
	}

	/** @return the milestone list, whose offset and bar the tests drive */
	ScrollList list()
	{
		return list;
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
		WindowStyle.Metrics m = style.metrics();
		int pad = m.pad;
		int gap = style.px(GAP_UNITS);
		int heading = style.px(HEADING_UNITS);
		int leftW = (int) Math.round((body.width - gap) * LEFT_SHARE);
		left = new Rectangle(body.x, body.y, leftW, body.height);
		right = new Rectangle(body.x + leftW + gap, body.y, body.width - leftW - gap, body.height);
		List<TaskView.Row> rows = panel.tasks() == null ? new ArrayList<>() : panel.tasks().rows();
		int rowH = style.px(TASK_UNITS);
		int y = left.y + pad / 2;
		headings = new Rectangle[] {new Rectangle(left.x + pad, y, left.width - 2 * pad, heading), new Rectangle()};
		y += heading;
		taskRows = new Rectangle[rows.size()];
		divider = new Rectangle();
		for (int i = 0; i < rows.size(); i++)
		{
			boolean weekly = i == rows.size() - 1 && rows.size() > TaskCalendar.DAILY_COUNT;
			if (weekly)
			{
				divider = new Rectangle(left.x + pad, y + gap / 2, left.width - 2 * pad, Math.max(1, m.border));
				y += gap;
				headings[1] = new Rectangle(left.x + pad, y, left.width - 2 * pad, heading);
				y += heading;
			}

			taskRows[i] = new Rectangle(left.x + pad, y, left.width - 2 * pad, rowH - gap / 2);
			y += rowH;
		}

		streak = new Rectangle(left.x + pad, Math.min(y + gap, left.y + left.height - style.px(LINE_UNITS) - pad / 2),
			left.width - 2 * pad, style.px(LINE_UNITS));
		milestoneHeading = new Rectangle(right.x + pad, right.y + pad / 2, right.width - 2 * pad, heading);
		int line = style.px(LINE_UNITS);
		nextLine = new Rectangle(right.x + pad, milestoneHeading.y + heading, right.width - 2 * pad, line);
		nextBar = new Rectangle(right.x + pad, nextLine.y + line, right.width - 2 * pad, Math.max(3, m.bar));
		int top = nextBar.y + nextBar.height + gap;
		Rectangle viewport = new Rectangle(right.x + pad / 2, top, right.width - pad,
			Math.max(0, right.y + right.height - top - pad / 2));
		List<MilestoneView.Row> milestones = panel.milestones().rows();
		list.layout(viewport, style.px(MILESTONE_UNITS), milestones.size(), style);
	}

	/**
	 * Lists the page's hits: a pin button for every task, then every row and the streak line for their tooltips.
	 *
	 * @param style the style the page was laid out in
	 * @param panel what the page shows
	 * @return the hits
	 */
	List<Hit> hits(WindowStyle style, PanelState panel)
	{
		List<Hit> hits = new ArrayList<>();
		List<TaskView.Row> rows = panel.tasks() == null ? new ArrayList<>() : panel.tasks().rows();
		int order = WindowShell.PAGE_ORDER;
		int used = panel.tracker().count();
		for (int i = 0; i < rows.size() && i < taskRows.length; i++)
		{
			TaskView.Row row = rows.get(i);
			String refusal = PinButton.refusal(row.pinned(), row.done(), used);
			Rectangle pin = PinButton.bounds(style, taskRows[i], taskRows[i].height / 2);
			hits.add(PinButton.hit(PIN_PREFIX + i, pin, order++, row.pinned(), refusal));
		}

		for (int i = 0; i < rows.size() && i < taskRows.length; i++)
		{
			Rectangle rest = PinButton.rest(style, taskRows[i], taskRows[i].height / 2);
			hits.add(new Hit(TASK_PREFIX + i, Hit.Kind.AREA, rest, -1, true, "Reward: " + rows.get(i).reward()));
		}

		if (panel.tasks() != null && !rows.isEmpty())
			hits.add(new Hit(ID_STREAK, Hit.Kind.AREA, streak, -1, true, streakTip()));

		List<MilestoneView.Row> milestones = panel.milestones().rows();
		for (int i = list.firstVisible(); i <= list.lastVisible() && i < milestones.size(); i++)
		{
			Rectangle box = list.rowBounds(i).intersection(list.viewport());
			if (!box.isEmpty())
				hits.add(new Hit(MILESTONE_PREFIX + i, Hit.Kind.AREA, box, -1, true, milestoneTip(milestones.get(i))));
		}

		return hits;
	}

	/**
	 * @return the streak line's tooltip: what days played pay
	 */
	static String streakTip()
	{
		return "Days played: days you caught something. The first catch of each day pays " + GoGame.STREAK_JARS
			+ " Bronze jars, and every " + TaskBoard.STREAK_EGG_EVERY + "th day played pays an Uncommon egg. "
			+ "Missing a day costs nothing.";
	}

	/**
	 * @param row a milestone
	 * @return what it pays, and a note when the reward waits for an egg slot
	 */
	static String milestoneTip(MilestoneView.Row row)
	{
		return row.reward() + (row.reached() && !row.paid() ? " (waiting for an egg slot)" : "");
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
		PageText text = new PageText(st, sink);
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		st.panel(g, rect(left), WindowDepth.Pane.TASKS_TASKS);
		st.panel(g, rect(right), WindowDepth.Pane.TASKS_MILESTONES);
		TaskView tasks = panel.tasks();
		List<TaskView.Row> rows = tasks == null ? new ArrayList<>() : tasks.rows();
		if (rows.isEmpty())
			text.draw(g, "Tasks appear once your save has loaded", headings[0], small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);
		else
			text.draw(g, "Tasks · resets in " + TaskView.duration(tasks.millisToReset()), headings[0], small,
				ThemeColor.SUBTEXT, PageText.Align.LEFT);

		if (divider.width > 0)
		{
			g.setColor(st.color(ThemeColor.ROW_BORDER));
			g.fill(rect(divider));
		}

		if (headings[1].width > 0)
			text.draw(g, "Weekly · resets in " + TaskView.duration(tasks.millisToWeeklyReset()), headings[1],
				small, ThemeColor.SUBTEXT, PageText.Align.LEFT);

		int used = panel.tracker().count();
		for (int i = 0; i < rows.size() && i < taskRows.length; i++)
			task(g, ctx, text, i, rows.get(i), small, used);

		if (!rows.isEmpty())
			text.draw(g, "Day " + tasks.streak() + " · egg in " + tasks.eggIn()
				+ (tasks.eggIn() == 1 ? " day" : " days"), streak, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);

		milestones(g, st, text, panel.milestones(), regular, small);
	}

	private void task(Graphics2D g, PageContext ctx, PageText text, int index, TaskView.Row row, Font small, int used)
	{
		WindowStyle st = ctx.style;
		int half = taskRows[index].height / 2;
		Rectangle box = PinButton.rest(st, taskRows[index], half);
		ThemeColor ink = row.done() ? ThemeColor.HP_FILL : ThemeColor.TEXT;
		String count = row.done() ? "Done" : row.count();
		int countW = Math.min(box.width / 3, g.getFontMetrics(small).stringWidth(count) + st.metrics().pad);
		text.draw(g, row.label(), new Rectangle(box.x, box.y, box.width - countW, half), small, ink,
			PageText.Align.LEFT);
		text.draw(g, count, new Rectangle(box.x + box.width - countW, box.y, countW, half), small,
			row.done() ? ThemeColor.HP_FILL : ThemeColor.SUBTEXT, PageText.Align.RIGHT);
		st.bar(g, new Rectangle2D.Double(box.x, box.y + half + 1, box.width, Math.max(3, st.metrics().bar / 2)),
			row.fraction(), row.done() ? ThemeColor.HP_FILL : ThemeColor.XP_FILL, ThemeColor.HP_TRACK);
		PinButton.paint(g, ctx, PinButton.bounds(st, taskRows[index], half), PIN_PREFIX + index, row.pinned(),
			PinButton.refusal(row.pinned(), row.done(), used) == null);
	}

	private void milestones(Graphics2D g, WindowStyle st, PageText text, MilestoneView view, Font regular, Font small)
	{
		text.draw(g, "Species milestones", milestoneHeading, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
		boolean any = view.nextLabel() != null;
		String label = any ? view.nextLabel() : view.rows().isEmpty() ? "No milestones yet"
			: "All species milestones reached";
		String count = any ? Milestones.count(view.nextCurrent()) + " / " + Milestones.count(view.nextTarget()) : "";
		int countW = count.isEmpty() ? 0 : g.getFontMetrics(small).stringWidth(count) + st.metrics().pad;
		text.draw(g, label, new Rectangle(nextLine.x, nextLine.y, nextLine.width - countW, nextLine.height), regular,
			ThemeColor.TEXT, PageText.Align.LEFT);
		text.draw(g, count, new Rectangle(nextLine.x + nextLine.width - countW, nextLine.y, countW, nextLine.height),
			small, ThemeColor.SUBTEXT, PageText.Align.RIGHT);
		st.bar(g, rect(nextBar), any ? (double) view.nextCurrent() / Math.max(1, view.nextTarget()) : 1.0,
			ThemeColor.XP_FILL, ThemeColor.HP_TRACK);
		List<MilestoneView.Row> rows = view.rows();
		list.paint(g, st, (gr, index, box) -> milestone(gr, st, text, rows.get(index), box, small));
	}

	private void milestone(Graphics2D g, WindowStyle st, PageText text, MilestoneView.Row row, Rectangle box,
		Font small)
	{
		int pad = st.metrics().pad;
		int bar = list.scrollable() ? st.px(ScrollList.BAR_UNITS) + pad / 2 : 0;
		int width = Math.max(0, box.width - 2 * pad - bar);
		int line = Math.max(1, (box.height - pad) / 3);
		ThemeColor ink = row.paid() ? ThemeColor.HP_FILL : ThemeColor.TEXT;
		String count = Milestones.count(row.current()) + " / " + Milestones.count(row.target());
		int countW = Math.min(width / 2, g.getFontMetrics(small).stringWidth(count) + pad);
		int x = box.x + pad;
		int y = box.y + pad / 2;
		text.draw(g, (row.paid() ? "✓ " : "") + row.label(), new Rectangle(x, y, width - countW, line), small,
			ink, PageText.Align.LEFT);
		text.draw(g, count, new Rectangle(x + width - countW, y, countW, line), small, ThemeColor.SUBTEXT,
			PageText.Align.RIGHT);
		st.bar(g, new Rectangle2D.Double(x, y + line + 1, width, Math.max(3, st.metrics().bar / 2)),
			(double) row.current() / Math.max(1, row.target()), row.paid() ? ThemeColor.HP_FILL : ThemeColor.XP_FILL,
			ThemeColor.HP_TRACK);
		text.draw(g, milestoneTip(row), new Rectangle(x, y + line + 1 + Math.max(3, st.metrics().bar / 2) + 1, width,
			line), small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
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
