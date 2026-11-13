/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * Which species grow into which, and at what creature level. Loaded once from the bundled
 * {@code evolutions.json}.
 */
final class EvolutionTable
{
	/** Resource path of the bundled evolution list, relative to this package. */
	static final String RESOURCE = "evolutions.json";

	private static final Type LIST_TYPE = new TypeToken<List<Step>>()
	{
	}.getType();

	/** One link in an evolution line, as written in the data file. */
	static final class Step
	{
		private String from;
		private String to;
		private int level;
		private EvolutionCondition when;

		/** Used by Gson. */
		Step()
		{
		}

		Step(String from, String to, int level)
		{
			this.from = from;
			this.to = to;
			this.level = level;
		}

		Step(String from, String to, int level, EvolutionCondition when)
		{
			this(from, to, level);
			this.when = when;
		}

		/** @return the name of the species that evolves */
		String from()
		{
			return from;
		}

		/** @return the name of the species it becomes */
		String to()
		{
			return to;
		}

		/** @return the creature level at which it evolves */
		int level()
		{
			return level;
		}

		/** @return what else the creature needs besides the level, or null when the level is all it needs */
		EvolutionCondition when()
		{
			return when;
		}

		/**
		 * Tells whether the step may be taken in the given world.
		 *
		 * @param now the world right now
		 * @return true when the step has no condition or its condition holds
		 */
		boolean allowed(Conditions now)
		{
			return when == null || when.holds(now);
		}
	}

	private final Map<String, List<Step>> byFrom = new HashMap<>();
	private final Map<String, Step> byTo = new LinkedHashMap<>();

	/**
	 * Builds the table, leaving out any link whose species the game does not know and any link that ends in a
	 * quest-only species (a link from one stays, for creatures already owned).
	 *
	 * @param steps the links from the data file
	 * @param species every catchable species
	 */
	EvolutionTable(List<Step> steps, SpeciesTable species)
	{
		for (Step step : steps)
		{
			if (step == null || species.find(step.from) == null || species.find(step.to) == null)
				continue;

			if (species.find(step.to).questOnly())
				continue;

			byFrom.computeIfAbsent(Species.keyFor(step.from), key -> new ArrayList<>()).add(step);
			byTo.putIfAbsent(Species.keyFor(step.to), step);
		}
	}

	/** @return a table in which nothing evolves */
	static EvolutionTable empty()
	{
		return new EvolutionTable(Collections.emptyList(), new SpeciesTable(Collections.emptyList()));
	}

	/**
	 * Reads an evolution table from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON array of links
	 * @param species every catchable species
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static EvolutionTable load(Gson gson, Reader reader, SpeciesTable species)
	{
		try
		{
			List<Step> steps = gson.fromJson(reader, LIST_TYPE);
			return new EvolutionTable(steps == null ? Collections.emptyList() : steps, species);
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/**
	 * Finds the first link out of a species, whatever its condition. Use it to describe a line, not to evolve.
	 *
	 * @param speciesKey the species key or name
	 * @return the first link, or null when the species is the end of its line or has none
	 */
	Step next(String speciesKey)
	{
		List<Step> all = steps(speciesKey);
		return all.isEmpty() ? null : all.get(0);
	}

	/**
	 * Lists every link out of a species, in the order of the data file.
	 *
	 * @param speciesKey the species key or name
	 * @return the links; empty when the species is the end of its line or has none
	 */
	List<Step> steps(String speciesKey)
	{
		List<Step> found = byFrom.get(Species.keyFor(speciesKey));
		return found == null ? Collections.emptyList() : Collections.unmodifiableList(found);
	}

	/**
	 * Finds the link a creature of this species would take in the given world, level aside.
	 *
	 * @param speciesKey the species key or name
	 * @param now the world right now
	 * @return the first link whose condition holds, or null when there is none
	 */
	Step next(String speciesKey, Conditions now)
	{
		for (Step step : steps(speciesKey))
		{
			if (step.allowed(now))
				return step;
		}

		return null;
	}

	/**
	 * Finds what a creature that has reached its evolution level is still waiting for.
	 *
	 * @param speciesKey the species key or name
	 * @param level the creature's level
	 * @param now the world right now
	 * @return the condition of the first link it has the level for, or null when it can evolve now or is not ready
	 */
	EvolutionCondition waitingFor(String speciesKey, int level, Conditions now)
	{
		EvolutionCondition waiting = null;
		for (Step step : steps(speciesKey))
		{
			if (level < step.level())
				continue;

			if (step.allowed(now))
				return null;

			if (waiting == null)
				waiting = step.when();
		}

		return waiting;
	}

	/**
	 * Finds what a species grows from.
	 *
	 * @param speciesKey the species key or name
	 * @return the first link that ends in the species, or null when nothing grows into it
	 */
	Step previous(String speciesKey)
	{
		return byTo.get(Species.keyFor(speciesKey));
	}

	/** @return how many species can evolve */
	int size()
	{
		return byFrom.size();
	}
}
