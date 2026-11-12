/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * The Elite challenge as data: the rectangle around the Champions' Guild, the five teams in the order they are
 * fought (four Elite trainers, then the Grand Champion) and the reward sizes. Loaded from {@code elite.json}. A team
 * is a {@link Champion} whose city field holds the opponent's name.
 */
final class EliteTable
{
	/** Resource path of the bundled Elite data, relative to this package. */
	static final String RESOURCE = "elite.json";

	/** How many battles an Elite run is. */
	static final int STAGES = 5;

	/** The shape of {@code elite.json}. */
	private static final class Raw
	{
		private int minX;
		private int minY;
		private int maxX;
		private int maxY;
		private int firstDragonJars;
		private int firstSpecialtyJars;
		private String firstEgg;
		private int laterDragonJars;
		private List<Champion> stages;
	}

	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;
	private final int firstDragonJars;
	private final int firstSpecialtyJars;
	private final Rarity firstEgg;
	private final int laterDragonJars;
	private final List<Champion> stages;

	private EliteTable(Raw raw, List<Champion> kept)
	{
		minX = raw.minX;
		minY = raw.minY;
		maxX = raw.maxX;
		maxY = raw.maxY;
		firstDragonJars = Math.max(0, raw.firstDragonJars);
		firstSpecialtyJars = Math.max(0, raw.firstSpecialtyJars);
		laterDragonJars = Math.max(0, raw.laterDragonJars);
		Rarity egg = Rarity.LEGENDARY;
		for (Rarity rarity : Rarity.values())
		{
			if (rarity.name().equals(raw.firstEgg))
				egg = rarity;
		}

		firstEgg = egg;
		stages = Collections.unmodifiableList(kept);
	}

	/** @return a table with no challenge, so the Elite card never shows */
	static EliteTable empty()
	{
		return new EliteTable(new Raw(), new ArrayList<>());
	}

	/**
	 * Reads the Elite data from JSON. The challenge is switched off, as an empty table, unless the data names exactly
	 * {@link #STAGES} teams made only of species the game knows.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON object
	 * @param species every catchable species
	 * @return the table, empty when the JSON is missing, malformed or names an unknown species
	 */
	static EliteTable load(Gson gson, Reader reader, SpeciesTable species)
	{
		try
		{
			Raw raw = gson.fromJson(reader, Raw.class);
			if (raw == null || raw.stages == null || raw.stages.size() != STAGES)
				return empty();

			for (Champion stage : raw.stages)
			{
				if (!usable(stage, species))
					return empty();
			}

			return new EliteTable(raw, new ArrayList<>(raw.stages));
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	private static boolean usable(Champion stage, SpeciesTable species)
	{
		if (stage == null || stage.id() == null || stage.city() == null || stage.team().isEmpty())
			return false;

		for (Champion.Member member : stage.team())
		{
			if (member == null || species.find(member.species()) == null)
				return false;
		}

		return true;
	}

	/** @return whether the challenge exists, that is whether the data loaded with all its teams */
	boolean available()
	{
		return stages.size() == STAGES;
	}

	/** @return the five teams in the order they are fought, the Grand Champion last */
	List<Champion> stages()
	{
		return stages;
	}

	/**
	 * Checks whether a tile is inside the Champions' Guild rectangle, which includes the ground outside its door.
	 *
	 * @param x world x
	 * @param y world y
	 * @param plane world plane; the guild's door is on the surface, plane 0
	 * @return true when the tile is inside
	 */
	boolean contains(int x, int y, int plane)
	{
		return available() && plane == 0 && x >= minX && x <= maxX && y >= minY && y <= maxY;
	}

	/**
	 * Checks whether the rectangle shares any tile with a city's.
	 *
	 * @param city a City Champion
	 * @return true when they overlap
	 */
	boolean overlaps(Champion city)
	{
		return new Champion("", "", minX, minY, maxX, maxY, Collections.emptyList(), JarType.IRON, 1)
			.overlaps(city);
	}

	/** @return Dragon jars paid for the first clear */
	int firstDragonJars()
	{
		return firstDragonJars;
	}

	/** @return jars of every specialty type paid for the first clear */
	int firstSpecialtyJars()
	{
		return firstSpecialtyJars;
	}

	/** @return the rarity of the egg paid for the first clear */
	Rarity firstEgg()
	{
		return firstEgg;
	}

	/** @return Dragon jars paid for every later clear */
	int laterDragonJars()
	{
		return laterDragonJars;
	}
}
