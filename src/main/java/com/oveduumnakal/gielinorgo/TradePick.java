/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;
import java.util.Objects;

/**
 * What the player picked to give in the trade window: the place it was listed at and what it looked like there.
 * The list can change between the window opening and the offer reaching the game thread, so the place alone is not
 * trusted; {@link #matches(PanelState)} checks that the same creature or egg is still there.
 */
final class TradePick
{
	private final Holding from;
	private final int index;
	private final CreatureView creature;
	private final EggView egg;

	private TradePick(Holding from, int index, CreatureView creature, EggView egg)
	{
		this.from = from;
		this.index = index;
		this.creature = creature;
		this.egg = egg;
	}

	/**
	 * Picks a creature.
	 *
	 * @param from {@link Holding#TEAM} or {@link Holding#STORAGE}
	 * @param index its place in that list
	 * @param view the creature as the window showed it
	 * @return the pick
	 */
	static TradePick of(Holding from, int index, CreatureView view)
	{
		return new TradePick(from, index, view, null);
	}

	/**
	 * Picks an egg.
	 *
	 * @param index its place in the list of eggs
	 * @param view the egg as the window showed it
	 * @return the pick
	 */
	static TradePick of(int index, EggView view)
	{
		return new TradePick(Holding.EGG, index, null, view);
	}

	/** @return where the thing is kept */
	Holding from()
	{
		return from;
	}

	/** @return its place in that list */
	int index()
	{
		return index;
	}

	/**
	 * Checks that the same creature or egg is still at the picked place. A creature matches on name, nickname,
	 * level, XP progress and gilded look, not on health, which recovers by itself. An egg matches on rarity and on
	 * not having more tiles left than before, because walking wears them down.
	 *
	 * @param state what the player owns now
	 * @return true when the pick still points at what the player chose
	 */
	boolean matches(PanelState state)
	{
		if (egg != null)
		{
			List<EggView> eggs = state.eggs();
			if (index < 0 || index >= eggs.size())
				return false;

			EggView now = eggs.get(index);
			return now.rarity() == egg.rarity() && now.tilesLeft() <= egg.tilesLeft();
		}

		List<CreatureView> kept = from == Holding.TEAM ? state.team() : state.storage();
		if (index < 0 || index >= kept.size())
			return false;

		CreatureView now = kept.get(index);
		return now.name().equals(creature.name()) && Objects.equals(now.nickname(), creature.nickname())
			&& now.level() == creature.level()
			&& now.gilded() == creature.gilded() && Double.compare(now.xpProgress(), creature.xpProgress()) == 0;
	}
}
