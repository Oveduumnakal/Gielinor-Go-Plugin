/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the list of owned jars the Catch menu reads. */
public class GoGameJarsTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Collections.singletonList(CHICKEN));
		game = new GoGame(table, new EvolutionTable(new ArrayList<>(), table),
			new ChampionTable(new ArrayList<>(), table), new Random(5), () -> 1_000_000L, notice ->
			{
			});
	}

	@Test
	public void aFreshGameOwnsOnlyBronze()
	{
		Map<JarType, Integer> owned = game.ownedJars();

		assertEquals(Collections.singletonList(JarType.BRONZE), new ArrayList<>(owned.keySet()));
		assertEquals(SaveData.STARTING_JARS, (int) owned.get(JarType.BRONZE));
	}

	@Test
	public void jarsComeInEnumOrder()
	{
		game.data().addJars(JarType.SLASH, 2);
		game.data().addJars(JarType.IRON, 4);

		assertEquals(Arrays.asList(JarType.BRONZE, JarType.IRON, JarType.SLASH),
			new ArrayList<>(game.ownedJars().keySet()));
		assertEquals(4, (int) game.ownedJars().get(JarType.IRON));
	}

	@Test
	public void aThrownJarLowersTheCountAndAZeroCountDisappears()
	{
		List<JarType> before = new ArrayList<>(game.ownedJars().keySet());
		game.data().addJars(JarType.IRON, 1);

		game.tryCatch(new WildEntry(1, CHICKEN, 3, 1), JarType.IRON);

		assertFalse(game.ownedJars().containsKey(JarType.IRON));
		Map<JarType, Integer> after = game.ownedJars();

		assertTrue(after.keySet().containsAll(before));
	}
}
