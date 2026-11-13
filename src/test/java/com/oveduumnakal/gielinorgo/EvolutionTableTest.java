/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the bundled evolution data and the way a step's condition picks a result. */
public class EvolutionTableTest
{
	private static final Type LIST_TYPE = new TypeToken<List<EvolutionTable.Step>>()
	{
	}.getType();
	private static final Conditions DAY_SURFACE = Conditions.of(false, false);
	private static final Conditions NIGHT_SURFACE = Conditions.of(true, false);
	private static final Conditions DAY_CAVE = Conditions.of(false, true);

	private final Gson gson = new Gson();

	private SpeciesTable species() throws IOException
	{
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			return SpeciesTable.load(gson, reader);
		}
	}

	private List<EvolutionTable.Step> raw() throws IOException
	{
		try (Reader reader = open(EvolutionTable.RESOURCE))
		{
			return gson.fromJson(reader, LIST_TYPE);
		}
	}

	private static Reader open(String resource)
	{
		return new InputStreamReader(EvolutionTable.class.getResourceAsStream(resource), StandardCharsets.UTF_8);
	}

	@Test
	public void theConditionalStepsArePinned() throws IOException
	{
		Set<String> found = new TreeSet<>();
		for (EvolutionTable.Step step : raw())
		{
			if (step.when() != null)
				found.add(step.from() + ">" + step.to() + ":" + step.when());
		}

		Set<String> expected = new TreeSet<>(Arrays.asList(
			"Banshee>Twisted Banshee:NIGHT", "Bloodveld>Mutated Bloodveld:UNDERGROUND",
			"Cave goblin>Cave goblin guard:UNDERGROUND", "Crypt rat>Giant crypt rat:UNDERGROUND",
			"Crypt spider>Giant crypt spider:UNDERGROUND", "Ghost>Ghast:NIGHT", "Giant bat>Dire bat:UNDERGROUND",
			"Jelly>Warped Jelly:UNDERGROUND", "Pygmy kraken>Cave kraken:UNDERGROUND", "Rat>Crypt rat:UNDERGROUND",
			"Rat>Giant rat:SURFACE", "Rockslug>Giant rockslug:UNDERGROUND", "Skeleton>Skeleton thug:NIGHT",
			"Skeleton thug>Skeleton hero:NIGHT", "Vampyre Juvinate>Vyrewatch:NIGHT", "Wolf>Dire Wolf:DAY",
			"Wolf>Werewolf:NIGHT", "Zombie>Armoured zombie:NIGHT"));
		assertEquals(expected, found);
	}

	@Test
	public void theNumberOfPlainStepsIsTheOriginalOnesLessTheConditionalOnes() throws IOException
	{
		int plain = 0;
		for (EvolutionTable.Step step : raw())
		{
			if (step.when() == null)
				plain++;
		}

		assertEquals(149, plain);
		assertEquals(167, raw().size());
	}

	@Test
	public void everyConditionalStepNamesRealSpeciesAndSurvivesLoading() throws IOException
	{
		SpeciesTable species = species();
		EvolutionTable table = new EvolutionTable(raw(), species);
		for (EvolutionTable.Step step : raw())
		{
			if (step.when() == null)
				continue;

			assertNotNull(step.from(), species.find(step.from()));
			assertNotNull(step.to(), species.find(step.to()));
			assertTrue(step.to(), table.steps(step.from()).stream().anyMatch(s -> s.to().equals(step.to())));
		}
	}

	@Test
	public void everyBranchingSpeciesCoversEveryCase() throws IOException
	{
		EvolutionTable table = new EvolutionTable(raw(), species());
		for (EvolutionTable.Step first : raw())
		{
			List<EvolutionTable.Step> all = table.steps(first.from());
			if (all.size() < 2)
				continue;

			for (EvolutionTable.Step step : all)
			{
				assertNotNull(step.from() + " has an unconditional step beside others", step.when());
				assertTrue(step.from() + " lacks " + step.when().opposite(),
					all.stream().anyMatch(s -> s.when() == step.when().opposite()));
			}

			for (Conditions world : Arrays.asList(DAY_SURFACE, NIGHT_SURFACE, DAY_CAVE, Conditions.of(true, true)))
				assertNotNull(step(all, first) + " stuck", table.next(first.from(), world));
		}
	}

	private static String step(List<EvolutionTable.Step> all, EvolutionTable.Step first)
	{
		return first.from() + " (" + all.size() + " steps)";
	}

	@Test
	public void noLineEverLoopsBack() throws IOException
	{
		EvolutionTable table = new EvolutionTable(raw(), species());
		for (EvolutionTable.Step start : raw())
			assertFalse(start.from(), reaches(table, start.from(), start.from(), new HashSet<>()));
	}

	private static boolean reaches(EvolutionTable table, String from, String target, Set<String> seen)
	{
		for (EvolutionTable.Step step : table.steps(from))
		{
			if (Species.keyFor(step.to()).equals(Species.keyFor(target)))
				return true;

			if (seen.add(Species.keyFor(step.to())) && reaches(table, step.to(), target, seen))
				return true;
		}

		return false;
	}

	@Test
	public void aBranchTakesTheSideWhoseConditionHolds() throws IOException
	{
		EvolutionTable table = new EvolutionTable(raw(), species());

		assertEquals("Giant rat", table.next("rat", DAY_SURFACE).to());
		assertEquals("Crypt rat", table.next("rat", DAY_CAVE).to());
		assertEquals("Dire Wolf", table.next("wolf", DAY_SURFACE).to());
		assertEquals("Werewolf", table.next("wolf", NIGHT_SURFACE).to());
	}

	@Test
	public void aSingleConditionalStepWaitsAndSaysWhatFor() throws IOException
	{
		EvolutionTable table = new EvolutionTable(raw(), species());

		assertNull(table.next("zombie", DAY_SURFACE));
		assertEquals("Armoured zombie", table.next("zombie", NIGHT_SURFACE).to());
		assertEquals(EvolutionCondition.NIGHT, table.waitingFor("zombie", 40, DAY_SURFACE));
		assertNull(table.waitingFor("zombie", 39, DAY_SURFACE));
		assertNull(table.waitingFor("zombie", 40, NIGHT_SURFACE));
		assertNull(table.waitingFor("rat", 15, DAY_SURFACE));
		assertEquals("Evolves underground", EvolutionCondition.UNDERGROUND.waitingText());
	}

	@Test
	public void aPlainLineIsUntouched() throws IOException
	{
		EvolutionTable table = new EvolutionTable(raw(), species());
		List<EvolutionTable.Step> out = new ArrayList<>(table.steps("imp"));

		assertEquals(1, out.size());
		assertNull(out.get(0).when());
		assertEquals("Lesser demon", table.next("imp", NIGHT_SURFACE).to());
		assertEquals("Lesser demon", table.next("imp", DAY_CAVE).to());
	}
}
