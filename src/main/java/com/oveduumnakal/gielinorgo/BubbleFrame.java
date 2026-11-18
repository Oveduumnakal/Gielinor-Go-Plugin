/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What to paint right now: the discs, back to front, and at most one card. It is immutable and holds only plain
 * numbers, so the painter needs no other state, and it works out the {@link BubbleHits} the mouse listener will use
 * from the same numbers, so what is clickable is exactly what was painted. Holds no RuneLite types.
 */
final class BubbleFrame
{
	/** Where the card sits beside the bubbles, and so which edge it grows from. */
	enum Side
	{
		/** To the right of the bubbles; grows from its left edge. */
		RIGHT,

		/** To the left of the bubbles; grows from its right edge. */
		LEFT,

		/** Above the bubbles; grows from its bottom edge. */
		ABOVE
	}

	/** Nothing to paint and nothing to click. */
	static final BubbleFrame NONE = new BubbleFrame(Collections.emptyList(), null, 0, false);

	/**
	 * One round bubble. The position, scale and alpha are final: the pop, the hover growth and the lift of a hovered
	 * bubble are already in them, so the painter draws exactly what it is given. Immutable.
	 */
	static final class Disc
	{
		private final BubbleKind kind;
		private final double x;
		private final double y;
		private final double radius;
		private final double scale;
		private final double alpha;
		private final double hover;
		private final JarType jar;
		private final int count;
		private final boolean disabled;
		private final boolean selected;
		private final double pulse;
		private final boolean clickable;

		/**
		 * @param kind {@link BubbleKind#JAR}, {@link BubbleKind#BATTLE}, {@link BubbleKind#INFO} or
		 *        {@link BubbleKind#CLUSTER_JAR}
		 * @param x the centre x on the canvas
		 * @param y the centre y on the canvas
		 * @param radius the radius at scale 1: 15 for a main bubble, 12 for a cluster jar
		 * @param scale the size now, a multiple of the radius, hover growth included
		 * @param alpha how opaque, from 0 to 1
		 * @param hover how hovered, from 0 to 1, for the white ring
		 * @param jar the jar a jar bubble shows, or null (also for the Catch bubble of a player with no jar)
		 * @param count how many of the jar the player owns, for the badge; 0 shows no badge
		 * @param disabled whether it is greyed: no jar, or Battle not ready; it stays clickable
		 * @param selected whether it is the jar a Catch throws, for the ring on a cluster jar
		 * @param pulse the pulse of the knocked-out ring, from 0 to 1; 0 when still
		 * @param clickable whether a click on it counts yet; false while it is still popping out or leaving
		 */
		Disc(BubbleKind kind, double x, double y, double radius, double scale, double alpha, double hover,
			JarType jar, int count, boolean disabled, boolean selected, double pulse, boolean clickable)
		{
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.radius = radius;
			this.scale = scale;
			this.alpha = alpha;
			this.hover = hover;
			this.jar = jar;
			this.count = count;
			this.disabled = disabled;
			this.selected = selected;
			this.pulse = pulse;
			this.clickable = clickable;
		}

		/** @return the kind */
		BubbleKind kind()
		{
			return kind;
		}

		/** @return the centre x */
		double x()
		{
			return x;
		}

		/** @return the centre y */
		double y()
		{
			return y;
		}

		/** @return the radius at scale 1 */
		double radius()
		{
			return radius;
		}

		/** @return the size now as a multiple of {@link #radius()}; paint at {@code radius() * scale()} */
		double scale()
		{
			return scale;
		}

		/** @return how opaque it is, from 0 to 1 */
		double alpha()
		{
			return alpha;
		}

		/** @return how hovered it is, from 0 to 1 */
		double hover()
		{
			return hover;
		}

		/** @return the jar shown, or null */
		JarType jar()
		{
			return jar;
		}

		/** @return how many of the jar the player owns; 0 for no badge */
		int count()
		{
			return count;
		}

