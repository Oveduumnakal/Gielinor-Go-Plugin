/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What an automatic wild battle with the whole team shows when a creature stepped in: the lines that float and the
 * beats that are acted out, one to one. Each duel, the stretch between two switches, is cut down to a few blows
 * ({@link TeamScript#cut}) so a long battle does not flood the screen; a creature that falls is named, the next one
 * is announced and, on stage, lies down and is replaced. It holds no game client types.
 */
final class WildScript
{
	private static final Logger log = LoggerFactory.getLogger(WildScript.class);

	/** The run of blows by one creature that is worth a line in the log. */
	private static final int LONG_STREAK = 3;

	private final List<BattleLine> lines;
	private final List<BattleTimeline.Step> steps;

	private WildScript(List<BattleLine> lines, List<BattleTimeline.Step> steps)
	{
		this.lines = Collections.unmodifiableList(lines);
		this.steps = Collections.unmodifiableList(steps);
	}

	/**
	 * Plans the show of a battle.
	 *
	 * @param result the battle, played turn by turn: its first line is the opening and its last the result
	 * @return the script
	 */
	static WildScript of(BattleResult result)
	{
		List<BattleLine> all = result.lines();
		List<BattleLine> lines = new ArrayList<>();
		List<BattleTimeline.Step> steps = new ArrayList<>();
		lines.add(all.get(0));
		List<BattleLine> hits = new ArrayList<>();
		String fighting = result.opener().name();
		for (TurnResult turn : result.played())
		{
			hits.addAll(turn.hits());
			Combatant entered = turn.enteredMine();
			if (entered == null)
				continue;

			flush(hits, lines, steps);
			if (fighting != null)
			{
				lines.add(new BattleLine(fighting + " fainted", Tone.BAD, true));
				steps.add(new BattleTimeline.Step(BattleTimeline.Beat.LEAD_FELL, null));
			}

			lines.add(new BattleLine("Go, " + entered.name() + "!", Tone.INFO, true));
			steps.add(new BattleTimeline.Step(entered));
			fighting = entered.name();
		}

		flush(hits, lines, steps);
		lines.add(all.get(all.size() - 1));
		logOneSidedShow(all, lines);
		return new WildScript(lines, steps);
	}

	private static void logOneSidedShow(List<BattleLine> all, List<BattleLine> shown)
	{
		if (TeamScript.sameSideStreak(shown) < LONG_STREAK || TeamScript.sameSideStreak(all) >= all.size())
			return;

		boolean lead = false;
		boolean foe = false;
		for (BattleLine line : all)
		{
			lead |= line.kind() == BattleLine.Kind.LEAD_HIT;
			foe |= line.kind() == BattleLine.Kind.FOE_HIT;
		}

		if (lead && foe)
			log.warn("Gielinor Go: the shown battle has {} attacks by one side in a row: {}",
				TeamScript.sameSideStreak(shown), shown);
	}

	private static void flush(List<BattleLine> hits, List<BattleLine> lines, List<BattleTimeline.Step> steps)
	{
		List<BattleLine> kept = TeamScript.cut(hits);
		lines.addAll(kept);
		steps.addAll(BattleTimeline.stepsOf(kept));
		hits.clear();
	}

	/** @return the lines to float, in order, the opening first and the result last */
	List<BattleLine> lines()
	{
		return lines;
	}

	/** @return the beats to act out after the face-off, in order; the end of the battle is not included */
	List<BattleTimeline.Step> steps()
	{
		return steps;
	}
}
