/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

/**
 * The odds of running from a wild creature: even, better for each level your creature that is out is above the
 * opponent and worse for each below, and better when the opponent has a status effect. Battles that are not against a
 * wild creature are always left at once, as is a battle the player walks away from. Holds no RuneLite types.
 */
final class FleeRules
{
	private static final double EVEN = 0.50;
	private static final double PER_LEVEL = 0.03;
	private static final double STATUS_BONUS = 0.20;
	private static final double LEAST = 0.10;
	private static final double MOST = 0.95;

	private FleeRules()
	{
	}

	/**
	 * Gives the chance of getting away.
	 *
	 * @param myLevel the level of your creature that is out
	 * @param foeLevel the opponent's level
	 * @param foeHasStatus whether the opponent has any status effect
	 * @return the chance, from 0.10 to 0.95
	 */
	static double chance(int myLevel, int foeLevel, boolean foeHasStatus)
	{
		double chance = EVEN + PER_LEVEL * (myLevel - foeLevel) + (foeHasStatus ? STATUS_BONUS : 0);
		return Math.max(LEAST, Math.min(MOST, chance));
	}

	/**
	 * Tries to get away.
	 *
	 * @param myLevel the level of your creature that is out
	 * @param foeLevel the opponent's level
	 * @param foeHasStatus whether the opponent has any status effect
	 * @param random the source of the roll
	 * @return whether the try succeeds
	 */
	static boolean escapes(int myLevel, int foeLevel, boolean foeHasStatus, Random random)
	{
		return random.nextDouble() < chance(myLevel, foeLevel, foeHasStatus);
	}
}
