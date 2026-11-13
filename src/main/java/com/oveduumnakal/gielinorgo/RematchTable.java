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
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * The second team each City Champion fields once the player has cleared the Elite challenge. Loaded from
 * {@code champion-rematches.json}. A champion with no usable rematch team simply has none, and is fought with the
 * first team as before.
 */
final class RematchTable
{
	/** Resource path of the bundled rematch teams, relative to this package. */
	static final String RESOURCE = "champion-rematches.json";

	/** How many creatures a rematch team has. */
	static final int TEAM_SIZE = 6;

	/** The lowest level a rematch creature may have. */
	static final int MIN_LEVEL = 80;

	/** The highest level a rematch creature may have. */
	static final int MAX_LEVEL = 99;

	private static final Type LIST_TYPE = new TypeToken<List<Entry>>()
	{
	}.getType();

	/** One champion's rematch team, as written in the data file. */
	static final class Entry
	{
		private String id;
		private List<Champion.Member> team;

		/** Used by Gson. */
		Entry()
		{
		}

		Entry(String id, List<Champion.Member> team)
		{
			this.id = id;
			this.team = team;
		}
	}

	private final Map<String, List<Champion.Member>> teams;

	/**
	 * Builds the table, leaving out any team that is not six known creatures at levels 80 to 99.
	 *
	 * @param loaded the entries from the data file
	 * @param species every catchable species
	 */
	RematchTable(List<Entry> loaded, SpeciesTable species)
	{
		Map<String, List<Champion.Member>> kept = new HashMap<>();
		for (Entry entry : loaded)
		{
			if (entry != null && entry.id != null && usable(entry.team, species))
				kept.put(entry.id, Collections.unmodifiableList(new ArrayList<>(entry.team)));
		}

		teams = Collections.unmodifiableMap(kept);
	}

	/** @return a table with no teams */
	static RematchTable empty()
	{
		return new RematchTable(Collections.emptyList(), new SpeciesTable(Collections.emptyList()));
	}

	/**
	 * Reads the rematch teams from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON array of entries
	 * @param species every catchable species
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static RematchTable load(Gson gson, Reader reader, SpeciesTable species)
	{
		try
		{
			List<Entry> loaded = gson.fromJson(reader, LIST_TYPE);
			return new RematchTable(loaded == null ? Collections.emptyList() : loaded, species);
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/**
	 * Finds the rematch team of a champion.
	 *
	 * @param championId the champion's id
	 * @return the six creatures, or an empty list when the champion has no rematch team
	 */
	List<Champion.Member> teamOf(String championId)
	{
		List<Champion.Member> found = teams.get(championId);
		return found == null ? Collections.emptyList() : found;
	}

	/**
	 * Makes the champion as fought in a rematch.
	 *
	 * @param champion the champion
	 * @return the champion with the rematch team, or the champion unchanged when there is none
	 */
	Champion rematchOf(Champion champion)
	{
		List<Champion.Member> team = teamOf(champion.id());
		return team.isEmpty() ? champion : champion.withTeam(team);
	}

	private static boolean usable(List<Champion.Member> team, SpeciesTable species)
	{
		if (team == null || team.size() != TEAM_SIZE)
			return false;

		for (Champion.Member member : team)
		{
			if (member == null || species.find(member.species()) == null)
				return false;

			if (member.level() < MIN_LEVEL || member.level() > MAX_LEVEL)
				return false;
		}

		return true;
	}
}
