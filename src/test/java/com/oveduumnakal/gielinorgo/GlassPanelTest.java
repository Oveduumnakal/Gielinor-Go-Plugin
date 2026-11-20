/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pins the frosted-glass card's readability: the fill and the strongest sheen, laid over a white, a black and a mid
 * grey scene, keep the card's text at WCAG contrast (4.5 for text, 3.0 for the grey subtext) in every theme.
 */
public class GlassPanelTest
{
	private static final int[] SCENES = {0xFFFFFF, 0x000000, 0x808080};

	private static Color behindText(WindowStyle style, int scene)
	{
		Color filled = WindowTheme.over(GlassPanel.fill(style), new Color(scene));
		return WindowTheme.over(GlassPanel.sheen(GlassPanel.SHEEN_TOP), filled);
	}

	@Test
	public void textReadsOverWhiteBlackAndGreyScenes()
	{
		StringBuilder report = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
		{
			WindowStyle style = WindowStyle.of(theme, 1.0);
			for (int scene : SCENES)
			{
				Color under = behindText(style, scene);
				double text = WindowTheme.contrast(style.color(ThemeColor.TEXT), under);
				double subtext = WindowTheme.contrast(style.color(ThemeColor.SUBTEXT), under);
				String name = String.format("%s over %06X: text %.2f, subtext %.2f", theme.name(), scene, text,
					subtext);
				report.append(name).append('\n');
				assertTrue(name, text >= 4.5);
				assertTrue(name, subtext >= 3.0);
			}
		}

		System.out.print(report);
	}

	@Test
	public void theFillIsSeeThrough()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			WindowStyle style = WindowStyle.of(theme, 1.0);
			assertTrue(theme.name(), GlassPanel.fill(style).getAlpha() < 255);
			assertNotEquals(theme.name(), behindText(style, 0xFFFFFF), behindText(style, 0x000000));
		}
	}

	@Test
	public void theStillLayerIsPaintedOnceForTheSameKey()
	{
		GlassPanel glass = new GlassPanel();
		WindowStyle style = WindowStyle.of(WindowTheme.values()[0], 1.0);
		BufferedImage canvas = WidgetKit.canvas(300, 200);
		Graphics2D g = WidgetKit.graphics(canvas);
		glass.paint(g, new Rectangle(10, 10, 200, 120), style, null);
		glass.paint(g, new Rectangle(40, 30, 200, 120), style, null);
		assertEquals(1, glass.paints());
		glass.paint(g, new Rectangle(40, 30, 210, 120), style, null);
		assertEquals(2, glass.paints());
		WindowStyle bigger = WindowStyle.of(WindowTheme.values()[0], 1.5);
		glass.paint(g, new Rectangle(40, 30, 210, 120), bigger, null);
		assertEquals(3, glass.paints());
		g.dispose();
	}

	@Test
	public void aTamedBackdropAlwaysLeavesTheTextReadable()
	{
		int[] backdrops = {0xFFFFFF, 0xF4F1E8, 0xFFFF00, 0x00FFFF, 0x80C0FF, 0x808080, 0x101418, 0x000000};
		StringBuilder report = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
		{
			WindowStyle style = WindowStyle.of(theme, 1.0);
			for (int backdrop : backdrops)
			{
				Color raw = new Color(backdrop);
				Color tamed = GlassPanel.tame(raw, style);
				Color under = GlassPanel.behindText(GlassPanel.seen(raw, tamed), style);
				double text = WindowTheme.contrast(style.color(ThemeColor.TEXT), under);
				double subtext = WindowTheme.contrast(style.color(ThemeColor.SUBTEXT), under);
				String name = String.format("%s backdrop %06X tamed to %06X: text %.2f, subtext %.2f", theme.name(),
					backdrop, tamed.getRGB() & 0xFFFFFF, text, subtext);
				report.append(name).append('\n');
				assertTrue(name, text >= GlassPanel.TEXT_CONTRAST);
				assertTrue(name, subtext >= GlassPanel.SUBTEXT_CONTRAST);
			}
		}

		System.out.print(report);
	}

	@Test
	public void aDarkBackdropIsLeftAsItIs()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.values()[0], 1.0);
		Color night = new Color(0x101418);
		assertEquals(night, GlassPanel.tame(night, style));
	}

	@Test
	public void theFillOverABackdropIsThinner()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.values()[0], 1.0);
		assertTrue(GlassPanel.fill(style, GlassPanel.BACKDROP_FILL_ALPHA).getAlpha() < GlassPanel.fill(style)
			.getAlpha());
	}

	@Test
	public void theGrainSpecksVaryInLightAndOpacity()
	{
		BufferedImage grain = GlassPanel.grain();
		int peak = (int) Math.round(GlassPanel.GRAIN_PEAK_ALPHA * 255);
		int light = 0;
		int dark = 0;
		Set<Integer> opacities = new HashSet<>();
		for (int y = 0; y < grain.getHeight(); y++)
		{
			for (int x = 0; x < grain.getWidth(); x++)
			{
				int argb = grain.getRGB(x, y);
				int a = argb >>> 24;
				assertTrue(a <= peak);
				opacities.add(a);
				if ((argb & 0xFF) == 0xFF)
					light++;
				else
					dark++;
			}
		}

		assertTrue(light > 0 && dark > 0);
		assertTrue(opacities.size() > 10);
	}
}
