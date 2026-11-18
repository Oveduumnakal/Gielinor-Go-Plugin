/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Paints the tracker offline: both themes, several scales and line counts, with nothing wider than its box. */
public class TrackerPainterTest
{
	private static List<TrackerView.Line> lines(int count)
	{
		TrackerView.Line[] all = {
			new TrackerView.Line(TrackerView.Kind.TASK, "task:0", "Walk 4,000 tiles with a creature in your team",
				"12.3k / 99.9k", 0.4, null, false),
			new TrackerView.Line(TrackerView.Kind.TASK, "task:3", "Weekly: win 10 battles", "Done!", 1.0, null, true),
			new TrackerView.Line(TrackerView.Kind.EGG, "egg:0", "Legendary egg", "12,345 steps", 0.2,
				Rarity.LEGENDARY, false),
			new TrackerView.Line(TrackerView.Kind.EGG, "egg:1", "Rare egg", "Ready", 1.0, Rarity.RARE, false),
			new TrackerView.Line(TrackerView.Kind.EGG, "egg:2", "Vorkath the Undying Dragon egg", "99999 steps", 0.0,
				Rarity.EPIC, false)};
		return new ArrayList<>(Arrays.asList(all).subList(0, count));
	}

	@Test
	public void everyCountDrawsInsideItsBox()
	{
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (double scale : new double[] {WindowFit.MIN_SCALE, 1.0, 2.0})
				for (int shown = 1; shown <= 5; shown++)
				{
					int count = shown;
					WindowStyle style = WindowStyle.of(theme, scale);
					Dimension size = TrackerPainter.size(style, count);
					BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = image.createGraphics();
					WindowStyle.prepare(g);
					int[] drawn = {0};
					new TrackerPainter((text, width, height, box) ->
					{
						drawn[0]++;
						if (width > box.width)
							problems.append(theme).append(' ').append(scale).append(' ').append(count).append(": ")
								.append(text).append(" is ").append(width).append(" in ").append(box.width)
								.append('\n');
					}).paint(g, style, lines(count));
					g.dispose();
					assertTrue("something was painted", image.getRGB(size.width / 2, size.height / 2) != 0);
					assertTrue("text was drawn", drawn[0] >= count * 2);
				}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void theBoxGrowsWithTheLines()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.DARK_GLASS, 1.0);

		Dimension one = TrackerPainter.size(style, 1);
		Dimension two = TrackerPainter.size(style, 2);
		Dimension three = TrackerPainter.size(style, 3);
		Dimension five = TrackerPainter.size(style, 5);
		assertTrue(three.height > two.height);
		assertEquals(one.width, five.width);
	}
}
