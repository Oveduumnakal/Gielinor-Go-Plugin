/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** The window's view of a creature carries the fainted flag. */
public class CreatureViewFaintedTest
{
	@Test
	public void theFlagIsPartOfEqualityButNotOfSameCreature()
	{
		CreatureView up = PartyRig.creature("Goblin", 9, 0.0, null);
		CreatureView down = up.withFainted(true);

		assertNotEquals(up, down);
		assertNotEquals(up.hashCode(), down.hashCode());
		assertTrue(up.sameCreature(down));
		assertEquals(down, up.withFainted(true));
	}

	@Test
	public void theGameShowsAFaintedCreatureAsFaintedInTheTeamAndInStorage()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB)));
		GoGame game = new GoGame(table, new Random(1), () -> 5_000L, notice ->
		{
		});
		Creature down = new Creature("chicken", 3, 1_000L);
		down.faint(2_000L);
		game.data().team.add(down);
		game.data().storage.add(new Creature("chicken", 3, 1_000L));
		game.data().storage.get(0).faint(2_000L);
		game.data().team.add(new Creature("chicken", 3, 1_000L));

		PanelState state = game.snapshot();

		assertTrue(state.team().get(0).fainted());
		assertFalse(state.team().get(1).fainted());
		assertTrue(state.storage().get(0).fainted());
		assertEquals(0.0, state.team().get(0).hpFraction(), 1e-9);
		assertEquals(RowModel.State.FAINTED, RowModel.of(state.team().get(0), 0).state());
	}
}
