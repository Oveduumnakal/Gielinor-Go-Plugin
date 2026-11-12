/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** What the Log tab shows of the milestones: the next species goal and the full list. Immutable. */
final class MilestoneView
{
	/** A view with nothing to show, used before the game has been set up. */
	static final MilestoneView NONE = new MilestoneView(null, 0, 0, Collections.emptyList());

	/** One milestone as the dialog lists it. */
	static final class Row
	{
		private final String label;
		private final int current;
		private final int target;
		private final String reward;
		private final boolean paid;
		private final boolean family;

		Row(String label, int current, int target, String reward, boolean paid, boolean family)
		{
			this.label = label;
			this.current = Math.min(current, target);
			this.target = target;
			this.reward = reward;
			this.paid = paid;
			this.family = family;
		}

		/** @return the goal in words, such as {@code 100 species} or {@code All Goblins} */
		String label()
		{
			return label;
		}

		/** @return progress so far, never above {@link #target()} */
		int current()
		{
			return current;
		}

		/** @return the number that completes the milestone */
		int target()
		{
			return target;
		}

		/** @return the reward in words */
		String reward()
		{
			return reward;
		}

		/** @return whether the reward has been paid */
		boolean paid()
		{
			return paid;
		}

		/** @return whether this is a family milestone rather than a species count */
		boolean family()
		{
			return family;
		}

		/** @return whether the goal is met, even when the reward still waits for a free egg slot */
		boolean reached()
		{
			return current >= target;
		}

		@Override
		public boolean equals(Object other)
		{
			if (!(other instanceof Row))
				return false;

			Row that = (Row) other;
			return current == that.current && target == that.target && paid == that.paid && family == that.family
				&& Objects.equals(label, that.label) && Objects.equals(reward, that.reward);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(label, current, target, reward, paid, family);
		}
	}

	private final String nextLabel;
	private final int nextCurrent;
	private final int nextTarget;
	private final List<Row> rows;

	MilestoneView(String nextLabel, int nextCurrent, int nextTarget, List<Row> rows)
	{
		this.nextLabel = nextLabel;
		this.nextCurrent = nextCurrent;
		this.nextTarget = nextTarget;
		this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
	}

	/** @return the next species goal in words, such as {@code Next: 100 species}, or null when there is none left */
	String nextLabel()
	{
		return nextLabel;
	}

	/** @return species caught so far */
	int nextCurrent()
	{
		return nextCurrent;
	}

	/** @return the species count of the next goal, 0 when there is none left */
	int nextTarget()
	{
		return nextTarget;
	}

	/** @return every milestone: the species goals first, then the families */
	List<Row> rows()
	{
		return rows;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof MilestoneView))
			return false;

		MilestoneView that = (MilestoneView) other;
		return nextCurrent == that.nextCurrent && nextTarget == that.nextTarget
			&& Objects.equals(nextLabel, that.nextLabel) && rows.equals(that.rows);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(nextLabel, nextCurrent, nextTarget, rows);
	}
}
