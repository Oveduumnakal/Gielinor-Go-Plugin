/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** What the Tasks section of the Team tab shows. Immutable, so it can cross from the game thread to the UI. */
final class TaskView
{
	/** A view with nothing to show. */
	static final TaskView NONE = new TaskView(Collections.emptyList(), 0, 7, 0L, 0L);

	/** One task row. */
	static final class Row
	{
		private final String label;
		private final int progress;
		private final int goal;
		private final boolean done;
		private final String reward;
		private final boolean weekly;

		Row(String label, int progress, int goal, boolean done, String reward, boolean weekly)
		{
			this.label = label;
			this.progress = progress;
			this.goal = goal;
			this.done = done;
			this.reward = reward;
			this.weekly = weekly;
		}

		/** @return the words the player reads, such as {@code Catch 3 Ranged creatures} */
		String label()
		{
			return label;
		}

		/** @return how far along the task is, never past {@link #goal()} */
		int progress()
		{
			return progress;
		}

		/** @return what the task asks for */
		int goal()
		{
			return goal;
		}

		/** @return whether the task is finished and has paid */
		boolean done()
		{
			return done;
		}

		/** @return the reward in words, such as {@code 2 Iron jars} */
		String reward()
		{
			return reward;
		}

		/** @return whether this is the weekly task */
		boolean weekly()
		{
			return weekly;
		}

		/** @return how far along the task is, from 0 to 1 */
		double fraction()
		{
			return goal <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, progress / (double) goal));
		}

		/** @return the progress as text such as {@code 2 / 3}, with thousands shortened to {@code 4.3k} */
		String count()
		{
			return compact(progress) + " / " + compact(goal);
		}

		private static String compact(int value)
		{
			if (value < 1000)
				return Integer.toString(value);

			if (value % 1000 == 0)
				return value / 1000 + "k";

			return String.format(Locale.ROOT, "%.1fk", Math.floor(value / 100.0) / 10.0);
		}
	}

	private final List<Row> rows;
	private final int streak;
	private final int eggIn;
	private final long millisToReset;
	private final long millisToWeeklyReset;

	/**
	 * Creates a view.
	 *
	 * @param rows the three daily rows, then the weekly row
	 * @param streak days in a row with a catch, 0 when the streak is broken
	 * @param eggIn catches on separate days still needed for the next streak egg, from 1 to 7
	 * @param millisToReset milliseconds until the daily tasks reset
	 * @param millisToWeeklyReset milliseconds until the weekly task resets
	 */
	TaskView(List<Row> rows, int streak, int eggIn, long millisToReset, long millisToWeeklyReset)
	{
		this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
		this.streak = streak;
		this.eggIn = eggIn;
		this.millisToReset = millisToReset;
		this.millisToWeeklyReset = millisToWeeklyReset;
	}

	/** @return the three daily rows, then the weekly row; empty when there is nothing to show */
	List<Row> rows()
	{
		return rows;
	}

	/** @return days in a row with a catch, 0 when the streak is broken */
	int streak()
	{
		return streak;
	}

	/** @return days of catching still needed for the next streak egg, from 1 to 7 */
	int eggIn()
	{
		return eggIn;
	}

	/** @return milliseconds until the daily tasks reset */
	long millisToReset()
	{
		return millisToReset;
	}

	/** @return milliseconds until the weekly task resets */
	long millisToWeeklyReset()
	{
		return millisToWeeklyReset;
	}

	/**
	 * Writes a time left in the shortest useful words.
	 *
	 * @param millis the time left
	 * @return {@code 5h 12m}, {@code 42m}, or {@code 3d 4h} from two days up
	 */
	static String duration(long millis)
	{
		long minutes = Math.max(0L, millis) / 60_000L;
		long hours = minutes / 60L;
		if (hours >= 24L)
			return hours / 24L + "d " + hours % 24L + "h";

		if (hours > 0L)
			return hours + "h " + minutes % 60L + "m";

		return Math.max(1L, minutes) + "m";
	}
}
