/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.util.ImageUtil;

/**
 * The small picture of each healing item, loaded once from the bundled files in {@code items/}. A file that is
 * missing or cannot be read gives null, and every caller then shows text alone.
 */
final class ItemIcons
{
	/** The side the picture is drawn at, in layout pixels. The file is bigger and is scaled to the real pixels. */
	static final int ICON_SIZE = 16;

	private static final Logger log = LoggerFactory.getLogger(ItemIcons.class);
	private static final Map<ItemType, BufferedImage> IMAGES = new EnumMap<>(ItemType.class);

	static
	{
		for (ItemType type : ItemType.values())
			load(type);
	}

	private ItemIcons()
	{
	}

	/**
	 * Gives the file name of an item's picture.
	 *
	 * @param type the item
	 * @return the path relative to this package, such as {@code items/small_salve.png}
	 */
	static String resourceName(ItemType type)
	{
		return "items/" + type.name().toLowerCase(Locale.ROOT) + ".png";
	}

	/**
	 * Loads one picture at its full size.
	 *
	 * @param type the item
	 */
	private static void load(ItemType type)
	{
		try
		{
			BufferedImage full = ImageUtil.loadImageResource(ItemIcons.class, resourceName(type));
			IMAGES.put(type, full);
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: no picture for the {} item", type, e);
		}
	}

	/**
	 * Finds a item's full-size picture.
	 *
	 * @param type the item, or null
	 * @return the picture as it is in the file, or null when there is none
	 */
	static BufferedImage source(ItemType type)
	{
		return type == null ? null : IMAGES.get(type);
	}

	/**
	 * Draws a item's picture sharply into a square, at the real pixel size the square covers.
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
		BufferedImage image = source(type);
		if (image == null)
			return false;

		CrispImages.draw(g, image, x, y, size);
		return true;
	}
}
