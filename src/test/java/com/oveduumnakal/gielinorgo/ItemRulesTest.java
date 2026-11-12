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
import static org.junit.Assert.assertTrue;

/** Checks the item drop chances and the rules for using an item outside battle. */
public class ItemRulesTest
{
	private static final double EPSILON = 1e-9;

	@Test
	public void theXpChanceGrowsWithTheDropAndIsCapped()
	{
		assertEquals(0.0, ItemRules.xpChance(0), EPSILON);
		assertEquals(0.0, ItemRules.xpChance(-5), EPSILON);
		assertEquals(0.05, ItemRules.xpChance(1_500), EPSILON);
		assertEquals(ItemRules.MAX_XP_CHANCE, ItemRules.xpChance(1_000_000), EPSILON);
		assertEquals(ItemRules.MAX_XP_CHANCE, ItemRules.xpChance(9_000), EPSILON);
	}

	@Test
	public void herbloreAndCookingFindSalvesAndHerbloreFindsAntidotes()
	{
		assertEquals(Arrays.asList(ItemType.SMALL_SALVE, ItemType.ANTIDOTE), ItemType.forSkill("HERBLORE"));
		assertEquals(Arrays.asList(ItemType.SMALL_SALVE, ItemType.ANTIDOTE), ItemType.forSkill("herblore"));
		assertEquals(Collections.singletonList(ItemType.SMALL_SALVE), ItemType.forSkill("COOKING"));
	}

	@Test
	public void hitpointsAndUnrelatedSkillsFindNothing()
	{
		assertTrue(ItemType.forSkill("HITPOINTS").isEmpty());
		assertTrue(ItemType.forSkill("MINING").isEmpty());
		assertTrue(ItemType.forSkill(null).isEmpty());
	}

	@Test
	public void battleWinsDropSalvesAndRevivesButNeverAntidotes()
	{
		assertEquals(0.10, ItemRules.winChance(ItemType.SMALL_SALVE), EPSILON);
		assertEquals(0.03, ItemRules.winChance(ItemType.LARGE_SALVE), EPSILON);
		assertEquals(0.02, ItemRules.winChance(ItemType.REVIVE), EPSILON);
		assertEquals(0.0, ItemRules.winChance(ItemType.ANTIDOTE), EPSILON);
	}

	@Test
	public void aSalveNeedsAHurtCreatureAndAReviveNeedsATiredOne()
	{
		assertTrue(ItemRules.appliesOutsideBattle(ItemType.SMALL_SALVE, 0.99));
		assertFalse(ItemRules.appliesOutsideBattle(ItemType.SMALL_SALVE, 1.0));
		assertTrue(ItemRules.appliesOutsideBattle(ItemType.REVIVE, 0.24));
		assertFalse(ItemRules.appliesOutsideBattle(ItemType.REVIVE, GoGame.MIN_BATTLE_HP));
		assertFalse(ItemRules.appliesOutsideBattle(ItemType.ANTIDOTE, 0.0));
	}

	@Test
	public void healthAfterAnItem()
	{
		assertEquals(0.5, ItemRules.healthAfter(ItemType.SMALL_SALVE, 0.25), EPSILON);
		assertEquals(1.0, ItemRules.healthAfter(ItemType.LARGE_SALVE, 0.7), EPSILON);
		assertEquals(0.5, ItemRules.healthAfter(ItemType.REVIVE, 0.1), EPSILON);
		assertEquals(0.8, ItemRules.healthAfter(ItemType.REVIVE, 0.8), EPSILON);
	}
}
