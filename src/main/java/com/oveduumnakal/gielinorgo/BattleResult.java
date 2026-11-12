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

	BattleResult(boolean won, List<BattleLine> lines, double hpFractionLeft, int turns)
	{
		this.won = won;
		this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
		this.hpFractionLeft = hpFractionLeft;
		this.turns = turns;
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
	 * The battle cut down for playback: its first lines and its last lines, with the middle left out.
	 *
	 * @param maxLines the most lines to return, at least 2
	 * @return every line when they fit, otherwise the start and the end
	 */
	List<BattleLine> summary(int maxLines)
	{
		return cut(lines, maxLines);
	}

	/**
	 * The battle cut down for showing to the player. The opening line is kept first so a scene can still open with
	 * its face-off, but it is not counted: it is never shown as text, so it must not take a slot a real line needs.
	 *
	 * @param maxLines the most lines to return that are shown as text, at least 2
	 * @return every line when they fit, otherwise the start and the end of the shown lines, after the opening
	 */
	List<BattleLine> playbackSummary(int maxLines)
	{
		if (lines.isEmpty() || lines.get(0).kind() != BattleLine.Kind.OPENING)
			return summary(maxLines);

		List<BattleLine> kept = new ArrayList<>();
		kept.add(lines.get(0));
		kept.addAll(cut(lines.subList(1, lines.size()), maxLines));
		return kept;
	}

	private static List<BattleLine> cut(List<BattleLine> lines, int maxLines)
	{
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
