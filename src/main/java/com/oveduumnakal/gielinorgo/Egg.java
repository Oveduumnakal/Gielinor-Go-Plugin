/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * An egg the player is carrying. It hatches into a random creature of its rarity, or into its fixed species for a
 * boss egg, once the player has walked far enough. Saved to the profile as JSON, so every field here is part of the
 * persisted schema.
 */
final class Egg
{
	/** The rarity of the creature inside, as a {@link Rarity#name()}. */
	String rarity;

	/** Tiles the player still has to walk before it hatches. */
	int tilesLeft;

	/** Tiles it needed when it was found, for the progress bar. */
	int tilesTotal;

	/** The species key of a boss egg, or null for an ordinary egg that hatches into a random creature. */
	String species;

	/** Used by Gson. */
	Egg()
	{
	}

	Egg(Rarity rarity)
	{
		this.rarity = rarity.name();
		this.tilesTotal = rarity.eggTiles();
		this.tilesLeft = tilesTotal;
	}

	/**
	 * Makes a boss egg.
	 *
	 * @param born the species the egg hatches into
	 */
	Egg(Species born)
	{
		this(born.rarity());
		this.species = born.key();
	}

	/** @return the rarity of the creature inside, Common when the saved value is not a known rarity */
	Rarity rarityTier()
	{
		for (Rarity tier : Rarity.values())
		{
			if (tier.name().equals(rarity))
				return tier;
		}

		return Rarity.COMMON;
	}
}
