/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import net.runelite.client.ui.FontManager;

/**
 * The fonts of the painted window, and the one rule that keeps them sharp.
 *
 * <p>Sharpness check (painted at 8 to 32 points into a {@code BufferedImage}, antialiasing on and off, and looked at
 * magnified): the RuneScape fonts are pixel fonts drawn on a 16 point grid. At 16 points (1x) and 32 points (2x) every
 * stem is a whole number of pixels and the text is crisp. At every size in between (12 to 15, 18, 20, and 24, which is
 * 1.5x) stems turn two-tone and uneven in weight and the letter spacing wobbles, so the text looks blurred, and below
 * 12 points it is not readable. So the window draws text only at whole multiples of the native 16 pixels, never scaled
 * freely: {@link #step} picks the multiple from the wanted size, and the layout gives the spare room to padding, not
 * to the glyphs. Text is drawn with antialiasing on and fractional metrics off, so glyph advances are whole pixels.
 * Body text is {@link #BODY_UNITS} layout units tall, which sets the smallest window scale (see
 * {@link WindowFit#MIN_SCALE}).
 */
final class WindowFonts
{
	/** The native pixel size of the RuneScape fonts, the only size that is sharp besides its whole multiples. */
	static final int NATIVE_PX = 16;

	/** The layout size of body text, in units. The smallest window scale is where this equals {@link #NATIVE_PX}. */
	static final double BODY_UNITS = 24;

	/** The layout size of titles, in units. */
	static final double TITLE_UNITS = 32;

	/** The layout size of small print, in units. It is drawn at the native size at every scale. */
	static final double SMALL_UNITS = 20;

	/** The biggest whole multiple of the native size that is used. */
	static final int MAX_STEP = 3;

	/** How far under a whole multiple a wanted size may be and still get that multiple. */
	private static final double STEP_TOLERANCE = 0.15;

	private static final Map<Face, Map<Integer, Font>> CACHE = new EnumMap<>(Face.class);

	private WindowFonts()
	{
	}

	/** The three RuneScape fonts RuneLite ships. */
	enum Face
	{
		/** The small font, for small print and dense rows. */
		SMALL,

		/** The regular font, for body text. */
		REGULAR,

		/** The bold font, for names and titles. */
		BOLD
	}

	/**
	 * Picks the whole multiple of the native size to draw a text at.
	 *
	 * @param units the wanted size in layout units
	 * @param scale the window scale, from {@link WindowFit#scale}
	 * @return 1, 2 or 3 (up to {@link #MAX_STEP}); never less than 1, since text under the native size is not sharp
	 */
	static int step(double units, double scale)
	{
		double wanted = units * scale / NATIVE_PX;
		int whole = (int) Math.floor(wanted + STEP_TOLERANCE);
		return Math.max(1, Math.min(MAX_STEP, whole));
	}

	/**
	 * @param units the wanted size in layout units
	 * @param scale the window scale
	 * @return the pixel size the text is drawn at, a whole multiple of {@link #NATIVE_PX}
	 */
	static int pixelSize(double units, double scale)
	{
		return step(units, scale) * NATIVE_PX;
	}

	/**
	 * Gives a font at the size the sharpness rule allows. Fonts are cached, so this is cheap to call each frame.
	 *
	 * @param face which font
	 * @param units the wanted size in layout units
	 * @param scale the window scale
	 * @return the font, at a whole multiple of the native size
	 */
	static synchronized Font font(Face face, double units, double scale)
	{
		int px = pixelSize(units, scale);
		Map<Integer, Font> sizes = CACHE.computeIfAbsent(face, f -> new HashMap<>());
		return sizes.computeIfAbsent(px, p -> base(face).deriveFont((float) p));
	}

	private static Font base(Face face)
	{
		switch (face)
		{
			case SMALL:
				return FontManager.getRunescapeSmallFont();
			case BOLD:
				return FontManager.getRunescapeBoldFont();
			default:
				return FontManager.getRunescapeFont();
		}
	}
}
