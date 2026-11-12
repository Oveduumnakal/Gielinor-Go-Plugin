/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * How far the player is through today's and this week's tasks, and the login streak. Saved to the profile as JSON,
 * so every field here is part of the persisted schema. The tasks themselves are not saved: they come from the day
 * and the week, so only the progress is.
 */
final class TaskProgress
{
	/** The UTC day the daily progress belongs to, or -1 before the first rollover. */
	long day = -1;

	/** Progress on each of the day's three tasks. */
	int[] daily = new int[TaskCalendar.DAILY_COUNT];

	/** Whether each of the day's tasks has paid its reward. */
	boolean[] dailyPaid = new boolean[TaskCalendar.DAILY_COUNT];

	/** The week the weekly progress belongs to, or -1 before the first rollover. */
	long week = -1;

	/** Progress on the week's task. */
	int weekly;

	/** Whether the week's task has paid its reward. */
	boolean weeklyPaid;

	/** Days in a row with a catch, up to and including {@link #lastCatchDay}. */
	int streak;

	/** The UTC day of the last catch, or -1 for none. */
	long lastCatchDay = -1;

	/**
	 * Repairs a damaged or older save: arrays of the wrong length or missing, and negative numbers.
	 *
	 * @return this progress
	 */
	TaskProgress sanitized()
	{
		daily = fit(daily);
		dailyPaid = dailyPaid == null ? new boolean[TaskCalendar.DAILY_COUNT] : fit(dailyPaid);
		weekly = Math.max(0, weekly);
		streak = Math.max(0, streak);
		return this;
	}

	private static int[] fit(int[] values)
	{
		int[] fitted = new int[TaskCalendar.DAILY_COUNT];
		if (values == null)
			return fitted;

		for (int i = 0; i < fitted.length && i < values.length; i++)
			fitted[i] = Math.max(0, values[i]);

		return fitted;
	}

	private static boolean[] fit(boolean[] values)
	{
		boolean[] fitted = new boolean[TaskCalendar.DAILY_COUNT];
		for (int i = 0; i < fitted.length && i < values.length; i++)
			fitted[i] = values[i];

		return fitted;
	}
}
