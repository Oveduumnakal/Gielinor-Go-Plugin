/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/** Every City Champion, in the order the player is meant to meet them. Loaded from {@code champions.json}. */
final class ChampionTable
{
	/** Resource path of the bundled champion list, relative to this package. */
	static final String RESOURCE = "champions.json";

	private static final Type LIST_TYPE = new TypeToken<List<Champion>>()
	{
	}.getType();

	private final List<Champion> champions;

	/**
	 * Builds the table, leaving out any champion with no id or with a creature the game does not know.
	 *
	 * @param loaded the champions from the data file
	 * @param species every catchable species
	 */
	ChampionTable(List<Champion> loaded, SpeciesTable species)
	{
		List<Champion> kept = new ArrayList<>();
		for (Champion champion : loaded)
		{
			if (usable(champion, species))
				kept.add(champion);
		}

		champions = Collections.unmodifiableList(kept);
	}

	/** @return a table with no champions */
	static ChampionTable empty()
	{
		return new ChampionTable(Collections.emptyList(), new SpeciesTable(Collections.emptyList()));
	}

	/**
	 * Reads a champion table from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON array of champions
	 * @param species every catchable species
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static ChampionTable load(Gson gson, Reader reader, SpeciesTable species)
	{
		try
		{
			List<Champion> loaded = gson.fromJson(reader, LIST_TYPE);
			return new ChampionTable(loaded == null ? Collections.emptyList() : loaded, species);
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/** @return every champion, easiest first */
	List<Champion> all()
	{
		return champions;
	}

	/**
	 * Finds a champion by id.
	 *
	 * @param id the champion's id
	 * @return the champion, or null when there is none with that id
	 */
	Champion byId(String id)
	{
		for (Champion champion : champions)
		{
			if (champion.id().equals(id))
				return champion;
		}

		return null;
	}

	/**
	 * Finds the champion whose city a tile is in.
	 *
	 * @param x world x
	 * @param y world y
	 * @param plane world plane; cities are on the surface, plane 0
	 * @return the champion, or null when the tile is in no champion's city
	 */
	Champion at(int x, int y, int plane)
	{
		if (plane != 0)
			return null;

		for (Champion champion : champions)
		{
			if (champion.contains(x, y))
				return champion;
		}

		return null;
	}

	private static boolean usable(Champion champion, SpeciesTable species)
	{
		if (champion == null || champion.id() == null || champion.city() == null || champion.team().isEmpty())
			return false;

		for (Champion.Member member : champion.team())
		{
			if (member == null || species.find(member.species()) == null)
				return false;
		}

		return true;
	}
}
