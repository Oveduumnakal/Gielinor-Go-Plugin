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

/** Checks the per-species stat factors in the bundled data (issue 86). */
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
