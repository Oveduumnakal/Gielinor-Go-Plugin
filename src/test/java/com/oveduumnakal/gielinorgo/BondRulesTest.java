/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks the heart thresholds and the XP bonus of a bond. */
public class BondRulesTest
{
	@Test
	public void heartsComeAtTheThresholds()
	{
		int[] points = {0, 99, 100, 399, 400, 1199, 1200, 2999, 3000, 5999, 6000, 99_999};
		int[] hearts = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5};
		for (int i = 0; i < points.length; i++)
			assertEquals("points " + points[i], hearts[i], BondRules.hearts(points[i]));
	}

	@Test
	public void xpBonusStartsAtThreeHearts()
	{
		int[] expected = {0, 0, 0, 2, 4, 5};
		for (int hearts = 0; hearts < expected.length; hearts++)
			assertEquals(expected[hearts], BondRules.xpBonus(hearts));

		assertEquals(0, BondRules.xpBonus(-1));
		assertEquals(5, BondRules.xpBonus(9));
		assertEquals(102, BondRules.withBonus(100, 3));
		assertEquals(100, BondRules.withBonus(100, 2));
	}

	@Test
	public void theTooltipSaysWhatTheHeartsGive()
	{
		assertEquals("Bond 3 of 5: +2% XP. Grows as your lead", BondRules.describe(3));
		assertEquals("Bond 1 of 5. Grows as your lead", BondRules.describe(1));
	}
}
