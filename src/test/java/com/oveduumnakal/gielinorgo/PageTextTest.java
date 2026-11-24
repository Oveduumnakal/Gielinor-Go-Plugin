/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks the room {@link PageText} keeps free at the sides of a line of text. */
public class PageTextTest
{
	private final WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
	private final PageText text = new PageText(style, PageText.Sink.NONE);

	private static int leftmost(BufferedImage image)
	{
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++)
				if (image.getRGB(x, y) >>> 24 > 0)
					return x;

		return -1;
	}

	@Test
	public void theInsetIsAQuarterOfThePadAndATooNarrowBoxKeepsNone()
	{
		assertEquals(Math.max(PageText.MIN_INSET, style.metrics().pad / 4), text.inset(new Rectangle(0, 0, 100, 20)));
		assertEquals(0, text.inset(new Rectangle(0, 0, 10, 20)));
	}

	@Test
	public void leftTextStaysOffTheEdgeOfItsBox()
	{
		BufferedImage image = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		WindowStyle.prepare(g);
		Font font = style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Rectangle box = new Rectangle(0, 0, 200, 40);
		text.draw(g, "Lead", box, font, ThemeColor.TEXT, PageText.Align.LEFT);
		g.dispose();

		assertTrue(leftmost(image) >= text.inset(box));
	}

	@Test
	public void textThatJustFitsIsNeverCutShortByTheInset()
	{
		BufferedImage image = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		WindowStyle.prepare(g);
		Font font = style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		FontMetrics fm = g.getFontMetrics(font);
		Rectangle box = new Rectangle(0, 0, fm.stringWidth("Lead"), 40);

		assertEquals("Lead", text.draw(g, "Lead", box, font, ThemeColor.TEXT, PageText.Align.LEFT));
		g.dispose();
	}
}
