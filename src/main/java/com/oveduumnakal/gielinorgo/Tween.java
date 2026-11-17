/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * A value that goes from one number to another over a time, after an optional delay. It is immutable and the caller
 * passes the time in, so it needs no clock and a test can ask for any moment. Before the delay is over it holds the
 * start value; after the duration it is exactly the end value. Holds no RuneLite types.
 */
final class Tween
{
	private final double from;
	private final double to;
	private final long startMs;
	private final long delayMs;
	private final long durationMs;
	private final Easing easing;

	/**
	 * Makes a tween.
	 *
	 * @param from the value at the start
	 * @param to the value at the end
	 * @param startMs when it was started, in milliseconds
	 * @param delayMs how long it waits after the start before moving; below 0 counts as 0
	 * @param durationMs how long the movement lasts; 0 or less jumps to the end after the delay
	 * @param easing the curve
	 */
	Tween(double from, double to, long startMs, long delayMs, long durationMs, Easing easing)
	{
		this.from = from;
		this.to = to;
		this.startMs = startMs;
		this.delayMs = Math.max(0, delayMs);
		this.durationMs = durationMs;
		this.easing = easing;
	}

	/**
	 * Makes a tween with no delay.
	 *
	 * @param from the value at the start
	 * @param to the value at the end
	 * @param startMs when it was started
	 * @param durationMs how long the movement lasts
	 * @param easing the curve
	 * @return the tween
	 */
	static Tween of(double from, double to, long startMs, long durationMs, Easing easing)
	{
		return new Tween(from, to, startMs, 0, durationMs, easing);
	}

	/**
	 * Gives the delay of one item in a staggered group, such as the party rows sliding in one after another.
	 *
	 * @param index the item's place in the group, from 0
	 * @param stepMs the gap between neighbours
	 * @return the delay before the item starts
	 */
	static long stagger(int index, long stepMs)
	{
		return Math.max(0, index) * Math.max(0, stepMs);
	}

	/**
	 * Gives how far along the movement is.
	 *
	 * @param nowMs the time in milliseconds
	 * @return 0 until the delay is over, then 0 to 1, then 1; the raw progress before easing
	 */
	double progress(long nowMs)
	{
		long begin = startMs + delayMs;
		if (durationMs <= 0)
			return nowMs >= begin ? 1.0 : 0.0;

		if (nowMs <= begin)
			return 0.0;

		if (nowMs >= begin + durationMs)
			return 1.0;

		return (double) (nowMs - begin) / durationMs;
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return the value at that time: the start before the delay is over, the end value exactly once it is done
	 */
	double value(long nowMs)
	{
		double p = progress(nowMs);
		if (p >= 1.0)
			return to;

		return from + (to - from) * easing.apply(p);
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return whether the movement has landed
	 */
	boolean done(long nowMs)
	{
		return progress(nowMs) >= 1.0;
	}

	/** @return the value the tween ends at */
	double to()
	{
		return to;
	}

	/** @return the value the tween starts from */
	double from()
	{
		return from;
	}

	/** @return when the tween lands, in milliseconds */
	long endMs()
	{
		return startMs + delayMs + Math.max(0, durationMs);
	}
}
