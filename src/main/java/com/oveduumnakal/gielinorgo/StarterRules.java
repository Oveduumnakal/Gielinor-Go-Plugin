/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Who a new player may start with, and when. A starter is offered while the player has no creature anywhere and has
 * never caught a species; the three below make the style triangle, one of each. No RuneLite types here, so every rule
 * is tested with a seeded random.
 */
final class StarterRules
{
	/** The species offered, by name: one Slash-or-Melee, one Ranged and one Magic. */
	static final List<String> STARTERS = Collections.unmodifiableList(Arrays.asList("Imp", "Gnome Archer", "Wizard"));

	/** The level a starter joins at. */
	static final int LEVEL = 5;

	private StarterRules()
	{
	}

	/**
	 * Tells whether the starter choice is open for a save.
	 *
	 * @param data the save
	 * @return true while the team and Storage are empty and the Creature Log has no caught species
	 */
	static boolean offered(SaveData data)
	{
		return data.team.isEmpty() && data.storage.isEmpty() && data.caught.isEmpty();
	}

	/**
	 * Rolls the three starters like a catch: stat rolls and a temperament each, never gilded. They are not saved.
	 *
	 * @param random the source of every roll
	 * @param nowMs the time to stamp them with
	 * @return one creature per entry of {@link #STARTERS}, in that order
	 */
	static List<Creature> roll(Random random, long nowMs)
	{
		List<Creature> rolled = new ArrayList<>();
		for (String name : STARTERS)
		{
			Creature creature = new Creature(Species.keyFor(name), LEVEL, nowMs);
			creature.setRolls(StatRolls.roll(random));
			rolled.add(creature);
		}

		return rolled;
	}
}
