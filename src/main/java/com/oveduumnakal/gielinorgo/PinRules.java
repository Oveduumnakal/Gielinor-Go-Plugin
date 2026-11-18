/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The rules for what can be pinned to the on-screen tracker: how many things fit, how long a finished line stays, and
 * the words for why a pin is refused. It has no RuneLite types and no clock.
 */
final class PinRules
{
	/** Most things pinned at once. It keeps the box small. */
	static final int MAX_PINS = 5;

	/** How long a "Done!" or "Hatched!" line stays on the tracker, in milliseconds. */
	static final long DONE_HOLD_MS = 4000L;

	/** The reason a pin is refused when the tracker already holds {@link #MAX_PINS} things. */
	static final String FULL = "Tracker is full (" + MAX_PINS + ")";

	/** The reason a finished task cannot be pinned. */
	static final String DONE = "Already done";

	private PinRules()
	{
	}

	/**
	 * Decides whether a pin button may be pressed.
	 *
	 * @param count how many things are pinned now
	 * @param alreadyPinned whether this thing is one of them, so the press would unpin it
	 * @param finished whether this is a task that is already finished
	 * @return null when the press is allowed (unpinning always is), otherwise the reason it is refused
	 */
	static String canPin(int count, boolean alreadyPinned, boolean finished)
	{
		if (alreadyPinned)
			return null;

		if (finished)
			return DONE;

		return count >= MAX_PINS ? FULL : null;
	}
}
