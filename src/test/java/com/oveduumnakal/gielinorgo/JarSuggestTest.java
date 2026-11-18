/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks which owned jars the Catch list suggests. */
public class JarSuggestTest
{
	private static JarSuggest.JarChoice tier(JarType jar, double chance)
	{
		return new JarSuggest.JarChoice(jar, 3, chance, false);
	}

	private static JarSuggest.JarChoice match(JarType jar, double chance)
	{
		return new JarSuggest.JarChoice(jar, 2, chance, true);
	}

	private static JarSuggest.JarChoice other(JarType jar, double chance)
	{
		return new JarSuggest.JarChoice(jar, 2, chance, false);
	}

	private static List<JarType> jars(List<JarSuggest.JarChoice> choices)
	{
		List<JarType> list = new ArrayList<>();
		for (JarSuggest.JarChoice choice : choices)
			list.add(choice.jar());

		return list;
	}

	@Test
	public void aWeakCreatureSuggestsTheTwoCheapestJars()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.DRAGON, 0.95), tier(JarType.RUNE, 0.95), tier(JarType.IRON, 0.95),
			tier(JarType.BRONZE, 0.95)));

		assertEquals(Arrays.asList(JarType.BRONZE, JarType.IRON), jars(split.suggested()));
		assertEquals(Arrays.asList(JarType.RUNE, JarType.DRAGON), jars(split.others()));
	}

	@Test
	public void aMatchingSpecialtyJarLeadsAWeakCreature()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.BRONZE, 0.95), tier(JarType.IRON, 0.95), match(JarType.SLASH, 0.95)));

		assertEquals(Arrays.asList(JarType.SLASH, JarType.BRONZE), jars(split.suggested()));
		assertEquals(Collections.singletonList(JarType.IRON), jars(split.others()));
	}

	@Test
	public void aStrongCreatureSuggestsTheBestOddsAndTheNearOne()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.BRONZE, 0.38), tier(JarType.IRON, 0.56), tier(JarType.RUNE, 0.75),
			tier(JarType.DRAGON, 0.95), match(JarType.SLASH, 0.94)));

		assertEquals(Arrays.asList(JarType.DRAGON, JarType.SLASH), jars(split.suggested()));
		assertEquals(Arrays.asList(JarType.RUNE, JarType.IRON, JarType.BRONZE), jars(split.others()));
	}

	@Test
	public void aSingleJarIsSuggestedAlone()
	{
		JarSuggest.Split split = JarSuggest.split(Collections.singletonList(tier(JarType.IRON, 0.5)));

		assertEquals(Collections.singletonList(JarType.IRON), jars(split.suggested()));
		assertTrue(split.others().isEmpty());
	}

	@Test
	public void noJarsGiveTwoEmptyLists()
	{
		JarSuggest.Split split = JarSuggest.split(Collections.emptyList());

		assertTrue(split.suggested().isEmpty());
		assertTrue(split.others().isEmpty());
	}

	@Test
	public void aTieOnTheShownPercentIsBrokenBySpendRankNotByTheRawChance()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.RUNE, 0.951), tier(JarType.IRON, 0.949)));

		assertEquals(Arrays.asList(JarType.IRON, JarType.RUNE), jars(split.suggested()));
	}

	@Test
	public void aNonMatchingSpecialtyJarTrailsBronzeAtEqualOdds()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			other(JarType.CAVE, 0.60), tier(JarType.BRONZE, 0.60), tier(JarType.DRAGON, 0.60)));

		assertEquals(Arrays.asList(JarType.BRONZE, JarType.DRAGON), jars(split.suggested()));
		assertEquals(Collections.singletonList(JarType.CAVE), jars(split.others()));
	}

	@Test
	public void aJarSixPointsUnderTheBestIsNotSuggested()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.DRAGON, 0.90), tier(JarType.RUNE, 0.84)));

		assertEquals(Collections.singletonList(JarType.DRAGON), jars(split.suggested()));
		assertEquals(Collections.singletonList(JarType.RUNE), jars(split.others()));
	}

	@Test
	public void aJarFourPointsUnderTheBestIsSuggested()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.DRAGON, 0.90), tier(JarType.RUNE, 0.86)));

		assertEquals(Arrays.asList(JarType.DRAGON, JarType.RUNE), jars(split.suggested()));
	}

	@Test
	public void aJarExactlyFivePointsUnderIsSuggested()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.DRAGON, 0.90), tier(JarType.RUNE, 0.85)));

		assertEquals(2, split.suggested().size());
	}

	@Test
	public void atMostTwoAreSuggestedEvenWhenMoreAreNear()
	{
		JarSuggest.Split split = JarSuggest.split(Arrays.asList(
			tier(JarType.BRONZE, 0.80), tier(JarType.IRON, 0.80), tier(JarType.RUNE, 0.80),
			tier(JarType.DRAGON, 0.80)));

		assertEquals(JarSuggest.MAX_SUGGESTED, split.suggested().size());
		assertEquals(2, split.others().size());
	}

	@Test
	public void theInputListIsNotChanged()
	{
		List<JarSuggest.JarChoice> input = new ArrayList<>(Arrays.asList(
			tier(JarType.DRAGON, 0.95), tier(JarType.BRONZE, 0.50)));
		List<JarSuggest.JarChoice> copy = new ArrayList<>(input);

		JarSuggest.split(input);

		assertEquals(copy, input);
	}

	@Test
	public void pctRoundsTheChance()
	{
		assertEquals(95, tier(JarType.BRONZE, 0.949).pct());
		assertEquals(5, tier(JarType.BRONZE, 0.05).pct());
	}
}
