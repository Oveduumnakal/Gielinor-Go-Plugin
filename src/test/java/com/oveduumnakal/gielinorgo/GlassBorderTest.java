/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
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

	private static WindowStyle style(WindowTheme theme)
	{
		return WindowStyle.of(theme, 1.0);
	}

	private static double luminance(int argb)
	{
		return 0.299 * (argb >> 16 & 0xFF) + 0.587 * (argb >> 8 & 0xFF) + 0.114 * (argb & 0xFF);
	}

	/** Gives the luminance of the first (top) and last (bottom) clearly coloured pixel in a column. */
	private static double[] edgeLuminance(BufferedImage image, int x, boolean warm)
	{
		double top = -1;
		double bottom = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			int argb = image.getRGB(x, y);
			int tint = (argb >> 16 & 0xFF) - (argb & 0xFF);
			if ((argb >>> 24) >= 128 && (warm ? tint : -tint) > 40)
			{
				if (top < 0)
					top = luminance(argb);

				bottom = luminance(argb);
			}
		}

		return new double[] {top, bottom};
	}

	@Test
	public void aPlainRaisedTabIsLightAtTheTopEdgeAndAnActiveOneIsDarkThere()
	{
		for (boolean active : new boolean[] {false, true})
		{
			BufferedImage canvas = WidgetKit.canvas(240, 140);
			Graphics2D g = WidgetKit.graphics(canvas);
			style(WindowTheme.RUNELITE_GLASS).tab(g, BOX, "Party", active, false, 0);
			g.dispose();
			double[] edge = edgeLuminance(canvas, 120, true);

			assertTrue("found the border", edge[0] > 0 && edge[1] > 0);
			if (active)
				assertTrue("inset " + edge[0] + " " + edge[1], edge[0] < edge[1]);
			else
				assertTrue("raised " + edge[0] + " " + edge[1], edge[0] > edge[1]);
		}
	}

	@Test
	public void aPlainRowIsInsetAndTheLeadRowIsMetal()
	{
		BufferedImage plain = WidgetKit.canvas(240, 140);
		Graphics2D g = WidgetKit.graphics(plain);
		style(WindowTheme.RUNELITE_GLASS).row(g, BOX, false, 0, 0);
		g.dispose();
		double[] edge = edgeLuminance(plain, 120, true);
		assertTrue("inset " + edge[0] + " " + edge[1], edge[0] > 0 && edge[0] < edge[1]);

		BufferedImage lead = WidgetKit.canvas(240, 140);
		g = WidgetKit.graphics(lead);
		style(WindowTheme.RUNELITE_GLASS).row(g, BOX, true, 0, 0);
		g.dispose();
		int top = lead.getRGB(120, 48);
		int bottom = lead.getRGB(120, 92);
		assertTrue("metal is lighter at the top", luminance(top) > luminance(bottom) + 20);
	}

	@Test
	public void theAccentIsMetalNotFlatOrangeAndSilverWithTheSteelBorder()
	{
		for (WindowTheme theme : new WindowTheme[] {WindowTheme.RUNELITE_GLASS, WindowTheme.RUNELITE_GLASS_STEEL})
		{
			BufferedImage canvas = WidgetKit.canvas(240, 140);
			Graphics2D g = WidgetKit.graphics(canvas);
			style(theme).button(g, BOX, "", true, false, 0, 0);
			g.dispose();
			int top = canvas.getRGB(120, 48);
			int middle = canvas.getRGB(120, 70);
			int bottom = canvas.getRGB(120, 92);

			assertTrue(theme + " gradient " + luminance(top) + " " + luminance(bottom),
				luminance(top) > luminance(bottom) + 25);
			float[] hsb = Color.RGBtoHSB(middle >> 16 & 0xFF, middle >> 8 & 0xFF, middle & 0xFF, null);
			if (theme == WindowTheme.RUNELITE_GLASS)
				assertTrue("orange " + hsb[1], hsb[1] > 0.6 && hsb[0] > 0.07 && hsb[0] < 0.14);
			else
				assertTrue("silver " + hsb[1], hsb[1] < 0.15);
		}
	}

	@Test
	public void theWindowFrameAndThePinnedBoxesWearTheBorder()
	{
		BufferedImage still = WidgetKit.canvas(300, 200);
		Graphics2D g = WidgetKit.graphics(still);
		style(WindowTheme.RUNELITE_GLASS).windowStill(g, new Rectangle(30, 30, 240, 140), false);
		g.dispose();
		assertTrue("frame", strongestTint(still, true) > 60);

		BufferedImage pinned = WidgetKit.canvas(300, 200);
		g = WidgetKit.graphics(pinned);
		style(WindowTheme.RUNELITE_GLASS_STEEL).pinnedOn(g, new Rectangle(30, 30, 240, 140), "tracker");
		g.dispose();
		assertTrue("pinned", strongestTint(pinned, false) > 60);
	}

	@Test
	public void earnedHeartsAreRaisedRedMetalAndEmptyOnesAreGreyWithADullRedEdge()
	{
		Shape heart = Icons.heart(40, 40, 60);
		BufferedImage full = WidgetKit.canvas(140, 140);
		Graphics2D g = WidgetKit.graphics(full);
		style(WindowTheme.RUNELITE_GLASS).heart(g, heart, true, false);
		g.dispose();
		int top = full.getRGB(56, 55);
		int bottom = full.getRGB(70, 88);
		assertTrue("red", (top >> 16 & 0xFF) > (top >> 8 & 0xFF) + 60);
		assertTrue("metal gradient " + luminance(top) + " " + luminance(bottom), luminance(top) > luminance(bottom));

		BufferedImage empty = WidgetKit.canvas(140, 140);
		g = WidgetKit.graphics(empty);
		style(WindowTheme.RUNELITE_GLASS).heart(g, heart, false, false);
		g.dispose();
		int centre = empty.getRGB(55, 55);
		assertTrue("grey fill", Math.abs((centre >> 16 & 0xFF) - (centre & 0xFF)) < 20 && (centre >>> 24) > 150);
		assertTrue("dull red edge", strongestTint(empty, true, 100) > 15);
		assertTrue("dull, not bright", strongestTint(empty, true, 100) < 90);
	}

	@Test
	public void movePillsAreOutlinedRedForMeleeGreenForRangedAndBlueForMagic()
	{
		assertEquals(GlassTokens.OUTLINE_MELEE, SummaryPainter.moveOutline(CombatType.STAB));
		assertEquals(GlassTokens.OUTLINE_MELEE, SummaryPainter.moveOutline(CombatType.SLASH));
		assertEquals(GlassTokens.OUTLINE_MELEE, SummaryPainter.moveOutline(CombatType.CRUSH));
		assertEquals(GlassTokens.OUTLINE_RANGED, SummaryPainter.moveOutline(CombatType.RANGED));
		assertEquals(GlassTokens.OUTLINE_MAGIC, SummaryPainter.moveOutline(CombatType.MAGIC));
		assertTrue(GlassTokens.OUTLINE_MELEE[1].getRed() > GlassTokens.OUTLINE_MELEE[1].getGreen() + 80);
		assertTrue(GlassTokens.OUTLINE_RANGED[1].getGreen() > GlassTokens.OUTLINE_RANGED[1].getRed() + 40);
		assertTrue(GlassTokens.OUTLINE_MAGIC[1].getBlue() > GlassTokens.OUTLINE_MAGIC[1].getRed() + 80);
	}
}
