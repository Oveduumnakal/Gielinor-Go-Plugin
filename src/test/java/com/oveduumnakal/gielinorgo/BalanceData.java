/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

/**
 * What the balance report measures: the species, the evolutions and the moves, as the game loads them, and a few
 * questions every section asks of them (the species of a tier, the middle one, the levels it spans). Test code only.
 */
final class BalanceData
{
	private final SpeciesTable species;
	private final EvolutionTable evolutions;
	private final MoveTable moves;
	private final SkillingTable skilling;
	private final int fights;
	private final Map<Rarity, List<Species>> byTier = new EnumMap<>(Rarity.class);

	/**
	 * Gathers the data.
	 *
	 * @param species every species
	 * @param evolutions the evolution lines
	 * @param moves the move table
	 * @param skilling the skilling table; an empty one leaves the skilling tables out
	 * @param fights how many seeded fights each battle cell runs
	 */
	BalanceData(SpeciesTable species, EvolutionTable evolutions, MoveTable moves, SkillingTable skilling, int fights)
	{
		this.species = species;
		this.evolutions = evolutions;
		this.moves = moves;
		this.skilling = skilling;
		this.fights = Math.max(1, fights);
		for (Rarity rarity : Rarity.values())
			byTier.put(rarity, new ArrayList<>());

		for (Species each : species.catchable())
			byTier.get(each.rarity()).add(each);
	}

	/**
	 * Loads the bundled data the game ships with.
	 *
	 * @param fights how many seeded fights each battle cell runs
	 * @return the data
	 * @throws IOException when a resource cannot be read
	 */
	static BalanceData load(int fights) throws IOException
	{
		Gson gson = new Gson();
		SpeciesTable table;
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			table = SpeciesTable.load(gson, reader);
		}

		EvolutionTable lines;
		try (Reader reader = open(EvolutionTable.RESOURCE))
		{
			lines = EvolutionTable.load(gson, reader, table);
		}

		MoveTable known;
		try (Reader reader = open(MoveTable.RESOURCE))
		{
			known = MoveTable.load(gson, reader);
		}

		AnimationTable animations;
		try (Reader reader = open(AnimationTable.RESOURCE))
		{
			animations = AnimationTable.load(gson, reader);
		}

		SkillingTable gathering;
		try (Reader reader = open(SkillingTable.RESOURCE))
		{
			gathering = SkillingTable.load(gson, reader, table, animations);
		}

		return new BalanceData(table, lines, known, gathering, fights);
	}

	private static Reader open(String resource) throws IOException
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(resource);
		if (in == null)
			throw new IOException(resource + " is missing from the resources");

		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	/** @return the species table */
	SpeciesTable species()
	{
		return species;
	}

	/** @return the evolution lines */
	EvolutionTable evolutions()
	{
		return evolutions;
	}

	/** @return the move table */
	MoveTable moves()
	{
		return moves;
	}

	/** @return the skilling table, empty when the report leaves skilling out */
	SkillingTable skilling()
	{
		return skilling;
	}

	/** @return how many seeded fights each battle cell runs */
	int fights()
	{
		return fights;
	}

	/**
	 * Lists the catchable species of a tier, weakest first.
	 *
	 * @param rarity the tier
	 * @return the species, possibly none
	 */
	List<Species> tier(Rarity rarity)
	{
		return byTier.get(rarity);
	}

	/**
	 * Finds the middle species of a tier by combat level.
	 *
	 * @param rarity the tier
	 * @return the middle species, or a stand-in at the tier's lowest level when the tier has none
	 */
	Species median(Rarity rarity)
	{
		List<Species> list = byTier.get(rarity);
		if (list.isEmpty())
			return new Species("Stand-in " + rarity.label(), rarity.minLevel(), CombatType.CRUSH);

		return list.get(list.size() / 2);
	}

	/**
	 * Finds the middle catchable species of a style by combat level.
	 *
	 * @param style the style
	 * @return the middle species, or a stand-in of combat level 60 when no species has the style
	 */
	Species median(CombatType style)
	{
		List<Species> list = new ArrayList<>();
		for (Species each : species.catchable())
		{
			if (each.style() == style)
				list.add(each);
		}

		if (list.isEmpty())
			return new Species("Stand-in " + style.label(), 60, style);

		return list.get(list.size() / 2);
	}

	/**
	 * Finds the middle species of one style within one tier.
	 *
	 * @param rarity the tier
	 * @param style the style
	 * @return the middle species, or a stand-in at the tier's lowest level when there is none
	 */
	Species median(Rarity rarity, CombatType style)
	{
		List<Species> list = new ArrayList<>();
		for (Species each : byTier.get(rarity))
		{
			if (each.style() == style)
				list.add(each);
		}

		if (list.isEmpty())
			return new Species("Stand-in " + rarity.label() + " " + style.label(), rarity.minLevel(), style);

		return list.get(list.size() / 2);
	}

	/**
	 * Finds the catchable species of a style whose combat level is closest to a level.
	 *
	 * @param style the style
	 * @param combat the combat level to be near
	 * @return the species, or a stand-in of that level when no species has the style
	 */
	Species nearest(CombatType style, int combat)
	{
		Species best = null;
		for (Species each : species.catchable())
		{
			if (each.style() != style)
				continue;

			if (best == null || Math.abs(each.level() - combat) < Math.abs(best.level() - combat))
				best = each;
		}

		return best == null ? new Species("Stand-in " + style.label(), Math.max(1, combat), style) : best;
	}

	/**
	 * Averages the combat level of a tier.
	 *
	 * @param rarity the tier
	 * @return the mean combat level, or the tier's lowest level when it has no species
	 */
	double meanLevel(Rarity rarity)
	{
		List<Species> list = byTier.get(rarity);
		if (list.isEmpty())
			return rarity.minLevel();

		double sum = 0;
		for (Species each : list)
			sum += each.level();

		return sum / list.size();
	}

	/**
	 * Finds the lowest combat level of a tier.
	 *
	 * @param rarity the tier
	 * @return the level, or the tier's lowest level when it has no species
	 */
	int minLevel(Rarity rarity)
	{
		List<Species> list = byTier.get(rarity);
		return list.isEmpty() ? rarity.minLevel() : list.get(0).level();
	}

	/**
	 * Finds the highest combat level of a tier.
	 *
	 * @param rarity the tier
	 * @return the level, or the tier's lowest level when it has no species
	 */
	int maxLevel(Rarity rarity)
	{
		List<Species> list = byTier.get(rarity);
		return list.isEmpty() ? rarity.minLevel() : list.get(list.size() - 1).level();
	}

	/**
	 * Counts the species of one style in one tier.
	 *
	 * @param rarity the tier
	 * @param style the style
	 * @return the count
	 */
	int count(Rarity rarity, CombatType style)
	{
		int count = 0;
		for (Species each : byTier.get(rarity))
		{
			if (each.style() == style)
				count++;
		}

		return count;
	}
}
