/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks who is offered a starter creature, the three starters, and what happens when one is picked. */
public class StarterRulesTest
{
	private final List<String> messages = new ArrayList<>();
	private GoGame game;
	private EvolutionTable evolutions;
	private SpeciesTable table;
	private long now = 1_000_000L;

	@Before
	public void setUp() throws IOException
	{
		Gson gson = new Gson();
		try (Reader species = reader("species.json"); Reader lines = reader("evolutions.json"))
		{
			table = SpeciesTable.load(gson, species);
			evolutions = EvolutionTable.load(gson, lines, table);
		}

		game = new GoGame(table, evolutions, ChampionTable.empty(), new Random(11), () -> now,
			notice -> messages.add(notice.text()));
		game.starterRandom(new Random(5));
	}

	private static Reader reader(String name)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(name);
		assertNotNull(name, in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	@Test
	public void aFreshSaveIsOfferedAStarter()
	{
		assertTrue(StarterRules.offered(SaveData.fresh()));
	}

	@Test
	public void aCatchACreatureOrAnythingStoredEndsTheOffer()
	{
		SaveData caught = SaveData.fresh();
		caught.caught.put("goblin", 1);
		SaveData team = SaveData.fresh();
		team.team.add(new Creature("goblin", 3, now));
		SaveData stored = SaveData.fresh();
		stored.storage.add(new Creature("goblin", 3, now));

		assertFalse(StarterRules.offered(caught));
		assertFalse(StarterRules.offered(team));
		assertFalse(StarterRules.offered(stored));
	}

	@Test
	public void threeStartersAtLevelFiveAreRolledNeverGildedInOrder()
	{
		List<Creature> rolled = StarterRules.roll(new Random(3), now);

		assertEquals(3, rolled.size());
		assertEquals("imp", rolled.get(0).species);
		assertEquals("gnome archer", rolled.get(1).species);
		assertEquals("wizard", rolled.get(2).species);
		for (Creature creature : rolled)
		{
			assertEquals(StarterRules.LEVEL, creature.level());
			assertFalse(creature.gilded);
			assertTrue(creature.rolls().valid());
		}
	}

	@Test
	public void aFixedSeedGivesTheSameThree()
	{
		List<Creature> first = StarterRules.roll(new Random(9), now);
		List<Creature> second = StarterRules.roll(new Random(9), now);

		for (int i = 0; i < 3; i++)
		{
			StatRolls a = first.get(i).rolls();
			StatRolls b = second.get(i).rolls();
			assertEquals(a.describeNet(), b.describeNet());
		}
	}

	@Test
	public void theStartersAreOneOfEachStyleAndAllExist()
	{
		assertEquals(CombatType.MELEE, table.find("Imp").style());
		assertEquals(CombatType.RANGED, table.find("Gnome Archer").style());
		assertEquals(CombatType.MAGIC, table.find("Wizard").style());
		for (String name : StarterRules.STARTERS)
			assertFalse(name, table.find(name).questOnly());
	}

	@Test
	public void theArcherLineGrowsFromTheGnomeArcherToTheElfArcher()
	{
		List<String> line = new ArrayList<>();
		EvolutionTable.Step step = evolutions.next("Gnome Archer");
		while (step != null)
		{
			line.add(step.to() + step.level());
			step = evolutions.next(step.to());
		}

		assertEquals(Arrays.asList("Archer20", "Tower Archer40", "Elf Archer60"), line);
		for (String name : Arrays.asList("Gnome Archer", "Archer", "Tower Archer", "Elf Archer"))
		{
			assertFalse(name, table.find(name).questOnly());
			assertEquals(CombatType.RANGED, table.find(name).style());
		}
	}

	@Test
	public void theOfferIsKeptSoReopeningThePanelDoesNotRerollIt()
	{
		List<StarterView> first = game.starterOffer();
		List<StarterView> second = game.starterOffer();

		assertEquals(3, first.size());
		for (int i = 0; i < 3; i++)
		{
			CreatureView a = first.get(i).creature();
			CreatureView b = second.get(i).creature();
			assertEquals(a.rollsNetLines(), b.rollsNetLines());
		}

		assertEquals("Grows into Lesser demon (20), Greater demon (40), Black demon (60)", first.get(0).growsInto());
		PanelState state = game.snapshot();
		assertEquals(3, state.starters().size());
	}

	@Test
	public void aPickMakesALeadMarksTheLogAndMovesNoTaskOrStreak()
	{
		int jars = game.data().jarCount(JarType.BRONZE);
		assertTrue(game.pickStarter(1));

		assertEquals(1, game.data().team.size());
		Creature lead = game.data().lead();
		assertEquals("gnome archer", lead.species);
		assertEquals(5, lead.level());
		assertEquals(1, (int) game.data().caught.get("gnome archer"));
		assertTrue(game.data().seen.contains("gnome archer"));
		assertEquals(0L, game.data().trainerXp);
		assertEquals(0, game.data().tasks.streak);
		assertEquals(jars, game.data().jarCount(JarType.BRONZE));
		assertTrue(messages.contains("Gnome Archer joined your team"));
		assertTrue(game.starterOffer().isEmpty());
		assertTrue(game.takeDirty());
	}

	@Test
	public void aSecondPickIsRefusedAndABadIndexToo()
	{
		assertFalse(game.pickStarter(3));
		assertFalse(game.pickStarter(-1));
		assertTrue(game.pickStarter(0));
		assertFalse(game.pickStarter(0));

		assertEquals(1, game.data().team.size());
	}

	@Test
	public void aCatchDuringTheOfferEndsIt()
	{
		Species goblin = table.find("Goblin");
		WildEntry entry = new WildEntry(1, goblin, 3, 5);
		CatchOutcome outcome = game.tryCatch(entry);
		for (int i = 0; i < 200 && outcome != CatchOutcome.CAUGHT; i++)
			outcome = game.tryCatch(entry);

		assertEquals(CatchOutcome.CAUGHT, outcome);
		assertTrue(game.starterOffer().isEmpty());
		assertFalse(game.pickStarter(0));
	}

	@Test
	public void noOfferWhenASpeciesIsMissingAndNoneAfterAReset()
	{
		GoGame bare = new GoGame(new SpeciesTable(Arrays.asList(new Species("Imp", 7, CombatType.MELEE))),
			new Random(1), () -> now, notice -> messages.add(notice.text()));

		assertTrue(bare.starterOffer().isEmpty());
		assertTrue(game.pickStarter(2));
		assertNull(game.resetBlocker());
		game.reset();

		assertEquals(3, game.starterOffer().size());
	}
}
