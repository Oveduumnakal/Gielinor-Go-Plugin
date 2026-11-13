/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One catchable kind of monster, as listed in the bundled {@code species.json}. */
final class Species
{
	private String name;
	private int level;
	private CombatType style;
	private List<Trait> traits;
	private int[] ids;
	private boolean quest;

	/** Used by Gson. */
	Species()
	{
	}

	Species(String name, int level, CombatType style, Trait... traits)
	{
		this.name = name;
		this.level = level;
		this.style = style;
		this.traits = Arrays.asList(traits);
	}

	/** @return the in-game monster name */
	String name()
	{
		return name;
	}

	/** @return the species' combat level, taken from its strongest variant */
	int level()
	{
		return level;
	}

	/** @return the combat style, Crush when the data file leaves it out */
	CombatType style()
	{
		return style == null ? CombatType.CRUSH : style;
	}

	/**
	 * Everything a specialty jar can be good at for this species.
	 *
	 * @return the traits from the species list plus the one for its combat style, in a set the caller may change
	 */
	Set<Trait> traits()
	{
		EnumSet<Trait> all = EnumSet.of(Trait.forStyle(style()));
		if (traits == null)
			return all;

		for (Trait trait : traits)
		{
			if (trait != null)
				all.add(trait);
		}

		return all;
	}

	/**
	 * Tells whether this species only exists during a quest. Such a species is still a species: a creature of it that
	 * the player already owns draws, fights, evolves and trades as before. It never turns wild, hatches, is featured
	 * or evolved into, and it does not count toward the Creature Log.
	 *
	 * @return true when the species can no longer be met in the open world once every quest is done
	 */
	boolean questOnly()
	{
		return quest;
	}

	/**
	 * Marks the species as quest-only, for tests and for building species in code.
	 *
	 * @return this species
	 */
	Species asQuestOnly()
	{
		this.quest = true;
		return this;
	}

	/** @return the NPC ids the game uses for this species, for drawing its portrait; empty when none are known */
	int[] ids()
	{
		return ids == null ? new int[0] : ids.clone();
	}

	/**
	 * Picks the NPC id to draw a creature of this species with.
	 *
	 * @param saved the NPC id the creature was caught as, or 0 when unknown
	 * @return the saved id when it is one of the species' ids, else the species' first id; 0 when the species has no
	 *     ids (an id that data changes dropped from the list falls back to the first one)
	 */
	int npcIdFor(int saved)
	{
		if (ids == null || ids.length == 0)
			return 0;

		if (saved > 0)
		{
			for (int id : ids)
			{
				if (id == saved)
					return saved;
			}
		}

		return ids[0];
	}

	/** @return the rarity tier for this species' combat level */
	Rarity rarity()
	{
		return Rarity.forLevel(level);
	}

	/** @return the lookup key for this species, see {@link #keyFor(String)} */
	String key()
	{
		return keyFor(name);
	}

	/**
	 * Turns a monster name into the key used in the species table and in saved data.
	 *
	 * @param name a monster name as the game or the data file gives it
	 * @return the name trimmed, lower-cased, and with non-breaking spaces made plain; empty for null
	 */
	static String keyFor(String name)
	{
		if (name == null)
			return "";

		return name.replace(' ', ' ')
			.trim()
			.toLowerCase(Locale.ROOT);
	}
}
