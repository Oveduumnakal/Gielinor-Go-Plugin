/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CombatWatch}.
 */
public class CombatWatchTest
{
	/** Tests the combat window. */
	@Test
	public void combatLastsFiveSecondsPastTheLastBlow()
	{
		CombatWatch watch = new CombatWatch();
		assertFalse("never fought", watch.inCombat(1_000));

		watch.noteFighting(10_000);
		assertTrue(watch.inCombat(10_000));
		assertTrue(watch.inCombat(14_900));
		assertFalse(watch.inCombat(15_100));

		watch.noteFighting(20_000);
		assertTrue(watch.inCombat(24_900));
		watch.reset();
		assertFalse(watch.inCombat(20_000));
	}
}
