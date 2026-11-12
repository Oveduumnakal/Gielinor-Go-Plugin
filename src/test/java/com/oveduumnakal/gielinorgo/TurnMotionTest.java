/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the timing of one turn of an acted-out battle. */
public class TurnMotionTest
{
	private static final double CLOSE = 0.0001;

	private static final double STEP = 1.25;

	@Test
	public void aTurnIsOneBeatLong()
	{
		assertEquals(BattleTimeline.BEAT_TICKS * 30, TurnMotion.TURN_TICKS);
		assertEquals(90, TurnMotion.TURN_TICKS);
	}

	@Test
	public void theStepStartsAndEndsAtRest()
	{
		assertEquals(0.0, TurnMotion.offset(0, STEP), CLOSE);
		assertEquals(0.0, TurnMotion.offset(TurnMotion.DASH_START, STEP), CLOSE);
		assertEquals(0.0, TurnMotion.offset(TurnMotion.RETURN_END, STEP), CLOSE);
		assertEquals(0.0, TurnMotion.offset(TurnMotion.TURN_TICKS, STEP), CLOSE);
		assertEquals(0.0, TurnMotion.offset(TurnMotion.TURN_TICKS + 40, STEP), CLOSE);
		assertEquals(0.0, TurnMotion.offset(-5, STEP), CLOSE);
	}

	@Test
	public void theAttackerStaysAtTheTargetFromTheDashToTheReturn()
	{
		for (int age = TurnMotion.DASH_END; age <= TurnMotion.RETURN_START; age++)
			assertEquals(STEP, TurnMotion.offset(age, STEP), CLOSE);
	}

	@Test
	public void theStepNeverGoesBeyondTheDistanceOrBackwards()
	{
		double last = 0.0;
		for (int age = 0; age <= TurnMotion.RETURN_START; age++)
		{
			double now = TurnMotion.offset(age, STEP);
			assertTrue("age " + age, now >= last - CLOSE && now <= STEP + CLOSE);
			last = now;
		}

		for (int age = TurnMotion.RETURN_START; age <= TurnMotion.TURN_TICKS; age++)
		{
			double now = TurnMotion.offset(age, STEP);
			assertTrue("age " + age, now <= last + CLOSE && now >= -CLOSE);
			last = now;
		}
	}

	@Test
	public void theAttackerWalksOnlyInTheDashAndOnTheWayBack()
	{
		assertFalse(TurnMotion.walking(0));
		assertFalse(TurnMotion.walking(TurnMotion.DASH_START - 1));
		assertTrue(TurnMotion.walking(TurnMotion.DASH_START));
		assertTrue(TurnMotion.walking(TurnMotion.DASH_END - 1));
		assertFalse(TurnMotion.walking(TurnMotion.DASH_END));
		assertFalse(TurnMotion.walking(TurnMotion.RETURN_START - 1));
		assertTrue(TurnMotion.walking(TurnMotion.RETURN_START));
		assertTrue(TurnMotion.walking(TurnMotion.RETURN_END - 1));
		assertFalse(TurnMotion.walking(TurnMotion.RETURN_END));
		assertFalse(TurnMotion.walking(TurnMotion.TURN_TICKS - 1));
	}

	@Test
	public void theMeleeStrikeStartsBeforeTheBlowLands()
	{
		assertEquals(20, TurnMotion.strikeTick());
		assertTrue(TurnMotion.strikeTick() > TurnMotion.DASH_START);
		assertTrue(TurnMotion.strikeTick() < TurnMotion.IMPACT_TICK);
	}

	@Test
	public void everyStyleEndsItsTurnInsideTheBeatAndLandsOnTheSameTick()
	{
		assertTrue(TurnMotion.RETURN_END <= TurnMotion.TURN_TICKS);
		assertTrue(TurnMotion.IMPACT_TICK < TurnMotion.TURN_TICKS);
		assertTrue(TurnMotion.DASH_END < TurnMotion.IMPACT_TICK);
		assertTrue(TurnMotion.LAUNCH_TICK < TurnMotion.IMPACT_TICK);
		assertTrue(TurnMotion.SHOT_TICK <= TurnMotion.LAUNCH_TICK);
		assertEquals(30, TurnMotion.IMPACT_TICK);
		assertEquals(TurnMotion.LAUNCH_TICK, ProjectilePath.LAUNCH_TICK);
	}

	@Test
	public void theGapsAreTwoTilesAndOne()
	{
		assertEquals(2.0, TurnMotion.GAP_TILES, CLOSE);
		assertEquals(1.0, TurnMotion.NEAR_GAP_TILES, CLOSE);
		assertEquals(0.75, TurnMotion.CONTACT_TILES, CLOSE);
		assertTrue(TurnMotion.NEAR_GAP_TILES > TurnMotion.CONTACT_TILES);
	}

	@Test
	public void aCutOffStepEasesBackToRest()
	{
		assertEquals(0.8, TurnMotion.settle(0.8, 0), CLOSE);
		assertEquals(0.0, TurnMotion.settle(0.8, TurnMotion.SETTLE_TICKS), CLOSE);
		double last = 0.8;
		for (int age = 1; age <= TurnMotion.SETTLE_TICKS; age++)
		{
			double now = TurnMotion.settle(0.8, age);
			assertTrue(now <= last + CLOSE && now >= 0.0);
			last = now;
		}
	}
}
