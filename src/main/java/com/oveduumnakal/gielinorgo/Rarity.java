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

	/** @return tiles the player walks to hatch an egg of this tier: 1,000 for Common, up to 4,000 for Legendary */
	int eggTiles()
	{
		int[] tiles = {1000, 1600, 2400, 3200, 4000};
		return tiles[ordinal()];
	}

	/**
	 * Gives how much this tier changes how often a monster turns wild: 1 for Common, 0.6 for Uncommon, 0.35 for Rare,
	 * 0.15 for Epic and 0.05 for Legendary. It multiplies the chance of one monster turning wild.
	 *
	 * @return the factor, from 1 down to 0.05
	 */
	double wildFactor()
	{
		double[] factors = {1.0, 0.6, 0.35, 0.15, 0.05};
		return factors[ordinal()];
	}

	/** @return the next tier up, or this one when it is already {@link #LEGENDARY} */
	Rarity next()
	{
		Rarity[] all = values();
		return all[Math.min(all.length - 1, ordinal() + 1)];
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
