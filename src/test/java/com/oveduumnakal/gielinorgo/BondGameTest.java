/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Plays the bond rules through the game service: who earns bond, and what the hearts give. */
public class BondGameTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, DRAGON));
		Champion strong = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250,
			Arrays.asList(new Champion.Member("Chicken", 90), new Champion.Member("Chicken", 90),
				new Champion.Member("Chicken", 90)), JarType.IRON, 3);
		game = new GoGame(table, EvolutionTable.empty(), new ChampionTable(Arrays.asList(strong), table),
			new Random(5), () -> now, notice -> messages.add(notice.text()));
		game.data().team.add(new Creature("king black dragon", 99, now));
		game.data().team.add(new Creature("king black dragon", 99, now));
	}

	@Test
	public void onlyTheLeadEarnsBondAndTheRestOfAPointIsKept()
	{
		game.addSteps(250);

		assertEquals(2, member(0).bond);
		assertEquals(0, member(1).bond);
		game.addSteps(50);
		assertEquals(3, member(0).bond);
	}

	@Test
	public void aWonBattleGivesTheLeadFivePoints()
	{
		BattleResult result = game.battle(new WildEntry(1, CHICKEN, 2, 1));

		assertNotNull(result);
		assertTrue(result.won());
		assertEquals(BondRules.WIN_POINTS, member(0).bond);
		assertEquals(0, member(1).bond);
	}

	@Test
	public void aChampionWinGivesTheLeadTwentyPointsAndAHeartLineWhenOneIsEarned()
	{
		member(0).bond = 90;
		game.moveTo(3222, 3218, 0);

		assertTrue(game.challenge("lumbridge").won());

		assertEquals(110, member(0).bond);
		assertTrue(messages.contains("King Black Dragon's bond grew"));
	}

	@Test
	public void threeHeartsEarnTwoPercentMoreXpThanANeighbour()
	{
		member(1).xp = XpRules.xpForLevel(50);
		long plain = shareGain(1);

		setUp();
		member(1).xp = XpRules.xpForLevel(50);
		member(1).bond = 1200;
		long bonded = shareGain(1);

		assertTrue(plain > 100);
		assertEquals(plain + plain * 2 / 100, bonded);
	}

	@Test
	public void anOldSaveLoadsWithNoBondAndANegativeOneIsCleaned()
	{
		SaveData loaded = new Gson().fromJson("{\"team\":[{\"species\":\"chicken\",\"xp\":0},"
			+ "{\"species\":\"chicken\",\"bond\":-4}]}", SaveData.class);
		game.load(loaded);

		assertEquals(0, member(0).bond);
		assertEquals(0, member(1).bond);
	}

	private Creature member(int index)
	{
		return game.data().team.get(index);
	}

	private long shareGain(int index)
	{
		long before = member(index).xp;
		game.moveTo(3222, 3218, 0);
		assertTrue(game.challenge("lumbridge").won());
		return member(index).xp - before;
	}
}
