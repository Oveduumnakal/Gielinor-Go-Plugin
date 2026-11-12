/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/**
 * What the menu offers over a wild creature's body, and in which order. The first entry is the one a plain left
 * click does. Which action that is lives in one constant, {@link #LEFT_CLICK_CATCHES}. Holds no RuneLite types.
 */
final class WildMenu
{
	/**
	 * Whether a plain left click on a wild creature throws the selected jar. When false, a left click starts a
	 * battle instead. Either way both are in the right-click menu. The owner chose a left click on the creature on
	 * 2026-11-12; which action it does is the builder's default and is meant to be flipped here.
	 */
	static final boolean LEFT_CLICK_CATCHES = true;

	/** The words of a catch entry. */
	static final String CATCH = "Catch";

	/** The words of a battle entry. */
	static final String BATTLE = "Battle";

	/** One menu entry. */
	static final class Item
	{
		private final String option;
		private final JarType jar;

		private Item(String option, JarType jar)
		{
			this.option = option;
			this.jar = jar;
		}

		/** @return the words of the entry: {@link WildMenu#CATCH} or {@link WildMenu#BATTLE} */
		String option()
		{
			return option;
		}

		/** @return the jar a catch entry throws, or null for a battle entry or when the player has no jar */
		JarType jar()
		{
			return jar;
		}

		/** @return whether this is a catch entry */
		boolean isCatch()
		{
			return CATCH.equals(option);
		}
	}

	private WildMenu()
	{
	}

	/**
	 * Tells whether a menu entry's words are one of this plugin's wild creature entries.
	 *
	 * @param option the entry's words
	 * @return true for a catch or battle entry
	 */
	static boolean isOurs(String option)
	{
		return CATCH.equals(option) || BATTLE.equals(option);
	}

	/**
	 * Lists the entries for one wild creature, the left-click one first.
	 *
	 * @param standard the jar the player has selected, or null when they have none
	 * @param specialty the specialty jars that are good against this creature
	 * @param withBattle whether a battle may be started now
	 * @param leftClickCatches whether the catch entries come before the battle entry
	 * @return the entries, top of the menu first
	 */
	static List<Item> topFirst(JarType standard, List<JarType> specialty, boolean withBattle,
		boolean leftClickCatches)
	{
		List<Item> catches = new ArrayList<>();
		catches.add(new Item(CATCH, standard));
		for (JarType jar : specialty)
			catches.add(new Item(CATCH, jar));

		List<Item> items = new ArrayList<>();
		Item battle = withBattle ? new Item(BATTLE, null) : null;
		if (battle != null && !leftClickCatches)
			items.add(battle);

		items.addAll(catches);
		if (battle != null && leftClickCatches)
			items.add(battle);

		return items;
	}
}
