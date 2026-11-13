/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * What releasing a creature pays and which creatures ask twice before they go. A release pays creature XP to the
 * lead and nothing else, so catching and releasing can never make jars. It has no RuneLite types.
 */
final class ReleaseRules
{
	/** XP every release pays before the level and rarity count. */
	static final long BASE_XP = 20;

	/** XP a release pays for each level of the creature. */
	static final long XP_PER_LEVEL = 4;

	private ReleaseRules()
	{
	}

	/**
	 * Works out the creature XP a release pays to the lead.
	 *
	 * @param level the released creature's level
	 * @param rarity its species' rarity
	 * @return {@code (20 + 4 * level)} times 1, 1.5, 2, 3 or 4 from Common to Legendary, rounded down
	 */
	static long xp(int level, Rarity rarity)
	{
		long base = BASE_XP + XP_PER_LEVEL * Math.max(1, level);
		switch (rarity)
		{
			case UNCOMMON:
				return base * 3 / 2;
			case RARE:
				return base * 2;
			case EPIC:
				return base * 3;
			case LEGENDARY:
				return base * 4;
			default:
				return base;
		}
	}

	/**
	 * Tells whether a creature is worth asking about a second time: gilded, rated Strong or Exceptional, or named.
	 *
	 * @param gilded whether it is gilded
	 * @param nicknamed whether the player gave it a nickname
	 * @param rating its stat rating
	 * @return true when the player should confirm again
	 */
	static boolean needsSecondConfirm(boolean gilded, boolean nicknamed, StatRolls.Rating rating)
	{
		return gilded || nicknamed || rating == StatRolls.Rating.STRONG || rating == StatRolls.Rating.EXCEPTIONAL;
	}

	/**
	 * Tells whether a listed creature is worth asking about a second time.
	 *
	 * @param view the creature as the panel shows it
	 * @return true when the player should confirm again
	 */
	static boolean needsSecondConfirm(CreatureView view)
	{
		return needsSecondConfirm(view.gilded(), view.nickname() != null, view.rolls().rating());
	}

	/**
	 * Works out the XP a listed creature pays when released.
	 *
	 * @param view the creature as the panel shows it
	 * @return its release XP
	 */
	static long xp(CreatureView view)
	{
		return xp(view.level(), view.rarity());
	}
}
