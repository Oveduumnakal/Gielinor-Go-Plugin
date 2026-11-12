/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Counts the game ticks a player has had to make a choice in a battle, and holds the rules for walking away from
 * one. It holds no game client types and reads no clock: it is moved on by game ticks.
 */
final class TurnClock
{
	/** Seconds in one game tick. */
	static final double SECONDS_PER_TICK = 0.6;

	/** How far, in tiles, the player may be from the wild creature, or from where a champion battle began. */
	static final int FORFEIT_DISTANCE = 15;

	private final int seconds;
	private final int limit;
	private int ticks;
	private boolean open;

	/**
	 * Makes a clock that is not running.
	 *
	 * @param seconds the time to choose, which {@link #ticksFor(int)} turns into game ticks
	 */
	TurnClock(int seconds)
	{
		this.seconds = seconds;
		this.limit = ticksFor(seconds);
	}

	/**
	 * Turns a number of seconds into game ticks.
	 *
	 * @param seconds the time
	 * @return the game ticks it takes, at least 1
	 */
	static int ticksFor(int seconds)
	{
		return Math.max(1, (int) Math.ceil(seconds / SECONDS_PER_TICK));
	}

	/** Starts counting from zero, as a prompt opens. */
	void open()
	{
		ticks = 0;
		open = true;
	}

	/** Stops counting, as the prompt closes. */
	void close()
	{
		open = false;
	}

	/** @return whether the clock is counting */
	boolean running()
	{
		return open;
	}

	/**
	 * Moves the clock on by one game tick.
	 *
	 * @return true on the tick the time is up; the clock then stops until it is opened again
	 */
	boolean tick()
	{
		if (!open)
			return false;

		ticks++;
		if (ticks < limit)
			return false;

		open = false;
		return true;
	}

	/** @return the game ticks left, 0 when the clock is not running */
	int ticksLeft()
	{
		return open ? Math.max(0, limit - ticks) : 0;
	}

	/** @return the whole seconds left, rounded up and never more than the time given, 0 when not running */
	int secondsLeft()
	{
		return Math.min(seconds, (int) Math.ceil(ticksLeft() * SECONDS_PER_TICK));
	}

	/** @return the share of the time that is left, from 0 to 1; 0 when the clock is not running */
	double fractionLeft()
	{
		return open ? Math.max(0.0, (limit - ticks) / (double) limit) : 0.0;
	}

	/**
	 * Tells whether the player has walked too far away from the battle.
	 *
	 * @param dx the distance in tiles along x
	 * @param dy the distance in tiles along y
	 * @return true when the larger distance is more than {@link #FORFEIT_DISTANCE}
	 */
	static boolean tooFar(int dx, int dy)
	{
		return Math.max(Math.abs(dx), Math.abs(dy)) > FORFEIT_DISTANCE;
	}

	/**
	 * Tells whether a battle in progress counts as run from.
	 *
	 * @param loggedIn whether the game is logged in, false while logging out or hopping
	 * @param foeInView whether the wild creature is still in view; true for a champion battle
	 * @param dx the distance in tiles along x between the player and the battle
	 * @param dy the distance in tiles along y between the player and the battle
	 * @return true when the player left the game, the creature is gone or the player walked too far away
	 */
	static boolean forfeits(boolean loggedIn, boolean foeInView, int dx, int dy)
	{
		return !loggedIn || !foeInView || tooFar(dx, dy);
	}
}
