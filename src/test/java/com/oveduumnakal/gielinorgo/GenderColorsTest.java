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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Checks the gender sign colours: blue for male, pink for female, and the sign is painted in them. */
public class GenderColorsTest
{
	private static boolean blue(Color c)
	{
		return c.getBlue() > c.getRed() && c.getBlue() > c.getGreen();
	}

	private static boolean pink(Color c)
	{
		return c.getRed() > c.getGreen() && c.getBlue() > c.getGreen() && c.getRed() >= c.getBlue();
	}

	@Test
	public void maleIsBlueAndFemaleIsPinkOnBothBackgrounds()
	{
		for (boolean light : new boolean[] {false, true})
		{
			assertTrue(blue(GenderColors.of(Gender.MALE, light)));
			assertTrue(pink(GenderColors.of(Gender.FEMALE, light)));
			assertNotEquals(GenderColors.of(Gender.MALE, light), GenderColors.of(Gender.FEMALE, light));
		}

		assertNotEquals(GenderColors.of(Gender.MALE, false), GenderColors.of(Gender.MALE, true));
	}

	@Test
	public void theSignIsPaintedInTheGenderColour()
	{
		for (Gender gender : new Gender[] {Gender.MALE, Gender.FEMALE})
		{
			BufferedImage image = WidgetKit.canvas(64, 64);
			Graphics2D g = WidgetKit.graphics(image);
			g.setColor(GenderColors.of(gender, false));
			Icons.gender(g, gender, 0, 0, 64);
			g.dispose();

			int rgb = image.getRGB(32, 32);
			boolean found = false;
			int want = GenderColors.of(gender, false).getRGB() & 0xFFFFFF;
			for (int x = 0; x < 64 && !found; x++)
			{
				for (int y = 0; y < 64 && !found; y++)
					found = (image.getRGB(x, y) >>> 24) == 255 && (image.getRGB(x, y) & 0xFFFFFF) == want;
			}

			assertTrue(gender.name() + " " + rgb, found);
			assertEquals(want, GenderColors.of(gender, false).getRGB() & 0xFFFFFF);
		}
	}
}
