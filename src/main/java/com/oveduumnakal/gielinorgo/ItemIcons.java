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
 * The small picture of each healing item, rendered from its model ({@link ItemShapes}) with the jar renderer, once for
 * each size it is drawn at. An item that cannot be rendered gives null, and every caller then shows text alone.
 */
final class ItemIcons
{
	/** The side the picture is drawn at beside floating text, in layout pixels. */
	static final int ICON_SIZE = 16;

	/** Pictures up to this side thicken the marks, which would otherwise vanish. */
	private static final int SMALL_UP_TO = 18;

	/** How much smaller the Small salve is than the Large one, standing on the same line. */
	private static final double SMALL_SALVE_SCALE = 0.82;

	/** Where the line the pictures stand on lies in the room left over by a smaller one, from the top. */
	private static final double BASELINE = 0.87;

	private static final Logger log = LoggerFactory.getLogger(ItemIcons.class);
	private static final Map<Integer, BufferedImage> CACHE = new HashMap<>();

	private ItemIcons()
	{
	}

	/**
	 * Finds an item's picture at one size, rendering it the first time.
	 *
	 * @param type the item, or null
	 * @param pixels the side wanted; held between 1 and {@link JarIcons#LARGEST}
	 * @return the square picture, or null when there is none
	 */
	static synchronized BufferedImage source(ItemType type, int pixels)
	{
		if (type == null)
			return null;

		int side = Math.max(1, Math.min(JarIcons.LARGEST, pixels));
		int key = type.ordinal() * (JarIcons.LARGEST + 1) + side;
		BufferedImage cached = CACHE.get(key);
		if (cached != null)
			return cached;

		try
		{
			BufferedImage image = picture(type, side);
			if (CACHE.size() >= JarIcons.LARGEST)
				CACHE.clear();

			CACHE.put(key, image);
			return image;
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: no picture for the {} item", type, e);
			return null;
		}
	}

	/**
	 * Draws an item's picture sharply into a square, at the real pixel size the square covers.
	 *
	 * @param g the graphics
	 * @param type the item, or null
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 * @return false when there is no picture, so nothing was drawn
	 */
	static boolean draw(Graphics2D g, ItemType type, double x, double y, double size)
	{
		BufferedImage image = source(type, JarIcons.pixels(g, size));
		if (image == null)
			return false;

		CrispImages.draw(g, image, x, y, size);
		return true;
	}

	private static BufferedImage picture(ItemType type, int side)
	{
		boolean small = side <= SMALL_UP_TO;
		if (type != ItemType.SMALL_SALVE)
			return render(type, side, small);

		int less = Math.max(1, (int) Math.round(side * SMALL_SALVE_SCALE));
		BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(render(type, less, small), (side - less) / 2, (int) Math.round((side - less) * BASELINE), null);
		g.dispose();
		return out;
	}

	private static BufferedImage render(ItemType type, int side, boolean small)
	{
		int edge = side <= 18 ? 0x73 : side <= 26 ? 0xA6 : 0xE0;
		return JarRenderer.render(ItemShapes.build(type, small), new JarFixedView(side, 0, 0),
			new Rectangle(0, 0, side, side), side <= 96 ? 4 : 2, 0.5, edge);
	}
}
