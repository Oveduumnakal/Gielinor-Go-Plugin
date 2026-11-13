/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The rule for hiding wild creatures while the player is in real combat. A hidden creature is not drawn, cannot be
 * clicked and gets no menu entries; it keeps its tile and its stay timer, which goes on running, so it returns as it
 * was, or not at all when its time ran out. A creature in a Gielinor Go battle is never hidden. Holds no RuneLite
 * types.
 */
final class WildHiding
{
	private WildHiding()
	{
	}

	/**
	 * Tells whether a creature is hidden right now.
	 *
	 * @param inCombat whether the player is in real combat, with the linger of {@link CombatWatch}
	 * @param state what the creature is doing
	 * @param foeOfBattle whether it is the foe of a Gielinor Go battle in progress
	 * @return true when it is not to be drawn or clicked
	 */
	static boolean hides(boolean inCombat, WildLife.State state, boolean foeOfBattle)
	{
		return inCombat && !foeOfBattle && state != WildLife.State.IN_BATTLE;
	}

	/**
	 * @param inCombat whether the player is in real combat
	 * @return whether a new wild creature may appear, from a monster or from skilling
	 */
	static boolean mayAppear(boolean inCombat)
	{
		return !inCombat;
	}
}
