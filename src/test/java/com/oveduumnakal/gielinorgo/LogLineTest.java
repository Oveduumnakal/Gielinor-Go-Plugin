/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Checks the evolution line, examine text and record a Creature Log entry carries. */
public class LogLineTest
{
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(new Species("Imp", 7, CombatType.MAGIC),
			new Species("Lesser demon", 82, CombatType.MELEE), new Species("Greater demon", 92, CombatType.MELEE),
			new Species("Rat", 1, CombatType.MELEE)));
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(
			new EvolutionTable.Step("Imp", "Lesser demon", 10),
			new EvolutionTable.Step("Lesser demon", "Greater demon", 20)), species);
		game = new GoGame(species, evolutions, ChampionTable.empty(), new Random(1), () -> 0L, notice ->
		{
		});
		game.examine(new ExamineTable(Collections.singletonMap("imp", "Annoying little pest.")));
	}

	private LogEntry entry(String name)
	{
		return game.snapshot().log().stream().filter(e -> e.name().equals(name)).findFirst().get();
	}

	@Test
	public void unseenStagesShowAsQuestionMarks()
	{
		game.data().caught.put("lesser demon", 1);

		assertEquals("??? → Lesser demon → ???", entry("Lesser demon").lineText());
		assertEquals("??? → Lesser demon → ???", entry("Imp").lineText());
	}

	@Test
	public void aStageShowsItsNameOnceSeenAndTheMiddleShowsBothWays()
	{
		game.data().caught.put("lesser demon", 2);
		game.data().seen.add("imp");
		game.data().seen.add("greater demon");

		assertEquals("Imp → Lesser demon → Greater demon", entry("Lesser demon").lineText());
		assertEquals("Imp → Lesser demon → Greater demon", entry("Greater demon").lineText());
	}

	@Test
	public void aSpeciesOutsideAnyLineHasNoLine()
	{
		assertEquals("", entry("Rat").lineText());
		assertNull(entry("Rat").examine());
	}

	@Test
	public void theEntryCarriesTheExamineTextAndRecord()
	{
		game.data().caught.put("imp", 4);
		game.data().gilded.add("imp");
		game.data().seen.add("rat");

		assertEquals("Annoying little pest.", entry("Imp").examine());
		assertEquals("Caught 4 · gilded owned", entry("Imp").recordText());
		assertEquals("Seen, not caught yet", entry("Rat").recordText());
	}

	@Test
	public void aConditionAndABranchShowBesideKnownStages()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(new Species("Wolf", 64, CombatType.MELEE),
			new Species("Dire Wolf", 88, CombatType.MELEE), new Species("Werewolf", 93, CombatType.MELEE)));
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(
			new EvolutionTable.Step("Wolf", "Dire Wolf", 55, EvolutionCondition.DAY),
			new EvolutionTable.Step("Wolf", "Werewolf", 55, EvolutionCondition.NIGHT)), species);
		GoGame wolves = new GoGame(species, evolutions, ChampionTable.empty(), new Random(1), () -> 0L, notice ->
		{
		});
		wolves.data().seen.add("wolf");
		wolves.data().seen.add("werewolf");
		LogEntry wolf = wolves.snapshot().log().stream().filter(e -> e.name().equals("Wolf")).findFirst().get();

		assertEquals("Wolf → ??? / Werewolf (at night)", wolf.lineText());
	}
}
