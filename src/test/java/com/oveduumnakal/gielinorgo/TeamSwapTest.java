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

/** Checks that the team is only changed inside a city or near a bank, and the spots a bank is found by. */
public class TeamSwapTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, CHICKEN));
		Champion lumbridge = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250, Arrays.asList(
			new Champion.Member("Chicken", 2)), JarType.IRON, 3);
		game = new GoGame(table, EvolutionTable.empty(), new ChampionTable(Arrays.asList(lumbridge), table),
			new Random(1), () -> 1_000L, notice -> messages.add(notice.text()));
		game.data().team.add(new Creature("goblin", 5, 1_000L));
		game.data().team.add(new Creature("chicken", 2, 1_000L));
	}

	@Test
	public void theRuleAllowsACityOrABankAndNothingElse()
	{
		assertTrue(TeamSwapRule.allowed(true, false));
		assertTrue(TeamSwapRule.allowed(false, true));
		assertTrue(TeamSwapRule.allowed(true, true));
		assertFalse(TeamSwapRule.allowed(false, false));
	}

	@Test
	public void awayFromCitiesAndBanksNothingMovesAndTheLineSaysWhereToGo()
	{
		game.moveToStorage(0);
		assertEquals(2, game.data().team.size());
		assertTrue(game.data().storage.isEmpty());
		assertEquals("Go to a city or a bank to change your team", messages.get(messages.size() - 1));

		game.data().storage.add(new Creature("goblin", 3, 1_000L));
		game.data().team.remove(1);
		game.moveToTeam(0);
		assertEquals(1, game.data().team.size());
		assertEquals(1, game.data().storage.size());
	}

	@Test
	public void insideACityTheTeamCanBeChanged()
	{
		game.moveTo(3222, 3218, 0);

		game.moveToStorage(1);
		assertEquals(1, game.data().team.size());
		game.moveToTeam(0);
		assertEquals(2, game.data().team.size());
	}

	@Test
	public void leavingTheCityTakesTheAllowanceAway()
	{
		game.moveTo(3222, 3218, 0);
		game.moveTo(3100, 3100, 0);

		game.moveToStorage(1);

		assertEquals(2, game.data().team.size());
	}

	@Test
	public void nearABankTheTeamCanBeChanged()
	{
		game.setBankNear(true);
		game.moveToStorage(1);
		assertEquals(1, game.data().storage.size());

		game.setBankNear(false);
		game.moveToTeam(0);
		assertEquals("without the bank it is refused again", 1, game.data().storage.size());
	}

	@Test
	public void leadChangesAreNotRestricted()
	{
		game.setLead(1);

		Creature lead = game.data().team.get(0);

		assertEquals("chicken", lead.species);
	}

	@Test
	public void aFullTeamStillSaysItIsFullNearABank()
	{
		game.setBankNear(true);
		while (game.data().team.size() < SaveData.TEAM_SIZE)
			game.data().team.add(new Creature("goblin", 2, 1_000L));

		game.data().storage.add(new Creature("goblin", 2, 1_000L));
		game.moveToTeam(0);

		assertEquals("Team is full", messages.get(messages.size() - 1));
	}

	@Test
	public void aBankIsNearWithinTenTilesOnTheSamePlane()
	{
		BankSpots spots = new BankSpots();
		Object booth = new Object();
		spots.add(booth, 3000, 3000, 0);

		assertTrue(spots.near(3010, 3000, 0));
		assertTrue(spots.near(3010, 2990, 0));
		assertFalse(spots.near(3011, 3000, 0));
		assertFalse(spots.near(3000, 3000, 1));
	}

	@Test
	public void spotsComeAndGoWithTheirOwners()
	{
		BankSpots spots = new BankSpots();
		Object banker = new Object();
		Object chest = new Object();
		spots.add(banker, 100, 100, 0);
		spots.add(chest, 500, 500, 0);
		assertEquals(2, spots.size());

		spots.remove(banker);
		assertFalse(spots.near(100, 100, 0));
		assertTrue(spots.near(505, 505, 0));

		spots.forgetFar(100, 100);
		assertEquals("a spot far away is forgotten", 0, spots.size());

		spots.add(banker, 1, 1, 0);
		spots.clear();
		assertFalse(spots.near(1, 1, 0));
	}
}
