/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks that the game's snapshot carries the featured species of the day. */
public class TodaySnapshotTest
{
	private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);

	@Test
	public void theSnapshotCarriesTheFeaturedSpecies()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Rat", 1, CombatType.STAB),
			new Species("Goblin", 13, CombatType.STAB), new Species("Dragon", 300, CombatType.STAB)));
		GoGame game = new GoGame(table, new Random(1), () -> 0L, notice ->
		{
		});
		SpawnClock clock = new SpawnClock()
		{
			@Override
			public LocalDate utcDate()
			{
				return SATURDAY;
			}

			@Override
			public LocalTime utcTime()
			{
				return LocalTime.of(18, 48);
			}

			@Override
			public LocalTime localTime()
			{
				return LocalTime.of(21, 0);
			}
		};
		SpawnCalendar calendar = new SpawnCalendar(table);
		game.today(() -> calendar.today(true, clock));
		PanelState state = game.snapshot();

		assertEquals(2, state.today().featured().size());
		assertTrue(state.today().isFeatured("Dragon"));
		assertTrue(state.today().isFeatured("Rat") || state.today().isFeatured("Goblin"));
	}
}
