/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Cached still layers. Painting is the real cost of the window (gradients, slanted clips, antialiased text on the game
 * thread), so the parts of a page that do not move are painted once into a {@link BufferedImage} and only drawn each
 * frame; the moving parts paint on top. A layer is kept per page and is painted again only when its key changes: the
 * size, the scale, the theme or the snapshot version. Only the game thread uses it. Holds no RuneLite types.
 */
final class StillLayers
{
	private final Map<String, Entry> layers = new HashMap<>();
	private int paints;

	/**
	 * Gives the still layer of a page, painting it first if the key is new. The painter draws in layer coordinates,
	 * with (0, 0) at the layer's top-left corner.
	 *
	 * @param page which page, such as "party"
	 * @param width the layer width in pixels
	 * @param height the layer height in pixels
	 * @param scale the window scale the layer is drawn at
	 * @param theme the theme the layer is drawn in
	 * @param version the snapshot version the layer shows
	 * @param painter paints the still parts onto the layer
	 * @return the layer, as an image with room for transparency
	 */
	BufferedImage layer(String page, int width, int height, double scale, WindowTheme theme, long version,
		Consumer<Graphics2D> painter)
	{
		return layer(page, width, height, scale, theme, version, painter, 1.0);
	}

	/**
	 * Gives the still layer of a page at the sharpness of the canvas it will be drawn on. The layer has
	 * {@code device} image pixels for each layout pixel, and the painter draws in layout pixels, so on a high-DPI
	 * canvas the text and shapes are drawn at the real pixel size instead of being stretched when the layer is drawn.
	 *
	 * @param page which page, such as "party"
	 * @param width the layer width in layout pixels
	 * @param height the layer height in layout pixels
	 * @param scale the window scale the layer is drawn at
	 * @param theme the theme the layer is drawn in
	 * @param version the snapshot version the layer shows
	 * @param painter paints the still parts onto the layer
	 * @param device the device pixels per layout pixel, from {@link CrispImages#deviceScale}; 1 for a plain canvas
	 * @return the layer, {@code device} times as wide and high as {@code width} and {@code height}
	 */
	BufferedImage layer(String page, int width, int height, double scale, WindowTheme theme, long version,
		Consumer<Graphics2D> painter, double device)
	{
		double pixelsPer = Math.max(1.0, device);
		Key key = new Key(width, height, Math.round(scale * 1000), theme, version, Math.round(pixelsPer * 1000));
		Entry entry = layers.get(page);
		if (entry != null && entry.key.equals(key))
			return entry.image;

		BufferedImage image = new BufferedImage(Math.max(1, (int) Math.ceil(width * pixelsPer)),
			Math.max(1, (int) Math.ceil(height * pixelsPer)), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.scale(pixelsPer, pixelsPer);
			painter.accept(g);
		}
		finally
		{
			g.dispose();
		}

		paints++;
		layers.put(page, new Entry(key, image));
		return image;
	}

	/**
	 * Draws the still layer of a page onto the canvas, painting it first if its key is new.
	 *
	 * @param target where to draw
	 * @param x the left edge on the target
	 * @param y the top edge on the target
	 * @param page which page
	 * @param width the layer width in pixels
	 * @param height the layer height in pixels
	 * @param scale the window scale
	 * @param theme the theme
	 * @param version the snapshot version
	 * @param painter paints the still parts onto the layer
	 */
	void draw(Graphics2D target, int x, int y, String page, int width, int height, double scale, WindowTheme theme,
		long version, Consumer<Graphics2D> painter)
	{
		double device = CrispImages.deviceScale(target);
		BufferedImage image = layer(page, width, height, scale, theme, version, painter, device);
		target.drawImage(image, x, y, Math.max(1, width), Math.max(1, height), null);
	}

	/** Forgets every layer, for when the window closes so no image is held while it is shut. */
	void clear()
	{
		layers.clear();
	}

	/** @return how many times a layer was painted, so a test can tell a cache hit from a repaint */
	int paints()
	{
		return paints;
	}

	/** @return how many pages hold a layer now */
	int size()
	{
		return layers.size();
	}

	/** What a layer was painted for. */
	private static final class Key
	{
		private final int width;
		private final int height;
		private final long scaleMilli;
		private final WindowTheme theme;
		private final long version;
		private final long deviceMilli;

		Key(int width, int height, long scaleMilli, WindowTheme theme, long version, long deviceMilli)
		{
			this.deviceMilli = deviceMilli;
			this.width = width;
			this.height = height;
			this.scaleMilli = scaleMilli;
			this.theme = theme;
			this.version = version;
		}

		@Override
		public boolean equals(Object other)
		{
			if (!(other instanceof Key))
				return false;

			Key k = (Key) other;
			return width == k.width && height == k.height && scaleMilli == k.scaleMilli && theme == k.theme
				&& version == k.version && deviceMilli == k.deviceMilli;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(width, height, scaleMilli, theme, version, deviceMilli);
		}
	}

	/** A painted layer and the key it was painted for. */
	private static final class Entry
	{
		private final Key key;
		private final BufferedImage image;

		Entry(Key key, BufferedImage image)
		{
			this.key = key;
			this.image = image;
		}
	}
}
