/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Picks which of the jars a player owns are worth throwing at one creature: the best odds, and at most one more that
 * is nearly as good, the cheaper first. Every comparison uses the percent the menu shows, so a tie on screen is a tie
 * here. Holds no RuneLite types.
 */
final class JarSuggest
{
	/** How many points under the best shown percent a jar may be and still be suggested. */
	static final int NEAR_BEST_POINTS = 5;

	/** The most jars suggested. */
	static final int MAX_SUGGESTED = 2;

	private static final int NON_MATCHING_SPECIALTY_RANK = 100;

	/** One owned jar and how well it does against the creature in question. */
	static final class JarChoice
	{
		private final JarType jar;
		private final int owned;
		private final double chance;
		private final boolean matching;

		/**
		 * @param jar the jar
		 * @param owned how many the player has
		 * @param chance the catch chance with this jar, from 0 to 1
		 * @param matching whether this is a specialty jar whose trait the creature has
		 */
		JarChoice(JarType jar, int owned, double chance, boolean matching)
		{
			this.jar = jar;
			this.owned = owned;
			this.chance = chance;
			this.matching = matching;
		}

		/** @return the jar */
		JarType jar()
		{
			return jar;
		}

		/** @return how many the player has */
		int owned()
		{
			return owned;
		}

		/** @return the catch chance, from 0 to 1 */
		double chance()
		{
			return chance;
		}

		/** @return whether this is a specialty jar whose trait the creature has */
		boolean matching()
		{
			return matching;
		}

		/** @return the chance as the whole percent the menu shows */
		int pct()
		{
			return (int) Math.round(chance * 100);
		}

		private int spendRank()
		{
			if (matching)
				return 0;

			return jar.specialty() ? NON_MATCHING_SPECIALTY_RANK : 1 + jar.ordinal();
		}
	}

	/** The owned jars split into the suggested ones and the rest, each best first. */
	static final class Split
	{
		private final List<JarChoice> suggested;
		private final List<JarChoice> others;

		private Split(List<JarChoice> suggested, List<JarChoice> others)
		{
			this.suggested = Collections.unmodifiableList(suggested);
			this.others = Collections.unmodifiableList(others);
		}

		/** @return the jars worth throwing, best first */
		List<JarChoice> suggested()
		{
			return suggested;
		}

		/** @return every other owned jar, in the order they would be tried */
		List<JarChoice> others()
		{
			return others;
		}
	}

	private JarSuggest()
	{
	}

	/**
	 * Splits the owned jars. Sorted by shown percent, best first; a tie goes to a matching specialty jar, then the
	 * cheaper tier jars (Bronze to Dragon), then a specialty jar that does not match. The suggested jars are the
	 * first {@link #MAX_SUGGESTED} within {@link #NEAR_BEST_POINTS} of the best.
	 *
	 * @param owned the jars the player has; not changed
	 * @return the two groups; both empty for no jars
	 */
	static Split split(List<JarChoice> owned)
	{
		List<JarChoice> sorted = new ArrayList<>(owned);
		sorted.sort(Comparator.comparingInt((JarChoice c) -> -c.pct())
			.thenComparingInt(JarChoice::spendRank)
			.thenComparingInt(c -> c.jar.ordinal()));
		List<JarChoice> suggested = new ArrayList<>();
		List<JarChoice> others = new ArrayList<>();
		for (JarChoice choice : sorted)
		{
			boolean near = choice.pct() >= sorted.get(0).pct() - NEAR_BEST_POINTS;
			if (near && suggested.size() < MAX_SUGGESTED)
				suggested.add(choice);
			else
				others.add(choice);
		}

		return new Split(suggested, others);
	}
}
