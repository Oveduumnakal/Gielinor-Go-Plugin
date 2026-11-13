/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the odds of a skilling encounter and its cooldown. */
public class SkillingRulesTest
{
	@Test
	public void theChanceGrowsWithTheSizeOfTheDrop()
	{
		assertTrue(SkillingRules.chance(100, 8) > SkillingRules.chance(10, 8));
		assertEquals(0.0, SkillingRules.chance(0, 8), 0.0);
	}

	@Test
	public void theChanceStopsAtItsCap()
	{
		assertEquals(SkillingRules.MAX_CHANCE, SkillingRules.chance(1_000_000, 8), 0.0);
	}

	@Test
	public void aRarerSettingMakesItRarer()
	{
		assertTrue(SkillingRules.chance(100, 16) < SkillingRules.chance(100, 8));
		assertEquals(SkillingRules.chance(100, 8) / 2, SkillingRules.chance(100, 16), 1e-12);
	}

	@Test
	public void steadySkillingLandsOneEncounterEverySixToElevenMinutes()
	{
		double[] xpPerHour = {40_000, 50_000, 70_000};
		for (double perHour : xpPerHour)
		{
			double minutes = SkillingRules.COOLDOWN_MS / 60_000.0 + SkillingRules.XP_PER_ENCOUNTER / perHour * 60;

			assertTrue(minutes >= 5 && minutes <= 10);
		}
	}

	@Test
	public void onlyTheFiveGatheringSkillsCount()
	{
		assertTrue(SkillingRules.gathering("FISHING"));
		assertTrue(SkillingRules.gathering("WOODCUTTING"));
		assertTrue(SkillingRules.gathering("MINING"));
		assertTrue(SkillingRules.gathering("FARMING"));
		assertTrue(SkillingRules.gathering("HUNTER"));
		assertFalse(SkillingRules.gathering("COOKING"));
		assertFalse(SkillingRules.gathering("ATTACK"));
	}

	@Test
	public void theCooldownHoldsUntilItIsOver()
	{
		assertFalse(SkillingRules.ready(1_000, 1_000 + SkillingRules.COOLDOWN_MS - 1));
		assertTrue(SkillingRules.ready(1_000, 1_000 + SkillingRules.COOLDOWN_MS));
	}
}
