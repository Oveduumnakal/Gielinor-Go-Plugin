/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/** Builds the {@code Use} submenu that a creature's right-click menu offers: the owned items that would help it. */
final class ItemMenu
{
	private ItemMenu()
	{
	}

	/**
	 * Lists the owned items that would do something for a creature outside battle: the one rule behind the side
	 * panel's {@code Use} submenu and the painted window's.
	 *
	 * @param owned how many of each item the player owns; may be null or empty
	 * @param health the creature's share of full health as the panel shows it
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

	/**
	 * Adds a {@code Use} submenu to a menu, when at least one owned item would do something for the creature.
	 *
	 * @param menu the menu to add to
	 * @param owned how many of each item the player owns; may be null or empty
	 * @param health the creature's share of full health as the panel shows it
	 * @param use called with the item the player clicks
	 * @return true when the submenu was added
	 */
	static boolean addTo(JPopupMenu menu, Map<ItemType, Integer> owned, double health, Consumer<ItemType> use)
	{
		if (owned == null)
			return false;

		JMenu submenu = new JMenu("Use");
		for (ItemType type : usable(owned, health))
		{
			JMenuItem item = PanelUi.menuItem(type.label() + "  x" + owned.get(type), () -> use.accept(type));
			item.setIcon(ItemIcons.swing(type));
			submenu.add(item);
		}

		if (submenu.getItemCount() == 0)
			return false;

		menu.add(submenu);
		return true;
	}
}
