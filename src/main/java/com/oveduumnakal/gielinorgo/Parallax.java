/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The small shift of the layers behind a creature as the mouse moves: the background goes against the mouse, the
 * creature with it by a third as much, and both glide. Holds no RuneLite types.
 */
final class Parallax
{
	/** How far the background shifts at most, as a share of the box's width or height. */
	static final double BACKGROUND = 0.025;

	/** The creature's shift as a share of the background's, the other way. */
	static final double MODEL = 1.0 / 3.0;

	/** The time in milliseconds for the glide to cover most of the way. */
	private static final double EASE_MS = 120;

	private double x;
	private double y;
	private long at = -1;

	/**
	 * Glides toward where the mouse is.
	 *
	 * @param targetX the mouse across the window, from -1 to 1; 0 when it is away or nothing moves
	 * @param targetY the same, downward
	 * @param nowMs the time in milliseconds
	 */
	void toward(double targetX, double targetY, long nowMs)
	{
		double step = at < 0 ? 0 : 1 - Math.exp(-Math.max(0, nowMs - at) / EASE_MS);
		at = nowMs;
		x += (clamp(targetX) - x) * step;
		y += (clamp(targetY) - y) * step;
	}

	/** Puts the layers back to the middle at once. */
	void rest()
	{
		x = 0;
		y = 0;
		at = -1;
	}

	/** @return the eased position across, from -1 to 1 */
	double x()
	{
		return x;
	}

	/** @return the eased position downward, from -1 to 1 */
	double y()
	{
		return y;
	}

	private static double clamp(double value)
	{
		return Math.max(-1, Math.min(1, value));
	}
}
