/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * How much the painted window moves; the "Window animations" setting. Every motion asks the mode whether it may play
 * and for how long, so no motion code checks the setting itself. Holds no RuneLite types.
 */
public enum MotionMode
{
	/** Everything moves: opening, sliding, bobbing, pulsing, popping. The default. */
	FULL("Full"),

	/** Only the open and close fade, smooth bars and the notice fade. No bobbing, sliding, popping or pulsing. */
	REDUCED("Reduced"),

	/** Nothing moves; every change is instant. */
	OFF("Off");

	/** How long the open and close fade lasts in {@link #REDUCED}, in milliseconds. */
	static final long REDUCED_FADE_MS = 80;

	private final String label;

	MotionMode(String label)
	{
		this.label = label;
	}

	/** @return whether things may slide, scale, hop and pop; only in {@link #FULL} */
	boolean moves()
	{
		return this == FULL;
	}

	/** @return whether endless motion (bobbing, pulsing, sweeps, shaking) may play; only in {@link #FULL} */
	boolean loops()
	{
		return this == FULL;
	}

	/** @return whether the window and the notice fade in and out; false only in {@link #OFF} */
	boolean fades()
	{
		return this != OFF;
	}

	/** @return whether bars glide to a new value instead of jumping; false only in {@link #OFF} */
	boolean smoothBars()
	{
		return this != OFF;
	}

	/**
	 * Gives how long a movement lasts in this mode.
	 *
	 * @param fullMs how long it lasts in {@link #FULL}
	 * @return the same in {@link #FULL}, 0 otherwise, since a slide or a pop does not play in the other modes
	 */
	long moveMs(long fullMs)
	{
		return moves() ? fullMs : 0;
	}

	/**
	 * Gives how long a fade or a smooth change lasts in this mode.
	 *
	 * @param fullMs how long it lasts in {@link #FULL}
	 * @return the same in {@link #FULL}, {@link #REDUCED_FADE_MS} at most in {@link #REDUCED}, 0 in {@link #OFF}
	 */
	long fadeMs(long fullMs)
	{
		switch (this)
		{
			case FULL:
				return fullMs;
			case REDUCED:
				return Math.min(fullMs, REDUCED_FADE_MS);
			default:
				return 0;
		}
	}

	/**
	 * Gives how long a bar takes to glide to a new value in this mode.
	 *
	 * @param fullMs how long it takes in {@link #FULL} and {@link #REDUCED}
	 * @return the time; 0 in {@link #OFF}
	 */
	long barMs(long fullMs)
	{
		return smoothBars() ? fullMs : 0;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
