/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.Locale;
import java.util.Set;

/**
 * A condition that lasts for part of a battle: poison, venom and burn take health at the end of each round, freeze
 * and stun make a creature lose turns. A creature has one at a time, and it ends with its time, with a switch or with
 * the battle. It is never saved. All the numbers here are first guesses for the owner to tune.
 */
enum Status
{
	POISON("PSN", "poisoned", "poison", new Color(0x4CD137), 4),
	VENOM("VNM", "envenomed", "venom", new Color(0x1E8449), 4),
	BURN("BRN", "burned", "burn", new Color(0xFF8C1A), 4),
	FREEZE("FRZ", "frozen", "freeze", new Color(0x4FE3F0), 2),
	STUN("STN", "stunned", "stun", new Color(0xF5E050), 3);

	/** Share of full health poison and burn take at the end of each round: 1/16. */
	static final double POISON_SHARE = 1.0 / 16.0;

	/** Share of full health venom takes in its first round, which it takes again, one more time, each round: 1/20. */
	static final double VENOM_SHARE = 1.0 / 20.0;

	/** What a burned creature's melee hits are multiplied by. */
	static final double BURN_MELEE = 0.75;

	/** The chance a stunned creature loses a turn. */
	static final double STUN_SKIP_CHANCE = 0.5;

	/** The chance a hit of a poisonous creature poisons, when the move has no effect of its own to roll. */
	static final double POISONOUS_HIT_CHANCE = 0.2;

	private final String tag;
	private final String past;
	private final String noun;
	private final Color color;
	private final int turns;

	Status(String tag, String past, String noun, Color color, int turns)
	{
		this.tag = tag;
		this.past = past;
		this.noun = noun;
		this.color = color;
		this.turns = turns;
	}

	/** @return the three letters shown over a creature that has it */
	String tag()
	{
		return tag;
	}

	/** @return the colour of the tag */
	Color color()
	{
		return color;
	}

	/** @return how many rounds (or, for a freeze, at most how many turns) it lasts; a freeze is rolled from 1 */
	int turns()
	{
		return turns;
	}

	/** @return the word for a creature that has it, such as {@code poisoned} */
	String past()
	{
		return past;
	}

	/** @return the plain name, such as {@code poison} */
	String noun()
	{
		return noun;
	}

	/** @return whether it takes health at the end of each round */
	boolean damaging()
	{
		return this == POISON || this == VENOM || this == BURN;
	}

	/** @return whether the creature can lose its turn to it */
	boolean skipping()
	{
		return this == FREEZE || this == STUN;
	}

	/** @return whether the Antidote cures it */
	boolean curable()
	{
		return damaging();
	}

	/**
	 * Tells whether a creature with these traits cannot have this status: poisonous creatures are immune to poison
	 * and venom, fiery creatures to burn.
	 *
	 * @param traits the creature's traits
	 * @return true when the status cannot be put on it
	 */
	boolean blockedBy(Set<Trait> traits)
	{
		if (this == POISON || this == VENOM)
			return traits.contains(Trait.POISONOUS);

		return this == BURN && traits.contains(Trait.FIERY);
	}

	/**
	 * Works out what a damaging status takes at the end of a round.
	 *
	 * @param maxHp the creature's full health
	 * @param venomStep how many rounds of venom it has already had, from 0
	 * @return the health taken, at least 1; 0 for a status that takes none
	 */
	int roundDamage(int maxHp, int venomStep)
	{
		if (this == VENOM)
			return Math.max(1, (int) Math.round(maxHp * VENOM_SHARE * (venomStep + 1)));

		return damaging() ? Math.max(1, (int) Math.round(maxHp * POISON_SHARE)) : 0;
	}

	/**
	 * Finds a status by the name a move gives it.
	 *
	 * @param name the status name in any letter case, or null
	 * @return the status, or null when the name is none
	 */
	static Status named(String name)
	{
		if (name == null)
			return null;

		try
		{
			return valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
