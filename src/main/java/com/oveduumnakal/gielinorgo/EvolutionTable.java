/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
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
	}

	private final Map<String, Step> byFrom = new HashMap<>();

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

			byFrom.putIfAbsent(Species.keyFor(step.from), step);
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
	 * Finds what a species grows into.
	 *
	 * @param speciesKey the species key or name
	 * @return the next link, or null when the species is the end of its line or has none
	 */
	Step next(String speciesKey)
	{
		return byFrom.get(Species.keyFor(speciesKey));
	}

	/** @return how many species can evolve */
	int size()
	{
		return byFrom.size();
	}
}
