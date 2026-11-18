/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The motion of the Party page, kept apart from the painting so it is tested with a clock that is passed in. Rows slide
 * in from the left one after another when the page opens; the health and experience bars ease to a new value; the
 * pointer's row nudges right and its portrait hops once; the selection cursor glides between rows; and when the team
 * changes, each creature is found again in the new list, so Make lead slides the two rows to each other's place, a
 * creature moved to storage slides out while the rows under it close the gap, and a creature that arrives slides in.
 * A row that is sliding refuses clicks until it lands (at most {@link WindowTimes#SLIDE}); every other hit rectangle is
 * the final one. Reduced and Off modes slide and hop nothing. Holds no RuneLite types.
 */
final class PartyMotion
{
	/** How far a row starts to the left when it slides in, as a share of its width. */
	static final double ENTER_FROM = 0.35;

	/** The height of a portrait's hop, in layout units. */
	static final double HOP_UNITS = 3;

	private final Animations animations;
	private final Smoothed[] hp = new Smoothed[PartyLayout.SLOTS];
	private final Smoothed[] xp = new Smoothed[PartyLayout.SLOTS];
	private final Smoothed[] hover = new Smoothed[PartyLayout.SLOTS];
	private final Smoothed cursor = new Smoothed(0, WindowTimes.CURSOR_GLIDE);
	private final List<Ghost> ghosts = new ArrayList<>();
	private List<CreatureView> last = Collections.emptyList();
	private int hovered = -1;

	/** A row that left the team and is sliding out. */
	static final class Ghost
	{
		private final CreatureView view;
		private final int index;
		private final long startMs;

		Ghost(CreatureView view, int index, long startMs)
		{
			this.view = view;
			this.index = index;
			this.startMs = startMs;
		}

		/** @return the creature that left */
		CreatureView view()
		{
			return view;
		}

		/** @return the row it was on */
		int index()
		{
			return index;
		}

		/**
		 * @param nowMs the time in milliseconds
		 * @return how far along the slide out is, from 0 to 1
		 */
		double progress(long nowMs)
		{
			return Math.max(0.0, Math.min(1.0, (double) (nowMs - startMs) / WindowTimes.SLIDE));
		}
	}

	/**
	 * Creates the motion.
	 *
	 * @param animations the window's table of animations, which keeps the slides
	 */
	PartyMotion(Animations animations)
	{
		this.animations = animations;
	}

	/**
	 * Starts the rows sliding in, one after another, as when the window or the page opens.
	 *
	 * @param rowWidth how wide a row is, in pixels
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 */
	void enter(int rowWidth, MotionMode mode, long nowMs)
	{
		ghosts.clear();
		for (int i = 0; i < PartyLayout.SLOTS; i++)
			animations.enter(xId(i), mode, -rowWidth * ENTER_FROM, 0, nowMs, i);
	}

	/**
	 * Takes in the team as it is now. The creatures are matched with the ones of the last call: one that changed place
	 * slides there, one that is new slides in, one that is gone slides out, and the bars of each creature carry on from
	 * where they were.
	 *
	 * @param team the team, lead first
	 * @param layout where the rows are
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 */
	void sync(List<CreatureView> team, PartyLayout layout, MotionMode mode, long nowMs)
	{
		if (team == last)
			return;

		List<CreatureView> old = last;
		boolean same = old.size() == team.size();
		for (int i = 0; same && i < team.size(); i++)
			same = old.get(i).sameCreature(team.get(i));

		if (!same && !old.isEmpty())
			reconcile(old, team, layout, mode, nowMs);

		for (int i = 0; i < team.size(); i++)
		{
			if (hp[i] == null)
			{
				hp[i] = new Smoothed(RowModel.of(team.get(i), i).hp(), WindowTimes.HP_BAR);
				xp[i] = new Smoothed(team.get(i).xpProgress(), WindowTimes.XP_BAR);
			}
			else
			{
				hp[i].set(RowModel.of(team.get(i), i).hp(), nowMs, mode);
				xp[i].set(team.get(i).xpProgress(), nowMs, mode);
			}
		}

		last = team;
	}

	private void reconcile(List<CreatureView> old, List<CreatureView> team, PartyLayout layout, MotionMode mode,
		long nowMs)
	{
		boolean[] taken = new boolean[old.size()];
		int[] from = new int[team.size()];
		for (int i = 0; i < team.size(); i++)
		{
			from[i] = i < old.size() && !taken[i] && old.get(i).sameCreature(team.get(i)) ? i : -1;
			if (from[i] >= 0)
				taken[i] = true;
		}

		for (int i = 0; i < team.size(); i++)
		{
			for (int j = 0; from[i] < 0 && j < old.size(); j++)
			{
				if (!taken[j] && old.get(j).sameCreature(team.get(i)))
				{
					from[i] = j;
					taken[j] = true;
				}
			}
		}

		Smoothed[] oldHp = hp.clone();
		Smoothed[] oldXp = xp.clone();
		for (int i = 0; i < PartyLayout.SLOTS; i++)
		{
			hp[i] = null;
			xp[i] = null;
		}

		int step = layout.rows[1].y - layout.rows[0].y;
		for (int i = 0; i < team.size(); i++)
		{
			if (from[i] < 0)
			{
				animations.enter(xId(i), mode, -layout.rows[i].width * ENTER_FROM, 0, nowMs, 0);
				continue;
			}

			hp[i] = oldHp[from[i]];
			xp[i] = oldXp[from[i]];
			if (from[i] != i)
				animations.slide(yId(i), mode, layout.rows[0].y + from[i] * step, layout.rows[i].y, nowMs);
		}

		for (int j = 0; j < old.size(); j++)
		{
			if (!taken[j] && mode.moves())
				ghosts.add(new Ghost(old.get(j), j, nowMs));
		}
	}

	/**
	 * Gives the health to draw on a row's bar.
	 *
	 * @param row the row
	 * @param nowMs the time in milliseconds
	 * @return the share from 0 to 1, easing toward the creature's health
	 */
	double hp(int row, long nowMs)
	{
		return hp[row] == null ? 0.0 : hp[row].value(nowMs);
	}

	/**
	 * Gives the experience to draw on a row's bar.
	 *
	 * @param row the row
	 * @param nowMs the time in milliseconds
	 * @return the share from 0 to 1, easing toward the creature's progress
	 */
	double xp(int row, long nowMs)
	{
		return xp[row] == null ? 0.0 : xp[row].value(nowMs);
	}

	/**
	 * Gives where a row's top edge is drawn.
	 *
	 * @param row the row
	 * @param resting where it rests
	 * @param nowMs the time in milliseconds
	 * @return the resting place, or where the slide has got to
	 */
	double rowY(int row, int resting, long nowMs)
	{
		return animations.value(yId(row), nowMs, resting);
	}

	/**
	 * Gives how far a row is shifted left or right while it slides in.
	 *
	 * @param row the row
	 * @param nowMs the time in milliseconds
	 * @return 0 at rest, negative while it is still coming in from the left
	 */
	double rowX(int row, long nowMs)
	{
		return animations.value(xId(row), nowMs, 0.0);
	}

	/**
	 * @param row the row
	 * @param nowMs the time in milliseconds
	 * @return whether the row is sliding to another place and so ignores clicks
	 */
	boolean sliding(int row, long nowMs)
	{
		return animations.blocksClicks(yId(row), nowMs);
	}

	/**
	 * Gives the rows that are sliding out of the page.
	 *
	 * @param nowMs the time in milliseconds
	 * @return the creatures that left less than {@link WindowTimes#SLIDE} ago; older ones are dropped
	 */
	List<Ghost> ghosts(long nowMs)
	{
		ghosts.removeIf(ghost -> ghost.progress(nowMs) >= 1.0);
		return ghosts;
	}

	/**
	 * Tells the motion which row the highlight is on, so that row nudges, hops and the cursor glides to it.
	 *
	 * @param row the row, or -1 for none
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 */
	void highlight(int row, MotionMode mode, long nowMs)
	{
		if (row == hovered)
			return;

		if (hovered >= 0 && hover[hovered] != null)
			hover[hovered].set(0.0, nowMs, mode);

		hovered = row;
		if (row < 0)
			return;

		if (hover[row] == null)
			hover[row] = new Smoothed(0.0, WindowTimes.HOVER_NUDGE);

		hover[row].set(1.0, nowMs, mode);
		cursor.set(row, nowMs, mode);
		animations.start(hopId(row), Tween.of(0.0, 1.0, nowMs, mode.moveMs(WindowTimes.PORTRAIT_HOP), Easing.LINEAR),
			false);
	}

	/**
	 * Gives how far a row is nudged by the pointer, as a share of the nudge.
	 *
	 * @param row the row
	 * @param nowMs the time in milliseconds
	 * @return from 0 to 1; always 0 unless motion moves things
	 */
	double nudge(int row, long nowMs)
	{
		return hover[row] == null ? 0.0 : hover[row].value(nowMs);
	}

	/**
	 * Gives the height of a portrait's hop.
	 *
	 * @param row the row
	 * @param nowMs the time in milliseconds
	 * @return from 0 up to 1 and back to 0 over one hop; the portrait moves up by that share of the hop height
	 */
	double hop(int row, long nowMs)
	{
		double t = animations.value(hopId(row), nowMs, 0.0);
		return Math.sin(Math.PI * Math.max(0.0, Math.min(1.0, t)));
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return where the selection cursor is, as a row number that glides between whole rows
	 */
	double cursor(long nowMs)
	{
		return cursor.value(nowMs);
	}

	/** @return the row the highlight is on, or -1 */
	int highlighted()
	{
		return hovered;
	}

	/** Forgets everything, for when the window closes so nothing replays on the next open. */
	void reset()
	{
		last = Collections.emptyList();
		ghosts.clear();
		hovered = -1;
		for (int i = 0; i < PartyLayout.SLOTS; i++)
		{
			hp[i] = null;
			xp[i] = null;
			hover[i] = null;
		}

		cursor.snap(0);
	}

	private static String xId(int row)
	{
		return "rowx:" + row;
	}

	private static String yId(int row)
	{
		return "rowy:" + row;
	}

	private static String hopId(int row)
	{
		return "hop:" + row;
	}
}
