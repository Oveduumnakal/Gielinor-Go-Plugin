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
 * Decides which monsters in view roll wild, and keeps the wild creatures that came of it. Knows nothing about the
 * game client: a real monster is only an index and an NPC id here, and a wild creature is its own thing with its own
 * id from a counter, so it carries on whatever its monster does. Two memories are kept apart: the wild creatures
 * that exist right now, and the real monsters that have rolled.
 *
 * <p>A monster's roll is remembered for {@link #MEMORY_MS} after it last left view, so a monster that reloads
 * (walking out of view and back) does not roll again. A monster that rolled wild stays remembered for as long as it
 * is in view.</p>
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
	private long previousWildMs = -COOLDOWN_MS;
	private int nextId = 1;

	WildTracker(Random random)
	{
		this.random = random;
	}

	/**
	 * Gives a monster its chance to roll wild, unless it already rolled within {@link #MEMORY_MS}.
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
	 * @return the new wild entry, with an id of its own, or null when the monster stays ordinary
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
			rolls.put(npcIndex, new Roll(npcId, false, nowMs));
			return null;
		}

		int level = CatchRules.wildLevel(combatLevel, random);
		boolean gilded = random.nextInt(Math.max(1, gildedOneIn)) == 0;
		boolean roaming = mayRoam && random.nextInt(ROAMING_ONE_IN) == 0;
		if (roaming)
			level = Math.min(XpRules.MAX_LEVEL, level + ROAMING_LEVELS);

		WildEntry entry = new WildEntry(nextId++, species, level, combatLevel, underground, gilded, roaming)
			.withNpcId(npcId);
		wild.put(entry.wildId(), entry);
		rolls.put(npcIndex, new Roll(npcId, true, nowMs));
		previousWildMs = lastWildMs;
		lastWildMs = nowMs;
		return entry;
	}

	/**
	 * Makes a wild creature that no real monster came from, such as one met while skilling. It is tied to no
	 * monster, so no roll is remembered and no monster's roll can touch it. It counts toward {@link #MAX_WILD}, but
	 * it neither waits for nor starts the {@link #COOLDOWN_MS} between monsters: the caller keeps its own cooldown.
	 *
	 * @param species the species
	 * @param underground whether the player is underground, which gives the creature the cave trait
	 * @param gildedOneIn the odds that the creature is gilded: one time in this many
	 * @param mayRoam whether the creature may turn out to be a roaming one, one time in {@link #ROAMING_ONE_IN}
	 * @return the new wild entry, with an id of its own, or null when {@link #MAX_WILD} creatures exist already or
	 *     the species has no NPC id to draw it with
	 */
	WildEntry offerLoose(Species species, boolean underground, int gildedOneIn, boolean mayRoam)
	{
		if (species == null || species.questOnly() || wild.size() >= MAX_WILD || species.npcIdFor(0) <= 0)
			return null;

		int combatLevel = species.level();
		int level = CatchRules.wildLevel(combatLevel, random);
		boolean gilded = random.nextInt(Math.max(1, gildedOneIn)) == 0;
		boolean roaming = mayRoam && random.nextInt(ROAMING_ONE_IN) == 0;
		if (roaming)
			level = Math.min(XpRules.MAX_LEVEL, level + ROAMING_LEVELS);

		WildEntry entry = new WildEntry(nextId++, species, level, combatLevel, underground, gilded, roaming)
			.withNpcId(species.npcIdFor(0));
		wild.put(entry.wildId(), entry);
		return entry;
	}

	/**
	 * Takes back a creature that {@link #offerLoose} made but that could not be put on screen. Nothing else is
	 * undone, since a loose creature touches neither the cooldown nor any roll.
	 *
	 * @param entry the entry that {@link #offerLoose} returned
	 */
	void withdrawLoose(WildEntry entry)
	{
		wild.remove(entry.wildId());
	}

	/**
	 * Takes back a creature that was just rolled but could not be put on screen, such as one with no free tile near
	 * its monster. The cooldown it started is undone, so the next monster may roll at once. The monster stays
	 * remembered as having rolled.
	 *
	 * @param entry the entry that {@link #offer} returned
	 */
	void withdraw(WildEntry entry)
	{
		if (wild.remove(entry.wildId()) != null)
			lastWildMs = previousWildMs;
	}

	/**
	 * Tells whether a monster may roll now: it has no roll remembered.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param npcId the monster's NPC id
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return whether {@link #offer} would roll for it, cap and cooldown aside
	 */
	boolean mayRoll(int npcIndex, int npcId, long nowMs)
	{
		return fresh(npcIndex, npcId, nowMs) == null;
	}

	/**
	 * Notes that a monster left view. Its roll is remembered from now on for {@link #MEMORY_MS}. A wild creature it
	 * made is not touched.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void leftView(int npcIndex, long nowMs)
	{
		Roll roll = rolls.get(npcIndex);
		if (roll != null)
		{
			roll.inView = false;
			roll.atMs = nowMs;
		}
	}

	/**
	 * Looks up a wild creature.
	 *
	 * @param wildId the creature's id
	 * @return the entry, or null when there is no such wild creature
	 */
	WildEntry get(int wildId)
	{
		return wild.get(wildId);
	}

	/**
	 * Ends a wild creature for good, because it was caught, fled or its time was up.
	 *
	 * @param wildId the creature's id
	 */
	void remove(int wildId)
	{
		wild.remove(wildId);
	}

	/**
	 * Drops the current wild creatures while creatures cannot be wild here (the wilderness, an instance). The rolls
	 * are kept, and their memory restarts now, so a monster that rolled does not roll again the moment it comes back
	 * into a playable area.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void clearWild(long nowMs)
	{
		wild.clear();
		for (Roll roll : rolls.values())
		{
			roll.inView = false;
			roll.atMs = nowMs;
		}
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

		return roll.inView || nowMs - roll.atMs < MEMORY_MS ? roll : null;
	}

	private void forgetOld(long nowMs)
	{
		Iterator<Roll> it = rolls.values().iterator();
		while (it.hasNext())
		{
			Roll roll = it.next();
			if (!roll.inView && nowMs - roll.atMs >= MEMORY_MS)
				it.remove();
		}
	}

	/**
	 * One remembered roll: the monster's NPC id, whether it came out wild, when, and whether the monster is still in
	 * view. A roll that came out wild is kept while the monster is in view; one that came out ordinary runs out
	 * {@link #MEMORY_MS} after it was made.
	 */
	private static final class Roll
	{
		private final int npcId;
		private boolean inView;
		private long atMs;

		Roll(int npcId, boolean wild, long atMs)
		{
			this.npcId = npcId;
			this.inView = wild;
			this.atMs = atMs;
		}
	}
}
