/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * How a team battle went: who won, one line per creature knocked out, and the duels the battle was made of, which
 * let it be acted out on screen.
 */
final class TeamBattleResult
{
	private final boolean won;
	private final List<BattleLine> lines;
	private final List<Duel> duels;
	private final boolean leadFirst;

	TeamBattleResult(boolean won, List<BattleLine> lines)
	{
		this(won, lines, Collections.emptyList(), false);
	}

	/**
	 * Builds a result that can be acted out.
	 *
	 * @param won whether the player's team won
	 * @param lines the lines of the battle, in order
	 * @param duels the duels, in order
	 */
	TeamBattleResult(boolean won, List<BattleLine> lines, List<Duel> duels)
	{
		this(won, lines, duels, false);
	}

	private TeamBattleResult(boolean won, List<BattleLine> lines, List<Duel> duels, boolean leadFirst)
	{
		this.won = won;
		this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
		this.duels = Collections.unmodifiableList(new ArrayList<>(duels));
		this.leadFirst = leadFirst;
	}

	/**
	 * Says whether the first creature to fight is the lead that walks beside the player.
	 *
	 * @param first true when it is, so it does not fade in when the battle is acted out
	 * @return a copy of this result with that noted
	 */
	TeamBattleResult withLeadFirst(boolean first)
	{
		return new TeamBattleResult(won, lines, duels, first);
	}

	/** @return the duels of the battle, in order; empty when the battle cannot be acted out */
	List<Duel> duels()
	{
		return duels;
	}

	/** @return whether the player's first creature to fight is the lead beside the player */
	boolean leadFirst()
	{
		return leadFirst;
	}

	/**
	 * @return the lines before the first knock-out line, which float over the player as the scene starts; all of
	 *     the lines when no duel was recorded
	 */
	List<BattleLine> leadIn()
	{
		List<BattleLine> before = new ArrayList<>();
		for (BattleLine line : lines)
		{
			if (knocksOut(line))
				break;

			before.add(line);
		}

		return before;
	}

	/**
	 * @return the lines after the first knock-out line that are not knock-out lines themselves, such as the verdict,
	 *     which float over the player once the scene is over
	 */
	List<BattleLine> leadOut()
	{
		List<BattleLine> after = new ArrayList<>();
		boolean seen = false;
		for (BattleLine line : lines)
		{
			if (knocksOut(line))
				seen = true;
			else if (seen)
				after.add(line);
		}

		return after;
	}

	private boolean knocksOut(BattleLine line)
	{
		for (Duel duel : duels)
		{
			if (duel.knockOut() == line)
				return true;
		}

		return false;
	}

	/** @return whether the player's team beat the whole opposing team */
	boolean won()
	{
		return won;
	}

	/** @return the lines of the battle, in order */
	List<BattleLine> lines()
	{
		return lines;
	}
}
