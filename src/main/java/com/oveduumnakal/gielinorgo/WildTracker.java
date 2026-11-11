/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Decides which monsters in view are wild, and remembers them. Knows nothing about the game client: monsters are
 * identified by their index, so the rules can be tested without one.
 */
final class WildTracker
{
	/** Most wild creatures at once. */
	static final int MAX_WILD = 3;

	/** Shortest time between two creatures turning wild, in milliseconds. */
	static final long COOLDOWN_MS = 30_000L;

	private final Map<Integer, WildEntry> wild = new LinkedHashMap<>();
	private final Random random;
	private long lastWildMs = -COOLDOWN_MS;

	WildTracker(Random random)
	{
		this.random = random;
	}

	/**
	 * Gives a monster its chance to turn wild.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param species the monster's species
	 * @param combatLevel the monster's combat level
	 * @param underground whether the monster is underground
	 * @param nowMs the current wall-clock time in milliseconds
	 * @param oneIn the odds: the monster turns wild one time in this many
	 * @return the new wild entry, or null when the monster stays ordinary
	 */
	WildEntry offer(int npcIndex, Species species, int combatLevel, boolean underground, long nowMs, int oneIn)
	{
		if (species == null || wild.containsKey(npcIndex) || wild.size() >= MAX_WILD)
			return null;

		if (nowMs - lastWildMs < COOLDOWN_MS || random.nextInt(Math.max(1, oneIn)) != 0)
			return null;

		int level = CatchRules.wildLevel(combatLevel, random);
		WildEntry entry = new WildEntry(npcIndex, species, level, combatLevel, underground);
		wild.put(npcIndex, entry);
		lastWildMs = nowMs;
		return entry;
	}

	/**
	 * Looks up a wild creature.
	 *
	 * @param npcIndex the game's index for the monster
	 * @return the entry, or null when that monster is not wild
	 */
	WildEntry get(int npcIndex)
	{
		return wild.get(npcIndex);
	}

	/**
	 * Forgets a monster, because it was caught, fled, or left view.
	 *
	 * @param npcIndex the game's index for the monster
	 */
	void remove(int npcIndex)
	{
		wild.remove(npcIndex);
	}

	/** Forgets every wild creature, for a logout or a world hop. */
	void clear()
	{
		wild.clear();
	}

	/** @return every wild creature, oldest first */
	Collection<WildEntry> all()
	{
		return Collections.unmodifiableCollection(wild.values());
	}
}
