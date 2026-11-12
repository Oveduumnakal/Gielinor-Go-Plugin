/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which tasks a day and a week hold, and where the day and week edges fall. Everything comes from {@link DateHash}
 * and fixed numbers, so every player gets the same tasks on the same UTC day.
 */
final class TaskCalendar
{
	/** Daily tasks every day has. */
	static final int DAILY_COUNT = 3;

	/** Jars a daily task pays when it pays jars. */
	static final int DAILY_JARS = 2;

	/** Trainer XP a daily task pays when it pays XP. */
	static final long DAILY_TRAINER_XP = 100L;

	/** Jars the weekly task pays, besides its egg. */
	static final int WEEKLY_JARS = 5;

	/** The rarity of the egg the weekly task pays. */
	static final Rarity WEEKLY_EGG = Rarity.RARE;

	private static final long DAY_MILLIS = 86_400_000L;
	private static final int PICK_SALT = 100;
	private static final int STYLE_SALT = 110;
	private static final int REWARD_SALT = 120;
	private static final int WEEK_PICK_SALT = 200;

	/**
	 * Days from the Unix epoch, a Thursday, back to the Monday before it. Adding this to an epoch day and dividing
	 * by 7 numbers the weeks that start on Monday.
	 */
	private static final long MONDAY_OFFSET = 3L;

	/**
	 * Finds the UTC day of a moment.
	 *
	 * @param millis wall-clock milliseconds since the epoch
	 * @return days since 1970-01-01 UTC
	 */
	long dayOf(long millis)
	{
		return Math.floorDiv(millis, DAY_MILLIS);
	}

	/**
	 * Finds the week of a moment. Weeks start on Monday at 00:00 UTC.
	 *
	 * @param millis wall-clock milliseconds since the epoch
	 * @return a number that changes every Monday
	 */
	long weekOf(long millis)
	{
		return weekOfDay(dayOf(millis));
	}

	/**
	 * Finds the week of a day.
	 *
	 * @param utcDay days since 1970-01-01 UTC
	 * @return a number that changes every Monday
	 */
	long weekOfDay(long utcDay)
	{
		return Math.floorDiv(utcDay + MONDAY_OFFSET, 7L);
	}

	/**
	 * Tells how long the daily tasks have left.
	 *
	 * @param millis wall-clock milliseconds since the epoch
	 * @return milliseconds until the next 00:00 UTC, from 1 up to a whole day
	 */
	long millisToReset(long millis)
	{
		return DAY_MILLIS - Math.floorMod(millis, DAY_MILLIS);
	}

	/**
	 * Tells how long the weekly task has left.
	 *
	 * @param millis wall-clock milliseconds since the epoch
	 * @return milliseconds until the next Monday 00:00 UTC
	 */
	long millisToWeeklyReset(long millis)
	{
		long nextWeekStart = (weekOf(millis) + 1L) * 7L - MONDAY_OFFSET;
		return nextWeekStart * DAY_MILLIS - millis;
	}

	/**
	 * Picks the day's tasks: three different kinds.
	 *
	 * @param utcDay days since 1970-01-01 UTC
	 * @return the three tasks, in a fixed order
	 */
	List<Task> dailyFor(long utcDay)
	{
		List<TaskKind> left = new ArrayList<>(TaskKind.dailyPool());
		List<Task> tasks = new ArrayList<>();
		for (int slot = 0; slot < DAILY_COUNT; slot++)
		{
			TaskKind kind = left.remove(DateHash.pick(utcDay, PICK_SALT + slot, left.size()));
			CombatType style = null;
			if (kind == TaskKind.CATCH_STYLE)
				style = CombatType.values()[DateHash.pick(utcDay, STYLE_SALT + slot, CombatType.values().length)];

			boolean jars = DateHash.pick(utcDay, REWARD_SALT + slot, 2) == 0;
			tasks.add(new Task(kind, kind.dailyGoal(), style, false, jars ? DAILY_JARS : 0,
				jars ? 0L : DAILY_TRAINER_XP, null));
		}

		return Collections.unmodifiableList(tasks);
	}

	/**
	 * Picks the week's task.
	 *
	 * @param week the number {@link #weekOf(long)} gives
	 * @return the weekly task
	 */
	Task weeklyFor(long week)
	{
		List<TaskKind> pool = TaskKind.weeklyPool();
		TaskKind kind = pool.get(DateHash.pick(week, WEEK_PICK_SALT, pool.size()));
		return new Task(kind, kind.weeklyGoal(), null, true, WEEKLY_JARS, 0L, WEEKLY_EGG);
	}
}
