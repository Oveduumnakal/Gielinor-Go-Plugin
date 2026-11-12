/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks which events move which tasks, that rewards come out once, and the day, week and streak rules. */
public class TaskBoardTest
{
	private static final long DAY = 86_400_000L;
	private static final long START = 20_000L * DAY + 3_600_000L;

	private final TaskCalendar calendar = new TaskCalendar();
	private TaskProgress progress;
	private TaskBoard board;

	@Before
	public void setUp()
	{
		progress = new TaskProgress();
		board = new TaskBoard(calendar, progress);
		board.rollover(START);
	}

	/** Finds a UTC day, at or after the start, whose daily tasks include a kind. */
	private long dayWith(TaskKind kind, long from)
	{
		for (long day = from; day < from + 400; day++)
		{
			for (Task task : calendar.dailyFor(day))
			{
				if (task.kind() == kind)
					return day;
			}
		}

		throw new AssertionError("no day with " + kind);
	}

	/** Finds the position of a kind in a day's tasks, or fails. */
	private int slot(TaskKind kind)
	{
		for (int i = 0; i < board.daily().size(); i++)
		{
			if (board.daily().get(i).kind() == kind)
				return i;
		}

		throw new AssertionError("not today: " + kind);
	}

	private void startOn(long day)
	{
		progress = new TaskProgress();
		board = new TaskBoard(calendar, progress);
		board.rollover(day * DAY + 1000L);
	}

	@Test
	public void onlyMatchingEventsCountAndAStyleTaskNeedsItsStyle()
	{
		long day = dayWith(TaskKind.CATCH_STYLE, 20_000L);
		startOn(day);
		int style = slot(TaskKind.CATCH_STYLE);
		CombatType wanted = board.daily().get(style).style();
		CombatType other = wanted == CombatType.MELEE ? CombatType.MAGIC : CombatType.MELEE;

		board.caught(other, false);
		board.battleWon();
		board.walked(50);
		assertEquals(0, progress.daily[style]);

		board.caught(wanted, false);
		assertEquals(1, progress.daily[style]);
	}

	@Test
	public void anUndergroundTaskNeedsAnUndergroundCatch()
	{
		startOn(dayWith(TaskKind.CATCH_UNDERGROUND, 20_000L));
		int slot = slot(TaskKind.CATCH_UNDERGROUND);

		List<Task> surface = board.caught(CombatType.MELEE, false);
		assertTrue(surface.stream().noneMatch(task -> task.kind() == TaskKind.CATCH_UNDERGROUND));
		assertEquals(0, progress.daily[slot]);

		List<Task> done = board.caught(CombatType.MELEE, true);
		assertTrue(done.stream().anyMatch(t -> t.kind() == TaskKind.CATCH_UNDERGROUND));
		assertTrue(progress.dailyPaid[slot]);
	}

	@Test
	public void eachEventKindMovesItsOwnTask()
	{
		startOn(dayWith(TaskKind.WIN_BATTLES, 20_000L));
		int wins = slot(TaskKind.WIN_BATTLES);
		board.battleWon();
		assertEquals(1, progress.daily[wins]);

		startOn(dayWith(TaskKind.WALK, 20_000L));
		int walk = slot(TaskKind.WALK);
		board.walked(40);
		assertEquals(40, progress.daily[walk]);

		startOn(dayWith(TaskKind.GAIN_XP, 20_000L));
		int xp = slot(TaskKind.GAIN_XP);
		board.gainedXp(120);
		assertEquals(120, progress.daily[xp]);

		startOn(dayWith(TaskKind.HATCH, 20_000L));
		assertEquals(1, board.hatched().size());
		assertEquals(1, progress.daily[slot(TaskKind.HATCH)]);
	}

	@Test
	public void progressStopsAtTheGoalAndTheRewardComesOutOnce()
	{
		startOn(dayWith(TaskKind.WALK, 20_000L));
		int walk = slot(TaskKind.WALK);
		int goal = board.daily().get(walk).goal();

		assertTrue(board.walked(goal - 1).isEmpty());
		List<Task> done = board.walked(10);
		assertEquals(1, done.size());
		assertEquals(TaskKind.WALK, done.get(0).kind());
		assertEquals(goal, progress.daily[walk]);

		assertTrue(board.walked(1000).isEmpty());
		assertEquals(goal, progress.daily[walk]);
	}

	@Test
	public void aHugeStepCountDoesNotOverflow()
	{
		startOn(dayWith(TaskKind.WALK, 20_000L));
		int walk = slot(TaskKind.WALK);

		board.walked(Integer.MAX_VALUE);
		board.walked(Integer.MAX_VALUE);

		assertEquals(board.daily().get(walk).goal(), progress.daily[walk]);
	}

