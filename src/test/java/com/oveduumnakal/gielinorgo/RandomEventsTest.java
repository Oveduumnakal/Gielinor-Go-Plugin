/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link RandomEvents}.
 */
public class RandomEventsTest
{
	/** Random event NPCs are recognised whatever their case or colour tags. */
	@Test
	public void recognisesRandomEvents()
	{
		assertTrue(RandomEvents.isRandomEvent("Genie"));
		assertTrue(RandomEvents.isRandomEvent("Drunken Dwarf"));
		assertTrue(RandomEvents.isRandomEvent("<col=ffff00>Dr Jekyll</col>"));
		assertTrue(RandomEvents.isRandomEvent("Sandwich lady"));
	}

	/** Real monsters and missing names are never excluded. */
	@Test
	public void keepsRealCombat()
	{
		assertFalse(RandomEvents.isRandomEvent("Goblin"));
		assertFalse(RandomEvents.isRandomEvent("Zombie"));
		assertFalse(RandomEvents.isRandomEvent("Rock golem"));
		assertFalse(RandomEvents.isRandomEvent(null));
	}
}
