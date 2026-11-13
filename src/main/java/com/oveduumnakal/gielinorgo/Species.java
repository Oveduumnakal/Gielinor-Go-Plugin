/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
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
	private double[] stats;

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

	/** Lowest factor a species may have for any stat. */
	static final double MIN_FACTOR = 0.75;

	/** Highest factor a species may have for any stat. */
	static final double MAX_FACTOR = 1.25;

	/** A factor this far from 1.0 or more is worth a word on the card. */
	static final double WORD_AT = 0.12;

	/**
	 * How much better or worse this species is than the baseline at one stat. Written by gen-species-stats.py from
	 * the wiki monster's numbers.
	 *
	 * @param stat the stat
	 * @return the factor, 0.75 to 1.25; 1.0 when the data has none, so a species built by hand is the baseline
	 */
	double factor(Stat stat)
	{
		if (stats == null || stats.length != StatRolls.COUNT)
			return 1.0;

		return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, stats[stat.ordinal()]));
	}

	/**
	 * The words that say what this species is good or bad at: a factor of 1.12 or more names it ({@code Tough},
	 * {@code Hits hard}, {@code Sturdy}, {@code Fast}), 0.88 or less names the opposite ({@code Frail},
	 * {@code Hits soft}, {@code Brittle}, {@code Slow}). The two stats furthest from 1.0 come first.
	 *
	 * @param limit the most words to return
	 * @return the words, strongest first; empty for a species near the baseline in every stat
	 */
	List<String> words(int limit)
	{
		List<Stat> stats = new ArrayList<>();
		for (Stat stat : Stat.values())
		{
			if (Math.abs(factor(stat) - 1.0) >= WORD_AT - 1e-9)
				stats.add(stat);
		}

		stats.sort((a, b) -> Double.compare(Math.abs(factor(b) - 1.0), Math.abs(factor(a) - 1.0)));
		List<String> words = new ArrayList<>();
		for (Stat stat : stats)
		{
			if (words.size() < limit)
				words.add(word(stat, factor(stat) > 1.0));
		}

		return words;
	}

	private static String word(Stat stat, boolean high)
	{
		switch (stat)
		{
			case HEALTH:
				return high ? "Tough" : "Frail";
			case ATTACK:
				return high ? "Hits hard" : "Hits soft";
			case DEFENCE:
				return high ? "Sturdy" : "Brittle";
			default:
				return high ? "Fast" : "Slow";
		}
	}

	/**
	 * Lists what this species does to each stat, for a tooltip.
	 *
	 * @return the four stats with their percentage, such as {@code Health +12% · Attack -5% · Defence 0% · Speed +25%}
	 */
	String describeFactors()
	{
		StringBuilder text = new StringBuilder();
		for (Stat stat : Stat.values())
		{
			long percent = Math.round((factor(stat) - 1.0) * 100);
			text.append(text.length() == 0 ? "" : " \u00b7 ")
				.append(stat.label())
				.append(' ')
				.append(percent > 0 ? "+" : "")
				.append(percent)
				.append('%');
		}

		return text.toString();
	}

	/**
	 * Sets the stat factors, for tests and for building species in code.
	 *
	 * @param health the health factor
	 * @param attack the attack factor
	 * @param defence the defence factor
	 * @param speed the speed factor
	 * @return this species
	 */
	Species withFactors(double health, double attack, double defence, double speed)
	{
		this.stats = new double[] {health, attack, defence, speed};
		return this;
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
