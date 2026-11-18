/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks the OSRS level table that offline XP uses to count levels gained while away. */
public class OsrsLevelsTest
{
	@Test
	public void zeroAndOneAreLevelOne()
	{
		assertEquals(1, OsrsLevels.levelForXp(0));
		assertEquals(1, OsrsLevels.levelForXp(1));
	}

	@Test
	public void theTableMatchesTheGame()
	{
		assertEquals(1, OsrsLevels.levelForXp(82));
		assertEquals(2, OsrsLevels.levelForXp(83));
		assertEquals(10, OsrsLevels.levelForXp(1_154));
		assertEquals(9, OsrsLevels.levelForXp(1_153));
		assertEquals(50, OsrsLevels.levelForXp(101_333));
		assertEquals(92, OsrsLevels.levelForXp(6_517_253));
		assertEquals(98, OsrsLevels.levelForXp(13_034_430));
		assertEquals(99, OsrsLevels.levelForXp(13_034_431));
	}

	@Test
	public void theLevelIsCappedAtNinetyNine()
	{
		assertEquals(99, OsrsLevels.levelForXp(200_000_000));
	}

	@Test
	public void negativeXpIsLevelOne()
	{
		assertEquals(1, OsrsLevels.levelForXp(-5));
	}
}
