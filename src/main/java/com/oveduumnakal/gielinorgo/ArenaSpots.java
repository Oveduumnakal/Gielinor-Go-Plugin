/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;

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

	/** Where the lead stands to face the opponent of a team battle, and the way there. */
	static final class Lead
	{
		/** The tiles to step onto, ending on the spot; empty when the lead already stands there. */
		final List<int[]> route;

		/** The gap, in tiles from the opponent's edge, of the spot found. */
		final double gap;

		private Lead(List<int[]> route, double gap)
		{
			this.route = route;
			this.gap = gap;
		}
	}

	/**
	 * Finds where the lead can stand to face the opponent: a tile it can walk to, {@link TurnMotion#GAP_TILES} tiles
	 * from the opponent's edge or {@link TurnMotion#NEAR_GAP_TILES} when none fits, with a clear line to the
	 * opponent, so a melee step crosses no wall. Same rule as a wild battle.
	 *
	 * @param tiles the collision map
	 * @param foeX the opponent's middle, world x in tiles
	 * @param foeY the opponent's middle, world y in tiles
	 * @param foeTiles how many tiles wide the opponent stands
	 * @param leadX the tile the lead stands on, x
	 * @param leadY the tile the lead stands on, y
	 * @param maxSteps the most steps the walk may have
	 * @return the spot and route; null when the opponent cannot be reached from where the lead stands
	 */
	static Lead lead(TilePath tiles, double foeX, double foeY, double foeTiles, int leadX, int leadY, int maxSteps)
	{
		double dx = leadX - foeX;
		double dy = leadY - foeY;
		double length = Math.hypot(dx, dy);
		for (double tryGap : new double[]{TurnMotion.GAP_TILES, TurnMotion.NEAR_GAP_TILES})
		{
			double reach = foeTiles / 2.0 + tryGap;
			double aimX = length < 0.01 ? foeX : foeX + dx / length * reach;
			double aimY = length < 0.01 ? foeY - reach : foeY + dy / length * reach;
			List<int[]> route = tiles.approach((int) Math.round(foeX), (int) Math.round(foeY),
				(int) Math.round(reach), aimX, aimY, leadX, leadY, maxSteps);
			if (route == null)
				continue;

			int[] spot = route.isEmpty() ? new int[]{leadX, leadY} : route.get(route.size() - 1);
			double edge = Math.hypot(spot[0] - foeX, spot[1] - foeY) - foeTiles / 2.0;
			return new Lead(route, Math.max(TurnMotion.CONTACT_TILES, edge));
		}

		return null;
	}
}
