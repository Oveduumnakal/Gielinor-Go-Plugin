/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Plays the seen-and-caught record of a wild creature's species through the game service. */
public class GoGameRecordTest
{
	private static final Species IMP = new Species("Imp", 7, CombatType.STAB);
	private static final Species LESSER = new Species("Lesser demon", 94, CombatType.STAB);
	private static final Species GREATER = new Species("Greater demon", 113, CombatType.STAB);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(IMP, LESSER, GREATER, CHICKEN));
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(
			new EvolutionTable.Step("Imp", "Lesser demon", 20),
			new EvolutionTable.Step("Lesser demon", "Greater demon", 40)), table);
		ChampionTable champions = new ChampionTable(Arrays.asList(), table);
		game = new GoGame(table, evolutions, champions, new Random(5), () -> now, notice ->
		{
		});
	}

	private WildEntry meet(Species species, int wildId)
	{
		WildEntry entry = new WildEntry(wildId, species, 2, 1);
		return entry.withFirstMeeting(game.markSeen(entry));
	}

	private void catchIt(WildEntry entry)
	{
		game.data().addJars(JarType.BRONZE, 100);
		CatchOutcome outcome = CatchOutcome.BROKE_FREE;
		for (int i = 0; i < 100 && outcome != CatchOutcome.CAUGHT; i++)
			outcome = game.tryCatch(entry);

		assertEquals(CatchOutcome.CAUGHT, outcome);
	}

	@Test
	public void markSeenSaysTrueOnlyForTheFirstOfASpecies()
	{
		assertTrue(game.markSeen(new WildEntry(1, CHICKEN, 2, 1)));
		assertFalse(game.markSeen(new WildEntry(2, CHICKEN, 2, 1)));
		assertTrue(game.markSeen(new WildEntry(3, IMP, 2, 1)));
	}

	@Test
	public void theFirstCreatureOfASpeciesIsNewAndTheSecondIsSeen()
	{
		WildEntry first = meet(CHICKEN, 1);
		WildEntry second = meet(CHICKEN, 2);

		assertEquals(WildRecord.Kind.NEW, game.recordOf(first).kind());
		assertEquals(WildRecord.Kind.SEEN, game.recordOf(second).kind());
	}

	@Test
	public void aCatchTurnsEveryCreatureOfTheSpeciesToCaught()
	{
		WildEntry first = meet(CHICKEN, 1);
		WildEntry second = meet(CHICKEN, 2);

		catchIt(first);

		WildRecord one = game.recordOf(second);
		assertEquals(WildRecord.Kind.CAUGHT, one.kind());
		assertEquals(1, one.caughtCount());
		assertEquals(WildRecord.Kind.CAUGHT, game.recordOf(first).kind());
	}

	@Test
	public void aSecondCatchRaisesTheCount()
	{
		WildEntry first = meet(CHICKEN, 1);
		WildEntry second = meet(CHICKEN, 2);
		catchIt(first);

		catchIt(second);

		assertEquals(2, game.recordOf(second).caughtCount());
	}

	@Test
	public void aSpeciesFromAnEvolutionReadsCaught()
	{
		game.data().team.add(new Creature("imp", 19, now));

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(20) - XpRules.xpForLevel(19)));

		WildRecord record = game.recordOf(new WildEntry(1, LESSER, 2, 1));
		assertEquals(WildRecord.Kind.CAUGHT, record.kind());
		assertEquals(1, record.caughtCount());
	}

	@Test
	public void aSpeciesFromAHatchReadsCaught()
	{
		game.data().caught.put("chicken", 1);

		assertEquals(WildRecord.Kind.CAUGHT, game.recordOf(new WildEntry(1, CHICKEN, 2, 1)).kind());
	}

	@Test
	public void markSeenStillClearsTheLogAndSetsDirtyOnlyTheFirstTime()
	{
		List<LogEntry> first = game.snapshot().log();
		assertSame(first, game.snapshot().log());
		game.takeDirty();

		game.markSeen(new WildEntry(1, CHICKEN, 2, 1));

		assertTrue(game.takeDirty());
		List<LogEntry> second = game.snapshot().log();
		assertNotSame(first, second);

		game.markSeen(new WildEntry(2, CHICKEN, 2, 1));

		assertFalse(game.takeDirty());
		assertSame(second, game.snapshot().log());
	}
}
