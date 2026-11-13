/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks that a quest-only species is data a player can own but never meet, hatch, feature or count (issue #50). */
public class QuestSpeciesTest
{
	private static final Type STEPS = new TypeToken<List<EvolutionTable.Step>>()
	{
	}.getType();

	private static final String QUEST_KEY = "quest beast";

	private final Gson gson = new Gson();

	private static SpeciesTable smallTable()
	{
		List<Species> all = new ArrayList<>();
		all.add(new Species("Quest Beast", 1, CombatType.STAB).asQuestOnly());
		all.add(new Species("Plain Beast", 1, CombatType.STAB));
		for (int i = 0; i < 24; i++)
			all.add(new Species(String.format("Beast %03d", i), 2 + i, CombatType.STAB));

		return new SpeciesTable(all);
	}

	private SpeciesTable bundled() throws IOException
	{
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			return SpeciesTable.load(gson, reader);
		}
	}

	private static Reader open(String resource)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(resource);
		assertNotNull(resource + " is missing from the resources", in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	private static List<String> names(String file) throws IOException
	{
		List<String> names = new ArrayList<>();
		for (String line : Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8))
		{
			String name = line.split("#", 2)[0].trim();
			if (!name.isEmpty())
				names.add(name.toLowerCase());
		}

		return names;
	}

	@Test
	public void aFlaggedSpeciesIsStillASpeciesButNotCatchable()
	{
		SpeciesTable table = smallTable();

		assertNotNull(table.find("quest beast"));
		assertTrue(table.find("Quest Beast").questOnly());
		assertFalse(table.find("Plain Beast").questOnly());
		assertEquals(26, table.size());
		assertEquals(25, table.catchableSize());
		assertFalse(table.catchable().contains(table.find(QUEST_KEY)));
	}

	@Test
	public void aFlaggedSpeciesNeverTurnsWildWhateverTheRoll()
	{
		Species quest = new Species("Quest Beast", 40, CombatType.STAB).asQuestOnly();
		WildTracker tracker = new WildTracker(new Random(5));
		for (int i = 0; i < 400; i++)
		{
			long now = i * (WildTracker.COOLDOWN_MS + 1);
			WildEntry entry = tracker.offer(i, 100 + i, quest, 40, i % 2 == 0, now, 1.0, 1, true);

			assertNull(entry);
			assertTrue(tracker.all().isEmpty());
		}

		Species plain = new Species("Plain Beast", 40, CombatType.STAB);
		assertNotNull(tracker.offer(999, 7, plain, 40, false, 10_000_000L, 1.0, 1, true));
	}

	@Test
	public void aFlaggedSpeciesIsNeverFeatured()
	{
		SpawnCalendar calendar = new SpawnCalendar(smallTable());
		LocalDate day = LocalDate.of(2026, 1, 1);
		for (int i = 0; i < 400; i++)
		{
			for (Species pick : calendar.featured(day.plusDays(i)))
				assertFalse(pick.name() + " is quest-only", pick.questOnly());
		}
	}

	@Test
	public void aFlaggedSpeciesNeverHatches()
	{
		GoGame game = new GoGame(smallTable(), new Random(9), () -> 0L, notice ->
		{
		});
		for (int i = 0; i < 200; i++)
		{
			game.data().team.clear();
			game.data().storage.clear();
			game.data().eggs.clear();
			game.data().eggs.add(new Egg(Rarity.COMMON));
			game.addSteps(Rarity.COMMON.eggTiles());

			assertEquals(1, game.data().team.size());
			Creature newborn = game.data().team.get(0);
			String born = newborn.species;
			assertFalse(born.equals(QUEST_KEY));
		}
	}

	@Test
	public void anEvolutionIntoAFlaggedSpeciesIsLeftOutAndOneOutOfItStays()
	{
		EvolutionTable.Step into = new EvolutionTable.Step("Plain Beast", "Quest Beast", 10);
		EvolutionTable.Step outOf = new EvolutionTable.Step("Quest Beast", "Beast 003", 10);

		EvolutionTable table = new EvolutionTable(Arrays.asList(into, outOf), smallTable());

		assertNull(table.next("Plain Beast"));
		assertNotNull(table.next("Quest Beast"));
	}

	@Test
	public void anOldSaveWithAFlaggedCreatureLoadsAndCountsOnlyTheOpenSpecies()
	{
		List<String> messages = new ArrayList<>();
		GoGame game = new GoGame(smallTable(), new Random(3), () -> 0L, notice -> messages.add(notice.text()));
		game.milestones(new Milestones(smallTable(), new ArrayList<>()));
		SaveData old = SaveData.fresh();
		for (int i = 0; i < 24; i++)
			old.caught.put(String.format("beast %03d", i), 1);

		old.caught.put(QUEST_KEY, 2);
		old.milestones.add("species-25");
		Creature owned = new Creature(QUEST_KEY, 5, 0L);
		old.team.add(owned);
		int jars = old.jarCount(JarType.IRON);

		game.load(old);
		PanelState state = game.snapshot();

		assertEquals(1, game.data().team.size());
		assertNotNull(game.speciesFor(owned.species));
		assertEquals(1, state.team().size());
		assertEquals(25, state.totalSpecies());
		assertEquals(24, state.caughtSpecies());
		assertEquals(26, state.log().size());
		assertTrue(state.log().stream().anyMatch(entry -> entry.retired() && entry.caught()));
		assertEquals(jars, game.data().jarCount(JarType.IRON));
		assertEquals(1, game.data().milestones.size());
		assertTrue(messages.stream().noneMatch(line -> line.startsWith("Milestone:")));
	}

	@Test
	public void aFlaggedSpeciesTheSaveNeverCaughtIsNotInTheLog()
	{
		GoGame game = new GoGame(smallTable(), new Random(3), () -> 0L, notice ->
		{
		});

		PanelState state = game.snapshot();

		assertEquals(25, state.totalSpecies());
		assertEquals(25, state.log().size());
		for (LogEntry entry : state.log())
			assertFalse(entry.retired());
	}

	@Test
	public void aFlaggedMemberIsNotCountedForAFamily()
	{
		Family family = new Family("beasts", "Beasts", JarType.EMBER,
			Arrays.asList("quest beast", "beast 001", "beast 002", "beast 003"));
		Milestones milestones = new Milestones(smallTable(), Arrays.asList(family));
		SaveData data = SaveData.fresh();
		data.caught.put("beast 001", 1);
		data.caught.put("beast 002", 1);
		data.caught.put("beast 003", 1);

		assertTrue(milestones.reached(data).contains("family-beasts"));
		data.caught.put(QUEST_KEY, 1);

		assertEquals(4, data.caught.size());
		assertEquals(3, milestones.caughtCount(data));
	}

	@Test
	public void noBundledOpenFeatureNamesAFlaggedSpecies() throws IOException
	{
		SpeciesTable table = bundled();
		assertTrue("expected some quest-only species", table.size() - table.catchableSize() > 50);
		for (EvolutionTable.Step step : stepsOf())
		{
			assertFalse(step.from() + " to " + step.to() + " ends in a quest-only species",
				table.find(step.to()).questOnly());
		}

		List<Family> families;
		try (Reader reader = open(Family.RESOURCE))
		{
			families = Family.load(gson, reader);
		}

		for (Family family : families)
		{
			for (String key : family.species())
				assertFalse(family.id() + " lists quest-only " + key, table.find(key).questOnly());
		}

		SpawnCalendar calendar = new SpawnCalendar(table);
		for (int i = 0; i < 800; i++)
		{
			for (Species pick : calendar.featured(LocalDate.of(2026, 1, 1).plusDays(i)))
				assertFalse(pick.name() + " is featured but quest-only", pick.questOnly());
		}

		for (Species candidate : table.catchable())
			assertFalse(candidate.name(), candidate.questOnly());
	}

	private List<EvolutionTable.Step> stepsOf() throws IOException
	{
		try (Reader reader = open(EvolutionTable.RESOURCE))
		{
			return gson.fromJson(reader, STEPS);
		}
	}

	@Test
	public void theOverrideFilesNameRealSpecies() throws IOException
	{
		SpeciesTable table = bundled();
		for (String name : names("scripts/species-quest-keep.txt"))
		{
			assertNotNull(name + " is in the keep file but is not a species", table.find(name));
			assertFalse(name + " is kept but flagged", table.find(name).questOnly());
		}

		for (String name : names("scripts/species-quest-add.txt"))
		{
			assertNotNull(name + " is in the add file but is not a species", table.find(name));
			assertTrue(name + " is added but not flagged", table.find(name).questOnly());
		}
	}

	@Test
	public void theCommittedListMatchesTheFlagsInTheSpeciesFile() throws IOException
	{
		Set<String> listed = new HashSet<>();
		try (InputStream in = QuestSpeciesTest.class.getResourceAsStream("/quest-species.txt"))
		{
			assertNotNull("quest-species.txt is missing from the test resources", in);
			for (String line : new String(readAll(in), StandardCharsets.UTF_8).split("\n"))
			{
				if (!line.isEmpty() && !line.startsWith("#"))
					listed.add(Species.keyFor(line));
			}
		}

		Set<String> flagged = new HashSet<>();
		for (Species species : bundled().all())
		{
			if (species.questOnly())
				flagged.add(species.key());
		}

		assertEquals(flagged, listed);
	}

	private static byte[] readAll(InputStream in) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = in.read(buffer)) > 0)
			out.write(buffer, 0, read);

		return out.toByteArray();
	}
}
