/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tests the sampling, timing and framing of an idle loop. */
public class IdleStripTest
{
	private static BufferedImage dot(int x, int y)
	{
		BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(x, y, 0xFFFFFFFF);
		return image;
	}

	/** A flat square facing the viewer, its lower left corner at the origin. */
	private static PortraitPainter.Mesh square(float width)
	{
		float[] x = {0, width, width, 0};
		float[] y = {0, 0, -50, -50};
		float[] z = {0, 0, 0, 0};
		int[][] corners = {{0, 0}, {1, 2}, {2, 3}};
		int[][] colors = {{5000, 5000}, {5000, 5000}, {5000, 5000}};
		return new PortraitPainter.Mesh(x, y, z, corners, colors, null);
	}

	@Test
	public void aShortAnimationKeepsEveryFrameAndALongOneIsSampledEvenly()
	{
		assertArrayEquals(new int[] {0, 1, 2, 3}, IdleStrip.sample(4));
		int[] sampled = IdleStrip.sample(100);
		assertEquals(IdleStrip.MOST_FRAMES, sampled.length);
		assertEquals(0, sampled[0]);
		assertTrue(sampled[sampled.length - 1] >= 95 && sampled[sampled.length - 1] < 100);
		assertEquals(0, IdleStrip.sample(0).length);
	}

	@Test
	public void aSampledFrameAlsoLastsAsLongAsThoseSkippedAfterIt()
	{
		long[] millis = IdleStrip.millis(new int[] {0, 2}, new int[] {1, 2, 3, 4});

		assertArrayEquals(new long[] {60, 140}, millis);
	}

	@Test
	public void theLoopPlaysEachPictureForItsTimeRoundAndRound()
	{
		BufferedImage first = dot(2, 3);
		BufferedImage second = dot(10, 12);
		IdleStrip strip = new IdleStrip(Arrays.asList(first, second), new long[] {100, 300});

		assertTrue(strip.at(0) == first);
		assertTrue(strip.at(99) == first);
		assertTrue(strip.at(100) == second);
		assertTrue(strip.at(399) == second);
		assertTrue(strip.at(400) == first);
		assertEquals(new Rectangle(2, 3, 9, 10), strip.shape());
		assertEquals(2, strip.size());
	}

	@Test
	public void everyPoseOfAStripIsDrawnAtTheSameScale()
	{
		List<BufferedImage> pictures = PortraitPainter.paintAll(Arrays.asList(square(50), square(100)), 64);

		Rectangle small = SummaryScene.trimmed(pictures.get(0));
		Rectangle alone = SummaryScene.trimmed(PortraitPainter.paint(square(50), 64));
		assertTrue("the small pose leaves room for the large one", small.width < alone.width * 3 / 4);
		assertTrue(SummaryScene.trimmed(pictures.get(1)).width >= alone.width - 2);
	}

	@Test
	public void aStripWithAnEmptyPoseIsNotMade()
	{
		PortraitPainter.Mesh empty = new PortraitPainter.Mesh(new float[0], new float[0], new float[0],
			new int[3][0], new int[3][0], null);

		assertNull(PortraitPainter.paintAll(Arrays.asList(square(50), empty), 64));
	}
}
