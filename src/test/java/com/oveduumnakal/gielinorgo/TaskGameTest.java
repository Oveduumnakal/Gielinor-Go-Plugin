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

/** Plays the daily and weekly tasks and the login streak through the game service, with a fake clock. */
public class TaskGameTest
{
	private static final long DAY = 86_400_000L;
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	private final TaskCalendar calendar = new TaskCalendar();
	private final List<String> messages = new ArrayList<>();
	private long now;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, new Species("Imp", 7, CombatType.STAB)));
		game = new GoGame(table, new Random(5), () -> now, notice -> messages.add(notice.text()));
		now = 20_000L * DAY + 1000L;
		game.data().addJars(JarType.BRONZE, 500);
	}

	private long dayWithDaily(TaskKind kind, boolean jarReward)
	{
		for (long day = 20_000L; day < 21_000L; day++)
		{
			for (Task task : calendar.dailyFor(day))
			{
				if (task.kind() == kind && (task.jars() > 0) == jarReward)
					return day;
			}
		}

		throw new AssertionError("no such day");
	}

	private void catchOne()
	{
		WildEntry chicken = new WildEntry(1, CHICKEN, 1, 1);
		CatchOutcome outcome = CatchOutcome.BROKE_FREE;
		for (int i = 0; i < 200 && outcome != CatchOutcome.CAUGHT; i++)
			outcome = game.tryCatch(chicken);

		assertEquals(CatchOutcome.CAUGHT, outcome);
	}

	private boolean said(String start)
	{
		return messages.stream().anyMatch(line -> line.startsWith(start));
	}

	@Test
	public void finishingAWalkTaskPaysItsJarsAndSaysSo()
	{
		long day = dayWithDaily(TaskKind.WALK, true);
		now = day * DAY + 1000L;
		int before = game.data().jarCount(JarType.BRONZE);

		game.addSteps(600);

		assertTrue(messages.contains("Daily task done: +2 Bronze jars"));
		assertEquals(before + 4 + 2, game.data().jarCount(JarType.BRONZE));
		TaskView view = game.snapshot().tasks();
		assertTrue(view.rows().stream().anyMatch(row -> row.done() && row.label().startsWith("Walk")));
	}

	@Test
	public void aTaskPaysOnlyOnce()
	{
		now = dayWithDaily(TaskKind.WALK, true) * DAY + 1000L;
		game.addSteps(600);
		int jars = game.data().jarCount(JarType.BRONZE);

		game.addSteps(150);

		assertEquals(jars + 1, game.data().jarCount(JarType.BRONZE));
		assertEquals(1, messages.stream().filter(line -> line.startsWith("Daily task done")).count());
	}

	@Test
	public void trainerXpRewardsAddTrainerXp()
	{
		now = dayWithDaily(TaskKind.WALK, false) * DAY + 1000L;
		long before = game.data().trainerXp;

		game.addSteps(600);

		assertTrue(messages.contains("Daily task done: +100 trainer XP"));
		assertEquals(before + 100L, game.data().trainerXp);
	}

	@Test
	public void jarRewardsComeInTheBestUnlockedTier()
	{
		now = dayWithDaily(TaskKind.WALK, true) * DAY + 1000L;
		game.data().trainerXp = XpRules.trainerXpForLevel(30);

		game.addSteps(600);

		assertTrue(messages.contains("Daily task done: +2 Rune jars"));
		assertEquals(2, game.data().jarCount(JarType.RUNE));
	}

	@Test
	public void aBattleXpAndCatchEachMoveTheirTasks()
	{
		now = dayWithDaily(TaskKind.GAIN_XP, true) * DAY + 1000L;
		game.data().team.add(new Creature("chicken", 1, now));

		game.addSkillXp("MINING", 5000);

		assertTrue(said("Daily task done: +2 Bronze jars"));
	}

	@Test
	public void theFirstCatchOfADayPaysTwoBronzeJarsAndASecondDoesNot()
	{
		catchOne();
		assertTrue(said("Day 1: +2 Bronze jars"));
		assertEquals(1, game.data().tasks.streak);

		messages.clear();
		catchOne();

		assertFalse(said("Day 1:"));
		assertEquals(1, game.data().tasks.streak);
	}

	@Test
	public void theDaysPlayedGrowOnConsecutiveDaysAndSurviveASkippedOne()
	{
		catchOne();
		now += DAY;
		catchOne();
		now += DAY;
		catchOne();
		assertEquals(3, game.data().tasks.streak);
		TaskView view = game.snapshot().tasks();
		assertEquals(3, view.streak());
		assertEquals(4, view.eggIn());

		now += 2 * DAY;
		TaskView later = game.snapshot().tasks();
		assertEquals(3, later.streak());
		catchOne();
		assertEquals(4, game.data().tasks.streak);
	}

	@Test
	public void theSeventhDayPaysAnUncommonEgg()
	{
		for (int day = 1; day <= 7; day++)
		{
			catchOne();
			now += DAY;
		}

		assertEquals(1, game.data().eggs.size());
		Egg egg = game.data().eggs.get(0);
		assertEquals(Rarity.UNCOMMON, egg.rarityTier());
		assertTrue(said("Day 7: +2 Bronze jars, +1 Uncommon egg"));
	}

	@Test
	public void aFullEggBagTurnsTheStreakEggIntoJars()
	{
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			game.data().eggs.add(new Egg(Rarity.COMMON));

		for (int day = 1; day <= 7; day++)
		{
			catchOne();
			now += DAY;
		}

		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());
		assertTrue(said("Day 7: +2 Bronze jars, +5 Bronze jars instead of an egg"));
	}

	@Test
	public void theWeeklyTaskPaysFiveJarsAndARareEgg()
	{
		now = weekWith(TaskKind.WALK) * DAY + 1000L;
		int before = game.data().jarCount(JarType.BRONZE);

		for (int i = 0; i < 40; i++)
			game.addSteps(125);

		assertTrue(said("Weekly task done: +5 Bronze jars, +1 Rare egg"));
		assertEquals(1, game.data().eggs.stream().filter(egg -> egg.rarityTier() == Rarity.RARE).count());
		assertTrue(game.data().jarCount(JarType.BRONZE) >= before + 5);
	}

	@Test
	public void aFullEggBagTurnsTheWeeklyEggIntoJars()
	{
		now = weekWith(TaskKind.WALK) * DAY + 1000L;
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
		{
			Egg egg = new Egg(Rarity.LEGENDARY);
			egg.tilesLeft = 1_000_000;
			game.data().eggs.add(egg);
		}

		game.addSteps(5000);

		assertTrue(said("Weekly task done: +5 Bronze jars, +5 Bronze jars instead of an egg"));
		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());
	}

	@Test
	public void aNewDayAndTheSnapshotMarkTheSaveForSaving()
	{
		game.snapshot();
		game.takeDirty();

		game.snapshot();
		assertFalse(game.takeDirty());

		now += DAY;
		game.snapshot();
		assertTrue(game.takeDirty());
	}

	@Test
	public void hatchingAnEggMovesTheHatchTask()
	{
		now = dayWithDaily(TaskKind.HATCH, true) * DAY + 1000L;
		Egg egg = new Egg(Rarity.COMMON);
		egg.tilesLeft = 5;
		game.data().eggs.add(egg);

		game.addSteps(10);

		assertTrue(said("Daily task done: +2 Bronze jars"));
		assertTrue(game.data().eggs.isEmpty());
	}

	private long weekWith(TaskKind kind)
	{
		for (long week = calendar.weekOf(now); week < calendar.weekOf(now) + 500; week++)
		{
			if (calendar.weeklyFor(week).kind() == kind)
				return week * 7 - 3;
		}

		throw new AssertionError("no such week");
	}
}
