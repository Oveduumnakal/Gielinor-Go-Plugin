/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** The three combat styles. Each one beats exactly one other, as in the OSRS combat triangle. */
enum CombatType
{
	MELEE("Melee"),
	RANGED("Ranged"),
	MAGIC("Magic");

	/** Damage multiplier when the attacker's style beats the defender's. */
	static final double STRONG = 1.25;

	/** Damage multiplier when the attacker's style loses to the defender's. */
	static final double WEAK = 0.8;

	private final String label;

	CombatType(String label)
	{
		this.label = label;
	}

	/** @return the name shown to the player */
	String label()
	{
		return label;
	}

	/** @return the style this one is strong against: melee beats ranged, ranged beats magic, magic beats melee */
	CombatType beats()
	{
		if (this == MELEE)
			return RANGED;

		if (this == RANGED)
			return MAGIC;

		return MELEE;
	}

	/** @return the style that is strong against this one: melee loses to magic, ranged to melee, magic to ranged */
	CombatType losesTo()
	{
		return beats().beats();
	}

	/** @return one quiet sentence for a tooltip: what this style beats and what beats it */
	String matchup()
	{
		return label + " beats " + beats().label() + ", loses to " + losesTo().label();
	}

	/**
	 * Damage multiplier for an attack of this style against a defender.
	 *
	 * @param defender the defender's style
	 * @return {@link #STRONG}, {@link #WEAK}, or 1 for a mirror match
	 */
	double multiplierAgainst(CombatType defender)
	{
		if (beats() == defender)
			return STRONG;

		if (defender.beats() == this)
			return WEAK;

		return 1.0;
	}
}
