/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks how XP gained while the plugin was away is credited by the game. */
public class GoGameOfflineTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private final Gson gson = new Gson();
	private GoGame game;

	@Before
	public void setUp()
	{
		game = gameWith(new Random(11), () -> 1_000_000L);
	}

	private GoGame gameWith(Random random, java.util.function.LongSupplier clock)
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN));
		return new GoGame(table, random, clock, notice -> messages.add(notice.text()));
	}

	private static Map<String, Integer> xp(Object... pairs)
	{
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
			map.put((String) pairs[i], (Integer) pairs[i + 1]);

		return map;
	}

	private static OfflineXp.Result earned(Map<String, Integer> before, Map<String, Integer> now)
	{
		return OfflineXp.compute(before, now, new Random(3));
	}

	private static void addGoblins(GoGame into, int count)
	{
		for (int i = 0; i < count; i++)
			into.data().team.add(new Creature("goblin", 1, 0));
	}

	@Test
	public void offlineXpTrainsTheTeam()
	{
		addGoblins(game, 3);

		game.addOfflineXp(earned(xp("HITPOINTS", 0), xp("HITPOINTS", 3_000)));

		for (Creature creature : game.data().team)
			assertEquals(100, creature.xp);
	}

	@Test
	public void offlineXpIsWorthTheSameAsOnlineXpOnALoneCreature()
	{
		addGoblins(game, 1);
		GoGame online = gameWith(new Random(11), () -> 1_000_000L);
		addGoblins(online, 1);

		game.addOfflineXp(earned(xp("HITPOINTS", 0), xp("HITPOINTS", 3_000)));
		online.addOsrsXp(3_000);

		Creature offlineLead = game.data().lead();
		Creature onlineLead = online.data().lead();
		assertEquals(onlineLead.xp, offlineLead.xp);
	}

	@Test
	public void theLeadGetsNoExtraShare()
	{
		int leadAhead = 0;
		int leadBehind = 0;
		Random seeds = new Random(1);
		for (int trial = 0; trial < 90; trial++)
		{
			GoGame fresh = gameWith(new Random(seeds.nextLong()), () -> 1_000_000L);
			addGoblins(fresh, 3);

			fresh.addOfflineXp(earned(xp("HITPOINTS", 0), xp("HITPOINTS", 3_010)));

			List<Creature> team = fresh.data().team;
			assertEquals(301, team.get(0).xp + team.get(1).xp + team.get(2).xp);
			long lead = team.get(0).xp;
			assertTrue(lead == 100 || lead == 101);
			leadAhead += lead == 101 ? 1 : 0;
			leadBehind += lead == 100 ? 1 : 0;
		}

		assertTrue("the lead sometimes gets the spare point: " + leadAhead, leadAhead > 10);
		assertTrue("and sometimes does not: " + leadBehind, leadBehind > 10);
	}

	@Test
	public void levelUpsPayThreeBronzeJarsEach()
	{
		game.addOfflineXp(earned(xp("HITPOINTS", 82), xp("HITPOINTS", 1_154)));

		assertEquals(SaveData.STARTING_JARS + 9 * GoGame.JARS_PER_LEVEL_UP, game.data().jarCount(JarType.BRONZE));
		assertTrue(messages.contains("Found 27 jars while you were away"));
	}

	@Test
	public void specialtyJarsAndItemsAreAdded()
	{
		game.addOfflineXp(earned(xp("MINING", 0, "HERBLORE", 0), xp("MINING", 40_000, "HERBLORE", 60_000)));

		assertTrue(game.data().jarCount(JarType.CAVE) >= 2);
		assertEquals(2, game.data().itemCount(ItemType.ANTIDOTE));
		assertTrue(game.data().itemCount(ItemType.SMALL_SALVE) >= 2);
	}

	@Test
	public void anEmptyTeamStillPaysJarsButTrainsNoone()
	{
		game.addOfflineXp(earned(xp("MINING", 13_034_431), xp("MINING", 13_074_431)));

		assertTrue(game.data().team.isEmpty());
		assertEquals(0L, game.data().trainerXp);
		assertEquals(2, game.data().jarCount(JarType.CAVE));
		assertEquals(Arrays.asList("Found 2 jars while you were away"), messages);
	}

	@Test
	public void offlineXpDoesNotAdvanceTheDailyTasks()
	{
		addGoblins(game, 2);
		String before = gson.toJson(game.data().tasks);

		game.addOfflineXp(earned(xp("HITPOINTS", 0), xp("HITPOINTS", 400_000)));

		assertEquals(before, gson.toJson(game.data().tasks));
		assertEquals(0, game.data().stepProgress);
	}

	@Test
	public void theClockIsNeverRead()
	{
		AtomicInteger reads = new AtomicInteger();
		GoGame counted = gameWith(new Random(11), () ->
		{
			reads.incrementAndGet();
			return 1_000_000L;
		});
		addGoblins(counted, 2);
		int before = reads.get();

		counted.addOfflineXp(earned(xp("HITPOINTS", 0, "MINING", 0), xp("HITPOINTS", 90_000, "MINING", 70_000)));

		assertEquals(before, reads.get());
	}

	@Test
	public void aClockTenYearsLaterChangesNothing()
	{
		GoGame now = gameWith(new Random(11), () -> 1_000_000L);
		GoGame later = gameWith(new Random(11), () -> 1_000_000L + 10L * 365 * 24 * 3_600_000L);
		addGoblins(now, 2);
		addGoblins(later, 2);
		OfflineXp.Result result = earned(xp("HITPOINTS", 0, "MINING", 0), xp("HITPOINTS", 90_000, "MINING", 70_000));

		now.addOfflineXp(result);
		later.addOfflineXp(result);

		assertEquals(gson.toJson(now.data()), gson.toJson(later.data()));
	}

	@Test
	public void anEmptyResultTellsNothing()
	{
		addGoblins(game, 2);
		game.takeDirty();

		game.addOfflineXp(earned(xp("MINING", 500), xp("MINING", 500)));

		assertTrue(messages.isEmpty());
		assertFalse(game.takeDirty());
		Creature lead = game.data().lead();
		assertEquals(0, lead.xp);
	}

	@Test
	public void theWelcomeComesFirstAndAtMostThreeReportLinesAreShown()
	{
		addGoblins(game, 2);

		game.addOfflineXp(earned(xp("MINING", 0, "HERBLORE", 0), xp("MINING", 40_000, "HERBLORE", 60_000)));

		List<String> report = new ArrayList<>();
		for (String line : messages)
		{
			if (line.startsWith("Welcome back") || line.startsWith("Found"))
				report.add(line);
		}

		assertEquals(3, report.size());
		assertEquals("Welcome back: your team trained +10,000 XP while you were away", report.get(0));
		assertTrue(report.get(1).endsWith(" while you were away"));
		assertTrue(report.get(2).startsWith("Found ") && report.get(2).contains("item"));
	}

	@Test
	public void aCappedGainSaysSo()
	{
		addGoblins(game, 1);

		game.addOfflineXp(earned(xp("HITPOINTS", 0), xp("HITPOINTS", 5_000_000)));

		assertEquals("Welcome back: your team trained +100,000 XP while you were away (capped)", messages.get(0));
	}

	@Test
	public void stampXpMarksTheSaveDirtyOnlyWhenSomethingChanged()
	{
		game.takeDirty();

		assertTrue(game.stampXp(xp("MINING", 5_000)));
		assertTrue(game.takeDirty());
		assertFalse(game.stampXp(xp("MINING", 5_000)));
		assertFalse(game.takeDirty());
		assertTrue(game.stampXp(xp("MINING", 5_100)));
		assertEquals(Integer.valueOf(5_100), game.xpSnapshot().get("MINING"));
	}

	@Test
	public void stampXpSkipsZeroAndOverall()
	{
		assertFalse(game.stampXp(xp("MINING", 0, "OVERALL", 9_000)));

		assertTrue(game.xpSnapshot().isEmpty());
	}

	@Test
	public void theSnapshotHandedOutIsACopy()
	{
		game.stampXp(xp("MINING", 5_000));

		game.xpSnapshot().put("MINING", 1);

		assertEquals(Integer.valueOf(5_000), game.xpSnapshot().get("MINING"));
	}
}
