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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks that a creature keeps its gender through evolution, except into a species that is always one gender. */
public class GoGameGenderTest
{
	private final List<String> messages = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Imp", 7, CombatType.STAB),
			new Species("Lesser demon", 82, CombatType.STAB), new Species("Bat", 6, CombatType.STAB),
			new Species("Banshee", 20, CombatType.STAB), new Species("Rat", 1, CombatType.STAB),
			new Species("Giant rat", 26, CombatType.STAB)));
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(
			new EvolutionTable.Step("Imp", "Lesser demon", 20), new EvolutionTable.Step("Bat", "Banshee", 12),
			new EvolutionTable.Step("Rat", "Giant rat", 15)), table);
		game = new GoGame(table, evolutions, ChampionTable.empty(), new Random(5), () -> 1_000L,
			notice -> messages.add(notice.text()));
	}

	private Creature evolve(Creature creature)
	{
		game.data().team.clear();
		game.data().team.add(creature);
		game.addOsrsXp(10 * (int) XpRules.xpForLevel(60));
		return game.data().lead();
	}

	@Test
	public void evolutionKeepsASavedGender()
	{
		for (Gender gender : Gender.values())
		{
			Creature imp = new Creature("imp", 19, 1_000L);
			imp.gender = gender;
			Creature grown = evolve(imp);
			assertEquals("lesser demon", grown.species);
			assertEquals(gender, grown.gender());
		}
	}

	@Test
	public void evolutionKeepsTheLegacyGuessOfAnOlderCreature()
	{
		boolean male = false;
		boolean female = false;
		for (long caughtAt = 1_000L; caughtAt < 1_040L; caughtAt++)
		{
			Creature rat = new Creature("rat", 14, caughtAt);
			Gender before = rat.gender();
			Creature grown = evolve(rat);
			assertEquals("giant rat", grown.species);
			assertNotNull(grown.gender);
			assertEquals("caughtAtMs " + caughtAt, before, grown.gender());
			male |= before == Gender.MALE;
			female |= before == Gender.FEMALE;
		}

		assertTrue("the sample should hold both", male && female);
	}

	@Test
	public void evolutionIntoAFixedSpeciesTakesItsGender()
	{
		Creature bat = new Creature("bat", 11, 1_000L);
		bat.gender = Gender.MALE;
		Creature grown = evolve(bat);
		assertEquals("banshee", grown.species);
		assertEquals(Gender.FEMALE, grown.gender());
		assertEquals(Gender.FEMALE, grown.gender);
	}

	@Test
	public void leavingAFixedSpeciesKeepsItsGender()
	{
		Creature woman = new Creature("woman", 5, 1_000L);
		woman.evolveInto("goblin");
		assertEquals(Gender.FEMALE, woman.gender());
		assertEquals(Gender.FEMALE, woman.gender);
	}

	@Test
	public void aNewCreatureRollsBothGendersWhenTheSpeciesIsOpen()
	{
		Random random = new Random(3);
		boolean male = false;
		boolean female = false;
		for (int i = 0; i < 100; i++)
		{
			Creature rat = new Creature("rat", 1, 0L);
			rat.rollGender(random);
			male |= rat.gender == Gender.MALE;
			female |= rat.gender == Gender.FEMALE;
		}

		assertTrue(male && female);
	}

	@Test
	public void everyStarterHasAGender()
	{
		for (Creature starter : StarterRules.roll(new Random(3), 1_000L))
			assertNotNull(starter.gender);
	}
}
