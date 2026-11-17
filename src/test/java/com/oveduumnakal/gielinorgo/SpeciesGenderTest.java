/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the fixed genders against the bundled species, and that every other creature rolls and falls back. */
public class SpeciesGenderTest
{
	private static final int ROLLS = 1000;

	private static Set<String> names;

	@BeforeClass
	public static void loadSpecies() throws Exception
	{
		try (Reader reader = new InputStreamReader(SpeciesTable.class.getResourceAsStream("species.json"),
			StandardCharsets.UTF_8))
		{
			names = new HashSet<>();
			for (Species species : SpeciesTable.load(new Gson(), reader).all())
				names.add(species.name());
		}
	}

	@Test
	public void everyListedNameIsASpeciesExactly()
	{
		for (String name : SpeciesGender.femaleNames())
			assertTrue("not a species: " + name, names.contains(name));

		for (String name : SpeciesGender.maleNames())
			assertTrue("not a species: " + name, names.contains(name));
	}

	@Test
	public void noNameIsListedTwice()
	{
		Set<String> keys = new HashSet<>();
		for (String name : SpeciesGender.femaleNames())
			assertTrue("listed twice: " + name, keys.add(Species.keyFor(name)));

		for (String name : SpeciesGender.maleNames())
			assertTrue("listed twice: " + name, keys.add(Species.keyFor(name)));
	}

	@Test
	public void everySirKnightAndTheCommaNameAreMale()
	{
		int knights = 0;
		for (String name : names)
		{
			if (name.startsWith("Sir "))
			{
				knights++;
				assertEquals(name, Gender.MALE, SpeciesGender.fixed(name));
			}
		}

		assertEquals(13, knights);
		assertEquals(Gender.MALE, SpeciesGender.fixed("Cuthbert, Lord of Dread"));
	}

	@Test
	public void bansheesAndSirenAreFemaleButChickenAndCowCalfAreOpen()
	{
		for (String name : names)
		{
			if (name.toLowerCase(Locale.ROOT).contains("banshee"))
				assertEquals(name, Gender.FEMALE, SpeciesGender.fixed(name));
		}

		assertEquals(Gender.FEMALE, SpeciesGender.fixed("Siren"));
		assertEquals(Gender.FEMALE, SpeciesGender.fixed("Cow"));
		assertNull(SpeciesGender.fixed("Cow calf"));
		assertNull(SpeciesGender.fixed("Chicken"));
		assertNull(SpeciesGender.fixed("Guard"));
		assertNull(SpeciesGender.fixed("Goblin"));
	}

	@Test
	public void womanAndManNeverChangeOverManyRolls()
	{
		Random random = new Random(7);
		for (int i = 0; i < ROLLS; i++)
		{
			assertEquals(Gender.FEMALE, SpeciesGender.roll("woman", random));
			assertEquals(Gender.MALE, SpeciesGender.roll("man", random));
			assertEquals(Gender.FEMALE, SpeciesGender.stable("Woman", i));
			assertEquals(Gender.MALE, SpeciesGender.stable("Man", i));
		}
	}

	@Test
	public void anyOtherSpeciesRollsBothGenders()
	{
		Random random = new Random(7);
		int males = 0;
		for (int i = 0; i < ROLLS; i++)
		{
			if (SpeciesGender.roll("goblin", random) == Gender.MALE)
				males++;
		}

		assertTrue("males: " + males, males > 400 && males < 600);
	}

	@Test
	public void theStableGuessIsTheSameEveryTimeAndSpreadsBothWays()
	{
		int males = 0;
		for (int i = 0; i < ROLLS; i++)
		{
			Gender first = SpeciesGender.stable("goblin", 1_700_000_000_000L + i);
			assertEquals(first, SpeciesGender.stable("goblin", 1_700_000_000_000L + i));
			if (first == Gender.MALE)
				males++;
		}

		assertTrue("males: " + males, males > 400 && males < 600);
	}

	@Test
	public void anOldCreatureGetsAGenderAndAFixedSpeciesWinsOverASavedOne()
	{
		Creature old = new Creature("goblin", 5, 123_456L);
		assertNotNull(old.gender());
		assertEquals(old.gender(), old.gender());

		Creature woman = new Creature("woman", 5, 123_456L);
		woman.gender = Gender.MALE;
		assertEquals(Gender.FEMALE, woman.gender());
	}

	@Test
	public void aNameIsFoundIgnoringCaseAndSpaces()
	{
		assertEquals(Gender.FEMALE, SpeciesGender.fixed("GNOME WOMAN"));
		assertEquals(Gender.FEMALE, SpeciesGender.fixed(" woman "));
	}

	@Test
	public void aRivalOrRematchMemberUsesTheTable()
	{
		assertEquals(Gender.FEMALE, new Champion.Member("Woman", 10).gender());
		assertEquals(Gender.MALE, new Champion.Member("Man", 10).gender());
		assertEquals(new Champion.Member("Goblin", 10).gender(), new Champion.Member("Goblin", 10).gender());
	}
}
