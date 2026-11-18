/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** When the tracker shows and how big it draws. It has no RuneLite types. */
final class TrackerRules
{
	/** The largest scale the tracker draws at, so it stays small on a big window. */
	static final double MAX_SCALE = 0.9;

	private TrackerRules()
	{
	}

	/**
	 * Decides whether the tracker is drawn at all. Real combat does not hide it, only a Go battle does.
	 *
	 * @param enabled the "Pinned tracker" setting
	 * @param battleOn whether a Go battle is on
	 * @param loaded whether the save is loaded and live
	 * @param lines how many lines there are to show
	 * @return true when the tracker shows
	 */
	static boolean visible(boolean enabled, boolean battleOn, boolean loaded, int lines)
	{
		return enabled && !battleOn && loaded && lines > 0;
	}

	/**
	 * Keeps the window's scale inside the range the tracker draws at.
	 *
	 * @param windowScale the scale of the painted window
	 * @return the scale to draw the tracker at, from {@link WindowFit#MIN_SCALE} to {@link #MAX_SCALE}
	 */
	static double scale(double windowScale)
	{
		return Math.max(WindowFit.MIN_SCALE, Math.min(MAX_SCALE, windowScale));
	}
}
