/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks the X that Close and the change log's Back are drawn with (issue 320). */
public class CrossIconTest
{
	private static BufferedImage paint(int canvas, double at, double size)
	{
		BufferedImage image = WidgetKit.canvas(canvas, canvas);
		Graphics2D g = WidgetKit.graphics(image);
		g.setColor(Color.WHITE);
		Icons.cross(g, at, at, size);
		g.dispose();
		return image;
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	@Test
	public void theXPaintsSomethingAtEverySize()
	{
		for (int size : new int[] {8, 10, 12, 16, 24, 48, 96})
			assertTrue("size " + size, WidgetKit.painted(paint(size, 0, size), 0, 0, size, size) > 0);
	}

	@Test
	public void theXStaysInsideItsSquare()
	{
		int size = 24;
		int pad = 8;
		BufferedImage image = paint(size + 2 * pad, pad, size);

		assertEquals(0, WidgetKit.painted(image, 0, 0, pad, size + 2 * pad));
		assertEquals(0, WidgetKit.painted(image, size + pad, 0, pad, size + 2 * pad));
		assertEquals(0, WidgetKit.painted(image, 0, 0, size + 2 * pad, pad));
		assertEquals(0, WidgetKit.painted(image, 0, size + pad, size + 2 * pad, pad));
	}

	@Test
	public void theXIsTheSameAcrossBothDiagonalsAndLeavesTheMiddleOfEachSideEmpty()
	{
		int size = 48;
		BufferedImage image = paint(size, 0, size);

		for (int i = 4; i < size - 4; i += 4)
		{
			assertEquals("main diagonal and its mirror at " + i, alpha(image, i, i), alpha(image, size - 1 - i,
				size - 1 - i), 40);
			assertTrue("both strokes cross the middle", alpha(image, size / 2, size / 2) > 0);
		}

		assertEquals(0, alpha(image, size / 2, 2));
		assertEquals(0, alpha(image, 2, size / 2));
		assertEquals(0, alpha(image, size - 3, size / 2));
		assertEquals(0, alpha(image, size / 2, size - 3));
	}
}
