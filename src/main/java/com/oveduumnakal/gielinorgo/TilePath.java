/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;

/**
 * Finds the way between tiles for the creatures this plugin draws: whether there is a route of a few steps, the
 * route itself, a free tile near a place that can be walked to, and the spot a fighter stands on to reach another.
 * It only asks one thing of the world, whether one step is open ({@link Ground}), so it is tested on small
 * hand-drawn maps and fed from the game's collision flags in play. A step is one tile in any of eight directions,
 * as in the game. Holds no RuneLite types.
 */
final class TilePath
{
	/** The eight neighbours of a tile: straight steps first, so a tie between two routes prefers the straight one. */
	private static final int[][] STEPS = {
		{0, -1}, {-1, 0}, {1, 0}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
	};

	/** What the world says about tiles. */
	interface Ground
	{
		/**
		 * Tells whether a creature can step between two neighbouring tiles.
		 *
		 * @param fromX the tile stepped from, x
		 * @param fromY the tile stepped from, y
		 * @param toX the tile stepped onto, x
		 * @param toY the tile stepped onto, y
		 * @return true when the step is open: the tile is free and no wall lies between
		 */
		boolean canStep(int fromX, int fromY, int toX, int toY);

		/**
		 * Tells whether a creature can stand on a tile.
		 *
		 * @param x the tile's x
		 * @param y the tile's y
		 * @return true when nothing fills the tile
		 */
		boolean standable(int x, int y);
	}

	private final Ground ground;

	/**
	 * Makes a finder for one map.
	 *
	 * @param ground what the map says about steps
	 */
	TilePath(Ground ground)
	{
		this.ground = ground;
	}

	/**
	 * Finds the shortest route between two tiles.
	 *
	 * @param fromX the start tile, x
	 * @param fromY the start tile, y
	 * @param toX the end tile, x
	 * @param toY the end tile, y
	 * @param maxSteps the most steps the route may have
	 * @return the tiles to step onto, in order, ending on the end tile; empty when the two are the same tile; null
	 *     when there is no route of {@code maxSteps} or fewer
	 */
	List<int[]> route(int fromX, int fromY, int toX, int toY, int maxSteps)
	{
		if (fromX == toX && fromY == toY)
			return new ArrayList<>();

		Map<Long, Long> came = new HashMap<>();
		Map<Long, Integer> far = new HashMap<>();
		Deque<int[]> queue = new ArrayDeque<>();
		far.put(key(fromX, fromY), 0);
		queue.add(new int[]{fromX, fromY});
		while (!queue.isEmpty())
		{
			int[] at = queue.poll();
			int steps = far.get(key(at[0], at[1]));
			if (steps >= maxSteps)
				continue;

			for (int[] step : STEPS)
			{
				int x = at[0] + step[0];
				int y = at[1] + step[1];
				long next = key(x, y);
				if (far.containsKey(next) || !ground.canStep(at[0], at[1], x, y))
					continue;

				far.put(next, steps + 1);
				came.put(next, key(at[0], at[1]));
				if (x == toX && y == toY)
					return unwind(came, fromX, fromY, toX, toY);

				queue.add(new int[]{x, y});
			}
		}

		return null;
	}

	/**
	 * Tells whether a route of at most some steps exists.
	 *
	 * @param fromX the start tile, x
	 * @param fromY the start tile, y
	 * @param toX the end tile, x
	 * @param toY the end tile, y
	 * @param maxSteps the most steps the route may have
	 * @return whether there is one
	 */
	boolean reachable(int fromX, int fromY, int toX, int toY, int maxSteps)
	{
		return route(fromX, fromY, toX, toY, maxSteps) != null;
	}

	/**
	 * Picks a tile at random that can be walked to, and gives the route to it. Used to place a wild creature (a free
	 * tile near the real monster that the player's side can walk to) and to pick its next wander step.
	 *
	 * @param fromX the tile the route starts on, x
	 * @param fromY the tile the route starts on, y
	 * @param maxSteps the most steps the route may have
	 * @param aroundX the tile the answer must be near, x
	 * @param aroundY the tile the answer must be near, y
	 * @param minDistance the least the answer may be from the tile it must be near, counted as the game counts
	 *     distance (the larger of the two differences)
	 * @param maxDistance the most the answer may be from that tile
	 * @param random picks among the tiles that qualify
	 * @param taken tells which tiles are not to be used, such as the one the player stands on
	 * @return the route, ending on the picked tile; null when no tile qualifies
	 */
	List<int[]> pick(int fromX, int fromY, int maxSteps, int aroundX, int aroundY, int minDistance,
		int maxDistance, Random random, BiPredicate<Integer, Integer> taken)
	{
		Map<Long, Long> came = new HashMap<>();
		List<int[]> found = new ArrayList<>();
		walk(fromX, fromY, maxSteps, came, found);
		List<int[]> fits = new ArrayList<>();
		for (int[] tile : found)
		{
			int distance = Math.max(Math.abs(tile[0] - aroundX), Math.abs(tile[1] - aroundY));
			boolean same = tile[0] == fromX && tile[1] == fromY;
			if (!same && distance >= minDistance && distance <= maxDistance && !taken.test(tile[0], tile[1]))
				fits.add(tile);
		}

		if (fits.isEmpty())
			return null;

		int[] chosen = fits.get(random.nextInt(fits.size()));
		return unwind(came, fromX, fromY, chosen[0], chosen[1]);
	}