	@Test
	public void aNewDayResetsDailyProgressAndKeepsTheWeek()
	{
		long monday = mondayOnOrAfter(20_000L);
		startOn(monday);
		board.walked(30);
		board.caught(CombatType.MELEE, false);
		progress.weekly = 4;

		assertTrue(board.rollover((monday + 1) * DAY + 5L));

		assertEquals(monday + 1, progress.day);
		for (int i = 0; i < progress.daily.length; i++)
		{
			assertEquals(0, progress.daily[i]);
			assertFalse(progress.dailyPaid[i]);
		}

		assertEquals(4, progress.weekly);
	}

	@Test
	public void aNewWeekResetsWeeklyProgress()
	{
		long monday = mondayOnOrAfter(20_000L);
		startOn(monday);
		progress.weekly = 4;
		progress.weeklyPaid = true;

		board.rollover((monday + 6) * DAY + 5L);
		assertEquals(4, progress.weekly);
		assertTrue(progress.weeklyPaid);

		assertTrue(board.rollover((monday + 7) * DAY));
		assertEquals(0, progress.weekly);
		assertFalse(progress.weeklyPaid);
	}

	@Test
	public void rolloverReportsNoChangeWhenNothingMoved()
	{
		assertFalse(board.rollover(START + 1000L));
		assertFalse(board.rollover(START + 2000L));
	}

	@Test
	public void theWeeklyTaskFinishesAndPaysOnce()
	{
		long week = calendar.weekOf(START);
		while (calendar.weeklyFor(week).kind() != TaskKind.CHAMPION)
			week++;

		long day = week * 7 - 3;
		startOn(day);
		assertEquals(TaskKind.CHAMPION, board.weekly().kind());

		List<Task> done = board.championBeaten();
		assertEquals(1, done.size());
		assertTrue(done.get(0).weekly());
		assertTrue(board.championBeaten().isEmpty());
	}

	@Test
	public void theStreakStartsAtOneAndGrowsOnConsecutiveDays()
	{
		startOn(20_000L);
		assertEquals(1, board.recordCatch().days());

		board.rollover(20_001L * DAY);
		assertEquals(2, board.recordCatch().days());

		board.rollover(20_002L * DAY);
		assertEquals(3, board.recordCatch().days());
	}

	@Test
	public void aSkippedDayStartsTheStreakOverAtOne()
	{
		startOn(20_000L);
		board.recordCatch();
		board.rollover(20_001L * DAY);
		board.recordCatch();

		board.rollover(20_003L * DAY);

		assertEquals(1, board.recordCatch().days());
	}

	@Test
	public void twoCatchesInOneDayCountOnce()
	{
		startOn(20_000L);

		assertNotNull(board.recordCatch());
		assertNull(board.recordCatch());
		assertEquals(1, progress.streak);
	}

	@Test
	public void theSeventhAndFourteenthDayEarnAnEgg()
	{
		startOn(20_000L);
		for (int n = 1; n <= 14; n++)
		{
			board.rollover((20_000L + n - 1) * DAY);
			TaskBoard.Streak streak = board.recordCatch();
			assertEquals(n, streak.days());
			assertEquals(n == 7 || n == 14, streak.eggDue());
		}
	}

	@Test
	public void theViewShowsTheStreakAsBrokenAfterAMissedDay()
	{
		startOn(20_000L);
		board.recordCatch();
		board.rollover(20_001L * DAY);
		board.recordCatch();

		assertEquals(2, board.view(20_001L * DAY + 1000L, JarType.BRONZE).streak());
		assertEquals(2, board.view(20_002L * DAY + 1000L, JarType.BRONZE).streak());
		assertEquals(0, board.view(20_003L * DAY + 1000L, JarType.BRONZE).streak());
		assertEquals(7, board.view(20_003L * DAY + 1000L, JarType.BRONZE).eggIn());
	}

	@Test
	public void theViewListsThreeDailyRowsThenTheWeeklyRow()
	{
		TaskView view = board.view(START, JarType.IRON);

		assertEquals(4, view.rows().size());
		assertFalse(view.rows().get(0).weekly());
		assertTrue(view.rows().get(3).weekly());
		assertEquals(86_400_000L - 3_600_000L, view.millisToReset());
		assertEquals("5h 12m", TaskView.duration(5 * 3_600_000L + 12 * 60_000L + 59_000L));
		assertEquals("42m", TaskView.duration(42 * 60_000L));
		assertEquals("3d 4h", TaskView.duration((3 * 24 + 4) * 3_600_000L));
		assertEquals("4.3k / 5k", new TaskView.Row("x", 4321, 5000, false, "", false).count());
		assertEquals("2 / 3", new TaskView.Row("x", 2, 3, false, "", false).count());
	}

	private long mondayOnOrAfter(long day)
	{
		long result = day;
		while (calendar.weekOfDay(result) == calendar.weekOfDay(result - 1))
			result++;

		return result;
	}
}
