/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks the window's depth: an element with no depth amounts draws nothing, and each effect lands on the side the
 * light puts it, lit from the top left.
 */
public class WindowDepthTest
{
	private static final int SIZE = 100;
	private static final int GREY = 128;
	private static final Rectangle BOX = new Rectangle(30, 30, 30, 30);

	private static BufferedImage canvas(boolean grey)
	{
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		if (grey)
		{
			Graphics2D g = image.createGraphics();
			g.setColor(new Color(GREY, GREY, GREY));
			g.fillRect(0, 0, SIZE, SIZE);
			g.dispose();
		}

		return image;
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}

	private static int red(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >> 16 & 0xFF;
	}

	@Test
	public void anElementWithNoAmountsDrawsNothing()
	{
		WindowDepth depth = new WindowDepth(1.0);
		for (WindowDepth.Element element : WindowDepth.Element.values())
		{
			if (depth.shows(element))
				continue;

			BufferedImage image = canvas(false);
			Graphics2D g = image.createGraphics();
			depth.under(g, element, BOX, WindowDepth.Pane.NONE);
			depth.over(g, element, BOX, WindowDepth.Pane.NONE);
			g.dispose();
			for (int y = 0; y < SIZE; y++)
			{
				for (int x = 0; x < SIZE; x++)
					assertEquals(element + " at " + x + "," + y, 0, alpha(image, x, y));
			}
		}
	}

	@Test
	public void aSideFallsAwayFromTheLightAndLeavesTheFaceClear()
	{
		BufferedImage image = canvas(false);
		Graphics2D g = image.createGraphics();
		new WindowDepth(1.0).under(g, BOX, 0, 4, 0);
		g.dispose();
		assertTrue("below right of the box", alpha(image, 62, 62) > 0);
		assertEquals("on the face", 0, alpha(image, 45, 45));
		assertEquals("above left of the box", 0, alpha(image, 27, 27));
	}

	@Test
	public void aShadowFallsAwayFromTheLight()
	{
		BufferedImage image = canvas(false);
		Graphics2D g = image.createGraphics();
		new WindowDepth(1.0).under(g, BOX, 0, 0, 6);
		g.dispose();
		assertTrue("below right of the box", alpha(image, 64, 64) > 0);
		assertEquals("on the face", 0, alpha(image, 45, 45));
		assertEquals("far above left", 0, alpha(image, 15, 15));
	}

	@Test
	public void aBevelLightsTheNearEdgeAndShadesTheFarOne()
	{
		BufferedImage image = canvas(true);
		Graphics2D g = image.createGraphics();
		new WindowDepth(1.0).over(g, BOX, 0, 3);
		g.dispose();
		assertTrue("left edge lit", red(image, 31, 45) > GREY);
		assertTrue("right edge shaded", red(image, 58, 45) < GREY);
		assertEquals("middle untouched", GREY, red(image, 45, 45));
	}

	@Test
	public void aRaisedPaneShadowsALowerOneBesideIt()
	{
		BufferedImage image = canvas(true);
		Graphics2D g = image.createGraphics();
		WindowDepth depth = new WindowDepth(1.0);
		depth.remember(g, new Rectangle(30, 30, 20, 20), 2);
		depth.over(g, new Rectangle(52, 30, 30, 40), 0, 0);
		g.dispose();
		assertTrue("next to the raised pane", red(image, 53, 45) < GREY);
		assertEquals("far side of the lower pane", GREY, red(image, 78, 45));
	}

	@Test
	public void aPaneDoesNotShadowAHigherOne()
	{
		BufferedImage image = canvas(true);
		Graphics2D g = image.createGraphics();
		WindowDepth depth = new WindowDepth(1.0);
		depth.remember(g, new Rectangle(30, 30, 20, 20), 1);
		depth.over(g, new Rectangle(52, 30, 30, 40), 2, 0);
		g.dispose();
		assertEquals(GREY, red(image, 53, 45));
	}

	@Test
	public void aSunkPaneIsShadedInsideItsLitEdges()
	{
		BufferedImage image = canvas(true);
		Graphics2D g = image.createGraphics();
		new WindowDepth(1.0).over(g, BOX, -2, 0);
		g.dispose();
		assertTrue("inside the top left edge", red(image, 32, 45) < GREY);
		assertEquals("deep inside, bottom right", GREY, red(image, 57, 57));
		assertEquals("outside the pane", GREY, red(image, 20, 45));
	}
}
