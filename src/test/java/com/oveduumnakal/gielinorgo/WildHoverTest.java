/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the hover delay and the rule for when the wild creature tooltip may show. */
public class WildHoverTest
{
	private static final long START = 10_000L;

	@Test
	public void nothingShowsBeforeAnythingIsSet()
	{
		assertNull(new WildHover().shown(START));
	}

	@Test
	public void theTextShowsOnlyAfterTheDelay()
	{
		WildHover hover = new WildHover();
		hover.update(1, "tip", START);

		assertNull(hover.shown(START));
		assertNull(hover.shown(START + WildHover.DELAY_MS - 1));
		assertEquals("tip", hover.shown(START + WildHover.DELAY_MS));
	}

	@Test
	public void theSameCreatureKeepsTheDelayRunningAndTakesTheNewText()
	{
		WildHover hover = new WildHover();
		hover.update(1, "old", START);
		hover.update(1, "new", START + 200);

		assertEquals("new", hover.shown(START + WildHover.DELAY_MS));
	}

	@Test
	public void anotherCreatureRestartsTheDelay()
	{
		WildHover hover = new WildHover();
		hover.update(1, "first", START);
		hover.update(2, "second", START + 250);

		assertNull(hover.shown(START + WildHover.DELAY_MS));
		assertEquals("second", hover.shown(START + 250 + WildHover.DELAY_MS));
	}

	@Test
	public void clearHidesAtOnceAndTheNextUpdateRestartsTheDelay()
	{
		WildHover hover = new WildHover();
		hover.update(1, "tip", START);
		hover.clear();

		assertNull(hover.shown(START + 5_000));

		hover.update(1, "tip", START + 5_000);

		assertNull(hover.shown(START + 5_000));
		assertEquals("tip", hover.shown(START + 5_000 + WildHover.DELAY_MS));
	}

	@Test
	public void theTooltipIsWantedOnlyWhenEnabledWithNoMenuOverTheSceneAndNoBattle()
	{
		for (int bits = 0; bits < 16; bits++)
		{
			boolean enabled = (bits & 8) != 0;
			boolean menuOpen = (bits & 4) != 0;
			boolean overScene = (bits & 2) != 0;
			boolean battle = (bits & 1) != 0;
			boolean expected = enabled && !menuOpen && overScene && !battle;

			assertEquals("bits " + bits, expected, WildHover.wanted(enabled, menuOpen, overScene, battle));
		}

		assertTrue(WildHover.wanted(true, false, true, false));
		assertFalse(WildHover.wanted(false, false, true, false));
	}
}
