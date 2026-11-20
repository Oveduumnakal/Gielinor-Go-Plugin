/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;

/**
 * Decides which skilling actions count as steps toward hatching eggs. A positive XP drop in a skill that is not a
 * combat skill is one action step, at most one for each game tick however many drops that tick has, and at most
 * {@link #PER_HOUR} in any hour. Walking stays far faster (one tile a game tick is 6,000 an hour, running 12,000), so
 * this only lets a player who does not walk hatch eggs slowly. It never counts toward jars, walk tasks or bond. The
 * hour is kept in memory only, so a restart gives at worst one extra hour of allowance. Holds no RuneLite types.
 */
final class ActionSteps
{
	/** The most action steps in any rolling hour. */
	static final int PER_HOUR = 1_200;

	/** The length of the rolling window, in milliseconds. */
	static final long WINDOW_MS = 3_600_000L;

	/** The skills whose XP never counts: one blow gives drops in several of them at once. */
	private static final Set<String> NOT_COUNTED = Set.of("ATTACK", "STRENGTH", "DEFENCE", "RANGED", "MAGIC",
		"HITPOINTS", "OVERALL");

	private final Deque<Long> taken = new ArrayDeque<>();
	private int lastTick = Integer.MIN_VALUE;

	/**
	 * Tells whether an XP drop in a skill can count as an action step.
	 *
	 * @param skill the skill's name in capitals, as {@link Enum#name()} of the game's skill
	 * @param xpGained the OSRS XP in the drop
	 * @return true for a positive drop in a skill that is not a combat skill
	 */
	static boolean counts(String skill, int xpGained)
	{
		return xpGained > 0 && skill != null && !NOT_COUNTED.contains(skill.toUpperCase(Locale.ROOT));
	}

	/**
	 * Takes one action step if the game tick has none yet and the hour has room.
	 *
	 * @param gameTick the number of the game tick the drop came in
	 * @param nowMs the time in milliseconds
	 * @return 1 when the step counts, 0 when this tick already counted one or the hour is full
	 */
	int take(int gameTick, long nowMs)
	{
		if (gameTick == lastTick)
			return 0;

		while (!taken.isEmpty() && nowMs - taken.peekFirst() >= WINDOW_MS)
			taken.pollFirst();

		if (taken.size() >= PER_HOUR)
			return 0;

		lastTick = gameTick;
		taken.addLast(nowMs);
		return 1;
	}

	/** Forgets the hour and the tick, for a new profile or a new session. */
	void reset()
	{
		taken.clear();
		lastTick = Integer.MIN_VALUE;
	}
}
