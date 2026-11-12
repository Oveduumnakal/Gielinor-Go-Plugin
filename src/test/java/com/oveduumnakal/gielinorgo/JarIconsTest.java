/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks the 15 bundled jar pictures: every jar has one, in the agreed size, with a clear margin, all different. */
public class JarIconsTest
{
	private static final int SIDE = 32;

	@Test
	public void everyJarHasA32PixelPictureWithAnAlphaChannel() throws IOException
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
	public void theOutermostPixelRingIsFullyTransparent() throws IOException
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
	public void allFifteenPicturesDiffer() throws IOException
	{
		Set<String> seen = new HashSet<>();
		for (JarType type : JarType.values())
			assertTrue(type.name() + " matches another jar", seen.add(pixels(read(type))));

		assertEquals(15, seen.size());
	}

	@Test
	public void theShownPictureIs16PixelsAndMissingJarsGiveNull()
	{
		for (JarType type : JarType.values())
		{
			assertNotNull(type.name(), JarIcons.image(type));
			assertEquals(JarIcons.ICON_SIZE, JarIcons.image(type).getWidth());
			assertEquals(JarIcons.ICON_SIZE, JarIcons.image(type).getHeight());
			assertNotNull(type.name(), JarIcons.swing(type));
		}

		assertEquals(null, JarIcons.image(null));
		assertEquals(null, JarIcons.swing(null));
	}

	private static BufferedImage read(JarType type) throws IOException
	{
		try (InputStream in = JarIcons.class.getResourceAsStream(JarIcons.resourceName(type)))
		{
			assertNotNull(type.name() + " has no file", in);
			return ImageIO.read(in);
		}
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
