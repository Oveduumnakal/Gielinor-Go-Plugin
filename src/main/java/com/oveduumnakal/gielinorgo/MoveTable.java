/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Every move and the pool of moves each species learns, as written by {@code scripts/gen-moves.py} from the table in
 * {@code scripts/move-pools.json}. A pool is a list of moves with the level each is learned at. The table holds no
 * game client types, so every rule about learning can be tested.
 */
final class MoveTable
{
	/** Resource path of the bundled move list, relative to this package. */
	static final String RESOURCE = "moves.json";

	/** Most moves a creature knows at once. */
	static final int MAX_KNOWN = 4;

	private static final MoveTable EMPTY = new MoveTable(new Raw());

	/** The shape of the file. */
	private static final class Raw
	{
		private List<Move> moves;
		private Map<String, String> defaults;
		private Map<String, Pool> pools;
		private Map<String, String> species;
	}

	/** One pool as written in the file. */
	private static final class Pool
	{
		private List<String> styles;
		private List<List<Object>> learns;
	}

	/** A move of a pool and the level it is learned at. */
	private static final class Learn
	{
		private final Move move;
		private final int level;

		private Learn(Move move, int level)
		{
			this.move = move;
			this.level = level;
		}
	}

	private final Map<String, Move> byId = new HashMap<>();
	private final Map<String, List<Learn>> pools = new HashMap<>();
	private final Map<String, String> defaults = new HashMap<>();
	private final Map<String, String> poolOfSpecies = new HashMap<>();

	private MoveTable(Raw file)
	{
		if (file.moves != null)
		{
			for (Move move : file.moves)
			{
				if (move != null && move.id() != null)
					byId.put(move.id(), move);
			}
		}

		if (file.pools != null)
		{
			for (Map.Entry<String, Pool> entry : file.pools.entrySet())
				pools.put(entry.getKey(), learnsOf(entry.getValue()));
		}

		if (file.defaults != null)
			defaults.putAll(file.defaults);

		if (file.species != null)
			poolOfSpecies.putAll(file.species);
	}

	private List<Learn> learnsOf(Pool pool)
	{
		List<Learn> learns = new ArrayList<>();
		if (pool == null || pool.learns == null)
			return learns;

		for (List<Object> pair : pool.learns)
		{
			if (pair == null || pair.size() != 2 || !(pair.get(0) instanceof String)
				|| !(pair.get(1) instanceof Number))
				continue;

			Move move = byId.get(pair.get(0));
			if (move != null)
				learns.add(new Learn(move, ((Number) pair.get(1)).intValue()));
		}

		learns.sort((a, b) -> Integer.compare(a.level, b.level));
		return learns;
	}

	/** @return a table that knows no moves, so every creature fights with its basic attack */
	static MoveTable empty()
	{
		return EMPTY;
	}

	/**
	 * Reads a move table from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON object written by {@code gen-moves.py}
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static MoveTable load(Gson gson, Reader reader)
	{
		try
		{
			Raw read = gson.fromJson(reader, Raw.class);
			return read == null ? EMPTY : new MoveTable(read);
		}
		catch (JsonParseException e)
		{
			return EMPTY;
		}
	}

	/** @return how many moves the table knows */
	int size()
	{
		return byId.size();
	}

	/**
	 * Finds a move.
	 *
	 * @param id a move id
	 * @return the move, or null when the table does not know it
	 */
	Move find(String id)
	{
		return id == null ? null : byId.get(id);
	}

	/**
	 * Finds the name of the pool a species learns from.
	 *
	 * @param species the species
	 * @return the pool name, or null when the table has no pool for it
	 */
	String poolOf(Species species)
	{
		String own = poolOfSpecies.get(species.key());
		if (own != null && pools.containsKey(own))
			return own;

		String fallback = defaults.get(species.style().name());
		return fallback != null && pools.containsKey(fallback) ? fallback : null;
	}

	/**
	 * Lists every move a species can learn, with the level of each.
	 *
	 * @param species the species
	 * @return the moves in the order they are learned; empty when the table has no pool for the species
	 */
	List<Move> pool(Species species)
	{
		List<Move> moves = new ArrayList<>();
		for (Learn learn : learnsOf(species))
			moves.add(learn.move);

		return moves;
	}

	/**
	 * Finds the level a species learns a move at.
	 *
	 * @param species the species
	 * @param moveId the move id
	 * @return the level, or 0 when the species' pool does not hold the move
	 */
	int levelOf(Species species, String moveId)
	{
		for (Learn learn : learnsOf(species))
		{
			if (learn.move.id().equals(moveId))
				return learn.level;
		}

		return 0;
	}

	/**
	 * Finds the moves a wild creature of a level knows, and what an old creature is given: the newest four moves its
	 * level has unlocked.
	 *
	 * @param species the species
	 * @param level the creature level
	 * @return at most {@link #MAX_KNOWN} moves, oldest first; empty when the table has no pool for the species
	 */
	List<Move> movesAt(Species species, int level)
	{
		List<Move> unlocked = new ArrayList<>();
		for (Learn learn : learnsOf(species))
		{
			if (learn.level <= level)
				unlocked.add(learn.move);
		}

		int from = Math.max(0, unlocked.size() - MAX_KNOWN);
		return new ArrayList<>(unlocked.subList(from, unlocked.size()));
	}

	/**
	 * Finds the moves a species unlocks when it goes from one level to a higher one.
	 *
	 * @param species the species
	 * @param fromLevel the level it had
	 * @param toLevel the level it has now
	 * @return the moves whose level is above {@code fromLevel} and at most {@code toLevel}, in the order learned
	 */
	List<Move> unlockedBetween(Species species, int fromLevel, int toLevel)
	{
		List<Move> unlocked = new ArrayList<>();
		for (Learn learn : learnsOf(species))
		{
			if (learn.level > fromLevel && learn.level <= toLevel)
				unlocked.add(learn.move);
		}

		return unlocked;
	}

	/**
	 * Tells whether a creature of a species and level may know a move: it must be in the species' pool at a level
	 * the creature has reached. This is how moves that friends send are checked.
	 *
	 * @param species the species
	 * @param level the creature level
	 * @param moveId the move id
	 * @return true when the move is allowed
	 */
	boolean knows(Species species, int level, String moveId)
	{
		int at = levelOf(species, moveId);
		return at > 0 && at <= level;
	}

	private List<Learn> learnsOf(Species species)
	{
		String name = poolOf(species);
		if (name == null)
			return Collections.emptyList();

		return pools.get(name);
	}

	/** @return how many pools the table has */
	int poolCount()
	{
		return pools.size();
	}

	/**
	 * Lists the pool names, for tests of the bundled data.
	 *
	 * @return the pool names
	 */
	List<String> poolNames()
	{
		List<String> names = new ArrayList<>(pools.keySet());
		Collections.sort(names);
		return names;
	}

	/**
	 * Lists a named pool, for tests of the bundled data.
	 *
	 * @param name the pool name
	 * @return the moves in learning order with their levels as {@code id@level}, empty for an unknown pool
	 */
	List<String> describePool(String name)
	{
		List<String> out = new ArrayList<>();
		for (Learn learn : pools.getOrDefault(name, Collections.emptyList()))
			out.add(learn.move.id() + "@" + learn.level);

		return out;
	}

	/** @return the species keys that have their own pool, for tests of the bundled data */
	List<String> assignedSpecies()
	{
		List<String> keys = new ArrayList<>(poolOfSpecies.keySet());
		Collections.sort(keys);
		return keys;
	}
}
