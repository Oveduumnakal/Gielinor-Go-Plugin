/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks the 17 rendered jar pictures: every jar has one, at the size asked, with a clear margin, all different. */
public class JarIconsTest
{
	private static final int SIDE = 128;

	@Test
	public void everyJarHasA128PixelPictureWithAnAlphaChannel()
	{
		for (JarType type : JarType.values())
		{
			BufferedImage image = read(type);

			assertEquals(type.name(), SIDE, image.getWidth());
			assertEquals(type.name(), SIDE, image.getHeight());
			assertTrue(type.name(), image.getColorModel().hasAlpha());
		}
	}

	@Test
	public void theOutermostPixelRingIsFullyTransparent()
	{
		for (JarType type : JarType.values())
		{
			BufferedImage image = read(type);
			for (int i = 0; i < SIDE; i++)
			{
				assertEquals(type.name(), 0, alpha(image, i, 0));
				assertEquals(type.name(), 0, alpha(image, i, SIDE - 1));
				assertEquals(type.name(), 0, alpha(image, 0, i));
				assertEquals(type.name(), 0, alpha(image, SIDE - 1, i));
			}
		}
	}

	@Test
	public void allSeventeenPicturesDiffer()
	{
		Set<String> seen = new HashSet<>();
		for (JarType type : JarType.values())
			assertTrue(type.name() + " matches another jar", seen.add(pixels(read(type))));

		assertEquals(17, seen.size());
	}

	@Test
	public void aPictureIsRenderedAtTheSizeAskedForAndKept()
	{
		for (JarType type : JarType.values())
		{
			for (int side : new int[] {16, 22, 33, 50})
			{
				BufferedImage image = JarIcons.source(type, side);
				assertNotNull(type.name(), image);
				assertEquals(side, image.getWidth());
				assertEquals(side, image.getHeight());
				assertTrue(image == JarIcons.source(type, side));
			}
		}

		assertEquals(null, JarIcons.source(null, 16));
		assertEquals(JarIcons.LARGEST, JarIcons.source(JarType.IRON, 4000).getWidth());
	}

	@Test
	public void allSeventeenDifferAtTheSmallestSizeDrawn()
	{
		Set<String> seen = new HashSet<>();
		for (JarType type : JarType.values())
			assertTrue(type.name() + " matches another jar", seen.add(pixels(JarIcons.source(type, 16))));
	}

	private static BufferedImage read(JarType type)
	{
		BufferedImage image = JarIcons.source(type, SIDE);
		assertNotNull(type.name() + " has no picture", image);
		return image;
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	private static String pixels(BufferedImage image)
	{
		StringBuilder out = new StringBuilder();
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
				out.append(Integer.toHexString(image.getRGB(x, y))).append(',');
		}

		return out.toString();
	}
}
