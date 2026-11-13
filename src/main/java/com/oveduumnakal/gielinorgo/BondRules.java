/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The bond between a creature and its trainer: how bond points turn into hearts, and what each heart gives. A bond
 * is cosmetic and gives a little creature XP; it never changes a battle. No RuneLite types, so it can be tested alone.
 */
final class BondRules
{
	/** Most hearts a creature can have. */
	static final int MAX_HEARTS = 5;

	/** Tiles walked with the lead following for one bond point. */
	static final int TILES_PER_POINT = 100;

	/** Bond points for a battle the lead won. */
	static final int WIN_POINTS = 5;

	/** Bond points for a City Champion or Elite win with the lead on the team. */
	static final int CHAMPION_POINTS = 20;

	/** Hearts from which the follower's name tag shows a small heart. */
	static final int TAG_HEARTS = 1;

	/** Bond points needed for hearts 1 to 5. */
	private static final int[] POINTS_FOR_HEART = {100, 400, 1200, 3000, 6000};

	/** Percent more creature XP at 0 to 5 hearts. */
	private static final int[] XP_PERCENT = {0, 0, 0, 2, 4, 5};

	private BondRules()
	{
	}

	/**
	 * Turns bond points into hearts.
	 *
	 * @param points the bond points, 0 or more
	 * @return the hearts, 0 to {@value #MAX_HEARTS}
	 */
	static int hearts(int points)
	{
		int hearts = 0;
		while (hearts < MAX_HEARTS && points >= POINTS_FOR_HEART[hearts])
			hearts++;

		return hearts;
	}

	/**
	 * Tells how many points a heart needs.
	 *
	 * @param heart the heart, 1 to {@value #MAX_HEARTS}
	 * @return the bond points that earn it
	 */
	static int pointsFor(int heart)
	{
		return POINTS_FOR_HEART[Math.max(1, Math.min(MAX_HEARTS, heart)) - 1];
	}

	/**
	 * Tells how much more creature XP a creature earns.
	 *
	 * @param hearts the creature's hearts
	 * @return the extra XP as a percentage: 0 below 3 hearts, then 2, 4 and 5
	 */
	static int xpBonus(int hearts)
	{
		return XP_PERCENT[Math.max(0, Math.min(MAX_HEARTS, hearts))];
	}

	/**
	 * Adds the bonus to an amount of XP.
	 *
	 * @param xp the XP a creature earned
	 * @param hearts the creature's hearts
	 * @return the XP plus its bond bonus, rounded down
	 */
	static long withBonus(long xp, int hearts)
	{
		return xp + xp * xpBonus(hearts) / 100;
	}

	/**
	 * Words the tooltip line for a creature's bond.
	 *
	 * @param hearts the creature's hearts
	 * @return for example {@code Bond 3 of 5: +2% XP. Grows as your lead}
	 */
	static String describe(int hearts)
	{
		int bonus = xpBonus(hearts);
		return "Bond " + hearts + " of " + MAX_HEARTS + (bonus > 0 ? ": +" + bonus + "% XP" : "")
			+ ". Grows as your lead";
	}
}
