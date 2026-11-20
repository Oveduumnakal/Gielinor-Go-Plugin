/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests the cached soft shadows. */
public class SoftShadowsTest
{
	private static Shape chip(double x, double y)
	{
		return Shapes.roundedSlant(new Rectangle2D.Double(x, y, 60, 26), 9, 3.9);
	}

	@Test
	public void theSecondPaintOfTheSameShadowDoesNotBlurAgain()
	{
		SoftShadows.clear();
		BufferedImage image = WidgetKit.canvas(200, 100);
		Graphics2D g = WidgetKit.graphics(image);
		SoftShadows.outer(g, chip(20, 20), GlassTokens.black(0.55), 2, 3, 2);
		int after = SoftShadows.blurs();
		SoftShadows.outer(g, chip(20, 20), GlassTokens.black(0.55), 2, 3, 2);
		SoftShadows.outer(g, chip(90.5, 40), GlassTokens.black(0.55), 2, 3, 2);
		g.dispose();
		assertEquals(after, SoftShadows.blurs());
	}

	@Test
	public void anOuterShadowFallsOnlyOutsideAndAnInnerShadowOnlyInside()
	{
		SoftShadows.clear();
		Shape shape = Shapes.roundedSlant(new Rectangle2D.Double(30, 30, 100, 40), 10, 6);
		BufferedImage outer = WidgetKit.canvas(200, 120);
		Graphics2D g = WidgetKit.graphics(outer);
		SoftShadows.outer(g, shape, GlassTokens.black(0.8), 6, 8, 6);
		g.dispose();
		BufferedImage inner = WidgetKit.canvas(200, 120);
		g = WidgetKit.graphics(inner);
		SoftShadows.inner(g, shape, GlassTokens.black(0.8), 2, 2, 2);
		g.dispose();
		assertEquals(0, outer.getRGB(80, 50) >>> 24);
		assertTrue((outer.getRGB(80, 78) >>> 24) > 20);
		assertEquals(0, inner.getRGB(80, 90) >>> 24);
		assertTrue((inner.getRGB(35, 50) >>> 24) > 20 || (inner.getRGB(80, 31) >>> 24) > 20);
	}

	@Test
	public void theCacheKeepsAtMostItsCapacity()
	{
		SoftShadows.clear();
		BufferedImage image = WidgetKit.canvas(40, 40);
		Graphics2D g = WidgetKit.graphics(image);
		for (int i = 0; i < SoftShadows.CAPACITY + 20; i++)
			SoftShadows.outer(g, new Rectangle2D.Double(5, 5, 10 + i, 10), new Color(0, 0, 0, 100), 1, 1, 1);

		g.dispose();
		assertEquals(SoftShadows.CAPACITY, SoftShadows.cached());
	}

	@Test
	public void aCachedChipShadowCostsUnderTwentyMicroseconds()
	{
		SoftShadows.clear();
		BufferedImage image = WidgetKit.canvas(200, 100);
		Graphics2D g = WidgetKit.graphics(image);
		Shape shape = chip(20, 20);
		for (int i = 0; i < 2000; i++)
			SoftShadows.outer(g, shape, GlassTokens.black(0.55), 2, 3, 2);

		int runs = 5000;
		long start = System.nanoTime();
		for (int i = 0; i < runs; i++)
			SoftShadows.outer(g, shape, GlassTokens.black(0.55), 2, 3, 2);

		double micros = (System.nanoTime() - start) / 1000.0 / runs;
		g.dispose();
		System.out.printf("cached chip shadow: %.2f us%n", micros);
		assertTrue("took " + micros, micros < 20);
	}
}
