/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Paints the pinned tracker with the painted window's kit: a framed box with one block for each line, the name on the
 * left and the progress on the right over a bar. An egg's name is in its rarity colour. It sets up and restores nothing
 * itself; the overlay that calls it does. Holds no RuneLite types.
 */
final class TrackerPainter
{
	/** The box's width in layout units. */
	static final double WIDTH_UNITS = 250;

	private static final double TEXT_UNITS = 26;
	private static final double GAP_UNITS = 6;
	private static final double VALUE_SHARE = 0.45;

	private final PageText.Sink sink;

	/**
	 * Creates the painter.
	 *
	 * @param sink hears what text is drawn, for tests
	 */
	TrackerPainter(PageText.Sink sink)
	{
		this.sink = sink;
	}

	/**
	 * Works out how big the box is.
	 *
	 * @param style the style, with the scale to draw at
	 * @param lines how many lines the box holds
	 * @return the box's size in pixels; one pixel high for no lines
	 */
	static Dimension size(WindowStyle style, int lines)
	{
		int pad = style.metrics().pad;
		int count = Math.max(0, lines);
		int height = count == 0 ? 1 : 2 * pad + count * block(style) + (count - 1) * style.px(GAP_UNITS);
		return new Dimension(style.px(WIDTH_UNITS), height);
	}

	/**
	 * Paints the box at (0, 0).
	 *
	 * @param g the graphics
	 * @param style the style, with the scale the box is sized at
	 * @param lines what to paint
	 */
	void paint(Graphics2D g, WindowStyle style, List<TrackerView.Line> lines)
	{
		PageText text = new PageText(style, sink);
		Dimension size = size(style, lines.size());
		style.pinnedOn(g, new Rectangle(size), "tracker");
		Font small = style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		int pad = style.metrics().pad;
		int inner = Math.max(0, size.width - 2 * pad);
		int textH = style.px(TEXT_UNITS);
		int barH = barHeight(style);
		int y = pad;
		for (TrackerView.Line line : lines)
		{
			int valueW = Math.min((int) (inner * VALUE_SHARE),
				g.getFontMetrics(small).stringWidth(line.valueText()) + pad);
			Rectangle label = new Rectangle(pad, y, inner - valueW, textH);
			Rectangle value = new Rectangle(pad + inner - valueW, y, valueW, textH);
			Color tint = line.rarity() == null ? style.color(ThemeColor.TEXT) : line.rarity().color();
			text.draw(g, line.label(), label, small, tint, PageText.Align.LEFT);
			text.draw(g, line.valueText(), value, small, line.done() ? ThemeColor.HP_FILL : ThemeColor.SUBTEXT,
				PageText.Align.RIGHT);
			style.bar(g, new Rectangle2D.Double(pad, y + textH + 1, inner, barH), line.fraction(),
				line.done() ? ThemeColor.HP_FILL : ThemeColor.XP_FILL, ThemeColor.HP_TRACK);
			y += block(style) + style.px(GAP_UNITS);
		}
	}

	private static int barHeight(WindowStyle style)
	{
		return Math.max(3, style.metrics().bar / 2);
	}

	private static int block(WindowStyle style)
	{
		return style.px(TEXT_UNITS) + 1 + barHeight(style);
	}
}
