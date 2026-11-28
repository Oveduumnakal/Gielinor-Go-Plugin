/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Plays a battle and a catch against a Hunter creature of the bundled species list (issue #207). */
public class HunterCreatureGameTest
{
	private final List<Notice> notices = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;
	private SpeciesTable table;

	@Before
	public void setUp() throws IOException
	{
		try (InputStream in = SpeciesTable.class.getResourceAsStream(SpeciesTable.RESOURCE))
		{
			assertNotNull(in);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				table = SpeciesTable.load(new Gson(), reader);
			}
		}

		game = new GoGame(table, new Random(3), () -> now, notices::add);
		game.setBankNear(true);
	}

	@Test
	public void aWildPyreFoxCanBeBattledToAResult()
	{
		game.data().team.add(new Creature("goblin", 30, now));
		Species fox = table.find("Pyre fox");
		WildEntry entry = new WildEntry(1, fox, 23, WildEligibility.combatLevel(fox, 0));

		BattleResult result = game.battle(entry);

		assertNotNull(result);
		assertTrue(entry.battled());
	}

	@Test
	public void aWildPyreFoxCanBeCaughtAndCountsInTheLog()
	{
		Species fox = table.find("Pyre fox");
		WildEntry entry = new WildEntry(1, fox, 23, WildEligibility.combatLevel(fox, 0));
		game.data().caught.put("goblin", 1);
		game.markSeen(entry);

		CatchOutcome outcome = game.tryCatch(entry);
		for (int i = 0; i < 100 && outcome != CatchOutcome.CAUGHT; i++)
			outcome = game.tryCatch(entry);

		assertEquals(CatchOutcome.CAUGHT, outcome);
		assertEquals(2, game.snapshot().caughtSpecies());
		Creature caught = game.data().team.get(0);
		assertEquals("pyre fox", caught.species);
	}
}
