/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Decides which monsters in view are wild, and remembers them. Knows nothing about the game client: monsters are
 * identified by their index and NPC id, so the rules can be tested without one.
 *
 * <p>Each roll is remembered for {@link #MEMORY_MS} after the monster last left view, so a monster that reloads
 * (walking out of view and back) keeps its result instead of rolling again.</p>
 */
final class WildTracker
{
	/** Most wild creatures at once. */
	static final int MAX_WILD = 3;

	/** Shortest time between two creatures turning wild, in milliseconds. */
	static final long COOLDOWN_MS = 30_000L;

	/** A wild creature is gilded one time in this many. */
	static final int GILDED_ONE_IN = 500;

	/** A wild creature is a stronger roaming one one time in this many. */
	static final int ROAMING_ONE_IN = 20;

	/** Levels a roaming creature has over an ordinary one. */
	static final int ROAMING_LEVELS = 10;

	/** How long a roll is remembered, counted from the roll or from the monster last leaving view. */
	static final long MEMORY_MS = 180_000L;

	private final Map<Integer, WildEntry> wild = new LinkedHashMap<>();
	private final Map<Integer, Roll> rolls = new HashMap<>();
	private final Random random;
	private long lastWildMs = -COOLDOWN_MS;

	WildTracker(Random random)
	{
		this.random = random;
	}

	/**
	 * Gives a monster its chance to turn wild, unless it already had one within {@link #MEMORY_MS}.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param npcId the monster's NPC id, which tells a reused index apart
	 * @param species the monster's species
	 * @param combatLevel the monster's combat level
	 * @param underground whether the monster is underground
	 * @param nowMs the current wall-clock time in milliseconds
	 * @param chance the chance, from 0 to 1, that the monster turns wild
	 * @param gildedOneIn the odds that a wild creature is gilded: one time in this many
	 * @param mayRoam whether a wild creature may turn out to be a roaming one, one time in {@link #ROAMING_ONE_IN}
	 * @return the new wild entry, or null when the monster stays ordinary
	 */
	WildEntry offer(int npcIndex, int npcId, Species species, int combatLevel, boolean underground, long nowMs,
		double chance, int gildedOneIn, boolean mayRoam)
	{
		forgetOld(nowMs);
		if (species == null || species.questOnly())
			return null;

		if (!mayRoll(npcIndex, npcId, nowMs) || wild.size() >= MAX_WILD)
			return null;

		if (nowMs - lastWildMs < COOLDOWN_MS)
			return null;

		if (random.nextDouble() >= chance)
		{
			rolls.put(npcIndex, new Roll(npcId, null, nowMs));
			return null;
		}

		int level = CatchRules.wildLevel(combatLevel, random);
		boolean gilded = random.nextInt(Math.max(1, gildedOneIn)) == 0;
		boolean roaming = mayRoam && random.nextInt(ROAMING_ONE_IN) == 0;
		if (roaming)
			level = Math.min(XpRules.MAX_LEVEL, level + ROAMING_LEVELS);

		WildEntry entry = new WildEntry(npcIndex, species, level, combatLevel, underground, gilded, roaming)
			.withNpcId(npcId);
		wild.put(npcIndex, entry);
		rolls.put(npcIndex, new Roll(npcId, entry, nowMs));
		lastWildMs = nowMs;
		return entry;
	}

	/**
	 * Brings back a wild creature that left view and came back, with its level, gilding and battle state.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param npcId the monster's NPC id
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return the remembered entry, now wild again, or null when there is none to bring back or no room
	 */
	WildEntry restore(int npcIndex, int npcId, long nowMs)
	{
		Roll roll = fresh(npcIndex, npcId, nowMs);
		if (roll == null || roll.entry == null || roll.entry.species().questOnly())
			return null;

		if (wild.containsKey(npcIndex))
			return null;

		if (wild.size() >= MAX_WILD)
		{
			roll.atMs = nowMs;
			return null;
		}

		wild.put(npcIndex, roll.entry);
		return roll.entry;
	}

	/**
	 * Tells whether a monster in view has a remembered wild entry that is not wild right now, so a
	 * {@link #restore} may be tried.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param npcId the monster's NPC id
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return whether there is a remembered wild creature waiting to be brought back
	 */
	boolean canRestore(int npcIndex, int npcId, long nowMs)
	{
		Roll roll = fresh(npcIndex, npcId, nowMs);
		boolean open = roll != null && roll.entry != null && !roll.entry.species().questOnly();
		return open && !wild.containsKey(npcIndex);
	}

	/**
	 * Tells whether a monster may roll now: it is not wild, and has no roll remembered.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param npcId the monster's NPC id
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return whether {@link #offer} would roll for it, cap and cooldown aside
	 */
	boolean mayRoll(int npcIndex, int npcId, long nowMs)
	{
		return !wild.containsKey(npcIndex) && fresh(npcIndex, npcId, nowMs) == null;
	}

	/**
	 * Notes that a monster left view. A wild one stops being wild for now, and its roll is remembered from now on.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void leftView(int npcIndex, long nowMs)
	{
		wild.remove(npcIndex);
		Roll roll = rolls.get(npcIndex);
		if (roll != null)
			roll.atMs = nowMs;
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
	 * Ends a wild creature for good, because it was caught or fled. The monster is remembered as not wild, so it
	 * does not roll again straight away.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void remove(int npcIndex, long nowMs)
	{
		wild.remove(npcIndex);
		Roll roll = rolls.get(npcIndex);
		if (roll != null)
			rolls.put(npcIndex, new Roll(roll.npcId, null, nowMs));
	}

	/**
	 * Drops the current wild creatures while creatures cannot be wild here (the wilderness, an instance). The rolls
	 * and their entries are kept, and their memory restarts now, so a creature that was battled or rolled "no" is
	 * the same one when its monster comes back into a playable area.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void clearWild(long nowMs)
	{
		wild.clear();
		for (Roll roll : rolls.values())
			roll.atMs = nowMs;
	}

	/** Forgets every wild creature and every roll, for a logout, a world hop or a profile change. */
	void clear()
	{
		wild.clear();
		rolls.clear();
	}

	/** @return every wild creature, oldest first */
	Collection<WildEntry> all()
	{
		return Collections.unmodifiableCollection(wild.values());
	}

	private Roll fresh(int npcIndex, int npcId, long nowMs)
	{
		Roll roll = rolls.get(npcIndex);
		if (roll == null || roll.npcId != npcId)
			return null;

		boolean inView = roll.entry != null && wild.get(npcIndex) == roll.entry;
		return inView || nowMs - roll.atMs < MEMORY_MS ? roll : null;
	}

	private void forgetOld(long nowMs)
	{
		Iterator<Map.Entry<Integer, Roll>> it = rolls.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Integer, Roll> next = it.next();
			Roll roll = next.getValue();
			boolean inView = roll.entry != null && wild.get(next.getKey()) == roll.entry;
			if (!inView && nowMs - roll.atMs >= MEMORY_MS)
				it.remove();
		}
	}

	/** One remembered roll: the monster's NPC id, the wild entry it produced (null for "not wild"), and when. */
	private static final class Roll
	{
		private final int npcId;
		private final WildEntry entry;
		private long atMs;

		Roll(int npcId, WildEntry entry, long atMs)
		{
			this.npcId = npcId;
			this.entry = entry;
			this.atMs = atMs;
		}
	}
}
