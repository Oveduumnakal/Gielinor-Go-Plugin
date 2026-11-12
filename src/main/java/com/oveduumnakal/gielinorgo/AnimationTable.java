/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * The animations of each NPC id a species can be drawn from: standing, walking, attacking, reacting to a hit, dying
 * and a second attack. The game client does not hand these out for an NPC type, so they are bundled;
 * {@code scripts/gen-animations.py} writes the file.
 */
final class AnimationTable
{
	/** Resource path of the bundled animation list, relative to this package. */
	static final String RESOURCE = "animations.json";

	/** The id that means "no animation". */
	static final int NONE = -1;

	private static final Type MAP_TYPE = new TypeToken<Map<Integer, int[]>>()
	{
	}.getType();

	private final Map<Integer, int[]> byNpcId;

	private AnimationTable(Map<Integer, int[]> byNpcId)
	{
		this.byNpcId = byNpcId;
	}

	/** @return a table that knows no animations, so every follower stands still */
	static AnimationTable empty()
	{
		return new AnimationTable(Collections.emptyMap());
	}

	/**
	 * Reads an animation table from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader a JSON object of NPC id to {@code [standing, walking, attack, defend, death]}; the last three
	 *     may be missing
	 * @return the table, empty when the JSON is missing or malformed
	 */
	static AnimationTable load(Gson gson, Reader reader)
	{
		try
		{
			Map<Integer, int[]> read = gson.fromJson(reader, MAP_TYPE);
			return read == null ? empty() : new AnimationTable(read);
		}
		catch (JsonParseException e)
		{
			return empty();
		}
	}

	/**
	 * Finds the animation an NPC plays while standing.
	 *
	 * @param npcId the NPC id
	 * @return the animation id, or {@link #NONE}
	 */
	int idle(int npcId)
	{
		return entry(npcId, 0);
	}

	/**
	 * Finds the stand animation for an NPC that may have no row in the table (a variant a game update added). A row
	 * is always trusted; with no row, the animation the live NPC itself stands in is used, so the model is never left
	 * in its bind pose.
	 *
	 * @param npcId the NPC id
	 * @param liveIdle the live NPC's own stand animation, or {@link #NONE} when there is no live NPC
	 * @return the animation id, or {@link #NONE}
	 */
	int idle(int npcId, int liveIdle)
	{
		return has(npcId) ? idle(npcId) : Math.max(NONE, liveIdle);
	}

	/**
	 * Finds the walk animation for an NPC that may have no row in the table, like {@link #idle(int, int)}: a row is
	 * always trusted; with no row the live NPC's own walk is used, or its stand when it has no walk.
	 *
	 * @param npcId the NPC id
	 * @param liveWalk the live NPC's own walk animation, or {@link #NONE}
	 * @param liveIdle the live NPC's own stand animation, or {@link #NONE}
	 * @return the animation id, or {@link #NONE}
	 */
	int walk(int npcId, int liveWalk, int liveIdle)
	{
		if (has(npcId))
			return walk(npcId);

		return liveWalk >= 0 ? liveWalk : Math.max(NONE, liveIdle);
	}

	/**
	 * @param npcId the NPC id
	 * @return whether the table has a row for it
	 */
	boolean has(int npcId)
	{
		return byNpcId.containsKey(npcId);
	}

	/**
	 * Finds the animation an NPC plays while walking.
	 *
	 * @param npcId the NPC id
	 * @return the animation id; the standing animation when the NPC has no walk of its own, or {@link #NONE}
	 */
	int walk(int npcId)
	{
		int walk = entry(npcId, 1);
		return walk == NONE ? idle(npcId) : walk;
	}

	/**
	 * Finds the animation an NPC plays when it attacks.
	 *
	 * @param npcId the NPC id
	 * @return the animation id, or {@link #NONE}
	 */
	int attack(int npcId)
	{
		return entry(npcId, 2);
	}

	/**
	 * Finds the second attack of an NPC that has one, such as a dragon's fire breath.
	 *
	 * @param npcId the NPC id
	 * @return the animation id, or {@link #NONE}
	 */
	int secondAttack(int npcId)
	{
		return entry(npcId, 5);
	}

	/**
	 * Finds the animation an NPC plays when it is hit.
	 *
	 * @param npcId the NPC id
	 * @return the animation id, or {@link #NONE}
	 */
	int defend(int npcId)
	{
		return entry(npcId, 3);
	}

	/**
	 * Finds the animation an NPC plays when it dies.
	 *
	 * @param npcId the NPC id
	 * @return the animation id, or {@link #NONE}
	 */
	int death(int npcId)
	{
		return entry(npcId, 4);
	}

	/** @return how many NPC ids have an animation */
	int size()
	{
		return byNpcId.size();
	}

	private int entry(int npcId, int slot)
	{
		int[] pair = byNpcId.get(npcId);
		return pair == null || pair.length <= slot || pair[slot] < 0 ? NONE : pair[slot];
	}
}
