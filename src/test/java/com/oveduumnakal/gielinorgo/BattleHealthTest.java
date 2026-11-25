/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** The health that travels with battle lines to the bars under the names. */
public class BattleHealthTest
{
	private static final double CLOSE = 1e-9;

	@Test
	public void aBeatKeepsTheHealthOfItsLine()
	{
		BattleLine hit = new BattleLine("-3", Tone.GOOD, false, BattleLine.Kind.LEAD_HIT)
			.withHealth(new BattleHealth(0, 1.0, 0.8, 1.0, 0.5));
		BattleLine closing = new BattleLine("Won", Tone.GOOD, true, BattleLine.Kind.CLOSING);

		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(Arrays.asList(hit, closing));

		BattleTimeline.Step first = steps.get(0);
		assertEquals(0.8, first.health().foeBefore(), CLOSE);
		assertEquals(0.5, first.health().foe(), CLOSE);
		BattleTimeline.Step last = steps.get(1);
		assertNull(last.health());
	}

	@Test
	public void aBarShowsASliverUntilTheHealthIsGone()
	{
		assertEquals(40, FloatingTextOverlay.filled(1.0, 40));
		assertEquals(20, FloatingTextOverlay.filled(0.5, 40));
		assertEquals(1, FloatingTextOverlay.filled(0.001, 40));
		assertEquals(0, FloatingTextOverlay.filled(0.0, 40));
		assertEquals(40, FloatingTextOverlay.filled(1.5, 40));
	}
}
