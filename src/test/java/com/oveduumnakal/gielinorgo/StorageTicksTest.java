/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks which Select mode ticks survive a refresh of the Storage tab. */
public class StorageTicksTest
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
		for (int level = 3; level < 6; level++)
		{
			Creature stored = new Creature("goblin", level, now);
			stored.hpFraction = 0.2;
			game.data().storage.add(stored);
		}
	}

	@Test
	public void ticksStayWhileACreatureRestsAndItsHealthRises()
	{
		List<CreatureView> before = game.snapshot().storage();
		now += 10_000L;
		List<CreatureView> after = game.snapshot().storage();
		Set<Integer> ticked = new TreeSet<>(Arrays.asList(0, 2));

		assertEquals(ticked, StorageTicks.kept(before, after, ticked));
	}

	@Test
	public void aTickGoesWhenItsCreatureIsGone()
	{
		List<CreatureView> before = game.snapshot().storage();
		game.data().storage.remove(0);
		List<CreatureView> after = game.snapshot().storage();
		Set<Integer> ticked = new TreeSet<>(Arrays.asList(0, 1, 2));

		assertEquals(0, StorageTicks.kept(before, after, ticked).size());
	}
}
