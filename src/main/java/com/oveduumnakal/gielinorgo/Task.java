/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One daily or weekly task: what to do, how much, and what it pays when done. Immutable. */
final class Task
{
	private final TaskKind kind;
	private final int goal;
	private final CombatType style;
	private final boolean weekly;
	private final int jars;
	private final long trainerXp;
	private final Rarity egg;

	/**
	 * Creates a task.
	 *
	 * @param kind what it asks for
	 * @param goal how many times, tiles or XP
	 * @param style the combat style a {@link TaskKind#CATCH_STYLE} task asks for, otherwise null
	 * @param weekly whether it is the weekly task
	 * @param jars jars of the best unlocked tier it pays
	 * @param trainerXp trainer XP it pays
	 * @param egg the rarity of the egg it pays, or null for none
	 */
	Task(TaskKind kind, int goal, CombatType style, boolean weekly, int jars, long trainerXp, Rarity egg)
	{
		this.kind = kind;
		this.goal = goal;
		this.style = style;
		this.weekly = weekly;
		this.jars = jars;
		this.trainerXp = trainerXp;
		this.egg = egg;
	}

	/** @return what it asks for */
	TaskKind kind()
	{
		return kind;
	}

	/** @return how many times, tiles or XP it asks for */
	int goal()
	{
		return goal;
	}

	/** @return the combat style of a {@link TaskKind#CATCH_STYLE} task, otherwise null */
	CombatType style()
	{
		return style;
	}

	/** @return whether this is the weekly task */
	boolean weekly()
	{
		return weekly;
	}

	/** @return jars of the best unlocked tier it pays, possibly 0 */
	int jars()
	{
		return jars;
	}

	/** @return trainer XP it pays, possibly 0 */
	long trainerXp()
	{
		return trainerXp;
	}

	/** @return the rarity of the egg it pays, or null for none */
	Rarity egg()
	{
		return egg;
	}

	/** @return the words the player reads, such as {@code Catch 3 Ranged creatures} */
	String label()
	{
		switch (kind)
		{
			case CATCH:
				return goal == 1 ? "Catch a creature" : "Catch " + number(goal) + " creatures";
			case CATCH_STYLE:
				return "Catch " + number(goal) + " " + style.label() + " creatures";
			case CATCH_UNDERGROUND:
				return "Catch a creature underground";
			case WIN_BATTLES:
				return goal == 1 ? "Win a battle" : "Win " + number(goal) + " battles";
			case WALK:
				return "Walk " + number(goal) + " tiles";
			case GAIN_XP:
				return "Gain " + number(goal) + " XP in any skill";
			case HATCH:
				return "Hatch an egg";
			default:
				return "Beat a City Champion";
		}
	}

	/**
	 * Words the reward.
	 *
	 * @param best the jar tier the jars come in
	 * @return the reward parts such as {@code 2 Iron jars}, {@code 100 trainer XP} or {@code Rare egg}
	 */
	List<String> rewardParts(JarType best)
	{
		List<String> parts = new ArrayList<>();
		if (jars > 0)
			parts.add(jars + " " + best.label() + (jars == 1 ? " jar" : " jars"));

		if (trainerXp > 0)
			parts.add(trainerXp + " trainer XP");

		if (egg != null)
			parts.add(egg.label() + " egg");

		return parts;
	}

	/**
	 * Writes a number with thousands separators.
	 *
	 * @param value the number
	 * @return the text, such as {@code 5,000}
	 */
	private static String number(int value)
	{
		return String.format(Locale.ROOT, "%,d", value);
	}
}
