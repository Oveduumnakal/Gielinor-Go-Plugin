/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks the three small marks that sit beside a wild creature's name. */
public class MarkIconsTest
{
	private static BufferedImage paint(WildRecord.Kind kind, int canvas, int at, int size)
	{
		BufferedImage image = WidgetKit.canvas(canvas, canvas);
		Graphics2D g = WidgetKit.graphics(image);
		g.setColor(Color.WHITE);
		Icons.mark(g, kind, at, at, size);
		g.dispose();
		return image;
	}

	@Test
	public void everyKindPaintsSomething()
	{
		for (WildRecord.Kind kind : WildRecord.Kind.values())
		{
			BufferedImage image = paint(kind, 16, 0, 16);

			assertTrue(kind.name(), WidgetKit.painted(image, 0, 0, 16, 16) > 0);
		}
	}

	@Test
	public void nothingIsPaintedOutsideTheSquare()
	{
		for (WildRecord.Kind kind : WildRecord.Kind.values())
		{
			BufferedImage image = paint(kind, 32, 8, 16);

			assertEquals(kind.name(), 0, WidgetKit.painted(image, 0, 0, 32, 8));
			assertEquals(kind.name(), 0, WidgetKit.painted(image, 0, 24, 32, 8));
			assertEquals(kind.name(), 0, WidgetKit.painted(image, 0, 8, 8, 16));
			assertEquals(kind.name(), 0, WidgetKit.painted(image, 24, 8, 8, 16));
		}
	}

	@Test
	public void theStrokeIsLeftAsFound()
	{
		for (WildRecord.Kind kind : WildRecord.Kind.values())
		{
			BufferedImage image = WidgetKit.canvas(16, 16);
			Graphics2D g = WidgetKit.graphics(image);
			Stroke before = g.getStroke();

			Icons.mark(g, kind, 0, 0, 16);

			assertSame(kind.name(), before, g.getStroke());
			g.dispose();
		}
	}

	@Test
	public void theEyeAndTheJarAreDrawnDirectlyToo()
	{
		BufferedImage image = WidgetKit.canvas(16, 16);
		Graphics2D g = WidgetKit.graphics(image);
		g.setColor(Color.WHITE);
		Icons.eye(g, 0, 0, 16);
		Icons.jar(g, 0, 0, 16);
		g.dispose();

		assertTrue(WidgetKit.painted(image, 0, 0, 16, 16) > 0);
	}

	@Test
	public void theThreeKindsPaintDifferentPixels()
	{
		BufferedImage fresh = paint(WildRecord.Kind.NEW, 16, 0, 16);
		BufferedImage seen = paint(WildRecord.Kind.SEEN, 16, 0, 16);
		BufferedImage caught = paint(WildRecord.Kind.CAUGHT, 16, 0, 16);

		assertTrue(differs(fresh, seen));
		assertTrue(differs(fresh, caught));
		assertTrue(differs(seen, caught));
	}

	@Test
	public void theCaughtMarkIsACheckmarkNotAJar()
	{
		BufferedImage caught = paint(WildRecord.Kind.CAUGHT, 16, 0, 16);
		BufferedImage check = WidgetKit.canvas(16, 16);
		Graphics2D g = WidgetKit.graphics(check);
		g.setColor(Color.WHITE);
		Icons.check(g, 0, 0, 16);
		g.dispose();
		BufferedImage jar = WidgetKit.canvas(16, 16);
		g = WidgetKit.graphics(jar);
		g.setColor(Color.WHITE);
		Icons.jar(g, 0, 0, 16);
		g.dispose();

		assertFalse(differs(caught, check));
		assertTrue(differs(caught, jar));
	}

	private static boolean differs(BufferedImage a, BufferedImage b)
	{
		for (int x = 0; x < a.getWidth(); x++)
			for (int y = 0; y < a.getHeight(); y++)
				if (a.getRGB(x, y) != b.getRGB(x, y))
					return true;

		return false;
	}
}
