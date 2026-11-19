/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws square pictures so they stay sharp at any scale. A picture is never stretched on the canvas: it is first
 * scaled to the exact number of device pixels it will cover (counting the canvas's own scale, such as a high-DPI
 * display or RuneLite's UI scale), by repeated halving and then one last step, and that copy is kept. Drawing it then
 * maps one image pixel to one screen pixel. Only the game thread draws, but the cache is synchronized anyway.
 */
final class CrispImages
{
	private static final int MAX_CACHED = 256;
	private static final int SHARPEN_MAX = 32;
	private static final double SHARPEN_AMOUNT = 1.2;
	private static final double CENTRE = 0.667;
	private static final double SIDE = 0.1665;
	private static final Map<Key, BufferedImage> CACHE = new HashMap<>();

	private CrispImages()
	{
	}

	/**
	 * Gives how many device pixels one unit of the graphics' drawing space covers.
	 *
	 * @param g the graphics
	 * @return the scale of its transform, never below 1
	 */
	static double deviceScale(Graphics2D g)
	{
		AffineTransform transform = g.getTransform();
		return Math.max(1.0, Math.hypot(transform.getScaleX(), transform.getShearY()));
	}

	/**
	 * Draws a square picture sharply into a square.
	 *
	 * @param g the graphics
	 * @param source the full-size picture, square
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square, in the graphics' units
	 */
	static void draw(Graphics2D g, BufferedImage source, double x, double y, double size)
	{
		if (source == null || size < 1.0)
			return;

		int pixels = Math.max(1, (int) Math.round(size * deviceScale(g)));
		BufferedImage image = fitted(source, pixels);
		Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, (int) Math.round(x), (int) Math.round(y), (int) Math.round(size), (int) Math.round(size),
			null);
		if (hint != null)
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
	}

	/**
	 * Gives a square picture scaled to an exact side, kept for next time.
	 *
	 * @param source the full-size picture, square
	 * @param pixels the side wanted
	 * @return the scaled picture, or the source itself when it is already that size
	 */
	static synchronized BufferedImage fitted(BufferedImage source, int pixels)
	{
		if (source.getWidth() == pixels && source.getHeight() == pixels)
			return source;

		Key key = new Key(source, pixels);
		BufferedImage cached = CACHE.get(key);
		if (cached != null)
			return cached;

		if (CACHE.size() >= MAX_CACHED)
			CACHE.clear();

		BufferedImage image = scaled(source, pixels);
		if (pixels <= SHARPEN_MAX && source.getWidth() >= 2 * pixels)
			image = sharpened(image);

		CACHE.put(key, image);
		return image;
	}

	/**
	 * Sharpens a small picture with an unsharp mask: each channel, alpha included, is pushed away from a light blur of
	 * itself. A picture shrunk by a large factor comes out soft, and this brings the edges and bands back.
	 *
	 * @param image the picture
	 * @return a sharpened copy
	 */
	static BufferedImage sharpened(BufferedImage image)
	{
		int w = image.getWidth();
		int h = image.getHeight();
		int[] in = image.getRGB(0, 0, w, h, null, 0, w);
		int[] out = new int[in.length];
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int packed = 0;
				for (int shift = 24; shift >= 0; shift -= 8)
				{
					double centre = (in[y * w + x] >>> shift) & 0xFF;
					double blur = 0;
					for (int dy = -1; dy <= 1; dy++)
					{
						for (int dx = -1; dx <= 1; dx++)
						{
							int sx = Math.max(0, Math.min(w - 1, x + dx));
							int sy = Math.max(0, Math.min(h - 1, y + dy));
							double weight = (dx == 0 ? CENTRE : SIDE) * (dy == 0 ? CENTRE : SIDE);
							blur += weight * ((in[sy * w + sx] >>> shift) & 0xFF);
						}
					}

					long value = Math.round(centre + SHARPEN_AMOUNT * (centre - blur));
					packed |= (int) Math.max(0, Math.min(255, value)) << shift;
				}

				out[y * w + x] = packed;
			}
		}

		BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		result.setRGB(0, 0, w, h, out, 0, w);
		return result;
	}

	/**
	 * Scales a picture down by halves and then to the exact size, so a 128 pixel picture stays smooth at 12 pixels.
	 *
	 * @param source the picture, square
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

	/** A source picture and a side, compared by the picture's identity. */
	private static final class Key
	{
		private final BufferedImage source;
		private final int pixels;

		Key(BufferedImage source, int pixels)
		{
			this.source = source;
			this.pixels = pixels;
		}

		@Override
		public boolean equals(Object other)
		{
			if (!(other instanceof Key))
				return false;

			Key k = (Key) other;
			return source == k.source && pixels == k.pixels;
		}

		@Override
		public int hashCode()
		{
			return System.identityHashCode(source) * 31 + pixels;
		}
	}
}
