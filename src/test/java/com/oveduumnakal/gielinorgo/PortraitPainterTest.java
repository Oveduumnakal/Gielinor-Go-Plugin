/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks that portraits are drawn from a model's numbers: shape, colour, depth, shading and hidden faces. */
public class PortraitPainterTest
{
	private static final int RED = (0 << 10) | (7 << 7) | 64;
	private static final int GREEN = (21 << 10) | (7 << 7) | 64;
	private static final int BLUE = (42 << 10) | (7 << 7) | 64;
	private static final int SIZE = 40;

	@Test
	public void aSolidShapeFillsTheImageAndLeavesTheCornersClear()
	{
		BufferedImage image = PortraitPainter.paint(pyramid(RED, PortraitPainter.FLAT, null), SIZE);

		assertNotNull(image);
		assertEquals(SIZE, image.getWidth());
		assertEquals(SIZE, image.getHeight());
		int painted = paintedPixels(image);
		assertTrue("expected a good share of 1600 pixels painted, got " + painted, painted > 400);
		assertEquals("the top corners stay see-through", 0, image.getRGB(0, 0) >>> 24);
		assertEquals("the top corners stay see-through", 0, image.getRGB(SIZE - 1, 0) >>> 24);
	}

	@Test
	public void theModelStandsUpright()
	{
		BufferedImage image = PortraitPainter.paint(pyramid(RED, PortraitPainter.FLAT, null), SIZE);

		assertTrue("a pyramid is narrow at the top", paintedInRow(image, 5) < paintedInRow(image, SIZE - 8));
	}

	@Test
	public void flatTrianglesUseTheirFirstColour()
	{
		BufferedImage image = PortraitPainter.paint(pyramid(GREEN, PortraitPainter.FLAT, null), SIZE);
		Color middle = new Color(image.getRGB(SIZE / 2, SIZE / 2 + 4), true);

		assertTrue("expected green in the middle, got " + middle, middle.getGreen() > middle.getRed() + 80);
		assertTrue("expected green in the middle, got " + middle, middle.getGreen() > middle.getBlue() + 80);
	}

	@Test
	public void cornerColoursBlendAcrossATriangle()
	{
		float[] x = {-50, 50, 0};
		float[] y = {0, 0, -100};
		float[] z = {0, 0, 0};
		int[][] corners = {{0}, {1}, {2}};
		int[][] colors = {{RED}, {BLUE}, {GREEN}};
		BufferedImage image = PortraitPainter.paint(new PortraitPainter.Mesh(x, y, z, corners, colors, null), SIZE);

		Color left = firstPaintedInRow(image, SIZE - 12, true);
		Color right = firstPaintedInRow(image, SIZE - 12, false);
		Color top = firstPaintedInRow(image, 8, true);
		assertTrue("the left side is redder than the right: " + left + " " + right, left.getRed() > right.getRed());
		assertTrue("the right side is bluer than the left: " + left + " " + right, right.getBlue() > left.getBlue());
		assertTrue("the top is greener than the left: " + top, top.getGreen() > left.getGreen());
		assertTrue("the top is greener than the right: " + top, top.getGreen() > right.getGreen());
	}

	@Test
	public void nearerSurfacesHideFartherOnes()
	{
		float[] x = {-50, 50, 0, -50, 50, 0};
		float[] y = {0, 0, -100, 0, 0, -100};
		float[] z = {-40, -40, -40, 40, 40, 40};
		int[][] cornersNearFirst = {{0, 3}, {1, 4}, {2, 5}};
		int[][] colorsNearFirst = {{RED, BLUE}, {RED, BLUE}, {PortraitPainter.FLAT, PortraitPainter.FLAT}};
		int[][] cornersFarFirst = {{3, 0}, {4, 1}, {5, 2}};
		int[][] colorsFarFirst = {{BLUE, RED}, {BLUE, RED}, {PortraitPainter.FLAT, PortraitPainter.FLAT}};

		BufferedImage first = PortraitPainter.paint(
			new PortraitPainter.Mesh(x, y, z, cornersNearFirst, colorsNearFirst, null), SIZE);
		BufferedImage second = PortraitPainter.paint(
			new PortraitPainter.Mesh(x, y, z, cornersFarFirst, colorsFarFirst, null), SIZE);

		assertTrue("the order triangles are listed in must not change the picture", sameImage(first, second));
	}

