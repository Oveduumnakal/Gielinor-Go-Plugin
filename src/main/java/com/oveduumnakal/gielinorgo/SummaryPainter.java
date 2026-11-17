/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Paints the Summary page of one creature: the portrait, who it is and its chips on the left; the stats, nature,
 * description, moves, bond hearts and evolution hint on the right; the action buttons along the foot. The words come
 * from {@link SummaryContent} and the boxes from {@link SummaryLayout}. In full motion the portrait grows out of the
 * row it was opened from, the stat bars fill one after another and the move cells pop in one by one; in the other
 * modes everything is simply there. All text is drawn through {@link PageText}. Holds no RuneLite types.
 */
final class SummaryPainter
{
	/** The tooltip of the portrait and name area. */
	static final String HEADER_TIP = "Right-click or press Enter for the creature's menu";

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
	 * @param enabled for each button in the order of {@link SummaryLayout#BUTTONS}, whether it can be pressed
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
		stats(g, ctx, layout, content, since);
		nature(g, ctx, layout, content);
		description(g, ctx, layout, content);
		moves(g, ctx, layout, content, since);
		bond(g, ctx, layout, content);
		g.setComposite(saved);
		buttons(g, ctx, layout, enabled);
		g.setClip(savedClip);
	}

	private void left(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, double grow,
		Rectangle origin)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Rectangle target = layout.portrait;
		Rectangle from = origin == null ? shrunk(target) : origin;
		double x = from.x + (target.x - from.x) * grow;
		double y = from.y + (target.y - from.y) * grow;
		double w = from.width + (target.width - from.width) * grow;
		double h = from.height + (target.height - from.height) * grow;
		double bob = Loop.wave(ctx.mode, ctx.now, WindowTimes.PORTRAIT_BOB, 0) * st.scale();
		PortraitFrame.paint(g, st, pictures, content.row(), new Rectangle2D.Double(x, y, w, h),
			st.metrics().slant / 2.0, bob, ctx.now, ctx.mode);
		if (ctx.focused && WindowPages.ID_SUMMARY_MENU.equals(ctx.highlight))
			st.focusRing(g, Slant.shape(new Rectangle2D.Double(layout.left.x, layout.left.y, layout.left.width,
				layout.left.height), st.metrics().slant / 2.0));

		Rectangle id = layout.identity;
		Font title = st.font(WindowFonts.Face.BOLD, WindowFonts.TITLE_UNITS);
		FontMetrics tm = g.getFontMetrics(title);
		int top = id.y;
		Rectangle titleBox = new Rectangle(id.x, top, id.width, tm.getHeight());
		text.draw(g, content.title(), titleBox, text.nameFont(g, content.title(), id.width, WindowFonts.TITLE_UNITS),
			ThemeColor.TEXT, PageText.Align.LEFT);
		top += tm.getHeight();
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics sm = g.getFontMetrics(small);
		if (!content.subtitle().isEmpty())
		{
			text.draw(g, content.subtitle(), new Rectangle(id.x, top, id.width, sm.getHeight()), small,
				ThemeColor.SUBTEXT, PageText.Align.LEFT);
			top += sm.getHeight();
		}

		int icon = sm.getHeight();
		int gx = id.x;
		g.setColor(st.color(ThemeColor.TEXT));
		if (content.gender() != null)
		{
			Icons.gender(g, content.gender(), gx, top, icon);
			gx += icon + st.metrics().pad / 2;
		}

