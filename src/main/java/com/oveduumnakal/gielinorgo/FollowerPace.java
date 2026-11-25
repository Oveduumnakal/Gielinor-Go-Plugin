/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

/**
 * Decides when the follower sets off after a walking player and how fast it goes, so it moves like a pet and not like a
 * shadow. Setting off from rest, the follower lets the gap grow by a rolled amount (the lag) first. Once moving it
 * keeps moving while the player does, easing its speed up and down instead of starting and stopping at once. Once the
 * player stands still it always catches up. It has no link to the game client and no clock, so it can be tested tick
 * by tick.
 */
final class FollowerPace
{
	/** Fewest extra tiles, beyond the usual gap, the follower lets build up before it sets off. */
	static final int LAG_MIN = 1;

	/** Most extra tiles, beyond the usual gap, the follower lets build up before it sets off. */
	static final int LAG_MAX = 2;

	/** Closer than this, in tiles, counts as caught up. */
	private static final double CAUGHT_UP = 0.001;

	/** The most the follower covers in one client tick, in tiles. */
	private static final double TOP_SPEED = (double) FollowerPath.MAX_SPEED / FollowerPath.CLIENT_TICKS;

	/** The slowest it creeps up on its usual gap, in tiles a client tick, so that it gets there. */
	private static final double CREEP = 0.5 / FollowerPath.CLIENT_TICKS;

	/** The most its speed changes in one client tick: from rest to a run in two thirds of a game tick. */
	private static final double ACCELERATION = 0.1 / FollowerPath.CLIENT_TICKS;

	/** Client ticks over which it would close the ground it is behind, on top of matching the player. */
	private static final double CLOSING_TICKS = 20;

	/** How much of the player's latest step goes into the speed it matches. */
	private static final double MATCHING = 0.1;

	private final Random random;
	private final boolean lagging;
	private int lag;
	private boolean catchingUp;
	private double speed;
	private double playerSpeed;

	/**
	 * Makes a pace.
	 *
	 * @param random rolls how far the follower lets itself lag each time
	 * @param lagging false to follow at once, with no lag, as a tight shadow
	 */
	FollowerPace(Random random, boolean lagging)
	{
		this.random = random;
		this.lagging = lagging;
		lag = rollLag();
	}

	/**
	 * Tells whether the follower should be moving now.
	 *
	 * @param excess tiles the follower is farther behind than its usual gap; 0 or less when it is caught up
	 * @param playerMoving whether the player is still walking
	 * @return true when the follower should walk this tick
	 */
	boolean moving(double excess, boolean playerMoving)
	{
		if (excess <= CAUGHT_UP)
		{
			if (!playerMoving)
				catchingUp = false;

			return false;
		}

		if (!lagging || !playerMoving || excess > lag)
			catchingUp = true;

		return catchingUp;
	}

	/**
	 * Gives how far the follower walks this client tick. Call it once each client tick.
	 *
	 * @param excess tiles the follower is farther behind than its usual gap; 0 or less when it is caught up
	 * @param playerStep tiles the player moved since the last client tick, 0 when standing
	 * @return tiles to walk, never more than the excess; a pace with no lag walks at top speed
	 */
	double step(double excess, double playerStep)
	{
		playerSpeed += (playerStep - playerSpeed) * MATCHING;
		if (!moving(excess, playerStep > 0))
		{
			speed = 0;
			return 0;
		}

		if (!lagging)
			return Math.min(excess, TOP_SPEED);

		double wanted = Math.max(CREEP, Math.min(TOP_SPEED, playerSpeed + excess / CLOSING_TICKS));
		speed += Math.max(-ACCELERATION, Math.min(ACCELERATION, wanted - speed));
		return Math.min(excess, speed);
	}

	/** Rolls a new lag, once the follower has caught up. */
	void caughtUp()
	{
		catchingUp = false;
		lag = rollLag();
	}

	/** Forgets any catch-up under way, for when the follower is put down afresh. */
	void reset()
	{
		catchingUp = false;
		speed = 0;
		playerSpeed = 0;
		lag = rollLag();
	}

	/** @return tiles the gap may grow by before the follower sets off */
	int lag()
	{
		return lag;
	}

	private int rollLag()
	{
		return LAG_MIN + random.nextInt(LAG_MAX - LAG_MIN + 1);
	}
}
