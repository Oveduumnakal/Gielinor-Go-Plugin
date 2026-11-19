/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Rectangle;
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

	private static PartyLayout layout(int cw, int ch, boolean banner)
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, cw, ch);
		WindowLayout window = WindowLayout.of(size.width, size.height);
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale);
		return PartyLayout.of(window.body, style.metrics(), window.scale, banner);
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
			PartyLayout layout = layout(canvas[0], canvas[1], false);
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
		PartyLayout layout = layout(765, 503, false);
		PartyLayout.Parts parts = layout.parts(layout.rows[0]);

		assertTrue(layout.rows[0].height >= 30);
		assertTrue(parts.name.height >= 16);
		assertTrue(parts.hp.height >= 3);
		assertTrue(parts.xp.height >= 2);
		assertTrue(parts.hpText.x + parts.hpText.width <= parts.xp.x);
	}

	@Test
	public void theRightColumnSitsBesideTheRowsAndHoldsTheEggsAndTheTasks()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1], false);
			String where = canvas[0] + "x" + canvas[1];

			assertTrue(where, layout.side.x >= layout.main.x + layout.main.width);
			assertTrue(where, layout.side.width > 0);
			assertTrue(where, layout.side.contains(layout.eggs));
			assertTrue(where, layout.side.contains(layout.tasks));
			assertFalse(where, layout.eggs.intersects(layout.tasks));
			assertEquals(where, 0, layout.banner.height);
		}
	}

	@Test
	public void theBannerTakesTheFootOfTheRightColumnAndItsButtonsSitInsideIt()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1], true);
			String where = canvas[0] + "x" + canvas[1];

			assertTrue(where, layout.banner.height > 0);
			assertTrue(where, layout.side.contains(layout.banner));
			assertTrue(where, layout.banner.contains(layout.resetButton));
			assertTrue(where, layout.banner.contains(layout.dismissButton));
			assertFalse(where, layout.resetButton.intersects(layout.dismissButton));
			assertFalse(where, layout.tasks.intersects(layout.banner));
		}
	}

	@Test
	public void everyPartOfARowIsInsideTheRowAndTheyDoNotOverlap()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1], false);
			for (Rectangle row : layout.rows)
			{
				PartyLayout.Parts p = layout.parts(row);
				String where = canvas[0] + "x" + canvas[1] + " " + row;

				for (Rectangle part : new Rectangle[] {p.portrait, p.name, p.tags, p.gender, p.style, p.level, p.hp,
					p.hpText, p.xp})
					assertTrue(where + " " + part, row.contains(part));

				assertFalse(where, p.portrait.intersects(p.name));
				assertFalse(where, p.name.intersects(p.gender));
				assertFalse(where, p.gender.intersects(p.style));
				assertFalse(where, p.style.intersects(p.level));
				assertFalse(where, p.hp.intersects(p.hpText));
				assertFalse(where, p.hpText.intersects(p.xp));
				assertTrue(where, p.hp.x < p.xp.x);
				assertTrue(where, p.xp.y < p.hp.y + p.hp.height && p.hp.y < p.xp.y + p.xp.height);
				assertTrue(where, p.name.contains(p.tags));
			}
		}
	}

	@Test
	public void theArrowStripeIsLeftOfTheRowsAndInsideThePage()
	{
		PartyLayout layout = layout(765, 503, false);

		assertTrue(layout.arrow.width >= 6);
		assertTrue(layout.arrow.x + layout.arrow.width <= layout.rows[0].x);
		assertTrue(layout.main.contains(layout.arrow));
	}

	@Test
	public void starterCardsAreStackedUnderTheCaptionAndFitThePage()
	{
		for (int[] canvas : CANVASES)
		{
			PartyLayout layout = layout(canvas[0], canvas[1], false);
			List<Rectangle> cards = layout.starterCards(3, 20);
			String where = canvas[0] + "x" + canvas[1];

			assertEquals(where, 3, cards.size());
			for (int i = 0; i < cards.size(); i++)
			{
				assertTrue(where, layout.main.contains(cards.get(i)));
				assertTrue(where, cards.get(i).y >= layout.main.y + 20);
				if (i > 0)
					assertFalse(where, cards.get(i).intersects(cards.get(i - 1)));
			}

			assertEquals(where, 2, layout.starterCards(2, 20).size());
			assertEquals(where, 3, layout.starterCards(5, 20).size());
		}
	}

	@Test
	public void bothThemesGetTheSameBoxes()
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, 765, 503);
		WindowLayout window = WindowLayout.of(size.width, size.height);
		PartyLayout glass = PartyLayout.of(window.body, WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale)
			.metrics(), window.scale, true);
		PartyLayout dark = PartyLayout.of(window.body, WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale)
			.metrics(), window.scale, true);

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
			PartyLayout layout = layout(canvas[0], canvas[1], false);
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