		/** @return whether it is greyed */
		boolean disabled()
		{
			return disabled;
		}

		/** @return whether it is the jar a Catch throws */
		boolean selected()
		{
			return selected;
		}

		/** @return the pulse of the knocked-out ring, from 0 to 1 */
		double pulse()
		{
			return pulse;
		}

		/** @return whether a click on it counts */
		boolean clickable()
		{
			return clickable;
		}

		/** @return the painted radius, {@code radius() * scale()} */
		double painted()
		{
			return radius * scale;
		}
	}

	/** The abbreviated card. The rectangle is its size at scale 1; the painter scales it about {@link #side()}. */
	static final class Card
	{
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final WildCardContent content;
		private final boolean pinned;
		private final double scale;
		private final double alpha;
		private final Side side;
		private final boolean clickable;

		/**
		 * @param bounds the card's rectangle at scale 1; copied
		 * @param content the words
		 * @param pinned whether the player pinned it, which draws a focus ring
		 * @param scale the size now, from 0.85 to 1
		 * @param alpha how opaque, from 0 to 1
		 * @param side the side of the bubbles it is on, which sets the edge it grows from
		 * @param clickable whether a click on it counts yet
		 */
		Card(Rectangle bounds, WildCardContent content, boolean pinned, double scale, double alpha, Side side,
			boolean clickable)
		{
			this.x = bounds.x;
			this.y = bounds.y;
			this.width = bounds.width;
			this.height = bounds.height;
			this.content = content;
			this.pinned = pinned;
			this.scale = scale;
			this.alpha = alpha;
			this.side = side;
			this.clickable = clickable;
		}

		/** @return a new rectangle with the card's position and size at scale 1 */
		Rectangle bounds()
		{
			return new Rectangle(x, y, width, height);
		}

		/** @return the words */
		WildCardContent content()
		{
			return content;
		}

		/** @return whether the card is pinned */
		boolean pinned()
		{
			return pinned;
		}

		/** @return the size now, from 0.85 to 1 */
		double scale()
		{
			return scale;
		}

		/** @return how opaque it is, from 0 to 1 */
		double alpha()
		{
			return alpha;
		}

		/** @return the side of the bubbles the card is on */
		Side side()
		{
			return side;
		}

		/** @return whether a click on it counts */
		boolean clickable()
		{
			return clickable;
		}
	}

	private final List<Disc> discs;
	private final Card card;
	private final BubbleHits hits;

	/**
	 * @param discs the discs, painted first to last, so the last is on top; copied. Several groups may be in one list
	 *        when one is leaving, with their discs not clickable
	 * @param card the card, painted over the discs, or null
	 * @param generation the generation of the live group, for the hits
	 * @param modal whether a click elsewhere should close something, for the hits
	 */
	BubbleFrame(List<Disc> discs, Card card, int generation, boolean modal)
	{
		this.discs = Collections.unmodifiableList(new ArrayList<>(discs));
		this.card = card;
		List<BubbleHits.Hit> targets = new ArrayList<>();
		if (card != null && card.clickable())
		{
			Rectangle box = card.bounds();
			targets.add(BubbleHits.Hit.box(BubbleKind.CARD, box.x, box.y, box.width, box.height));
		}

		for (int i = this.discs.size() - 1; i >= 0; i--)
		{
			Disc disc = this.discs.get(i);
			if (disc.clickable())
				targets.add(BubbleHits.Hit.disc(disc.kind(), disc.jar(), disc.x(), disc.y(), disc.painted()));
		}

		this.hits = targets.isEmpty() && !modal ? BubbleHits.EMPTY : new BubbleHits(generation, modal, targets);
	}

	/** @return the discs, first painted first; the last is on top. Never changes */
	List<Disc> discs()
	{
		return discs;
	}

	/** @return the card to paint over the discs, or null for none */
	Card card()
	{
		return card;
	}

	/** @return the click targets of exactly what this frame paints: card, then discs from the top down */
	BubbleHits hits()
	{
		return hits;
	}
}
