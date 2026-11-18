/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks that every easing curve starts at exactly 0 and ends at exactly 1, and how far the soft pop overshoots. */
public class EasingTest
{
	@Test
	public void everyCurveStartsAtZeroAndEndsAtOne()
	{
		for (Easing easing : Easing.values())
		{
			assertEquals(easing.name(), 0.0, easing.apply(0.0), 0.0);
			assertEquals(easing.name(), 1.0, easing.apply(1.0), 0.0);
		}
	}

	@Test
	public void theSoftPopGoesPastOneByAtMostFourPercent()
	{
		double peak = 0;
		for (int i = 1; i < 1000; i++)
			peak = Math.max(peak, Easing.OUT_BACK_SOFT.apply(i / 1000.0));

		assertTrue("peak " + peak, peak > 1.0);
		assertTrue("peak " + peak, peak <= 1.04);
		assertEquals(1.037, peak, 0.002);
	}

	@Test
	public void theSoftPopIsGentlerThanTheFullPop()
	{
		assertTrue(Easing.OUT_BACK_SOFT.apply(0.7) < Easing.OUT_BACK.apply(0.7));
	}
}
