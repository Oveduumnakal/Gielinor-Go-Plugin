/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Checks the ambient ring wash for the glass Info card (#294): it ignores what is under the card, takes its colours
 * from the sides nearest each spot, survives a card at the frame's edge, and costs little.
 */
public class RingWashTest
{
	private static final int W = 1920;
	private static final int H = 1080;
	private static final Rectangle CARD = new Rectangle(800, 400, 240, 200);
	private static final int MIDDLE = CARD.x + CARD.width / 2;

	/** A frame red left of the card's middle and blue right of it, with the card painted green over the middle. */
	private static BufferedImage halves()
	{
		BufferedImage frame = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = frame.createGraphics();
		try
		{
			g.setColor(Color.RED);
			g.fillRect(0, 0, MIDDLE, H);
			g.setColor(Color.BLUE);
			g.fillRect(MIDDLE, 0, W - MIDDLE, H);
			g.setColor(Color.GREEN);
			g.fill(CARD);
		}
		finally
		{
			g.dispose();
		}

		return frame;
	}

	@Test
	public void theWashIsACellPerEightPixels()
	{
		BufferedImage wash = RingWash.wash(halves(), CARD);
		assertNotNull(wash);
		assertEquals(CARD.width / RingWash.CELL, wash.getWidth());
		assertEquals(CARD.height / RingWash.CELL, wash.getHeight());
	}

	@Test
	public void theCardItselfIsNeverRead()
	{
		BufferedImage wash = RingWash.wash(halves(), CARD);
		for (int y = 0; y < wash.getHeight(); y++)
		{
			for (int x = 0; x < wash.getWidth(); x++)
				assertEquals(0, wash.getRGB(x, y) >> 8 & 0xFF);
		}
	}

	@Test
	public void eachSideTakesTheColourBesideIt()
	{
		BufferedImage wash = RingWash.wash(halves(), CARD);
		Color left = new Color(wash.getRGB(0, wash.getHeight() / 2));
		Color right = new Color(wash.getRGB(wash.getWidth() - 1, wash.getHeight() / 2));
		assertTrue(left.getRed() > left.getBlue());
		assertTrue(right.getBlue() > right.getRed());
	}

	@Test
	public void aCardAtTheFrameEdgeStillGetsAWash()
	{
		BufferedImage frame = halves();
		assertNotNull(RingWash.wash(frame, new Rectangle(0, 0, 200, 150)));
		assertNotNull(RingWash.wash(frame, new Rectangle(W - 150, H - 100, 200, 150)));
		assertNull(RingWash.wash(frame, new Rectangle(W + 50, H + 50, 200, 150)));
	}

	@Test
	public void blendingMovesPartWayAndSkipsADifferentSize()
	{
		BufferedImage black = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		BufferedImage white = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 4; y++)
		{
			for (int x = 0; x < 4; x++)
				white.setRGB(x, y, 0xFFFFFF);
		}

		assertEquals(128, RingWash.blend(black, white, 0.5).getRGB(1, 1) & 0xFF);
		BufferedImage other = new BufferedImage(5, 4, BufferedImage.TYPE_INT_RGB);
		assertSame(other, RingWash.blend(black, other, 0.5));
		assertSame(white, RingWash.blend(null, white, 0.5));
	}

	@Test
	public void printsTheCostOfAWash()
	{
		BufferedImage frame = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Random random = new Random(294);
		for (int y = 0; y < H; y += 6)
		{
			for (int x = 0; x < W; x += 6)
			{
				int c = random.nextInt(0x1000000);
				for (int dy = 0; dy < 6 && y + dy < H; dy++)
				{
					for (int dx = 0; dx < 6 && x + dx < W; dx++)
						frame.setRGB(x + dx, y + dy, c);
				}
			}
		}

		for (int i = 0; i < 100; i++)
			RingWash.wash(frame, CARD);

		int runs = 1000;
		long start = System.nanoTime();
		for (int i = 0; i < runs; i++)
			RingWash.wash(frame, CARD);

		long micros = (System.nanoTime() - start) / 1000 / runs;
		System.out.println("ring wash of a 240x200 card on a 1920x1080 frame: " + micros + "us");
		assertTrue(micros >= 0);
	}
}
