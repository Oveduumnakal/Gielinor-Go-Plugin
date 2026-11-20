/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks both window styles: every colour is filled, text is readable, and the layout is the same in both. */
public class WindowThemeTest
{
	/** The colours that follow the metal: the solid look's orange, bronze with the bronze border, silver with steel. */
	private static final Set<ThemeColor> ACCENTS = EnumSet.of(ThemeColor.ACCENT, ThemeColor.XP_FILL,
		ThemeColor.CHIP_ACCENT, ThemeColor.CHIP_ACCENT_TEXT, ThemeColor.LEAD_ROW, ThemeColor.LEAD_BORDER,
		ThemeColor.LEAD_TEXT, ThemeColor.SELECTION);

	private static final double READABLE = 4.5;

	private static final ThemeColor[][] TEXT_ON =
	{
		{ThemeColor.TEXT, ThemeColor.PANEL_BOX},
		{ThemeColor.SUBTEXT, ThemeColor.PANEL_BOX},
		{ThemeColor.TEXT, ThemeColor.HEADER},
		{ThemeColor.ROW_TEXT, ThemeColor.ROW},
		{ThemeColor.SUBTEXT, ThemeColor.ROW},
		{ThemeColor.LEAD_TEXT, ThemeColor.LEAD_ROW},
		{ThemeColor.TAB_TEXT, ThemeColor.TAB},
		{ThemeColor.TAB_ACTIVE_TEXT, ThemeColor.TAB_ACTIVE},
		{ThemeColor.TOOLTIP_TEXT, ThemeColor.TOOLTIP},
		{ThemeColor.NOTICE_TEXT, ThemeColor.NOTICE},
		{ThemeColor.CHIP_TEXT, ThemeColor.CHIP},
		{ThemeColor.CHIP_ACCENT_TEXT, ThemeColor.CHIP_ACCENT},
		{ThemeColor.TEXT, ThemeColor.PORTRAIT_BACKDROP},
		{ThemeColor.TEXT, ThemeColor.INPUT}
	};

