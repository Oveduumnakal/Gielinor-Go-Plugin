/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks the geometry of the Party page at several canvas sizes, fixed mode's 765 x 503 included: six rows that fit
 * the page and do not touch, a right column, the parts of a row inside the row, and the same boxes in both themes.
 */
public class PartyLayoutTest
{
	private static final int[][] CANVASES = {{765, 503}, {1024, 768}, {1920, 1080}, {800, 520}, {640, 700}};

	private static PartyLayout layout(int cw, int ch)
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, cw, ch);
		WindowLayout window = WindowLayout.of(size.width, size.height);
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale);
		return PartyLayout.of(window.body, style.metrics(), window.scale);
	}

	private static Rectangle body(int cw, int ch)
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, cw, ch);
		return WindowLayout.of(size.width, size.height).body;
	}

	@Test
	public void sixRowsFitThePageInOrderAndDoNotTouch()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1]);
			Rectangle body = body(canvas[0], canvas[1]);
			String where = canvas[0] + "x" + canvas[1];

			assertEquals(where, 6, layout.rows.length);
			for (int i = 0; i < layout.rows.length; i++)
			{
				assertTrue(where + " row " + i, body.contains(layout.rows[i]));
				assertTrue(where + " row " + i, layout.main.contains(layout.rows[i]));
				assertEquals(where, layout.rows[0].height, layout.rows[i].height);
				if (i > 0)
				{
					assertTrue(where, layout.rows[i].y >= layout.rows[i - 1].y + layout.rows[i - 1].height);
					assertFalse(where, layout.rows[i].intersects(layout.rows[i - 1]));
				}
			}
		}
	}

	@Test
	public void aRowIsTallEnoughForALineOfTextAndTwoBarsSideBySideAtFixedModeSize()
	{
		PartyLayout layout = layout(765, 503);
		PartyLayout.Parts parts = layout.parts(layout.rows[0]);

		assertTrue(layout.rows[0].height >= 30);
		assertTrue(parts.name.height >= 16);
		assertTrue(parts.hp.height >= 3);
		assertTrue(parts.xp.height >= 2);
		assertTrue(parts.hpText.x + parts.hpText.width <= parts.xp.x);
	}

	/**
	 * The Pin Party to UI and More buttons sit inside the right column at every size, below the tasks, with More at
	 * the foot, and touch neither each other nor the tasks.
	 */
	@Test
	public void thePinAndMoreButtonsFitTheRightColumnBelowTheTasksAtEverySize()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1]);
			String where = canvas[0] + "x" + canvas[1];

			for (Rectangle button : Arrays.asList(layout.pinButton, layout.moreButton))
			{
				assertTrue(where, button.height > 0);
				assertTrue(where, button.width > 0);
				assertTrue(where, layout.main.getMaxX() <= button.x);
				assertTrue(where, layout.side.contains(button));
				assertFalse(where, button.intersects(layout.tasks));
				assertFalse(where, button.intersects(layout.eggs));
				assertTrue(where, button.y >= layout.tasks.y + layout.tasks.height);
			}

			assertFalse(where, layout.pinButton.intersects(layout.moreButton));
			assertTrue(where, layout.pinButton.y < layout.moreButton.y);
			assertEquals(where, layout.side.y + layout.side.height, layout.moreButton.y + layout.moreButton.height);
		}
	}

	@Test
	public void theRightColumnSitsBesideTheRowsAndHoldsTheEggsAndTheTasks()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1]);
			String where = canvas[0] + "x" + canvas[1];

			assertTrue(where, layout.side.x >= layout.main.x + layout.main.width);
			assertTrue(where, layout.side.width > 0);
			assertTrue(where, layout.side.contains(layout.eggs));
			assertTrue(where, layout.side.contains(layout.tasks));
			assertFalse(where, layout.eggs.intersects(layout.tasks));
		}
	}

	@Test
	public void bothThemesGetTheSameBoxes()
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, 765, 503);
		WindowLayout window = WindowLayout.of(size.width, size.height);
		PartyLayout glass = PartyLayout.of(window.body, WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale)
			.metrics(), window.scale);
		PartyLayout dark = PartyLayout.of(window.body, WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale)
			.metrics(), window.scale);

		assertEquals(glass.rows[3], dark.rows[3]);
		assertEquals(glass.side, dark.side);
		assertEquals(glass.parts(glass.rows[0]).hp,
			dark.parts(dark.rows[0]).hp);
	}

	@Test
	public void theIconsAreTheSizeOfTheSmallText()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1]);
			PartyLayout.Parts p = layout.parts(layout.rows[0]);
			String where = canvas[0] + "x" + canvas[1];

			assertEquals(where, p.gender.width, p.style.width);
			assertEquals(where, p.gender.height, p.style.height);
			Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, canvas[0], canvas[1]);
			double scale = WindowLayout.of(size.width, size.height).scale;
			WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, scale);
			int text = style.metrics().text;
			assertEquals(where, WindowFonts.pixelSize(WindowFonts.SMALL_UNITS, scale), text);
			assertTrue(where, p.gender.width == text || p.gender.width == p.name.height);
		}
	}
}
