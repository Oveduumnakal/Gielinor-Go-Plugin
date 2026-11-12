/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.util.ImageUtil;

/**
 * The small picture of each healing item, loaded once from the bundled files in {@code items/}. A file that is
 * missing or cannot be read gives null, and every caller then shows text alone.
 */
final class ItemIcons
{
	/** The side of the shown picture in pixels; the files are 32 pixels and are scaled down to this. */
	static final int ICON_SIZE = 16;

	private static final Logger log = LoggerFactory.getLogger(ItemIcons.class);
	private static final Map<ItemType, BufferedImage> IMAGES = new EnumMap<>(ItemType.class);
	private static final Map<ItemType, ImageIcon> ICONS = new EnumMap<>(ItemType.class);
	private static final Map<ItemType, ImageIcon> GREY_ICONS = new EnumMap<>(ItemType.class);

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
	 * Loads and shrinks one picture, and makes its faded twin for an item the player does not own.
	 *
	 * @param type the item
	 */
	private static void load(ItemType type)
	{
		try
		{
			BufferedImage full = ImageUtil.loadImageResource(ItemIcons.class, resourceName(type));
			BufferedImage small = ImageUtil.resizeImage(full, ICON_SIZE, ICON_SIZE);
			IMAGES.put(type, small);
			ICONS.put(type, new ImageIcon(small));
			GREY_ICONS.put(type, new ImageIcon(ImageUtil.grayscaleImage(ImageUtil.alphaOffset(small, -150))));
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: no picture for the {} item", type, e);
		}
	}

	/**
	 * Finds an item's picture for drawing on the game screen.
	 *
	 * @param type the item, or null
	 * @return the 16 pixel picture, or null when there is none
	 */
	static BufferedImage image(ItemType type)
	{
		return type == null ? null : IMAGES.get(type);
	}

	/**
	 * Finds an item's picture for a Swing label.
	 *
	 * @param type the item, or null
	 * @return the 16 pixel icon, or null when there is none
	 */
	static ImageIcon swing(ItemType type)
	{
		return type == null ? null : ICONS.get(type);
	}

	/**
	 * Finds an item's greyed picture, for an item the player owns none of.
	 *
	 * @param type the item, or null
	 * @return the faded 16 pixel icon, or null when there is none
	 */
	static ImageIcon swingGrey(ItemType type)
	{
		return type == null ? null : GREY_ICONS.get(type);
	}
}
