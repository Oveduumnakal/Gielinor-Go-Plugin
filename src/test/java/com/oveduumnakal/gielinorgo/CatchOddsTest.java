/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks that the menu's odds are the catch rules' odds, in a battle and out of one. */
public class CatchOddsTest
{
	private static final double EPS = 1e-9;
	private static final Species GOBLIN = new Species("Goblin", 2, CombatType.SLASH);

	private static WildEntry entry(int combatLevel)
	{
		return new WildEntry(1, GOBLIN, 5, combatLevel);
	}

	@Test
	public void outOfBattleItIsThePlainChance()
	{
		WildEntry foe = entry(80);

		for (JarType jar : JarType.values())
		{
			assertEquals(CatchRules.chance(jar, 80, foe.weakened(), foe.traits(), false),
				CatchOdds.of(jar, foe, false, CatchOdds.NOT_IN_BATTLE), EPS);
			assertEquals(CatchRules.chance(jar, 80, foe.weakened(), foe.traits(), true),
				CatchOdds.of(jar, foe, true, CatchOdds.NOT_IN_BATTLE), EPS);
		}
	}

	@Test
	public void aKnockedOutCreatureIsEasierThanAStandingOne()
	{
		WildEntry foe = entry(80);

		assertEquals(true, CatchOdds.of(JarType.BRONZE, foe, true, -1) > CatchOdds.of(JarType.BRONZE, foe, false, -1));
	}

	@Test
	public void aMatchingSpecialtyJarBeatsBronzeOutOfBattle()
	{
		WildEntry foe = entry(80);

		assertEquals(CatchRules.chance(JarType.SLASH, 80, foe.weakened(), foe.traits(), false),
			CatchOdds.of(JarType.SLASH, foe, false, -1), EPS);
		assertEquals(true, CatchOdds.of(JarType.SLASH, foe, false, -1) > CatchOdds.of(JarType.BRONZE, foe, false, -1));
	}

	@Test
	public void inBattleItIsTheInBattleChanceAtEveryHealth()
	{
		WildEntry foe = entry(80);

		for (double health : new double[] {1.0, 0.5, 0.0})
		{
			for (JarType jar : JarType.values())
				assertEquals(CatchRules.inBattle(jar, 80, foe.traits(), health),
					CatchOdds.of(jar, foe, false, health), EPS);
		}
	}

	@Test
	public void zeroHealthLeftStillMeansABattleIsOn()
	{
		WildEntry foe = entry(80);

		assertEquals(CatchRules.inBattle(JarType.IRON, 80, foe.traits(), 0.0),
			CatchOdds.of(JarType.IRON, foe, true, 0.0), EPS);
	}

	@Test
	public void anyNegativeHealthMeansNoBattle()
	{
		WildEntry foe = entry(80);

		assertEquals(CatchOdds.of(JarType.IRON, foe, false, CatchOdds.NOT_IN_BATTLE),
			CatchOdds.of(JarType.IRON, foe, false, -0.25), EPS);
	}
}
