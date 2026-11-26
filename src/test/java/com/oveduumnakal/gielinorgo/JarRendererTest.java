/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests the painting of a jar into a picture. */
public class JarRendererTest
{
	private static final int SIZE = 56;

	private static BufferedImage paint(JarType type, double spin, double roll)
	{
		JarModel jar = JarShapes.build(type, true);
		return JarRenderer.render(jar, new JarFixedView(SIZE, spin, roll), new Rectangle(0, 0, SIZE, SIZE), 4, 0.5,
			0xB4);
	}

	private static int solid(BufferedImage image)
	{
		return (int) Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
				.filter(p -> p >>> 24 > 128)
				.count();
	}

	@Test
	public void everyJarPaintsAtEveryMomentOfATumble()
	{
		for (JarType type : JarType.values())
		{
			for (int step = 0; step < 8; step++)
			{
				double turn = step * Math.PI / 4;
				assertTrue(type + " step " + step, solid(paint(type, turn, -turn)) > SIZE * SIZE / 10);
			}
		}
	}

	@Test
	public void noTwoJarsLookTheSame()
	{
		Set<String> seen = new HashSet<>();
		for (JarType type : JarType.values())
		{
			BufferedImage image = paint(type, 0, 0);
			seen.add(Arrays.toString(image.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE)));
		}

		assertEquals(JarType.values().length, seen.size());
	}

	@Test
	public void thePictureHasAClearRimAroundIt()
	{
		BufferedImage image = paint(JarType.DRAGON, 0, 0);
		for (int i = 0; i < SIZE; i++)
		{
			assertEquals(0, image.getRGB(i, 0) >>> 24);
			assertEquals(0, image.getRGB(i, SIZE - 1) >>> 24);
			assertEquals(0, image.getRGB(0, i) >>> 24);
			assertEquals(0, image.getRGB(SIZE - 1, i) >>> 24);
		}
	}

	@Test
	public void theCorkIsPaintedOverTheGlassWhenItIsNearer()
	{
		BufferedImage towards = paint(JarType.EMBER, 0, 0);
		int[] pixels = towards.getRGB(0, 0, SIZE, SIZE / 3, null, 0, SIZE);
		assertTrue("the orange cork shows at the top", Arrays.stream(pixels)
				.anyMatch(p -> p >>> 24 > 200 && (p >> 16 & 255) > 180 && (p & 255) < 60));
	}
}
