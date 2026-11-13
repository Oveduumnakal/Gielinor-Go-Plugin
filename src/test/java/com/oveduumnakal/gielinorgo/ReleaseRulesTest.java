/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks what a release pays and which creatures ask twice. */
public class ReleaseRulesTest
{
	@Test
	public void theRewardGrowsWithLevelAndRarity()
	{
		assertEquals(60, ReleaseRules.xp(10, Rarity.COMMON));
		assertEquals(90, ReleaseRules.xp(10, Rarity.UNCOMMON));
		assertEquals(120, ReleaseRules.xp(10, Rarity.RARE));
		assertEquals(780, ReleaseRules.xp(60, Rarity.EPIC));
		assertEquals(240, ReleaseRules.xp(10, Rarity.LEGENDARY));
	}

	@Test
	public void gildedRatedOrNamedCreaturesAskASecondTime()
	{
		assertTrue(ReleaseRules.needsSecondConfirm(true, false, StatRolls.Rating.AVERAGE));
		assertTrue(ReleaseRules.needsSecondConfirm(false, true, StatRolls.Rating.AVERAGE));
		assertTrue(ReleaseRules.needsSecondConfirm(false, false, StatRolls.Rating.STRONG));
		assertTrue(ReleaseRules.needsSecondConfirm(false, false, StatRolls.Rating.EXCEPTIONAL));
		assertFalse(ReleaseRules.needsSecondConfirm(false, false, StatRolls.Rating.AVERAGE));
		assertFalse(ReleaseRules.needsSecondConfirm(false, false, StatRolls.Rating.WEAK));
	}
}
