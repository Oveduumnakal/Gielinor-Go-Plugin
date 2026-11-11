/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the pure rules: the type triangle, catch chances, level curves and XP sharing. */
public class RulesTest
{
	private static final double EPSILON = 0.005;

	@Test
	public void typeTriangleRunsMeleeRangedMagic()
	{
		assertEquals(CombatType.STRONG, CombatType.MELEE.multiplierAgainst(CombatType.RANGED), 0);
		assertEquals(CombatType.STRONG, CombatType.RANGED.multiplierAgainst(CombatType.MAGIC), 0);
		assertEquals(CombatType.STRONG, CombatType.MAGIC.multiplierAgainst(CombatType.MELEE), 0);
		assertEquals(CombatType.WEAK, CombatType.RANGED.multiplierAgainst(CombatType.MELEE), 0);
		assertEquals(CombatType.WEAK, CombatType.MAGIC.multiplierAgainst(CombatType.RANGED), 0);
		assertEquals(CombatType.WEAK, CombatType.MELEE.multiplierAgainst(CombatType.MAGIC), 0);
		for (CombatType type : CombatType.values())
			assertEquals(1.0, type.multiplierAgainst(type), 0);
	}

	@Test
	public void catchChanceMatchesThePlanTable()
	{
		assertEquals(0.95, CatchRules.chance(JarType.BRONZE, 2, false), EPSILON);
		assertEquals(0.375, CatchRules.chance(JarType.BRONZE, 100, false), EPSILON);
		assertEquals(0.079, CatchRules.chance(JarType.BRONZE, 700, false), EPSILON);
		assertEquals(0.5625, CatchRules.chance(JarType.IRON, 100, false), EPSILON);
		assertEquals(0.118, CatchRules.chance(JarType.IRON, 700, false), EPSILON);
		assertEquals(0.75, CatchRules.chance(JarType.RUNE, 100, false), EPSILON);
		assertEquals(0.158, CatchRules.chance(JarType.RUNE, 700, false), EPSILON);
		assertEquals(0.95, CatchRules.chance(JarType.DRAGON, 100, false), EPSILON);
		assertEquals(0.237, CatchRules.chance(JarType.DRAGON, 700, false), EPSILON);
	}

	@Test
	public void catchChanceStaysInsideItsLimits()
	{
		assertEquals(CatchRules.MIN_CHANCE, CatchRules.chance(JarType.BRONZE, 5000, false), 0);
		assertEquals(CatchRules.MAX_CHANCE, CatchRules.chance(JarType.DRAGON, 1, true), 0);
	}

	@Test
	public void weakenedCreaturesAreEasierToCatch()
	{
		double normal = CatchRules.chance(JarType.BRONZE, 100, false);
		assertEquals(normal * CatchRules.WEAKENED_BONUS, CatchRules.chance(JarType.BRONZE, 100, true), EPSILON);
	}

	@Test
	public void aSpecialtyJarOnlyHelpsOnItsTrait()
	{
		Set<Trait> fast = EnumSet.of(Trait.MELEE, Trait.FAST);
		Set<Trait> plain = EnumSet.of(Trait.MELEE);

		double bronze = CatchRules.chance(JarType.BRONZE, 200, false, plain);
		assertEquals(bronze, CatchRules.chance(JarType.SWIFT, 200, false, plain), 0);
		assertEquals(bronze * JarType.SWIFT.traitBonus(), CatchRules.chance(JarType.SWIFT, 200, false, fast), EPSILON);
		assertEquals(bronze * JarType.DRAGON.bonus(), CatchRules.chance(JarType.DRAGON, 200, false, fast), EPSILON);
	}

	@Test
	public void everySpecialtyJarHasATraitAndASkillAndTierJarsHaveNeither()
	{
		Set<String> skills = new HashSet<>();
		for (JarType type : JarType.values())
		{
			assertEquals(type.specialty(), type.trait() != null);
			assertEquals(type.specialty(), !type.skills().isEmpty());
			assertEquals(type.specialty(), type.trainerLevel() == 0);
			for (String skill : type.skills())
			{
				assertTrue(skill + " finds two different jars", skills.add(skill));
				assertEquals(type, JarType.forSkill(skill));
			}
		}

		assertEquals(JarType.CAVE, JarType.forSkill("mining"));
		assertEquals(JarType.SWIFT, JarType.forSkill("AGILITY"));
		assertNull(JarType.forSkill("HITPOINTS"));
		assertNull(JarType.forSkill("OVERALL"));
		assertNull(JarType.forSkill(null));
	}

	@Test
	public void specialtyDropChanceGrowsWithTheDropAndIsCapped()
	{
		assertEquals(0.0, CatchRules.specialtyDropChance(0), 0);
		assertEquals(0.0025, CatchRules.specialtyDropChance(50), 0.00001);
		assertEquals(CatchRules.MAX_SPECIALTY_CHANCE, CatchRules.specialtyDropChance(5_000_000), 0);
	}

