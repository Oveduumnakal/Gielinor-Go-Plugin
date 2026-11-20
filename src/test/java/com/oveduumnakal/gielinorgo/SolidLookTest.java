/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Holds the solid look to recorded pictures: every primitive of the kit, the Info card and the wild bubbles, painted
 * with {@link WindowTheme#RUNELITE_DARK} at scales 1.0 and 1.5. With the environment variable
 * {@code SOLID_LOOK_RECORD=1} it writes the pictures to {@code src/test/resources/.../solid-look/}; otherwise it
 * compares to them and passes when at most 0.5% of the pixels differ and none by more than 8 levels in any channel
 * (fonts can rasterize slightly differently on another machine). The pictures are re-recorded only when the solid
 * look is meant to change (#321, #322).
 */
public class SolidLookTest
{
	/** The folder of the recorded pictures, under the test resources. */
	static final String FOLDER = "solid-look";

	/** The most pixels that may differ, as a share of all. */
	static final double MAX_DIFFERING = 0.005;

	/** The most a channel may differ by in a differing pixel. */
	static final int MAX_LEVELS = 8;

	private static final double[] SCALES = {1.0, 1.5};

	/**
	 * Compares a picture to its recording, or records it.
	 *
	 * @param folder the folder under the test resources
	 * @param name the picture's file name without the extension
	 * @param actual the picture
	 */
	static void check(String folder, String name, BufferedImage actual)
	{
		String file = name + ".png";
		String path = "/com/oveduumnakal/gielinorgo/" + folder + "/" + file;
		if ("1".equals(System.getenv("SOLID_LOOK_RECORD")))
		{
			File out = new File("src/test/resources/com/oveduumnakal/gielinorgo/" + folder, file);
			out.getParentFile().mkdirs();
			try
			{
				ImageIO.write(actual, "png", out);
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}

			return;
		}

		BufferedImage expected = read(path);
		assertNotNull("missing recording " + path + "; run with SOLID_LOOK_RECORD=1", expected);
		assertEquals(file + " width", expected.getWidth(), actual.getWidth());
		assertEquals(file + " height", expected.getHeight(), actual.getHeight());
		int differing = 0;
		int worst = 0;
		for (int y = 0; y < actual.getHeight(); y++)
		{
			for (int x = 0; x < actual.getWidth(); x++)
			{
				int level = difference(expected.getRGB(x, y), actual.getRGB(x, y));
				if (level > 0)
					differing++;

				worst = Math.max(worst, level);
			}
		}

		double share = differing / (double) (actual.getWidth() * actual.getHeight());
		assertTrue(file + ": " + share * 100 + "% of the pixels differ", share <= MAX_DIFFERING);
		assertTrue(file + ": a channel differs by " + worst, worst <= MAX_LEVELS);
	}

	private static BufferedImage read(String path)
	{
		try (InputStream in = SolidLookTest.class.getResourceAsStream(path))
		{
			return in == null ? null : ImageIO.read(in);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static int difference(int a, int b)
	{
		int worst = 0;
		for (int shift = 0; shift <= 24; shift += 8)
			worst = Math.max(worst, Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF)));

		return worst;
	}

	@Test
	public void everyPrimitiveLooksAsRecorded()
	{
		for (double scale : SCALES)
			check(FOLDER, "primitives-" + scale, LookSheet.primitives(WindowTheme.RUNELITE_DARK, scale));
	}

	@Test
	public void theInfoCardLooksAsRecorded()
	{
		for (double scale : SCALES)
			check(FOLDER, "card-" + scale, LookSheet.card(WindowTheme.RUNELITE_DARK, scale));
	}

	@Test
	public void theWildBubblesLookAsRecorded()
	{
		check(FOLDER, "bubbles-1.0", LookSheet.bubbles(WindowTheme.RUNELITE_DARK, 1.0));
	}
}
