/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the list of towns where the team may be changed, and that it stays apart from the City Champions. */
public class TeamTownsTest
{
	private final Gson gson = new Gson();

	@Test
	public void theBundledListLoadsWithEveryTownUsable() throws IOException
	{
		TeamTowns towns = bundled();

		assertTrue("expected a good list of towns, got " + towns.all().size(), towns.all().size() >= 25);
		Set<String> names = new HashSet<>();
		for (TeamTowns.Town town : towns.all())
			assertTrue("duplicate town " + town.name(), names.add(town.name()));
	}

	@Test
	public void varlamoreZeahAndPrifddinasCount() throws IOException
	{
		TeamTowns towns = bundled();

		assertEquals("Civitas illa Fortis", towns.at(1680, 3120).name());
		assertEquals("Aldarin", towns.at(1400, 2930).name());
		assertEquals("Prifddinas", towns.at(3240, 6110).name());
		assertEquals("Hosidius", towns.at(1750, 3598).name());
		assertEquals("Piscarilius", towns.at(1810, 3740).name());
	}

	@Test
	public void theOtherRealCitiesCount() throws IOException
	{
		TeamTowns towns = bundled();

		assertEquals("Burthorpe", towns.at(2899, 3545).name());
		assertEquals("Taverley", towns.at(2895, 3455).name());
		assertEquals("Camelot", towns.at(2757, 3477).name());
		assertEquals("Catherby", towns.at(2808, 3441).name());
		assertEquals("Port Sarim", towns.at(3030, 3220).name());
		assertEquals("Rimmington", towns.at(2957, 3214).name());
		assertEquals("West Ardougne", towns.at(2500, 3300).name());
	}

	@Test
	public void wildernessAndOpenCountrysideDoNotCount() throws IOException
	{
		TeamTowns towns = bundled();

		assertFalse(towns.contains(3100, 3700));
		assertFalse(towns.contains(3000, 3100));
		assertFalse(towns.contains(2900, 3300));
		assertNull(towns.at(1800, 3120));
	}

	@Test
	public void theChampionCitiesAreUnchangedAndNoTownTilesAreAChampionsCity() throws IOException
	{
		TeamTowns towns = bundled();
		ChampionTable champions = bundledChampions();

		assertEquals(8, champions.all().stream().filter(c -> !c.rival()).count());
		assertNull("Fortis is a town, not a champion's city", champions.at(1680, 3120, 0));
		assertNotNull(champions.at(3222, 3218, 0));
		assertEquals("Lumbridge", champions.at(3222, 3218, 0).city());
		assertFalse("a champion's city is not also in the town list by accident", towns.contains(3222, 3218));
	}

	@Test
	public void unusableEntriesAreDroppedAndBadJsonGivesAnEmptyList()
	{
		TeamTowns kept = TeamTowns.load(gson, new StringReader("[{\"name\":\"Ok\",\"minX\":1,\"minY\":1,\"maxX\":2,"
			+ "\"maxY\":2},{\"minX\":1,\"minY\":1,\"maxX\":2,\"maxY\":2},{\"name\":\"Flipped\",\"minX\":5,"
			+ "\"minY\":5,\"maxX\":1,\"maxY\":1},null]"));
		assertEquals(1, kept.all().size());
		assertTrue(kept.contains(2, 2));
		assertFalse(kept.contains(3, 2));

		assertTrue(TeamTowns.load(gson, new StringReader("{not json")).all().isEmpty());
		assertTrue(TeamTowns.load(gson, new StringReader("")).all().isEmpty());
		assertFalse(TeamTowns.empty().contains(0, 0));
	}

	private TeamTowns bundled() throws IOException
	{
		try (InputStream in = TeamTownsTest.class.getResourceAsStream(TeamTowns.RESOURCE))
		{
			assertNotNull("team-towns.json is missing", in);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return TeamTowns.load(gson, reader);
			}
		}
	}

	private ChampionTable bundledChampions() throws IOException
	{
		SpeciesTable species;
		try (InputStream in = TeamTownsTest.class.getResourceAsStream(SpeciesTable.RESOURCE);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			species = SpeciesTable.load(gson, reader);
		}

		try (InputStream in = TeamTownsTest.class.getResourceAsStream(ChampionTable.RESOURCE);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return ChampionTable.load(gson, reader, species);
		}
	}
}
