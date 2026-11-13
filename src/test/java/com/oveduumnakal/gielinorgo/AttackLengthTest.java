/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks how long a beat of an acted-out battle lasts for the attack in it. */
public class AttackLengthTest
{
	private static final int BEAT = BattleTimeline.BEAT_TICKS;

	@Test
	public void frameLengthsAddUp()
	{
		assertEquals(0, AttackLength.clientTicks(null));
		assertEquals(0, AttackLength.clientTicks(new int[0]));
		assertEquals(12, AttackLength.clientTicks(new int[] {5, 4, 3}));
		assertEquals(9, AttackLength.clientTicks(new int[] {5, -1, 4}));
	}

	@Test
	public void aShortAttackKeepsTheNormalBeat()
	{
		assertEquals(BEAT, AttackLength.beatTicks(true, 24, 15, true));
		assertEquals(BEAT, AttackLength.beatTicks(false, 40, 30, true));
		assertEquals(BEAT, AttackLength.beatTicks(true, 0, 0, true));
	}

	@Test
	public void aLongMeleeSwingOutlastsTheBeat()
	{
		int swing = 100;
		int ends = TurnMotion.STRIKE_TICK + swing;
		int expected = (ends + TurnMotion.CLIENT_TICKS_PER_GAME_TICK - 1) / TurnMotion.CLIENT_TICKS_PER_GAME_TICK;

		assertEquals(expected, AttackLength.beatTicks(true, swing, 10, true));
		assertTrue(expected > BEAT);
	}

	@Test
	public void aLongCastStartsAtOnceAndEndsSooner()
	{
		assertEquals(4, AttackLength.beatTicks(false, 110, 10, true));
	}

	@Test
	public void aLongHitReactionCountsUnlessTheAttackMissed()
	{
		assertEquals(5, AttackLength.beatTicks(false, 10, 120, true));
		assertEquals(BEAT, AttackLength.beatTicks(false, 10, 120, false));
	}

	@Test
	public void aBeatNeverPassesTheCap()
	{
		assertEquals(AttackLength.MAX_BEAT_TICKS, AttackLength.beatTicks(true, 5000, 5000, true));
	}

	@Test
	public void aStatusFlinchAloneCanLengthenItsBeat()
	{
		assertEquals(BEAT, AttackLength.flinchTicks(20));
		assertEquals(4, AttackLength.flinchTicks(120));
	}
}
