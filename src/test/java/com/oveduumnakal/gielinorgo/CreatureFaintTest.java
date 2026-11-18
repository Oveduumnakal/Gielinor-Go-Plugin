/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the fainted flag on a creature: it stops resting, and reviving restores health and rest. */
public class CreatureFaintTest
{
	private static final double EPSILON = 1e-9;

	@Test
	public void aFaintedCreatureHasNoHealthAndNeverRestsBackUp()
	{
		Creature creature = new Creature("chicken", 3, 1_000L);

		creature.faint(2_000L);

		assertTrue(creature.fainted);
		assertEquals(0.0, creature.hpFraction, EPSILON);
		assertEquals(0.0, creature.hpFractionAt(2_000L), EPSILON);
		assertEquals(0.0, creature.hpFractionAt(2_000L + Creature.FULL_REST_MS * 10), EPSILON);
	}

	@Test
	public void reviveStandsTheCreatureUpAtTheGivenShareAndItRestsFromThere()
	{
		Creature creature = new Creature("chicken", 3, 1_000L);
		creature.faint(2_000L);

		creature.revive(0.5, 5_000L);

		assertFalse(creature.fainted);
		assertEquals(0.5, creature.hpFractionAt(5_000L), EPSILON);
		assertEquals(0.6, creature.hpFractionAt(5_000L + Creature.FULL_REST_MS / 10), EPSILON);
		assertEquals(1.0, creature.hpFractionAt(5_000L + Creature.FULL_REST_MS), EPSILON);
	}

	@Test
	public void reviveClampsTheShare()
	{
		Creature creature = new Creature("chicken", 3, 1_000L);

		creature.revive(7.0, 1_000L);
		assertEquals(1.0, creature.hpFraction, EPSILON);

		creature.revive(-3.0, 1_000L);
		assertEquals(0.0, creature.hpFraction, EPSILON);
	}

	@Test
	public void aCreatureThatNeverFaintedRestsAsBefore()
	{
		Creature creature = new Creature("chicken", 3, 1_000L);
		creature.hpFraction = 0.0;
		creature.hpStampMs = 1_000L;

		assertEquals(0.5, creature.hpFractionAt(1_000L + Creature.FULL_REST_MS / 2), EPSILON);
	}
}
