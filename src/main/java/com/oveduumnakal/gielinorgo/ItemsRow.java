/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The Items line under the jar drop-down: one picture and count for each healing item, greyed at zero, each with a
 * tooltip. The four cells share the width of the panel equally, so the line fits in 225 pixels.
 */
final class ItemsRow extends JPanel
{
	private final Map<ItemType, JLabel> cells = new EnumMap<>(ItemType.class);

	ItemsRow()
	{
		super(new GridLayout(1, ItemType.values().length, 2, 0));
		setOpaque(false);
		for (ItemType type : ItemType.values())
		{
			JLabel cell = PanelUi.label("", FontManager.getRunescapeSmallFont(), Color.WHITE);
			cell.setHorizontalAlignment(SwingConstants.LEFT);
			cell.setIconTextGap(3);
			cells.put(type, cell);
			add(cell);
		}

		update(new EnumMap<>(ItemType.class));
	}

	/**
	 * Shows the counts.
	 *
	 * @param owned how many of each item the player owns; an item that is missing counts as none
	 */
	void update(Map<ItemType, Integer> owned)
	{
		for (ItemType type : ItemType.values())
		{
			Integer found = owned.get(type);
			int count = found == null ? 0 : found;
			JLabel cell = cells.get(type);
			cell.setText(String.valueOf(count));
			cell.setForeground(count > 0 ? Color.WHITE : ColorScheme.MEDIUM_GRAY_COLOR);
			cell.setIcon(count > 0 ? ItemIcons.swing(type) : ItemIcons.swingGrey(type));
			cell.setToolTipText(tooltip(type, count));
		}
	}

	/**
	 * Words what an item does and where it is found.
	 *
	 * @param type the item
	 * @param count how many the player owns
	 * @return the tooltip, as HTML
	 */
	static String tooltip(ItemType type, int count)
	{
		return "<html>" + type.label() + "  x" + count + "<br>" + effect(type) + "<br>" + found(type) + "</html>";
	}

	private static String effect(ItemType type)
	{
		if (type.revives())
			return "Lifts a creature that is too tired to battle to " + Math.round(ItemRules.REVIVE_TO * 100)
				+ "% health";

		if (type.cures())
			return "Cures poison, venom and burn in battle";

		return "Restores " + Math.round(type.heal() * 100) + "% health";
	}

	private static String found(ItemType type)
	{
		switch (type)
		{
			case SMALL_SALVE:
				return "Found training Herblore and Cooking, and after battles";
			case LARGE_SALVE:
				return "Found after battles and beating a City Champion";
			case REVIVE:
				return "Found after battles and beating a City Champion";
			default:
				return "Found training Herblore";
		}
	}
}
