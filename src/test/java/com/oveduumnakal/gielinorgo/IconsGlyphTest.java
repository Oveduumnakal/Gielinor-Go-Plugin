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

/** Checks that the swords and info glyphs draw something inside their square and nothing outside it. */
public class IconsGlyphTest
{
	private static final int MARGIN = 10;

	/** Draws one glyph. */
	private interface Glyph
	{
		/** Draws the glyph in the square. */
		void draw(Graphics2D g, int x, int y, int size);
	}

	private static void check(Glyph glyph, int size)
	{
		int canvas = size + 2 * MARGIN;
		BufferedImage image = WidgetKit.canvas(canvas, canvas);
		Graphics2D g = WidgetKit.graphics(image);
		g.setColor(Color.WHITE);
		glyph.draw(g, MARGIN, MARGIN, size);
		g.dispose();

		int inside = WidgetKit.painted(image, MARGIN, MARGIN, size, size);
		int total = WidgetKit.painted(image, 0, 0, canvas, canvas);

		assertTrue("nothing painted at size " + size, inside > size);
		assertEquals("painted outside the square at size " + size, inside, total);
	}

	@Test
	public void swordsStayInsideTheirSquare()
	{
		for (int size : new int[] {12, 16, 18, 32, 48})
			check(Icons::swords, size);
	}

	@Test
	public void infoStaysInsideItsSquare()
	{
		for (int size : new int[] {12, 16, 17, 32, 48})
			check(Icons::info, size);
	}

	@Test
	public void genderSignsStayInsideTheirSquareAndLeaveTheSameMarginAsStylePictures()
	{
		for (int size : new int[] {16, 32, 64, 128})
		{
			check((g, x, y, s) -> Icons.gender(g, Gender.MALE, x, y, s), size);
			check((g, x, y, s) -> Icons.gender(g, Gender.FEMALE, x, y, s), size);
		}

		BufferedImage image = WidgetKit.canvas(128, 128);
		Graphics2D g = WidgetKit.graphics(image);
		g.setColor(Color.WHITE);
		Icons.gender(g, Gender.MALE, 0, 0, 128);
		g.dispose();
		assertEquals(0, WidgetKit.painted(image, 0, 0, 4, 128));
		assertEquals(0, WidgetKit.painted(image, 124, 0, 4, 128));
	}
}
