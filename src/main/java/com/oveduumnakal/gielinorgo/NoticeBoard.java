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
 * lines wait in a queue and come out one every {@link #LINE_TICKS} game ticks. Knows nothing about drawing, so the
 * timing can be tested.
 */
final class NoticeBoard
{
	/** How long a line stays on screen, in milliseconds. */
	static final long LIFE_MS = 3_500L;

	/** Game ticks between two battle lines. */
	static final int LINE_TICKS = 3;

	/** Most lines floating over one head, or one stage anchor, at once. */
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
	private final List<Notice> held = new ArrayList<>();
	private int wait;

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
			if (line.notice.sameAnchor(notice))
				sameHead++;
		}

		Iterator<Shown> oldestFirst = shown.iterator();
		while (sameHead >= MAX_PER_HEAD && oldestFirst.hasNext())
		{
			if (!oldestFirst.next().notice.sameAnchor(notice))
				continue;

			oldestFirst.remove();
			sameHead--;
		}

		shown.add(new Shown(notice, nowMs));
	}

	/**
	 * Queues lines for playback, one every {@link #LINE_TICKS} calls to {@link #tick(long)}.
	 *
	 * @param notices the lines, in order
	 */
	void queue(List<Notice> notices)
	{
		queued.addAll(notices);
	}

	/**
	 * Adds a battle's lines behind whatever is already waiting. They are held back, in one ordered list, while a
	 * battle is acted out on screen ({@code staged}) or while an earlier battle's lines are still held, so a second
	 * battle never jumps ahead of the first. Otherwise they go straight into the queue.
	 *
	 * @param notices the lines, in order
	 * @param staged whether this battle is acted out on screen, so its lines wait for {@link #release()}
	 */
	void enqueue(List<Notice> notices, boolean staged)
	{
		if (staged || !held.isEmpty())
			held.addAll(notices);
		else
			queue(notices);
	}

	/** @return whether lines are being held back for a battle that is acted out on screen */
	boolean holding()
	{
		return !held.isEmpty();
	}

	/** Moves every held line into the queue, in order. */
	void release()
	{
		queue(held);
		held.clear();
	}

	/**
	 * Shows the next queued line when one is due. Called once per game tick. The first line of a queue that arrives
	 * once the previous one has finished shows at once; the next one {@link #LINE_TICKS} ticks later.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 */
	void tick(long nowMs)
	{
		if (queued.isEmpty())
		{
			wait = 0;
			return;
		}

		if (wait > 0)
		{
			wait--;
			return;
		}

		Notice next = queued.poll();

		show(next, nowMs);
		wait = LINE_TICKS - 1;
	}

	/** Removes every line, shown, queued or held. */
	void clear()
	{
		shown.clear();
		queued.clear();
		held.clear();
		wait = 0;
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