	@Test
	public void packedColoursConvertToRedGreenAndBlue()
	{
		Color red = PortraitPainter.color(RED);
		assertTrue("got " + red, red.getRed() > 200 && red.getGreen() < 80 && red.getBlue() < 80);

		Color green = PortraitPainter.color(GREEN);
		assertTrue("got " + green, green.getGreen() > 200 && green.getRed() < 80 && green.getBlue() < 80);

		Color dark = PortraitPainter.color(0);
		assertTrue("got " + dark, dark.getRed() < 10 && dark.getGreen() < 10 && dark.getBlue() < 10);

		Color light = PortraitPainter.color(127);
		assertTrue("got " + light, light.getRed() > 240 && light.getGreen() > 240 && light.getBlue() > 240);
	}

	@Test
	public void hiddenAndInvisibleFacesAreNotDrawn()
	{
		assertNull(PortraitPainter.paint(pyramid(RED, PortraitPainter.HIDDEN, null), SIZE));

		byte[] invisible = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
		assertNull(PortraitPainter.paint(pyramid(RED, PortraitPainter.FLAT, invisible), SIZE));
	}

	@Test
	public void seeThroughFacesAreFainterThanSolidOnes()
	{
		byte[] half = {(byte) 128, (byte) 128, (byte) 128, (byte) 128};
		BufferedImage solid = PortraitPainter.paint(pyramid(RED, PortraitPainter.FLAT, null), SIZE);
		BufferedImage faint = PortraitPainter.paint(pyramid(RED, PortraitPainter.FLAT, half), SIZE);

		int solidAlpha = solid.getRGB(SIZE / 2, SIZE / 2 + 4) >>> 24;
		int faintAlpha = faint.getRGB(SIZE / 2, SIZE / 2 + 4) >>> 24;
		assertEquals(255, solidAlpha);
		assertTrue("expected a part see-through pixel, got alpha " + faintAlpha, faintAlpha > 60 && faintAlpha < 250);
	}

	@Test
	public void anEmptyOrBrokenModelGivesNoPicture()
	{
		int[][] none = {new int[0], new int[0], new int[0]};
		PortraitPainter.Mesh empty = new PortraitPainter.Mesh(new float[0], new float[0], new float[0], none, none,
			null);
		assertNull(PortraitPainter.paint(empty, SIZE));

		float[] one = {0};
		int[][] outOfRange = {{5}, {5}, {5}};
		int[][] colors = {{RED}, {RED}, {PortraitPainter.FLAT}};
		assertNull(PortraitPainter.paint(new PortraitPainter.Mesh(one, one, one, outOfRange, colors, null), SIZE));
	}

	/**
	 * Builds a four-sided pyramid about 100 units tall, standing the way game models do: up is negative y.
	 *
	 * @param color the packed colour of every side
	 * @param third the third corner colour of every side: a colour, or one of the painter's special values
	 * @param transparencies per-side transparency, or null for solid
	 * @return the model
	 */
	private static PortraitPainter.Mesh pyramid(int color, int third, byte[] transparencies)
	{
		float[] x = {-50, 50, 50, -50, 0};
		float[] y = {0, 0, 0, 0, -100};
		float[] z = {-50, -50, 50, 50, 0};
		int[][] corners = {{0, 1, 2, 3}, {1, 2, 3, 0}, {4, 4, 4, 4}};
		int[] same = {color, color, color, color};
		int[] thirds = new int[4];
		Arrays.fill(thirds, third);
		int[][] colors = {same, same, thirds};
		return new PortraitPainter.Mesh(x, y, z, corners, colors, transparencies);
	}

	private static int paintedPixels(BufferedImage image)
	{
		int painted = 0;
		for (int row = 0; row < image.getHeight(); row++)
			painted += paintedInRow(image, row);

		return painted;
	}

	private static int paintedInRow(BufferedImage image, int row)
	{
		int painted = 0;
		for (int column = 0; column < image.getWidth(); column++)
		{
			if (image.getRGB(column, row) >>> 24 > 128)
				painted++;
		}

		return painted;
	}

	private static Color firstPaintedInRow(BufferedImage image, int row, boolean fromLeft)
	{
		for (int step = 0; step < image.getWidth(); step++)
		{
			int column = fromLeft ? step : image.getWidth() - 1 - step;
			if (image.getRGB(column, row) >>> 24 == 255)
				return new Color(image.getRGB(column, row));
		}

		throw new AssertionError("row " + row + " has no solid pixel");
	}

	private static boolean sameImage(BufferedImage first, BufferedImage second)
	{
		for (int row = 0; row < first.getHeight(); row++)
		{
			for (int column = 0; column < first.getWidth(); column++)
			{
				if (first.getRGB(column, row) != second.getRGB(column, row))
					return false;
			}
		}

		return true;
	}
}
