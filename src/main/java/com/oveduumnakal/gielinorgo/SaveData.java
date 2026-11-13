/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the game remembers for one account. Saved to the profile as JSON, so every field here is part of the
 * persisted schema.
 */
final class SaveData
{
	/** Current schema version. */
	static final int VERSION = 1;

	/** Most creatures a team can hold. */
	static final int TEAM_SIZE = 6;

	/** Most moves that can wait for a choice on one creature. */
	static final int MAX_PENDING_MOVES = 8;

	/** Most creatures storage can hold. */
	static final int STORAGE_SIZE = 300;

	/** Most eggs the player can carry at once. */
	static final int MAX_EGGS = 3;

	/** Bronze jars a new player starts with. */
	static final int STARTING_JARS = 10;

	int version = VERSION;

	/** The active team. Index 0 is the lead, which fights battles and earns a double XP share. */
	List<Creature> team = new ArrayList<>();

	/** Owned creatures that are not on the team. */
	List<Creature> storage = new ArrayList<>();

	/** Jars owned, keyed by {@link JarType#name()}. */
	Map<String, Integer> jars = new HashMap<>();

	/** The jar the player chose to throw, as a {@link JarType#name()}. */
	String selectedJar = JarType.BRONZE.name();

	/** Trainer XP, from catching and winning battles. */
	long trainerXp;

	/** Times each species has been caught, keyed by species key. */
	Map<String, Integer> caught = new HashMap<>();

	/** Species keys the player has met as a wild creature. */
	Set<String> seen = new LinkedHashSet<>();

	/** Tiles walked since the last jar found by walking. */
	int stepProgress;

	/** Battles won. */
	int battlesWon;

	/** Eggs being carried, each hatching after enough walking. */
	List<Egg> eggs = new ArrayList<>();

	/** Species keys the player has owned a gilded creature of. */
	Set<String> gilded = new LinkedHashSet<>();

	/** Ids of the City Champions the player has beaten. */
	Set<String> sigils = new LinkedHashSet<>();

	/** Ids of the cities whose champion has been beaten again in a rematch, the gold sigils. Null in older saves. */
	Set<String> goldSigils = new LinkedHashSet<>();

	/** The UTC day each champion last paid its daily rematch reward, by champion id. Null in older saves. */
	Map<String, Long> rematchDay = new HashMap<>();

	/** Progress on the daily and weekly tasks and the login streak. Null in saves from before tasks existed. */
	TaskProgress tasks = new TaskProgress();

	/** Ids of the Creature Log milestones whose reward has been paid. Null in saves from before milestones. */
	Set<String> milestones = new LinkedHashSet<>();

	/** How many times the player has cleared the Elite challenge. 0 in saves from before it existed. */
	int eliteClears;

	/** Healing items owned, keyed by {@link ItemType#name()}. Null in saves from before items existed. */
	Map<String, Integer> items = new HashMap<>();

	/** Species keys of the bosses that have already paid their sure first-kill egg. Null in older saves. */
	Set<String> bossKills = new LinkedHashSet<>();

	/** @return a save for a player who has never played, with the starting jars */
	static SaveData fresh()
	{
		SaveData data = new SaveData();
		data.jars.put(JarType.BRONZE.name(), STARTING_JARS);
		return data;
	}

