/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/** Checks which ground a wild creature counts as standing on. */
public class WildFootprintTest
{
	@Test
	public void aBodyOnATileGivesThatTilesMiddle()
	{
		assertArrayEquals(new int[] {5 * 128 + 64, 7 * 128 + 64}, WildFootprint.centre(105, 207, 1, 100, 200));
	}

	@Test
	public void aBodyPartWayBetweenTilesStaysOnTheNearerTile()
	{
		assertArrayEquals(new int[] {5 * 128 + 64, 7 * 128 + 64}, WildFootprint.centre(105.3, 206.8, 1, 100, 200));
		assertArrayEquals(new int[] {6 * 128 + 64, 7 * 128 + 64}, WildFootprint.centre(105.6, 207.2, 1, 100, 200));
	}

	@Test
	public void aWiderBodyKeepsItsOwnCentre()
	{
		assertArrayEquals(new int[] {5 * 128 + 64 + 32, 7 * 128 + 64}, WildFootprint.centre(105.25, 207, 2, 100, 200));
	}
}
