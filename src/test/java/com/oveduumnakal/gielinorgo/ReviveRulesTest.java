/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the revive numbers and where a free revive is allowed. */
public class ReviveRulesTest
{
	@Test
	public void theNumbersAreTheOnesTheDesignSays()
	{
		assertEquals(0.5, ReviveRules.BANK_TO, 1e-9);
		assertEquals(0.25, ReviveRules.RESCUE_TO, 1e-9);
		assertEquals(10 * 60 * 1000L, ReviveRules.RESCUE_MS);
	}

	@Test
	public void aFreeReviveFollowsTheTeamSwapRule()
	{
		assertTrue(ReviveRules.mayReviveHere(true, false, false));
		assertTrue(ReviveRules.mayReviveHere(false, true, false));
		assertTrue(ReviveRules.mayReviveHere(false, false, true));
		assertFalse(ReviveRules.mayReviveHere(false, false, false));
	}

	@Test
	public void theRefusalNamesTheCreature()
	{
		assertEquals("Go to a bank or a city to revive Bob", ReviveRules.placeRefusal("Bob"));
	}

	@Test
	public void theRescueNeedsEveryCreatureFaintedAndTenMinutesSinceTheNewestFaint()
	{
		long faint = 1_000_000L;

		assertFalse("nine minutes", ReviveRules.rescueDue(faint + 9 * 60_000L, faint, false, true));
		assertTrue("ten minutes", ReviveRules.rescueDue(faint + 10 * 60_000L, faint, false, true));
		assertFalse("one is alive", ReviveRules.rescueDue(faint + 60 * 60_000L, faint, true, true));
		assertFalse("owns nothing", ReviveRules.rescueDue(faint + 60 * 60_000L, faint, false, false));
	}
}
