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
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Paints the Summary page of one creature, in two columns. Left: the header pill with the round style badge, the
 * chips, the image box and the Description box with its "Grows into..." line. Right: the Stats box, the Nature box
 * with the bond hearts under it, and the Moves box; the action buttons run along the foot. The words come from
 * {@link SummaryContent} and the boxes from {@link SummaryLayout}. In full motion the portrait grows out of the row
 * it was opened from, the stat bars fill one after another and the move pills pop in one by one; in the other modes
 * everything is simply there. All text is drawn through {@link PageText}. Holds no RuneLite types.
 */
final class SummaryPainter
{
	/** The tooltip of the portrait and name area. */
	static final String HEADER_TIP = "Right-click or press Enter for the creature's menu";

	/** The id of the tooltip over the Stats box, which says each stat's roll and net change. */
	static final String ID_STATS = "summary:stats";

	/** The id of the tooltip over the bond hearts. */
	static final String ID_BOND = "summary:bond";

	/** The heading of the Stats box. */
	static final String STATS_HEADING = "Stats";

	/** The heading of the Nature box. */
	static final String NATURE_HEADING = "Nature";

	/** The heading of the Moves box. */
	static final String MOVES_HEADING = "Moves";

	/** The heading of the Description box. */
	static final String DESCRIPTION_HEADING = "Description";

	private final PortraitCache pictures;
	private final PageText.Sink sink;

	/**
	 * Creates the painter.
	 *
	 * @param pictures where creature pictures are
	 * @param sink hears every string drawn, for tests
	 */
	SummaryPainter(PortraitCache pictures, PageText.Sink sink)
	{
		this.pictures = pictures;
		this.sink = sink;
	}

	/**
	 * Paints the page.
	 *
	 * @param g the graphics
	 * @param ctx the frame's facts
	 * @param layout where everything is
	 * @param content what the page says
	 * @param enabled for each button in the order of {@link SummaryLayout#labels()}, whether it can be pressed
	 * @param openedMs when the page opened, in milliseconds
	 * @param origin the portrait box of the row the page grew out of, or null to grow from the middle
	 */
	void paint(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, boolean[] enabled,
		long openedMs, Rectangle origin)
	{
		Shape savedClip = g.getClip();
		g.clipRect(ctx.body.x, ctx.body.y, ctx.body.width, ctx.body.height);
		long since = ctx.now - openedMs;
		long growMs = ctx.mode.moveMs(WindowTimes.SUMMARY_GROW);
		double grow = growMs <= 0 ? 1.0 : Easing.OUT_CUBIC.apply(Math.min(1.0, (double) since / growMs));
		left(g, ctx, layout, content, grow, origin);
		Composite saved = PageContext.fade(g, ctx.mode.fades() ? Math.max(0.2, grow) : 1.0);
		description(g, ctx, layout, content);
		stats(g, ctx, layout, content, since);
		nature(g, ctx, layout, content);
		bond(g, ctx, layout, content);
		moves(g, ctx, layout, content, since);
		g.setComposite(saved);
		buttons(g, ctx, layout, enabled);
		g.setClip(savedClip);
	}

	private void left(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, double grow,
		Rectangle origin)
	{
		WindowStyle st = ctx.style;
		Rectangle target = layout.portrait;
		if (target.width > 0 && target.height > 0)
		{
			Rectangle from = origin == null ? shrunk(target) : origin;
			double x = from.x + (target.x - from.x) * grow;
			double y = from.y + (target.y - from.y) * grow;
			double w = from.width + (target.width - from.width) * grow;
			double h = from.height + (target.height - from.height) * grow;
			double bob = Loop.wave(ctx.mode, ctx.now, WindowTimes.PORTRAIT_BOB, 0) * st.scale();
			PortraitFrame.paint(g, st, pictures, content.row(), new Rectangle2D.Double(x, y, w, h),
				st.metrics().slant / 2.0, bob, ctx.now, ctx.mode);
		}

		header(g, ctx, layout, content);
		chips(g, ctx, layout, content);
		if (ctx.focused && WindowPages.ID_SUMMARY_MENU.equals(ctx.highlight))
			st.focusRing(g, Slant.shape(new Rectangle2D.Double(layout.left.x, layout.left.y, layout.left.width,
				layout.left.height), st.metrics().slant / 2.0));
	}

	private static Rectangle shrunk(Rectangle target)
	{
		int w = target.width * 6 / 10;
		int h = target.height * 6 / 10;
		return new Rectangle(target.x + (target.width - w) / 2, target.y + (target.height - h) / 2, w, h);
	}

