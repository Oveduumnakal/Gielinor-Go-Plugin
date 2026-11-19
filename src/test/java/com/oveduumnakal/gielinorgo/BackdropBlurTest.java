/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the backdrop blur on synthetic frames: its size, that a hard edge turns into a ramp, that a region off the
 * frame gives nothing, and prints what a blur of a card-sized region of a full HD frame costs.
 */
public class BackdropBlurTest
{
	private static int[] halves(int width, int height)
	{
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
				pixels[y * width + x] = x < width / 2 ? 0xFF000000 : 0xFFFFFFFF;
		}

		return pixels;
	}

	private static int grey(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) & 0xFF;
	}

	@Test
	public void theBlurIsAQuarterOfTheRegion()
	{
		BufferedImage out = BackdropBlur.blur(halves(200, 100), 200, 100, new Rectangle(20, 10, 160, 80));

		assertNotNull(out);
		assertEquals(40, out.getWidth());
		assertEquals(20, out.getHeight());
	}

	@Test
	public void aHardEdgeBecomesAGreyRamp()
	{
		BufferedImage out = BackdropBlur.blur(halves(200, 100), 200, 100, new Rectangle(36, 20, 128, 64));
		int row = out.getHeight() / 2;
		int mid = out.getWidth() / 2;
		boolean between = false;
		for (int x = mid - 3; x <= mid + 3; x++)
		{
			int v = grey(out, x, row);
			between |= v > 10 && v < 245;
			assertTrue("ramp at " + x, grey(out, x + 1, row) >= v);
		}

		assertTrue(between);
		assertTrue(grey(out, 1, row) < 10);
		assertTrue(grey(out, out.getWidth() - 2, row) > 245);
	}

	@Test
	public void aRegionOffTheFrameGivesNothingAndAnEdgeRegionIsClamped()
	{
		assertNull(BackdropBlur.blur(halves(200, 100), 200, 100, new Rectangle(300, 10, 40, 40)));
		BufferedImage clamped = BackdropBlur.blur(halves(200, 100), 200, 100, new Rectangle(180, 80, 40, 40));
		assertNotNull(clamped);
		assertEquals(5, clamped.getWidth());
		assertEquals(5, clamped.getHeight());
	}

	@Test
	public void printsTheCostOfACardSizedBlurOfAFullHdFrame()
	{
		int w = 1920;
		int h = 1080;
		int[] frame = new int[w * h];
		Random random = new Random(294);
		for (int i = 0; i < frame.length; i++)
			frame[i] = 0xFF000000 | random.nextInt(0x1000000);

		Rectangle region = new Rectangle(800, 400, 250, 200);
		for (int i = 0; i < 250; i++)
			BackdropBlur.blur(frame, w, h, region);

		int runs = 1000;
		long start = System.nanoTime();
		for (int i = 0; i < runs; i++)
			BackdropBlur.blur(frame, w, h, region);

		double micros = (System.nanoTime() - start) / 1000.0 / runs;
		System.out.printf("backdrop blur 250x200 of 1920x1080: %.1f us per frame%n", micros);
		assertTrue(micros > 0);
	}
}
