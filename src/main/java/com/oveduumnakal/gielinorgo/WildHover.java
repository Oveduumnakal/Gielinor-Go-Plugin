/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Holds the tooltip text for the wild creature the mouse rests on, so the client thread can decide and the render
 * thread can paint. The state is one immutable snapshot, so the two threads never see a torn value. Free of
 * RuneLite types, and the time is always passed in.
 */
final class WildHover
{
	/** How long the mouse must rest on a creature before its tooltip shows, so a quick sweep shows nothing. */
	static final long DELAY_MS = 300;

	private volatile Snapshot current;

	/**
	 * Notes the creature under the mouse. The same creature keeps its delay running and takes the new text; another
	 * creature restarts the delay.
	 *
	 * @param wildId the creature's id
	 * @param text the tooltip text
	 * @param nowMs the time now, in milliseconds
	 */
	void update(int wildId, String text, long nowMs)
	{
		Snapshot before = current;
		long since = before != null && before.wildId == wildId ? before.sinceMs : nowMs;
		current = new Snapshot(wildId, text, since);
	}

	/** Forgets the creature at once; the next update starts the delay again. */
	void clear()
	{
		current = null;
	}

	/**
	 * Reads the text to show.
	 *
	 * @param nowMs the time now, in milliseconds
	 * @return the tooltip text once the delay has passed, else null
	 */
	String shown(long nowMs)
	{
		Snapshot now = current;
		if (now == null || nowMs - now.sinceMs < DELAY_MS)
			return null;

		return now.text;
	}

	/**
	 * Says whether the tooltip may show at all.
	 *
	 * @param enabled whether the player left the setting on
	 * @param menuOpen whether a game menu is open
	 * @param overScene whether the pointer is over the game view and no interface
	 * @param battleActive whether a Gielinor Go battle is running
	 * @return true only when enabled, with no menu open, over the scene and outside a battle
	 */
	static boolean wanted(boolean enabled, boolean menuOpen, boolean overScene, boolean battleActive)
	{
		return enabled && !menuOpen && overScene && !battleActive;
	}

	/** One moment of hovering: which creature, what to say, and when the mouse first rested on it. */
	private static final class Snapshot
	{
		private final int wildId;
		private final String text;
		private final long sinceMs;

		private Snapshot(int wildId, String text, long sinceMs)
		{
			this.wildId = wildId;
			this.text = text;
			this.sinceMs = sinceMs;
		}
	}
}
