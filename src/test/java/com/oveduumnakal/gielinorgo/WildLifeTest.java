/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the life of a wild creature's body on a fake clock. */
public class WildLifeTest
{
	private static WildLife born()
	{
		return new WildLife(0, new Random(1));
	}

	@Test
	public void itFadesInThenStands()
	{
		WildLife life = born();

		assertEquals(WildLife.State.APPEARING, life.state());
		assertEquals(0.0, life.opacity(0), 0.0001);
		assertEquals(0.5, life.opacity(WildLife.APPEAR_MS / 2), 0.0001);

		life.update(WildLife.APPEAR_MS, 3, true);

		assertEquals(WildLife.State.IDLE, life.state());
		assertEquals(1.0, life.opacity(WildLife.APPEAR_MS), 0.0001);
	}

	@Test
	public void aWanderIsDueAfterARestAndTakesItBackToIdle()
	{
		WildLife life = born();
		life.update(WildLife.APPEAR_MS, 3, true);
		assertFalse(life.wanderDue(WildLife.APPEAR_MS));

		long later = WildLife.APPEAR_MS + WildLife.WANDER_MAX_MS;
		life.update(later, 3, true);
		assertTrue(life.wanderDue(later));

		life.startWander();
		assertEquals(WildLife.State.ROAMING, life.state());
		assertFalse("not while it walks", life.wanderDue(later));

		life.endWander(later);
		assertEquals(WildLife.State.IDLE, life.state());
		assertFalse(life.wanderDue(later));
		assertTrue(life.wanderDue(later + WildLife.WANDER_MAX_MS));
	}

	@Test
	public void itLeavesWhenItsTimeIsUp()
	{
		WildLife life = born();
		life.update(WildLife.STAY_MS - 1, 3, true);
		assertEquals(WildLife.State.IDLE, life.state());

		life.update(WildLife.STAY_MS, 3, true);

		assertEquals(WildLife.State.LEAVING, life.state());
		assertEquals(1.0, life.opacity(WildLife.STAY_MS), 0.0001);
	}

	@Test
	public void itFadesOutOverTwoSecondsAndIsThenGone()
	{
		WildLife life = born();
		life.update(WildLife.STAY_MS, 3, true);

		assertEquals(0.5, life.opacity(WildLife.STAY_MS + WildLife.FADE_MS / 2), 0.0001);
		life.update(WildLife.STAY_MS + WildLife.FADE_MS - 1, 3, true);
		assertFalse(life.gone());

		life.update(WildLife.STAY_MS + WildLife.FADE_MS, 3, true);

		assertTrue(life.gone());
		assertEquals(0.0, life.opacity(WildLife.STAY_MS + WildLife.FADE_MS), 0.0001);
	}

	@Test
	public void itLeavesWhenThePlayerIsMoreThan25TilesAway()
	{
		WildLife life = born();
		life.update(5_000, WildLife.LEAVE_TILES, true);
		assertEquals(WildLife.State.IDLE, life.state());

		life.update(6_000, WildLife.LEAVE_TILES + 1, true);

		assertEquals(WildLife.State.LEAVING, life.state());
	}

	@Test
	public void itLeavesOnAPlaneChange()
	{
		WildLife life = born();
		life.update(5_000, 3, true);

		life.update(6_000, 3, false);

		assertEquals(WildLife.State.LEAVING, life.state());
	}

	@Test
	public void aBattlePausesTheStayTimer()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		long left = life.stayLeftMs();
		assertTrue(life.beginBattle());

		life.update(100_000, 3, true);
		life.update(200_000, 3, true);

		assertEquals(left, life.stayLeftMs());
		assertEquals(WildLife.State.IN_BATTLE, life.state());

		life.endBattle(200_000);
		life.update(210_000, 3, true);
		assertEquals(left - 10_000, life.stayLeftMs());
	}

	@Test
	public void aKnockedOutCreatureLiesAndTheTimerStaysPaused()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		life.beginBattle();

		life.knockOut();

		assertTrue(life.knockedOut());
		long left = life.stayLeftMs();
		life.update(500_000, 3, true);
		assertEquals(left, life.stayLeftMs());

		life.endBattle(500_000);
		assertEquals(WildLife.State.IDLE, life.state());
	}

	@Test
	public void aCatchPausesTheTimerToo()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		long left = life.stayLeftMs();

		assertTrue(life.beginCatch());
		life.update(400_000, 3, true);

		assertEquals(left, life.stayLeftMs());
		assertEquals(WildLife.State.IN_CATCH, life.state());
	}

	@Test
	public void aCreatureThatBrokeFreeStandsAgain()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		life.beginCatch();

		life.endCatch(true, 12_000);

		assertEquals(WildLife.State.IDLE, life.state());
		assertFalse(life.wanderDue(12_000));
	}

	@Test
	public void aCaughtOrFledCreatureIsGoneAtOnce()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		life.beginCatch();

		life.endCatch(false, 12_000);

		assertTrue(life.gone());
	}

	@Test
	public void removeEndsItAtOnce()
	{
		WildLife life = born();

		life.remove();

		assertTrue(life.gone());
	}

	@Test
	public void noBattleStartsOnACreatureThatIsLeavingOrBusy()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		assertTrue(life.beginBattle());
		assertFalse("already in one", life.beginBattle());

		WildLife leaving = born();
		leaving.update(WildLife.STAY_MS, 3, true);
		assertFalse(leaving.beginBattle());
		assertFalse(leaving.beginCatch());
	}

	@Test
	public void aWalkingCreatureCanStillBeBattled()
	{
		WildLife life = born();
		life.update(WildLife.APPEAR_MS + WildLife.WANDER_MAX_MS, 3, true);
		life.startWander();

		assertTrue(life.beginBattle());
	}

	@Test
	public void distanceLeavesEvenInABattle()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		life.beginBattle();

		life.update(11_000, WildLife.LEAVE_TILES + 5, true);

		assertTrue(life.leaving());
	}

	@Test
	public void aClockThatRunsBackwardsLosesNoTime()
	{
		WildLife life = born();
		life.update(10_000, 3, true);
		long left = life.stayLeftMs();

		life.update(5_000, 3, true);

		assertEquals(left, life.stayLeftMs());
	}
}
