/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the tracker shows, copied out of the game at one moment: one {@link Line} for each pinned task and carried egg,
 * tasks first and then eggs. Immutable, so the game thread can hand it on without a half-finished change. It has no
 * RuneLite types.
 */
final class TrackerView
{
	/** A view with nothing pinned. */
	static final TrackerView NONE = new TrackerView(Collections.emptyList());

	private static final String TASK_PREFIX = "task:";
	private static final String EGG_PREFIX = "egg:";

	/** What a line stands for. */
	enum Kind
	{
		/** A daily or weekly task. */
		TASK,

		/** A carried egg. */
		EGG
	}

	/** One pinned thing: its words, its value and how far along it is. */
	static final class Line
	{
		private final Kind kind;
		private final String id;
		private final String label;
		private final String valueText;
		private final double fraction;
		private final Rarity rarity;
		private final boolean done;

		/**
		 * Makes a line.
		 *
		 * @param kind a task or an egg
		 * @param id {@code task:<slot>} or {@code egg:<place>}, or any text for a line the game no longer holds
		 * @param label what it is called
		 * @param valueText the progress in words, such as {@code 340 / 1.2k} or {@code 780 steps}
		 * @param fraction how far along it is, from 0 to 1
		 * @param rarity the rarity of an egg, or null for a task
		 * @param done whether the task is finished, or the egg just hatched
		 */
		Line(Kind kind, String id, String label, String valueText, double fraction, Rarity rarity, boolean done)
		{
			this.kind = kind;
			this.id = id;
			this.label = label;
			this.valueText = valueText;
			this.fraction = Math.max(0.0, Math.min(1.0, fraction));
			this.rarity = rarity;
			this.done = done;
		}

		/** @return a task or an egg */
		Kind kind()
		{
			return kind;
		}

		/** @return the line's name for the game, such as {@code task:2} */
		String id()
		{
			return id;
		}

		/** @return what the line is called */
		String label()
		{
			return label;
		}

		/** @return the progress in words, such as {@code 780 steps} or {@code Done!} */
		String valueText()
		{
			return valueText;
		}

		/** @return how far along it is, from 0 to 1 */
		double fraction()
		{
			return fraction;
		}

		/** @return the rarity of an egg, or null for a task */
		Rarity rarity()
		{
			return rarity;
		}

		/** @return whether the task is finished, or the egg just hatched */
		boolean done()
		{
			return done;
		}

		/**
		 * Copies the line as a finished one.
		 *
		 * @param text the words to show in place of the progress, such as {@code Done!}
		 * @return the new line, full and marked done
		 */
		Line finished(String text)
		{
			return new Line(kind, id, label, text, 1.0, rarity, true);
		}
	}

	private final List<Line> lines;

	/**
	 * Makes a view.
	 *
	 * @param lines the pinned things, tasks first and then eggs
	 */
	TrackerView(List<Line> lines)
	{
		this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
	}

	/** @return the pinned things, tasks first and then eggs */
	List<Line> lines()
	{
		return lines;
	}

	/** @return how many things are pinned */
	int count()
	{
		return lines.size();
	}

	/**
	 * @param slot a task place: 0 to 2 for the day's tasks, 3 for the week's
	 * @return the id of that task's line
	 */
	static String taskId(int slot)
	{
		return TASK_PREFIX + slot;
	}

	/**
	 * @param place an egg's place in the list of carried eggs
	 * @return the id of that egg's line
	 */
	static String eggId(int place)
	{
		return EGG_PREFIX + place;
	}

	/**
	 * @param id a line's id
	 * @return the task slot the id names, or -1 when it is not a task's
	 */
	static int taskSlot(String id)
	{
		if (id == null || !id.startsWith(TASK_PREFIX))
			return -1;

		try
		{
			return Integer.parseInt(id.substring(TASK_PREFIX.length()));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}
}
