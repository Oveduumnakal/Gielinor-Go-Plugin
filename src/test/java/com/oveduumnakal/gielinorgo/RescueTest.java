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

/** The passing healer: when everything the player owns has fainted, the first creature gets up after ten minutes. */
public class RescueTest
{
	private static final long MINUTE = 60_000L;

	private final List<String> messages = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;
	private Creature first;
	private Creature second;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB)));
		game = new GoGame(table, new Random(3), () -> now, notice -> messages.add(notice.text()));
		first = new Creature("chicken", 5, now);
		second = new Creature("chicken", 6, now);
		game.data().team.add(first);
		game.data().team.add(second);
		first.faint(now);
		second.faint(now + MINUTE);
	}

	@Test
	public void nineMinutesAfterTheNewestFaintNothingHappens()
	{
		now += MINUTE + 9 * MINUTE;

		assertFalse(game.rescue());
		assertTrue(first.fainted);
	}

	@Test
	public void tenMinutesAfterTheNewestFaintTheFirstTeamCreatureGetsUpAtAQuarter()
	{
		now += MINUTE + 10 * MINUTE;

		assertTrue(game.rescue());

		assertFalse(first.fainted);
		assertEquals(0.25, first.hpFractionAt(now), 1e-9);
		assertTrue(second.fainted);
		assertTrue(messages.toString(), messages.contains("A passing healer got Chicken back on its feet"));
	}

	@Test
	public void itDoesNotFireTwiceForTheSameFaint()
	{
		now += 12 * MINUTE;
		game.rescue();

		assertFalse(game.rescue());
		assertTrue(second.fainted);
	}

	@Test
	public void aLivingCreatureInStorageMeansNoRescue()
	{
		Creature alive = new Creature("chicken", 2, now);
		game.data().storage.add(alive);
		now += 60 * MINUTE;

		assertFalse(game.rescue());
		assertTrue(first.fainted);
	}

	@Test
	public void aLivingCreatureInTheTeamMeansNoRescue()
	{
		second.revive(1.0, now);
		now += 60 * MINUTE;

		assertFalse(game.rescue());
	}

	@Test
	public void withOnlyStoredCreaturesTheFirstStoredOneIsPicked()
	{
		game.data().team.clear();
		game.data().storage.add(first);
		game.data().storage.add(second);
		now += 60 * MINUTE;

		assertTrue(game.rescue());

		assertFalse(first.fainted);
		assertTrue(second.fainted);
	}

	@Test
	public void anOwnerOfNothingHasNothingToRescue()
	{
		game.data().team.clear();
		now += 60 * MINUTE;

		assertFalse(game.rescue());
	}
}
