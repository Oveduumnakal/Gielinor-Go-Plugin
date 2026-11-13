/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Remembers when the player was last seen fighting, so "in combat" lasts a few seconds past the last blow. The
 * caller passes the time in, so the rule needs no clock. Holds no RuneLite types.
 */
final class CombatWatch
{
	/** How long after the last fighting tick the player still counts as in combat, in milliseconds. */
	static final long LINGER_MS = 5_000;

	private boolean seen;
	private long lastFightMs;

	/**
	 * Records that the player is fighting right now.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void noteFighting(long nowMs)
	{
		seen = true;
		lastFightMs = nowMs;
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return whether the player fought within {@link #LINGER_MS} of now
	 */
	boolean inCombat(long nowMs)
	{
		return seen && nowMs - lastFightMs <= LINGER_MS;
	}

	/** Forgets any fight, for when the player logs out. */
	void reset()
	{
		seen = false;
	}
}
