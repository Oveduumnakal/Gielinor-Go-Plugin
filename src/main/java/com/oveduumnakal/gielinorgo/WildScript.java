/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What an automatic wild battle with the whole team shows when a creature stepped in: the lines that float and the
 * beats that are acted out, one to one. Each duel, the stretch between two switches, is cut down to a few blows
 * ({@link TeamScript#cut}) so a long battle does not flood the screen; a creature that falls is named, the next one
 * is announced and, on stage, lies down and is replaced. It holds no game client types.
 */
final class WildScript
{
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
		return new WildScript(lines, steps);
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
