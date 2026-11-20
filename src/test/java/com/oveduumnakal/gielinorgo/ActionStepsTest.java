/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks which XP drops count as action steps toward eggs, and the limits on them (#323). */
public class ActionStepsTest
{
	private static final long START = 1_000_000L;

	@Test
	public void aDropInASkillThatIsNotACombatSkillCounts()
	{
		for (String skill : new String[] {"COOKING", "FISHING", "WOODCUTTING", "MINING", "SMITHING", "FLETCHING",
			"HERBLORE", "AGILITY", "THIEVING", "PRAYER", "RUNECRAFT", "FARMING", "HUNTER"})
			assertTrue(skill, ActionSteps.counts(skill, 40));
	}

	@Test
	public void combatSkillsTheTotalAndEmptyDropsNeverCount()
	{
		for (String skill : new String[] {"ATTACK", "STRENGTH", "DEFENCE", "RANGED", "MAGIC", "HITPOINTS",
			"OVERALL"})
			assertFalse(skill, ActionSteps.counts(skill, 40));

		assertFalse(ActionSteps.counts("COOKING", 0));
		assertFalse(ActionSteps.counts("COOKING", -5));
		assertFalse(ActionSteps.counts(null, 40));
	}

	@Test
	public void aGameTickGivesAtMostOneStepHoweverManyDropsItHas()
	{
		ActionSteps steps = new ActionSteps();

		assertEquals(1, steps.take(10, START));
		assertEquals(0, steps.take(10, START));
		assertEquals(0, steps.take(10, START + 5));
		assertEquals(1, steps.take(11, START + 600));
	}

	@Test
	public void anHourHoldsAtMostTheCapAndTheRoomComesBackAsItPasses()
	{
		ActionSteps steps = new ActionSteps();
		int taken = 0;
		for (int tick = 0; tick < ActionSteps.PER_HOUR + 500; tick++)
			taken += steps.take(tick, START + tick * 600L / 10);

		assertEquals(ActionSteps.PER_HOUR, taken);
		assertEquals(0, steps.take(10_000, START + ActionSteps.WINDOW_MS - 1));
		assertEquals(1, steps.take(10_001, START + ActionSteps.WINDOW_MS + 1));
	}

	@Test
	public void resetForgetsTheHourAndTheTick()
	{
		ActionSteps steps = new ActionSteps();
		for (int tick = 0; tick < ActionSteps.PER_HOUR; tick++)
			steps.take(tick, START);

		assertEquals(0, steps.take(5_000, START));
		steps.reset();

		assertEquals(1, steps.take(5_000, START));
	}

	@Test
	public void theCapIsOneStepPerTickAtMostAndFarBelowWalking()
	{
		assertTrue(ActionSteps.PER_HOUR <= 6_000 / 5);
	}
}
