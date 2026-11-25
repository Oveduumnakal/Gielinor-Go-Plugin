/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the quest-only machinery that stays in the code although no bundled species uses it now. */
public class QuestOnlySpeciesTest
{
	private final Species chicken = new Species("Chicken", 1, CombatType.STAB);
	private final Species hen = new Species("Hen", 5, CombatType.STAB);
	private final Species ghost = new Species("Ghost", 20, CombatType.MAGIC).asQuestOnly();
	private final SpeciesTable table = new SpeciesTable(Arrays.asList(chicken, hen, ghost));

	@Test
	public void aQuestOnlySpeciesIsKnownButNotCatchable()
	{
		assertTrue(ghost.questOnly());
		assertNotNull(table.find("ghost"));
		assertEquals(2, table.catchableSize());
		assertFalse(table.catchable().contains(ghost));
		assertTrue(table.all().contains(ghost));
	}

	@Test
	public void aQuestOnlySpeciesNeverTurnsWild()
	{
		assertFalse(WildEligibility.eligible(ghost, 1, 20));
		assertTrue(WildEligibility.eligible(chicken, 1, 1));
	}

	@Test
	public void aQuestOnlySpeciesIsSkippedAsAnEvolutionTarget()
	{
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(new EvolutionTable.Step("chicken", "ghost", 10),
			new EvolutionTable.Step("chicken", "hen", 10)), table);

		assertEquals(1, evolutions.steps("chicken").size());
		assertEquals("hen", Species.keyFor(evolutions.next("chicken").to()));
		assertNull(evolutions.previous("ghost"));
	}
}
