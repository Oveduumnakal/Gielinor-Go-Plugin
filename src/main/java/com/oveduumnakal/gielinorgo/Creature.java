/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * One creature the player owns. Saved to the profile as JSON, so every field here is part of the persisted schema.
 */
final class Creature
{
	/** How long a creature takes to go from no health back to full, in milliseconds. */
	static final long FULL_REST_MS = 5 * 60 * 1000L;

	/** Species key, see {@link Species#keyFor(String)}. */
	String species;

	/** Total creature XP. The level is derived from it by {@link XpRules#levelForXp(long)}. */
	long xp;

	/** Share of full health the creature had at {@link #hpStampMs}, from 0 to 1. */
	double hpFraction = 1.0;

	/** Wall-clock time {@link #hpFraction} was recorded, in milliseconds. */
	long hpStampMs;

	/** Wall-clock time the creature was caught, in milliseconds. */
	long caughtAtMs;

	/** Used by Gson. */
	Creature()
	{
	}

	Creature(String species, int level, long nowMs)
	{
		this.species = species;
		this.xp = XpRules.xpForLevel(level);
		this.caughtAtMs = nowMs;
		this.hpStampMs = nowMs;
	}

	/** @return the creature's level, 1 to 99 */
	int level()
	{
		return XpRules.levelForXp(xp);
	}

	/**
	 * Health right now, counting the rest since the last battle.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return the share of full health, from 0 to 1
	 */
	double hpFractionAt(long nowMs)
	{
		double rested = Math.max(0, nowMs - hpStampMs) / (double) FULL_REST_MS;
		return Math.max(0.0, Math.min(1.0, hpFraction + rested));
	}
}
