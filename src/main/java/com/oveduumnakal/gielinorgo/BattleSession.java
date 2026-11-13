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
	private FriendTurnBattle friend;
	private int eliteNumber;
	private List<BattleLine> verdict = Collections.emptyList();

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

	/**
	 * Marks the battle as one played turn by turn against a friend, whose choices arrive through the given battle.
	 * Such a battle pays nothing, writes nothing back to the save but the items used, and has no catch.
	 *
	 * @param battle the battle shared with the friend
	 * @return this session
	 */
	BattleSession friend(FriendTurnBattle battle)
	{
		this.friend = battle;
		return this;
	}

	/** @return the battle shared with a friend, or null when the battle is not one against a friend */
	FriendTurnBattle friendTurn()
	{
		return friend;
	}

	/**
	 * Marks the battle as one of the open Elite run, against the Elite team held as the champion.
	 *
	 * @param number which battle of the run this is, counted from 1
	 * @return this session
	 */
	BattleSession elite(int number)
	{
		this.eliteNumber = number;
		return this;
	}

	/** @return whether the battle is a battle of the Elite challenge */
	boolean isElite()
	{
		return eliteNumber > 0;
	}

	/** @return which battle of the Elite run this is, counted from 1, or 0 for any other battle */
	int eliteNumber()
	{
		return eliteNumber;
	}

	/**
	 * Keeps the lines that close an Elite battle, written when its result is settled.
	 *
	 * @param lines the lines
	 */
	void verdict(List<BattleLine> lines)
	{
		this.verdict = Collections.unmodifiableList(new ArrayList<>(lines));
	}

	/** @return the lines that close an Elite battle once it is settled, none before */
	List<BattleLine> verdict()
	{
		return verdict;
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
