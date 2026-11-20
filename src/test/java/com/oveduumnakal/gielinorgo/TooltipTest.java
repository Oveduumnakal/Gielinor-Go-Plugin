/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the tooltip: the 400 ms delay, restarts, staying inside the canvas, wrapping and painting. */
public class TooltipTest
{
	private static final WindowStyle STYLE = WidgetKit.style(1.0);
	private static final Rectangle CANVAS = new Rectangle(0, 0, 400, 300);

	private static BufferedImage paintAt(Tooltip tip, long now)
	{
		BufferedImage image = WidgetKit.canvas(400, 300);
		Graphics2D g = WidgetKit.graphics(image);
		tip.paint(g, STYLE, CANVAS, now);
		g.dispose();
		return image;
	}

	@Test
	public void theTipShowsOnlyAfterTheDelay()
	{
		Tooltip tip = new Tooltip();
		tip.hover("row:1", "Giant rat", 50, 50, 1_000);

		assertFalse(tip.visible(1_000));
		assertFalse(tip.visible(1_399));
		assertTrue(tip.visible(1_400));
		assertTrue(tip.visible(9_000));
		assertEquals("Giant rat", tip.text());
	}

	@Test
	public void theSameKeyKeepsTheDelayRunningAndANewKeyRestartsIt()
	{
		Tooltip tip = new Tooltip();
		tip.hover("row:1", "One", 50, 50, 0);
		tip.hover("row:1", "One", 52, 50, 300);
		assertTrue("moving within the same thing does not restart", tip.visible(400));

		tip.hover("row:2", "Two", 60, 80, 500);
		assertFalse(tip.visible(700));
		assertFalse(tip.visible(899));
		assertTrue(tip.visible(900));
		assertEquals("Two", tip.text());
	}

	@Test
	public void clearHidesAtOnceAndTheNextHoverStartsTheDelayAgain()
	{
		Tooltip tip = new Tooltip();
		tip.hover("row:1", "One", 50, 50, 0);
		assertTrue(tip.visible(500));

		tip.clear();
		assertFalse(tip.visible(500));
		assertEquals("", tip.text());
		tip.hover("row:1", "One", 50, 50, 600);
		assertFalse(tip.visible(900));
		assertTrue(tip.visible(1_000));
	}

	@Test
	public void anEmptyTextOrNoKeyHides()
	{
		Tooltip tip = new Tooltip();
		tip.hover("row:1", "One", 50, 50, 0);

		tip.hover("row:1", "", 50, 50, 100);
		assertFalse(tip.visible(5_000));
		tip.hover("row:1", "One", 50, 50, 200);
		tip.hover(null, "One", 50, 50, 300);
		assertFalse(tip.visible(5_000));
		tip.hover("row:1", null, 50, 50, 400);
		assertFalse(tip.visible(5_000));
	}

	@Test
	public void theTipSitsBelowAndRightOfThePointerWhenThereIsRoom()
	{
		Rectangle box = Tooltip.place(new Dimension(100, 40), new Point(50, 60), CANVAS, 14);

		assertEquals(new Rectangle(64, 74, 100, 40), box);
	}

	@Test
	public void theTipFlipsToTheLeftAndAboveAtTheEdges()
	{
		Rectangle right = Tooltip.place(new Dimension(100, 40), new Point(380, 60), CANVAS, 14);
		Rectangle bottom = Tooltip.place(new Dimension(100, 40), new Point(50, 290), CANVAS, 14);
		Rectangle corner = Tooltip.place(new Dimension(100, 40), new Point(395, 295), CANVAS, 14);

		assertEquals(380 - 14 - 100, right.x);
		assertEquals(74, right.y);
		assertEquals(64, bottom.x);
		assertEquals(290 - 14 - 40, bottom.y);
		assertEquals(395 - 14 - 100, corner.x);
		assertEquals(295 - 14 - 40, corner.y);
	}

