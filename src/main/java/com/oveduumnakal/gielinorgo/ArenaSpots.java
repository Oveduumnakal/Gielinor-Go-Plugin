/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Where the opponent of a team battle that is acted out on screen may stand: {@link #DISTANCE} tiles from the
 * player, in the way the player faces first, then in the other seven directions of the compass, nearest the
 * facing first. Holds no RuneLite types.
 */
final class ArenaSpots
{
	/** How many tiles from the player the opponent stands. */
	static final int DISTANCE = 3;

	/** The eight directions as steps east and north, going round the compass from east. */
	private static final int[][] COMPASS = {
		{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
	};

	private ArenaSpots()
	{
	}

	/**
	 * Lists the places the opponent may stand, best first.
	 *
	 * @param orientation the way the player faces, on the game's angle scale ({@link FollowerPath#FULL_TURN} to a
	 *     full turn)
	 * @return eight offsets from the player's tile, {@code {east, north}} in tiles, each {@link #DISTANCE} tiles
	 *     along one of the compass directions; the first is the direction the player faces, then the others by how
	 *     close they are to it
	 */
	static int[][] candidates(int orientation)
	{
		int[][] ordered = new int[COMPASS.length][];
		double[] gaps = new double[COMPASS.length];
		for (int i = 0; i < COMPASS.length; i++)
		{
			int angle = FollowerPath.angle(COMPASS[i][0], COMPASS[i][1]);
			int difference = ((angle - orientation + FollowerPath.FULL_TURN / 2) & (FollowerPath.FULL_TURN - 1))
				- FollowerPath.FULL_TURN / 2;
			gaps[i] = Math.abs(difference) + i * 1e-6;
			ordered[i] = new int[]{COMPASS[i][0] * DISTANCE, COMPASS[i][1] * DISTANCE};
		}

		for (int i = 1; i < ordered.length; i++)
		{
			for (int j = i; j > 0 && gaps[j] < gaps[j - 1]; j--)
			{
				int[] spot = ordered[j];
				ordered[j] = ordered[j - 1];
				ordered[j - 1] = spot;
				double gap = gaps[j];
				gaps[j] = gaps[j - 1];
				gaps[j - 1] = gap;
			}
		}

		return ordered;
	}
}
