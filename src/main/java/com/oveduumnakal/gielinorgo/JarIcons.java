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
 * The small picture of each jar, loaded once from the bundled files in {@code jars/}. A file that is missing or
 * cannot be read gives null, and every caller then shows text alone.
 */
final class JarIcons
{
	/** The side the picture is drawn at, in layout pixels. The file is bigger and is scaled to the real pixels. */
	static final int ICON_SIZE = 16;

	private static final Logger log = LoggerFactory.getLogger(JarIcons.class);
	private static final Map<JarType, BufferedImage> IMAGES = new EnumMap<>(JarType.class);

	static
	{
		for (JarType type : JarType.values())
			load(type);
	}

	private JarIcons()
	{
	}

	/**
	 * Gives the file name of a jar's picture.
	 *
	 * @param type the jar
	 * @return the path relative to this package, such as {@code jars/bronze.png}
	 */
	static String resourceName(JarType type)
	{
		return "jars/" + type.name().toLowerCase(Locale.ROOT) + ".png";
	}

	/**
	 * Loads one picture at its full size.
	 *
	 * @param type the jar
	 */
	private static void load(JarType type)
	{
		try
		{
			BufferedImage full = ImageUtil.loadImageResource(JarIcons.class, resourceName(type));
			IMAGES.put(type, full);
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: no picture for the {} jar", type, e);
		}
	}

	/**
	 * Finds a jar's full-size picture.
	 *
	 * @param type the jar, or null
	 * @return the picture as it is in the file, or null when there is none
	 */
	static BufferedImage source(JarType type)
	{
		return type == null ? null : IMAGES.get(type);
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
		BufferedImage image = source(type);
		if (image == null)
			return false;

		CrispImages.draw(g, image, x, y, size);
		return true;
	}
}