	@Test
	public void theTipIsKeptInsideTheCanvasWhateverTheSizeAndPointer()
	{
		Rectangle canvas = new Rectangle(100, 100, 300, 200);
		int[] xs = {100, 101, 250, 399, 400, 50, 700};
		int[] ys = {100, 101, 200, 299, 300, 20, 900};
		for (int x : xs)
		{
			for (int y : ys)
			{
				Rectangle box = Tooltip.place(new Dimension(180, 90), new Point(x, y), canvas, 14);
				assertTrue(x + "," + y + " -> " + box, canvas.contains(box));
			}
		}
	}

	@Test
	public void aTipBiggerThanTheCanvasStartsAtItsCorner()
	{
		Rectangle box = Tooltip.place(new Dimension(500, 400), new Point(50, 50), CANVAS, 14);

		assertEquals(0, box.x);
		assertEquals(0, box.y);
	}

	@Test
	public void aLongTextWrapsToTheMaximumWidthAndStaysOnTheCanvas()
	{
		Tooltip tip = new Tooltip();
		String text = "A very long description of a creature that keeps going and going well past the edge "
			+ "of any reasonable tooltip box and so must wrap onto several lines";
		tip.hover("row:1", text, 390, 290, 0);
		FontMetrics fm = WidgetKit.metrics(STYLE, WindowFonts.Face.SMALL);
		Rectangle box = tip.bounds(STYLE, fm, CANVAS);

		assertTrue(CANVAS.contains(box));
		assertTrue(box.width <= STYLE.px(Tooltip.MAX_WIDTH_UNITS) + 2 * STYLE.metrics().pad);
		assertTrue("several lines", box.height > 2 * fm.getHeight());
	}

	@Test
	public void lineBreaksInTheTextAreKept()
	{
		Tooltip tip = new Tooltip();
		tip.hover("jar", "Jar odds\nCommon 60%\nRare 5%", 100, 100, 0);
		FontMetrics fm = WidgetKit.metrics(STYLE, WindowFonts.Face.SMALL);
		Rectangle one = new Tooltip().bounds(STYLE, fm, CANVAS);
		Rectangle three = tip.bounds(STYLE, fm, CANVAS);

		assertNull(one);
		assertTrue(three.height >= 3 * fm.getHeight());
	}

	@Test
	public void nothingPaintsBeforeTheDelayAndThemedPixelsPaintAfter()
	{
		Tooltip tip = new Tooltip();
		tip.hover("row:1", "Giant rat", 100, 100, 0);

		assertEquals(0, WidgetKit.painted(paintAt(tip, 399), 0, 0, 400, 300));
		BufferedImage shown = paintAt(tip, 400);
		assertTrue(WidgetKit.painted(shown, 0, 0, 400, 300) > 0);
		Rectangle box = tip.paint(WidgetKit.graphics(WidgetKit.canvas(400, 300)), STYLE, CANVAS, 400);
		int pixel = shown.getRGB(box.x + 4, box.y + box.height / 2);
		Color expected = STYLE.color(ThemeColor.TOOLTIP);
		assertTrue("tooltip colour", Math.abs(expected.getRed() - (pixel >> 16 & 0xFF)) <= 3
			&& Math.abs(expected.getGreen() - (pixel >> 8 & 0xFF)) <= 3
			&& Math.abs(expected.getBlue() - (pixel & 0xFF)) <= 3 && (pixel >>> 24) >= expected.getAlpha());
		assertEquals("only the tip paints, inside its box",
			0, WidgetKit.painted(shown, 0, 0, Math.max(0, box.x - STYLE.bodyPad()), 300));
	}

	@Test
	public void theHitBoxIsTheSameBeforeAndAfterItShows()
	{
		Tooltip tip = new Tooltip();
		tip.hover("row:1", "Giant rat", 100, 100, 0);
		FontMetrics fm = WidgetKit.metrics(STYLE, WindowFonts.Face.SMALL);
		Rectangle before = tip.bounds(STYLE, fm, CANVAS);
		Rectangle painted = tip.paint(WidgetKit.graphics(WidgetKit.canvas(400, 300)), STYLE, CANVAS, 500);

		assertEquals(before, painted);
	}
}
