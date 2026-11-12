/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

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
		for (ItemType type : ItemType.values())
		{
			Integer count = owned.get(type);
			if (count == null || count <= 0 || !ItemRules.appliesOutsideBattle(type, health))
				continue;

			JMenuItem item = PanelUi.menuItem(type.label() + "  x" + count, () -> use.accept(type));
			item.setIcon(ItemIcons.swing(type));
			submenu.add(item);
		}

		if (submenu.getItemCount() == 0)
			return false;

		menu.add(submenu);
		return true;
	}
}
