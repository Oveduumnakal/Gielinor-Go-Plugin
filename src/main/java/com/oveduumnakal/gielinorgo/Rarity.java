/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

/** How hard a species is to find and catch, decided by its combat level. */
enum Rarity
{
	COMMON("Common", 1, new Color(0xC8C8C8)),
	UNCOMMON("Uncommon", 31, new Color(0x4CD964)),
	RARE("Rare", 81, new Color(0x4AA3FF)),
	EPIC("Epic", 151, new Color(0xB36BFF)),
	LEGENDARY("Legendary", 301, new Color(0xFF9F1A));

	private final String label;
	private final int minLevel;
	private final Color color;

	Rarity(String label, int minLevel, Color color)
	{
		this.label = label;
		this.minLevel = minLevel;
		this.color = color;
	}

	/** @return the name shown to the player */
	String label()
	{
		return label;
	}

	/** @return the lowest combat level in this tier */
	int minLevel()
	{
		return minLevel;
	}

	/** @return the colour used for this tier in the panel and overlays */
	Color color()
	{
		return color;
	}

	/** @return tiles the player walks to hatch an egg of this tier: 500 for Common, up to 2,000 for Legendary */
	int eggTiles()
	{
		int[] tiles = {500, 800, 1200, 1600, 2000};
		return tiles[ordinal()];
	}

	/**
	 * Finds the tier for a combat level.
	 *
	 * @param combatLevel the species' combat level
	 * @return the highest tier whose minimum the level reaches
	 */
	static Rarity forLevel(int combatLevel)
	{
		Rarity result = COMMON;
		for (Rarity rarity : values())
		{
			if (combatLevel >= rarity.minLevel)
				result = rarity;
		}

		return result;
	}
}
