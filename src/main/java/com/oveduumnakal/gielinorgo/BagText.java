/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The words of the Bag page: what each jar, item and egg does and where it is found. Holds no RuneLite types. */
final class BagText
{
	private BagText()
	{
	}

	/**
	 * Words what an item does.
	 *
	 * @param type the item
	 * @return one sentence
	 */
	static String effect(ItemType type)
	{
		if (type.revives())
			return "Lifts a creature that is too tired to battle to " + Math.round(ItemRules.REVIVE_TO * 100)
				+ "% health";

		if (type.cures())
			return "Cures poison, venom and burn in battle";

		return "Restores " + Math.round(type.heal() * 100) + "% health";
	}

	/**
	 * Words where an item is found.
	 *
	 * @param type the item
	 * @return one sentence
	 */
	static String found(ItemType type)
	{
		switch (type)
		{
			case SMALL_SALVE:
				return "Found training Herblore and Cooking, and after battles";
			case LARGE_SALVE:
			case REVIVE:
				return "Found after battles and beating a City Champion";
			default:
				return "Found training Herblore";
		}
	}

	/**
	 * @param type the item
	 * @param count how many the player owns
	 * @return the item's tooltip, three lines
	 */
	static String itemTip(ItemType type, int count)
	{
		return type.label() + "  x" + count + "\n" + effect(type) + "\n" + found(type);
	}

	/**
	 * @param type the jar
	 * @return the jar's tooltip: its catch chance and where it is found
	 */
	static String jarTip(JarType type)
	{
		if (!type.specialty())
			return "x" + String.format(Locale.ROOT, "%.1f", type.bonus()) + " catch chance\n"
				+ "Drops from battles at trainer level " + type.trainerLevel();

		List<String> skills = new ArrayList<>();
		for (String skill : type.skills())
			skills.add(skill.substring(0, 1) + skill.substring(1).toLowerCase(Locale.ROOT));

		return "x" + String.format(Locale.ROOT, "%.2f", type.traitBonus()) + " catch chance on "
			+ type.trait().label() + "\nx" + String.format(Locale.ROOT, "%.1f", type.bonus()) + " on anything else\n"
			+ "Found while training " + String.join(", ", skills);
	}

	/**
	 * @param egg the egg
	 * @return the egg's tooltip: its name and how far it has to go
	 */
	static String eggTip(EggView egg)
	{
		return egg.label() + (egg.tilesLeft() <= 0 ? ": ready, waiting for room" : ": " + egg.tilesLeft()
			+ " steps to go");
	}

	/**
	 * @param shown the title the player shows, or null
	 * @return {@code Grand Champion}, {@code Master Collector} or {@code Trainer}
	 */
	static String titleName(String shown)
	{
		if (GoGame.TITLE_GRAND_CHAMPION.equals(shown))
			return "Grand Champion";

		return GoGame.TITLE_MASTER_COLLECTOR.equals(shown) ? "Master Collector" : "Trainer";
	}

	/**
	 * @param progress the walk towards the next jar, in tiles
	 * @return the tooltip of the next-jar strip
	 */
	static String stepsTip(int progress)
	{
		return "Walk " + (GoGame.TILES_PER_JAR - progress) + " more tiles to find a Bronze jar";
	}

	/**
	 * @param state the panel state
	 * @return the jars the page shows: every tier jar, and a specialty jar the player owns or has selected
	 */
	static List<JarType> jarsShown(PanelState state)
	{
		List<JarType> shown = new ArrayList<>();
		for (JarType type : JarType.values())
		{
			Integer owned = state.jars().get(type);
			if (!type.specialty() || owned != null && owned > 0 || type == state.selectedJar())
				shown.add(type);
		}

		return shown;
	}
}
