/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** When an XP drop in a gathering skill may bring a wild creature. Pure maths, no game state. */
final class SkillingRules
{
	/** Shortest time between two skilling encounters, in milliseconds, whatever the roll says. */
	static final long COOLDOWN_MS = 180_000L;

	/**
	 * OSRS XP in drops, on average, per skilling encounter at the default rarity. About 3 minutes of cooldown plus
	 * this much XP at 40,000 to 70,000 XP an hour lands one creature every 6 to 8 minutes.
	 */
	static final double XP_PER_ENCOUNTER = 3_500.0;

	/** The "Wild creature rarity" setting that {@link #XP_PER_ENCOUNTER} is worked out for. */
	static final int REFERENCE_ONE_IN = 8;

	/** Highest chance a single XP drop has to bring a creature. */
	static final double MAX_CHANCE = 0.5;

	private SkillingRules()
	{
	}

	/**
	 * Tells whether a skill brings creatures.
	 *
	 * @param skill the skill's name, as the game spells it in capitals
	 * @return true for Fishing, Woodcutting, Mining, Farming and Hunter
	 */
	static boolean gathering(String skill)
	{
		return "FISHING".equals(skill) || "WOODCUTTING".equals(skill) || "MINING".equals(skill)
			|| "FARMING".equals(skill) || "HUNTER".equals(skill);
	}

	/**
	 * Chance that one XP drop brings a creature. It grows with the size of the drop, so many small drops and a few
	 * big ones find creatures at about the same rate per XP, and with the rarity setting.
	 *
	 * @param xpGained OSRS XP in the drop
	 * @param wildChance the "Wild creature rarity" setting: a monster turns wild one time in this many
	 * @return the chance, from 0 to {@link #MAX_CHANCE}
	 */
	static double chance(int xpGained, int wildChance)
	{
		double perXp = (double) REFERENCE_ONE_IN / Math.max(1, wildChance) / XP_PER_ENCOUNTER;
		return Math.max(0.0, Math.min(MAX_CHANCE, xpGained * perXp));
	}

	/**
	 * Tells whether the cooldown since the last skilling encounter is over.
	 *
	 * @param lastMs when the last creature appeared, in milliseconds
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return true when at least {@link #COOLDOWN_MS} have passed
	 */
	static boolean ready(long lastMs, long nowMs)
	{
		return nowMs - lastMs >= COOLDOWN_MS;
	}
}
