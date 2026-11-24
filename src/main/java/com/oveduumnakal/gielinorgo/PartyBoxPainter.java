/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Paints the pinned party box with the painted window's kit: a framed box with one block for each creature, its
 * portrait on the left, then the name and the level over the health bar. It sets up and restores nothing itself; the
 * overlay that calls it does. Holds no RuneLite types.
 */
final class PartyBoxPainter
{
	/** The box's width in layout units. */
	static final double WIDTH_UNITS = 220;

	private static final double ROW_UNITS = 34;
	private static final double GAP_UNITS = 6;

	private final PortraitCache pictures;
	private final PageText.Sink sink;

	/**
	 * Creates the painter.
	 *
	 * @param pictures where creature pictures are
	 * @param sink hears what text is drawn, for tests
	 */
	PartyBoxPainter(PortraitCache pictures, PageText.Sink sink)
	{
		this.pictures = pictures;
		this.sink = sink;
	}

	/**
	 * Works out how big the box is.
	 *
	 * @param style the style, with the scale to draw at
	 * @param rows how many creatures the box holds
	 * @return the box's size in pixels; one pixel high for none
	 */
	static Dimension size(WindowStyle style, int rows)
	{
		int pad = style.metrics().pad;
		int count = Math.max(0, rows);
		int height = count == 0 ? 1 : 2 * pad + count * style.px(ROW_UNITS) + (count - 1) * style.px(GAP_UNITS);
		return new Dimension(style.px(WIDTH_UNITS), height);
	}

	/**
	 * Paints the box at (0, 0).
	 *
	 * @param g the graphics
	 * @param style the style, with the scale the box is sized at
	 * @param rows what to paint
	 * @param now the time in milliseconds, for a picture that fades in
	 */
	void paint(Graphics2D g, WindowStyle style, List<RowModel> rows, long now)
	{
		PageText text = new PageText(style, sink);
		Dimension size = size(style, rows.size());
		style.pinnedOn(g, new Rectangle(size), "partybox");
		Font small = style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics sm = g.getFontMetrics(small);
		int pad = style.metrics().pad;
		int rowH = style.px(ROW_UNITS);
		int inner = Math.max(0, size.width - 2 * pad);
		int y = pad;
		for (RowModel row : rows)
		{
			ThemeColor ink = row.state() == RowModel.State.NORMAL ? ThemeColor.TEXT : ThemeColor.SUBTEXT;
			Rectangle2D portrait = new Rectangle2D.Double(pad, y, rowH, rowH);
			PortraitFrame.paint(g, style, pictures, row, portrait, style.metrics().slant / 2.0, 0.0, now,
				MotionMode.OFF);
			int left = pad + rowH + pad;
			int levelW = Math.min(inner / 3, sm.stringWidth(row.levelText()) + pad);
			int right = pad + inner;
			int topH = Math.max(1, rowH / 2);
			text.drawName(g, row.name(), new Rectangle(left, y, Math.max(0, right - left - levelW), topH), ink);
			text.draw(g, row.levelText(), new Rectangle(right - levelW, y, levelW, topH), small,
				ThemeColor.SUBTEXT, PageText.Align.RIGHT);
			int barH = Math.max(3, style.metrics().bar * 3 / 4);
			int pctW = Math.min(inner / 4, sm.stringWidth("100%") + pad);
			Rectangle2D bar = new Rectangle2D.Double(left, y + rowH - barH, Math.max(1, right - left - pctW - pad),
				barH);
			style.hpBar(g, bar, row.hp());
			text.draw(g, row.hpText(), new Rectangle(right - pctW, y + rowH - topH, pctW, topH), small,
				ThemeColor.SUBTEXT, PageText.Align.RIGHT);
			y += rowH + style.px(GAP_UNITS);
		}
	}
}
