/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks the window size rules: scale, shape, the canvas limit, the text minimum and the stored fraction. */
public class WindowFitTest
{
	private static final int[][] CANVASES =
		{{765, 503}, {1024, 768}, {1920, 1080}, {1280, 800}, {2560, 1440}, {1000, 400}, {500, 900}, {300, 300}};

	@Test
	public void scaleIsTheSmallerRatioOfTheDesignSpace()
	{
		assertEquals(1.0, WindowFit.scale(800, 520), 1e-9);
		assertEquals(0.5, WindowFit.scale(400, 260), 1e-9);
		assertEquals(0.5, WindowFit.scale(800, 260), 1e-9);
		assertEquals(0.5, WindowFit.scale(2000, 260), 1e-9);
		assertEquals(0.0, WindowFit.scale(-5, 100), 1e-9);
	}

	@Test
	public void fitSizeIsTheBiggestWindowShapeInABox()
	{
		Dimension wide = WindowFit.fitSize(1920, 1080);
		assertEquals(1080, wide.height);
		assertEquals(1661, wide.width);

		Dimension tall = WindowFit.fitSize(800, 2000);
		assertEquals(800, tall.width);
		assertEquals(520, tall.height);

		assertEquals(new Dimension(0, 0), WindowFit.fitSize(0, 500));
	}

	@Test
	public void fixedModeDefaultIsAboutFiveThirtyFiveWideAndNeverAboveTheCanvas()
	{
		Dimension size = WindowFit.sizeFor(WindowSizePreset.MEDIUM.fraction(), 765, 503);
		assertTrue("width " + size.width, size.width >= 533 && size.width <= 537);
		assertTrue(size.width <= 765 && size.height <= 503);
		assertTrue(WindowFit.scale(size.width, size.height) >= WindowFit.MIN_SCALE);
		assertTrue("scale " + WindowFit.scale(size.width, size.height),
			Math.abs(WindowFit.scale(size.width, size.height) - 0.67) < 0.01);
	}

	@Test
	public void noPresetOrSizeIsEverAboveTheCanvas()
	{
		for (int[] canvas : CANVASES)
		{
			for (WindowSizePreset preset : WindowSizePreset.values())
			{
				Rectangle r = WindowFit.preset(preset, canvas[0], canvas[1]);
				String where = preset + " in " + canvas[0] + "x" + canvas[1];
				assertTrue(where, r.width <= canvas[0] && r.height <= canvas[1]);
				assertTrue(where, r.x >= 0 && r.y >= 0 && r.x + r.width <= canvas[0] && r.y + r.height <= canvas[1]);
			}

			Dimension huge = WindowFit.clamp(100_000, 100_000, canvas[0], canvas[1]);
			assertTrue(huge.width <= canvas[0] && huge.height <= canvas[1]);
		}
	}

	@Test
	public void theWindowAlwaysKeepsItsShape()
	{
		for (int[] canvas : CANVASES)
		{
			for (double fraction : new double[] {0.3, 0.5, 0.7, 0.85, 1.0})
			{
				Dimension size = WindowFit.sizeFor(fraction, canvas[0], canvas[1]);
				if (size.width < 100)
					continue;

				double aspect = (double) size.width / size.height;
				assertTrue(canvas[0] + "x" + canvas[1] + " at " + fraction + ": " + aspect,
					Math.abs(aspect - 800.0 / 520.0) < 0.02);
			}
		}
	}

	@Test
	public void aFreeFormDragSnapsBackToTheShape()
	{
		Dimension snapped = WindowFit.clamp(1000, 900, 1920, 1080);
		assertEquals(1000, snapped.width);
		assertEquals(650, snapped.height);

		Dimension fromHeight = WindowFit.clamp(1500, 390, 1920, 1080);
		assertEquals(600, fromHeight.width);
		assertEquals(390, fromHeight.height);
	}

	@Test
	public void theMinimumScaleComesFromTheSmallestReadableText()
	{
		assertEquals((double) WindowFonts.NATIVE_PX / WindowFonts.BODY_UNITS, WindowFit.MIN_SCALE, 1e-9);
		Dimension tiny = WindowFit.clamp(100, 65, 2000, 1200);
		assertTrue(WindowFit.scale(tiny.width, tiny.height) >= WindowFit.MIN_SCALE - 0.002);
		assertEquals(Math.round(800 * WindowFit.MIN_SCALE), tiny.width);

		assertEquals((int) Math.round(WindowFonts.BODY_UNITS * WindowFit.MIN_SCALE), WindowFonts.NATIVE_PX);
	}

	@Test
	public void aCanvasSmallerThanTheMinimumWindowWins()
	{
		Dimension size = WindowFit.sizeFor(0.7, 400, 300);
		assertTrue(size.width <= 400 && size.height <= 300);
		assertEquals(WindowFit.fitSize(400, 300), size);
	}

	@Test
	public void aDragResizeBecomesTheNewFraction()
	{
		double fraction = WindowFit.fractionAfterResize(1000, 900, 1920, 1080);
		assertEquals(1000.0 / 1920.0, fraction, 1e-9);

		Dimension again = WindowFit.sizeFor(fraction, 1920, 1080);
		assertEquals(1000, again.width);
		assertEquals(650, again.height);

		Dimension followsCanvas = WindowFit.sizeFor(fraction, 1280, 800);
		assertEquals(Math.round(1280 * fraction), followsCanvas.width, 1.0);
	}

	@Test
	public void theFractionStaysInRange()
	{
		assertEquals(1.0, WindowFit.fractionOf(5000, 1000), 1e-9);
		assertEquals(WindowFit.MIN_FRACTION, WindowFit.fractionOf(1, 1000), 1e-9);
		assertEquals(WindowFit.DEFAULT_FRACTION, WindowFit.fractionOf(500, 0), 1e-9);
		assertEquals(0.5, WindowFit.fractionOf(500, 1000), 1e-9);
	}

	@Test
	public void presetsGrowFromSmallToFitCanvas()
	{
		int last = 0;
		for (WindowSizePreset preset : WindowSizePreset.values())
		{
			Rectangle r = WindowFit.preset(preset, 2560, 1440);
			assertTrue(preset + " " + r.width, r.width > last);
			last = r.width;
		}

		assertEquals(1440, WindowFit.preset(WindowSizePreset.FIT_CANVAS, 2560, 1440).height);
		assertEquals(0.7, WindowSizePreset.MEDIUM.fraction(), 1e-9);
	}

	@Test
	public void resetIsTheDefaultSizeCentred()
	{
		Rectangle r = WindowFit.reset(1920, 1080);
		assertEquals(WindowFit.preset(WindowSizePreset.SMALL, 1920, 1080), r);
		assertEquals((1920 - r.width) / 2, r.x);
		assertEquals((1080 - r.height) / 2, r.y);
		assertEquals(new Point(100, 50), WindowFit.centered(new Dimension(100, 50), 300, 150));
		assertEquals(new Point(0, 0), WindowFit.centered(new Dimension(500, 500), 300, 150));
	}
}
