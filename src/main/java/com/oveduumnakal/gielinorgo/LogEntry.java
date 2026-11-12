/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** One species as the Creature Log shows it. Immutable, so it can cross from the game thread to the UI. */
final class LogEntry
{
	private final String name;
	private final int speciesLevel;
	private final Rarity rarity;
	private final CombatType type;
	private final int caughtCount;
	private final boolean seen;
	private final boolean gilded;
	private final boolean retired;

	LogEntry(String name, int speciesLevel, Rarity rarity, CombatType type, int caughtCount, boolean seen,
		boolean gilded)
	{
		this(name, speciesLevel, rarity, type, caughtCount, seen, gilded, false);
	}

	LogEntry(String name, int speciesLevel, Rarity rarity, CombatType type, int caughtCount, boolean seen,
		boolean gilded, boolean retired)
	{
		this.retired = retired;
		this.gilded = gilded;
		this.name = name;
		this.speciesLevel = speciesLevel;
		this.rarity = rarity;
		this.type = type;
		this.caughtCount = caughtCount;
		this.seen = seen;
	}

	/** @return the species name */
	String name()
	{
		return name;
	}

	/** @return the species' combat level */
	int speciesLevel()
	{
		return speciesLevel;
	}

	/** @return the species' rarity tier */
	Rarity rarity()
	{
		return rarity;
	}

	/** @return the species' combat style */
	CombatType type()
	{
		return type;
	}

	/** @return how many of this species the player has caught */
	int caughtCount()
	{
		return caughtCount;
	}

	/** @return whether the player has met this species as a wild creature */
	boolean seen()
	{
		return seen;
	}

	/** @return whether the player has owned a gilded creature of this species */
	boolean gilded()
	{
		return gilded;
	}

	/**
	 * Tells whether this is a quest-only species the player owns: it is listed as caught, can no longer be caught and
	 * does not count toward the Log total.
	 *
	 * @return true for a retired species
	 */
	boolean retired()
	{
		return retired;
	}

	/** @return whether the player has caught this species at least once */
	boolean caught()
	{
		return caughtCount > 0;
	}
}
