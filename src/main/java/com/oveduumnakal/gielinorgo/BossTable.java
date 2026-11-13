/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * Which species a boss kill gives an egg of. Loaded from {@code bosses.json}, a hand-written map from the name in
 * the game's kill-count line to a species name.
 */
final class BossTable
{
	/** Resource path of the bundled boss list, relative to this package. */
	static final String RESOURCE = "bosses.json";

	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>()
	{
	}.getType();

	private final Map<String, Species> bosses = new HashMap<>();

	/**
	 * Builds the table, leaving out any entry whose species is not catchable.
	 *
	 * @param names chat name to species name
	 * @param species every species
	 */
	BossTable(Map<String, String> names, SpeciesTable species)
	{
		for (Map.Entry<String, String> entry : names.entrySet())
		{
			Species found = species.find(entry.getValue());
			if (entry.getKey() != null && found != null && !found.questOnly())
				bosses.put(Species.keyFor(entry.getKey()), found);
		}
	}

	/** @return a table with no bosses */
	static BossTable empty()
	{
		return new BossTable(Collections.emptyMap(), new SpeciesTable(Collections.emptyList()));
	}

	/**
	 * Reads a boss table from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON object
	 * @param species every species
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static BossTable load(Gson gson, Reader reader, SpeciesTable species)
	{
		try
		{
			Map<String, String> loaded = gson.fromJson(reader, MAP_TYPE);
			return loaded == null ? empty() : new BossTable(loaded, species);
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/**
	 * Finds the species a kill gives an egg of.
	 *
	 * @param chatName the name in the kill-count line, in any letter case
	 * @return the species, or null when the name is not in the table
	 */
	Species speciesFor(String chatName)
	{
		return chatName == null ? null : bosses.get(Species.keyFor(chatName));
	}

	/**
	 * Tells whether a species is one a boss egg can hold.
	 *
	 * @param species the species
	 * @return true when some kill-count name maps to it
	 */
	boolean isBoss(Species species)
	{
		if (species == null)
			return false;

		for (Species boss : bosses.values())
		{
			if (boss.key().equals(species.key()))
				return true;
		}

		return false;
	}

	/** @return how many names the table knows */
	int size()
	{
		return bosses.size();
	}
}
