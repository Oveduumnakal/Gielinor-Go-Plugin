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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks that a save keeping a species the game no longer has loads without it, silently (issue #353). */
public class RemovedSpeciesLoadTest
{
	private static final long NOW = 1_000_000L;

	private int notices;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB),
			new Species("Goblin", 13, CombatType.STAB)));
		game = new GoGame(table, new Random(3), () -> NOW, notice -> notices++);
	}

	@Test
	public void removedSpeciesAreDroppedFromTheTeamAndStorageWithoutANotice()
	{
		SaveData save = SaveData.fresh();
		save.team.add(new Creature("Whale", 30, NOW));
		save.team.add(new Creature("goblin", 5, NOW));
		save.storage.add(new Creature("Bloodthirsty banshee", 40, NOW));
		save.storage.add(new Creature("chicken", 2, NOW));

		game.load(save);

		SaveData loaded = game.data();
		PanelState state = game.snapshot();
		assertEquals(1, loaded.team.size());
		assertEquals("goblin", loaded.lead().species);
		assertEquals(1, loaded.storage.size());
		Creature stored = loaded.storage.get(0);
		assertEquals("chicken", stored.species);
		assertEquals(0, notices);
		assertEquals(1, state.team().size());
	}

	@Test
	public void aTeamWhoseOnlyCreatureWasRemovedLoadsLikeANewPlayersTeam()
	{
		SaveData save = SaveData.fresh();
		save.team.add(new Creature("Whale", 30, NOW));
		save.caught.put("whale", 2);
		save.seen.add("whale");

		game.load(save);

		assertTrue(game.data().team.isEmpty());
		assertNull(game.followerCreature());
		assertNull(game.data().lead());
		PanelState state = game.snapshot();
		assertTrue(state.team().isEmpty());
		assertTrue(StarterRules.offered(game.data()));
		assertEquals(0, notices);
	}

	@Test
	public void caughtAndSeenCountsIgnoreUnknownSpeciesAndPaidMilestonesStay()
	{
		SaveData save = SaveData.fresh();
		save.caught.put("whale", 3);
		save.caught.put("bloodthirsty banshee", 1);
		save.caught.put("chicken", 1);
		save.seen.add("whale");
		save.seen.add("chicken");
		save.gilded.add("whale");
		save.milestones.add("species-25");

		game.load(save);

		assertEquals(1, game.snapshot().caughtSpecies());
		assertEquals(1, game.snapshot().seenSpecies());
		assertEquals(1, game.data().caught.size());
		assertTrue(game.data().gilded.isEmpty());
		assertTrue(game.data().milestones.contains("species-25"));
	}
}
