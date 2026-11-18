/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** The rules for what can be pinned to the tracker. */
public class PinRulesTest
{
	@Test
	public void theTrackerHoldsFivePins()
	{
		assertNull(PinRules.canPin(0, false, false));
		assertNull(PinRules.canPin(4, false, false));
		assertEquals("Tracker is full (5)", PinRules.canPin(5, false, false));
		assertEquals("Tracker is full (5)", PinRules.canPin(6, false, false));
	}

	@Test
	public void unpinningIsAlwaysAllowed()
	{
		assertNull(PinRules.canPin(5, true, false));
		assertNull(PinRules.canPin(5, true, true));
	}

	@Test
	public void aFinishedTaskCannotBePinned()
	{
		assertEquals("Already done", PinRules.canPin(0, false, true));
		assertNotNull(PinRules.canPin(5, false, true));
	}

	@Test
	public void theDoneLineStaysForFourSeconds()
	{
		assertEquals(4000L, PinRules.DONE_HOLD_MS);
		assertEquals(5, PinRules.MAX_PINS);
	}
}
