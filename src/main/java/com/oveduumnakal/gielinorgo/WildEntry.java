/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.Set;

/** A wild creature that is currently catchable. It has its own body on screen and is not tied to any monster. */
final class WildEntry
{
	private final int wildId;
	private final Species species;
	private final int level;
	private final int combatLevel;
	private final Set<Trait> traits;
	private final boolean gilded;
	private final boolean roaming;
	private boolean battled;
	private boolean weakened;
	private int npcId;

	WildEntry(int wildId, Species species, int level, int combatLevel)
	{
		this(wildId, species, level, combatLevel, false);
	}

	/**
	 * Creates a wild creature.
	 *
	 * @param wildId the creature's own id, from the tracker's counter
	 * @param species the species
	 * @param level the creature level it would be caught at
	 * @param combatLevel the monster's combat level in the game
	 * @param underground whether it was met underground, which gives it the {@link Trait#CAVE} trait
	 */
	WildEntry(int wildId, Species species, int level, int combatLevel, boolean underground)
	{
		this(wildId, species, level, combatLevel, underground, false);
	}

	/**
	 * Creates a wild creature that may be gilded.
	 *
	 * @param wildId the creature's own id, from the tracker's counter
	 * @param species the species
	 * @param level the creature level it would be caught at
	 * @param combatLevel the monster's combat level in the game
	 * @param underground whether it was met underground, which gives it the {@link Trait#CAVE} trait
	 * @param gilded whether it is a rare gilded creature
	 */
	WildEntry(int wildId, Species species, int level, int combatLevel, boolean underground, boolean gilded)
	{
		this(wildId, species, level, combatLevel, underground, gilded, false);
	}

	/**
	 * Creates a wild creature that may be gilded and may be a roaming one.
	 *
	 * @param wildId the creature's own id, from the tracker's counter
	 * @param species the species
	 * @param level the creature level it would be caught at, already raised for a roaming one
	 * @param combatLevel the monster's combat level in the game
	 * @param underground whether it was met underground, which gives it the {@link Trait#CAVE} trait
	 * @param gilded whether it is a rare gilded creature
	 * @param roaming whether it is a stronger roaming creature that drops a better egg
	 */
	WildEntry(int wildId, Species species, int level, int combatLevel, boolean underground, boolean gilded,
		boolean roaming)
	{
		this.gilded = gilded;
		this.roaming = roaming;
		this.wildId = wildId;
		this.species = species;
		this.level = level;
		this.combatLevel = combatLevel;
		Set<Trait> all = species.traits();
		if (underground)
			all.add(Trait.CAVE);

		this.traits = Collections.unmodifiableSet(all);
	}

	/**
	 * Notes which NPC this entry looks like, so its body and a creature caught from it are drawn as it.
	 *
	 * @param id the monster's NPC id
	 * @return this entry
	 */
	WildEntry withNpcId(int id)
	{
		this.npcId = id;
		return this;
	}

	/** @return the monster's NPC id, or 0 when it is not known */
	int npcId()
	{
		return npcId;
	}

	/** @return the creature's own id; no two wild creatures share one, and it is not a monster's index */
	int wildId()
	{
		return wildId;
	}

	/** @return the species */
	Species species()
	{
		return species;
	}

	/** @return the creature level it would be caught at */
	int level()
	{
		return level;
	}

	/** @return the monster's combat level in the game */
	int combatLevel()
	{
		return combatLevel;
	}

	/** @return what specialty jars can be good at for this creature: its species' traits, plus where it was met */
	Set<Trait> traits()
	{
		return traits;
	}

	/** @return whether this is a rare gilded creature */
	boolean gilded()
	{
		return gilded;
	}

	/** @return whether this is a stronger roaming creature, which drops a better egg when beaten */
	boolean roaming()
	{
		return roaming;
	}

	/** @return the words that go in front of the species name: "Gilded ", "Roaming ", both, or nothing */
	String prefix()
	{
		return (gilded ? "Gilded " : "") + (roaming ? "Roaming " : "");
	}

	/** @return whether the player has already battled this one */
	boolean battled()
	{
		return battled;
	}

	/** @return whether it lost a battle and is easier to catch */
	boolean weakened()
	{
		return weakened;
	}

	/**
	 * Records the outcome of the one battle allowed against this creature.
	 *
	 * @param playerWon whether the player's creature won
	 */
	void markBattled(boolean playerWon)
	{
		battled = true;
		weakened = playerWon;
	}
}
