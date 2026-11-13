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
import static org.junit.Assert.assertTrue;

/** Checks that panel actions on a creature do nothing when the list moved since the panel drew it. */
public class StalePanelPickTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species WOLF = new Species("Wolf", 106, CombatType.SLASH);

	private final List<String> messages = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, WOLF));
		game = new GoGame(table, new Random(3), () -> 1_000_000L, notice -> messages.add(notice.text()));
		game.setBankNear(true);
	}

	private TradePick storagePick(int index)
	{
		List<CreatureView> storage = game.snapshot().storage();
		return TradePick.of(Holding.STORAGE, index, storage.get(index));
	}

	private TradePick teamPick(int index)
	{
		List<CreatureView> team = game.snapshot().team();
		return TradePick.of(Holding.TEAM, index, team.get(index));
	}

	@Test
	public void releaseDoesNothingWhenTheListShiftedUnderThePick()
	{
		game.data().team.add(new Creature("goblin", 1, 0));
		game.data().storage.add(new Creature("goblin", 5, 0));
		game.data().storage.add(new Creature("wolf", 6, 0));
		TradePick pick = storagePick(1);
		game.data().storage.remove(0);

		game.release(pick);

		assertEquals(1, game.data().storage.size());
		assertTrue(messages.contains("That creature moved. Try again."));
	}

	@Test
	public void releaseStillWorksWhenThePickMatches()
	{
		game.data().team.add(new Creature("goblin", 1, 0));
		game.data().storage.add(new Creature("goblin", 5, 0));

		game.release(storagePick(0));

		assertTrue(game.data().storage.isEmpty());
	}

	@Test
	public void secondMoveToTeamClickDoesNotMoveADifferentCreature()
	{
		game.data().storage.add(new Creature("goblin", 5, 0));
		game.data().storage.add(new Creature("wolf", 6, 0));
		TradePick pick = storagePick(0);

		game.moveToTeam(pick);
		game.moveToTeam(pick);

		assertEquals(1, game.data().team.size());
		assertEquals(1, game.data().storage.size());
	}

	@Test
	public void secondSetLeadClickDoesNotMakeAnotherCreatureLead()
	{
		game.data().team.add(new Creature("goblin", 1, 0));
		game.data().team.add(new Creature("wolf", 2, 0));
		game.data().team.add(new Creature("goblin", 3, 0));
		TradePick pick = teamPick(2);

		game.setLead(pick);
		game.setLead(pick);

		List<Creature> team = game.data().team;
		assertEquals(3, team.get(0).level());
		assertEquals(1, team.get(1).level());
	}

	@Test
	public void moveToStorageRefusesAMovedPick()
	{
		game.data().team.add(new Creature("goblin", 1, 0));
		game.data().team.add(new Creature("wolf", 2, 0));
		TradePick pick = teamPick(1);
		game.setLead(1);

		game.moveToStorage(pick);

		assertEquals(2, game.data().team.size());
		assertTrue(game.data().storage.isEmpty());
	}
}
