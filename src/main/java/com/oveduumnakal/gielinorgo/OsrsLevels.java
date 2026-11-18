/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The OSRS level table, used to count the levels gained while the plugin was not watching. Pure maths: level 99 is
 * the cap, there are no virtual levels.
 */
final class OsrsLevels
{
	/** Highest real level. */
	static final int MAX_LEVEL = 99;

	/** XP at which each level starts, indexed by level; index 0 is unused. */
	private static final int[] START = new int[MAX_LEVEL + 1];

	static
	{
		double points = 0;
		for (int level = 1; level <= MAX_LEVEL; level++)
		{
			START[level] = (int) Math.floor(points / 4);
			points += Math.floor(level + 300 * Math.pow(2, level / 7.0));
		}
	}

	private OsrsLevels()
	{
	}

	/**
	 * The real level for a skill's total XP.
	 *
	 * @param xp the skill's OSRS XP
	 * @return the level, 1 to 99
	 */
	static int levelForXp(int xp)
	{
		int level = 1;
		while (level < MAX_LEVEL && START[level + 1] <= xp)
			level++;

		return level;
	}
}
