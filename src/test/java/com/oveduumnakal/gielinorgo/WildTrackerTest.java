/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks when monsters may turn wild: the odds, the cap, the cooldown and the memory of past rolls. */
public class WildTrackerTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);

	private static final int ID = 3029;

	private static final double ALWAYS = 1.0;

	private static final double NEVER = 0.0;

	private static WildEntry offer(WildTracker tracker, int index, int id, long nowMs, double chance)
	{
		return tracker.offer(index, id, GOBLIN, 13, false, nowMs, chance, WildTracker.GILDED_ONE_IN, false);
	}

	@Test
	public void aChanceOfOneAlwaysTurnsWild()
	{
		WildTracker tracker = new WildTracker(new Random(1));

		WildEntry entry = offer(tracker, 5, ID, 0, ALWAYS);

		assertNotNull(entry);
		assertEquals(entry, tracker.get(entry.wildId()));
	}

	@Test
	public void aWildCreatureHasAnIdOfItsOwnNotTheMonstersIndex()
	{
		WildTracker tracker = new WildTracker(new Random(1));

		WildEntry first = offer(tracker, 4_000, ID, 0, ALWAYS);
		WildEntry second = offer(tracker, 4_001, ID, WildTracker.COOLDOWN_MS, ALWAYS);

		assertNotEquals(first.wildId(), second.wildId());
		assertNull(tracker.get(4_000));
	}

	@Test
	public void cooldownBlocksASecondCreature()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(offer(tracker, 1, ID, 0, ALWAYS));

		assertNull(offer(tracker, 2, ID, WildTracker.COOLDOWN_MS - 1, ALWAYS));
		assertNotNull(offer(tracker, 2, ID, WildTracker.COOLDOWN_MS, ALWAYS));
	}

	@Test
	public void neverMoreThanTheCap()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		for (int i = 0; i < 10; i++)
			offer(tracker, i, ID, i * WildTracker.COOLDOWN_MS, ALWAYS);

		assertEquals(WildTracker.MAX_WILD, tracker.all().size());
	}

	@Test
	public void theSameMonsterIsNotOfferedTwice()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(offer(tracker, 1, ID, 0, ALWAYS));

		assertNull(offer(tracker, 1, ID, 10 * WildTracker.COOLDOWN_MS, ALWAYS));
	}

	@Test
	public void removingFreesASlot()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = offer(tracker, 1, ID, 0, ALWAYS);

		tracker.remove(entry.wildId());

		assertNull(tracker.get(entry.wildId()));
		assertTrue(tracker.all().isEmpty());
	}

	@Test
	public void oddsAreRoughlyOneInN()
	{
		Random shared = new Random(99);
		int wild = 0;
		for (int i = 0; i < 4000; i++)
		{
			if (offer(new WildTracker(shared), 1, ID, 0, 1.0 / 8) != null)
				wild++;
		}

		assertTrue("expected about 500 of 4000, got " + wild, wild > 400 && wild < 600);
	}

	@Test
	public void monstersWithoutASpeciesNeverTurnWild()
	{
		assertNull(new WildTracker(new Random(1)).offer(1, ID, null, 13, false, 0, ALWAYS,
			WildTracker.GILDED_ONE_IN, false));
	}

	@Test
	public void aWildCreatureStaysWhenItsMonsterLeavesView()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = offer(tracker, 1, ID, 0, ALWAYS);
		entry.markBattled(true);

		tracker.leftView(1, 1_000);

		assertSame(entry, tracker.get(entry.wildId()));
		assertTrue(entry.battled());
		assertEquals(1, tracker.all().size());
	}

	@Test
	public void aMonsterThatRolledNoDoesNotRollAgainOnReload()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(offer(tracker, 1, ID, 0, NEVER));
		tracker.leftView(1, 1_000);

		assertFalse(tracker.mayRoll(1, ID, 2_000));
		assertNull(offer(tracker, 1, ID, 2_000, ALWAYS));
	}

	@Test
	public void aMonsterThatRolledWildDoesNotRollAgainOnReload()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = offer(tracker, 1, ID, 0, ALWAYS);
		tracker.remove(entry.wildId());
		tracker.leftView(1, 2_000);

		assertFalse(tracker.mayRoll(1, ID, 3_000));
		assertNull(offer(tracker, 1, ID, 3_000 + WildTracker.COOLDOWN_MS, ALWAYS));
	}

	@Test
	public void aRollIsForgottenAfterTheMemoryRunsOut()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(offer(tracker, 1, ID, 0, NEVER));
		tracker.leftView(1, 1_000);

		long later = 1_000 + WildTracker.MEMORY_MS;
		assertTrue(tracker.mayRoll(1, ID, later));
		assertNotNull(offer(tracker, 1, ID, later, ALWAYS));
	}

	@Test
	public void aWildRollInViewKeepsItsMemoryPastTheLimit()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		offer(tracker, 1, ID, 0, ALWAYS);

		assertFalse(tracker.mayRoll(1, ID, 10 * WildTracker.MEMORY_MS));
	}

	@Test
	public void aWildRollIsForgottenWhenItsMonsterHasBeenGoneLongEnough()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		offer(tracker, 1, ID, 0, ALWAYS);
		tracker.leftView(1, 1_000);

		assertTrue(tracker.mayRoll(1, ID, 1_000 + WildTracker.MEMORY_MS));
	}

	@Test
	public void aDifferentMonsterOnAReusedIndexRollsFresh()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(offer(tracker, 1, ID, 0, NEVER));
		tracker.leftView(1, 1_000);

		assertTrue(tracker.mayRoll(1, ID + 1, 2_000));
	}

	@Test
	public void aCooldownBlockIsNotARoll()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		offer(tracker, 1, ID, 0, ALWAYS);

		assertNull(offer(tracker, 2, ID, 1, ALWAYS));
		assertTrue(tracker.mayRoll(2, ID, 2));
	}

	@Test
	public void aWithdrawnCreatureUndoesTheCooldownButNotTheMemory()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = offer(tracker, 1, ID, 0, ALWAYS);

		tracker.withdraw(entry);

		assertTrue(tracker.all().isEmpty());
		assertFalse(tracker.mayRoll(1, ID, 1));
		assertNotNull(offer(tracker, 2, ID, 1, ALWAYS));
	}

	@Test
	public void clearingTheWildDropsTheCreaturesButKeepsTheRolls()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry entry = offer(tracker, 1, ID, 0, ALWAYS);

		tracker.clearWild(5_000);

		assertTrue(tracker.all().isEmpty());
		assertNull(tracker.get(entry.wildId()));
		assertFalse(tracker.mayRoll(1, ID, 6_000));
	}

	@Test
	public void clearingTheWildKeepsANoRollAsNo()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNull(offer(tracker, 1, ID, 0, NEVER));

		tracker.clearWild(5_000);

		assertFalse(tracker.mayRoll(1, ID, 6_000));
		assertNull(offer(tracker, 1, ID, 6_000, ALWAYS));
	}

	@Test
	public void clearingTheWildRestartsTheMemoryOfTheRolls()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		offer(tracker, 1, ID, 0, ALWAYS);
		long later = 10 * WildTracker.MEMORY_MS;

		tracker.clearWild(later);

		assertFalse(tracker.mayRoll(1, ID, later + WildTracker.MEMORY_MS - 1));
		assertTrue(tracker.mayRoll(1, ID, later + WildTracker.MEMORY_MS));
	}

	@Test
	public void clearingEverythingForgetsTheRollsToo()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		offer(tracker, 1, ID, 0, ALWAYS);

		tracker.clear();

		assertTrue(tracker.all().isEmpty());
		assertTrue(tracker.mayRoll(1, ID, 1));
	}

	@Test
	public void aChanceOfZeroNeverTurnsWild()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		for (int i = 0; i < 200; i++)
			assertNull(offer(tracker, i, ID, i * WildTracker.COOLDOWN_MS, NEVER));

		assertTrue(tracker.all().isEmpty());
	}

	@Test
	public void theGildedOddsParameterIsUsed()
	{
		int always = 0;
		int ordinary = 0;
		for (int i = 0; i < 400; i++)
		{
			WildTracker tracker = new WildTracker(new Random(i));
			if (tracker.offer(1, ID, GOBLIN, 13, false, 0, ALWAYS, 1, false).gilded())
				always++;

			if (tracker.offer(2, ID, GOBLIN, 13, false, WildTracker.COOLDOWN_MS, ALWAYS, 1_000_000, false).gilded())
				ordinary++;
		}

		assertEquals(400, always);
		assertEquals(0, ordinary);
	}

	@Test
	public void roamingAppearsAboutOneTimeInTwenty()
	{
		int roaming = 0;
		Random shared = new Random(2024);
		for (int i = 0; i < 10_000; i++)
		{
			WildTracker tracker = new WildTracker(shared);
			WildEntry entry = tracker.offer(1, ID, GOBLIN, 13, false, 0, ALWAYS, WildTracker.GILDED_ONE_IN, true);
			assertNotNull(entry);
			if (entry.roaming())
				roaming++;
		}

		assertTrue("expected about 500 of 10000, got " + roaming, roaming > 420 && roaming < 580);
	}

	@Test
	public void aRoamingCreatureIsTenLevelsAboveTheSameRollWithoutRoaming()
	{
		for (int seed = 0; seed < 2000; seed++)
		{
			WildEntry roamer = new WildTracker(new Random(seed))
				.offer(1, ID, GOBLIN, 13, false, 0, ALWAYS, WildTracker.GILDED_ONE_IN, true);
			if (!roamer.roaming())
				continue;

			Random replay = new Random(seed);
			replay.nextDouble();
			int plain = CatchRules.wildLevel(13, replay);
			assertEquals(Math.min(99, plain + WildTracker.ROAMING_LEVELS), roamer.level());
			return;
		}

		throw new AssertionError("no roaming creature in 2000 seeds");
	}

	@Test
	public void roamingIsCappedAtLevelNinetyNine()
	{
		Species boss = new Species("Boss", 1000, CombatType.MELEE);
		int capped = 0;
		for (int seed = 0; seed < 2000; seed++)
		{
			WildEntry entry = new WildTracker(new Random(seed))
				.offer(1, ID, boss, 1000, false, 0, ALWAYS, WildTracker.GILDED_ONE_IN, true);
			assertTrue(entry.level() <= 99);
			if (entry.roaming() && entry.level() == 99)
				capped++;
		}

		assertTrue(capped > 0);
	}

	@Test
	public void nothingRoamsWhenRoamingIsOff()
	{
		for (int seed = 0; seed < 2000; seed++)
		{
			WildEntry entry = new WildTracker(new Random(seed))
				.offer(1, ID, GOBLIN, 13, false, 0, ALWAYS, WildTracker.GILDED_ONE_IN, false);
			assertFalse(entry.roaming());
		}
	}

	private static Species beast()
	{
		return new Gson().fromJson("{\"name\":\"Crab\",\"level\":23,\"ids\":[4819]}", Species.class);
	}

	@Test
	public void aLooseCreatureCountsTowardTheCapAndLeavesNoRoll()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		for (int i = 0; i < 5; i++)
			tracker.offerLoose(beast(), false, WildTracker.GILDED_ONE_IN, false);

		assertEquals(WildTracker.MAX_WILD, tracker.all().size());
		assertNull(offer(tracker, 1, ID, 0, ALWAYS));
		assertTrue(tracker.mayRoll(7, ID, 0));
	}

	@Test
	public void aLooseCreatureHasItsOwnIdAndTheSpeciesFirstNpcId()
	{
		WildTracker tracker = new WildTracker(new Random(1));

		WildEntry entry = tracker.offerLoose(beast(), true, WildTracker.GILDED_ONE_IN, false);

		assertSame(entry, tracker.get(entry.wildId()));
		assertEquals(4819, entry.npcId());
		assertEquals(23, entry.combatLevel());
		assertTrue(entry.traits().contains(Trait.CAVE));
	}

	@Test
	public void aLooseCreatureDoesNotUseOrStartTheMonsterCooldown()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(offer(tracker, 1, ID, 0, ALWAYS));

		assertNotNull(tracker.offerLoose(beast(), false, WildTracker.GILDED_ONE_IN, false));
		assertNotNull(offer(tracker, 2, ID, WildTracker.COOLDOWN_MS, ALWAYS));
	}

	@Test
	public void aLooseCreatureExpiresAndWithdrawingItKeepsTheCooldown()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		assertNotNull(offer(tracker, 1, ID, 0, ALWAYS));
		WildEntry loose = tracker.offerLoose(beast(), false, WildTracker.GILDED_ONE_IN, false);

		tracker.withdrawLoose(loose);

		assertNull(tracker.get(loose.wildId()));
		assertNull(offer(tracker, 2, ID, WildTracker.COOLDOWN_MS - 1, ALWAYS));
	}

	@Test
	public void aLooseCreatureIsEndedLikeAnyOtherAndNotRerolledByAMonster()
	{
		WildTracker tracker = new WildTracker(new Random(1));
		WildEntry loose = tracker.offerLoose(beast(), false, WildTracker.GILDED_ONE_IN, false);

		tracker.leftView(loose.wildId(), 0);
		assertNotNull(tracker.get(loose.wildId()));

		tracker.remove(loose.wildId());
		assertNull(tracker.get(loose.wildId()));
	}

	@Test
	public void aSpeciesWithNoNpcIdOrAQuestOneIsRefused()
	{
		WildTracker tracker = new WildTracker(new Random(1));

		assertNull(tracker.offerLoose(GOBLIN, false, WildTracker.GILDED_ONE_IN, false));
		assertNull(tracker.offerLoose(beast().asQuestOnly(), false, WildTracker.GILDED_ONE_IN, false));
	}
}
