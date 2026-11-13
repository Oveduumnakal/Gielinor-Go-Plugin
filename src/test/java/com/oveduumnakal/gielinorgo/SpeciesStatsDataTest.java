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
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks the per-species stat factors in the bundled data and how a fighter uses them (issue 86). */
public class SpeciesStatsDataTest
{
	private static final double EPSILON = 1e-9;

	@Test
	public void everyFactorIsWithinAQuarterOfTheBaseline() throws IOException
	{
		for (Species species : species().all())
		{
			for (Stat stat : Stat.values())
			{
				double factor = species.factor(stat);

				assertTrue(species.name() + " " + stat, factor >= 0.75 && factor <= 1.25);
			}
		}
	}

	@Test
	public void aSpeciesWithNoStatsAndAHandBuiltOneAreTheBaseline()
	{
		Species plain = new Species("Plain", 10, CombatType.CRUSH);

		for (Stat stat : Stat.values())
			assertEquals(1.0, plain.factor(stat), 0);
	}

	@Test
	public void aFactorOutsideTheRangeIsPulledBackIn()
	{
		Species wild = new Species("Wild", 10, CombatType.CRUSH).withFactors(2.0, 0.1, 1.0, 1.1);

		assertEquals(1.25, wild.factor(Stat.HEALTH), 0);
		assertEquals(0.75, wild.factor(Stat.ATTACK), 0);
		assertEquals(1.1, wild.factor(Stat.SPEED), 0);
	}

	@Test
	public void theFourFactorsOfATypicalSpeciesMultiplyToAboutOne() throws IOException
	{
		List<Double> products = new ArrayList<>();
		for (Species species : species().all())
		{
			double product = 1.0;
			for (Stat stat : Stat.values())
				product *= species.factor(stat);

			products.add(product);
		}

		Collections.sort(products);
		double median = products.get(products.size() / 2);

		assertTrue("median product " + median, Math.abs(median - 1.0) < 0.08);
	}

	@Test
	public void pinnedSpeciesKeepTheirRecordedFactors() throws IOException
	{
		SpeciesTable table = species();

		assertFactors(table.find("Chicken"), 1.11, 1.15, 0.87, 0.75);
		assertFactors(table.find("Gnome Archer"), 1.18, 1.18, 0.76, 0.93);
		assertFactors(table.find("Rock Crab"), 1.25, 0.86, 0.75, 1.25);
		assertFactors(table.find("Zombie"), 1.06, 0.93, 1.03, 0.95);
	}

	@Test
	public void aSpeciesWithAllFactorsOneKeepsTodaysNumbers()
	{
		Combatant fighter = new Combatant(new Species("Plain", 13, CombatType.STAB), 40, 1.0);

		assertEquals(172, fighter.maxHp());
		assertEquals(43, fighter.attack());
		assertEquals(39, fighter.defence());
		assertEquals(25, fighter.speed());
		Combatant same = new Combatant(new Species("Plain", 13, CombatType.STAB).withFactors(1, 1, 1, 1), 40, 1.0);

		assertEquals(fighter.maxHp(), same.maxHp());
		assertEquals(fighter.attack(), same.attack());
		assertEquals(fighter.defence(), same.defence());
		assertEquals(fighter.speed(), same.speed());
	}

	@Test
	public void theFactorsMoveEachStatAndOnlyThatStat()
	{
		Combatant plain = new Combatant(new Species("Plain", 13, CombatType.STAB), 40, 1.0);
		Combatant shaped = new Combatant(
			new Species("Shaped", 13, CombatType.STAB).withFactors(1.25, 0.75, 1.0, 1.2), 40, 1.0);

		assertEquals(Math.round(plain.maxHp() * 1.25), shaped.maxHp(), 1);
		assertEquals(Math.round(plain.attack() * 0.75), shaped.attack(), 1);
		assertEquals(plain.defence(), shaped.defence());
		assertEquals(30, shaped.speed());
	}

	@Test
	public void aFastSpeciesAttacksBeforeASlowOneOfTheSameLevel()
	{
		Combatant fast = new Combatant(new Species("Fast", 20, CombatType.STAB).withFactors(1, 1, 1, 1.25), 30, 1.0);
		Combatant slow = new Combatant(new Species("Slow", 20, CombatType.STAB).withFactors(1, 1, 1, 0.8), 30, 1.0);

		assertTrue(fast.speed() > slow.speed());
	}

	@Test
	public void anEvolutionIsNeverWorseInAnyStatThanWhatItEvolvesFrom() throws IOException
	{
		SpeciesTable table = species();
		EvolutionTable evolutions;
		try (Reader reader = new InputStreamReader(
			EvolutionTable.class.getResourceAsStream(EvolutionTable.RESOURCE), StandardCharsets.UTF_8))
		{
			evolutions = EvolutionTable.load(new Gson(), reader, table);
		}

		int checked = 0;
		for (Species from : table.all())
		{
			EvolutionTable.Step step = evolutions.next(from.key());
			if (step == null)
				continue;

			Species to = table.find(step.to());
			for (int level = step.level(); level <= 99; level++)
			{
				Combatant before = new Combatant(from, level, 1.0);
				Combatant after = new Combatant(to, level, 1.0);
				String where = from.name() + " to " + to.name() + " at " + level;

				assertTrue(where + " health", after.maxHp() >= before.maxHp());
				assertTrue(where + " attack", after.attack() >= before.attack());
				assertTrue(where + " defence", after.defence() >= before.defence());
				assertTrue(where + " speed", after.speed() >= before.speed());
			}

			checked++;
		}

		assertTrue("links checked: " + checked, checked > 100);
	}

	@Test
	public void theCardNamesWhatASpeciesIsGoodAndBadAt()
	{
		Species shaped = new Species("Shaped", 20, CombatType.STAB).withFactors(1.2, 0.8, 1.05, 1.25);

		assertEquals("Fast", shaped.words(2).get(0));
		assertEquals("Tough", shaped.words(2).get(1));
		assertEquals(2, shaped.words(2).size());
		assertEquals(1, shaped.words(1).size());
		assertEquals("Health +20% \u00b7 Attack -20% \u00b7 Defence +5% \u00b7 Speed +25%", shaped.describeFactors());
		assertTrue(new Species("Plain", 20, CombatType.STAB).words(2).isEmpty());
		Species soft = new Species("S", 1, CombatType.STAB).withFactors(1, 0.88, 1, 1);
		Species near = new Species("N", 1, CombatType.STAB).withFactors(1, 0.89, 1, 1.11);

		assertEquals("Hits soft", soft.words(2).get(0));
		assertTrue(near.words(2).isEmpty());
	}

	private static void assertFactors(Species species, double health, double attack, double defence, double speed)
	{
		assertNotNull(species);
		assertEquals(health, species.factor(Stat.HEALTH), EPSILON);
		assertEquals(attack, species.factor(Stat.ATTACK), EPSILON);
		assertEquals(defence, species.factor(Stat.DEFENCE), EPSILON);
		assertEquals(speed, species.factor(Stat.SPEED), EPSILON);
	}

	private SpeciesTable species() throws IOException
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(SpeciesTable.RESOURCE);
		assertNotNull(SpeciesTable.RESOURCE + " is missing", in);
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return SpeciesTable.load(new Gson(), reader);
		}
	}
}
