/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The things a daily or weekly task can ask for. Each kind has a fixed goal for a daily task and another for a
 * weekly one, or 0 where it is not in that pool, so the goals are the same for every player.
 */
enum TaskKind
{
	CATCH(3, 25),
	CATCH_STYLE(2, 0),
	CATCH_UNDERGROUND(1, 0),
	WIN_BATTLES(3, 15),
	WALK(600, 5000),
	GAIN_XP(5000, 0),
	HATCH(1, 0),
	CHAMPION(0, 1);

	private final int dailyGoal;
	private final int weeklyGoal;

	TaskKind(int dailyGoal, int weeklyGoal)
	{
		this.dailyGoal = dailyGoal;
		this.weeklyGoal = weeklyGoal;
	}

	/** @return what a daily task of this kind asks for, or 0 when the kind is not a daily task */
	int dailyGoal()
	{
		return dailyGoal;
	}

	/** @return what a weekly task of this kind asks for, or 0 when the kind is not a weekly task */
	int weeklyGoal()
	{
		return weeklyGoal;
	}

	/** @return the kinds a daily task can be, in a fixed order */
	static List<TaskKind> dailyPool()
	{
		List<TaskKind> pool = new ArrayList<>();
		for (TaskKind kind : values())
		{
			if (kind.dailyGoal > 0)
				pool.add(kind);
		}

		return Collections.unmodifiableList(pool);
	}

	/** @return the kinds a weekly task can be, in a fixed order */
	static List<TaskKind> weeklyPool()
	{
		List<TaskKind> pool = new ArrayList<>();
		for (TaskKind kind : new TaskKind[] {CHAMPION, CATCH, WIN_BATTLES, WALK})
			pool.add(kind);

		return Collections.unmodifiableList(pool);
	}
}