		Rectangle levelBox = new Rectangle(gx, top, Math.max(0, id.x + id.width - gx), icon);
		text.draw(g, content.level(), levelBox, small, ThemeColor.TEXT, PageText.Align.LEFT);
		top += icon + st.metrics().pad / 3;
		chips(g, ctx, layout, content, top);
	}

	private static Rectangle shrunk(Rectangle target)
	{
		int w = target.width * 6 / 10;
		int h = target.height * 6 / 10;
		return new Rectangle(target.x + (target.width - w) / 2, target.y + (target.height - h) / 2, w, h);
	}

	private void chips(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, int top)
	{
		WindowStyle st = ctx.style;
		Rectangle id = layout.identity;
		int x = id.x;
		int y = top;
		int gap = Math.max(2, st.metrics().pad / 3);
		int slant = st.metrics().chipSlant;
		int h = st.metrics().chipHeight;
		for (SummaryContent.Chip chip : content.chips())
		{
			int w = st.chipWidth(g, chip.label());
			if (x + w > id.x + id.width && x > id.x)
			{
				x = id.x;
				y += h + gap;
			}

			if (y + h > id.y + id.height || w > id.width)
				return;

			st.chip(g, x, y, chip.label(), chip.style());
			x += w + gap + slant;
		}

		String rating = content.rating();
		int w = st.chipWidth(g, rating);
		if (x + w > id.x + id.width && x > id.x)
		{
			x = id.x;
			y += h + gap;
		}

		if (y + h <= id.y + id.height && w <= id.width)
			st.chip(g, x, y, rating, false);
	}

	private void stats(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, long since)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Rectangle box = layout.stats;
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics sm = g.getFontMetrics(small);
		List<SummaryContent.StatLine> stats = content.stats();
		long fillMs = ctx.mode.moveMs(WindowTimes.SUMMARY_GROW);
		for (int i = 0; i < stats.size(); i++)
		{
			SummaryContent.StatLine line = stats.get(i);
			Rectangle cell = layout.cell(box, i);
			int gap = Math.max(2, layout.pad);
			String value = line.rollText() + (line.netText().isEmpty() ? "" : " " + line.netText());
			int valueW = sm.stringWidth("+5 +99%");
			int labelW = sm.stringWidth("Defence");
			Rectangle label = new Rectangle(cell.x + gap, cell.y, labelW, cell.height);
			text.draw(g, line.label(), label, small, ThemeColor.TEXT, PageText.Align.LEFT);
			Rectangle val = new Rectangle(cell.x + cell.width - gap - valueW, cell.y, valueW, cell.height);
			text.draw(g, value, val, small, ThemeColor.SUBTEXT, PageText.Align.RIGHT);
			int barX = label.x + labelW + gap;
			int barW = Math.max(0, val.x - gap - barX);
			double t = fillMs <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0,
				(double) (since - (long) i * WindowTimes.SUMMARY_STAGGER) / fillMs));
			int barH = Math.max(3, st.metrics().bar * 3 / 4);
			if (barW > 0)
				st.xpBar(g, new Rectangle2D.Double(barX, cell.y + (cell.height - barH) / 2.0, barW, barH),
					line.fraction() * Easing.OUT_CUBIC.apply(t));
		}
	}

	private void nature(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Rectangle box = layout.nature;
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		Font bold = st.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics bm = g.getFontMetrics(bold);
		int pad = layout.pad + st.metrics().pad / 3;
		Rectangle line = new Rectangle(box.x + pad, box.y + layout.pad, box.width - 2 * pad,
			box.height - 2 * layout.pad);
		String title = text.draw(g, content.natureTitle(), line, bold, ThemeColor.TEXT, PageText.Align.LEFT);
		int used = bm.stringWidth(title) + pad;
		Rectangle effect = new Rectangle(line.x + used, line.y, Math.max(0, line.width - used), line.height);
		text.draw(g, content.natureEffect(), effect, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
	}

	private void description(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		Rectangle box = layout.description;
		if (box.height <= 0)
			return;

		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		int pad = layout.pad + st.metrics().pad / 3;
		text.drawWrapped(g, content.description(), new Rectangle(box.x + pad, box.y + layout.pad,
			box.width - 2 * pad, box.height - 2 * layout.pad), small, ThemeColor.SUBTEXT);
	}

	private void moves(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content, long since)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Rectangle box = layout.moves;
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		if (ctx.focused && WindowPages.ID_MOVES.equals(ctx.highlight))
			st.focusRing(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));

		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics rm = g.getFontMetrics(regular);
		FontMetrics sm = g.getFontMetrics(small);
		List<SummaryContent.MoveLine> moves = content.moves();
		long popMs = ctx.mode.moveMs(WindowTimes.SUMMARY_GROW);
		if (moves.isEmpty())
			text.draw(g, SummaryContent.NO_MOVES, layout.cell(box, 0), small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);

		for (int i = 0; i < SummaryContent.MAX_MOVES; i++)
		{
			Rectangle cell = layout.cell(box, i);
			boolean pendingCell = i == moves.size() && content.pendingMove() != null;
			if (i >= moves.size() && !pendingCell)
				continue;

			double t = popMs <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0,
				(double) (since - (long) i * WindowTimes.SUMMARY_STAGGER) / popMs));
			AffineTransform at = g.getTransform();
			Composite saved = PageContext.fade(g, t);
			double pop = popMs <= 0 ? 1.0 : 0.8 + 0.2 * Easing.OUT_BACK.apply(t);
			g.translate(cell.getCenterX(), cell.getCenterY());
			g.scale(pop, pop);
			g.translate(-cell.getCenterX(), -cell.getCenterY());
			int icon = rm.getHeight() - 4;
			int gap = Math.max(2, layout.pad);
			Rectangle name = new Rectangle(cell.x + gap + icon + gap, cell.y, cell.width - 2 * gap - icon - gap,
				rm.getHeight());
			Rectangle detail = new Rectangle(cell.x + gap, cell.y + rm.getHeight(), cell.width - 2 * gap,
				sm.getHeight());
			if (pendingCell)
			{
				g.setColor(st.color(ThemeColor.GOLD));
				g.fill(Icons.star(cell.x + gap + icon / 2.0, cell.y + rm.getHeight() / 2.0, icon / 2.0 + 1));
				text.draw(g, content.pendingMove(), name, regular, ThemeColor.TEXT, PageText.Align.LEFT);
				text.draw(g, "Click Moves to choose", detail, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
			}
			else
			{
				SummaryContent.MoveLine move = moves.get(i);
				g.setColor(st.color(ThemeColor.TEXT));
				Icons.style(g, move.style(), cell.x + gap, cell.y + 2, icon);
				text.draw(g, move.name(), name, regular, ThemeColor.TEXT, PageText.Align.LEFT);
				text.draw(g, move.detail(), detail, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
			}

			g.setComposite(saved);
			g.setTransform(at);
		}

		if (content.pendingMove() != null && moves.size() >= SummaryContent.MAX_MOVES)
		{
			double r = Math.max(4.0, rm.getHeight() / 3.0);
			g.setColor(st.color(ThemeColor.GOLD));
			g.fill(Icons.star(box.x + box.width - r - layout.pad, box.y + r + 1, r));
		}
	}

	private void bond(Graphics2D g, PageContext ctx, SummaryLayout layout, SummaryContent content)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Rectangle box = layout.bond;
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics sm = g.getFontMetrics(small);
		int size = Math.max(6, Math.min(sm.getHeight() - 1, box.height - 2 * layout.pad));
		int pad = layout.pad + st.metrics().pad / 3;
		int x = box.x + pad;
		int y = box.y + (box.height - size) / 2;
		boolean full = content.hearts() >= BondRules.MAX_HEARTS;
		for (int i = 0; i < BondRules.MAX_HEARTS; i++)
		{
			Shape heart = Icons.heart(x + i * (size + 2), y, size);
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

		int after = x + BondRules.MAX_HEARTS * (size + 2) + pad;
		Rectangle words = new Rectangle(after, box.y, Math.max(0, box.x + box.width - pad - after), box.height);
		text.draw(g, content.evolution(), words, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
	}

	private void buttons(Graphics2D g, PageContext ctx, SummaryLayout layout, boolean[] enabled)
	{
		WindowStyle st = ctx.style;
		for (int i = 0; i < SummaryLayout.BUTTONS.length; i++)
		{
			String id = WindowPages.summaryButtonId(i);
			Rectangle b = layout.buttons[i];
			Rectangle2D shape = new Rectangle2D.Double(b.x, b.y, b.width, b.height);
			st.button(g, shape, SummaryLayout.BUTTONS[i], i == 0, !enabled[i], ctx.hoverOf(id), ctx.pressOf(id));
			if (ctx.focused && id.equals(ctx.highlight))
				st.focusRing(g, Slant.shape(shape, st.metrics().chipSlant));
		}
	}
}
