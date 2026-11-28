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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks evolutions that wait for night or for the player to be underground. */
public class GoGameConditionTest
{
	private static final Conditions NIGHT = Conditions.of(true, false);
	private static final Conditions CAVE = Conditions.of(false, true);

	private final List<String> messages = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Imp", 7, CombatType.STAB),
			new Species("Lesser demon", 82, CombatType.STAB), new Species("Zombie", 24, CombatType.STAB),
			new Species("Armoured zombie", 105, CombatType.STAB), new Species("Rat", 1, CombatType.STAB),
			new Species("Giant rat", 26, CombatType.STAB), new Species("Crypt rat", 43, CombatType.STAB),
			new Species("Bat", 6, CombatType.STAB), new Species("Giant bat", 27, CombatType.STAB),
			new Species("Dire bat", 142, CombatType.STAB)));
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(
			new EvolutionTable.Step("Imp", "Lesser demon", 20),
			new EvolutionTable.Step("Zombie", "Armoured zombie", 40, EvolutionCondition.NIGHT),
			new EvolutionTable.Step("Rat", "Giant rat", 15, EvolutionCondition.SURFACE),
			new EvolutionTable.Step("Rat", "Crypt rat", 15, EvolutionCondition.UNDERGROUND),
			new EvolutionTable.Step("Bat", "Giant bat", 12),
			new EvolutionTable.Step("Giant bat", "Dire bat", 40, EvolutionCondition.UNDERGROUND)), table);
		game = new GoGame(table, evolutions, ChampionTable.empty(), new Random(5), () -> 1_000L,
			notice -> messages.add(notice.text()));
	}

	private Creature lead()
	{
		return game.data().lead();
	}

	private String waiting()
	{
		List<CreatureView> team = game.snapshot().team();
		return team.get(0).evolvesWhen();
	}

	private String speciesAt(int index)
	{
		List<Creature> team = game.data().team;
		return team.get(index).species;
	}

	private void train()
	{
		game.addOsrsXp(10 * (int) XpRules.xpForLevel(60));
	}

	@Test
	public void aNightStepWaitsByDayAndFiresWhenNightFalls()
	{
		game.data().team.add(new Creature("zombie", 39, 1_000L));
		train();

		assertEquals("zombie", lead().species);
		assertEquals("Evolves at night", waiting());

		assertTrue(game.setConditions(NIGHT));

		assertEquals("armoured zombie", lead().species);
		assertTrue(messages.contains("Zombie evolved into Armoured zombie!"));
		assertNull(waiting());
	}

	@Test
	public void aNightStepFiresOnTheNextXpShareAtNight()
	{
		game.setConditions(NIGHT);
		game.data().team.add(new Creature("zombie", 39, 1_000L));

		train();

		assertEquals("armoured zombie", lead().species);
	}

	@Test
	public void anUndergroundStepFiresWhenThePlayerWalksIntoACave()
	{
		game.data().team.add(new Creature("giant bat", 39, 1_000L));
		train();
		assertEquals("giant bat", lead().species);

		assertFalse(game.setConditions(Conditions.NONE));
		assertTrue(game.setConditions(CAVE));

		assertEquals("dire bat", lead().species);
	}

	@Test
	public void aBranchTakesTheSideThatMatchesWhereTheCreatureGrows()
	{
		game.data().team.add(new Creature("rat", 14, 1_000L));
		game.data().team.add(new Creature("rat", 14, 1_000L));
		game.setConditions(CAVE);
		train();

		assertEquals("crypt rat", speciesAt(0));

		game.setConditions(Conditions.NONE);
		game.data().team.add(new Creature("rat", 14, 1_000L));
		train();

		assertEquals("giant rat", speciesAt(2));
	}

	@Test
	public void anOrdinaryLineIsUnchangedByTheWorld()
	{
		game.setConditions(NIGHT);
		game.data().team.add(new Creature("imp", 19, 1_000L));
		train();

		assertEquals("lesser demon", lead().species);
	}

	@Test
	public void severalLevelsEvolveUpToAWaitingStep()
	{
		game.data().team.add(new Creature("bat", 1, 1_000L));
		train();

		assertEquals("giant bat", lead().species);
		assertEquals("Evolves underground", waiting());
	}

	@Test
	public void aStoredCreatureDoesNotEvolveWhenTheConditionComes()
	{
		Creature stored = new Creature("zombie", 50, 1_000L);
		game.data().storage.add(stored);

		game.setConditions(NIGHT);

		assertEquals("zombie", stored.species);
	}

	private String nextForm()
	{
		List<CreatureView> team = game.snapshot().team();
		return team.get(0).nextForm();
	}

	@Test
	public void theNextFormShowsQuestionMarksUntilItIsSeenOrCaught()
	{
		game.data().team.add(new Creature("imp", 5, 1_000L));
		game.data().seen.clear();

		assertEquals(SummaryContent.UNKNOWN_FORM, nextForm());

		game.data().seen.add("lesser demon");

		assertEquals("Lesser demon", nextForm());

		game.data().seen.clear();
		game.data().caught.put("lesser demon", 1);

		assertEquals("Lesser demon", nextForm());
	}

	@Test
	public void aSpeciesThatDoesNotEvolveHasNoNextForm()
	{
		game.data().team.add(new Creature("dire bat", 5, 1_000L));

		assertNull(nextForm());
	}

	@Test
	public void aCreatureThatArrivesPastItsLevelEvolvesOnTheNextTick()
	{
		game.setConditions(Conditions.NONE);
		assertTrue(game.receive(new Creature("bat", 15, 1_000L)));
		assertEquals("bat", lead().species);

		assertTrue(game.setConditions(Conditions.NONE));

		assertEquals("giant bat", lead().species);
		assertFalse(game.setConditions(Conditions.NONE));
	}

	@Test
	public void aStoredCreatureEvolvesWhenItJoinsTheTeam()
	{
		game.data().team.add(new Creature("imp", 3, 1_000L));
		game.data().storage.add(new Creature("bat", 15, 1_000L));
		game.setConditions(Conditions.NONE);
		game.setBankNear(true);

		game.moveToTeam(0);
		game.setConditions(Conditions.NONE);

		assertEquals("giant bat", speciesAt(1));
	}

	@Test
	public void aLoadedCreatureEvolvesEvenWhenNothingChanged()
	{
		game.setConditions(NIGHT);
		SaveData saved = SaveData.fresh();
		saved.team.add(new Creature("zombie", 45, 1_000L));
		game.load(saved);

		game.setConditions(NIGHT);

		assertEquals("armoured zombie", lead().species);
	}
}
