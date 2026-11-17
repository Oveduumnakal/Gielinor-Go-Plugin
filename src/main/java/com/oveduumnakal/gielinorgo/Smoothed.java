/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * A value that glides toward a target, for a health or experience bar. A new target starts a glide from wherever the
 * value is now, so a bar hit twice in a row does not jump. The glide uses an out-cubic curve, so the value never goes
 * past its target, and it lands on the target exactly. In {@link MotionMode#OFF} the value jumps at once. The caller
 * passes the time in. Holds no RuneLite types.
 */
final class Smoothed
{
	private final long durationMs;
	private double target;
	private Tween glide;

	/**
	 * Starts a value at rest.
	 *
	 * @param initial the value to start at, which is also the target
	 * @param durationMs how long a glide takes
	 */
	Smoothed(double initial, long durationMs)
	{
		this.target = initial;
		this.durationMs = durationMs;
	}

	/**
	 * Aims at a new target. Aiming at the target it already has changes nothing.
	 *
	 * @param newTarget the value to glide to
	 * @param nowMs the time in milliseconds
	 * @param mode the motion mode; {@link MotionMode#OFF} jumps at once
	 */
	void set(double newTarget, long nowMs, MotionMode mode)
	{
		if (newTarget == target)
			return;

		double current = value(nowMs);
		target = newTarget;
		long ms = mode.barMs(durationMs);
		glide = ms > 0 ? Tween.of(current, newTarget, nowMs, ms, Easing.OUT_CUBIC) : null;
	}

	/** Jumps to a value with no glide, for a bar that is shown for the first time. */
	void snap(double newValue)
	{
		target = newValue;
		glide = null;
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return the value to draw; it moves monotonically from the old value to the target, never past it
	 */
	double value(long nowMs)
	{
		return glide == null ? target : glide.value(nowMs);
	}

	/** @return the value the glide is heading for */
	double target()
	{
		return target;
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return whether the value has reached its target
	 */
	boolean settled(long nowMs)
	{
		return glide == null || glide.done(nowMs);
	}
}