	/**
	 * Finds where a fighter can stand to face another: a tile at a set distance from the target that can be walked to
	 * and from which a straight line to the target has no wall or object in it, so a step towards the target is
	 * open. Of the tiles that qualify, the one nearest to an aimed-at place wins.
	 *
	 * @param foeX the target's tile, x
	 * @param foeY the target's tile, y
	 * @param ring how far from the target the fighter stands, counted as the game counts distance; at least 1
	 * @param aimX the place the fighter would like to stand nearest to, x in tiles
	 * @param aimY the place the fighter would like to stand nearest to, y in tiles
	 * @param fromX the tile the fighter walks from, x
	 * @param fromY the tile the fighter walks from, y
	 * @param maxSteps the most steps the walk may have
	 * @return the route, ending on the spot; null when no tile qualifies
	 */
	List<int[]> approach(int foeX, int foeY, int ring, double aimX, double aimY, int fromX, int fromY,
		int maxSteps)
	{
		Map<Long, Long> came = new HashMap<>();
		List<int[]> found = new ArrayList<>();
		walk(fromX, fromY, maxSteps, came, found);
		List<int[]> fits = new ArrayList<>();
		for (int[] tile : found)
		{
			int distance = Math.max(Math.abs(tile[0] - foeX), Math.abs(tile[1] - foeY));
			if (distance == Math.max(1, ring) && lineClear(tile[0], tile[1], foeX, foeY))
				fits.add(tile);
		}

		Collections.sort(fits, Comparator.comparingDouble(tile -> Math.hypot(tile[0] - aimX, tile[1] - aimY)));
		if (fits.isEmpty())
			return null;

		int[] chosen = fits.get(0);
		return unwind(came, fromX, fromY, chosen[0], chosen[1]);
	}

	/**
	 * Tells whether every step along the straight line between two tiles is open, which is what a melee attacker
	 * walking up to its target needs.
	 *
	 * @param fromX the first tile, x
	 * @param fromY the first tile, y
	 * @param toX the last tile, x
	 * @param toY the last tile, y
	 * @return whether each tile on the line can be stepped onto from the one before; true for the same tile
	 */
	boolean lineClear(int fromX, int fromY, int toX, int toY)
	{
		int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
		int x = fromX;
		int y = fromY;
		for (int i = 1; i <= steps; i++)
		{
			int nextX = fromX + (int) Math.round((toX - fromX) * (double) i / steps);
			int nextY = fromY + (int) Math.round((toY - fromY) * (double) i / steps);
			if (!ground.canStep(x, y, nextX, nextY))
				return false;

			x = nextX;
			y = nextY;
		}

		return true;
	}

	/**
	 * Tells whether a tile can be stood on.
	 *
	 * @param x the tile's x
	 * @param y the tile's y
	 * @return true when nothing fills it
	 */
	boolean standable(int x, int y)
	{
		return ground.standable(x, y);
	}

	private void walk(int fromX, int fromY, int maxSteps, Map<Long, Long> came, List<int[]> found)
	{
		Map<Long, Integer> far = new HashMap<>();
		Deque<int[]> queue = new ArrayDeque<>();
		far.put(key(fromX, fromY), 0);
		queue.add(new int[]{fromX, fromY});
		found.add(new int[]{fromX, fromY});
		while (!queue.isEmpty())
		{
			int[] at = queue.poll();
			int steps = far.get(key(at[0], at[1]));
			if (steps >= maxSteps)
				continue;

			for (int[] step : STEPS)
			{
				int x = at[0] + step[0];
				int y = at[1] + step[1];
				long next = key(x, y);
				if (far.containsKey(next) || !ground.canStep(at[0], at[1], x, y))
					continue;

				far.put(next, steps + 1);
				came.put(next, key(at[0], at[1]));
				queue.add(new int[]{x, y});
				found.add(new int[]{x, y});
			}
		}
	}

	private static List<int[]> unwind(Map<Long, Long> came, int fromX, int fromY, int toX, int toY)
	{
		List<int[]> route = new ArrayList<>();
		long start = key(fromX, fromY);
		long at = key(toX, toY);
		while (at != start)
		{
			route.add(new int[]{(int) (at >> 32), (int) at});
			at = came.get(at);
		}

		Collections.reverse(route);
		return route;
	}

	private static long key(int x, int y)
	{
		return ((long) x << 32) | (y & 0xFFFFFFFFL);
	}
}
