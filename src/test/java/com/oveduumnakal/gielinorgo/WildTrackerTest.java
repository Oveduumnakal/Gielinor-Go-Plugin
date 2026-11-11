/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks when monsters may turn wild: the odds, the cap and the cooldown. */
public class WildTrackerTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);

	@Test
	public void oneInOneAlwaysTurnsWild()
	{
		WildTracker tracker = new WildTracker(new Random(1));

		WildEntry entry = tracker.offer(5, GOBLIN, 13, false, 0, 1);

		assertNotNull(entry);
		assertEquals(5, entry.npcIndex());
		assertEquals(entry, tracker.get(5));
	}

	@Test
	public void cooldownBlocksASecondCreature()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(tracker.offer(1, GOBLIN, 13, false, 0, 1));

		assertNull(tracker.offer(2, GOBLIN, 13, false, WildTracker.COOLDOWN_MS - 1, 1));
		assertNotNull(tracker.offer(2, GOBLIN, 13, false, WildTracker.COOLDOWN_MS, 1));
	}

	@Test
	public void neverMoreThanTheCap()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		for (int i = 0; i < 10; i++)
			tracker.offer(i, GOBLIN, 13, false, i * WildTracker.COOLDOWN_MS, 1);

		assertEquals(WildTracker.MAX_WILD, tracker.all().size());
	}

	@Test
	public void theSameMonsterIsNotOfferedTwice()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(tracker.offer(1, GOBLIN, 13, false, 0, 1));

		assertNull(tracker.offer(1, GOBLIN, 13, false, 10 * WildTracker.COOLDOWN_MS, 1));
	}

	@Test
	public void removingFreesASlot()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		tracker.offer(1, GOBLIN, 13, false, 0, 1);

		tracker.remove(1);

		assertNull(tracker.get(1));
		assertTrue(tracker.all().isEmpty());
	}

	@Test
	public void oddsAreRoughlyOneInN()
	{
		Random shared = new Random(99);
		int wild = 0;
		for (int i = 0; i < 4000; i++)
		{
			if (new WildTracker(shared).offer(1, GOBLIN, 13, false, 0, 8) != null)
				wild++;
		}

		assertTrue("expected about 500 of 4000, got " + wild, wild > 400 && wild < 600);
	}

	@Test
	public void monstersWithoutASpeciesNeverTurnWild()
	{
		assertNull(new WildTracker(new Random(1)).offer(1, null, 13, false, 0, 1));
	}
}
