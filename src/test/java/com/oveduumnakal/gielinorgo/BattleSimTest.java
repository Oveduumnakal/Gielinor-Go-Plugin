/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks that battles are repeatable, always finish, and respect the type triangle. */
public class BattleSimTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final Species WIZARD = new Species("Wizard", 13, CombatType.MAGIC);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);

	@Test
	public void sameSeedGivesTheSameBattle()
	{
		BattleResult first = fight(GOBLIN, 10, ARCHER, 10, 42);
		BattleResult second = fight(GOBLIN, 10, ARCHER, 10, 42);

		assertEquals(first.log(), second.log());
		assertEquals(first.won(), second.won());
		assertEquals(first.hpFractionLeft(), second.hpFractionLeft(), 0);
	}

	@Test
	public void everyBattleEndsWithinTheTurnLimit()
	{
		Species[] all = {GOBLIN, ARCHER, WIZARD, DRAGON};
		for (int seed = 0; seed < 200; seed++)
		{
			Random pick = new Random(seed);
			Species mine = all[pick.nextInt(all.length)];
			Species foe = all[pick.nextInt(all.length)];
			BattleResult result = fight(mine, 1 + pick.nextInt(99), foe, 1 + pick.nextInt(99), seed);

			assertTrue(result.turns() <= BattleSim.MAX_TURNS);
			assertTrue(result.hpFractionLeft() >= 0 && result.hpFractionLeft() <= 1);
			assertEquals(result.turns() + 2, result.log().size());
		}
	}

	@Test
	public void typeAdvantageDecidesAnEvenMatch()
	{
		int meleeWins = 0;
		int rangedWins = 0;
		for (int seed = 0; seed < 200; seed++)
		{
			if (fight(GOBLIN, 20, ARCHER, 20, seed).won())
				meleeWins++;

			if (fight(ARCHER, 20, GOBLIN, 20, seed).won())
				rangedWins++;
		}

		assertTrue("melee should beat ranged most of the time, won " + meleeWins, meleeWins > 150);
		assertTrue("ranged should lose to melee most of the time, won " + rangedWins, rangedWins < 50);
	}

	@Test
	public void attackMultiplierShowsInTheDamage()
	{
		Combatant melee = new Combatant(GOBLIN, 50, 1.0);
		Combatant ranged = new Combatant(ARCHER, 50, 1.0);
		Combatant magic = new Combatant(WIZARD, 50, 1.0);

		int strong = BattleSim.damage(melee, ranged, new Random(3));
		int weak = BattleSim.damage(melee, magic, new Random(3));

		assertTrue(strong > weak);
	}

	@Test
	public void aStrongerSpeciesWinsAtTheSameLevel()
	{
		for (int seed = 0; seed < 50; seed++)
			assertTrue(fight(DRAGON, 30, GOBLIN, 30, seed).won());
	}

	@Test
	public void aTiredCreatureStartsWithLessHealth()
	{
		Combatant rested = new Combatant(GOBLIN, 10, 1.0);
		Combatant tired = new Combatant(GOBLIN, 10, 0.5);

		assertEquals(rested.maxHp(), tired.maxHp());
		assertEquals(rested.hp() / 2, tired.hp(), 0.001);
		assertFalse(tired.fainted());
	}

	@Test
	public void summaryKeepsTheOpeningAndTheResult()
	{
		BattleResult result = fight(GOBLIN, 10, GOBLIN, 10, 5);
		assertTrue("the test needs a long battle", result.log().size() > 6);

		List<BattleLine> full = result.lines();
		List<BattleLine> summary = result.summary(6);
		assertEquals(6, summary.size());
		assertEquals(full.subList(0, 3), summary.subList(0, 3));
		assertEquals(full.subList(full.size() - 3, full.size()), summary.subList(3, 6));
		assertEquals(full, result.summary(1000));
	}

	@Test
	public void hitsFloatOverWhoeverWasHit()
	{
		BattleResult result = fight(GOBLIN, 10, GOBLIN, 10, 5);
		List<BattleLine> lines = result.lines();

		assertFalse("the opening is about the wild creature", lines.get(0).overPlayer());
		assertFalse("the result is about the wild creature", lines.get(lines.size() - 1).overPlayer());
		int overPlayer = 0;
		for (BattleLine line : lines.subList(1, lines.size() - 1))
		{
			assertEquals(line.overPlayer() ? Tone.BAD : Tone.GOOD, line.tone());
			assertEquals(line.overPlayer(), line.text().startsWith("Goblin -"));
			if (line.overPlayer())
				overPlayer++;
		}

		assertEquals("hits alternate between the two sides", result.turns() / 2.0, overPlayer, 0.5);
	}

	private static BattleResult fight(Species mine, int myLevel, Species foe, int foeLevel, long seed)
	{
		return BattleSim.fight(new Combatant(mine, myLevel, 1.0), new Combatant(foe, foeLevel, 1.0),
			new Random(seed));
	}
}
