/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Draws the Log tab's Today section offline and checks the featured filter. */
public class LogTabTest
{
	private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);

	@Test
	public void theTodaySectionAndTheFeaturedFilterWork()
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

		LogTab tab = new LogTab();
		tab.setSize(new Dimension(225, 600));
		tab.update(state);
		tab.doLayout();
		BufferedImage image = new BufferedImage(225, 600, BufferedImage.TYPE_INT_ARGB);
		tab.paint(image.getGraphics());

		assertEquals(2, state.today().featured().size());
		assertTrue(state.today().isFeatured("Dragon"));
		assertTrue(state.today().isFeatured("Rat") || state.today().isFeatured("Goblin"));
	}
}
