/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Objects;

/** One owned creature as the side panel shows it. Immutable, so it can cross from the game thread to the UI. */
final class CreatureView
{
	private final String name;
	private final int level;
	private final double xpProgress;
	private final double hpFraction;
	private final Rarity rarity;
	private final CombatType type;
	private final boolean gilded;

	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded)
	{
		this.gilded = gilded;
		this.name = name;
		this.level = level;
		this.xpProgress = xpProgress;
		this.hpFraction = hpFraction;
		this.rarity = rarity;
		this.type = type;
	}

	/** @return the species name */
	String name()
	{
		return name;
	}

	/** @return the creature level, 1 to 99 */
	int level()
	{
		return level;
	}

	/** @return progress to the next level, from 0 to 1 */
	double xpProgress()
	{
		return xpProgress;
	}

	/** @return the share of full health, from 0 to 1 */
	double hpFraction()
	{
		return hpFraction;
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

	/** @return whether this is a rare gilded creature */
	boolean gilded()
	{
		return gilded;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof CreatureView))
			return false;

		CreatureView that = (CreatureView) other;
		return level == that.level
			&& gilded == that.gilded
			&& Double.compare(xpProgress, that.xpProgress) == 0
			&& Double.compare(hpFraction, that.hpFraction) == 0
			&& rarity == that.rarity
			&& type == that.type
			&& Objects.equals(name, that.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, level, xpProgress, hpFraction, rarity, type, gilded);
	}
}