	private void header(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		Rectangle box = layout.header;
		if (box.height <= 0 || box.width <= 0)
			return;

		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		int d = box.height;
		RoundRectangle2D pill = new RoundRectangle2D.Double(box.x + d / 2.0, box.y, Math.max(0, box.width - d / 2.0),
			box.height, box.height, box.height);
		g.setColor(st.color(ThemeColor.PANEL_BOX));
		g.fill(pill);
		outline(g, st, pill);
		Ellipse2D badge = new Ellipse2D.Double(box.x, box.y, d, d);
		g.setColor(st.color(ThemeColor.CHIP_ACCENT));
		g.fill(badge);
		outline(g, st, badge);
		double icon = d * 0.62;
		g.setColor(st.color(ThemeColor.CHIP_ACCENT_TEXT));
		Icons.style(g, content.style(), box.x + (d - icon) / 2.0, box.y + (d - icon) / 2.0, icon);

		int pad = Math.max(3, st.metrics().pad / 2);
		int x0 = box.x + d + pad;
		int end = box.x + box.width - d / 2 - pad;
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		Font bold = st.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		FontMetrics bm = g.getFontMetrics(bold);
		int levelW = bm.stringWidth(content.levelBadge());
		int levelX = Math.max(x0, end - levelW);
		text.draw(g, content.levelBadge(), new Rectangle(levelX, box.y, Math.max(0, end - levelX), box.height), bold,
			ThemeColor.TEXT, PageText.Align.RIGHT);
		int icons = content.gender() == null ? 0 : Math.max(8, box.height / 2);
		int nameRoom = Math.max(0, levelX - pad - x0 - (icons > 0 ? icons + pad : 0));
		Font title = text.nameFont(g, content.title(), nameRoom, WindowFonts.BODY_UNITS);
		String shown = text.draw(g, content.title(), new Rectangle(x0, box.y, nameRoom, box.height), title,
			ThemeColor.TEXT, PageText.Align.LEFT);
		int x = x0 + g.getFontMetrics(title).stringWidth(shown) + pad;
		if (icons > 0 && x + icons <= levelX)
		{
			g.setColor(GenderColors.of(content.gender(), false));
			Icons.gender(g, content.gender(), x, box.y + (box.height - icons) / 2.0, icons);
			x += icons + pad;
		}

		if (!content.subtitle().isEmpty() && levelX - pad - x > 0)
			text.draw(g, content.subtitle(), new Rectangle(x, box.y, levelX - pad - x, box.height), small,
				ThemeColor.SUBTEXT, PageText.Align.LEFT);
	}

	private static void outline(Graphics2D g, WindowStyle st, Shape shape)
	{
		Stroke saved = g.getStroke();
		g.setStroke(new BasicStroke((float) Math.max(1.0, st.metrics().border)));
		g.setColor(st.color(ThemeColor.PANEL_BORDER));
		g.draw(shape);
		g.setStroke(saved);
	}

	private void chips(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		Rectangle box = layout.chips;
		WindowStyle st = ctx.style;
		int x = box.x + st.metrics().chipSlant;
		int gap = Math.max(2, st.metrics().pad / 3);
		for (SummaryContent.Chip chip : content.chipRow())
		{
			int w = st.chipWidth(g, chip.label());
			if (box.height < st.metrics().chipHeight || x + w > box.x + box.width)
				return;

			st.chip(g, x, box.y, chip.label(), chip.style());
			x += w + gap + st.metrics().chipSlant;
		}
	}

	/**
	 * Paints a box with its heading centred along the top and a line under it.
	 *
	 * @return whether the box has any room, so the caller paints its content
	 */
	private boolean frame(Graphics2D g, WindowStyle st, SummaryLayout layout, Rectangle box, String heading)
	{
		if (box.height < layout.headingH || box.width <= 0)
			return false;

		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		Font bold = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		new PageText(st, sink).draw(g, heading, layout.heading(box), bold, ThemeColor.TEXT, PageText.Align.CENTER);
		g.setColor(st.color(ThemeColor.PANEL_BORDER));
		g.fillRect(box.x, box.y + layout.headingH, box.width, Math.max(1, st.metrics().border / 2));
		return true;
	}

