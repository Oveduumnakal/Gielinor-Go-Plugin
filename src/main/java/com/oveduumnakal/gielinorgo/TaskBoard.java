/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Today's tasks and this week's, with the player's progress on them and the days played. It has no RuneLite types:
 * the game calls {@link #rollover(long)} and then an event method, and each event method returns the tasks that
 * finished because of it. A finished task is marked paid here, so it is returned once; paying it is up to the caller.
 */
final class TaskBoard
{
	/** Days played between two eggs: every day this many pays an egg. */
	static final int STREAK_EGG_EVERY = 7;

	/** What the first catch of a day did to the days-played count. */
	static final class Streak
	{
		private final int days;

		Streak(int days)
		{
			this.days = days;
		}

		/** @return the days played after this catch, at least 1 */
		int days()
		{
			return days;
		}

		/** @return whether this day earns an egg */
		boolean eggDue()
		{
			return days % STREAK_EGG_EVERY == 0;
		}
	}

	private final TaskCalendar calendar;
	private final TaskProgress progress;
	private List<Task> daily = Collections.emptyList();
	private Task weekly;
	private long cachedDay = Long.MIN_VALUE;

	/**
	 * Creates a board over some progress.
	 *
	 * @param calendar picks the tasks
	 * @param progress the progress to read and change, which must already be sanitized
	 */
	TaskBoard(TaskCalendar calendar, TaskProgress progress)
	{
		this.calendar = calendar;
		this.progress = progress;
	}

	/**
	 * Checks whether this board works on some progress.
	 *
	 * @param other the progress
	 * @return true when it is the very same object
	 */
	boolean tracks(TaskProgress other)
	{
		return progress == other;
	}

	/**
	 * Starts a new day or week when the clock has moved into one. The check is two comparisons when nothing
	 * changed, so it can run on every event and every snapshot.
	 *
	 * @param now wall-clock milliseconds
	 * @return true when the progress changed and needs saving
	 */
	boolean rollover(long now)
	{
		long day = calendar.dayOf(now);
		long week = calendar.weekOfDay(day);
		boolean changed = false;
		if (progress.day != day)
		{
			progress.day = day;
			Arrays.fill(progress.daily, 0);
			Arrays.fill(progress.dailyPaid, false);
			changed = true;
		}

		if (progress.week != week)
		{
			progress.week = week;
			progress.weekly = 0;
			progress.weeklyPaid = false;
			changed = true;
		}

		if (daily.isEmpty() || weekly == null || cachedDay != day)
		{
			daily = calendar.dailyFor(day);
			weekly = calendar.weeklyFor(week);
			cachedDay = day;
		}

		return changed;
	}

	/** @return today's three tasks, as of the last {@link #rollover(long)} */
	List<Task> daily()
	{
		return daily;
	}

	/** @return this week's task, as of the last {@link #rollover(long)} */
	Task weekly()
	{
		return weekly;
	}

	/**
	 * Notes a catch for the days played. Only the first catch of a UTC day counts.
	 *
	 * @return what the days played became, or null when today already had a catch
	 */
	Streak recordCatch()
	{
		if (progress.lastCatchDay == progress.day)
			return null;

		progress.streak++;
		progress.lastCatchDay = progress.day;
		return new Streak(progress.streak);
	}

	/**
	 * Counts a catch.
	 *
	 * @param style the caught species' combat style
	 * @param underground whether it was met underground
	 * @return the tasks this finished
	 */
	List<Task> caught(CombatType style, boolean underground)
	{
		List<Task> done = new ArrayList<>();
		advance(TaskKind.CATCH, null, 1, done);
		advance(TaskKind.CATCH_STYLE, style, 1, done);
		if (underground)
			advance(TaskKind.CATCH_UNDERGROUND, null, 1, done);

		return done;
	}

	/** @return the tasks a battle win finished */
	List<Task> battleWon()
	{
		return advanceOne(TaskKind.WIN_BATTLES, 1);
	}

	/**
	 * Counts tiles walked.
	 *
	 * @param tiles tiles walked
	 * @return the tasks this finished
	 */
	List<Task> walked(int tiles)
	{
		return advanceOne(TaskKind.WALK, tiles);
	}

	/**
	 * Counts XP gained in the real game.
	 *
	 * @param amount XP gained, in any skill
	 * @return the tasks this finished
	 */
	List<Task> gainedXp(int amount)
	{
		return advanceOne(TaskKind.GAIN_XP, amount);
	}

	/** @return the tasks a hatched egg finished */
	List<Task> hatched()
	{
		return advanceOne(TaskKind.HATCH, 1);
	}

	/** @return the tasks a beaten City Champion finished */
	List<Task> championBeaten()
	{
		return advanceOne(TaskKind.CHAMPION, 1);
	}

	/**
	 * Builds what the panel shows.
	 *
	 * @param now wall-clock milliseconds
	 * @param best the jar tier jar rewards are paid in
	 * @return the view, with the tasks as of {@code now}
	 */
	TaskView view(long now, JarType best)
	{
		rollover(now);
		List<TaskView.Row> rows = new ArrayList<>();
		for (int i = 0; i < daily.size(); i++)
			rows.add(row(daily.get(i), progress.daily[i], progress.dailyPaid[i], best));

		rows.add(row(weekly, progress.weekly, progress.weeklyPaid, best));
		int alive = progress.streak;
		return new TaskView(rows, alive, STREAK_EGG_EVERY - alive % STREAK_EGG_EVERY, calendar.millisToReset(now),
			calendar.millisToWeeklyReset(now));
	}

	private static TaskView.Row row(Task task, int done, boolean paid, JarType best)
	{
		return new TaskView.Row(task.label(), Math.min(done, task.goal()), task.goal(), paid,
			String.join(" + ", task.rewardParts(best)), task.weekly());
	}

	private List<Task> advanceOne(TaskKind kind, int amount)
	{
		List<Task> done = new ArrayList<>();
		advance(kind, null, amount, done);
		return done;
	}

	/**
	 * Adds progress to every task of one kind, today's and the week's, and collects the ones that just finished.
	 *
	 * @param kind the kind of event
	 * @param style the combat style of the event, or null when it has none
	 * @param amount how much to add
	 * @param done receives each task that just reached its goal
	 */
	private void advance(TaskKind kind, CombatType style, int amount, List<Task> done)
	{
		if (amount <= 0)
			return;

		for (int i = 0; i < daily.size(); i++)
		{
			Task task = daily.get(i);
			if (!matches(task, kind, style) || progress.dailyPaid[i])
				continue;

			progress.daily[i] = add(progress.daily[i], amount, task.goal());
			if (progress.daily[i] >= task.goal())
			{
				progress.dailyPaid[i] = true;
				done.add(task);
			}
		}

		if (weekly == null || !matches(weekly, kind, style) || progress.weeklyPaid)
			return;

		progress.weekly = add(progress.weekly, amount, weekly.goal());
		if (progress.weekly >= weekly.goal())
		{
			progress.weeklyPaid = true;
			done.add(weekly);
		}
	}

	private static boolean matches(Task task, TaskKind kind, CombatType style)
	{
		return task.kind() == kind && (task.style() == null || task.style() == style);
	}

	private static int add(int have, int amount, int goal)
	{
		return (int) Math.min((long) goal, (long) have + amount);
	}
}
