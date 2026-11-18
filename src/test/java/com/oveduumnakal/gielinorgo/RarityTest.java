/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks how a tier changes how often a monster turns wild (issue 213) and what level its eggs hatch at (220). */
public class RarityTest
{
	@Test
	public void wildFactorByTier()
	{
		assertEquals(1.0, Rarity.COMMON.wildFactor(), 0.0);
		assertEquals(0.6, Rarity.UNCOMMON.wildFactor(), 0.0);
		assertEquals(0.35, Rarity.RARE.wildFactor(), 0.0);
		assertEquals(0.15, Rarity.EPIC.wildFactor(), 0.0);
		assertEquals(0.05, Rarity.LEGENDARY.wildFactor(), 0.0);
	}

	@Test
	public void wildFactorNeverRisesWithTierAndIsPositive()
	{
		Rarity[] tiers = Rarity.values();
		for (int i = 0; i < tiers.length; i++)
		{
			assertTrue(tiers[i].name(), tiers[i].wildFactor() > 0.0);
			if (i > 0)
				assertTrue(tiers[i].name(), tiers[i].wildFactor() <= tiers[i - 1].wildFactor());
		}
	}

	@Test
	public void hatchLevelByTier()
	{
		assertEquals(1, Rarity.COMMON.hatchLevel());
		assertEquals(5, Rarity.UNCOMMON.hatchLevel());
		assertEquals(10, Rarity.RARE.hatchLevel());
		assertEquals(20, Rarity.EPIC.hatchLevel());
		assertEquals(30, Rarity.LEGENDARY.hatchLevel());
	}

	@Test
	public void hatchLevelNeverFallsWithTierAndStaysInsideTheLevelRange()
	{
		Rarity[] tiers = Rarity.values();
		for (int i = 0; i < tiers.length; i++)
		{
			assertTrue(tiers[i].name(), tiers[i].hatchLevel() >= 1 && tiers[i].hatchLevel() <= 99);
			if (i > 0)
				assertTrue(tiers[i].name(), tiers[i].hatchLevel() > tiers[i - 1].hatchLevel());
		}
	}

	@Test
	public void aRareSpeciesTurnsWildAboutAThirdAsOftenAsACommonOne()
	{
		Species common = new Species("Test common", 20, CombatType.STAB);
		Species rare = new Species("Test rare", 100, CombatType.STAB);
		SpawnCalendar calendar = new SpawnCalendar(new SpeciesTable(Arrays.asList(common, rare)));

		int commons = wildCount(common, calendar.monsterChance(common, 4, false, null));
		int rares = wildCount(rare, calendar.monsterChance(rare, 4, false, null));

		double ratio = (double) rares / commons;
		assertTrue("rare " + rares + " common " + commons + " ratio " + ratio, ratio > 0.30 && ratio < 0.40);
	}

	private static int wildCount(Species species, double chance)
	{
		WildTracker tracker = new WildTracker(new Random(11));
		int turned = 0;
		for (int i = 0; i < 20_000; i++)
		{
			long now = i * (WildTracker.COOLDOWN_MS + 1);
			WildEntry entry = tracker.offer(i, 100 + i, species, species.level(), false, now, chance, 1, true);
			if (entry == null)
				continue;

			turned++;
			tracker.withdraw(entry);
		}

		return turned;
	}
}
