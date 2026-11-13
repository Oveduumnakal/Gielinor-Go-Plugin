/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * The rival's name, lines and teams, loaded from {@code rival.json}. There is a team for each of the three starters
 * and each of the five battles; a file that is missing a team, or names a creature that cannot be caught, loads as
 * no rival at all.
 */
final class RivalTable
{
	/** Resource path of the bundled rival file, relative to this package. */
	static final String RESOURCE = "rival.json";

	/** The shape of the data file. */
	private static final class Data
	{
		private String name;
		private List<String> before;
		private List<String> after;
		private Map<String, List<List<Champion.Member>>> teams;
	}

	private final String name;
	private final List<String> before;
	private final List<String> after;
	private final Map<String, List<List<Champion.Member>>> teams;

	private RivalTable(Data data)
	{
		name = data == null ? null : data.name;
		before = data == null || data.before == null ? Collections.emptyList() : data.before;
		after = data == null || data.after == null ? Collections.emptyList() : data.after;
		teams = data == null || data.teams == null ? Collections.emptyMap() : data.teams;
	}

	/** @return a table with no rival */
	static RivalTable empty()
	{
		return new RivalTable(null);
	}

	/**
	 * Reads the rival from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON object
	 * @param species every species, to check the teams against
	 * @return the table, empty when the JSON is missing, malformed or names a creature that cannot be caught
	 */
	static RivalTable load(Gson gson, Reader reader, SpeciesTable species)
	{
		try
		{
			RivalTable table = new RivalTable(gson.fromJson(reader, Data.class));
			return table.problem(species) == null ? table : empty();
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/**
	 * Finds what is wrong with the table, for a test to report.
	 *
	 * @param species every species
	 * @return the first problem found, or null when the table is sound
	 */
	String problem(SpeciesTable species)
	{
		if (name == null || name.trim().isEmpty())
			return "no name";

		if (before.size() < RivalRules.BATTLES || after.size() < RivalRules.BATTLES)
			return "fewer than " + RivalRules.BATTLES + " lines before or after";

		for (String starter : StarterRules.STARTERS)
		{
			String key = RivalRules.counterOf(starter);
			List<List<Champion.Member>> rows = teams.get(key);
			if (rows == null || rows.size() != RivalRules.BATTLES)
				return "no five teams for " + key;

			for (int i = 0; i < rows.size(); i++)
			{
				String bad = badTeam(rows.get(i), i + 1, species);
				if (bad != null)
					return key + " battle " + (i + 1) + ": " + bad;
			}
		}

		return null;
	}

	private static String badTeam(List<Champion.Member> team, int battle, SpeciesTable species)
	{
		if (team == null || team.size() != battle + 1)
			return "needs " + (battle + 1) + " creatures";

		for (Champion.Member member : team)
		{
			Species found = member == null ? null : species.find(member.species());
			if (found == null || found.questOnly())
				return "a creature cannot be caught";
		}

		return null;
	}

	/** @return whether there is a rival to meet */
	boolean available()
	{
		return name != null && !teams.isEmpty();
	}

	/** @return the rival's name */
	String name()
	{
		return name;
	}

	/**
	 * Gives what the rival says before a battle.
	 *
	 * @param battle the battle, counted from 1
	 * @return the line
	 */
	String before(int battle)
	{
		return before.get(battle - 1);
	}

	/**
	 * Gives what the rival says after losing a battle.
	 *
	 * @param battle the battle, counted from 1
	 * @return the line
	 */
	String after(int battle)
	{
		return after.get(battle - 1);
	}

	/**
	 * Gives the team the rival brings to a battle.
	 *
	 * @param starter the species key of the rival's starter
	 * @param battle the battle, counted from 1
	 * @return the team in the order it fights, or an empty list when the table has none for these
	 */
	List<Champion.Member> team(String starter, int battle)
	{
		List<List<Champion.Member>> rows = teams.get(Species.keyFor(starter));
		if (rows == null || battle < 1 || battle > rows.size())
			return Collections.emptyList();

		return new ArrayList<>(rows.get(battle - 1));
	}
}
