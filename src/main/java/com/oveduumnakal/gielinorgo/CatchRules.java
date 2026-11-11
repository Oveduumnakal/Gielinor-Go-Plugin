/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

/** Catch chances and wild creature levels. Pure maths, no game state. */
final class CatchRules
{
	/** Lowest catch chance, however strong the monster. */
	static final double MIN_CHANCE = 0.05;

	/** Highest catch chance, however weak the monster. */
	static final double MAX_CHANCE = 0.95;

	/** Multiplier on the catch chance for a wild creature that just lost a battle. */
	static final double WEAKENED_BONUS = 1.5;

	/** Chance that a creature runs away after breaking out of a jar. */
	static final double FLEE_CHANCE = 0.3;

	/** OSRS XP in one drop that would make a specialty jar certain, before {@link #MAX_SPECIALTY_CHANCE}. */
	static final double XP_PER_SPECIALTY_JAR = 20_000.0;

	/** Highest chance a single XP drop has to turn up a specialty jar. */
	static final double MAX_SPECIALTY_CHANCE = 0.5;

	private static final double CHANCE_PIVOT = 60.0;

	private CatchRules()
	{
	}

	/**
	 * Chance that one throw catches a monster with no traits.
	 *
	 * @param jar the jar thrown
	 * @param combatLevel the monster's combat level
	 * @param weakened whether the monster just lost a battle to the player
	 * @return the chance, from {@link #MIN_CHANCE} to {@link #MAX_CHANCE}
	 */
	static double chance(JarType jar, int combatLevel, boolean weakened)
	{
		return chance(jar, combatLevel, weakened, Collections.emptySet());
	}

	/**
	 * Chance that one throw catches a monster.
	 *
	 * @param jar the jar thrown
	 * @param combatLevel the monster's combat level
	 * @param weakened whether the monster just lost a battle to the player
	 * @param traits the monster's traits, which a specialty jar may be good at
	 * @return the chance, from {@link #MIN_CHANCE} to {@link #MAX_CHANCE}
	 */
	static double chance(JarType jar, int combatLevel, boolean weakened, Set<Trait> traits)
	{
		double base = jar.bonusAgainst(traits) * CHANCE_PIVOT / (CHANCE_PIVOT + Math.max(0, combatLevel));
		if (weakened)
			base *= WEAKENED_BONUS;

		return Math.max(MIN_CHANCE, Math.min(MAX_CHANCE, base));
	}

	/**
	 * Level a wild creature appears at. Stronger monsters appear at higher levels, with a little spread.
	 *
	 * @param combatLevel the monster's combat level
	 * @param random the source of the spread
	 * @return the creature level, 1 to 99
	 */
	static int wildLevel(int combatLevel, Random random)
	{
		int base = (int) Math.round(Math.sqrt(Math.max(1, combatLevel)) * 3);
		int spread = random.nextInt(5) - 2;
		return Math.max(1, Math.min(XpRules.MAX_LEVEL, base + spread));
	}

	/**
	 * Chance that one XP drop turns up the skill's specialty jar. It grows with the size of the drop, so slow
	 * big-XP actions and fast small-XP actions find jars at about the same rate per XP.
	 *
	 * @param xpGained OSRS XP in the drop
	 * @return the chance, from 0 to {@link #MAX_SPECIALTY_CHANCE}
	 */
	static double specialtyDropChance(int xpGained)
	{
		return Math.max(0.0, Math.min(MAX_SPECIALTY_CHANCE, xpGained / XP_PER_SPECIALTY_JAR));
	}

	/**
	 * Trainer XP for a successful catch.
	 *
	 * @param wildLevel the level of the creature caught
	 * @return the trainer XP earned
	 */
	static long catchXp(int wildLevel)
	{
		return 20L + wildLevel;
	}
}
