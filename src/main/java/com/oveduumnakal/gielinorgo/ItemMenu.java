/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Picks the items the {@code Use} submenu of a creature's right-click menu offers: the owned ones that would help. */
final class ItemMenu
{
	private ItemMenu()
	{
	}

	/**
	 * Lists the owned items that would do something for a creature outside battle: the one rule behind the painted
	 * window's {@code Use} submenu.
	 *
	 * @param owned how many of each item the player owns; may be null or empty
	 * @param health the creature's share of full health as the window shows it
	 * @return the items to offer, in the order of {@link ItemType#values()}; empty when none would help
	 */
	static List<ItemType> usable(Map<ItemType, Integer> owned, double health)
	{
		List<ItemType> list = new ArrayList<>();
		if (owned == null)
			return list;

		for (ItemType type : ItemType.values())
		{
			Integer count = owned.get(type);
			if (count != null && count > 0 && ItemRules.appliesOutsideBattle(type, health))
				list.add(type);
		}

		return list;
	}
}
