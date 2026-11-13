/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the one-sound-at-a-time rule and the gilded exception. */
public class CueGateTest
{
	@Test
	public void twoCuesWithinTheQuietTimePlayOnlyTheFirst()
	{
		CueGate gate = new CueGate();

		assertTrue(gate.allow(Cue.CATCH, 1000));
		assertFalse(gate.allow(Cue.LEVEL_UP, 1000 + CueGate.QUIET_MS - 1));
	}

	@Test
	public void theNextCueAfterTheQuietTimePlays()
	{
		CueGate gate = new CueGate();
		gate.allow(Cue.CATCH, 1000);

		assertTrue(gate.allow(Cue.LEVEL_UP, 1000 + CueGate.QUIET_MS));
	}

	@Test
	public void theGildedCueAlwaysPlays()
	{
		CueGate gate = new CueGate();
		gate.allow(Cue.CATCH, 1000);

		assertTrue(gate.allow(Cue.GILDED, 1001));
		assertFalse("and it counts as the last sound", gate.allow(Cue.HATCH, 1002));
	}

	@Test
	public void theStrongerCueWinsAndEveryCueHasASound()
	{
		assertEquals(Cue.EVOLVE, Cue.stronger(Cue.LEVEL_UP, Cue.EVOLVE));
		assertEquals(Cue.EVOLVE, Cue.stronger(Cue.EVOLVE, Cue.LEVEL_UP));
		assertEquals(Cue.CATCH, Cue.stronger(null, Cue.CATCH));
		for (Cue cue : Cue.values())
			assertTrue(cue.soundId() > 0);
	}
}
