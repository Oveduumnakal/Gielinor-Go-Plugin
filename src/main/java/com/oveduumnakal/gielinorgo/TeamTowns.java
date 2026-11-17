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

/**
 * The towns and cities where the team may be changed, as rectangles of world tiles. Loaded from
 * {@code team-towns.json}. It is a separate list from the City Champions' areas and only {@link TeamSwapRule} reads
 * it: standing in one of these towns never lets a champion be challenged. A town counts on every plane, so the upper
 * floors of its buildings count too. The rectangles are generous, not surveyed; a bank next to the player allows the
 * swap anyway.
 */
final class TeamTowns
{
	/** Resource path of the bundled town list, relative to this package. */
	static final String RESOURCE = "team-towns.json";

	private static final Type LIST_TYPE = new TypeToken<List<Town>>()
	{
	}.getType();

	/** One town as written in the data file. */
	static final class Town
	{
		private String name;
		private int minX;
		private int minY;
		private int maxX;
		private int maxY;

		/** Used by Gson. */
		Town()
		{
		}

		Town(String name, int minX, int minY, int maxX, int maxY)
		{
			this.name = name;
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
		}

		/** @return the town's name */
		String name()
		{
			return name;
		}

		/**
		 * Tells whether a tile is inside this town.
		 *
		 * @param x world x
		 * @param y world y
		 * @return true when the tile is inside the rectangle, edges included
		 */
		boolean contains(int x, int y)
		{
			return x >= minX && x <= maxX && y >= minY && y <= maxY;
		}

		private boolean usable()
		{
			return name != null && !name.trim().isEmpty() && minX <= maxX && minY <= maxY;
		}
	}

	private final List<Town> towns;

	/**
	 * Builds the list, leaving out any town with no name or an empty rectangle.
	 *
	 * @param loaded the towns from the data file
	 */
	TeamTowns(List<Town> loaded)
	{
		List<Town> kept = new ArrayList<>();
		for (Town town : loaded)
		{
			if (town != null && town.usable())
				kept.add(town);
		}

		towns = Collections.unmodifiableList(kept);
	}

	/** @return a list with no towns, so only a champion's city or a bank allows a change */
	static TeamTowns empty()
	{
		return new TeamTowns(Collections.emptyList());
	}

	/**
	 * Reads the town list from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON array of towns
	 * @return the list, empty when the JSON is missing or malformed
	 */
	static TeamTowns load(Gson gson, Reader reader)
	{
		try
		{
			List<Town> loaded = gson.fromJson(reader, LIST_TYPE);
			return new TeamTowns(loaded == null ? Collections.emptyList() : loaded);
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/** @return every town */
	List<Town> all()
	{
		return towns;
	}

	/**
	 * Finds the town a tile is in.
	 *
	 * @param x world x
	 * @param y world y
	 * @return the town, or null when the tile is in none
	 */
	Town at(int x, int y)
	{
		for (Town town : towns)
		{
			if (town.contains(x, y))
				return town;
		}

		return null;
	}

	/**
	 * Tells whether a tile is inside any town.
	 *
	 * @param x world x
	 * @param y world y
	 * @return true when the tile is in a town
	 */
	boolean contains(int x, int y)
	{
		return at(x, y) != null;
	}
}
