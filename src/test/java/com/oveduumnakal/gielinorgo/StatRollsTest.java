/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the rolls: their range, the rating thresholds and the factors that scale the stats. */
public class StatRollsTest
{
	@Test
	public void tenThousandRollsStayInRangeAndReachBothEnds()
	{
		Random random = new Random(7);
		Set<Temperament> seen = EnumSet.noneOf(Temperament.class);
		int lowest = 0;
		int highest = 0;
		for (int i = 0; i < 10_000; i++)
		{
			StatRolls rolls = StatRolls.roll(random);
			assertTrue(rolls.valid());
			seen.add(rolls.temperament());
			for (int value : rolls.values())
			{
				lowest = Math.min(lowest, value);
				highest = Math.max(highest, value);
			}
		}

		assertEquals(StatRolls.MIN, lowest);
		assertEquals(StatRolls.MAX, highest);
		assertEquals(EnumSet.allOf(Temperament.class), seen);
	}

	@Test
	public void theSameSeedRollsTheSameCreature()
	{
		assertEquals(StatRolls.roll(new Random(3)), StatRolls.roll(new Random(3)));
	}

	@Test
	public void ratingFollowsTheSumOfTheFourRolls()
	{
		assertEquals(StatRolls.Rating.WEAK, rated(-2, -2, -2, -2));
		assertEquals(StatRolls.Rating.AVERAGE, rated(-2, -2, -2, -1));
		assertEquals(StatRolls.Rating.AVERAGE, rated(0, 0, 0, 0));
		assertEquals(StatRolls.Rating.AVERAGE, rated(2, 2, 2, 1));
		assertEquals(StatRolls.Rating.STRONG, rated(2, 2, 2, 2));
		assertEquals(StatRolls.Rating.STRONG, rated(5, 5, 3, 0));
		assertEquals(StatRolls.Rating.EXCEPTIONAL, rated(5, 5, 3, 1));
		assertEquals(StatRolls.Rating.EXCEPTIONAL, rated(5, 5, 5, 5));
		assertEquals(StatRolls.Rating.WEAK, rated(-5, -5, -5, -5));
	}

	@Test
	public void averageScalesNothing()
	{
		for (Stat stat : Stat.values())
			assertEquals(1.0, StatRolls.AVERAGE.factor(stat), 0.0);

		assertEquals(StatRolls.Rating.AVERAGE, StatRolls.AVERAGE.rating());
		assertTrue(StatRolls.AVERAGE.valid());
	}

	@Test
	public void eachRollPointIsOneAndAHalfPercent()
	{
		StatRolls rolls = new StatRolls(5, -5, 0, 2, Temperament.STEADY);

		assertEquals(1.075, rolls.factor(Stat.HEALTH), 1e-9);
		assertEquals(0.925, rolls.factor(Stat.ATTACK), 1e-9);
		assertEquals(1.0, rolls.factor(Stat.DEFENCE), 1e-9);
		assertEquals(1.03, rolls.factor(Stat.SPEED), 1e-9);
	}

	@Test
	public void aTemperamentRaisesOneStatAndLowersAnother()
	{
		StatRolls brave = new StatRolls(0, 0, 0, 0, Temperament.BRAVE);

		assertEquals(1.1, brave.factor(Stat.ATTACK), 1e-9);
		assertEquals(0.9, brave.factor(Stat.DEFENCE), 1e-9);
		assertEquals(1.0, brave.factor(Stat.HEALTH), 0.0);
		assertEquals(1.0, brave.factor(Stat.SPEED), 0.0);
		for (Temperament temperament : Temperament.values())
		{
			double product = 1.0;
			for (Stat stat : Stat.values())
				product *= new StatRolls(0, 0, 0, 0, temperament).factor(stat);

			assertEquals(temperament.name(), temperament == Temperament.STEADY ? 1.0 : 0.99, product, 1e-9);
		}
	}

	@Test
	public void rollsReadFromAMessageAreChecked()
	{
		assertEquals(StatRolls.AVERAGE, StatRolls.of(null, null));
		assertEquals(StatRolls.AVERAGE, StatRolls.of(new int[4], "STEADY"));
		assertNotNull(StatRolls.of(new int[] {5, -5, 0, 1}, "NIMBLE"));
		assertNull(StatRolls.of(new int[] {9, 0, 0, 0}, null));
		assertNull(StatRolls.of(new int[] {0, 0, 0, -6}, null));
		assertNull(StatRolls.of(new int[3], null));
		assertNull(StatRolls.of(new int[5], null));
		assertNull(StatRolls.of(null, "GRUMPY"));
		assertNull(StatRolls.of(null, "brave"));
		assertFalse(new StatRolls(6, 0, 0, 0, Temperament.STEADY).valid());
	}

	@Test
	public void theNumbersAreWrittenForTheTooltip()
	{
		StatRolls rolls = new StatRolls(3, -1, 0, 5, Temperament.STEADY);

		assertEquals("Health +3 · Attack -1 · Defence 0 · Speed +5", rolls.describe());
		assertArrayEquals(new int[] {3, -1, 0, 5}, rolls.values());
	}

	@Test
	public void theTooltipShowsTheNetPercentageOfRollAndTemperament()
	{
		StatRolls rolls = new StatRolls(0, 2, 0, -4, Temperament.NIMBLE);

		assertEquals("Health 0 (-10%) · Attack +2 (+3%)<br>Defence 0 · Speed -4 (+3%)", rolls.describeNet());
	}

	@Test
	public void steadyAndUnrolledCreaturesShowNoPercentages()
	{
		assertEquals("Health 0 · Attack 0<br>Defence 0 · Speed 0", StatRolls.AVERAGE.describeNet());
		assertEquals("Health +2 (+3%) · Attack 0<br>Defence 0 · Speed -4 (-6%)",
			new StatRolls(2, 0, 0, -4, Temperament.STEADY).describeNet());
	}

	@Test
	public void aTemperamentSaysWhatItDoes()
	{
		assertEquals("+10% Speed, -10% Health", Temperament.NIMBLE.effect());
		assertEquals("+10% Attack, -10% Defence", Temperament.BRAVE.effect());
		assertEquals("no effect", Temperament.STEADY.effect());
	}

	private static StatRolls.Rating rated(int hp, int attack, int defence, int speed)
	{
		return new StatRolls(hp, attack, defence, speed, Temperament.STEADY).rating();
	}
}
