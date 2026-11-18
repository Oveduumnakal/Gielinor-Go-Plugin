/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks that the row model carries every badge the side panel's card shows, defines each one once, and words the
 * name and the tooltip the way the plan says.
 */
public class RowBadgesTest
{
	private static final Move SPARK = new Move("spark", "Spark", CombatType.MAGIC, 1.2, 0.9);

	private static CreatureView view(double hp, boolean gilded, String nickname, StatRolls rolls, String waiting,
		boolean pending)
	{
		return new CreatureView("Goblin", 12, 0.5, hp, Rarity.EPIC, CombatType.SLASH, gilded, nickname, rolls, 0,
			Collections.singletonList(SPARK), pending ? Collections.singletonList(SPARK) : Collections.emptyList(), 2,
			waiting);
	}

	private static CreatureView plain()
	{
		return view(1.0, false, null, StatRolls.AVERAGE, null, false);
	}

	@Test
	public void onlyTheFirstRowIsTheLead()
	{
		assertTrue(RowModel.of(plain(), 0).lead());
		assertTrue(RowModel.of(plain(), 0).has(RowModel.Badge.LEAD));
		assertFalse(RowModel.of(plain(), 1).lead());
		assertFalse(RowModel.of(plain(), 5).has(RowModel.Badge.LEAD));
	}

	@Test
	public void aWaitingMoveGivesTheGoldStarAndOnlyThen()
	{
		assertTrue(RowModel.of(view(1.0, false, null, StatRolls.AVERAGE, null, true), 2).newMove());
		assertTrue(RowModel.of(view(1.0, false, null, StatRolls.AVERAGE, null, true), 2)
			.has(RowModel.Badge.NEW_MOVE));
		assertFalse(RowModel.of(plain(), 2).newMove());
	}

	@Test
	public void aCreatureWaitingToEvolveCarriesWhatItWaitsFor()
	{
		RowModel row = RowModel.of(view(1.0, false, null, StatRolls.AVERAGE, "Evolves at night", false), 3);

		assertTrue(row.has(RowModel.Badge.WAITING));
		assertTrue(row.tags().contains("Evolves at night"));
		assertFalse(RowModel.of(plain(), 3).has(RowModel.Badge.WAITING));
	}

	@Test
	public void aGildedCreatureHasTheGoldFrame()
	{
		assertTrue(RowModel.of(view(1.0, true, null, StatRolls.AVERAGE, null, false), 1).gilded());
		assertTrue(RowModel.of(view(1.0, true, null, StatRolls.AVERAGE, null, false), 1).has(RowModel.Badge.GILDED));
		assertFalse(RowModel.of(plain(), 1).gilded());
	}

	@Test
	public void theRatingBadgeIsCarriedUnlessTheRollsAreAverage()
	{
		StatRolls strong = new StatRolls(5, 5, 0, 0, Temperament.STEADY);
		StatRolls weak = new StatRolls(-5, -5, 0, 0, Temperament.STEADY);

		assertTrue(RowModel.of(view(1.0, false, null, strong, null, false), 1).has(RowModel.Badge.RATING));
		assertEquals("Strong", RowModel.of(view(1.0, false, null, strong, null, false), 1).rating());
		assertEquals("Weak", RowModel.of(view(1.0, false, null, weak, null, false), 1).rating());
		assertFalse(RowModel.of(plain(), 1).has(RowModel.Badge.RATING));
	}

	@Test
	public void theRarityIsCarriedForTheStripe()
	{
		assertEquals(Rarity.EPIC, RowModel.of(plain(), 1).rarity());
	}

	@Test
	public void theRowShowsTheNicknameAndTheSpeciesWhenThereIsNone()
	{
		RowModel named = RowModel.of(view(1.0, false, "Gobbo", StatRolls.AVERAGE, null, false), 1);
		RowModel unnamed = RowModel.of(plain(), 1);

		assertEquals("Gobbo", named.name());
		assertEquals("Goblin", named.species());
		assertTrue(named.nicknamed());
		assertEquals("Goblin", unnamed.name());
		assertFalse(unnamed.nicknamed());
	}

	@Test
	public void theTooltipNamesTheFullNameTheSpeciesAndTheRarityWord()
	{
		RowModel named = RowModel.of(view(1.0, true, "Gobbo", StatRolls.AVERAGE, null, true), 0);
		String tip = named.tooltip();

		assertTrue(tip, tip.startsWith("Gobbo (Goblin)"));
		assertTrue(tip, tip.contains("Epic"));
		assertTrue(tip, tip.contains("Gilded"));
		assertTrue(tip, tip.contains("Your lead"));
		assertTrue(tip, tip.contains("Wants to learn Spark"));
		assertTrue(tip, tip.contains("Health 100%"));
	}

	@Test
	public void genderIsHiddenWhenTheViewHasNone()
	{
		assertNull(RowModel.of(plain(), 1).gender());
		assertEquals(Gender.MALE, RowModel.of(plain().withGender(Gender.MALE), 1)
			.gender());
	}

	@Test
	public void oneTagIsChosenForARowAndAStateWordComesFirst()
	{
		CreatureView resting = view(0.1, false, null, new StatRolls(5, 5, 0, 0, Temperament.STEADY),
			"Evolves at night", false);

		assertEquals("Resting", PartyPainter.tag(RowModel.of(resting, 0)));
		assertEquals("Fainted", PartyPainter.tag(RowModel.of(resting.withFainted(true), 0)));
		assertEquals("Waiting", PartyPainter.tag(RowModel.of(view(1.0, false, null, StatRolls.AVERAGE,
			"Evolves at night", false), 0)));
		assertEquals("Strong", PartyPainter.tag(RowModel.of(view(1.0, false, null,
			new StatRolls(5, 5, 0, 0, Temperament.STEADY), null, false), 0)));
		assertEquals("Lead", PartyPainter.tag(RowModel.of(plain(), 0)));
		assertEquals("", PartyPainter.tag(RowModel.of(plain(), 1)));
	}

	@Test
	public void theBadgesComeInTheOrderTheyAreDrawn()
	{
		StatRolls rolls = new StatRolls(5, 5, 0, 0, Temperament.STEADY);
		RowModel row = RowModel.of(view(0.1, true, null, rolls, "Evolves at night", true), 0);

		assertEquals(Arrays.asList(RowModel.Badge.LEAD, RowModel.Badge.NEW_MOVE, RowModel.Badge.RESTING,
			RowModel.Badge.WAITING, RowModel.Badge.RATING, RowModel.Badge.GILDED), row.badges());
	}
}
