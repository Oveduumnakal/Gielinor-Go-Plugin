/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Objects;

/** One carried egg as the side panel shows it. Immutable, so it can cross from the game thread to the UI. */
final class EggView
{
	private final Rarity rarity;
	private final int tilesLeft;
	private final int tilesTotal;

	EggView(Rarity rarity, int tilesLeft, int tilesTotal)
	{
		this.rarity = rarity;
		this.tilesLeft = tilesLeft;
		this.tilesTotal = tilesTotal;
	}

	/** @return the rarity of the creature inside */
	Rarity rarity()
	{
		return rarity;
	}

	/** @return tiles still to walk before it hatches; 0 means it is ready and waiting for room */
	int tilesLeft()
	{
		return tilesLeft;
	}

	/** @return tiles it needed when it was found */
	int tilesTotal()
	{
		return tilesTotal;
	}

	/** @return how far along it is, from 0 to 1 */
	double progress()
	{
		if (tilesTotal <= 0)
			return 1.0;

		return Math.max(0.0, Math.min(1.0, 1.0 - tilesLeft / (double) tilesTotal));
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof EggView))
			return false;

		EggView that = (EggView) other;
		return rarity == that.rarity && tilesLeft == that.tilesLeft && tilesTotal == that.tilesTotal;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(rarity, tilesLeft, tilesTotal);
	}
}
