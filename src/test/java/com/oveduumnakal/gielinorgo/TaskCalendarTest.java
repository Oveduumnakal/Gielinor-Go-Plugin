/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks which tasks a day and a week hold, and where their edges fall. */
public class TaskCalendarTest
{
	private final TaskCalendar calendar = new TaskCalendar();

	private static long millis(int year, int month, int day, int hour, int minute, int second)
	{
		LocalDateTime time = LocalDateTime.of(year, month, day, hour, minute, second);
		return time.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	@Test
	public void aDayAlwaysGivesTheSameThreeDifferentKinds()
	{
		for (long day = 20_000; day < 20_200; day++)
		{
			List<Task> first = calendar.dailyFor(day);
			List<Task> second = calendar.dailyFor(day);
			Set<TaskKind> kinds = new HashSet<>();
			for (int i = 0; i < first.size(); i++)
			{
				Task one = first.get(i);
				Task two = second.get(i);
				assertEquals(one.label(), two.label());
				assertEquals(one.kind(), two.kind());
				kinds.add(one.kind());
			}

			assertEquals(3, first.size());
			assertEquals(3, kinds.size());
			assertTrue(!kinds.contains(TaskKind.CHAMPION));
		}
	}

	@Test
	public void thirtyDaysUseEveryDailyKind()
	{
		Set<TaskKind> seen = EnumSet.noneOf(TaskKind.class);
		for (long day = 20_400; day < 20_430; day++)
		{
			for (Task task : calendar.dailyFor(day))
				seen.add(task.kind());
		}

		assertEquals(new HashSet<>(TaskKind.dailyPool()), seen);
	}

	@Test
	public void styleTasksNameAStyleAndOthersDoNot()
	{
		boolean sawStyle = false;
		for (long day = 20_000; day < 20_100; day++)
		{
			for (Task task : calendar.dailyFor(day))
			{
				if (task.kind() == TaskKind.CATCH_STYLE)
				{
					sawStyle = true;
					assertNotNull(task.style());
					String styleName = task.style().label();
					assertTrue(task.label().contains(styleName));
				}
				else
				{
					assertEquals(null, task.style());
				}
			}
		}

		assertTrue(sawStyle);
	}

	@Test
	public void dailyRewardsAreJarsOrTrainerXpAndNeverAnEgg()
	{
		for (long day = 20_000; day < 20_100; day++)
		{
			for (Task task : calendar.dailyFor(day))
			{
				assertEquals(null, task.egg());
				assertTrue(task.jars() > 0 ^ task.trainerXp() > 0);
			}
		}
	}

	@Test
	public void weeklyTasksComeFromTheWeeklyPoolAndPayJarsAndARareEgg()
	{
		Set<TaskKind> seen = EnumSet.noneOf(TaskKind.class);
		for (long week = 2900; week < 2960; week++)
		{
			Task task = calendar.weeklyFor(week);
			assertEquals(calendar.weeklyFor(week).label(), task.label());
			assertTrue(task.weekly());
			assertTrue(TaskKind.weeklyPool().contains(task.kind()));
			assertEquals(TaskCalendar.WEEKLY_JARS, task.jars());
			assertEquals(Rarity.RARE, task.egg());
			seen.add(task.kind());
		}

		assertEquals(new HashSet<>(TaskKind.weeklyPool()), seen);
	}

	@Test
	public void labelsReadTheWayThePlanWritesThem()
	{
		assertEquals("Walk 5,000 tiles", new Task(TaskKind.WALK, 5000, null, true, 5, 0L, null).label());
		assertEquals("Catch 3 Ranged creatures",
			new Task(TaskKind.CATCH_STYLE, 3, CombatType.RANGED, false, 2, 0L, null).label());
		assertEquals("Beat a City Champion", new Task(TaskKind.CHAMPION, 1, null, true, 5, 0L, null).label());
		assertEquals("Gain 5,000 XP in any skill", new Task(TaskKind.GAIN_XP, 5000, null, false, 2, 0L, null).label());
	}

	@Test
	public void theDayChangesAtMidnightUtc()
	{
		long before = millis(2026, 11, 11, 23, 59, 59);
		long after = millis(2026, 11, 12, 0, 0, 0);

		assertEquals(LocalDate.of(2026, 11, 11).toEpochDay(), calendar.dayOf(before));
		assertEquals(LocalDate.of(2026, 11, 12).toEpochDay(), calendar.dayOf(after));
		assertEquals(1000L, calendar.millisToReset(before));
		assertEquals(86_400_000L, calendar.millisToReset(after));
	}

	@Test
	public void theWeekStartsOnMondayAtMidnightUtc()
	{
		long sunday = millis(2026, 11, 8, 23, 59, 59);
		long monday = millis(2026, 11, 9, 0, 0, 0);
		long nextSunday = millis(2026, 11, 15, 23, 59, 59);

		assertNotEquals(calendar.weekOf(sunday), calendar.weekOf(monday));
		assertEquals(calendar.weekOf(monday), calendar.weekOf(nextSunday));
		assertEquals(calendar.weekOf(monday) + 1, calendar.weekOf(millis(2026, 11, 16, 0, 0, 0)));
		assertEquals(1000L, calendar.millisToWeeklyReset(nextSunday));
		assertEquals(7 * 86_400_000L, calendar.millisToWeeklyReset(monday));
	}

	@Test
	public void theEpochAndTimesBeforeItStillFallInTheRightDayAndWeek()
	{
		assertEquals(0L, calendar.dayOf(0L));
		assertEquals(-1L, calendar.dayOf(-1L));
		assertEquals(0L, calendar.weekOf(millis(1969, 12, 29, 0, 0, 0)));
		assertEquals(1L, calendar.weekOf(millis(1970, 1, 5, 0, 0, 0)));
		assertEquals(0L, calendar.weekOf(millis(1970, 1, 4, 23, 59, 59)));
	}
}
