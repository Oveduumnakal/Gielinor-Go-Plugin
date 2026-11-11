/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Keeps track of the floating lines that are on screen. A line shows for a few seconds and then drops off. Battle
 * lines wait in a queue and come out one per game tick. Knows nothing about drawing, so the timing can be tested.
 */
final class NoticeBoard
{
	/** How long a line stays on screen, in milliseconds. */
	static final long LIFE_MS = 3_500L;

	/** Most lines floating over one head at once. */
	static final int MAX_PER_HEAD = 4;

	/** A notice on screen, with the time it appeared. */
	static final class Shown
	{
		private final Notice notice;
		private final long bornMs;

		private Shown(Notice notice, long bornMs)
		{
			this.notice = notice;
			this.bornMs = bornMs;
		}

		/** @return the notice */
		Notice notice()
		{
			return notice;
		}

		/**
		 * How far through its life the line is.
		 *
		 * @param nowMs the current wall-clock time in milliseconds
		 * @return age from 0, just shown, to 1, about to disappear
		 */
		double age(long nowMs)
		{
			return Math.max(0.0, Math.min(1.0, (nowMs - bornMs) / (double) LIFE_MS));
		}
	}

	private final List<Shown> shown = new ArrayList<>();
	private final Deque<Notice> queued = new ArrayDeque<>();

	/**
	 * Shows a line straight away. When its head already has {@link #MAX_PER_HEAD} lines, the oldest one goes.
	 *
	 * @param notice the line
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void show(Notice notice, long nowMs)
	{
		int sameHead = 0;
		for (Shown line : shown)
		{
			if (line.notice.npcIndex() == notice.npcIndex())
				sameHead++;
		}

		Iterator<Shown> oldestFirst = shown.iterator();
		while (sameHead >= MAX_PER_HEAD && oldestFirst.hasNext())
		{
			if (oldestFirst.next().notice.npcIndex() != notice.npcIndex())
				continue;

			oldestFirst.remove();
			sameHead--;
		}

		shown.add(new Shown(notice, nowMs));
	}

	/**
	 * Queues lines for playback, one per call to {@link #tick(long)}.
	 *
	 * @param notices the lines, in order
	 */
	void queue(List<Notice> notices)
	{
		queued.addAll(notices);
	}

	/**
	 * Shows the next queued line, if there is one. Called once per game tick.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void tick(long nowMs)
	{
		Notice next = queued.poll();
		if (next != null)
			show(next, nowMs);
	}

	/** Removes every line, shown or queued. */
	void clear()
	{
		shown.clear();
		queued.clear();
	}

	/**
	 * Lists the lines that are still on screen, dropping the ones whose time is up.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return the live lines, oldest first
	 */
	List<Shown> visible(long nowMs)
	{
		shown.removeIf(line -> nowMs - line.bornMs >= LIFE_MS);
		return new ArrayList<>(shown);
	}
}
