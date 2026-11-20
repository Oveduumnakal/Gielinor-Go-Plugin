/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * The window's tooltip: a small themed box that shows next to the pointer after it has rested on something with a text
 * for 400 ms, and stays inside the canvas. Whatever the pointer is over reports itself with {@link #hover}; a new key
 * restarts the delay, the same key keeps it running, and {@link #clear} hides the tip at once. A long text wraps to a
 * width, and line breaks in the text are kept. Time is passed in, like {@link CombatWatch}. Holds no RuneLite types.
 */
final class Tooltip
{
	/** How long the pointer must rest before the tip shows, in milliseconds. */
	static final long DELAY_MS = 400;

	/** The widest a tip gets before its text wraps, in layout units. */
	static final double MAX_WIDTH_UNITS = 280;

	/** How far the tip sits from the pointer, in layout units. */
	static final double OFFSET_UNITS = 14;

	private Object key;
	private String text = "";
	private Point pointer = new Point();
	private long sinceMs;

	/**
	 * Reports what the pointer is over. Call it on every pointer move and every frame the pointer stays on the same
	 * thing; the delay counts from the first call with that key.
	 *
	 * @param key what the pointer is over, such as "row:2"; equal keys are the same thing
	 * @param text what to show; null or empty hides the tip, as {@link #clear} does
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @param nowMs the time in milliseconds
	 */
	void hover(Object key, String text, int x, int y, long nowMs)
	{
		if (key == null || text == null || text.isEmpty())
		{
			clear();
			return;
		}

		if (!Objects.equals(this.key, key))
			sinceMs = nowMs;

		this.key = key;
		this.text = text;
		this.pointer = new Point(x, y);
	}

	/** Hides the tip and forgets what the pointer was on; the next hover starts the delay again. */
	void clear()
	{
		key = null;
		text = "";
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return whether the tip shows now: something is hovered and it has been for {@link #DELAY_MS}
	 */
	boolean visible(long nowMs)
	{
		return key != null && nowMs - sinceMs >= DELAY_MS;
	}

	/** @return the text of the tip, or an empty text when nothing is hovered */
	String text()
	{
		return text;
	}

	/**
	 * Works out where a tip of a size goes: below and right of the pointer, flipped to the other side when it would
	 * leave the canvas, and kept inside the canvas whatever happens.
	 *
	 * @param size the tip's size
	 * @param pointer the pointer's position
	 * @param canvas the canvas bounds
	 * @param gap the distance from the pointer
	 * @return the tip's box
	 */
	static Rectangle place(Dimension size, Point pointer, Rectangle canvas, int gap)
	{
		int x = pointer.x + gap;
		if (x + size.width > canvas.x + canvas.width)
			x = pointer.x - gap - size.width;

		int y = pointer.y + gap;
		if (y + size.height > canvas.y + canvas.height)
			y = pointer.y - gap - size.height;

		x = Math.max(canvas.x, Math.min(canvas.x + canvas.width - size.width, x));
		y = Math.max(canvas.y, Math.min(canvas.y + canvas.height - size.height, y));
		return new Rectangle(x, y, size.width, size.height);
	}

	/**
	 * Gives the tip's box as it paints now, by its final place.
	 *
	 * @param style the style
	 * @param fm the measures of the tip's font, from {@link #font}
	 * @param canvas the canvas bounds
	 * @return the box, or null when nothing shows; call {@link #visible} first for the delay
	 */
	Rectangle bounds(WindowStyle style, FontMetrics fm, Rectangle canvas)
	{
		if (key == null)
			return null;

		int pad = style.metrics().pad;
		int maxText = Math.max(fm.getHeight(), Math.min(style.px(MAX_WIDTH_UNITS), canvas.width - 2 * pad));
		TextLayout layout = TextLayout.plain(text, fm, maxText, 0);
		int widest = 0;
		for (int i = 0; i < layout.lineCount(); i++)
			widest = Math.max(widest, layout.lineWidth(i));

		Dimension size = new Dimension(widest + 2 * pad, layout.height() + pad);
		return place(size, pointer, canvas, style.px(OFFSET_UNITS));
	}

	/**
	 * @param style the style
	 * @return the font the tip uses, a whole multiple of the small font's native size
	 */
	static Font font(WindowStyle style)
	{
		return style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
	}

	/**
	 * Paints the tip when it is visible.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param canvas the canvas bounds, which the tip stays inside
	 * @param nowMs the time in milliseconds
	 * @return the tip's box, or null when nothing was painted
	 */
	Rectangle paint(Graphics2D g, WindowStyle style, Rectangle canvas, long nowMs)
	{
		if (!visible(nowMs))
			return null;

		Font font = font(style);
		FontMetrics fm = g.getFontMetrics(font);
		Rectangle box = bounds(style, fm, canvas);
		int pad = style.metrics().pad;
		TextLayout layout = TextLayout.plain(text, fm, box.width - 2 * pad, 0);
		style.popup(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height), "tooltip", ThemeColor.TOOLTIP,
			ThemeColor.PANEL_BORDER, true, Readability.TOOLTIP);
		layout.paint(g, style, box.x + pad, box.y + pad / 2, ThemeColor.TOOLTIP_TEXT);
		return box;
	}
}
