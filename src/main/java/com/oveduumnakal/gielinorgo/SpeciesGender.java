/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The species that are plainly one gender, and the rules for every other creature's gender. A listed species always
 * has its gender: it wins over a saved or sent gender, so it holds through catching, hatching, starters, evolution,
 * trades and the rematch and rival teams. Every other species rolls 50/50 when it is made, and an older creature with
 * none saved gets a stable guess from {@link #stable(String, long)}.
 *
 * <p>Names are matched on the exact species name of {@code species.json}, ignoring case. {@code SpeciesGenderTest}
 * fails the build when a listed name is not a species.
 */
final class SpeciesGender
{
	private static final List<String> FEMALE = Collections.unmodifiableList(Arrays.asList(
		"Woman", "Gnome woman", "Ice troll female", "Frenzied ice troll female", "Ogress Shaman",
		"Ogress Warrior", "Ice Queen", "Kalphite Queen", "Queen spawn", "Penance Queen", "Drink troll queen",
		"Branda the Fire Queen", "The Maiden of Sugadinti", "Cow", "Banshee", "Twisted Banshee",
		"Screaming banshee", "Screaming twisted banshee"));

	private static final List<String> MALE = Collections.unmodifiableList(Arrays.asList(
		"Man", "Drunken man", "Ice troll male", "Frenzied ice troll male", "King Black Dragon", "King Roald",
		"King kurask", "King Scorpion", "King Sand Crab", "Maggot King", "Eldric the Ice King", "Duke Sucellus",
		"Sir Bedivere", "Sir Carl", "Sir Gawain", "Sir Harry", "Sir Jerro", "Sir Kay", "Sir Lancelot", "Sir Lucan",
		"Sir Mordred", "Sir Palomedes", "Sir Pelleas", "Sir Tristram", "Bull", "Ram", "Rooster"));

	private static final Map<String, Gender> FIXED = new HashMap<>();

	static
	{
		for (String name : FEMALE)
			FIXED.put(Species.keyFor(name), Gender.FEMALE);

		for (String name : MALE)
			FIXED.put(Species.keyFor(name), Gender.MALE);
	}

	private SpeciesGender()
	{
	}

	/** @return the species names that are always female, as written in {@code species.json} */
	static List<String> femaleNames()
	{
		return FEMALE;
	}

	/** @return the species names that are always male, as written in {@code species.json} */
	static List<String> maleNames()
	{
		return MALE;
	}

	/**
	 * Looks up the gender a species always has.
	 *
	 * @param species a species name or key
	 * @return the fixed gender, or null when the species can be either
	 */
	static Gender fixed(String species)
	{
		return FIXED.get(Species.keyFor(species));
	}

	/**
	 * Gives a new creature its gender: the fixed one, or a 50/50 roll. Rolls nothing for a fixed species, so a
	 * seeded random is read the same as before for those.
	 *
	 * @param species the species name or key
	 * @param random the source of the roll
	 * @return the gender
	 */
	static Gender roll(String species, Random random)
	{
		Gender fixed = fixed(species);
		if (fixed != null)
			return fixed;

		return random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
	}

	/**
	 * Works out a gender without a roll, the same every time: the fixed one, or a hash of the species and a number
	 * the creature carries. It is how a creature saved before gender, and one from a friend whose build sent none,
	 * get theirs.
	 *
	 * @param species the species name or key
	 * @param seed the creature's {@code caughtAtMs}, or its level for a friend's creature
	 * @return the gender
	 */
	static Gender stable(String species, long seed)
	{
		Gender fixed = fixed(species);
		if (fixed != null)
			return fixed;

		long h = Species.keyFor(species).hashCode() * 0x9E3779B97F4A7C15L + seed;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return (h & 1L) == 0 ? Gender.MALE : Gender.FEMALE;
	}
}
