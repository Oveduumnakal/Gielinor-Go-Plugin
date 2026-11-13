/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Lets one sound through at a time: a cue is dropped when another played within the last {@link #QUIET_MS}
 * milliseconds. The gilded cue always plays. It takes the time as an argument and has no RuneLite types.
 */
final class CueGate
{
	/** How long after a sound the next one is dropped, in milliseconds. */
	static final long QUIET_MS = 600;

	private long lastPlayed = Long.MIN_VALUE;

	/**
	 * Asks whether a cue may play now, and records it when it may.
	 *
	 * @param cue the cue
	 * @param nowMs the current time in milliseconds
	 * @return true when the sound should play
	 */
	boolean allow(Cue cue, long nowMs)
	{
		boolean quiet = lastPlayed == Long.MIN_VALUE || nowMs - lastPlayed >= QUIET_MS || nowMs < lastPlayed;
		if (!quiet && cue != Cue.GILDED)
			return false;

		lastPlayed = nowMs;
		return true;
	}
}
