/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks that each rendered jar picture is the right jar: its cork, band or emblem has its own colour, the grey jars
 * have none, and all stand at the same scale.
 */
public class JarArtTest
{
	/** The colour family of each jar's lid, band or mark, as hue ranges in degrees, or none when it is grey. */
	private static final Map<JarType, double[]> HUES = new EnumMap<>(JarType.class);

	static
	{
		HUES.put(JarType.BRONZE, new double[]{25, 50});
		HUES.put(JarType.RUNE, new double[]{195, 230});
		HUES.put(JarType.DRAGON, new double[]{350, 15});
		HUES.put(JarType.VENOM, new double[]{100, 150});
		HUES.put(JarType.EMBER, new double[]{15, 40});
		HUES.put(JarType.GIANT, new double[]{15, 40});
		HUES.put(JarType.TIDE, new double[]{190, 220});
		HUES.put(JarType.STAB, new double[]{350, 15});
		HUES.put(JarType.SLASH, new double[]{350, 15});
		HUES.put(JarType.CRUSH, new double[]{350, 15});
		HUES.put(JarType.ARCHER, new double[]{100, 150});
		HUES.put(JarType.MYSTIC, new double[]{205, 235});
	}

	private static BufferedImage read(JarType type)
	{
		BufferedImage image = JarIcons.source(type, 128);
		assertNotNull(type.name() + " has no picture", image);
		return image;
	}

	/** @return the average hue in degrees of the clearly coloured pixels, or -1 when there are too few */
	private static double hue(BufferedImage image)
	{
		double x = 0;
		double y = 0;
		int count = 0;
		for (int i = 0; i < image.getWidth(); i++)
		{
			for (int j = 0; j < image.getHeight(); j++)
			{
				int argb = image.getRGB(i, j);
				if ((argb >>> 24) < 200)
					continue;

				float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
				if (hsb[1] < 0.45f || hsb[2] < 0.25f)
					continue;

				x += Math.cos(hsb[0] * 2 * Math.PI);
				y += Math.sin(hsb[0] * 2 * Math.PI);
				count++;
			}
		}

		if (count < 6)
			return -1;

		double degrees = Math.toDegrees(Math.atan2(y, x));
		return degrees < 0 ? degrees + 360 : degrees;
	}

	private static boolean inside(double hue, double[] range)
	{
		return range[0] <= range[1] ? hue >= range[0] && hue <= range[1] : hue >= range[0] || hue <= range[1];
	}

	@Test
	public void eachColouredJarHasItsOwnColour()
	{
		for (Map.Entry<JarType, double[]> entry : HUES.entrySet())
		{
			double hue = hue(read(entry.getKey()));

			assertTrue(entry.getKey() + " has no clear colour", hue >= 0);
			assertTrue(entry.getKey() + " hue " + Math.round(hue), inside(hue, entry.getValue()));
		}
	}

	@Test
	public void theGreyJarsHaveNoStrongColour()
	{
		for (JarType type : new JarType[]{JarType.IRON, JarType.SWIFT, JarType.BLESSED, JarType.CAVE, JarType.SLAYER})
			assertEquals(type.name(), -1.0, hue(read(type)), 0.0);
	}

	@Test
	public void everyJarIsTallerThanWideAndTheSameHeightAcrossTheSet()
	{
		int shortest = Integer.MAX_VALUE;
		int tallest = 0;
		for (JarType type : JarType.values())
		{
			BufferedImage image = read(type);
			int top = image.getHeight();
			int bottom = -1;
			int left = image.getWidth();
			int right = -1;
			for (int i = 0; i < image.getWidth(); i++)
			{
				for (int j = 0; j < image.getHeight(); j++)
				{
					if ((image.getRGB(i, j) >>> 24) < 64)
						continue;

					top = Math.min(top, j);
					bottom = Math.max(bottom, j);
					left = Math.min(left, i);
					right = Math.max(right, i);
				}
			}

			int height = bottom - top + 1;
			assertTrue(type.name() + " is not taller than wide", height > right - left + 1);
			assertTrue(type.name() + " is too small", height >= 96);
			shortest = Math.min(shortest, height);
			tallest = Math.max(tallest, height);
		}

		assertTrue("the jars differ in height by " + (tallest - shortest), tallest - shortest <= 16);
	}
}
