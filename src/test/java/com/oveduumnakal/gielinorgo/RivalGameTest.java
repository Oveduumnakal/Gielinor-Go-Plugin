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

/** Plays the rival through a game: who the rival is, when battles open, what a win pays and what a loss costs. */
public class RivalGameTest
{
	private final List<String> messages = new ArrayList<>();
	private GoGame game;
	private SpeciesTable species;
	private RivalTable rivals;
	private long now = 1_000_000L;

	@Before
	public void setUp() throws IOException
	{
		Gson gson = new Gson();
		try (Reader in = reader("species.json"))
		{
			species = SpeciesTable.load(gson, in);
		}

		try (Reader in = reader("rival.json"))
		{
			rivals = RivalTable.load(gson, in, species);
		}

		game = new GoGame(species, new Random(7), () -> now, notice -> messages.add(notice.text()));
		game.rival(rivals);
		game.data().team.add(new Creature("abyssal demon", 99, now));
		game.data().caught.put("abyssal demon", 1);
	}

	private static Reader reader(String name)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(name);
		assertNotNull(name, in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	private void sigils(int count)
	{
		for (int i = 0; i < count; i++)
			game.data().sigils.add("city-" + i);
	}

	private void restAll()
	{
		for (Creature creature : game.data().team)
			creature.hpFraction = 1.0;
	}

	private TurnResult fightOut(BattleSession battle)
	{
		TurnResult last = null;
		while (!battle.over())
		{
			BattleState state = battle.state();
			last = game.act(battle, state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack());
		}

		return last;
	}

	@Test
	public void theCardIsLockedUntilTwoSigilsAndSaysWhatOpensIt()
	{
		RivalView view = game.snapshot().rival();

		assertEquals(RivalView.Mode.LOCKED, view.mode());
		assertEquals("Bryn", view.name());
		assertEquals("Earn 2 sigils to meet Bryn", view.status());
		assertNull(view.team());
		assertNull(game.challenge(RivalRules.ID));
		assertTrue(messages.contains("Earn 2 sigils to meet Bryn again"));
		assertEquals(0, game.data().battlesWon);
	}

	@Test
	public void noRivalFileMeansNoCard()
	{
		game.rival(RivalTable.empty());

		RivalView view = game.snapshot().rival();

		assertEquals(RivalView.Mode.HIDDEN, view.mode());
		assertNull(game.challenge(RivalRules.ID));
	}

	@Test
	public void twoSigilsOpenBattleOneWithTheTeamLineAndReward()
	{
		sigils(2);

		RivalView view = game.snapshot().rival();

		assertEquals(RivalView.Mode.READY, view.mode());
		assertEquals(1, view.battle());
		assertEquals(rivals.before(1), view.line());
		assertEquals("Wizard 15, Goblin 14", view.team());
		assertTrue(view.reward().endsWith("a Rare egg"));
	}

	@Test
	public void aWinPaysOnceAndOpensNothingEarly()
	{
		sigils(2);
		int jars = game.data().jarCount(JarType.BRONZE);

		TeamBattleResult result = game.challenge(RivalRules.ID);

		assertNotNull(result);
		assertTrue(result.won());
		assertEquals(1, game.data().rival.won);
		assertEquals(1, game.data().battlesWon);
		assertEquals(1, game.data().eggs.size());
		Egg egg = game.data().eggs.get(0);
		assertEquals(Rarity.RARE, egg.rarityTier());
		assertEquals(jars + RivalRules.jars(1), game.data().jarCount(JarType.BRONZE));
		List<BattleLine> lines = result.lines();
		BattleLine first = lines.get(0);
		BattleLine last = lines.get(lines.size() - 1);
		assertTrue(first.text().startsWith("Bryn: \""));
		assertTrue(last.text().startsWith("Bryn: \""));
		assertEquals(0, game.data().sigils.size() - 2);
		assertNull("battle two needs four sigils", game.challenge(RivalRules.ID));
		assertEquals(1, game.data().eggs.size());
		RivalView view = game.snapshot().rival();
		assertEquals(RivalView.Mode.LOCKED, view.mode());
		assertEquals("Earn 4 sigils to meet Bryn again", view.status());
	}

	@Test
	public void aLossPaysNothingAndCanBeRetried()
	{
		sigils(2);
		game.data().team.clear();
		game.data().team.add(new Creature("goblin", 1, now));
		game.data().caught.put("goblin", 1);
		int jars = game.data().jarCount(JarType.BRONZE);

		TeamBattleResult result = game.challenge(RivalRules.ID);

		assertNotNull(result);
		assertFalse(result.won());
		assertEquals(0, game.data().rival.won);
		assertEquals(0, game.data().battlesWon);
		assertTrue(game.data().eggs.isEmpty());
		assertEquals(jars, game.data().jarCount(JarType.BRONZE));
		now += 24L * 3_600_000L;
		game.data().team.clear();
		game.data().team.add(new Creature("abyssal demon", 99, now));
		assertTrue(game.challenge(RivalRules.ID).won());
		assertEquals(1, game.data().rival.won);
	}

	@Test
	public void aSteeredBattleWinsLikeAnAutomaticOne()
	{
		sigils(2);

		BattleSession battle = game.startChallenge(RivalRules.ID);

		assertNotNull(battle);
		assertFalse(battle.wild());
		BattleLine opening = battle.opening();
		assertTrue(opening.text().startsWith("Bryn: \""));
		TurnResult end = fightOut(battle);
		List<BattleLine> closing = game.closingLines(battle, end);
		assertEquals(TurnResult.Status.WON, end.status());
		assertEquals("You beat Bryn!", closing.get(0).text());
		assertEquals(2, closing.size());
		assertEquals(1, game.data().rival.won);
		assertEquals(1, game.data().eggs.size());
	}

	@Test
	public void allFiveBattlesWinInOrderAndTheLastNeedsTheEliteClear()
	{
		sigils(8);
		for (int i = 0; i < 5; i++)
			game.data().team.add(new Creature("bloodthirsty kurask", 99, now));

		for (int battle = 1; battle <= 4; battle++)
		{
			restAll();
			assertTrue("battle " + battle, game.challenge(RivalRules.ID).won());
			assertEquals(battle, game.data().rival.won);
		}

		assertNull(game.challenge(RivalRules.ID));
		assertEquals("Clear the Elite challenge to meet Bryn again", messages.get(messages.size() - 1));
		game.data().eliteClears = 1;
		restAll();
		assertTrue(game.challenge(RivalRules.ID).won());
		RivalView view = game.snapshot().rival();
		assertEquals(RivalView.Mode.BEATEN, view.mode());
		assertEquals(rivals.after(5), view.line());
		assertNull(game.challenge(RivalRules.ID));
		assertEquals(5, game.data().rival.won);
	}

	@Test
	public void theRivalIsBlockedDuringAnEliteRun()
	{
		StringBuilder stages = new StringBuilder();
		for (int i = 1; i <= 5; i++)
		{
			stages.append(i > 1 ? "," : "").append("{\"id\":\"e").append(i).append("\",\"city\":\"Elite ").append(i)
				.append("\",\"team\":[{\"species\":\"Goblin\",\"level\":5}]}");
		}

		String json = "{\"minX\":3180,\"minY\":3346,\"maxX\":3202,\"maxY\":3372,\"firstDragonJars\":10,"
			+ "\"firstSpecialtyJars\":3,\"firstEgg\":\"LEGENDARY\",\"laterDragonJars\":2,\"stages\":["
			+ stages + "]}";
		game.elite(EliteTable.load(new Gson(), new StringReader(json), species));
		sigils(8);
		game.moveTo(3191, 3367, 0);
		assertNotNull(game.eliteSteered());
		assertTrue(game.eliteOpen());

		assertNull(game.challenge(RivalRules.ID));
		assertNull(game.startChallenge(RivalRules.ID));
		assertTrue(messages.contains("Finish or give up the Elite challenge first"));
		assertNull(game.data().rival);
	}

	@Test
	public void anOldSaveGetsARivalFromItsLeadAndKeepsIt()
	{
		sigils(2);

		assertNull(game.data().rival);
		game.snapshot();

		assertEquals("wizard", game.data().rival.starter);
		game.data().team.clear();
		game.data().team.add(new Creature("banshee", 20, now));
		game.snapshot();
		assertEquals("wizard", game.data().rival.starter);
	}

	@Test
	public void pickingAStarterFixesTheRivalAgainstIt()
	{
		GoGame fresh = new GoGame(species, new Random(7), () -> now, notice -> messages.add(notice.text()));
		fresh.starterRandom(new Random(5));
		fresh.rival(rivals);

		assertTrue(fresh.pickStarter(2));

		assertEquals("gnome archer", fresh.data().rival.starter);
	}

	@Test
	public void aRivalSaveRoundTripsAndOldOrDamagedOnesLoad()
	{
		Gson gson = new Gson();
		game.data().rival = new RivalProgress("imp");
		game.data().rival.won = 3;

		SaveData back = gson.fromJson(gson.toJson(game.data()), SaveData.class).sanitized();
		SaveData old = gson.fromJson("{\"version\":1}", SaveData.class).sanitized();
		SaveData damaged = gson.fromJson("{\"rival\":{\"starter\":\"imp\",\"won\":99}}", SaveData.class).sanitized();
		SaveData nameless = gson.fromJson("{\"rival\":{\"won\":2}}", SaveData.class).sanitized();

		assertEquals("imp", back.rival.starter);
		assertEquals(3, back.rival.won);
		assertNull(old.rival);
		assertEquals(RivalRules.BATTLES, damaged.rival.won);
		assertNull(nameless.rival);
	}
}
