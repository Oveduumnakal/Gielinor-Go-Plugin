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
import static org.junit.Assert.assertTrue;

/** Fainting in the Elite challenge: a faint is final for the run unless a draught is used, and a bank waits. */
public class EliteFaintTest
{
	private final List<String> messages = new ArrayList<>();
	private GoGame game;
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
			new Species("Brute", 200, CombatType.STAB)));
		game = new GoGame(species, new Random(7), () -> now, notice -> messages.add(notice.text()));
		game.elite(EliteTable.load(new Gson(), new StringReader(json("Mite", 70)), species));
		for (int i = 0; i < 8; i++)
			game.data().sigils.add("city-" + i);

		game.moveTo(3191, 3367, 0);
	}

	private static TurnResult fightOut(GoGame played, BattleSession battle)
	{
		TurnResult last = null;
		while (!battle.over())
		{
			BattleState state = battle.state();
			last = played.act(battle, state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack());
			assertNotNull(last);
		}

		return last;
	}

	@Test
	public void aSteeredEliteLossFaintsTheCreatureAndItStaysFaintedAfterTheRun()
	{
		Creature weak = new Creature("mite", 1, now);
		game.data().team.add(weak);

		BattleSession battle = game.eliteSteered();
		TurnResult end = fightOut(game, battle);

		assertEquals(TurnResult.Status.LOST, end.status());
		assertFalse(game.eliteOpen());
		assertTrue(weak.fainted);
		assertTrue(messages.toString(), messages.contains("Mite fainted. Revive it at a bank"));
	}

	@Test
	public void anAutoEliteLossFaintsTheCreature()
	{
		Creature weak = new Creature("mite", 1, now);
		game.data().team.add(weak);

		TeamBattleResult result = game.eliteChallenge();

		assertNotNull(result);
		assertFalse(result.won());
		assertTrue(weak.fainted);
	}

	@Test
	public void aFreeReviveIsRefusedWhileARunIsOpenAndAfterwardsItWorksAtABank()
	{
		Creature brute = new Creature("brute", 60, now);
		Creature other = new Creature("mite", 5, now);
		game.data().team.add(brute);
		game.data().team.add(other);
		game.setBankNear(true);
		BattleSession battle = game.eliteSteered();
		fightOut(game, battle);
		assertTrue("a run is open after the first win", game.eliteOpen());
		other.faint(now);

		assertFalse(game.reviveAtBank(other));

		assertTrue(other.fainted);
		assertTrue(messages.toString(), messages.contains("Finish or give up the Elite challenge first"));

		game.eliteForfeit();

		assertTrue(game.reviveAtBank(other));
	}

	@Test
	public void theAutomaticReviveWaitsWhileARunIsOpenAndWorksAfterwards()
	{
		Creature brute = new Creature("brute", 60, now);
		Creature other = new Creature("mite", 5, now);
		game.data().team.add(brute);
		game.data().team.add(other);
		game.setBankNear(true);
		fightOut(game, game.eliteSteered());
		assertTrue(game.eliteOpen());
		other.faint(now);

		assertFalse(game.reviveHere());
		assertTrue(other.fainted);

		game.eliteForfeit();

		assertTrue(game.reviveHere());
		assertFalse(other.fainted);
	}

	@Test
	public void aReviveDraughtStillWorksDuringARun()
	{
		Creature brute = new Creature("brute", 60, now);
		Creature other = new Creature("mite", 5, now);
		game.data().team.add(brute);
		game.data().team.add(other);
		game.data().addItems(ItemType.REVIVE, 1);
		fightOut(game, game.eliteSteered());
		assertTrue(game.eliteOpen());
		other.faint(now);

		assertTrue(game.useItem(other, ItemType.REVIVE));

		assertFalse(other.fainted);
	}

	@Test
	public void theRescueWaitsWhileARunIsOpen()
	{
		Creature brute = new Creature("brute", 60, now);
		game.data().team.add(brute);
		fightOut(game, game.eliteSteered());
		assertTrue(game.eliteOpen());
		brute.faint(now);
		now += 60 * 60_000L;

		assertFalse(game.rescue());
		assertTrue(brute.fainted);
	}
}
