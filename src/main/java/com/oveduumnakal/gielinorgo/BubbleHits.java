/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What can be clicked on the canvas right now, as the last painted frame left it. The overlay builds one after every
 * paint and shares it with the mouse listener on another thread, so it is immutable all the way down: every list is
 * copied and every shape is plain numbers. Each target is grown by {@link #HIT_SLOP} pixels so a click on the edge of
 * a bubble that has just moved still lands. Targets are kept topmost first. Holds no RuneLite types.
 */
final class BubbleHits
{
	/** How many pixels every target is grown by when a point is tested. */
	static final int HIT_SLOP = 2;

	/** Nothing to click; the generation is 0 and nothing is modal. */
	static final BubbleHits EMPTY = new BubbleHits(0, false, Collections.emptyList());

	/** One thing that can be clicked: a round bubble or the rectangle of the card. Immutable. */
	static final class Hit
	{
		private final BubbleKind kind;
		private final JarType jar;
		private final boolean round;
		private final double x;
		private final double y;
		private final double width;
		private final double height;

		private Hit(BubbleKind kind, JarType jar, boolean round, double x, double y, double width, double height)
		{
			this.kind = kind;
			this.jar = jar;
			this.round = round;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		/**
		 * @param kind the bubble's kind
		 * @param jar the jar the bubble shows, or null
		 * @param cx the centre x
		 * @param cy the centre y
		 * @param radius the painted radius, without the slop
		 * @return a round target
		 */
		static Hit disc(BubbleKind kind, JarType jar, double cx, double cy, double radius)
		{
			return new Hit(kind, jar, true, cx, cy, radius, radius);
		}

		/**
		 * @param kind the kind, normally {@link BubbleKind#CARD}
		 * @param x the left edge
		 * @param y the top edge
		 * @param width the width
		 * @param height the height
		 * @return a rectangular target
		 */
		static Hit box(BubbleKind kind, double x, double y, double width, double height)
		{
			return new Hit(kind, null, false, x, y, width, height);
		}

		/** @return what was hit */
		BubbleKind kind()
		{
			return kind;
		}

		/** @return the jar of a jar bubble, else null */
		JarType jar()
		{
			return jar;
		}

		/**
		 * @param px the point's x
		 * @param py the point's y
		 * @return whether the point is on the target or within {@link #HIT_SLOP} of it
		 */
		boolean contains(double px, double py)
		{
			if (round)
			{
				double reach = width + HIT_SLOP;
				double dx = px - x;
				double dy = py - y;
				return dx * dx + dy * dy <= reach * reach;
			}

			return px >= x - HIT_SLOP && px <= x + width + HIT_SLOP && py >= y - HIT_SLOP
				&& py <= y + height + HIT_SLOP;
		}
	}

	private final int generation;
	private final boolean modal;
	private final List<Hit> targets;

	/**
	 * @param generation the generation of the group the targets belong to
	 * @param modal whether a click elsewhere should close something: a cluster is open or the card is pinned
	 * @param topmostFirst the targets, the one painted last first; copied
	 */
	BubbleHits(int generation, boolean modal, List<Hit> topmostFirst)
	{
		this.generation = generation;
		this.modal = modal;
		this.targets = Collections.unmodifiableList(new ArrayList<>(topmostFirst));
	}

	/**
	 * Finds what is under a point.
	 *
	 * @param x the point's x
	 * @param y the point's y
	 * @return the topmost target there (card, then cluster, then main bubbles), or null when there is none
	 */
	Hit at(double x, double y)
	{
		for (Hit hit : targets)
		{
			if (hit.contains(x, y))
				return hit;
		}

		return null;
	}

	/**
	 * @param x the point's x
	 * @param y the point's y
	 * @return whether any target is under the point
	 */
	boolean covers(double x, double y)
	{
		return at(x, y) != null;
	}

	/** @return the generation of the group these targets belong to; a press carries it back to catch a stale one */
	int generation()
	{
		return generation;
	}

	/** @return whether a click elsewhere should close the cluster or unpin the card */
	boolean modal()
	{
		return modal;
	}

	/** @return how many targets there are */
	int size()
	{
		return targets.size();
	}
}
