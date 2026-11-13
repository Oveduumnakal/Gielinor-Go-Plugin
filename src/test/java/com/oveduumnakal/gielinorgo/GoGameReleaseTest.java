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

/** Checks what releasing stored creatures pays the lead. */
public class GoGameReleaseTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);

	private final List<String> messages = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, DRAGON));
		game = new GoGame(table, new Random(3), () -> 1_000_000L, notice -> messages.add(notice.text()));
	}

	@Test
	public void aReleasePaysTheLeadByTheRule()
	{
		game.data().team.add(new Creature("goblin", 1, 0));
		game.data().storage.add(new Creature("goblin", 10, 0));

		game.release(0);

		assertTrue(game.data().storage.isEmpty());
		assertEquals(ReleaseRules.xp(10, Rarity.COMMON), game.data().team.get(0).xp
			- XpRules.xpForLevel(1));
		assertTrue(messages.contains("Goblin +60 XP (released Goblin)"));
	}

	@Test
	public void aBigReleaseCanLevelTheLead()
	{
		Creature lead = new Creature("goblin", 1, 0);
		game.data().team.add(lead);
		for (int i = 0; i < 5; i++)
			game.data().storage.add(new Creature("king black dragon", 90, 0));

		game.release(Arrays.asList(0, 1, 2, 3, 4));

		assertTrue(lead.level() > 1);
		assertTrue(messages.contains("Goblin +" + 5 * ReleaseRules.xp(90, Rarity.EPIC)
			+ " XP (released 5 creatures)"));
	}

	@Test
	public void aTradeLockedCreatureIsSkippedAndTheRestGo()
	{
		game.data().team.add(new Creature("goblin", 1, 0));
		Creature locked = new Creature("goblin", 5, 0);
		game.data().storage.add(new Creature("goblin", 6, 0));
		game.data().storage.add(locked);
		game.data().storage.add(new Creature("goblin", 7, 0));
		game.tradeLocks(new GoGame.TradeLocks()
		{
			@Override
			public boolean locked(Creature creature)
			{
				return creature == locked;
			}
		});

		game.release(Arrays.asList(2, 0, 1));

		assertEquals(Arrays.asList(locked), game.data().storage);
		assertTrue(messages.contains("Goblin is promised in a trade"));
	}

	@Test
	public void withNoLeadAReleasePaysNothing()
	{
		game.data().storage.add(new Creature("goblin", 10, 0));

		game.release(0);

		assertTrue(game.data().storage.isEmpty());
		assertTrue(messages.contains("Released Goblin"));
	}

	@Test
	public void indexesAreRemovedCorrectlyInAnyOrderWithDuplicatesAndBadPlaces()
	{
		game.data().team.add(new Creature("goblin", 1, 0));
		Creature a = new Creature("goblin", 2, 0);
		Creature b = new Creature("goblin", 3, 0);
		Creature c = new Creature("goblin", 4, 0);
		Creature d = new Creature("goblin", 5, 0);
		game.data().storage.addAll(Arrays.asList(a, b, c, d));

		game.release(Arrays.asList(1, 3, 1, 9, -1));

		assertEquals(Arrays.asList(a, c), game.data().storage);
	}
}
