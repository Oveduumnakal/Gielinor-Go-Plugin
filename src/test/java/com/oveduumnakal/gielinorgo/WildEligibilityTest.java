/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks which monsters may turn wild, including the Hunter creatures that have no combat level (issue #207). */
public class WildEligibilityTest
{
	private static final int FOX_ID = 13138;

	private static final int PET_FOX_ID = 12548;

	private static Species pyreFox()
	{
		return SpeciesTable.load(new Gson(), new StringReader(
			"[{\"name\":\"Pyre fox\",\"level\":57,\"style\":\"SLASH\",\"traits\":[\"FIERY\"],"
				+ "\"ids\":[13138],\"hunter\":true}]"))
			.find("Pyre fox");
	}

	@Test
	public void anOrdinarySpeciesNeedsACombatLevel()
	{
		Species goblin = new Species("Goblin", 13, CombatType.STAB);

		assertFalse(WildEligibility.eligible(goblin, 3029, 0));
		assertTrue(WildEligibility.eligible(goblin, 3029, 1));
		assertTrue(WildEligibility.eligible(goblin, 3029, 13));
	}

	@Test
	public void aHunterSpeciesIsAcceptedByItsOwnNpcIdWithNoCombatLevel()
	{
		Species fox = pyreFox();

		assertTrue(fox.hunter());
		assertTrue(WildEligibility.eligible(fox, FOX_ID, 0));
	}

	@Test
	public void aHunterSpeciesIsNotAcceptedForAnotherNpcWithTheSameName()
	{
		Species fox = pyreFox();

		assertFalse(WildEligibility.eligible(fox, PET_FOX_ID, 0));
		assertFalse(WildEligibility.eligible(fox, PET_FOX_ID, 19));
	}

	@Test
	public void aQuestOnlyOrMissingSpeciesIsNeverAccepted()
	{
		Species questFox = new Species("Pyre fox", 57, CombatType.SLASH).asHunter().asQuestOnly();

		assertFalse(WildEligibility.eligible(questFox, FOX_ID, 0));
		assertFalse(WildEligibility.eligible(new Species("Goblin", 13, CombatType.STAB).asQuestOnly(), 1, 13));
		assertFalse(WildEligibility.eligible(null, FOX_ID, 20));
	}

	@Test
	public void theLevelOfAHunterSpeciesIsItsHunterLevel()
	{
		assertEquals(57, WildEligibility.combatLevel(pyreFox(), 0));
		assertEquals(13, WildEligibility.combatLevel(new Species("Goblin", 13, CombatType.STAB), 13));
		assertEquals(7, WildEligibility.combatLevel(null, 7));
	}

	@Test
	public void aSpeciesKnowsItsNpcIds()
	{
		Species fox = pyreFox();

		assertTrue(fox.hasNpcId(FOX_ID));
		assertFalse(fox.hasNpcId(PET_FOX_ID));
		assertFalse(new Species("Goblin", 13, CombatType.STAB).hasNpcId(0));
	}
}
