/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks that every NPC variant of a species has animations, and the run-time fallback for one that has not. */
public class VariantAnimationsTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void everyNpcIdOfEverySpeciesHasARowOrIsOnTheExplicitList() throws IOException
	{
		AnimationTable table = animations();
		Set<Integer> exempt = idsWithoutAnimations();
		for (Species species : species().all())
		{
			for (int id : species.ids())
			{
				boolean listed = exempt.contains(id);
				assertTrue(species.name() + " NPC " + id + " has no animation row", table.has(id) || listed);
				assertFalse(species.name() + " NPC " + id + " has a row but is on the list", table.has(id) && listed);
			}
		}
	}

	@Test
	public void theFirstNpcIdOfEverySpeciesIsTheOneRecordedBeforeVariantsWereAdded() throws IOException
	{
		SpeciesTable species = species();
		int checked = 0;
		try (BufferedReader reader = lines("/species-first-ids.txt"))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] parts = line.split("\t");
				Species found = species.find(parts[0]);
				assertNotNull(parts[0] + " is gone", found);
				assertEquals(parts[0], Integer.parseInt(parts[1]), found.ids()[0]);
				checked++;
			}
		}

		assertTrue("only " + checked + " species were checked", checked > 1200);
	}

	@Test
	public void goblinAndMonkeyGuardVariantsAreListed() throws IOException
	{
		SpeciesTable species = species();
		AnimationTable table = animations();

		assertTrue(contains(species.find("Goblin").ids(), 3029));
		assertTrue(table.has(3029));
		assertEquals(3046, species.find("Goblin").ids()[0]);
		assertEquals(5275, species.find("Monkey Guard").ids()[0]);
		assertTrue(contains(species.find("Monkey Guard").ids(), 5271));
		assertTrue(table.has(5271));
	}

	@Test
	public void aRowIsAlwaysTrustedOverTheLiveNpc() throws IOException
	{
		AnimationTable table = animations();

		assertEquals(table.idle(2), table.idle(2, 999));
		assertEquals(table.walk(2), table.walk(2, 998, 999));
	}

	@Test
	public void anIdWithNoRowStandsAndWalksLikeTheLiveNpc()
	{
		AnimationTable table = AnimationTable.empty();

		assertEquals(808, table.idle(77, 808));
		assertEquals(819, table.walk(77, 819, 808));
	}

	@Test
	public void anIdWithNoRowAndNoLiveWalkWalksInTheLiveStand()
	{
		AnimationTable table = AnimationTable.empty();

		assertEquals(808, table.walk(77, AnimationTable.NONE, 808));
	}

	@Test
	public void anIdWithNoRowAndNoLiveNpcStandsStill()
	{
		AnimationTable table = AnimationTable.empty();

		assertEquals(AnimationTable.NONE, table.idle(77, AnimationTable.NONE));
		assertEquals(AnimationTable.NONE, table.walk(77, AnimationTable.NONE, AnimationTable.NONE));
		assertEquals(AnimationTable.NONE, table.idle(77, -5));
	}

	@Test
	public void attackDefendAndDeathStayOnTheTableOnly()
	{
		AnimationTable table = AnimationTable.empty();

		assertEquals(AnimationTable.NONE, table.attack(77));
		assertEquals(AnimationTable.NONE, table.defend(77));
		assertEquals(AnimationTable.NONE, table.death(77));
	}

	private static boolean contains(int[] ids, int wanted)
	{
		for (int id : ids)
		{
			if (id == wanted)
				return true;
		}

		return false;
	}

	private static Set<Integer> idsWithoutAnimations() throws IOException
	{
		Set<Integer> ids = new HashSet<>();
		try (BufferedReader reader = lines("/ids-without-animations.txt"))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!line.isEmpty() && line.charAt(0) != '#')
					ids.add(Integer.parseInt(line.trim()));
			}
		}

		return ids;
	}

	private static BufferedReader lines(String resource)
	{
		InputStream in = VariantAnimationsTest.class.getResourceAsStream(resource);
		assertNotNull(resource + " is missing", in);
		return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
	}

	private static AnimationTable animations() throws IOException
	{
		try (Reader reader = open(AnimationTable.RESOURCE))
		{
			return AnimationTable.load(GSON, reader);
		}
	}

	private static SpeciesTable species() throws IOException
	{
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			return SpeciesTable.load(GSON, reader);
		}
	}

	private static Reader open(String resource)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(resource);
		assertNotNull(resource + " is missing from the resources", in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}
}
