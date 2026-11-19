/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

/**
 * Decides when the follower sets off after a walking player, so it moves like a pet and not like a shadow. While the
 * player keeps walking, the follower lets the gap grow by a rolled amount (the lag) before it moves, then catches up
 * all the way to its usual gap, then waits again. Once the player stands still it always catches up. It has no link to
 * the game client and no clock, so it can be tested tick by tick.
 */
final class FollowerPace
{
	/** Fewest extra tiles, beyond the usual gap, the follower lets build up before it sets off. */
	static final int LAG_MIN = 1;

	/** Most extra tiles, beyond the usual gap, the follower lets build up before it sets off. */
	static final int LAG_MAX = 2;

	/** Closer than this, in tiles, counts as caught up. */
	private static final double CAUGHT_UP = 0.001;

	private final Random random;
	private final boolean lagging;
	private int lag;
	private boolean catchingUp;

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
			catchingUp = false;
			return false;
		}

		if (!lagging || !playerMoving || excess > lag)
			catchingUp = true;

		return catchingUp;
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
