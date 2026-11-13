/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One species as the Creature Log shows it. Immutable, so it can cross from the game thread to the UI. */
final class LogEntry
{
	private final String name;
	private final int speciesLevel;
	private final Rarity rarity;
	private final CombatType type;
	private final int caughtCount;
	private final boolean seen;
	private final boolean gilded;
	private final boolean retired;
	private final String examine;
	private final List<Stage> line;

	/** One stage of an evolution line: its name, and whether the player has met it. */
	static final class Stage
	{
		private final String name;
		private final boolean known;
		private final String condition;
		private final List<Stage> alternatives;

		Stage(String name, boolean known)
		{
			this(name, known, null, Collections.emptyList());
		}

		/**
		 * Describes one stage.
		 *
		 * @param name the species name
		 * @param known whether the player has met it
		 * @param condition what the step into this stage needs besides the level, such as {@code at night}, or null
		 * @param alternatives the other species the same step can lead to, each with its own condition
		 */
		Stage(String name, boolean known, String condition, List<Stage> alternatives)
		{
			this.name = name;
			this.known = known;
			this.condition = condition;
			this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
		}

		/** @return what the step into this stage needs besides the level, or null when it needs nothing */
		String condition()
		{
			return condition;
		}

		/** @return the other results of the same step; empty unless the species branches */
		List<Stage> alternatives()
		{
			return alternatives;
		}

		/** @return the stage as the detail box writes it, such as {@code Werewolf (at night)} */
		String text()
		{
			return known && condition != null ? name + " (" + condition + ")" : known ? name : "???";
		}

		/** @return the species name */
		String name()
		{
			return name;
		}

		/** @return whether the player has seen or caught this species */
		boolean known()
		{
			return known;
		}
	}

	LogEntry(String name, int speciesLevel, Rarity rarity, CombatType type, int caughtCount, boolean seen,
		boolean gilded)
	{
		this(name, speciesLevel, rarity, type, caughtCount, seen, gilded, false);
	}

	LogEntry(String name, int speciesLevel, Rarity rarity, CombatType type, int caughtCount, boolean seen,
		boolean gilded, boolean retired)
	{
		this(name, speciesLevel, rarity, type, caughtCount, seen, gilded, retired, null, Collections.emptyList());
	}

	LogEntry(String name, int speciesLevel, Rarity rarity, CombatType type, int caughtCount, boolean seen,
		boolean gilded, boolean retired, String examine, List<Stage> line)
	{
		this.examine = examine;
		this.line = line;
		this.retired = retired;
		this.gilded = gilded;
		this.name = name;
		this.speciesLevel = speciesLevel;
		this.rarity = rarity;
		this.type = type;
		this.caughtCount = caughtCount;
		this.seen = seen;
	}

	/** @return the species name */
	String name()
	{
		return name;
	}

	/** @return the species' combat level */
	int speciesLevel()
	{
		return speciesLevel;
	}

	/** @return the species' rarity tier */
	Rarity rarity()
	{
		return rarity;
	}

	/** @return the species' combat style */
	CombatType type()
	{
		return type;
	}

	/** @return how many of this species the player has caught */
	int caughtCount()
	{
		return caughtCount;
	}

	/** @return whether the player has met this species as a wild creature */
	boolean seen()
	{
		return seen;
	}

	/** @return whether the player has owned a gilded creature of this species */
	boolean gilded()
	{
		return gilded;
	}

	/**
	 * Tells whether this is a quest-only species the player owns: it is listed as caught, can no longer be caught and
	 * does not count toward the Log total.
	 *
	 * @return true for a retired species
	 */
	boolean retired()
	{
		return retired;
	}

	/** @return whether the player has caught this species at least once */
	boolean caught()
	{
		return caughtCount > 0;
	}

	/** @return the wiki's examine text for this species, or null when there is none */
	String examine()
	{
		return examine;
	}

	/** @return the species' evolution line, weakest first; empty when it neither grows from nor into anything */
	List<Stage> line()
	{
		return line;
	}

	/**
	 * Writes the evolution line for the detail box: stages the player has not met show as {@code ???}.
	 *
	 * @return for example {@code Imp -> Lesser demon -> ???}, or an empty string when the species has no line
	 */
	String lineText()
	{
		if (line.size() < 2)
			return "";

		StringBuilder text = new StringBuilder();
		for (Stage stage : line)
		{
			if (text.length() > 0)
				text.append(" \u2192 ");

			text.append(stage.text());
			for (Stage other : stage.alternatives())
				text.append(" / ").append(other.text());
		}

		return text.toString();
	}

	/** @return the player's record for the detail box, such as {@code Caught 4 - gilded owned} */
	String recordText()
	{
		String text = caught() ? "Caught " + caughtCount : "Seen, not caught yet";
		return gilded ? text + " \u00B7 gilded owned" : text;
	}
}
