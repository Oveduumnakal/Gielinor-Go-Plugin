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
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the bundled boss list against the bundled species. */
public class BossTableTest
{
	private final Gson gson = new Gson();

	private SpeciesTable bundledSpecies() throws IOException
	{
		try (InputStream in = SpeciesTable.class.getResourceAsStream(SpeciesTable.RESOURCE);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return SpeciesTable.load(gson, reader);
		}
	}

	private Map<String, String> bundledNames() throws IOException
	{
		Type type = new TypeToken<Map<String, String>>()
		{
		}.getType();
		try (InputStream in = BossTable.class.getResourceAsStream(BossTable.RESOURCE))
		{
			assertNotNull("bosses.json is missing from the resources", in);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return gson.fromJson(reader, type);
			}
		}
	}

	@Test
	public void everyMappedSpeciesExistsAndIsCatchable() throws IOException
	{
		SpeciesTable species = bundledSpecies();
		for (Map.Entry<String, String> entry : bundledNames().entrySet())
		{
			Species found = species.find(entry.getValue());
			assertNotNull(entry.getKey() + " maps to a missing species " + entry.getValue(), found);
			assertFalse(entry.getKey() + " maps to a quest-only species", found.questOnly());
		}
	}

	@Test
	public void theTableKeepsEveryNameOfTheFile() throws IOException
	{
		Map<String, String> names = bundledNames();
		BossTable table = new BossTable(names, bundledSpecies());

		assertEquals(names.size(), table.size());
	}

	@Test
	public void theSourceFileAndTheResourceAreTheSame() throws IOException
	{
		String source = new String(Files.readAllBytes(Paths.get("scripts", "boss-names.json")),
			StandardCharsets.UTF_8);

		assertEquals(gson.fromJson(source, Map.class), bundledNames());
	}

	@Test
	public void raidsMapToTheirFinalBoss() throws IOException
	{
		BossTable table = new BossTable(bundledNames(), bundledSpecies());

		assertEquals("Great Olm", table.speciesFor("Chambers of Xeric").name());
		assertEquals("Great Olm", table.speciesFor("Chambers of Xeric Challenge Mode").name());
		assertEquals("Verzik Vitur", table.speciesFor("Theatre of Blood").name());
		assertEquals("Verzik Vitur", table.speciesFor("Theatre of Blood: Hard Mode").name());
		assertEquals("Elidinis' Warden", table.speciesFor("Tombs of Amascut").name());
		assertEquals("Elidinis' Warden", table.speciesFor("Tombs of Amascut Expert Mode").name());
	}

	@Test
	public void theReFightableInstanceBossesAreInTheTable() throws IOException
	{
		BossTable table = new BossTable(bundledNames(), bundledSpecies());

		assertEquals("Vorkath", table.speciesFor("Vorkath").name());
		assertEquals("Vorkath", table.speciesFor("vorkath").name());
		assertEquals("Giant Mole", table.speciesFor("Giant Mole").name());
		assertTrue(table.isBoss(table.speciesFor("Zulrah")));
	}

	@Test
	public void anUnknownNameDoesNothing() throws IOException
	{
		BossTable table = new BossTable(bundledNames(), bundledSpecies());

		assertNull(table.speciesFor("Chicken"));
		assertNull(table.speciesFor(null));
		assertNull(BossTable.empty().speciesFor("Vorkath"));
		assertFalse(BossTable.empty().isBoss(null));
	}
}
