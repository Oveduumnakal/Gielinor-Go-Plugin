/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Something about a wild creature that a specialty jar can be good at. Most traits belong to the species and come
 * from the bundled species list; {@link #CAVE} depends on where the creature is met.
 */
enum Trait
{
	CAVE("creatures met underground"),
	FAST("fast attackers"),
	UNHOLY("undead and demons"),
	SLAYER("Slayer monsters"),
	POISONOUS("poisonous creatures"),
	FIERY("dragons and fiery creatures"),
	LARGE("large creatures"),
	AQUATIC("water creatures"),
	STAB("stabbing creatures"),
	SLASH("slashing creatures"),
	CRUSH("crushing creatures"),
	RANGED("ranged creatures"),
	MAGIC("magic creatures");

	private final String label;

	Trait(String label)
	{
		this.label = label;
	}

	/** @return the creatures this trait covers, worded to finish the sentence "works best on ..." */
	String label()
	{
		return label;
	}

	/**
	 * Finds the trait every creature of a combat style has.
	 *
	 * @param style the combat style
	 * @return the matching trait
	 */
	static Trait forStyle(CombatType style)
	{
		if (style == CombatType.RANGED)
			return RANGED;

		if (style == CombatType.MAGIC)
			return MAGIC;

		if (style == CombatType.STAB)
			return STAB;

		if (style == CombatType.SLASH)
			return SLASH;

		return CRUSH;
	}
}
