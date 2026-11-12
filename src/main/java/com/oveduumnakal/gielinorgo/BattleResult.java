/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** How a battle went: who won, the blow-by-blow lines, and the health the player's creature has left. */
final class BattleResult
{
	private final boolean won;
	private final List<BattleLine> lines;
	private final double hpFractionLeft;
	private final int turns;
	private final List<TurnResult> played;
	private final Combatant opener;
	private final boolean leadOpened;

	BattleResult(boolean won, List<BattleLine> lines, double hpFractionLeft, int turns)
	{
		this(won, lines, hpFractionLeft, turns, Collections.emptyList(), null, false);
	}

	/**
	 * Builds the result of an automatic battle that was played turn by turn.
	 *
	 * @param won whether the player's side won
	 * @param lines every line of the battle, in order
	 * @param hpFractionLeft the share of full health the creature in the fight at the end has left
	 * @param turns how many attacks were made in total
	 * @param played the turns the battle was made of, in order
	 * @param opener the creature that opened the battle
	 * @param leadOpened whether the opener is the lead, the creature the player sees beside them
	 */
	BattleResult(boolean won, List<BattleLine> lines, double hpFractionLeft, int turns, List<TurnResult> played,
		Combatant opener, boolean leadOpened)
	{
		this.opener = opener;
		this.leadOpened = leadOpened;
		this.played = Collections.unmodifiableList(new ArrayList<>(played));
		this.won = won;
		this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
		this.hpFractionLeft = hpFractionLeft;
		this.turns = turns;
	}

	/** @return the turns the battle was made of, in order; none for a battle that was resolved in one go */
	List<TurnResult> played()
	{
		return played;
	}

	/** @return the creature that opened an automatic battle, or null when it was resolved in one go */
	Combatant opener()
	{
		return opener;
	}

	/** @return whether the creature that opened the battle is the lead, so it already stands beside the player */
	boolean leadOpened()
	{
		return leadOpened;
	}

	/** @return how many times a creature of the team stepped in after another fainted */
	int switches()
	{
		int count = 0;
		for (TurnResult turn : played)
		{
			if (turn.enteredMine() != null)
				count++;
		}

		return count;
	}

	/** @return whether the player's creature won */
	boolean won()
	{
		return won;
	}

	/** @return every line of the battle, in order: the opening, one per attack, and the result */
	List<BattleLine> lines()
	{
		return lines;
	}

	/** @return the text of every line of the battle, in order */
	List<String> log()
	{
		List<String> texts = new ArrayList<>(lines.size());
		for (BattleLine line : lines)
			texts.add(line.text());

		return texts;
	}

	/** @return the share of full health the player's creature has left, from 0 to 1 */
	double hpFractionLeft()
	{
		return hpFractionLeft;
	}

	/** @return how many attacks were made in total */
	int turns()
	{
		return turns;
	}

	/**
	 * The battle cut down for playback: its first lines and its last lines, with the middle left out. A battle in
	 * which a creature stepped in is cut down duel by duel instead ({@link WildScript}), whatever the limit.
	 *
	 * @param maxLines the most lines to return, at least 2
	 * @return every line when they fit, otherwise the start and the end
	 */
	List<BattleLine> summary(int maxLines)
	{
		if (switches() > 0)
			return WildScript.of(this).lines();

		int limit = Math.max(2, maxLines);
		if (lines.size() <= limit)
			return lines;

		int tail = limit / 2;
		int head = limit - tail;
		List<BattleLine> kept = new ArrayList<>(lines.subList(0, head));
		kept.addAll(lines.subList(lines.size() - tail, lines.size()));
		return kept;
	}
}
