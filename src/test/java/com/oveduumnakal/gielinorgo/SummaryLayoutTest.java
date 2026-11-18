/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
}