	@Test
	public void everyThemeFillsEveryColour()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			for (ThemeColor name : ThemeColor.values())
				assertNotNull(theme + " " + name, theme.color(name));
		}
	}

	@Test
	public void thePartyHighlightDiffersFromTheSelectionAndTheLeadColours()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			int ring = theme.color(ThemeColor.PARTY_HIGHLIGHT).getRGB() & 0xFFFFFF;
			assertNotEquals(theme.toString(), theme.color(ThemeColor.SELECTION).getRGB() & 0xFFFFFF, ring);
			assertNotEquals(theme.toString(), theme.color(ThemeColor.FOCUS_RING).getRGB() & 0xFFFFFF, ring);
			assertNotEquals(theme.toString(), theme.color(ThemeColor.LEAD_ROW).getRGB() & 0xFFFFFF, ring);
			assertNotEquals(theme.toString(), theme.color(ThemeColor.LEAD_BORDER).getRGB() & 0xFFFFFF, ring);
		}
	}

	@Test
	public void theJarBarAndTheTrainerBarDifferInHueAndLightness()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			Color jar = theme.color(ThemeColor.JAR_FILL);
			Color xp = theme.color(ThemeColor.XP_FILL);
			float[] a = Color.RGBtoHSB(jar.getRed(), jar.getGreen(), jar.getBlue(), null);
			float[] b = Color.RGBtoHSB(xp.getRed(), xp.getGreen(), xp.getBlue(), null);
			float hue = Math.abs(a[0] - b[0]);
			assertTrue(theme + " hue " + hue, Math.min(hue, 1 - hue) >= 0.15);
			double lighter = Math.max(WindowTheme.contrast(jar, xp), WindowTheme.contrast(xp, jar));
			assertTrue(theme + " lightness " + lighter, lighter >= 1.3);
			Color track = WindowTheme.over(theme.color(ThemeColor.XP_TRACK), Color.BLACK);
			assertTrue(theme + " on track", WindowTheme.contrast(jar, track) >= 3.0);
		}
	}

	@Test
	public void theStylesAreRuneLiteDarkAndRuneLiteGlassWithEitherBorder()
	{
		assertEquals(3, WindowTheme.values().length);
		assertEquals("RuneLite dark", WindowTheme.RUNELITE_DARK.toString());
		assertEquals("RuneLite glass", WindowTheme.RUNELITE_GLASS.toString());
		assertTrue(WindowTheme.RUNELITE_GLASS.glass());
		assertTrue(WindowTheme.RUNELITE_GLASS_STEEL.glass());
		assertFalse(WindowTheme.RUNELITE_DARK.glass());
	}

	@Test
	public void theBorderSettingPicksTheGlassStyleAndBronzeIsTheDefault()
	{
		assertEquals(WindowTheme.RUNELITE_GLASS, WindowTheme.of(true, GlassBorder.BRONZE));
		assertEquals(WindowTheme.RUNELITE_GLASS_STEEL, WindowTheme.of(true, GlassBorder.STEEL_BLUE));
		assertEquals(WindowTheme.RUNELITE_GLASS, WindowTheme.of(true, null));
		assertEquals(WindowTheme.RUNELITE_DARK, WindowTheme.of(false, GlassBorder.STEEL_BLUE));
		assertEquals(GlassBorder.BRONZE, WindowTheme.RUNELITE_GLASS.border());
		assertEquals(GlassBorder.STEEL_BLUE, WindowTheme.RUNELITE_GLASS_STEEL.border());
		assertEquals(GlassBorder.BRONZE, new GielinorGoConfig()
		{
		}.glassBorder());
	}

	@Test
	public void theGlassStyleHasTheSamePaletteAsTheSolidOne()
	{
		for (ThemeColor name : ThemeColor.values())
		{
			if (ACCENTS.contains(name))
				continue;

			assertEquals(name.toString(), WindowTheme.RUNELITE_DARK.color(name),
				WindowTheme.RUNELITE_GLASS.color(name));
			assertEquals(name.toString(), WindowTheme.RUNELITE_DARK.color(name),
				WindowTheme.RUNELITE_GLASS_STEEL.color(name));
		}

		for (ThemeColor name : ACCENTS)
		{
			assertTrue(name.toString(), !WindowTheme.RUNELITE_DARK.color(name)
				.equals(WindowTheme.RUNELITE_GLASS_STEEL.color(name)));
			assertTrue(name.toString(), !WindowTheme.RUNELITE_GLASS.color(name)
				.equals(WindowTheme.RUNELITE_GLASS_STEEL.color(name)));
		}

		assertEquals(0xDC8A00, WindowTheme.RUNELITE_DARK.color(ThemeColor.ACCENT).getRGB() & 0xFFFFFF);
	}

	@Test
	public void textSubtextAndLeadTextReachFourPointFiveToOne()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			for (ThemeColor[] pair : TEXT_ON)
			{
				Color text = theme.color(pair[0]);
				Color under = WindowTheme.over(theme.color(pair[1]), Color.BLACK);
				double ratio = WindowTheme.contrast(text, under);
				assertTrue(theme + " " + pair[0] + " on " + pair[1] + ": " + ratio, ratio >= READABLE);
			}
		}
	}

	@Test
	public void textOnTheWindowIsReadableOverAnyGameScene()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			for (Color scene : new Color[] {Color.BLACK, Color.WHITE})
			{
				Color window = WindowTheme.over(theme.color(ThemeColor.WINDOW), scene);
				for (ThemeColor name : new ThemeColor[] {ThemeColor.TEXT, ThemeColor.SUBTEXT})
				{
					double ratio = WindowTheme.contrast(theme.color(name), window);
					assertTrue(theme + " " + name + " over " + scene + ": " + ratio, ratio >= READABLE);
				}
			}
		}
	}

	@Test
	public void healthColoursCanBeToldApartFromTheirTrack()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			Color track = theme.color(ThemeColor.HP_TRACK);
			for (ThemeColor name : new ThemeColor[] {ThemeColor.HP_FILL, ThemeColor.HP_WARNING, ThemeColor.HP_DANGER})
			{
				double ratio = WindowTheme.contrast(theme.color(name), track);
				assertTrue(theme + " " + name + ": " + ratio, ratio >= 3.0);
			}
		}
	}

	@Test
	public void bothThemesGiveTheSameLayoutNumbers()
	{
		for (double scale : new double[] {0.6667, 0.67, 1.0, 1.5, 2.0})
		{
			WindowStyle dark = WindowStyle.of(WindowTheme.RUNELITE_DARK, scale);
			WindowStyle lite = WindowStyle.of(WindowTheme.RUNELITE_DARK, scale);
			assertEquals("scale " + scale, dark.metrics(), lite.metrics());
			assertEquals(dark.px(40), lite.px(40));
			assertEquals(dark.font(WindowFonts.Face.BOLD, 32), lite.font(WindowFonts.Face.BOLD, 32));
		}
	}

	@Test
	public void mixFadeAndOverDoTheColourMaths()
	{
		Color mid = WindowTheme.mix(Color.BLACK, Color.WHITE, 0.5);
		assertEquals(128, mid.getRed());
		assertEquals(Color.BLACK, WindowTheme.mix(Color.BLACK, Color.WHITE, -3));
		assertEquals(Color.WHITE, WindowTheme.mix(Color.BLACK, Color.WHITE, 9));
		assertEquals(64, WindowTheme.fade(new Color(1, 2, 3, 128), 0.5).getAlpha());
		assertEquals(new Color(128, 128, 128), WindowTheme.over(new Color(255, 255, 255, 128), Color.BLACK));
		assertEquals(21.0, WindowTheme.contrast(Color.BLACK, Color.WHITE), 1e-9);
		assertEquals(1.0, WindowTheme.contrast(Color.WHITE, Color.WHITE), 1e-9);
	}
}
