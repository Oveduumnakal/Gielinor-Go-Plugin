/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** The numbers that decide when a healing item drops and when one can be used. No game types, so tests are plain. */
final class ItemRules
{
	/** OSRS XP in one drop that makes up one whole chance of finding an item. */
	static final double XP_PER_ITEM = 30_000;

	/** The most likely an XP drop gets to turn up one item. */
	static final double MAX_XP_CHANCE = 0.3;

	/** Chance that a battle win drops a Small salve. */
	static final double WIN_SMALL_SALVE_CHANCE = 0.10;

	/** Chance that a battle win drops a Large salve. */
	static final double WIN_LARGE_SALVE_CHANCE = 0.03;

	/** Chance that a battle win drops a Revive draught. */
	static final double WIN_REVIVE_CHANCE = 0.02;

	/** Large salves the first win against a City Champion pays. */
	static final int CHAMPION_LARGE_SALVES = 2;

	/** Revive draughts the first win against a City Champion pays. */
	static final int CHAMPION_REVIVES = 1;

	/** The share of full health a Revive draught lifts a creature to. */
	static final double REVIVE_TO = 0.5;

	private ItemRules()
	{
	}

	/**
	 * Chance that one XP drop turns up one item of the skill. It is a separate roll from the specialty jar.
	 *
	 * @param xpGained OSRS XP in the drop
	 * @return the chance, from 0 to {@link #MAX_XP_CHANCE}
	 */
	static double xpChance(int xpGained)
	{
		return Math.max(0.0, Math.min(MAX_XP_CHANCE, xpGained / XP_PER_ITEM));
	}

	/**
	 * Chance that a battle win drops one kind of item.
	 *
	 * @param type the item
	 * @return the chance; 0 for an item battle wins never drop
	 */
	static double winChance(ItemType type)
	{
		switch (type)
		{
			case SMALL_SALVE:
				return WIN_SMALL_SALVE_CHANCE;
			case LARGE_SALVE:
				return WIN_LARGE_SALVE_CHANCE;
			case REVIVE:
				return WIN_REVIVE_CHANCE;
			default:
				return 0.0;
		}
	}

	/**
	 * Tells whether using an item on a creature outside battle would do something.
	 *
	 * @param type the item
	 * @param health the creature's share of full health right now, from 0 to 1
	 * @return true for a salve on a creature below full health, or a Revive draught on one too tired to battle
	 */
	static boolean appliesOutsideBattle(ItemType type, double health)
	{
		if (type.revives())
			return health < GoGame.MIN_BATTLE_HP;

		return type.heal() > 0 && health < 1.0;
	}

	/**
	 * Works out the health a creature has after an item.
	 *
	 * @param type the item
	 * @param health the creature's share of full health right now
	 * @return the new share, never lower than before
	 */
	static double healthAfter(ItemType type, double health)
	{
		if (type.revives())
			return Math.max(health, REVIVE_TO);

		return Math.min(1.0, health + type.heal());
	}
}
