/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the bundled evolution lines and City Champions against the rules the game relies on. */
public class GameDataTest
{
	private static final Type STEPS = new TypeToken<List<EvolutionTable.Step>>()
	{
	}.getType();

	private static final Type CHAMPIONS = new TypeToken<List<Champion>>()
	{
	}.getType();

	private final Gson gson = new Gson();
	private SpeciesTable species;
	private List<EvolutionTable.Step> steps;
	private List<Champion> champions;

	@Before
	public void load() throws IOException
	{
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			species = SpeciesTable.load(gson, reader);
		}

		try (Reader reader = open(EvolutionTable.RESOURCE))
		{
			steps = gson.fromJson(reader, STEPS);
		}

		try (Reader reader = open(ChampionTable.RESOURCE))
		{
			champions = gson.fromJson(reader, CHAMPIONS);
		}
	}

	@Test
	public void everyEvolutionLinksTwoRealSpeciesAndGetsStronger()
	{
		assertTrue("expected at least 50 evolutions, got " + steps.size(), steps.size() >= 50);
		Set<String> froms = new HashSet<>();
		for (EvolutionTable.Step step : steps)
		{
			Species from = species.find(step.from());
			Species to = species.find(step.to());
			String link = step.from() + " to " + step.to();
			assertNotNull(link + ": unknown species " + step.from(), from);
			assertNotNull(link + ": unknown species " + step.to(), to);
			assertEquals(link + ": name must match the species list exactly", step.from(), from.name());
			assertEquals(link + ": name must match the species list exactly", step.to(), to.name());
			assertTrue(link + ": the next stage must be a stronger species", to.level() > from.level());
			assertTrue(link + ": level out of range", step.level() >= 10 && step.level() <= 85);
			assertTrue(step.from() + " evolves twice", froms.add(from.key()));
		}
	}

	@Test
	public void evolutionLinesAreShortAndTheirLevelsRise()
	{
		Map<String, EvolutionTable.Step> byFrom = new HashMap<>();
		for (EvolutionTable.Step step : steps)
			byFrom.put(Species.keyFor(step.from()), step);

		for (EvolutionTable.Step start : steps)
		{
			int stages = 1;
			int lastLevel = 0;
			EvolutionTable.Step step = start;
			while (step != null)
			{
				stages++;
				assertTrue(start.from() + ": line is longer than 4 stages or loops", stages <= 4);
				assertTrue(start.from() + ": levels must rise along the line", step.level() > lastLevel);
				lastLevel = step.level();
				step = byFrom.get(Species.keyFor(step.to()));
			}
		}
	}

	@Test
	public void theEvolutionTableLoadsEveryLink()
	{
		EvolutionTable table = new EvolutionTable(steps, species);

		assertEquals(steps.size(), table.size());
		assertEquals("Lesser demon", table.next("Imp").to());
		assertNull(table.next("Zulrah"));
	}

	@Test
	public void championsAreCompleteAndGetHarder()
	{
		assertEquals(8, champions.size());
		assertEquals("lumbridge", champions.get(0).id());
		Set<String> ids = new HashSet<>();
		int previousTop = 0;
		for (Champion champion : champions)
		{
			assertTrue(champion.id() + ": id must be lowercase letters and hyphens", champion.id().matches("[a-z-]+"));
			assertTrue(champion.id() + " is listed twice", ids.add(champion.id()));
			assertFalse(champion.city().isEmpty());
			assertEquals(champion.id() + ": team size", 3, champion.team().size());
			for (Champion.Member member : champion.team())
			{
				Species found = species.find(member.species());
				assertNotNull(champion.id() + ": unknown species " + member.species(), found);
				assertEquals(member.species(), found.name());
			}

			assertTrue(champion.id() + " must be harder than the city before it", champion.topLevel() > previousTop);
			previousTop = champion.topLevel();
			assertFalse(champion.rewardJar().specialty());
			assertTrue(champion.rewardJars() >= 2 && champion.rewardJars() <= 5);
		}

		assertEquals(champions.size(), new ChampionTable(champions, species).all().size());
	}

	@Test
	public void cityRectanglesDoNotOverlapAndHoldTheirLandmarks()
	{
		for (int i = 0; i < champions.size(); i++)
		{
			for (int j = i + 1; j < champions.size(); j++)
			{
				Champion first = champions.get(i);
				Champion second = champions.get(j);
				assertFalse(first.id() + " overlaps " + second.id(), first.overlaps(second));
			}
		}

		ChampionTable table = new ChampionTable(champions, species);
		assertEquals("lumbridge", table.at(3222, 3218, 0).id());
		assertEquals("varrock", table.at(3212, 3428, 0).id());
		assertEquals("falador", table.at(2965, 3380, 0).id());
		assertEquals("east-ardougne", table.at(2660, 3300, 0).id());
		assertNull("upstairs is not the city floor", table.at(3222, 3218, 1));
		assertNull("the Wilderness has no champion", table.at(3100, 3800, 0));
	}

	@Test
	public void theBlocklistIsNotEmpty() throws IOException
	{
		assertFalse("scripts/species-blocklist.txt has no names", blockedNames().isEmpty());
	}

	@Test
	public void noBlockedSpeciesIsBundled() throws IOException
	{
		for (String name : blockedNames())
			assertNull(name + " is blocked but still bundled", species.find(name));
	}

	private static List<String> blockedNames() throws IOException
	{
		List<String> names = new ArrayList<>();
		for (String line : Files.readAllLines(Paths.get("scripts", "species-blocklist.txt"), StandardCharsets.UTF_8))
		{
			String name = line.split("#", 2)[0].trim();
			if (!name.isEmpty())
				names.add(name);
		}

		return names;
	}

	private static Reader open(String resource)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(resource);
		assertNotNull(resource + " is missing from the resources", in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}
}
