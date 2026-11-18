/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * The follower's idle behaviour: when the player stands still it rests for a few seconds, then strolls to another
 * tile near the player, stands there, and rests again. Time is counted in client ticks that the caller feeds in, never
 * from a clock, so it can be tested tick by tick. It has no link to the game client; where the follower may walk is
 * asked of a {@link TilePath}, which knows the walls.
 */
final class FollowerStroll
{
	/**
	 * Farthest a stroll may end from the player's tile, counted as the game counts distance (the larger of the two
	 * differences): three tiles.
	 */
	static final int RADIUS = 3;

	/** Most steps one stroll may take. The follower starts about a tile from the player, so the far side is near. */
	static final int MAX_STEPS = 5;

	/** Tiles per game tick the follower covers while strolling: a walk, not a run. */
	static final int SPEED = 1;

	/** Shortest rest before a stroll, in client ticks: three seconds, as long as a wild creature rests. */
	static final int REST_MIN = 150;

	/** Longest rest before a stroll, in client ticks: eight seconds, as long as a wild creature rests. */
	static final int REST_MAX = 400;

	/** Longest route home when the player sets off again, in steps. */
	static final int REJOIN_STEPS = FollowerPath.MAX_ERRAND;

	/** What the follower is doing. */
	enum Phase
	{
		/** Not strolling: the follower trails the player, or waits one tile behind. */
		OFF,

		/** Walking to a tile it picked. */
		WALKING,

		/** Standing on the tile it strolled to, resting before the next stroll. */
		STANDING
	}

	private final Random random;
	private final Deque<double[]> route = new ArrayDeque<>();
	private Phase phase = Phase.OFF;
	private int restLeft = -1;

	/**
	 * Makes the idle behaviour.
	 *
	 * @param random picks how long a rest lasts and where a stroll goes
	 */
	FollowerStroll(Random random)
	{
		this.random = random;
	}

	/**
	 * Counts one client tick of rest. The rest is rolled on the first settled tick, between {@link #REST_MIN} and
	 * {@link #REST_MAX}, and only counts down while the follower is settled.
	 *
	 * @param settled whether the follower is standing with nothing left to do, so the rest may run
	 * @return true when the rest is over and a stroll is due
	 */
	boolean restOver(boolean settled)
	{
		if (!settled)
			return false;

		if (restLeft < 0)
			restLeft = rollRest();

		if (restLeft > 0)
			restLeft--;

		return restLeft == 0;
	}

	/**
	 * Picks where to stroll to, and starts walking there. The tile is at least one and at most {@link #RADIUS} tiles
	 * from the player, never the player's own, reached by a route of at most {@link #MAX_STEPS} steps that does not
	 * cross a wall and does not pass over the player's tile ({@link TilePath#avoiding(int, int)}). With nowhere to
	 * go, the phase is left as it was and a new rest is rolled, so a boxed-in follower does not search again at once.
	 *
	 * @param tiles knows which steps are open
	 * @param fromX the tile the follower stands on, x
	 * @param fromY the tile the follower stands on, y
	 * @param playerX the player's tile, x
	 * @param playerY the player's tile, y
	 * @return true when a route was found and the follower is walking
	 */
	boolean begin(TilePath tiles, int fromX, int fromY, int playerX, int playerY)
	{
		route.clear();
		List<int[]> found = tiles.avoiding(playerX, playerY).pick(fromX, fromY, MAX_STEPS, playerX, playerY, 1,
			RADIUS, random, (x, y) -> x == playerX && y == playerY);
		if (found != null && !found.isEmpty())
		{
			for (int[] tile : found)
				route.add(new double[]{tile[0], tile[1]});

			phase = Phase.WALKING;
			return true;
		}

		restLeft = rollRest();
		return false;
	}

	/** Notes that the follower reached the end of its stroll: it stands there and rolls a new rest. */
	void arrived()
	{
		route.clear();
		phase = Phase.STANDING;
		restLeft = rollRest();
	}

	/** Ends the idle behaviour, back to trailing the player. The next rest is rolled afresh. */
	void stop()
	{
		route.clear();
		phase = Phase.OFF;
		restLeft = -1;
	}

	/** Forgets the rest so far, so the next stop of the player starts a fresh one. */
	void forgetRest()
	{
		restLeft = -1;
	}

	/** @return what the follower is doing */
	Phase phase()
	{
		return phase;
	}

	/** @return the tiles of the stroll still to walk, in order; empty unless {@link Phase#WALKING} */
	Deque<double[]> route()
	{
		return route;
	}

	/** @return client ticks of rest left, or -1 when none is rolled */
	int restLeft()
	{
		return restLeft;
	}

	private int rollRest()
	{
		return REST_MIN + random.nextInt(REST_MAX - REST_MIN + 1);
	}
}
