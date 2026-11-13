/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
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

/** Plays the Elite challenge one turn at a time, as a battle the player steers. */
public class EliteSteeredTest
{
	private final List<String> messages = new ArrayList<>();
	private GoGame game;
	private EliteTable weak;
	private long now = 1_000_000L;

	private static String json(String species, int level)
	{
		StringBuilder stages = new StringBuilder();
		for (int i = 1; i <= 5; i++)
		{
			stages.append(i > 1 ? "," : "").append("{\"id\":\"e").append(i).append("\",\"city\":\"Elite ").append(i)
				.append("\",\"team\":[{\"species\":\"").append(species).append("\",\"level\":").append(level)
				.append("}]}");
		}

		return "{\"minX\":3180,\"minY\":3346,\"maxX\":3202,\"maxY\":3372,\"firstDragonJars\":10,"
			+ "\"firstSpecialtyJars\":3,\"firstEgg\":\"LEGENDARY\",\"laterDragonJars\":2,\"stages\":[" + stages + "]}";
	}

	@Before
	public void setUp()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(new Species("Mite", 1, CombatType.STAB),
			new Species("Brute", 200, CombatType.STAB), new Species("Titan", 400, CombatType.STAB)));
		weak = EliteTable.load(new Gson(), new StringReader(json("Mite", 70)), species);
		game = new GoGame(species, new Random(7), () -> now, notice -> messages.add(notice.text()));
		game.elite(weak);
		game.data().team.add(new Creature("brute", 60, now));
		for (int i = 0; i < 8; i++)
			game.data().sigils.add("city-" + i);

		game.moveTo(3191, 3367, 0);
	}

	private Creature lead()
	{
		return game.data().lead();
	}

	private String verdict(BattleSession battle, TurnResult end)
	{
		List<BattleLine> lines = game.closingLines(battle, end);
		BattleLine first = lines.get(0);
		return first.text();
	}

	private static double healthOf(BattleSession battle)
	{
		Combatant mine = battle.state().mine();
		return mine.hpFraction();
	}

	private TurnResult fightOut(BattleSession battle)
	{
		TurnResult last = null;
		while (!battle.over())
		{
			BattleState state = battle.state();
			last = game.act(battle, state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack());
			assertNotNull(last);
		}

		return last;
	}

	@Test
	public void aSteeredBattleOpensARunAndAWinMovesItOn()
	{
		BattleSession battle = game.eliteSteered();

		assertNotNull(battle);
		assertTrue(battle.isElite());
		assertFalse(battle.wild());
		assertEquals(1, battle.eliteNumber());
		assertTrue(game.eliteOpen());
		assertFalse(game.battleView(battle, 10, 1.0).canCatch());
		TurnResult end = fightOut(battle);
		assertEquals(TurnResult.Status.WON, end.status());
		assertTrue(game.eliteOpen());
		assertEquals(1, game.data().battlesWon);
		assertTrue(verdict(battle, end).contains("Next: Elite 2"));
		assertTrue(lead().hpFraction < 1.0);
		EliteView view = game.snapshot().elite();
		assertEquals(2, view.stage());
	}

	@Test
	public void fiveSteeredWinsClearTheChallenge()
	{
		for (int i = 0; i < 5; i++)
		{
			BattleSession battle = game.eliteSteered();
			assertNotNull("battle " + (i + 1), battle);
			assertEquals(i + 1, battle.eliteNumber());
			lead().hpFraction = 1.0;
			fightOut(battle);
		}

		assertFalse(game.eliteOpen());
		assertEquals(1, game.data().eliteClears);
		assertEquals(5, game.data().battlesWon);
	}

	@Test
	public void runningLosesTheBattleAndTheRunStartsOver()
	{
		BattleSession battle = game.eliteSteered();

		TurnResult ran = game.act(battle, BattleChoice.run());

		assertEquals(TurnResult.Status.RAN, ran.status());
		assertFalse(game.eliteOpen());
		assertEquals(0, game.data().battlesWon);
		assertTrue(verdict(battle, ran).contains("The Elite challenge is over"));
		assertEquals(1, game.eliteSteered().eliteNumber());
	}

	@Test
	public void leavingTheBattleCountsAsALossAndSaysSo()
	{
		BattleSession battle = game.eliteSteered();
		messages.clear();

		game.forfeit(battle);

		assertFalse(game.eliteOpen());
		assertTrue(messages.stream().anyMatch(line -> line.contains("The Elite challenge is over")));
	}

	@Test
	public void aLostBattleEndsTheRun()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(new Species("Brute", 200, CombatType.STAB),
			new Species("Titan", 400, CombatType.STAB)));
		game.elite(EliteTable.load(new Gson(), new StringReader(json("Titan", 99)), species));
		BattleSession battle = game.eliteSteered();

		TurnResult end = fightOut(battle);

		assertEquals(TurnResult.Status.LOST, end.status());
		assertFalse(game.eliteOpen());
		assertEquals(0, game.data().eliteClears);
	}

	@Test
	public void healthCarriesAndNothingRestsBetweenSteeredBattles()
	{
		fightOut(game.eliteSteered());
		double after = lead().hpFraction;
		now += 10 * 3_600_000L;

		BattleSession next = game.eliteSteered();

		assertEquals(after, healthOf(next), 0.0);
	}

	@Test
	public void anItemIsSpentAsATurnInASteeredEliteBattle()
	{
		game.data().addItems(ItemType.SMALL_SALVE, 2);
		BattleSession battle = game.eliteSteered();
		game.act(battle, BattleChoice.attack());

		TurnResult used = game.act(battle, BattleChoice.item(ItemType.SMALL_SALVE, 0));

		assertNotNull(used);
		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
	}

	@Test
	public void aRunWithNoFitCreatureCannotStartASteeredBattle()
	{
		lead().hpFraction = 0.1;
		lead().hpStampMs = now;

		assertNull(game.eliteSteered());
		assertFalse(game.eliteOpen());
	}
}