	/**
	 * Fills in anything a damaged or older save left out, so the rest of the code never meets a null.
	 *
	 * @return this save
	 */
	SaveData sanitized()
	{
		team = team == null ? new ArrayList<>() : new ArrayList<>(team);
		storage = storage == null ? new ArrayList<>() : new ArrayList<>(storage);
		jars = jars == null ? new HashMap<>() : new HashMap<>(jars);
		items = items == null ? new HashMap<>() : new HashMap<>(items);
		caught = caught == null ? new HashMap<>() : new HashMap<>(caught);
		seen = seen == null ? new LinkedHashSet<>() : new LinkedHashSet<>(seen);
		eggs = eggs == null ? new ArrayList<>() : new ArrayList<>(eggs);
		gilded = gilded == null ? new LinkedHashSet<>() : new LinkedHashSet<>(gilded);
		sigils = sigils == null ? new LinkedHashSet<>() : new LinkedHashSet<>(sigils);
		goldSigils = goldSigils == null ? new LinkedHashSet<>() : new LinkedHashSet<>(goldSigils);
		rematchDay = rematchDay == null ? new HashMap<>() : new HashMap<>(rematchDay);
		rematchDay.values().removeIf(day -> day == null);
		tasks = tasks == null ? new TaskProgress() : tasks.sanitized();
		milestones = milestones == null ? new LinkedHashSet<>() : new LinkedHashSet<>(milestones);
		bossKills = bossKills == null ? new LinkedHashSet<>() : new LinkedHashSet<>(bossKills);
		eggs.removeIf(egg -> egg == null);
		team.removeIf(creature -> creature == null || creature.species == null);
		storage.removeIf(creature -> creature == null || creature.species == null);
		for (Creature creature : team)
			clean(creature);

		for (Creature creature : storage)
			clean(creature);

		while (team.size() > TEAM_SIZE)
			storage.add(team.remove(team.size() - 1));

		if (selectedJar == null)
			selectedJar = JarType.BRONZE.name();

		version = VERSION;
		return this;
	}

	/**
	 * Cleans the parts of a creature a damaged or older save may have left out of range.
	 *
	 * @param creature the creature
	 */
	private static void clean(Creature creature)
	{
		creature.nickname = Nickname.clean(creature.nickname);
		creature.npcId = Math.max(0, creature.npcId);
		creature.rollHp = StatRolls.clamp(creature.rollHp);
		creature.rollAttack = StatRolls.clamp(creature.rollAttack);
		creature.rollDefence = StatRolls.clamp(creature.rollDefence);
		creature.rollSpeed = StatRolls.clamp(creature.rollSpeed);
		if (Temperament.named(creature.temperament) == null)
			creature.temperament = null;

		creature.moves = trimmed(creature.moves, MoveTable.MAX_KNOWN, null);
		creature.pendingMoves = trimmed(creature.pendingMoves, MAX_PENDING_MOVES, creature.moves);
	}

	/**
	 * Cuts a list of move ids to a length, dropping nulls, blanks and repeats.
	 *
	 * @param ids the ids as saved, or null
	 * @param most the most ids to keep
	 * @param apart ids the result must not repeat, or null
	 * @return a list the caller owns; null when the save had none, so a creature with no moves stays recognisable
	 */
	private static List<String> trimmed(List<String> ids, int most, List<String> apart)
	{
		if (ids == null)
			return null;

		List<String> kept = new ArrayList<>();
		for (String id : ids)
		{
			if (id != null && !id.trim().isEmpty() && !kept.contains(id) && (apart == null || !apart.contains(id))
				&& kept.size() < most)
				kept.add(id);
		}

		return kept;
	}

	/**
	 * Counts the jars owned of one type.
	 *
	 * @param type the jar type
	 * @return the number owned, never negative
	 */
	int jarCount(JarType type)
	{
		Integer count = jars.get(type.name());
		return count == null ? 0 : Math.max(0, count);
	}

	/**
	 * Adds or removes jars of one type.
	 *
	 * @param type the jar type
	 * @param delta the change, negative to spend
	 */
	void addJars(JarType type, int delta)
	{
		jars.put(type.name(), Math.max(0, jarCount(type) + delta));
	}

	/**
	 * Counts the healing items owned of one type.
	 *
	 * @param type the item type
	 * @return the number owned, never negative
	 */
	int itemCount(ItemType type)
	{
		Integer count = items.get(type.name());
		return count == null ? 0 : Math.max(0, count);
	}

	/**
	 * Adds or removes healing items of one type.
	 *
	 * @param type the item type
	 * @param delta the change, negative to spend
	 */
	void addItems(ItemType type, int delta)
	{
		items.put(type.name(), Math.max(0, itemCount(type) + delta));
	}

	/** @return the lead creature, or null when the team is empty */
	Creature lead()
	{
		return team.isEmpty() ? null : team.get(0);
	}

	/** @return how many different species have been caught at least once */
	int caughtSpecies()
	{
		return caught.size();
	}
}
