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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the examine lines the Creature Log shows. */
public class ExamineTableTest
{
	/** Species the wiki's monster box gives no examine line for. */
	private static final Set<String> WITHOUT = new HashSet<>(Arrays.asList("goblin lieutenant", "goblin priest",
		"goblin raider", "king snagwort", "spawn sharhai", "starved horror", "underwater horror", "whale"));

	private final Gson gson = new Gson();

	@Test
	public void aLineIsFoundByKeyOrName()
	{
		ExamineTable table = ExamineTable.load(gson, new StringReader("{\"goblin\":\"An ugly green creature.\"}"));

		assertEquals("An ugly green creature.", table.textFor("goblin"));
		assertEquals("An ugly green creature.", table.textFor("  Goblin "));
		assertNull(table.textFor("imp"));
		assertEquals(1, table.size());
	}

	@Test
	public void missingOrMalformedJsonGivesAnEmptyTable()
	{
		assertEquals(0, ExamineTable.load(gson, new StringReader("[1,")).size());
		assertEquals(0, ExamineTable.load(gson, new StringReader("")).size());
		assertEquals(0, ExamineTable.empty().size());
	}

	@Test
	public void everySpeciesHasALineOrIsOnTheShortList() throws IOException
	{
		SpeciesTable species = bundledSpecies();
		ExamineTable table = bundledExamine();

		for (Species entry : species.all())
		{
			String line = table.textFor(entry.key());
			if (WITHOUT.contains(entry.key()))
			{
				assertNull(entry.name() + " now has a line: take it off the list", line);
				continue;
			}

			assertNotNull(entry.name() + " has no examine line", line);
			assertTrue(entry.name() + " has an empty line", !line.isEmpty());
		}
	}

	private SpeciesTable bundledSpecies() throws IOException
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream("species.json");
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return SpeciesTable.load(gson, reader);
		}
	}

	private ExamineTable bundledExamine() throws IOException
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(ExamineTable.RESOURCE);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return ExamineTable.load(gson, reader);
		}
	}
}
