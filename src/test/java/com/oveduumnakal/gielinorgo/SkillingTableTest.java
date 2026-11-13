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
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the skilling list: every name is a real, drawable species, and the picks follow the skill level. */
public class SkillingTableTest
{
	private static final String[] SKILLS = {"FISHING", "WOODCUTTING", "MINING", "FARMING", "HUNTER"};

	private final Gson gson = new Gson();
	private SpeciesTable species;
	private AnimationTable animations;
	private SkillingTable table;

	private Reader open(String resource)
	{
		InputStream in = SkillingTable.class.getResourceAsStream(resource);
		assertNotNull(resource + " is missing from the resources", in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	@Before
	public void setUp() throws IOException
	{
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			species = SpeciesTable.load(gson, reader);
		}

		try (Reader reader = open(AnimationTable.RESOURCE))
		{
			animations = AnimationTable.load(gson, reader);
		}

		try (Reader reader = open(SkillingTable.RESOURCE))
		{
			table = SkillingTable.load(gson, reader, species, animations);
		}
	}

	@Test
	public void everySkillHasSpeciesAndEveryListedNameSurvivesTheChecks() throws IOException
	{
		String raw;
		try (Reader reader = open(SkillingTable.RESOURCE))
		{
			StringBuilder text = new StringBuilder();
			for (int c = reader.read(); c >= 0; c = reader.read())
				text.append((char) c);

			raw = text.toString();
		}

		int listed = raw.split("\"species\"", -1).length - 1;
		int kept = table.exclusiveCount();
		for (String skill : SKILLS)
		{
			assertFalse(skill + " has no species", table.speciesOf(skill).isEmpty());
			kept += table.speciesOf(skill).size() - exclusiveIn(skill);
		}

		assertEquals("a listed name was dropped: not catchable or no animations", listed - 5, kept);
	}

	private int exclusiveIn(String skill)
	{
		int count = 0;
		for (Species each : table.speciesOf(skill))
		{
			if (table.exclusive(each))
				count++;
		}

		return count;
	}

	@Test
	public void everyListedSpeciesIsCatchableAndHasAnimations()
	{
		for (String skill : SKILLS)
		{
			for (Species each : table.speciesOf(skill))
			{
				assertFalse(each.name() + " is quest-only", each.questOnly());
				assertTrue(each.name() + " has no id", each.ids().length > 0);
				assertTrue(each.name() + " has no animation row", animations.has(each.ids()[0]));
			}
		}
	}

	@Test
	public void aLowSkillLevelNeverGetsAHighBracket()
	{
		Random random = new Random(3);
		for (int i = 0; i < 2_000; i++)
		{
			Species pick = table.pick("FISHING", 1, random);

			assertNotNull(pick);
			assertTrue(pick.name() + " is too strong for level 1", pick.level() < 20);
		}
	}

	@Test
	public void aHighSkillLevelMeetsStrongerCreatures()
	{
		Random random = new Random(4);
		int low = 0;
		int high = 0;
		for (int i = 0; i < 2_000; i++)
		{
			low += table.pick("FISHING", 10, random).level();
			high += table.pick("FISHING", 90, random).level();
		}

		assertTrue(high > low * 3);
	}

	@Test
	public void anExclusiveSpeciesComesOnlyForItsSkillAndLevelAtItsOdds()
	{
		Species troll = species.find("River troll");
		Random random = new Random(5);
		int trolls = 0;
		for (int i = 0; i < 20_000; i++)
		{
			assertFalse(table.pick("MINING", 99, random) == troll);
			assertFalse(table.pick("FISHING", 39, random) == troll);
			if (table.pick("FISHING", 99, random) == troll)
				trolls++;
		}

		assertTrue("about one in 20, got " + trolls, trolls > 800 && trolls < 1_200);
		assertEquals("FISHING", table.skillOf(troll));
	}

	@Test
	public void theExclusivesAreFlaggedSoNoRealMonsterMakesOne()
	{
		assertEquals(3, table.exclusiveCount());
		assertTrue(table.exclusive(species.find("River troll")));
		assertTrue(table.exclusive(species.find("Rock Golem")));
		assertTrue(table.exclusive(species.find("Tree spirit")));
		assertFalse(table.exclusive(species.find("Frog")));
		assertFalse(table.exclusive(null));
	}

	@Test
	public void everyExclusiveSpeciesIsInTheCatchableLog()
	{
		Set<Species> catchable = new HashSet<>(species.catchable());
		for (String skill : SKILLS)
		{
			for (Species each : table.speciesOf(skill))
				assertTrue(each.name() + " is not catchable", catchable.contains(each));
		}
	}

	@Test
	public void aSkillBringsItsOwnMessage()
	{
		assertEquals("surfaced", table.message("FISHING"));
		assertEquals("broke cover", table.message("HUNTER"));
		assertEquals("appeared", table.message("COOKING"));
	}

	@Test
	public void unknownNamesAreDroppedAndBadJsonGivesAnEmptyTable()
	{
		String json = "{\"skills\":{\"FISHING\":{\"message\":\"x\","
			+ "\"species\":[{\"species\":\"Nope\",\"minLevel\":1}]}}}";

		SkillingTable dropped = SkillingTable.load(gson, new StringReader(json), species, animations);
		SkillingTable broken = SkillingTable.load(gson, new StringReader("{\"skills\":3}"), species, animations);

		assertNull(dropped.pick("FISHING", 99, new Random(1)));
		assertNull(broken.pick("FISHING", 99, new Random(1)));
		assertEquals(0, broken.exclusiveCount());
	}
}
