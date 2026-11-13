/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/**
 * The five combat styles. Each one beats exactly two others and loses to the other two: Stab beats Slash and Ranged,
 * Slash beats Crush and Ranged, Crush beats Stab and Magic, Ranged beats Crush and Magic, Magic beats Stab and Slash.
 */
enum CombatType
{
	STAB("Stab"),
	SLASH("Slash"),
	CRUSH("Crush"),
	RANGED("Ranged"),
	MAGIC("Magic");

	/** Damage multiplier when the attacker's style beats the defender's. */
	static final double STRONG = 1.25;

	/** Damage multiplier when the attacker's style loses to the defender's. */
	static final double WEAK = 0.8;

	/** Row is the attacker, column the defender: true when the attacker's style is strong against it. */
	private static final boolean[][] BEATS =
		{
			{ false, true, false, true, false },
			{ false, false, true, true, false },
			{ true, false, false, false, true },
			{ false, false, true, false, true },
			{ true, true, false, false, false }
		};

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

	/** @return whether this is one of the three melee styles, whose attacker walks up to the target */
	boolean melee()
	{
		return this == STAB || this == SLASH || this == CRUSH;
	}

	/**
	 * Tells whether this style is strong against another.
	 *
	 * @param defender the defender's style
	 * @return true when an attack of this style gets {@link #STRONG}
	 */
	boolean beats(CombatType defender)
	{
		return BEATS[ordinal()][defender.ordinal()];
	}

	/** @return the two styles this one is strong against, in the order of the enum */
	List<CombatType> strongAgainst()
	{
		return related(true);
	}

	/** @return the two styles this one is weak against, in the order of the enum */
	List<CombatType> weakAgainst()
	{
		return related(false);
	}

	private List<CombatType> related(boolean strong)
	{
		List<CombatType> found = new ArrayList<>();
		for (CombatType other : values())
		{
			if (strong ? beats(other) : other.beats(this))
				found.add(other);
		}

		return found;
	}

	/** @return one quiet sentence for a tooltip: what this style beats and what beats it */
	String matchup()
	{
		List<CombatType> strong = strongAgainst();
		List<CombatType> weak = weakAgainst();
		return label + " beats " + strong.get(0).label() + " and " + strong.get(1).label() + ", loses to "
			+ weak.get(0).label() + " and " + weak.get(1).label();
	}

	/**
	 * Damage multiplier for an attack of this style against a defender.
	 *
	 * @param defender the defender's style
	 * @return {@link #STRONG}, {@link #WEAK}, or 1 for a mirror match
	 */
	double multiplierAgainst(CombatType defender)
	{
		if (beats(defender))
			return STRONG;

		if (defender.beats(this))
			return WEAK;

		return 1.0;
	}
}
