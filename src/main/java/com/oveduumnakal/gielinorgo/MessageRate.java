/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A simple limit on how many party messages one sender may get through: {@link #LIMIT} in any
 * {@link #WINDOW_MS}. Anyone in the party can send as many messages as they like, so the rest are dropped before
 * they cost the player any work.
 */
final class MessageRate
{
	/** Messages one sender may get through in one window. */
	static final int LIMIT = 10;

	/** Length of the window, in milliseconds. */
	static final long WINDOW_MS = 10_000L;

	/** Most senders remembered at once. */
	private static final int MAX_SENDERS = 100;

	private final Map<Long, Deque<Long>> seen = new HashMap<>();

	/**
	 * Counts a message and says whether it may be handled.
	 *
	 * @param sender the sender's party member id
	 * @param nowMs wall-clock time in milliseconds
	 * @return false when the sender already used up their share of the window
	 */
	boolean allow(long sender, long nowMs)
	{
		if (seen.size() >= MAX_SENDERS && !seen.containsKey(sender))
			forgetOld(nowMs);

		Deque<Long> times = seen.computeIfAbsent(sender, key -> new ArrayDeque<>());
		while (!times.isEmpty() && nowMs - times.peekFirst() >= WINDOW_MS)
			times.pollFirst();

		if (times.size() >= LIMIT)
			return false;

		times.addLast(nowMs);
		return true;
	}

	/** Forgets every sender, for when the player joins or leaves a party. */
	void clear()
	{
		seen.clear();
	}

	private void forgetOld(long nowMs)
	{
		Iterator<Deque<Long>> all = seen.values().iterator();
		while (all.hasNext())
		{
			Deque<Long> times = all.next();
			if (times.isEmpty() || nowMs - times.peekLast() >= WINDOW_MS)
				all.remove();
		}

		if (seen.size() >= MAX_SENDERS)
			seen.clear();
	}
}
