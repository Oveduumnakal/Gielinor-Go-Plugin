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

/** Pins the chart of the five combat styles (issue 85). */
public class CombatTypeTest
{
	private static final CombatType STAB = CombatType.STAB;
	private static final CombatType SLASH = CombatType.SLASH;
	private static final CombatType CRUSH = CombatType.CRUSH;
	private static final CombatType RANGED = CombatType.RANGED;
	private static final CombatType MAGIC = CombatType.MAGIC;

	@Test
	public void theChartIsTheOneTheOwnerConfirmed()
	{
		assertEquals(Arrays.asList(SLASH, RANGED), STAB.strongAgainst());
		assertEquals(Arrays.asList(CRUSH, RANGED), SLASH.strongAgainst());
		assertEquals(Arrays.asList(STAB, MAGIC), CRUSH.strongAgainst());
		assertEquals(Arrays.asList(CRUSH, MAGIC), RANGED.strongAgainst());
		assertEquals(Arrays.asList(STAB, SLASH), MAGIC.strongAgainst());
		assertEquals(Arrays.asList(CRUSH, MAGIC), STAB.weakAgainst());
		assertEquals(Arrays.asList(STAB, MAGIC), SLASH.weakAgainst());
		assertEquals(Arrays.asList(SLASH, RANGED), CRUSH.weakAgainst());
		assertEquals(Arrays.asList(STAB, SLASH), RANGED.weakAgainst());
		assertEquals(Arrays.asList(CRUSH, RANGED), MAGIC.weakAgainst());
	}

	@Test
	public void everyStyleBeatsExactlyTwoAndLosesToExactlyTwo()
	{
		for (CombatType type : CombatType.values())
		{
			assertEquals(type.name(), 2, type.strongAgainst().size());
			assertEquals(type.name(), 2, type.weakAgainst().size());
			assertFalse(type.name(), type.beats(type));
			assertTrue(type.name(), Collections.disjoint(type.strongAgainst(), type.weakAgainst()));
		}
	}

	@Test
	public void ifOneBeatsTheOtherTheOtherDoesNotBeatItBack()
	{
		for (CombatType a : CombatType.values())
		{
			for (CombatType b : CombatType.values())
			{
				if (a.beats(b))
					assertFalse(a + " and " + b, b.beats(a));
			}
		}
	}

	@Test
	public void theMultipliersAreOneAndAQuarterAndFourFifths()
	{
		assertEquals(1.25, STAB.multiplierAgainst(SLASH), 0);
		assertEquals(0.8, SLASH.multiplierAgainst(STAB), 0);
		assertEquals(1.0, STAB.multiplierAgainst(STAB), 0);
		assertEquals(1.25, CRUSH.multiplierAgainst(MAGIC), 0);
		assertEquals(0.8, MAGIC.multiplierAgainst(CRUSH), 0);
	}

	@Test
	public void onlyStabSlashAndCrushWalkUpToTheirTarget()
	{
		assertTrue(STAB.melee());
		assertTrue(SLASH.melee());
		assertTrue(CRUSH.melee());
		assertFalse(RANGED.melee());
		assertFalse(MAGIC.melee());
	}

	@Test
	public void theMatchupSentenceNamesBothPairs()
	{
		assertEquals("Slash beats Crush and Ranged, loses to Stab and Magic", SLASH.matchup());
	}
}
