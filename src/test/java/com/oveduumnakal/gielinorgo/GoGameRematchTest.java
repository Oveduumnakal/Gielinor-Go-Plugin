/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Plays the City Champion rematches after the Elite clear: teams, gold sigils, the daily reward and the save. */
public class GoGameRematchTest
{
	private static final long DAY = 86_400_000L;

	private final List<String> messages = new ArrayList<>();
	private SpeciesTable species;
	private GoGame game;
	private long now = 40_000_000L;

	@Before
	public void setUp()
	{
		species = new SpeciesTable(Arrays.asList(new Species("Mite", 1, CombatType.STAB),
			new Species("Titan", 400, CombatType.STAB)));
		Champion lumbridge = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250,
			Arrays.asList(new Champion.Member("Mite", 5), new Champion.Member("Mite", 6),
				new Champion.Member("Mite", 7)), JarType.IRON, 3);
		game = new GoGame(species, EvolutionTable.empty(), new ChampionTable(Arrays.asList(lumbridge), species),
			new Random(5), () -> now, notice -> messages.add(notice.text()));
		game.rematches(table(Arrays.asList(rematchEntry("lumbridge", 80))));
		game.data().team.add(new Creature("titan", 50, now));
		game.data().sigils.add("lumbridge");
		game.moveTo(3222, 3218, 0);
	}

	private RematchTable table(List<RematchTable.Entry> entries)
	{
		return new RematchTable(entries, species);
	}

	private static List<Champion.Member> members(int level)
	{
		List<Champion.Member> team = new ArrayList<>();
		for (int i = 0; i < RematchTable.TEAM_SIZE; i++)
			team.add(new Champion.Member("Mite", level + i));

		return team;
	}

	private static RematchTable.Entry rematchEntry(String id, int level)
	{
		return new RematchTable.Entry(id, members(level));
	}

	private ChampionView card()
	{
		PanelState state = game.snapshot();
		return state.champions().get(0);
	}

	private long epicEggs()
	{
		return game.data().eggs.stream().filter(egg -> egg.rarityTier() == Rarity.EPIC).count();
	}

	private long xp()
	{
		Creature lead = game.data().lead();
		return lead.xp;
	}

	private TeamBattleResult fight()
	{
		now += Creature.FULL_REST_MS;
		TeamBattleResult result = game.challenge("lumbridge");
		assertNotNull(result);
		assertTrue(result.won());
		return result;
	}

	@Test
	public void beforeTheEliteClearTheChampionIsFoughtWithTheFirstTeam()
	{
		assertFalse(card().rematchOpen());

		TeamBattleResult result = fight();

		BattleLine opening = result.lines().get(0);
		assertEquals("Challenging the Champion of Lumbridge!", opening.text());
		assertTrue(game.data().goldSigils.isEmpty());
		assertEquals(0, game.data().jarCount(JarType.DRAGON));
	}

	@Test
	public void afterTheClearTheCardOffersTheRematchTeam()
	{
		game.data().eliteClears = 1;

		ChampionView view = card();

		assertTrue(view.rematchOpen());
		assertFalse(view.gold());
		assertEquals("Mite 80, Mite 81, Mite 82, Mite 83, Mite 84, Mite 85", view.rematchTeam());
		assertEquals("Reward ready", view.rematchNote());
	}

	@Test
	public void theFirstRematchWinTurnsTheSigilGoldOnce()
	{
		game.data().eliteClears = 1;

		TeamBattleResult first = fight();

		BattleLine opening = first.lines().get(0);
		assertEquals("Rematch with the Champion of Lumbridge!", opening.text());
		assertTrue(game.data().goldSigils.contains("lumbridge"));
		assertEquals(GoGame.REMATCH_FIRST_JARS, game.data().jarCount(JarType.DRAGON));
		assertTrue(messages.contains("Gold sigil of Lumbridge earned!"));
		assertTrue(card().gold());
		assertEquals("Reward again in 13 h", card().rematchNote());

		fight();

		assertEquals(1, game.data().goldSigils.size());
		assertEquals(GoGame.REMATCH_FIRST_JARS, game.data().jarCount(JarType.DRAGON));
	}

	@Test
	public void theDailyRewardPaysOncePerUtcDayPerChampion()
	{
		game.data().eliteClears = 1;
		fight();
		int jars = game.data().jarCount(JarType.DRAGON);

		long before = xp();
		fight();
		long sameDayXp = xp() - before;

		assertEquals("no jar the same day", jars, game.data().jarCount(JarType.DRAGON));
		assertTrue(sameDayXp > 0);

		now += DAY;
		before = xp();
		fight();

		assertEquals("one Dragon jar the next day", jars + GoGame.REMATCH_DAILY_JARS,
			game.data().jarCount(JarType.DRAGON));
		assertEquals("full XP for the daily win", sameDayXp * 2, xp() - before, 1);
		assertEquals("Reward again in 13 h", card().rematchNote());

		fight();

		assertEquals(jars + GoGame.REMATCH_DAILY_JARS, game.data().jarCount(JarType.DRAGON));
	}

	@Test
	public void theEighthGoldSigilPaysTheMilestoneOnce()
	{
		game.milestones(new Milestones(species, Collections.emptyList()));
		game.data().eliteClears = 1;
		for (int i = 0; i < 7; i++)
			game.data().goldSigils.add("city-" + i);

		fight();

		assertTrue(game.data().milestones.contains(Milestones.GOLD_SIGILS_ID));
		assertEquals(1, epicEggs());
		assertEquals(Milestones.GOLD_SIGILS_SPECIALTY_JARS, game.data().jarCount(JarType.EMBER));

		now += DAY;
		fight();

		assertEquals(1, epicEggs());
		assertEquals(Milestones.GOLD_SIGILS_SPECIALTY_JARS, game.data().jarCount(JarType.EMBER));
	}

	@Test
	public void aSteeredRematchFightsTheSecondTeamAndPaysTheSame()
	{
		game.data().eliteClears = 1;
		now += Creature.FULL_REST_MS;

		BattleSession battle = game.startChallenge("lumbridge");

		assertNotNull(battle);
		assertTrue(battle.isRematch());
		Champion foe = battle.champion();
		assertEquals(RematchTable.TEAM_SIZE, foe.team().size());
		TurnResult last = null;
		while (!battle.over())
		{
			BattleState state = battle.state();
			last = game.act(battle, state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack());
		}

		assertEquals(TurnResult.Status.WON, last.status());
		assertTrue(game.data().goldSigils.contains("lumbridge"));
		assertEquals(GoGame.REMATCH_FIRST_JARS, game.data().jarCount(JarType.DRAGON));
	}

	@Test
	public void aRematchIsBlockedDuringAnEliteRun()
	{
		game.data().eliteClears = 1;
		for (int i = 0; i < 8; i++)
			game.data().sigils.add("city-" + i);

		StringBuilder stages = new StringBuilder();
		for (int i = 1; i <= 5; i++)
		{
			stages.append(i > 1 ? "," : "").append("{\"id\":\"e").append(i).append("\",\"city\":\"E").append(i)
				.append("\",\"team\":[{\"species\":\"Mite\",\"level\":70}]}");
		}

		game.elite(EliteTable.load(new Gson(), new StringReader("{\"minX\":3180,\"minY\":3346,\"maxX\":3202,"
			+ "\"maxY\":3372,\"firstDragonJars\":10,\"firstSpecialtyJars\":3,\"firstEgg\":\"LEGENDARY\","
			+ "\"laterDragonJars\":2,\"stages\":[" + stages + "]}"), species));
		game.moveTo(3191, 3367, 0);
		assertNotNull(game.eliteSteered());
		game.moveTo(3222, 3218, 0);

		assertNull(game.challenge("lumbridge"));
		assertNull(game.startChallenge("lumbridge"));
		assertTrue(game.data().goldSigils.isEmpty());
	}

	@Test
	public void anOldSaveLoadsWithNoGoldSigils()
	{
		SaveData old = new Gson().fromJson("{\"sigils\":[\"lumbridge\"],\"eliteClears\":1}", SaveData.class)
			.sanitized();
		SaveData damaged = new Gson().fromJson("{\"goldSigils\":null,\"rematchDay\":null}", SaveData.class)
			.sanitized();

		assertTrue(old.goldSigils.isEmpty());
		assertTrue(old.rematchDay.isEmpty());
		assertTrue(damaged.goldSigils.isEmpty());
		assertTrue(damaged.rematchDay.isEmpty());
	}

	@Test
	public void aRematchTeamNeedsSixKnownCreaturesAtLevels80To99()
	{
		List<Champion.Member> five = new ArrayList<>(members(80));
		five.remove(0);
		List<Champion.Member> low = new ArrayList<>(members(80));
		low.set(0, new Champion.Member("Mite", 79));
		List<Champion.Member> unknown = new ArrayList<>(members(80));
		unknown.set(0, new Champion.Member("Nobody", 90));

		RematchTable built = table(Arrays.asList(rematchEntry("good", 94), new RematchTable.Entry("five", five),
			new RematchTable.Entry("low", low), new RematchTable.Entry("unknown", unknown),
			new RematchTable.Entry(null, five)));

		assertEquals(RematchTable.TEAM_SIZE, built.teamOf("good").size());
		assertTrue(built.teamOf("five").isEmpty());
		assertTrue(built.teamOf("low").isEmpty());
		assertTrue(built.teamOf("unknown").isEmpty());
		assertTrue(built.teamOf("missing").isEmpty());
	}

	@Test
	public void theBundledRematchTeamsCoverEveryChampion() throws IOException
	{
		Gson gson = new Gson();
		SpeciesTable real;
		ChampionTable champions;
		RematchTable rematches;
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			real = SpeciesTable.load(gson, reader);
		}

		try (Reader reader = open(ChampionTable.RESOURCE))
		{
			champions = ChampionTable.load(gson, reader, real);
		}

		try (Reader reader = open(RematchTable.RESOURCE))
		{
			rematches = RematchTable.load(gson, reader, real);
		}

		assertEquals(8, champions.all().size());
		int previousTop = 0;
		for (Champion champion : champions.all())
		{
			List<Champion.Member> team = rematches.teamOf(champion.id());
			assertEquals(champion.id(), RematchTable.TEAM_SIZE, team.size());
			int top = 0;
			for (Champion.Member member : team)
			{
				Species found = real.find(member.species());
				assertEquals(member.species(), found.name());
				assertFalse(member.species() + " is quest only", found.questOnly());
				top = Math.max(top, member.level());
			}

			assertTrue(champion.id() + " must be harder than the city before it", top > previousTop);
			previousTop = top;
		}
	}

	@Test
	public void aClockSetBackDoesNotPayTheDailyRewardAgain()
	{
		game.data().eliteClears = 1;
		fight();
		int jars = game.data().jarCount(JarType.DRAGON);

		now -= DAY;
		fight();
		now += DAY;
		fight();

		assertEquals(jars, game.data().jarCount(JarType.DRAGON));
	}

	private static Reader open(String name)
	{
		InputStream in = GoGameRematchTest.class.getResourceAsStream(name);
		assertNotNull(name, in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}
}
