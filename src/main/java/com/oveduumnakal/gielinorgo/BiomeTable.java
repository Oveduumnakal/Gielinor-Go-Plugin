/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * The primary biome of each species, which picks the background behind it. {@code scripts/gen-biomes.py} writes the
 * file from {@code docs/SPECIES.md}. Holds no RuneLite types.
 */
final class BiomeTable
{
	/** Resource name, relative to this package. */
	static final String RESOURCE = "biomes.json";

	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>()
	{
	}.getType();

	private final Map<String, String> byKey;

	private BiomeTable(Map<String, String> byKey)
	{
		this.byKey = byKey;
	}

	/**
	 * Reads the bundled table.
	 *
	 * @param gson the Gson to parse with
	 * @return the table, empty when it cannot be read, which leaves every creature on the plain box
	 */
	static BiomeTable bundled(Gson gson)
	{
		try (InputStream in = BiomeTable.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
				return new BiomeTable(Collections.emptyMap());

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				Map<String, String> read = gson.fromJson(reader, MAP_TYPE);
				Map<String, String> byKey = new HashMap<>();
				if (read != null)
					read.forEach((name, biome) -> byKey.put(Species.keyFor(name), biome));

				return new BiomeTable(byKey);
			}
		}
		catch (IOException | JsonParseException e)
		{
			return new BiomeTable(Collections.emptyMap());
		}
	}

	/**
	 * @param species a species' name or key
	 * @return the folder of its biome's loop, or null when none is known
	 */
	String of(String species)
	{
		return species == null ? null : byKey.get(Species.keyFor(species));
	}
}
