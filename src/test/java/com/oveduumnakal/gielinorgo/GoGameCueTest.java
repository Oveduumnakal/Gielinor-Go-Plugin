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

/** Checks which sound cue each game event fires. */
public class GoGameCueTest
{
	private static final Species IMP = new Species("Imp", 7, CombatType.STAB);
	private static final Species LESSER = new Species("Lesser demon", 94, CombatType.STAB);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	private final List<Cue> cues = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(IMP, LESSER, CHICKEN));
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(
			new EvolutionTable.Step("Imp", "Lesser demon", 20)), table);
		game = new GoGame(table, evolutions, ChampionTable.empty(), new Random(5), () -> 1_000_000L, notice ->
		{
		});
		game.setCues(cues::add);
	}

	@Test
	public void aCatchFiresCatchAndABrokenJarFiresBrokeFree()
	{
		game.data().addJars(JarType.BRONZE, 500);
		int caught = 0;
		int broke = 0;
		for (int i = 0; i < 100; i++)
		{
			cues.clear();
			CatchOutcome outcome = game.tryCatch(new WildEntry(1, CHICKEN, 3, 1));
			assertEquals(1, cues.size());
			if (outcome == CatchOutcome.CAUGHT)
			{
				assertEquals(Cue.CATCH, cues.get(0));
				caught++;
			}
			else
			{
				assertEquals(Cue.BROKE_FREE, cues.get(0));
				broke++;
			}
		}

		assertTrue(caught > 0 && broke > 0);
	}

	@Test
	public void aLevelUpFiresLevelUpOnceEvenWhenSeveralCreaturesGrow()
	{
		game.data().team.add(new Creature("chicken", 1, 0));
		game.data().team.add(new Creature("chicken", 1, 0));

		game.addOsrsXp(10_000);

		assertEquals(Arrays.asList(Cue.LEVEL_UP), cues);
	}

	@Test
	public void anEvolutionFiresEvolveAndNotAlsoLevelUp()
	{
		game.data().team.add(new Creature("imp", 19, 0));

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(20) - XpRules.xpForLevel(19)));

		assertEquals(Arrays.asList(Cue.EVOLVE), cues);
	}
}
