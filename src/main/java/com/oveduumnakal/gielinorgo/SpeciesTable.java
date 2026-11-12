/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/** Every catchable species, looked up by monster name. Loaded once from the bundled {@code species.json}. */
final class SpeciesTable
{
	/** Resource path of the bundled species list, relative to this package. */
	static final String RESOURCE = "species.json";

	private static final Type LIST_TYPE = new TypeToken<List<Species>>()
	{
	}.getType();

	private final Map<String, Species> byKey = new HashMap<>();
	private final List<Species> ordered;
	private final List<Species> catchable;

	SpeciesTable(List<Species> species)
	{
		List<Species> kept = new ArrayList<>();
		for (Species entry : species)
		{
			if (entry == null || entry.key().isEmpty() || entry.level() < 1)
				continue;

			if (byKey.putIfAbsent(entry.key(), entry) == null)
				kept.add(entry);
		}

		kept.sort(Comparator.comparingInt(Species::level).thenComparing(Species::key));
		ordered = Collections.unmodifiableList(kept);
		List<Species> open = new ArrayList<>();
		for (Species entry : kept)
		{
			if (!entry.questOnly())
				open.add(entry);
		}

		catchable = Collections.unmodifiableList(open);
	}

	/**
	 * Reads a species table from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON array of species
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static SpeciesTable load(Gson gson, Reader reader)
	{
		try
		{
			List<Species> species = gson.fromJson(reader, LIST_TYPE);
			return new SpeciesTable(species == null ? Collections.emptyList() : species);
		}
		catch (JsonParseException e)
		{
			return new SpeciesTable(Collections.emptyList());
		}
	}

	/**
	 * Finds a species by monster name.
	 *
	 * @param name a monster name or a saved species key
	 * @return the species, or null when the monster is not catchable
	 */
	Species find(String name)
	{
		return byKey.get(Species.keyFor(name));
	}

	/**
	 * Lists the species a player can still meet in the open world: every species that is not quest-only. Every pool
	 * that makes or asks for a creature draws from this list.
	 *
	 * @return the catchable species, weakest first
	 */
	List<Species> catchable()
	{
		return catchable;
	}

	/** @return how many species can be caught, the total of the Creature Log */
	int catchableSize()
	{
		return catchable.size();
	}

	/** @return every species, quest-only ones included, weakest first; creatures already owned may belong to them */
	List<Species> all()
	{
		return ordered;
	}

	/** @return how many species exist */
	int size()
	{
		return ordered.size();
	}
}
