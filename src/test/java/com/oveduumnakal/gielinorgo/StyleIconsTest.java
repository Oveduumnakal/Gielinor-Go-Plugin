/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Checks the five combat style icons cut from the owner's artwork (issue 258). */
public class StyleIconsTest
{
	private static BufferedImage paint(CombatType style, Color colour, int canvas, int at, int size)
	{
		BufferedImage image = WidgetKit.canvas(canvas, canvas);
		Graphics2D g = WidgetKit.graphics(image);
		g.setColor(colour);
		Icons.style(g, style, at, at, size);
		g.dispose();
		return image;
	}

	private static CombatType[] styles()
	{
		return new CombatType[]{CombatType.STAB, CombatType.SLASH, CombatType.CRUSH, CombatType.RANGED,
			CombatType.MAGIC};
	}

	@Test
	public void everyStyleHasABundledPicture()
	{
		for (CombatType style : styles())
		{
			assertTrue(style.name(), StyleIcons.available(style));
			assertEquals("styles/" + style.name().toLowerCase(Locale.ROOT) + ".png",
				StyleIcons.resourceName(style));
		}
	}

	@Test
	public void everyStylePaintsSomethingAtEverySize()
	{
		for (CombatType style : styles())
		{
			for (int size : new int[]{10, 12, 16, 24, 48, 96})
			{
				BufferedImage image = paint(style, Color.WHITE, size, 0, size);

				assertTrue(style + " at " + size, WidgetKit.painted(image, 0, 0, size, size) > 0);
			}
		}
	}

	@Test
	public void theFiveIconsLookDifferent()
	{
		Set<Integer> hashes = new HashSet<>();
		for (CombatType style : styles())
			hashes.add(Arrays.hashCode(paint(style, Color.WHITE, 32, 0, 32).getRGB(0, 0, 32, 32, null, 0, 32)));

		assertEquals(5, hashes.size());
	}

	@Test
	public void theIconTakesTheColourTheCallerSet()
	{
		Color gold = new Color(200, 120, 20);
		BufferedImage image = paint(CombatType.CRUSH, gold, 32, 0, 32);

		int seen = 0;
		for (int x = 0; x < 32; x++)
		{
			for (int y = 0; y < 32; y++)
			{
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) == 0)
					continue;

				if ((argb >>> 24) < 32)
					continue;

				seen++;
				assertEquals(gold.getRed(), (argb >> 16) & 0xFF, 4);
				assertEquals(gold.getGreen(), (argb >> 8) & 0xFF, 4);
				assertEquals(gold.getBlue(), argb & 0xFF, 4);
			}
		}

		assertTrue(seen > 20);
		assertNotEquals(Arrays.hashCode(image.getRGB(0, 0, 32, 32, null, 0, 32)),
			Arrays.hashCode(paint(CombatType.CRUSH, Color.BLUE, 32, 0, 32).getRGB(0, 0, 32, 32, null, 0, 32)));
	}

	@Test
	public void theIconStaysInsideItsSquare()
	{
		for (CombatType style : styles())
		{
			BufferedImage image = paint(style, Color.WHITE, 60, 20, 20);

			assertEquals(style.name() + " left", 0, WidgetKit.painted(image, 0, 0, 20, 60));
			assertEquals(style.name() + " right", 0, WidgetKit.painted(image, 40, 0, 20, 60));
			assertEquals(style.name() + " above", 0, WidgetKit.painted(image, 0, 0, 60, 20));
			assertEquals(style.name() + " below", 0, WidgetKit.painted(image, 0, 40, 60, 20));
			assertTrue(style.name() + " inside", WidgetKit.painted(image, 20, 20, 20, 20) > 0);
		}
	}

	@Test
	public void theBundledPicturesAreOnTheClasspath()
	{
		for (CombatType style : styles())
			assertTrue(style.name(), StyleIcons.class.getResource(StyleIcons.resourceName(style)) != null);
	}
}
