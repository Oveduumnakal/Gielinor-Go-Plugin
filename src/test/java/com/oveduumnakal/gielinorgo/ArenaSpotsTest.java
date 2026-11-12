/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** Checks where the opponent of a team battle may stand. */
public class ArenaSpotsTest
{
	private static final int SOUTH = 0;
	private static final int WEST = 512;
	private static final int NORTH = 1024;
	private static final int EAST = 1536;

	@Test
	public void theFirstChoiceIsThreeTilesInTheWayThePlayerFaces()
	{
		assertArrayEquals(new int[]{0, -3}, ArenaSpots.candidates(SOUTH)[0]);
		assertArrayEquals(new int[]{-3, 0}, ArenaSpots.candidates(WEST)[0]);
		assertArrayEquals(new int[]{0, 3}, ArenaSpots.candidates(NORTH)[0]);
		assertArrayEquals(new int[]{3, 0}, ArenaSpots.candidates(EAST)[0]);
		assertArrayEquals(new int[]{3, 3}, ArenaSpots.candidates(FollowerPath.angle(1, 1))[0]);
	}

	@Test
	public void allEightDirectionsAreOfferedOnceNearestFirst()
	{
		int[][] spots = ArenaSpots.candidates(NORTH);
		Set<String> seen = new HashSet<>();
		for (int[] spot : spots)
			seen.add(spot[0] + "," + spot[1]);

		assertEquals(8, seen.size());
		assertArrayEquals(new int[]{0, 3}, spots[0]);
		assertEquals(3, Math.abs(spots[1][1]));
		assertEquals(3, Math.abs(spots[2][1]));
		assertArrayEquals(new int[]{0, -3}, spots[7]);
	}

	@Test
	public void everySpotIsThreeTilesAwayAlongTheCompass()
	{
		for (int turn = 0; turn < FollowerPath.FULL_TURN; turn += 37)
		{
			for (int[] spot : ArenaSpots.candidates(turn))
				assertEquals(3, Math.max(Math.abs(spot[0]), Math.abs(spot[1])));
		}
	}
}
