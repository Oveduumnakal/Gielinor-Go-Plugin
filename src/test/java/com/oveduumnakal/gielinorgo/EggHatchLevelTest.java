/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks that an egg hatches at the level of its tier, and that the hatched creature then behaves as usual (#220). */
public class EggHatchLevelTest
{
	private static final Species COMMON = new Species("Test common", 10, CombatType.STAB);
	private static final Species UNCOMMON = new Species("Test uncommon", 50, CombatType.SLASH);
	private static final Species GROWN = new Species("Test grown", 120, CombatType.SLASH);
	private static final Species RARE = new Species("Test rare", 100, CombatType.CRUSH);
	private static final Species EPIC = new Species("Test epic", 200, CombatType.RANGED);
	private static final Species LEGENDARY = new Species("Test legendary", 500, CombatType.MAGIC);

	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(COMMON, UNCOMMON, GROWN, RARE, EPIC, LEGENDARY));
		EvolutionTable evolutions = new EvolutionTable(Collections.singletonList(
			new EvolutionTable.Step("Test uncommon", "Test grown", 8)), table);
		game = new GoGame(table, evolutions, new ChampionTable(Collections.emptyList(), table), new Random(5),
			() -> now, notice ->
			{
			});
	}

	private Creature hatch(Rarity tier)
	{
		game.data().eggs.clear();
		game.data().eggs.add(new Egg(tier));
		game.addSteps(tier.eggTiles());
		assertTrue("the egg hatched", game.data().eggs.isEmpty());
		List<Creature> team = game.data().team;
		return team.get(team.size() - 1);
	}

	@Test
	public void everyTierHatchesAtItsOwnLevel()
	{
		Map<Rarity, Integer> expected = new HashMap<>();
		expected.put(Rarity.COMMON, 1);
		expected.put(Rarity.UNCOMMON, 5);
		expected.put(Rarity.RARE, 10);
		expected.put(Rarity.EPIC, 20);
		expected.put(Rarity.LEGENDARY, 30);

		for (Rarity tier : Rarity.values())
		{
			Creature born = hatch(tier);

			assertEquals(tier.name(), (int) expected.get(tier), born.level());
			assertEquals(tier.name(), XpRules.xpForLevel(expected.get(tier)), born.xp);
		}
	}

	@Test
	public void aHatchedCreatureIsAtFullHealthAndKnowsTheMovesItsLevelUnlocked()
	{
		Creature born = hatch(Rarity.LEGENDARY);

		assertEquals(1.0, born.hpFractionAt(now), 0.0);
		assertFalse(born.fainted);
		assertEquals(now, born.caughtAtMs);
		assertEquals(1, (int) game.data().caught.get("test legendary"));
	}

	@Test
	public void aHatchedCreatureDoesNotEvolveUntilItReachesItsEvolutionLevel()
	{
		Creature born = hatch(Rarity.UNCOMMON);
		assertEquals("test uncommon", born.species);
		assertEquals(5, born.level());

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(7) - XpRules.xpForLevel(5)));
		assertEquals("test uncommon", born.species);
		assertEquals(7, born.level());

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(8) - XpRules.xpForLevel(7)));
		assertEquals("test grown", born.species);
		assertEquals(8, born.level());
	}

	@Test
	public void aHatchedCreatureLevelsFromWhereItStarted()
	{
		Creature born = hatch(Rarity.RARE);
		assertEquals(10, born.level());

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(11) - XpRules.xpForLevel(10)));

		assertEquals(11, born.level());
	}

	@Test
	public void aBossEggHatchesIntoItsBossAtTheLevelOfItsTier()
	{
		Egg egg = new Egg(EPIC);
		game.data().eggs.add(egg);

		game.addSteps(Rarity.EPIC.eggTiles());

		Creature born = game.data().team.get(0);
		assertEquals("test epic", born.species);
		assertEquals(20, born.level());
	}

	@Test
	public void aHatchedCreatureStaysAtItsLevelWhenItRests()
	{
		Creature born = hatch(Rarity.EPIC);
		born.hpFraction = 0.2;
		born.hpStampMs = now;
		now += Creature.FULL_REST_MS;

		assertEquals(1.0, born.hpFractionAt(now), 0.0);
		assertEquals(20, born.level());
	}
}
