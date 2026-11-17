/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Endless motion: the lead arrow's bob, a portrait's idle bob, a pulsing dot, a blinking caret. Each is a function of
 * the time only, with a period and a phase offset so neighbours do not move in step. Outside {@link MotionMode#FULL}
 * the mode versions return the resting value, so the page code needs no check. Holds no RuneLite types.
 */
final class Loop
{
	private Loop()
	{
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @param periodMs how long one round lasts; 0 or less gives 0
	 * @param offsetMs shifts the round, so each creature can bob out of step
	 * @return where in the round the time is, from 0 (inclusive) to 1 (exclusive)
	 */
	static double phase(long nowMs, long periodMs, long offsetMs)
	{
		if (periodMs <= 0)
			return 0.0;

		long m = Math.floorMod(nowMs + offsetMs, periodMs);
		return (double) m / periodMs;
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @param periodMs how long one round lasts
	 * @param offsetMs shifts the round
	 * @return a sine wave from -1 to 1, starting at 0 and rising
	 */
	static double wave(long nowMs, long periodMs, long offsetMs)
	{
		return Math.sin(2.0 * Math.PI * phase(nowMs, periodMs, offsetMs));
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @param periodMs how long one round lasts
	 * @param offsetMs shifts the round
	 * @return a soft pulse from 0 to 1, starting at 0, for brightness
	 */
	static double pulse(long nowMs, long periodMs, long offsetMs)
	{
		return (1.0 - Math.cos(2.0 * Math.PI * phase(nowMs, periodMs, offsetMs))) / 2.0;
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @param periodMs how long one on-and-off round lasts
	 * @return whether a blinking thing, such as a caret, is on: on for the first half of each round
	 */
	static boolean blink(long nowMs, long periodMs)
	{
		return phase(nowMs, periodMs, 0) < 0.5;
	}

	/**
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 * @param periodMs how long one round lasts
	 * @param offsetMs shifts the round
	 * @return {@link #wave} in {@link MotionMode#FULL}, 0 otherwise
	 */
	static double wave(MotionMode mode, long nowMs, long periodMs, long offsetMs)
	{
		return mode.loops() ? wave(nowMs, periodMs, offsetMs) : 0.0;
	}

	/**
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 * @param periodMs how long one round lasts
	 * @param offsetMs shifts the round
	 * @return {@link #pulse} in {@link MotionMode#FULL}, 0 otherwise
	 */
	static double pulse(MotionMode mode, long nowMs, long periodMs, long offsetMs)
	{
		return mode.loops() ? pulse(nowMs, periodMs, offsetMs) : 0.0;
	}
}
