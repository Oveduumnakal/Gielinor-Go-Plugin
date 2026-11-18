/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The tracker's game-thread state. It turns the game's {@link TrackerView} into the lines to draw, and holds two short
 * lived things the game does not: a pinned task that finished says "Done!" for {@link PinRules#DONE_HOLD_MS} before it
 * is unpinned, and a pinned egg that hatched leaves a "Hatched!" line for the same time. It has no clock of its own:
 * every call is told the time. It has no RuneLite types.
 */
final class TrackerModel
{
	private static final String DONE_TEXT = "Done!";
	private static final String HATCHED_TEXT = "Hatched!";

	/** A line of a pinned egg that hatched, kept only to show for a moment. */
	private static final class Ghost
	{
		private final String label;
		private final long until;

		Ghost(String label, long until)
		{
			this.label = label;
			this.until = until;
		}
	}

	private final Map<String, Long> doneSince = new HashMap<>();
	private final Map<String, Rarity> rarities = new HashMap<>();
	private final List<Ghost> ghosts = new ArrayList<>();

	/**
	 * Works out the lines to draw, and starts the hold for each pinned task that has just finished.
	 *
	 * @param view what the game says is pinned
	 * @param now the time in milliseconds
	 * @return the lines to draw: tasks, then eggs, then the "Hatched!" lines
	 */
	List<TrackerView.Line> frame(TrackerView view, long now)
	{
		List<TrackerView.Line> out = new ArrayList<>();
		Map<String, Long> seen = new HashMap<>();
		for (TrackerView.Line line : view.lines())
		{
			if (line.rarity() != null)
				rarities.put(line.label(), line.rarity());

			if (line.done() && line.kind() == TrackerView.Kind.TASK)
			{
				Long since = doneSince.get(line.id());
				seen.put(line.id(), since == null ? Long.valueOf(now) : since);
				out.add(line.finished(DONE_TEXT));
				continue;
			}

			out.add(line);
		}

		doneSince.clear();
		doneSince.putAll(seen);
		int place = 0;
		Iterator<Ghost> each = ghosts.iterator();
		while (each.hasNext())
		{
			Ghost ghost = each.next();
			if (now >= ghost.until)
			{
				each.remove();
				continue;
			}

			out.add(new TrackerView.Line(TrackerView.Kind.EGG, "hatched:" + place++, ghost.label, HATCHED_TEXT,
				1.0, rarities.get(ghost.label), true));
		}

		return out;
	}

	/**
	 * Lists the pinned tasks whose "Done!" has been showing long enough, to be unpinned.
	 *
	 * @param now the time in milliseconds
	 * @return the ids of the lines to unpin
	 */
	List<String> expired(long now)
	{
		List<String> ids = new ArrayList<>();
		for (Map.Entry<String, Long> entry : doneSince.entrySet())
		{
			if (now - entry.getValue() >= PinRules.DONE_HOLD_MS)
				ids.add(entry.getKey());
		}

		ids.sort(null);
		return ids;
	}

	/**
	 * Notes that a pinned egg hatched, so it shows "Hatched!" for a moment.
	 *
	 * @param label what the egg was called, such as {@code Rare egg}
	 * @param now the time in milliseconds
	 */
	void hatched(String label, long now)
	{
		ghosts.add(new Ghost(label, now + PinRules.DONE_HOLD_MS));
	}

	/** Forgets every line that is only being shown for a moment, for when the save is replaced. */
	void clear()
	{
		doneSince.clear();
		ghosts.clear();
	}
}
