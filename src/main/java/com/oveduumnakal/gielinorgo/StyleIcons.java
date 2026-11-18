/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.util.ImageUtil;

/**
 * The picture of each combat style, cut from the owner's artwork (one image with all five in a grid, kept in
 * {@code scripts/style-icons-source.png} and cut with {@code scripts/cut-style-icons.py}) into the bundled files in
 * {@code styles/}. The art is white with soft edges, so it is drawn in the colour the caller set: the window's
 * theme, a muted text colour or a rarity colour all work. Pictures are scaled once per size and colour and kept. A
 * file that is missing or cannot be read gives false from {@link #draw}, and the caller draws the old vector icon.
 */
final class StyleIcons
{
	private static final Logger log = LoggerFactory.getLogger(StyleIcons.class);
	private static final int MAX_CACHED = 256;
	private static final Map<CombatType, BufferedImage> SOURCES = new EnumMap<>(CombatType.class);
	private static final Map<String, BufferedImage> CACHE = new HashMap<>();

	static
	{
		for (CombatType style : CombatType.values())
			load(style);
	}

	private StyleIcons()
	{
	}

	/**
	 * Gives the file name of a style's picture.
	 *
	 * @param style the style
	 * @return the path relative to this package, such as {@code styles/stab.png}
	 */
	static String resourceName(CombatType style)
	{
		return "styles/" + style.name().toLowerCase(Locale.ROOT) + ".png";
	}

	private static void load(CombatType style)
	{
		try
		{
			SOURCES.put(style, ImageUtil.loadImageResource(StyleIcons.class, resourceName(style)));
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: no picture for the {} style, drawing the vector icon", style, e);
		}
	}

	/**
	 * @param style the style
	 * @return whether its picture was loaded
	 */
	static boolean available(CombatType style)
	{
		return SOURCES.containsKey(style);
	}

	/**
	 * Draws a style's picture in a square, in the graphics' current colour.
	 *
	 * @param g the graphics, with the colour set
	 * @param style the style
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 * @return false when the picture is not available, so nothing was drawn
	 */
	static boolean draw(Graphics2D g, CombatType style, double x, double y, double size)
	{
		BufferedImage source = SOURCES.get(style);
		if (source == null || size < 1.0)
			return false;

		try
		{
			AffineTransform transform = g.getTransform();
			double scale = Math.max(1.0, Math.hypot(transform.getScaleX(), transform.getShearY()));
			int pixels = Math.max(1, (int) Math.round(size * scale));
			BufferedImage image = picture(style, source, pixels, g.getColor());
			Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(image, (int) Math.round(x), (int) Math.round(y), (int) Math.round(size),
				(int) Math.round(size), null);
			if (hint != null)
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);

			return true;
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not draw the {} style picture", style, e);
			return false;
		}
	}

	private static synchronized BufferedImage picture(CombatType style, BufferedImage source, int pixels, Color colour)
	{
		String key = style.ordinal() + ":" + pixels + ":" + colour.getRGB();
		BufferedImage cached = CACHE.get(key);
		if (cached != null)
			return cached;

		if (CACHE.size() >= MAX_CACHED)
			CACHE.clear();

		BufferedImage image = tint(scaled(source, pixels), colour);
		CACHE.put(key, image);
		return image;
	}

	/**
	 * Scales a picture down by halves and then to the exact size, so a 128 pixel picture stays smooth at 12 pixels.
	 *
	 * @param source the picture
	 * @param pixels the side wanted
	 * @return the scaled picture
	 */
	static BufferedImage scaled(BufferedImage source, int pixels)
	{
		BufferedImage current = source;
		while (current.getWidth() / 2 >= pixels && current.getWidth() / 2 >= 2)
			current = resize(current, current.getWidth() / 2);

		return current.getWidth() == pixels ? current : resize(current, pixels);
	}

	private static BufferedImage resize(BufferedImage source, int pixels)
	{
		BufferedImage out = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(source, 0, 0, pixels, pixels, null);
		g.dispose();
		return out;
	}

	/**
	 * Gives a picture the colour wanted, keeping its see-through shape.
	 *
	 * @param source the picture
	 * @param colour the colour; its own see-through part scales the whole picture
	 * @return a new picture
	 */
	private static BufferedImage tint(BufferedImage source, Color colour)
	{
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int rgb = colour.getRGB() & 0xFFFFFF;
		int opacity = colour.getAlpha();
		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				int alpha = (source.getRGB(i, j) >>> 24) * opacity / 255;
				out.setRGB(i, j, (alpha << 24) | rgb);
			}
		}

		return out;
	}
}
