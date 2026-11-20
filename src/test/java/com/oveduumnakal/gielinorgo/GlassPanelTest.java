/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Tests the glass body: its cached still layer, its see-through tint. */
public class GlassPanelTest
{
	private static final WindowStyle STYLE = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);

	private static Shape body(int x, int y, int w, int h)
	{
		return Shapes.roundedSlant(new Rectangle2D.Double(x, y, w, h), 14, h * GlassTokens.TIP_ROUND);
	}

	@Test
	public void theStillLayerIsPaintedOnceForTheSameForm()
	{
		GlassPanel.clear();
		BufferedImage canvas = WidgetKit.canvas(400, 300);
		Graphics2D g = WidgetKit.graphics(canvas);
		int before = GlassPanel.paints();
		GlassPanel.paint(g, body(10, 10, 200, 120), STYLE, null, Readability.BODY);
		GlassPanel.paint(g, body(40, 30, 200, 120), STYLE, null, Readability.BODY);
		assertEquals(before + 1, GlassPanel.paints());
		GlassPanel.paint(g, body(40, 30, 210, 120), STYLE, null, Readability.BODY);
		assertEquals(before + 2, GlassPanel.paints());
		WindowStyle bigger = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.5);
		GlassPanel.paint(g, body(40, 30, 210, 120), bigger, null, Readability.BODY);
		assertEquals(before + 3, GlassPanel.paints());
		g.dispose();
	}

	@Test
	public void theTintIsSeeThroughAndThickerWithoutAWash()
	{
		assertTrue(GlassPanel.tint(STYLE, GlassTokens.TINT_SOLID).getAlpha() < 255);
		assertTrue(GlassPanel.tint(STYLE, GlassTokens.TINT_OVER_WASH).getAlpha()
			< GlassPanel.tint(STYLE, GlassTokens.TINT_SOLID).getAlpha());
	}

	@Test
	public void theSceneShowsThroughTheBody()
	{
		Shape shape = body(30, 30, 160, 100);
		BufferedImage bright = WidgetKit.canvas(260, 200);
		BufferedImage dark = WidgetKit.canvas(260, 200);
		for (BufferedImage image : new BufferedImage[] {bright, dark})
		{
			Graphics2D g = image.createGraphics();
			g.setColor(image == bright ? java.awt.Color.WHITE : java.awt.Color.BLACK);
			g.fillRect(0, 0, 260, 200);
			WindowStyle.prepare(g);
			GlassPanel.paint(g, shape, STYLE, null, Readability.BODY);
			g.dispose();
		}

		assertNotEquals(bright.getRGB(110, 80), dark.getRGB(110, 80));
	}

	@Test
	public void aWashIsDrawnInsideTheBodyOnly()
	{
		Shape shape = body(30, 30, 160, 100);
		BufferedImage wash = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 4; y++)
		{
			for (int x = 0; x < 4; x++)
				wash.setRGB(x, y, 0x2040C0);
		}

		BufferedImage image = WidgetKit.canvas(260, 200);
		Graphics2D g = WidgetKit.graphics(image);
		GlassPanel.paint(g, shape, STYLE, wash, Readability.BODY);
		g.dispose();
		int inside = image.getRGB(110, 80);
		assertTrue((inside & 0xFF) > (inside >> 16 & 0xFF));
		assertEquals(0, image.getRGB(3, 3) >>> 24);
		assertEquals(0, image.getRGB(250, 3) >>> 24);
	}

	@Test
	public void thePaddingHoldsTheLargestShadow()
	{
		Rectangle box = new Rectangle(0, 0, 10, 10);
		assertTrue(box.width > 0);
		assertTrue(GlassPanel.pad(STYLE) >= GlassStyle.px(STYLE, GlassTokens.LARGE_DARK_DY
			+ GlassTokens.LARGE_DARK_BLUR));
	}
}
