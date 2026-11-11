/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks when monsters may turn wild: the odds, the cap, the cooldown and the memory of past rolls. */
public class WildTrackerTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);

	private static final int ID = 3029;

	@Test
	public void oneInOneAlwaysTurnsWild()
	{
		WildTracker tracker = new WildTracker(new Random(1));

		WildEntry entry = tracker.offer(5, ID, GOBLIN, 13, false, 0, 1);

		assertNotNull(entry);
		assertEquals(5, entry.npcIndex());
		assertEquals(entry, tracker.get(5));
	}

	@Test
	public void cooldownBlocksASecondCreature()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(tracker.offer(1, ID, GOBLIN, 13, false, 0, 1));

		assertNull(tracker.offer(2, ID, GOBLIN, 13, false, WildTracker.COOLDOWN_MS - 1, 1));
		assertNotNull(tracker.offer(2, ID, GOBLIN, 13, false, WildTracker.COOLDOWN_MS, 1));
	}

	@Test
	public void neverMoreThanTheCap()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		for (int i = 0; i < 10; i++)
			tracker.offer(i, ID, GOBLIN, 13, false, i * WildTracker.COOLDOWN_MS, 1);

		assertEquals(WildTracker.MAX_WILD, tracker.all().size());
	}

	@Test
	public void theSameMonsterIsNotOfferedTwice()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(tracker.offer(1, ID, GOBLIN, 13, false, 0, 1));

		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 10 * WildTracker.COOLDOWN_MS, 1));
	}

	@Test
	public void removingFreesASlot()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);

		tracker.remove(1, 0);

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
			if (new WildTracker(shared).offer(1, ID, GOBLIN, 13, false, 0, 8) != null)
				wild++;
		}

		assertTrue("expected about 500 of 4000, got " + wild, wild > 400 && wild < 600);
	}

	@Test
	public void monstersWithoutASpeciesNeverTurnWild()
	{
		assertNull(new WildTracker(new Random(1)).offer(1, ID, null, 13, false, 0, 1));
	}

	@Test
	public void aWildCreatureIsStillWildWhenItComesBack()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);
		entry.markBattled(true);

		tracker.leftView(1, 1_000);
		assertNull(tracker.get(1));

		assertSame(entry, tracker.restore(1, ID, 1_000 + WildTracker.MEMORY_MS - 1));
		assertSame(entry, tracker.get(1));
		assertTrue(tracker.get(1).battled());
	}

	@Test
	public void aMonsterThatRolledNoDoesNotRollAgainOnReload()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 0, Integer.MAX_VALUE));
		tracker.leftView(1, 1_000);

		assertFalse(tracker.mayRoll(1, ID, 2_000));
		assertNull(tracker.restore(1, ID, 2_000));
		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 2_000, 1));
	}

	@Test
	public void aRollIsForgottenAfterTheMemoryRunsOut()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 0, Integer.MAX_VALUE));
		tracker.leftView(1, 1_000);

		long later = 1_000 + WildTracker.MEMORY_MS;
		assertTrue(tracker.mayRoll(1, ID, later));
		assertNotNull(tracker.offer(1, ID, GOBLIN, 13, false, later, 1));
	}

	@Test
	public void aWildCreatureIsForgottenAfterTheMemoryRunsOut()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);
		tracker.leftView(1, 1_000);

		assertNull(tracker.restore(1, ID, 1_000 + WildTracker.MEMORY_MS));
	}

	@Test
	public void aWildCreatureInViewKeepsItsRollPastTheMemory()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);
		tracker.offer(2, ID, GOBLIN, 13, false, 10 * WildTracker.MEMORY_MS, Integer.MAX_VALUE);
		tracker.leftView(1, 10 * WildTracker.MEMORY_MS);

		assertSame(entry, tracker.restore(1, ID, 10 * WildTracker.MEMORY_MS + 1));
	}

	@Test
	public void aDifferentMonsterOnAReusedIndexRollsFresh()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 0, Integer.MAX_VALUE));
		tracker.leftView(1, 1_000);

		assertTrue(tracker.mayRoll(1, ID + 1, 2_000));
		assertNull(tracker.restore(1, ID + 1, 2_000));
	}

	@Test
	public void aCaughtCreatureIsNotWildWhenItComesBack()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);
		tracker.remove(1, 1_000);
		tracker.leftView(1, 2_000);

		assertNull(tracker.restore(1, ID, 3_000));
		assertFalse(tracker.mayRoll(1, ID, 3_000));
	}

	@Test
	public void aRestoredCreatureStillCountsTowardTheCap()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		tracker.offer(0, ID, GOBLIN, 13, false, 0, 1);
		tracker.leftView(0, 1);
		for (int i = 1; i <= WildTracker.MAX_WILD; i++)
			tracker.offer(i, ID, GOBLIN, 13, false, i * WildTracker.COOLDOWN_MS, 1);

		assertNull(tracker.restore(0, ID, 4 * WildTracker.COOLDOWN_MS));
	}

	@Test
	public void aCooldownBlockIsNotARoll()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);

		assertNull(tracker.offer(2, ID, GOBLIN, 13, false, 1, 1));
		assertTrue(tracker.mayRoll(2, ID, 2));
	}

	@Test
	public void clearingTheWildKeepsABattledCreatureForWhenItComesBack()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);
		entry.markBattled(true);

		tracker.clearWild(5_000);

		assertTrue(tracker.all().isEmpty());
		assertNull(tracker.get(1));
		assertFalse(tracker.mayRoll(1, ID, 6_000));
		assertSame(entry, tracker.restore(1, ID, 6_000));
		assertTrue(tracker.get(1).battled());
	}

	@Test
	public void clearingTheWildKeepsANoRollAsNo()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 0, Integer.MAX_VALUE));

		tracker.clearWild(5_000);

		assertFalse(tracker.mayRoll(1, ID, 6_000));
		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 6_000, 1));
	}

	@Test
	public void clearingTheWildRestartsTheMemoryOfTheRolls()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);
		long later = 10 * WildTracker.MEMORY_MS;

		tracker.clearWild(later);

		assertSame(entry, tracker.restore(1, ID, later + WildTracker.MEMORY_MS - 1));
	}

	@Test
	public void clearingEverythingForgetsTheRollsToo()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		tracker.offer(1, ID, GOBLIN, 13, false, 0, 1);

		tracker.clear();

		assertTrue(tracker.mayRoll(1, ID, 1));
		assertNull(tracker.restore(1, ID, 1));
	}

	@Test
	public void aBlockedRestoreCanBeRetriedWhenARoomOpens()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = tracker.offer(0, ID, GOBLIN, 13, false, 0, 1);
		tracker.leftView(0, 1);
		for (int i = 1; i <= WildTracker.MAX_WILD; i++)
			tracker.offer(i, ID, GOBLIN, 13, false, i * WildTracker.COOLDOWN_MS, 1);

		long now = 4 * WildTracker.COOLDOWN_MS;
		assertTrue(tracker.canRestore(0, ID, now));
		assertNull(tracker.restore(0, ID, now));
		assertTrue(tracker.canRestore(0, ID, now));

		tracker.remove(1, now);

		assertSame(entry, tracker.restore(0, ID, now + 1));
		assertFalse(tracker.canRestore(0, ID, now + 1));
	}

	@Test
	public void aRestoreBlockedByTheCapKeepsTheEntryFromExpiring()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = tracker.offer(0, ID, GOBLIN, 13, false, 0, 1);
		tracker.leftView(0, 1);
		for (int i = 1; i <= WildTracker.MAX_WILD; i++)
			tracker.offer(i, ID, GOBLIN, 13, false, i * WildTracker.COOLDOWN_MS, 1);

		long step = WildTracker.MEMORY_MS - 1_000;
		long now = 0;

		for (int i = 0; i < 4; i++)
		{
			now += step;
			assertNull(tracker.restore(0, ID, now));
		}

		tracker.remove(1, now);
		assertSame(entry, tracker.restore(0, ID, now + 1));
	}

	@Test
	public void aCreatureThatCannotBeRestoredIsNotRestorable()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(tracker.offer(1, ID, GOBLIN, 13, false, 0, Integer.MAX_VALUE));
		tracker.leftView(1, 1);

		assertFalse(tracker.canRestore(1, ID, 2));
		assertFalse(tracker.canRestore(9, ID, 2));
	}
}