	private void stats(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, long since)
	{
		WindowStyle st = ctx.style;
		if (!frame(g, st, layout, layout.stats, STATS_HEADING))
			return;

		PageText text = new PageText(st, sink);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics sm = g.getFontMetrics(small);
		List<SummaryContent.BarLine> bars = content.bars();
		long fillMs = ctx.mode.moveMs(WindowTimes.SUMMARY_GROW);
		int labelW = sm.stringWidth("Attack");
		int valueW = Math.max(sm.stringWidth("100%"), sm.stringWidth("+5"));
		int gap = Math.max(2, layout.pad);
		for (int i = 0; i < bars.size() && i < SummaryLayout.BAR_ROWS; i++)
		{
			SummaryContent.BarLine line = bars.get(i);
			Rectangle row = layout.statRow(i);
			text.draw(g, line.label(), new Rectangle(row.x, row.y, labelW, row.height), small, ThemeColor.TEXT,
				PageText.Align.LEFT);
			Rectangle val = new Rectangle(row.x + row.width - valueW, row.y, valueW, row.height);
			text.draw(g, line.valueText(), val, small, ThemeColor.SUBTEXT, PageText.Align.RIGHT);
			int barX = row.x + labelW + gap;
			int barW = Math.max(0, val.x - gap - barX);
			double t = fillMs <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0,
				(double) (since - (long) i * WindowTimes.SUMMARY_STAGGER) / fillMs));
			int barH = Math.max(3, Math.min(st.metrics().bar * 3 / 4, row.height - 2));
			if (barW > 0)
				st.xpBar(g, new Rectangle2D.Double(barX, row.y + (row.height - barH) / 2.0, barW, barH),
					line.fraction() * Easing.OUT_CUBIC.apply(t));
		}
	}

	private void nature(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		WindowStyle st = ctx.style;
		Rectangle box = layout.nature;
		if (!frame(g, st, layout, box, NATURE_HEADING))
			return;

		PageText text = new PageText(st, sink);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		int lineH = g.getFontMetrics(small).getHeight();
		int y = box.y + layout.headingH + layout.pad;
		int w = box.width - 2 * layout.pad;
		text.draw(g, content.natureName(), new Rectangle(box.x + layout.pad, y, w, lineH), small, ThemeColor.TEXT,
			PageText.Align.LEFT);
		y += lineH;
		for (String effect : content.natureLines())
		{
			if (y + lineH > box.y + box.height)
				return;

			text.draw(g, effect, new Rectangle(box.x + layout.pad, y, w, lineH), small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);
			y += lineH;
		}
	}

	private void bond(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		Rectangle box = layout.bond;
		if (box.height < 6 || box.width <= 0)
			return;

		WindowStyle st = ctx.style;
		RoundRectangle2D pill = new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, box.height,
			box.height);
		g.setColor(st.color(ThemeColor.PANEL_BOX));
		g.fill(pill);
		outline(g, st, pill);
		int hearts = BondRules.MAX_HEARTS;
		int between = 2;
		int size = Math.max(4, Math.min(box.height - 2 * Math.max(1, layout.pad / 2),
			(box.width - 2 * layout.pad - (hearts - 1) * between) / hearts));
		int total = hearts * size + (hearts - 1) * between;
		int x = box.x + (box.width - total) / 2;
		int y = box.y + (box.height - size) / 2;
		boolean full = content.hearts() >= hearts;
		for (int i = 0; i < hearts; i++)
		{
			Shape heart = Icons.heart(x + i * (size + between), y, size);
			if (i < content.hearts())
			{
				g.setColor(st.color(full ? ThemeColor.GOLD : ThemeColor.HP_DANGER));
				g.fill(heart);
			}
			else
			{
				g.setColor(st.color(ThemeColor.PANEL_BORDER));
				g.draw(heart);
			}
		}
	}

	private void description(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		WindowStyle st = ctx.style;
		Rectangle box = layout.description;
		if (!frame(g, st, layout, box, DESCRIPTION_HEADING))
			return;

		PageText text = new PageText(st, sink);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		Rectangle body = layout.descriptionText();
		if (body.height > 0)
			text.drawWrapped(g, content.description(), body, small, ThemeColor.SUBTEXT);

		Rectangle grows = layout.growsLine();
		String right = content.evolvesWhen() != null ? content.evolvesWhen() : content.caught();
		int rightW = right.isEmpty() ? 0 : Math.min(g.getFontMetrics(small).stringWidth(right), grows.width / 2);
		int gap = rightW > 0 ? layout.pad : 0;
		text.draw(g, content.evolution(), new Rectangle(grows.x, grows.y, grows.width - rightW - gap, grows.height),
			small, ThemeColor.TEXT, PageText.Align.LEFT);
		if (rightW > 0)
			text.draw(g, right, new Rectangle(grows.x + grows.width - rightW, grows.y, rightW, grows.height), small,
				ThemeColor.SUBTEXT, PageText.Align.RIGHT);
	}

	private void moves(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, long since)
	{
		WindowStyle st = ctx.style;
		Rectangle box = layout.moves;
		if (!frame(g, st, layout, box, MOVES_HEADING))
			return;

		PageText text = new PageText(st, sink);
		if (ctx.focused && WindowPages.ID_MOVES.equals(ctx.highlight))
			st.focusRing(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));

		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		List<SummaryContent.MoveLine> moves = content.moves();
		long popMs = ctx.mode.moveMs(WindowTimes.SUMMARY_GROW);
		if (moves.isEmpty() && content.pendingMove() == null)
			text.draw(g, SummaryContent.NO_MOVES, layout.movePill(0), small, ThemeColor.SUBTEXT, PageText.Align.LEFT);

		for (int i = 0; i < SummaryContent.MAX_MOVES; i++)
		{
			boolean pendingPill = i == moves.size() && content.pendingMove() != null;
			if (i >= moves.size() && !pendingPill)
				continue;

			Rectangle pill = layout.movePill(i);
			double t = popMs <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0,
				(double) (since - (long) i * WindowTimes.SUMMARY_STAGGER) / popMs));
			AffineTransform at = g.getTransform();
			Composite saved = PageContext.fade(g, t);
			double pop = popMs <= 0 ? 1.0 : 0.8 + 0.2 * Easing.OUT_BACK.apply(t);
			g.translate(pill.getCenterX(), pill.getCenterY());
			g.scale(pop, pop);
			g.translate(-pill.getCenterX(), -pill.getCenterY());
			RoundRectangle2D shape = new RoundRectangle2D.Double(pill.x, pill.y, pill.width, pill.height, pill.height,
				pill.height);
			g.setColor(st.color(ThemeColor.CHIP));
			g.fill(shape);
			outline(g, st, shape);
			int icon = Math.max(4, pill.height * 6 / 10);
			int inset = Math.max(3, (pill.height - icon) / 2 + 2);
			double iconY = pill.y + (pill.height - icon) / 2.0;
			Rectangle name = new Rectangle(pill.x + inset + icon + inset, pill.y,
				Math.max(0, pill.width - 2 * inset - icon - inset), pill.height);
			if (pendingPill)
			{
				g.setColor(st.color(ThemeColor.GOLD));
				g.fill(Icons.star(pill.x + inset + icon / 2.0, iconY + icon / 2.0, icon / 2.0 + 1));
				text.draw(g, content.pendingMove(), name, regular, ThemeColor.TEXT, PageText.Align.LEFT);
			}
			else
			{
				SummaryContent.MoveLine move = moves.get(i);
				g.setColor(st.color(ThemeColor.CHIP_TEXT));
				Icons.style(g, move.style(), pill.x + inset, iconY, icon);
				text.draw(g, move.name(), name, regular, ThemeColor.CHIP_TEXT, PageText.Align.LEFT);
			}

			g.setComposite(saved);
			g.setTransform(at);
		}

		if (content.pendingMove() != null && moves.size() >= SummaryContent.MAX_MOVES)
		{
			double r = Math.max(4.0, g.getFontMetrics(regular).getHeight() / 3.0);
			g.setColor(st.color(ThemeColor.GOLD));
			g.fill(Icons.star(box.x + box.width - r - layout.pad, box.y + r + 1, r));
		}
	}

	private void buttons(Graphics2D g, PageContext ctx, SummaryLayout layout, boolean[] enabled)
	{
		WindowStyle st = ctx.style;
		String[] labels = layout.labels();
		for (int i = 0; i < labels.length; i++)
		{
			String id = WindowPages.summaryButtonId(i);
			Rectangle b = layout.buttons[i];
			Rectangle2D shape = new Rectangle2D.Double(b.x, b.y, b.width, b.height);
			st.button(g, shape, labels[i], i == 0, !enabled[i], ctx.hoverOf(id), ctx.pressOf(id));
			if (ctx.focused && id.equals(ctx.highlight))
				st.focusRing(g, Slant.shape(shape, st.metrics().chipSlant));
		}
	}
}
