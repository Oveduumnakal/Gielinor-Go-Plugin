/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The small picture of each jar, rendered from the same model as the thrown jar ({@link JarShapes}), once for each
 * size it is drawn at, so it is never stretched. A jar that cannot be rendered gives null, and every caller then shows
 * text or a plain glyph.
 */
final class JarIcons
{
	/** The side the picture is drawn at beside floating text, in layout pixels. */
	static final int ICON_SIZE = 16;

	/** The largest picture rendered; a bigger square shows this one scaled up. */
	static final int LARGEST = 256;

	/** The most pictures kept before all are rendered afresh, which covers every jar at many sizes. */
	private static final int MAX_CACHED = 256;

	/** Pictures up to this side are averaged from sixteen samples a pixel, larger ones from four. */
	private static final int FINE_UP_TO = 96;

	private static final Logger log = LoggerFactory.getLogger(JarIcons.class);
	private static final Map<Integer, BufferedImage> CACHE = new HashMap<>();

	private JarIcons()
	{
	}

	/**
	 * Gives the number of device pixels a square covers, which is the size to render at.
	 *
	 * @param g the graphics it will be drawn on
	 * @param size the side of the square, in the graphics' units
	 * @return the side in device pixels
	 */
	static int pixels(Graphics2D g, double size)
	{
		return Math.max(1, (int) Math.round(size * CrispImages.deviceScale(g)));
	}

	/**
	 * Finds a jar's picture at one size, rendering it the first time.
	 *
	 * @param type the jar, or null
	 * @param pixels the side wanted; held between 1 and {@link #LARGEST}
	 * @return the square picture, or null when there is none
	 */
	static synchronized BufferedImage source(JarType type, int pixels)
	{
		if (type == null)
			return null;

		int side = Math.max(1, Math.min(LARGEST, pixels));
		int key = type.ordinal() * (LARGEST + 1) + side;
		BufferedImage cached = CACHE.get(key);
		if (cached != null)
			return cached;

		try
		{
			BufferedImage image = render(type, side);
			if (CACHE.size() >= MAX_CACHED)
				CACHE.clear();

			CACHE.put(key, image);
			return image;
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: no picture for the {} jar", type, e);
			return null;
		}
	}

	/**
	 * Draws a jar's picture sharply into a square, at the real pixel size the square covers.
	 *
	 * @param g the graphics
	 * @param type the jar, or null
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 * @return false when there is no picture, so nothing was drawn
	 */
	static boolean draw(Graphics2D g, JarType type, double x, double y, double size)
	{
		BufferedImage image = source(type, pixels(g, size));
		if (image == null)
			return false;

		CrispImages.draw(g, image, x, y, size);
		return true;
	}

	/** Renders a jar front on and empty, with a ring that is fainter on a small picture. */
	private static BufferedImage render(JarType type, int side)
	{
		int edge = side <= 18 ? 0x73 : side <= 26 ? 0xA6 : 0xE0;
		return JarRenderer.render(JarShapes.build(type, false), new JarFixedView(side, 0, 0),
			new Rectangle(0, 0, side, side), side <= FINE_UP_TO ? 4 : 2, 0.5, edge);
	}
}
