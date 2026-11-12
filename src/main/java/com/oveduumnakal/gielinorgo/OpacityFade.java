/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * How solid something is at each client tick while it fades: a current opacity that moves toward a target at a set
 * rate, and the few levels the opacity is drawn at. A model is built per level, so only a handful of levels exist.
 * It has no link to the game client, so it can be tested without one.
 */
final class OpacityFade
{
	/** A faint opacity the fade levels include, 10%: a face this faint is 90% see-through. */
	static final double FAINT = 0.10;

	/**
	 * The opacities a faded model is drawn at, faintest first, from unseen (0) to plain and solid (1). They are
	 * 0.1 apart, with finer steps near the bottom so a fade between 0 and {@link #FAINT} is not a pop.
	 */
	private static final double[] LEVELS =
		{0.0, 0.025, 0.05, FAINT, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

	/** The most see-through value a face can have; 255 cannot be seen at all. */
	private static final int MAX_TRANSPARENCY = 255;

	/** The level for unseen: nothing is drawn. */
	static final int HIDDEN = 0;

	/** The level for solid: the plain model is drawn, with no transparency forced onto it. */
	static final int SOLID = LEVELS.length - 1;

	private double opacity;
	private double target;
	private double rate;

	/**
	 * Starts a fade that is not moving.
	 *
	 * @param opacity the opacity to start at, from 0 to 1
	 */
	OpacityFade(double opacity)
	{
		this.opacity = clamp(opacity);
		this.target = this.opacity;
	}

	/**
	 * Makes one face see-through. The result is never less see-through than the face was.
	 *
	 * @param transparency the face's value, 0 for solid up to 255 for unseen
	 * @param opacity the share of the face that stays visible, from 0 to 1
	 * @return the new value
	 */
	static int fade(int transparency, double opacity)
	{
		int t = Math.max(0, Math.min(MAX_TRANSPARENCY, transparency));
		return MAX_TRANSPARENCY - (int) Math.round((MAX_TRANSPARENCY - t) * opacity);
	}

	/**
	 * Makes every face the given share visible, in place. A missing array means every face was solid, so there is
	 * nothing to change here; the caller makes the array first.
	 *
	 * @param transparencies one value per face, or null
	 * @param opacity the share of each face that stays visible, from 0 to 1
	 */
	static void fadeAll(byte[] transparencies, double opacity)
	{
		if (transparencies == null)
			return;

		for (int i = 0; i < transparencies.length; i++)
			transparencies[i] = (byte) fade(transparencies[i] & 0xFF, opacity);
	}

	/**
	 * Picks the level an opacity is drawn at: the nearest one.
	 *
	 * @param opacity an opacity, from 0 to 1
	 * @return the level, from {@link #HIDDEN} to {@link #SOLID}
	 */
	static int levelOf(double opacity)
	{
		int best = HIDDEN;
		for (int i = 1; i < LEVELS.length; i++)
			if (Math.abs(LEVELS[i] - opacity) < Math.abs(LEVELS[best] - opacity))
				best = i;

		return best;
	}

	/**
	 * Gives the opacity a level stands for.
	 *
	 * @param level a level, from {@link #HIDDEN} to {@link #SOLID}
	 * @return its opacity, from 0 to 1
	 */
	static double opacityOf(int level)
	{
		return LEVELS[Math.max(HIDDEN, Math.min(SOLID, level))];
	}

	/**
	 * Heads for a target at a set rate, from wherever the opacity is now. Calling it again with another target
	 * continues from the current opacity.
	 *
	 * @param newTarget the opacity to head for, from 0 to 1
	 * @param perTick how far the opacity moves each client tick, never negative
	 */
	void moveAt(double newTarget, double perTick)
	{
		target = clamp(newTarget);
		rate = Math.max(0.0, perTick);
	}

	/**
	 * Heads for a target so as to arrive after a number of client ticks, from wherever the opacity is now.
	 *
	 * @param newTarget the opacity to head for, from 0 to 1
	 * @param ticks the client ticks left to get there; below 1 counts as 1
	 */
	void moveTo(double newTarget, int ticks)
	{
		double goal = clamp(newTarget);
		moveAt(goal, Math.abs(goal - opacity) / Math.max(1, ticks));
	}

	/** Moves the opacity on by one client tick; it never goes past the target. */
	void tick()
	{
		double gap = target - opacity;
		if (Math.abs(gap) <= rate)
			opacity = target;
		else
			opacity += Math.signum(gap) * rate;
	}

	/** @return the opacity now, from 0 to 1 */
	double opacity()
	{
		return opacity;
	}

	/** @return the level the opacity is drawn at now */
	int level()
	{
		return levelOf(opacity);
	}

	/** @return whether the opacity has reached its target */
	boolean settled()
	{
		return opacity == target;
	}

	private static double clamp(double value)
	{
		return Math.max(0.0, Math.min(1.0, value));
	}
}