	@Test
	public void undergroundIsAboveTheSurfaceMapExceptPrifddinas()
	{
		assertFalse("Lumbridge", Terrain.underground(3222, 3218));
		assertFalse("deep Wilderness", Terrain.underground(3100, 3950));
		assertTrue("Lumbridge Swamp Caves", Terrain.underground(3168, 9572));
		assertTrue("Stronghold of Security", Terrain.underground(1860, 5244));
		assertFalse("Prifddinas", Terrain.underground(3264, 6064));
	}

	@Test
	public void speciesTraitsIncludeTheCombatStyle()
	{
		Species spectre = new Species("Aberrant spectre", 96, CombatType.MAGIC, Trait.UNHOLY, Trait.SLAYER);

		assertEquals(EnumSet.of(Trait.MAGIC, Trait.UNHOLY, Trait.SLAYER), spectre.traits());
		assertEquals(EnumSet.of(Trait.MELEE), new Species("Goblin", 13, null).traits());
	}

	@Test
	public void aCreatureMetUndergroundGainsTheCaveTrait()
	{
		Species goblin = new Species("Goblin", 13, CombatType.MELEE);

		assertFalse(new WildEntry(1, goblin, 4, 13, false).traits().contains(Trait.CAVE));
		assertTrue(new WildEntry(1, goblin, 4, 13, true).traits().contains(Trait.CAVE));
	}

	@Test
	public void wildLevelRisesWithCombatLevelAndStaysInRange()
	{
		Random random = new Random(7);
		for (int combat = 1; combat <= 2000; combat += 13)
		{
			int level = CatchRules.wildLevel(combat, random);
			assertTrue(level >= 1 && level <= XpRules.MAX_LEVEL);
		}

		assertTrue(CatchRules.wildLevel(700, new Random(1)) > CatchRules.wildLevel(2, new Random(1)));
	}

	@Test
	public void levelCurveRoundTrips()
	{
		for (int level = 1; level <= XpRules.MAX_LEVEL; level++)
		{
			assertEquals(level, XpRules.levelForXp(XpRules.xpForLevel(level)));
			assertEquals(level, XpRules.trainerLevelForXp(XpRules.trainerXpForLevel(level)));
		}

		assertEquals(1, XpRules.levelForXp(0));
		assertEquals(1, XpRules.levelForXp(-5));
		assertEquals(XpRules.MAX_LEVEL, XpRules.levelForXp(Long.MAX_VALUE / 2));
		assertEquals(4, XpRules.levelForXp(XpRules.xpForLevel(5) - 1));
	}

	@Test
	public void progressRunsFromZeroToOne()
	{
		assertEquals(0.0, XpRules.progress(XpRules.xpForLevel(10)), 0);
		assertEquals(1.0, XpRules.progress(XpRules.xpForLevel(XpRules.MAX_LEVEL)), 0);
		long halfway = (XpRules.xpForLevel(10) + XpRules.xpForLevel(11)) / 2;
		assertEquals(0.5, XpRules.progress(halfway), 0.01);
	}

	@Test
	public void leadGetsADoubleShareAndTheLeftover()
	{
		List<Creature> team = new ArrayList<>();
		for (int i = 0; i < 3; i++)
			team.add(new Creature("goblin", 1, 0));

		XpRules.share(team, 103);

		assertEquals(53, team.get(0).xp);
		assertEquals(25, team.get(1).xp);
		assertEquals(25, team.get(2).xp);
	}

	@Test
	public void aLoneCreatureGetsEverything()
	{
		List<Creature> team = new ArrayList<>();
		team.add(new Creature("goblin", 1, 0));

		XpRules.share(team, 77);

		assertEquals(77, team.get(0).xp);
	}

	@Test
	public void xpStopsAtTheLevelCap()
	{
		List<Creature> team = new ArrayList<>();
		team.add(new Creature("goblin", XpRules.MAX_LEVEL, 0));

		XpRules.share(team, 1_000_000);

		assertEquals(XpRules.xpForLevel(XpRules.MAX_LEVEL), team.get(0).xp);
	}

	@Test
	public void rarityFollowsCombatLevelBands()
	{
		assertEquals(Rarity.COMMON, Rarity.forLevel(1));
		assertEquals(Rarity.COMMON, Rarity.forLevel(30));
		assertEquals(Rarity.UNCOMMON, Rarity.forLevel(31));
		assertEquals(Rarity.RARE, Rarity.forLevel(81));
		assertEquals(Rarity.EPIC, Rarity.forLevel(151));
		assertEquals(Rarity.EPIC, Rarity.forLevel(300));
		assertEquals(Rarity.LEGENDARY, Rarity.forLevel(301));
	}

	@Test
	public void stepsCountWalkingButNotTeleports()
	{
		StepCounter counter = new StepCounter();
		assertEquals(0, counter.advance(3200, 3200, 0));
		assertEquals(1, counter.advance(3201, 3200, 0));
		assertEquals(2, counter.advance(3203, 3202, 0));
		assertEquals(0, counter.advance(3203, 3202, 0));
		assertEquals(0, counter.advance(2900, 3500, 0));
		assertEquals(0, counter.advance(2900, 3500, 1));
		counter.reset();
		assertEquals(0, counter.advance(2901, 3500, 1));
	}
}
