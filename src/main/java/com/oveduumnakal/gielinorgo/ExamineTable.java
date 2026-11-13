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
 * The wiki's examine line for each species, shown in the Creature Log. Loaded once from the bundled
 * {@code examine.json}, which {@code scripts/gen-species.py} writes beside the species list.
 */
final class ExamineTable
{
	/** Resource path of the bundled examine lines, relative to this package. */
	static final String RESOURCE = "examine.json";

	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>()
	{
	}.getType();

	private final Map<String, String> byKey = new HashMap<>();

	/**
	 * Builds the table.
	 *
	 * @param lines the examine line of each species, by species key
	 */
	ExamineTable(Map<String, String> lines)
	{
		for (Map.Entry<String, String> line : lines.entrySet())
		{
			if (line.getKey() != null && line.getValue() != null && !line.getValue().trim().isEmpty())
				byKey.put(Species.keyFor(line.getKey()), line.getValue().trim());
		}
	}

	/** @return a table with no lines */
	static ExamineTable empty()
	{
		return new ExamineTable(Collections.emptyMap());
	}

	/**
	 * Reads the table from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON object of species key to examine line
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static ExamineTable load(Gson gson, Reader reader)
	{
		try
		{
			Map<String, String> lines = gson.fromJson(reader, MAP_TYPE);
			return lines == null ? empty() : new ExamineTable(lines);
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/**
	 * Finds the examine line of a species.
	 *
	 * @param speciesKey the species key or name
	 * @return the line, or null when the wiki has none
	 */
	String textFor(String speciesKey)
	{
		return byKey.get(Species.keyFor(speciesKey));
	}

	/** @return how many species have a line */
	int size()
	{
		return byKey.size();
	}
}
