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
 * The small picture of each jar, loaded once from the bundled files in {@code jars/}. A file that is missing or
 * cannot be read gives null, and every caller then shows text alone.
 */
final class JarIcons
{
	/** The side of the shown picture in pixels; the files are 32 pixels and are scaled down to this. */
	static final int ICON_SIZE = 16;

	private static final Logger log = LoggerFactory.getLogger(JarIcons.class);
	private static final Map<JarType, BufferedImage> IMAGES = new EnumMap<>(JarType.class);
	private static final Map<JarType, ImageIcon> ICONS = new EnumMap<>(JarType.class);

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
	 * Loads and shrinks one picture.
	 *
	 * @param type the jar
	 */
	private static void load(JarType type)
	{
		try
		{
			BufferedImage full = ImageUtil.loadImageResource(JarIcons.class, resourceName(type));
			BufferedImage small = ImageUtil.resizeImage(full, ICON_SIZE, ICON_SIZE);
			IMAGES.put(type, small);
			ICONS.put(type, new ImageIcon(small));
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: no picture for the {} jar", type, e);
		}
	}

	/**
	 * Finds a jar's picture for drawing on the game screen.
	 *
	 * @param type the jar, or null
	 * @return the 16 pixel picture, or null when there is none
	 */
	static BufferedImage image(JarType type)
	{
		return type == null ? null : IMAGES.get(type);
	}

	/**
	 * Finds a jar's picture for a Swing label or list line.
	 *
	 * @param type the jar, or null
	 * @return the 16 pixel icon, or null when there is none
	 */
	static ImageIcon swing(JarType type)
	{
		return type == null ? null : ICONS.get(type);
	}
}
