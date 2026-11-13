/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The rival's rules: which starter the rival takes, when each of the five battles opens and what a win pays. No
 * RuneLite types, so every rule is tested without a client.
 */
final class RivalRules
{
	/** The id the panel passes back to challenge the rival; no City Champion has it. */
	static final String ID = "rival";

	/** How many battles the rival fights in all. */
	static final int BATTLES = 5;

	/** The sigils needed before each of the first four battles open. The fifth needs the first Elite clear. */
	private static final int[] SIGILS = {2, 4, 6, 8};

	/** Jars paid for a first win of each battle, of the best tier the player has unlocked. */
	private static final int[] JARS = {3, 4, 5, 6, 8};

	/** Egg paid for a first win of each battle. */
	private static final Rarity[] EGGS = {Rarity.RARE, Rarity.RARE, Rarity.EPIC, Rarity.EPIC, Rarity.LEGENDARY};

	/** The species key of the starter that takes each starter on, by the species key of the player's starter. */
	private static final Map<String, String> COUNTERS = counters();

	private RivalRules()
	{
	}

	private static Map<String, String> counters()
	{
		Map<String, String> map = new HashMap<>();
		map.put(Species.keyFor("Imp"), Species.keyFor("Wizard"));
		map.put(Species.keyFor("Wizard"), Species.keyFor("Gnome Archer"));
		map.put(Species.keyFor("Gnome Archer"), Species.keyFor("Imp"));
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Gives the starter that takes a player's starter on.
	 *
	 * @param playerStarter the species key of one of the {@link StarterRules#STARTERS}
	 * @return the rival's starter, or null when the species is not a starter
	 */
	static String counterOf(String playerStarter)
	{
		return COUNTERS.get(Species.keyFor(playerStarter));
	}

	/**
	 * Picks the rival's starter for a save that never recorded the player's starter: the one that takes on the
	 * starter sharing the lead's combat style, with Stab, Slash and Crush all answered like the Imp's Slash. A lead
	 * that matches no starter gives the Imp.
	 *
	 * @param lead the species of the player's lead, or null when there is none
	 * @param species every species, to read the starters' styles from
	 * @return the species key of the rival's starter
	 */
	static String starterAgainst(Species lead, SpeciesTable species)
	{
		if (lead != null && lead.style().melee())
			return counterOf("Imp");

		if (lead != null)
		{
			for (String name : StarterRules.STARTERS)
			{
				Species starter = species.find(name);
				if (starter != null && starter.style() == lead.style())
					return counterOf(name);
			}
		}

		return Species.keyFor("Imp");
	}

	/**
	 * Counts the rival battles that are open.
	 *
	 * @param sigils the City Champion sigils held
	 * @param eliteClears how many times the Elite challenge has been cleared
	 * @return how many of the battles can be fought, 0 to {@link #BATTLES}
	 */
	static int unlocked(int sigils, int eliteClears)
	{
		int open = 0;
		for (int needed : SIGILS)
		{
			if (sigils >= needed)
				open++;
		}

		return eliteClears > 0 ? Math.min(BATTLES, open + 1) : open;
	}

	/**
	 * Says what opens a battle.
	 *
	 * @param battle the battle, counted from 1
	 * @return the requirement, such as {@code Earn 4 sigils}
	 */
	static String requirement(int battle)
	{
		return battle <= SIGILS.length ? "Earn " + SIGILS[battle - 1] + " sigils" : "Clear the Elite challenge";
	}

	/**
	 * Gives the jars a first win pays.
	 *
	 * @param battle the battle, counted from 1
	 * @return the number of jars
	 */
	static int jars(int battle)
	{
		return JARS[Math.max(1, Math.min(BATTLES, battle)) - 1];
	}

	/**
	 * Gives the egg a first win pays.
	 *
	 * @param battle the battle, counted from 1
	 * @return the egg's rarity
	 */
	static Rarity egg(int battle)
	{
		return EGGS[Math.max(1, Math.min(BATTLES, battle)) - 1];
	}
}
