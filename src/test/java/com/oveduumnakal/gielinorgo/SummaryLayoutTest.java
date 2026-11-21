/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the Summary layout's action buttons: the team and the Storage sets take the same rectangles. */
public class SummaryLayoutTest
{
	private static SummaryLayout layout(String[] labels)
	{
		WindowStyle style = WidgetKit.style(1.0);
		return SummaryLayout.of(new Rectangle(10, 40, 520, 300), style.metrics(), 16, 12, labels);
	}

	@Test
	public void theFourArgumentFormUsesTheTeamButtons()
	{
		WindowStyle style = WidgetKit.style(1.0);
		SummaryLayout old = SummaryLayout.of(new Rectangle(10, 40, 520, 300), style.metrics(), 16, 12);

		assertArrayEquals(SummaryLayout.BUTTONS, old.labels());
	}

	@Test
	public void theStorageButtonsAreBackRenameMovesToTeamReleaseUseItem()
	{
		assertArrayEquals(new String[] {"Back", "Rename", "Moves", "To team", "Release", "Use item"},
			SummaryLayout.STORED_BUTTONS);
		assertEquals(SummaryLayout.BUTTONS.length, SummaryLayout.STORED_BUTTONS.length);
	}

	@Test
	public void theStorageButtonsSitWhereTheTeamButtonsSit()
	{
		SummaryLayout team = layout(SummaryLayout.BUTTONS);
		SummaryLayout stored = layout(SummaryLayout.STORED_BUTTONS);

		assertArrayEquals(team.buttons, stored.buttons);
		assertEquals(team.stats, stored.stats);
		assertEquals(team.moves, stored.moves);
		assertEquals(team.left, stored.left);
		assertArrayEquals(SummaryLayout.STORED_BUTTONS, stored.labels());
	}

	private static SummaryLayout at(double scale, int width, int height)
	{
		WindowStyle style = WidgetKit.style(scale);
		int line = (int) Math.round(24 * scale * 1.25);
		int small = (int) Math.round(20 * scale * 1.25);
		return SummaryLayout.of(new Rectangle(8, 40, width, height), style.metrics(), line, small);
	}

	@Test
	public void theTwoColumnsHoldTheirBoxesAtSeveralSizes()
	{
		int[][] sizes = {{380, 210, 50}, {560, 300, 75}, {760, 400, 100}, {1100, 600, 150}, {1500, 800, 200}};
		for (int[] size : sizes)
		{
			SummaryLayout l = at(size[2] / 100.0, size[0], size[1]);
			Rectangle body = new Rectangle(8, 40, size[0], size[1]);
			String where = size[0] + "x" + size[1];

			assertTrue(where, l.left.x + l.left.width < l.right.x);
			for (Rectangle r : new Rectangle[] {l.header, l.chips, l.portrait, l.stats, l.nature, l.bond, l.moves})
				assertTrue(where + " " + r, body.contains(r) && r.width > 0 && r.height > 0);

			assertTrue(where, l.left.contains(l.header) && l.left.contains(l.chips) && l.left.contains(l.portrait));
			assertTrue(where, l.right.contains(l.stats) && l.right.contains(l.nature) && l.right.contains(l.moves));
			assertFalse(where, l.stats.intersects(l.nature));
			assertFalse(where, l.stats.intersects(l.moves));
			assertFalse(where, l.nature.intersects(l.bond));
			assertFalse(where, l.bond.intersects(l.moves));
			assertFalse(where, l.header.intersects(l.chips));
			assertFalse(where, l.chips.intersects(l.portrait));
			assertTrue(where, l.nature.x >= l.stats.x + l.stats.width);
			assertTrue(where, l.nature.y == l.stats.y);
			assertTrue(where, l.bond.y > l.nature.y && l.bond.x == l.nature.x);
			assertTrue(where, l.moves.y >= l.stats.y + l.stats.height);
			if (l.description.height > 0)
			{
				assertTrue(where, l.left.contains(l.description));
				assertFalse(where, l.portrait.intersects(l.description));
				assertTrue(where, l.description.y >= l.portrait.y + l.portrait.height);
			}
		}
	}

	@Test
	public void aBiggerWindowGivesTheDescriptionRoomAndTheStatRowsStayInsideTheBox()
	{
		SummaryLayout big = at(1.5, 1100, 600);

		assertTrue(big.description.height > 0);
		for (int i = 0; i < SummaryLayout.BAR_ROWS; i++)
		{
			assertTrue(big.stats.contains(big.statRow(i)));
			assertTrue(big.descriptionText().height > 0);
		}

		for (int i = 0; i < SummaryContent.MAX_MOVES; i++)
			assertTrue(big.moves.contains(big.movePill(i)));

		assertTrue(big.description.contains(big.growsLine()));
	}

	@Test
	public void theButtonsStandInTwoRowsOfThreeWithBackAtTheBottomRight()
	{
		SummaryLayout l = at(1.0, 760, 400);
		Rectangle back = l.buttons[0];
		assertEquals("Back", l.labels()[0]);
		for (int i = 1; i < l.buttons.length; i++)
		{
			assertTrue(l.buttons[i].y < back.y == i < 4);
			assertFalse(l.buttons[i].intersects(back));
		}

		assertTrue(l.buttons[1].x < l.buttons[2].x && l.buttons[2].x < l.buttons[3].x);
		assertTrue(l.buttons[4].x < l.buttons[5].x && l.buttons[5].x < back.x);
		assertEquals(l.buttons[3].x, back.x);
		assertEquals(40 + 400, back.y + back.height);
		assertTrue(l.moves.y + l.moves.height <= l.buttons[1].y);
	}
}
