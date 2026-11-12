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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Checks the places where battles the player steers, healing items and the Elite challenge meet. Each was built on
 * its own, so these rules belong to none of them alone.
 */
public class JoinedFeaturesTest
{
	private static final long HOUR = 3_600_000L;

	private final List<String> messages = new ArrayList<>();
	private SpeciesTable species;
	private GoGame game;
	private long now = 1_000_000L;

	private static String elite()
	{
		StringBuilder stages = new StringBuilder();
		for (int i = 1; i <= 5; i++)
		{
			stages.append(i > 1 ? "," : "").append("{\"id\":\"e").append(i).append("\",\"city\":\"Elite ").append(i)
				.append("\",\"team\":[{\"species\":\"Mite\",\"level\":70}]}");
		}

		return "{\"minX\":3180,\"minY\":3346,\"maxX\":3202,\"maxY\":3372,\"firstDragonJars\":10,"
			+ "\"firstSpecialtyJars\":3,\"firstEgg\":\"LEGENDARY\",\"laterDragonJars\":2,\"stages\":[" + stages + "]}";
	}

	@Before
	public void setUp()
	{
		species = new SpeciesTable(Arrays.asList(new Species("Mite", 1, CombatType.MELEE),
			new Species("Brute", 200, CombatType.MELEE)));
		game = new GoGame(species, new Random(7), () -> now, notice -> messages.add(notice.text()));
		game.elite(EliteTable.load(new Gson(), new StringReader(elite()), species));
		game.data().team.add(new Creature("brute", 60, now));
		for (int i = 0; i < 8; i++)
			game.data().sigils.add("city-" + i);

		game.moveTo(3191, 3367, 0);
	}

	private boolean said(String text)
	{
		return messages.stream().anyMatch(line -> line.contains(text));
	}

	@Test
	public void aSteeredBattleIsRefusedDuringAnEliteRun()
	{
		game.eliteChallenge();
		assertTrue(game.eliteOpen());
		messages.clear();

		assertNull(game.startBattle(new WildEntry(1, species.find("Mite"), 3, 3)));
		assertNull(game.startChallenge("anyone"));

		assertTrue(said("Finish or give up the Elite challenge first"));
	}

	@Test
	public void aSteeredBattleStartsAgainOnceTheRunIsGivenUp()
	{
		game.eliteChallenge();
		game.eliteForfeit();

		assertNotNull(game.startBattle(new WildEntry(1, species.find("Mite"), 3, 3)));
	}

	@Test
	public void aSalveUsedBetweenEliteBattlesHealsAndTheHealthStaysWhereItWasPut()
	{
		game.eliteChallenge();
		assertTrue(game.eliteOpen());
		Creature lead = game.data().lead();
		lead.hpFraction = 0.5;
		lead.hpStampMs = now;
		game.data().addItems(ItemType.SMALL_SALVE, 1);

		assertTrue(game.useItem(lead, ItemType.SMALL_SALVE));
		now += HOUR;
		game.snapshot();

		assertEquals(0.75, lead.hpFractionAt(now), 1e-9);
		assertEquals(0, game.data().itemCount(ItemType.SMALL_SALVE));
	}

	@Test
	public void thePanelStateKeepsItemsAndGoalsWhenABattleWaits()
	{
		game.data().addItems(ItemType.REVIVE, 2);
		BattleSession session = game.startBattle(new WildEntry(1, species.find("Mite"), 3, 3));
		BattleView waiting = game.battleView(session, 10, 1.0);
		PanelState plain = game.snapshot();

		PanelState state = plain.withBattle(waiting);

		assertSame(waiting, state.battle());
		assertEquals(Integer.valueOf(2), state.items().get(ItemType.REVIVE));
		assertSame(plain.elite(), state.elite());
		assertSame(plain.milestones(), state.milestones());
		assertNull(plain.battle());
	}

	@Test
	public void theFollowerTagShowsTheSpeciesNameAsWrittenNotTheSavedKey()
	{
		Creature lead = game.data().lead();
		assertEquals("brute", lead.species);

		assertEquals("Brute", FollowerTagOverlay.nameFor(lead.nickname, game.speciesNameOf(lead)));

		lead.nickname = "Rex";
		assertEquals("Rex", FollowerTagOverlay.nameFor(lead.nickname, game.speciesNameOf(lead)));
	}
}
