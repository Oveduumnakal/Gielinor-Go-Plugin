/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The chance a jar catches one wild creature right now: the in-battle rule while a battle with it is on, the plain
 * rule otherwise. The menus and their tests share it. Holds no RuneLite types.
 */
final class CatchOdds
{
	/** The health share that stands for "no battle is on". */
	static final double NOT_IN_BATTLE = -1;

	private CatchOdds()
	{
	}

	/**
	 * Works out the chance of one jar on one creature.
	 *
	 * @param jar the jar thrown
	 * @param entry the wild creature
	 * @param knockedOut whether it lies knocked out after losing a battle
	 * @param healthLeft the share of full health it has left in the battle the player steers, or
	 *        {@link #NOT_IN_BATTLE} (any negative number) when no battle with it is on
	 * @return the chance, from {@link CatchRules#MIN_CHANCE} to {@link CatchRules#MAX_CHANCE}
	 */
	static double of(JarType jar, WildEntry entry, boolean knockedOut, double healthLeft)
	{
		if (healthLeft >= 0)
			return CatchRules.inBattle(jar, entry.combatLevel(), entry.traits(), healthLeft);

		return CatchRules.chance(jar, entry.combatLevel(), entry.weakened(), entry.traits(), knockedOut);
	}
}
