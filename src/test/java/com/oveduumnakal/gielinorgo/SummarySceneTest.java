/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests where the layers of the Summary's scene go and how they shift. */
public class SummarySceneTest
{
	private static final Rectangle2D BOX = new Rectangle2D.Double(10, 20, 410, 305);

	@Test
	public void theBackgroundCoversTheBoxHoweverTheMouseShiftsIt()
	{
		for (double x = -1; x <= 1; x += 0.5)
		{
			for (double y = -1; y <= 1; y += 0.5)
			{
				Rectangle2D drawn = SummaryScene.cover(BOX, 480, 264, x, y);
				assertTrue(x + "," + y, drawn.contains(BOX));
			}
		}

		assertEquals(480.0 / 264, SummaryScene.cover(BOX, 480, 264, 0, 0).getWidth()
			/ SummaryScene.cover(BOX, 480, 264, 0, 0).getHeight(), 1e-9);
	}

	@Test
	public void theBackgroundMovesAgainstTheMouse()
	{
		double middle = SummaryScene.cover(BOX, 480, 264, 0, 0).getX();
		assertTrue(SummaryScene.cover(BOX, 480, 264, 1, 0).getX() < middle);
		assertEquals(0.025 * 410, middle - SummaryScene.cover(BOX, 480, 264, 1, 0).getX(), 1e-9);
	}

	@Test
	public void theCreatureStandsAtNineTenthsAndStaysUnderTheChips()
	{
		for (Rectangle shape : new Rectangle[] {new Rectangle(0, 0, 40, 180), new Rectangle(0, 0, 180, 40)})
		{
			Rectangle2D stood = SummaryScene.place(BOX, 60, shape);
			assertEquals(20 + 305 * 0.9, stood.getMaxY(), 1e-9);
			assertTrue(stood.getY() >= 60);
			assertTrue(stood.getWidth() <= 410 * 0.65 + 1e-9);
			assertEquals(BOX.getCenterX(), stood.getCenterX(), 1e-9);
		}
	}

	@Test
	public void theEmptyMarginOfAPortraitIsTrimmed()
	{
		BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(5, 7, 0xFFFFFFFF);
		image.setRGB(9, 12, 0xFFFFFFFF);

		assertEquals(new Rectangle(5, 7, 5, 6), SummaryScene.trimmed(image));
		assertEquals(new Rectangle(0, 0, 4, 4),
			SummaryScene.trimmed(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)));
	}

	@Test
	public void theShiftGlidesTowardTheMouseAndBack()
	{
		Parallax parallax = new Parallax();
		parallax.toward(1, -1, 0);
		assertEquals(0, parallax.x(), 0.0);

		parallax.toward(1, -1, 100);
		assertTrue(parallax.x() > 0.3 && parallax.x() < 1);
		assertEquals(-parallax.x(), parallax.y(), 1e-9);

		parallax.toward(5, -5, 100_000);
		assertEquals(1, parallax.x(), 1e-6);

		parallax.toward(0, 0, 200_000);
		assertEquals(0, parallax.x(), 1e-6);
		parallax.rest();
		assertEquals(0, parallax.y(), 0.0);
	}
}
