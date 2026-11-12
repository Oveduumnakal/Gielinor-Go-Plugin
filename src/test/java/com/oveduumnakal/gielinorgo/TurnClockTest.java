/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the choice timer and the rules for walking away from a battle. */
public class TurnClockTest
{
	@Test
	public void secondsBecomeGameTicksRoundedUp()
	{
		assertEquals(17, TurnClock.ticksFor(10));
		assertEquals(9, TurnClock.ticksFor(5));
		assertEquals(50, TurnClock.ticksFor(30));
		assertEquals(1, TurnClock.ticksFor(0));
	}

	@Test
	public void aClockThatIsNotOpenNeverRunsOut()
	{
		TurnClock clock = new TurnClock(10);

		for (int i = 0; i < 100; i++)
			assertFalse(clock.tick());

		assertFalse(clock.running());
		assertEquals(0, clock.ticksLeft());
		assertEquals(0.0, clock.fractionLeft(), 0.0);
	}

	@Test
	public void theTimeIsUpAfterTheLimitAndOnlyOnce()
	{
		TurnClock clock = new TurnClock(10);
		int limit = TurnClock.ticksFor(10);
		clock.open();

		for (int i = 1; i < limit; i++)
			assertFalse("tick " + i, clock.tick());

		assertTrue(clock.tick());
		assertFalse("it stops when the time is up", clock.running());
		assertFalse(clock.tick());
	}

	@Test
	public void theTimeLeftCountsDown()
	{
		TurnClock clock = new TurnClock(10);
		clock.open();

		assertEquals(10, clock.secondsLeft());
		assertEquals(1.0, clock.fractionLeft(), 0.0);

		for (int i = 0; i < 8; i++)
			clock.tick();

		assertEquals(TurnClock.ticksFor(10) - 8, clock.ticksLeft());
		assertEquals(6, clock.secondsLeft());
		assertEquals((TurnClock.ticksFor(10) - 8) / (double) TurnClock.ticksFor(10), clock.fractionLeft(), 1e-12);
	}

	@Test
	public void openingAgainStartsOver()
	{
		TurnClock clock = new TurnClock(5);
		clock.open();
		for (int i = 0; i < 5; i++)
			clock.tick();

		clock.open();

		assertEquals(TurnClock.ticksFor(5), clock.ticksLeft());
		assertFalse(clock.tick());
	}

	@Test
	public void closingStopsTheCount()
	{
		TurnClock clock = new TurnClock(5);
		clock.open();
		clock.close();

		for (int i = 0; i < 20; i++)
			assertFalse(clock.tick());
	}

	@Test
	public void walkingMoreThanFifteenTilesAwayIsTooFarInEitherDirection()
	{
		assertFalse(TurnClock.tooFar(15, 15));
		assertFalse(TurnClock.tooFar(-15, 0));
		assertTrue(TurnClock.tooFar(16, 0));
		assertTrue(TurnClock.tooFar(0, -16));
		assertTrue(TurnClock.tooFar(-20, 3));
	}

	@Test
	public void leavingTheGameTheCreatureOrTheBattleCountsAsRunning()
	{
		assertFalse(TurnClock.forfeits(true, true, 3, 4));
		assertTrue("logging out or hopping", TurnClock.forfeits(false, true, 0, 0));
		assertTrue("the creature despawned", TurnClock.forfeits(true, false, 0, 0));
		assertTrue("walked away", TurnClock.forfeits(true, true, 0, 16));
	}
}
