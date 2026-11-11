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

	LogEntry(String name, int speciesLevel, Rarity rarity, CombatType type, int caughtCount, boolean seen,
		boolean gilded)
	{
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

	/** @return whether the player has caught this species at least once */
	boolean caught()
	{
		return caughtCount > 0;
	}
}
