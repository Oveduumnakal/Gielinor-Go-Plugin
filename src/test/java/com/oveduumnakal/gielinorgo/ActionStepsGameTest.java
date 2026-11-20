/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks that action steps hatch eggs and nothing else (#323): no jars, no walk tasks, no bond. */
public class ActionStepsGameTest
{
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		Species goblin = new Species("Goblin", 13, CombatType.STAB);
		game = new GoGame(new SpeciesTable(Arrays.asList(goblin)), new Random(3), () -> now, notice ->
		{
		});
		game.data().team.add(new Creature("goblin", 5, now));
	}

	private Egg carry(Rarity tier)
	{
		Egg egg = new Egg(tier);
		game.data().eggs.add(egg);
		return egg;
	}

	@Test
	public void noEggMeansNothingToCount()
	{
		assertFalse(game.carriesEgg());

		game.addActionSteps(50);

		assertEquals(0, game.data().stepProgress);
	}

	@Test
	public void actionStepsWearDownEveryCarriedEgg()
	{
		Egg first = carry(Rarity.COMMON);
		Egg second = carry(Rarity.RARE);

		game.addActionSteps(10);

		assertTrue(game.carriesEgg());
		assertEquals(Rarity.COMMON.eggTiles() - 10, first.tilesLeft);
		assertEquals(Rarity.RARE.eggTiles() - 10, second.tilesLeft);
	}

	@Test
	public void enoughActionStepsHatchTheEgg()
	{
		carry(Rarity.COMMON);
		int before = game.data().team.size();

		game.addActionSteps(Rarity.COMMON.eggTiles());

		assertFalse(game.carriesEgg());
		assertEquals(before + 1, game.data().team.size());
	}

	@Test
	public void actionStepsPayNoJarsAndKeepTheWalkToTheNextJar()
	{
		carry(Rarity.LEGENDARY);
		int jars = game.data().jarCount(JarType.BRONZE);
		game.addSteps(100);

		game.addActionSteps(GoGame.TILES_PER_JAR * 3);

		assertEquals(jars, game.data().jarCount(JarType.BRONZE));
		assertEquals(100, game.data().stepProgress);
	}

	@Test
	public void actionStepsGiveTheLeadNoBond()
	{
		carry(Rarity.LEGENDARY);

		game.addActionSteps(BondRules.TILES_PER_POINT * 5);

		Creature lead = game.data().team.get(0);
		assertEquals(0, lead.bond);
	}

	@Test
	public void actionStepsDoNotFinishAWalkTask()
	{
		carry(Rarity.LEGENDARY);

		game.addActionSteps(TaskKind.WALK.weeklyGoal() + TaskKind.WALK.dailyGoal());

		assertFalse(game.snapshot().tasks().rows().stream()
			.anyMatch(row -> row.done() && row.label().startsWith("Walk")));
	}
}
