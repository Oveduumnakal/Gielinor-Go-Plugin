/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** When the tracker shows, and how big it draws. */
public class TrackerRulesTest
{
	@Test
	public void itShowsOnlyWhenEnabledOutsideABattleLoadedAndWithALine()
	{
		assertTrue(TrackerRules.visible(true, false, true, 1));
		assertFalse(TrackerRules.visible(false, false, true, 1));
		assertFalse(TrackerRules.visible(true, true, true, 1));
		assertFalse(TrackerRules.visible(true, false, false, 1));
		assertFalse(TrackerRules.visible(true, false, true, 0));
	}

	@Test
	public void theScaleStaysSmallOnABigWindow()
	{
		assertEquals(TrackerRules.MAX_SCALE, TrackerRules.scale(2.5), 1e-9);
		assertEquals(WindowFit.MIN_SCALE, TrackerRules.scale(0.1), 1e-9);
		assertEquals(0.8, TrackerRules.scale(0.8), 1e-9);
	}
}
