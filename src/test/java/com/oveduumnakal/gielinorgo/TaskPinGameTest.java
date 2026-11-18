/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pins tasks to the tracker through the game service, with a fake clock. */
public class TaskPinGameTest
{
	private static final long DAY = 86_400_000L;

	private final TaskCalendar calendar = new TaskCalendar();
	private final List<String> messages = new ArrayList<>();
	private long now;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB),
			new Species("Imp", 7, CombatType.STAB)));
		game = new GoGame(table, new Random(5), () -> now, notice -> messages.add(notice.text()));
		now = walkDay() * DAY + 1000L;
	}

	private long walkDay()
	{
		for (long day = 20_000L; day < 21_000L; day++)
		{
			for (Task task : calendar.dailyFor(day))
			{
				if (task.kind() == TaskKind.WALK)
					return day;
			}
		}

		throw new AssertionError("no such day");
	}

	private int walkSlot()
	{
		List<TaskView.Row> rows = rows();
		for (int i = 0; i < TaskCalendar.DAILY_COUNT; i++)
		{
			if (rows.get(i).label().startsWith("Walk"))
				return i;
		}

		throw new AssertionError("no walk task");
	}

	private List<TrackerView.Line> trackerLines()
	{
		TrackerView view = game.snapshot().tracker();
		return view.lines();
	}

	private List<TaskView.Row> rows()
	{
		TaskView view = game.snapshot().tasks();
		return view.rows();
	}

	private TrackerView.Line line(String id)
	{
		for (TrackerView.Line line : trackerLines())
		{
			if (line.id().equals(id))
				return line;
		}

		return null;
	}

	@Test
	public void aPinnedDailyShowsItsLiveProgress()
	{
		int slot = walkSlot();
		assertTrue(game.pinTask(slot));
		assertEquals(rows().get(slot).count(), line("task:" + slot).valueText());
		assertTrue(line("task:" + slot).valueText().startsWith("0 / "));

		game.addSteps(100);

		TaskView.Row row = rows().get(slot);
		assertEquals(100, row.progress());
		assertEquals(row.count(), line("task:" + slot).valueText());
		assertEquals(row.label(), line("task:" + slot).label());
		assertEquals(100 / (double) row.goal(), line("task:" + slot).fraction(), 1e-9);
		assertTrue(row.pinned());
	}

	@Test
	public void aFinishedTaskCannotBePinnedAndSaysWhy()
	{
		int slot = walkSlot();
		game.addSteps(TaskKind.WALK.dailyGoal());

		assertFalse(game.pinTask(slot));

		assertEquals(0, game.pinCount());
		assertTrue(messages.toString(), messages.contains("Already done"));
	}

	@Test
	public void aPinnedTaskThatFinishesStaysPinnedAsADoneLine()
	{
		int slot = walkSlot();
		game.pinTask(slot);

		game.addSteps(TaskKind.WALK.dailyGoal());

		assertTrue(line("task:" + slot).done());
		assertEquals(1, game.pinCount());
		game.unpinTask(slot);
		assertEquals(0, game.pinCount());
	}

	@Test
	public void aNewDayClearsTheDailyPinsButNotTheWeeklyOne()
	{
		now = dayInsideAWeek() * DAY + 1000L;
		game.pinTask(0);
		game.pinTask(3);
		assertEquals(2, game.pinCount());

		now += DAY;

		TrackerView view = game.snapshot().tracker();
		assertEquals(1, view.lines().size());
		List<TrackerView.Line> lines = view.lines();
		assertEquals("task:3", lines.get(0).id());
		assertFalse(game.data().tasks.pinnedDaily[0]);
		assertTrue(game.data().tasks.pinnedWeekly);
	}

	@Test
	public void aNewWeekClearsTheWeeklyPin()
	{
		game.pinTask(3);
		assertEquals(1, game.pinCount());

		now += 7 * DAY;

		assertEquals(0, game.pinCount());
		assertTrue(trackerLines().isEmpty());
	}

	@Test
	public void theSixthPinIsRefusedWithTheFullNotice()
	{
		for (int slot = 0; slot < 4; slot++)
			assertTrue(game.pinTask(slot));

		game.data().eggs.add(new Egg(Rarity.RARE));
		game.data().eggs.add(new Egg(Rarity.COMMON));
		assertTrue(game.pinEgg(game.data().eggs.get(0)));
		assertFalse(game.pinEgg(game.data().eggs.get(1)));

		assertEquals(5, game.pinCount());
		assertTrue(messages.toString(), messages.contains("Tracker is full (5)"));
		List<Egg> eggs = game.data().eggs;
		assertFalse(eggs.get(1).pinned);
	}

	@Test
	public void unpinningFreesTheRoomAndUnpinAllClears()
	{
		for (int slot = 0; slot < 4; slot++)
			game.pinTask(slot);

		game.unpinTask(1);
		assertEquals(3, game.pinCount());
		game.unpinAll();

		assertEquals(0, game.pinCount());
		assertTrue(trackerLines().isEmpty());
	}

	@Test
	public void aBadSlotChangesNothing()
	{
		assertFalse(game.pinTask(-1));
		assertFalse(game.pinTask(4));
		game.unpinTask(9);

		assertEquals(0, game.pinCount());
	}

	@Test
	public void pinningMarksTheSaveDirty()
	{
		game.takeDirty();

		game.pinTask(0);

		assertTrue(game.takeDirty());
	}

	private long dayInsideAWeek()
	{
		for (long day = 20_000L; day < 20_100L; day++)
		{
			if (calendar.weekOfDay(day) == calendar.weekOfDay(day + 1))
				return day;
		}

		throw new AssertionError("no such day");
	}
}
