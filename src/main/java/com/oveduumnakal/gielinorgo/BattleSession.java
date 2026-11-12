/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A battle the player steers, from the first turn to the last: the engine, the saved creatures behind the player's
 * fighters, and what the battle is against, a wild creature or a City Champion. {@link GoGame} makes one with
 * {@code startBattle} or {@code startChallenge} and plays it with {@code act}; nothing is paid before the battle is
 * over. It holds no game client types.
 */
final class BattleSession
{
	private final BattleState state;
	private final List<Creature> fighters;
	private final double[] startHealth;
	private final WildEntry entry;
	private final Champion champion;
	private final BattleLine opening;

	/**
	 * Builds a session.
	 *
	 * @param state the engine, with the player's fighters in the same order as {@code fighters}
	 * @param fighters the saved creatures the player's fighters stand for
	 * @param entry the wild creature fought, or null for a champion battle
	 * @param champion the champion fought, or null for a wild battle
	 * @param opening the line that starts the battle
	 */
	BattleSession(BattleState state, List<Creature> fighters, WildEntry entry, Champion champion, BattleLine opening)
	{
		this.state = state;
		this.fighters = Collections.unmodifiableList(new ArrayList<>(fighters));
		this.entry = entry;
		this.champion = champion;
		this.opening = opening;
		this.startHealth = new double[fighters.size()];
		for (int i = 0; i < startHealth.length; i++)
			startHealth[i] = fighter(i).hpFraction();
	}

	/** @return the engine */
	BattleState state()
	{
		return state;
	}

	/** @return whether the battle is against a wild creature, false for a City Champion */
	boolean wild()
	{
		return entry != null;
	}

	/** @return the wild creature fought, or null in a champion battle */
	WildEntry entry()
	{
		return entry;
	}

	/** @return the champion fought, or null in a wild battle */
	Champion champion()
	{
		return champion;
	}

	/** @return the line that starts the battle */
	BattleLine opening()
	{
		return opening;
	}

	/** @return whether the battle is over */
	boolean over()
	{
		return state.ended() != null;
	}

	/**
	 * Finds one of the player's fighters.
	 *
	 * @param place its place in the team
	 * @return the fighter
	 */
	Combatant fighter(int place)
	{
		List<Combatant> team = state.team();
		return team.get(place);
	}

	/** @return the saved creatures behind the player's fighters, in the engine's team order */
	List<Creature> fighters()
	{
		return fighters;
	}

	/**
	 * Tells whether a fighter has lost health since the last time it was written to its saved creature.
	 *
	 * @param place the fighter's place in the team
	 * @return true when its health differs from what was written last
	 */
	boolean changed(int place)
	{
		return fighter(place).hpFraction() != startHealth[place];
	}

	/**
	 * Notes that a fighter's health was written to its saved creature.
	 *
	 * @param place the fighter's place in the team
	 */
	void written(int place)
	{
		startHealth[place] = fighter(place).hpFraction();
	}
}
