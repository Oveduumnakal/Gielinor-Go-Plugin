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
	private final String nickname;
	private final StatRolls rolls;

	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded, String nickname)
	{
		this(name, level, xpProgress, hpFraction, rarity, type, gilded, nickname, StatRolls.AVERAGE);
	}

	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded, String nickname, StatRolls rolls)
	{
		this.rolls = rolls;
		this.gilded = gilded;
		this.nickname = nickname;
		this.name = name;
		this.level = level;
		this.xpProgress = xpProgress;
		this.hpFraction = hpFraction;
		this.rarity = rarity;
		this.type = type;
	}

	/** @return the species name, which stays the species name even when there is a nickname */
	String name()
	{
		return name;
	}

	/** @return the nickname the player gave it, or null when it has none */
	String nickname()
	{
		return nickname;
	}

	/** @return the creature's rolls and temperament */
	StatRolls rolls()
	{
		return rolls;
	}

	/** @return the one-word rating of its rolls */
	StatRolls.Rating rating()
	{
		return rolls.rating();
	}

	/** @return the name of its temperament, such as Brave */
	String temperamentLabel()
	{
		return rolls.temperament().label();
	}

	/** @return what its temperament does, such as {@code +10% Speed, -10% Health}, or {@code no effect} */
	String temperamentEffect()
	{
		return rolls.temperament().effect();
	}

	/** @return its rolls with the net percentage each stat gets, as two HTML lines */
	String rollsNetLines()
	{
		return rolls.describeNet();
	}

	/** @return its four rolls as a line, such as {@code Health +3 · Attack -1 · Defence 0 · Speed +5} */
	String rollsLine()
	{
		return rolls.describe();
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
			&& Objects.equals(name, that.name)
			&& Objects.equals(nickname, that.nickname)
			&& rolls.equals(that.rolls);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, level, xpProgress, hpFraction, rarity, type, gilded, nickname, rolls);
	}
}
