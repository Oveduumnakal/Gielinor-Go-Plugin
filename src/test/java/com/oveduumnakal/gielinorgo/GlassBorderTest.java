/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the thin metal border of glass buttons and pop-ups and the setting that picks its metal. */
public class GlassBorderTest
{
	private static final Rectangle2D BOX = new Rectangle2D.Double(40, 40, 160, 60);

	private static BufferedImage button(WindowTheme theme, boolean disabled)
	{
		BufferedImage canvas = WidgetKit.canvas(240, 140);
		Graphics2D g = WidgetKit.graphics(canvas);
		WindowStyle.of(theme, 1.0).button(g, BOX, "Rename", false, disabled, 0, 0);
		g.dispose();
		return canvas;
	}

	private static BufferedImage popup(WindowTheme theme, String surface)
	{
		GlassPanel.clear();
		BufferedImage canvas = WidgetKit.canvas(240, 140);
		Graphics2D g = WidgetKit.graphics(canvas);
		WindowStyle.of(theme, 1.0).popup(g, BOX, surface, ThemeColor.TOOLTIP, ThemeColor.PANEL_BORDER, false,
			Collections.emptyList());
		g.dispose();
		return canvas;
	}

	/** The largest amount any pixel is warmer (positive) or cooler (negative) than grey, as red minus blue. */
	private static int strongestTint(BufferedImage image, boolean warm)
	{
		return strongestTint(image, warm, 128);
	}

	private static int strongestTint(BufferedImage image, boolean warm, int leastAlpha)
	{
		int best = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) < leastAlpha)
					continue;

				int warmth = (argb >> 16 & 0xFF) - (argb & 0xFF);
				best = Math.max(best, warm ? warmth : -warmth);
			}
		}

		return best;
	}

	@Test
	public void bronzeButtonsWearAWarmBorder()
	{
		BufferedImage image = button(WindowTheme.RUNELITE_GLASS, false);

		assertTrue(strongestTint(image, true) > 60);
		assertTrue(strongestTint(image, false) < 20);
	}

	@Test
	public void steelBlueButtonsWearACoolBorder()
	{
		BufferedImage image = button(WindowTheme.RUNELITE_GLASS_STEEL, false);

		assertTrue(strongestTint(image, false) > 60);
		assertTrue(strongestTint(image, true) < 20);
	}

	/** The strongest opacity of any pixel that is clearly warm: the border line, not the grey glass. */
	private static int warmLineAlpha(BufferedImage image)
	{
		int best = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int argb = image.getRGB(x, y);
				if ((argb >> 16 & 0xFF) - (argb & 0xFF) > 60)
					best = Math.max(best, argb >>> 24);
			}
		}

		return best;
	}

	@Test
	public void aDisabledButtonKeepsAFaintBorder()
	{
		int firm = warmLineAlpha(button(WindowTheme.RUNELITE_GLASS, false));
		int faint = warmLineAlpha(button(WindowTheme.RUNELITE_GLASS, true));

		assertTrue("firm " + firm, firm > 200);
		assertTrue("faint " + faint, faint > 60 && faint < 160);
	}

	@Test
	public void tooltipsAndMenusWearTheBorderToo()
	{
		for (String surface : new String[] {"tooltip", "menu", "modal"})
		{
			assertTrue(surface, strongestTint(popup(WindowTheme.RUNELITE_GLASS, surface), true) > 60);
			assertTrue(surface, strongestTint(popup(WindowTheme.RUNELITE_GLASS_STEEL, surface), false) > 60);
		}
	}

	@Test
	public void theSolidLookHasNoMetalBorder()
	{
		assertTrue(strongestTint(button(WindowTheme.RUNELITE_DARK, false), true) < 60);
		assertTrue(strongestTint(popup(WindowTheme.RUNELITE_DARK, "tooltip"), true) < 60);
		assertTrue(strongestTint(popup(WindowTheme.RUNELITE_DARK, "tooltip"), false) < 60);
	}

	@Test
	public void theSettingHasTwoMetalsAndBronzeIsFirst()
	{
		assertEquals(2, GlassBorder.values().length);
		assertEquals(GlassBorder.BRONZE, GlassBorder.values()[0]);
		assertEquals("Bronze", GlassBorder.BRONZE.toString());
		assertEquals("Steel blue", GlassBorder.STEEL_BLUE.toString());
		assertFalse(GlassBorder.BRONZE.high().equals(GlassBorder.STEEL_BLUE.high()));
	}
}
