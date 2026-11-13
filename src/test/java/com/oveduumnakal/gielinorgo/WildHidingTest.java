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

/** Checks when wild creatures are hidden for real combat and when they come back. */
public class WildHidingTest
{
	@Test
	public void nothingIsHiddenOutOfCombat()
	{
		for (WildLife.State state : WildLife.State.values())
			assertFalse(state.name(), WildHiding.hides(false, state, false));
	}

	@Test
	public void everyCreatureIsHiddenInCombat()
	{
		assertTrue(WildHiding.hides(true, WildLife.State.APPEARING, false));
		assertTrue(WildHiding.hides(true, WildLife.State.IDLE, false));
		assertTrue(WildHiding.hides(true, WildLife.State.ROAMING, false));
		assertTrue(WildHiding.hides(true, WildLife.State.KNOCKED_OUT, false));
		assertTrue(WildHiding.hides(true, WildLife.State.IN_CATCH, false));
		assertTrue(WildHiding.hides(true, WildLife.State.LEAVING, false));
	}

	@Test
	public void theFoeOfABattleInProgressIsNeverHidden()
	{
		assertFalse(WildHiding.hides(true, WildLife.State.IN_BATTLE, false));
		assertFalse(WildHiding.hides(true, WildLife.State.IDLE, true));
		assertFalse(WildHiding.hides(true, WildLife.State.KNOCKED_OUT, true));
	}

	@Test
	public void nothingNewAppearsInCombat()
	{
		assertFalse(WildHiding.mayAppear(true));
		assertTrue(WildHiding.mayAppear(false));
	}

	@Test
	public void theStayTimerKeepsRunningWhileHidden()
	{
		WildLife life = new WildLife(0, new Random(1));
		life.update(WildLife.APPEAR_MS, 0, true);
		long before = life.stayLeftMs();
		life.update(WildLife.APPEAR_MS + 60_000L, 0, true);
		assertEquals(before - 60_000L, life.stayLeftMs());

		life.update(WildLife.STAY_MS + 10_000L, 0, true);
		assertTrue(life.leaving());
		life.update(WildLife.STAY_MS + 20_000L, 0, true);
		assertTrue(life.gone());
	}
}
