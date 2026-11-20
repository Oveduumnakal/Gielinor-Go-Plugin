/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pins the readability guard: every text colour of a surface reads over six scenes, tamed or not. */
public class ReadabilityTest
{
	private static final int[] SCENES = {0xFFFFFF, 0x000000, 0x808080, 0xFFFF00, 0x80C0FF, 0xF4F1E8};

	private static final List<List<Readability.Need>> SURFACES = Arrays.asList(Readability.BODY,
		Readability.TOOLTIP, Readability.NOTICE);

	@Test
	public void everyTextColourReadsOverEverySceneOnceTheWashIsTamed()
	{
		StringBuilder report = new StringBuilder();
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		Readability.Stack stack = Readability.body(style);
		for (List<Readability.Need> needs : SURFACES)
		{
			for (int scene : SCENES)
			{
				Color raw = new Color(scene);
				Color tamed = Readability.tame(raw, style, stack, needs);
				for (Readability.Need need : needs)
				{
					Color under = Readability.behind(stack, need, raw, tamed);
					double ratio = WindowTheme.contrast(style.color(need.text()), under);
					String line = String.format("%s over %06X (tamed to %06X): %.2f, needs %.1f", need.text(), scene,
						tamed.getRGB() & 0xFFFFFF, ratio, need.minimum());
					report.append(line).append('\n');
					assertTrue(line, ratio >= need.minimum());
				}
			}
		}

		System.out.print(report);
	}

	@Test
	public void aWashThatAlreadyReadsIsLeftAsItIs()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		Color night = new Color(0x101418);
		assertEquals(night, Readability.tame(night, style, Readability.body(style), Readability.BODY));
	}

	@Test
	public void aBrighterWashIsDarkenedMoreThanADimmerOne()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		Readability.Stack stack = Readability.body(style);
		Color white = Readability.tame(new Color(0xFFFFFF), style, stack, Readability.BODY);
		Color grey = Readability.tame(new Color(0x808080), style, stack, Readability.BODY);
		assertTrue(white.getRed() <= 0xFF && white.getRed() < 0xFF);
		assertTrue(grey.getRed() >= white.getRed() * 0.5);
	}

	@Test
	public void aTamedPictureHasEveryPixelTamed()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		BufferedImage wash = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
		wash.setRGB(0, 0, 0xFFFFFF);
		wash.setRGB(1, 0, 0x000000);
		wash.setRGB(2, 0, 0xFFFF00);
		BufferedImage tamed = Readability.tame(wash, style, Readability.body(style), Readability.BODY);
		for (int x = 0; x < 3; x++)
		{
			Color expected = Readability.tame(new Color(wash.getRGB(x, 0)), style, Readability.body(style),
				Readability.BODY);
			assertEquals(expected.getRGB() & 0xFFFFFF, tamed.getRGB(x, 0) & 0xFFFFFF);
		}
	}

	@Test
	public void taminingAWindowSizedWashIsQuick()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		BufferedImage wash = new BufferedImage(125, 88, BufferedImage.TYPE_INT_RGB);
		java.util.Random random = new java.util.Random(3);
		for (int y = 0; y < wash.getHeight(); y++)
		{
			for (int x = 0; x < wash.getWidth(); x++)
				wash.setRGB(x, y, 0x808080 + (random.nextInt(0x7F7F7F) & 0x7F7F7F));
		}

		Readability.Stack stack = Readability.body(style);
		Readability.tame(wash, style, stack, Readability.BODY);
		long start = System.nanoTime();
		Readability.tame(wash, style, stack, Readability.BODY);
		double millis = (System.nanoTime() - start) / 1e6;
		System.out.printf("tame a 125x88 wash: %.2f ms%n", millis);
		assertTrue("took " + millis, millis < 50);
	}
}
