/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Which species a gathering skill brings, loaded from the bundled {@code skilling.json}
 * ({@code scripts/skilling-species.json} is the source). Each skill has a list of species with the skill level they
 * start at, and a few species are exclusive: met only while skilling, one time in so many.
 */
final class SkillingTable
{
	/** Resource path of the bundled list, relative to this package. */
	static final String RESOURCE = "skilling.json";

	/** Added to a species' level when weighing the picks, so stronger species are likelier as the skill grows. */
	static final int WEIGHT_BASE = 10;

	private final Map<String, List<Row>> bySkill = new HashMap<>();
	private final Map<String, String> messages = new HashMap<>();
	private final Map<String, List<Row>> exclusiveBySkill = new HashMap<>();
	private final Set<String> exclusiveKeys = new HashSet<>();

	private SkillingTable()
	{
	}

	/** @return a table that brings nothing */
	static SkillingTable empty()
	{
		return new SkillingTable();
	}

	/**
	 * Reads the table from JSON. A name that is not a catchable species, or whose first NPC id has no animations (it
	 * could not be drawn standing and walking), is left out.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON
	 * @param species the species list the names are looked up in
	 * @param animations the animations a body is drawn with
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static SkillingTable load(Gson gson, Reader reader, SpeciesTable species, AnimationTable animations)
	{
		try
		{
			JsonObject root = gson.fromJson(reader, JsonObject.class);
			SkillingTable table = new SkillingTable();
			if (root != null)
				table.read(root, species, animations);

			return table;
		}
		catch (JsonParseException | ClassCastException | IllegalStateException | NullPointerException e)
		{
			return empty();
		}
	}

	private void read(JsonObject root, SpeciesTable species, AnimationTable animations)
	{
		JsonObject skills = root.getAsJsonObject("skills");
		for (Map.Entry<String, JsonElement> skill : skills.entrySet())
		{
			JsonObject block = skill.getValue().getAsJsonObject();
			messages.put(skill.getKey(), block.get("message").getAsString());
			bySkill.put(skill.getKey(), rows(block.getAsJsonArray("species"), species, animations, 0));
		}

		JsonArray exclusive = root.getAsJsonArray("exclusive");
		if (exclusive == null)
			return;

		for (JsonElement element : exclusive)
		{
			JsonObject object = element.getAsJsonObject();
			List<Row> one = rows(singleton(element), species, animations, object.get("oneIn").getAsInt());
			for (Row row : one)
			{
				exclusiveBySkill.computeIfAbsent(object.get("skill").getAsString(), k -> new ArrayList<>()).add(row);
				exclusiveKeys.add(row.species.key());
			}
		}
	}

	private static JsonArray singleton(JsonElement element)
	{
		JsonArray array = new JsonArray();
		array.add(element);
		return array;
	}

	private static List<Row> rows(JsonArray array, SpeciesTable species, AnimationTable animations, int oneIn)
	{
		List<Row> rows = new ArrayList<>();
		for (JsonElement element : array)
		{
			JsonObject object = element.getAsJsonObject();
			Species found = species.find(object.get("species").getAsString());
			if (found == null || found.questOnly() || found.ids().length == 0 || !animations.has(found.ids()[0]))
				continue;

			rows.add(new Row(found, object.get("minLevel").getAsInt(), oneIn));
		}

		return rows;
	}

	/**
	 * Picks a species for an XP drop. An exclusive species of the skill comes first at its odds; otherwise one of the
	 * species the skill level has reached, stronger ones likelier.
	 *
	 * @param skill the skill's name in capitals, such as {@code FISHING}
	 * @param skillLevel the player's level in that skill
	 * @param random the source of chance
	 * @return the species, or null when the skill brings none at that level
	 */
	Species pick(String skill, int skillLevel, Random random)
	{
		for (Row row : exclusiveBySkill.getOrDefault(skill, Collections.emptyList()))
		{
			if (skillLevel >= row.minLevel && random.nextInt(Math.max(1, row.oneIn)) == 0)
				return row.species;
		}

		List<Row> open = new ArrayList<>();
		int total = 0;
		for (Row row : bySkill.getOrDefault(skill, Collections.emptyList()))
		{
			if (skillLevel >= row.minLevel)
			{
				open.add(row);
				total += row.weight();
			}
		}

		if (open.isEmpty())
			return null;

		int roll = random.nextInt(total);
		for (Row row : open)
		{
			roll -= row.weight();
			if (roll < 0)
				return row.species;
		}

		return open.get(open.size() - 1).species;
	}

	/**
	 * Finds what to say when a creature comes of a skill.
	 *
	 * @param skill the skill's name in capitals
	 * @return the words after the species name, such as {@code surfaced}; {@code appeared} when unknown
	 */
	String message(String skill)
	{
		return messages.getOrDefault(skill, "appeared");
	}

	/**
	 * Tells whether a species is met only while skilling. Such a species never turns wild from a real monster.
	 *
	 * @param species the species, may be null
	 * @return true when it is one of the exclusive species
	 */
	boolean exclusive(Species species)
	{
		return species != null && exclusiveKeys.contains(species.key());
	}

	/**
	 * Names the skill an exclusive species comes from.
	 *
	 * @param species the species
	 * @return the skill's name in capitals, or null when the species is not exclusive
	 */
	String skillOf(Species species)
	{
		for (Map.Entry<String, List<Row>> skill : exclusiveBySkill.entrySet())
		{
			for (Row row : skill.getValue())
			{
				if (row.species == species || row.species.key().equals(species.key()))
					return skill.getKey();
			}
		}

		return null;
	}

	/** @return how many exclusive species the table holds */
	int exclusiveCount()
	{
		return exclusiveKeys.size();
	}

	/**
	 * Lists the species a skill can bring, for tests and tooling.
	 *
	 * @param skill the skill's name in capitals
	 * @return the species of the skill, exclusive ones included
	 */
	List<Species> speciesOf(String skill)
	{
		List<Species> all = new ArrayList<>();
		for (Row row : bySkill.getOrDefault(skill, Collections.emptyList()))
			all.add(row.species);

		for (Row row : exclusiveBySkill.getOrDefault(skill, Collections.emptyList()))
			all.add(row.species);

		return all;
	}

	/** One species a skill brings, with the skill level it starts at and, for an exclusive one, its odds. */
	private static final class Row
	{
		private final Species species;
		private final int minLevel;
		private final int oneIn;

		Row(Species species, int minLevel, int oneIn)
		{
			this.species = species;
			this.minLevel = minLevel;
			this.oneIn = oneIn;
		}

		int weight()
		{
			return WEIGHT_BASE + minLevel;
		}
	}
}
