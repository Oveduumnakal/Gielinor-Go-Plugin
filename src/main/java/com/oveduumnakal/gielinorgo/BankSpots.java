/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The places in view where a player can open a bank: bankers, bank booths and bank chests. The plugin adds a spot when
 * one spawns and removes it when it despawns, so asking whether a bank is near costs a look at a handful of spots, not
 * a scan of the scene. Holds no client types, so the rule is tested without a client.
 */
final class BankSpots
{
	/** How close the player must be to a bank, in tiles: the larger of the gap in x and the gap in y. */
	static final int NEAR_TILES = 10;

	/** Spots further than this from the player are forgotten: a despawn that was never seen cannot linger. */
	static final int FORGET_TILES = 110;

	private final Map<Object, int[]> spots = new HashMap<>();

	/**
	 * Remembers a bank spot. Adding the same owner again moves it.
	 *
	 * @param owner whatever the spot belongs to, such as the banker or the booth, compared by identity
	 * @param x world x
	 * @param y world y
	 * @param plane world plane
	 */
	void add(Object owner, int x, int y, int plane)
	{
		spots.put(owner, new int[] {x, y, plane});
	}

	/**
	 * Forgets a bank spot.
	 *
	 * @param owner what was passed to {@link #add}
	 */
	void remove(Object owner)
	{
		spots.remove(owner);
	}

	/** Forgets every spot, as when the scene is left. */
	void clear()
	{
		spots.clear();
	}

	/** @return how many spots are remembered */
	int size()
	{
		return spots.size();
	}

	/**
	 * Tells whether a bank is within {@link #NEAR_TILES} of a tile, on the same plane.
	 *
	 * @param x world x of the player
	 * @param y world y of the player
	 * @param plane world plane of the player
	 * @return true when a remembered spot is {@link #NEAR_TILES} tiles away or closer
	 */
	boolean near(int x, int y, int plane)
	{
		for (int[] spot : spots.values())
		{
			if (spot[2] == plane && Math.max(Math.abs(spot[0] - x), Math.abs(spot[1] - y)) <= NEAR_TILES)
				return true;
		}

		return false;
	}

	/**
	 * Forgets spots that are further than {@link #FORGET_TILES} from the player, so a despawn that was never
	 * reported cannot leave a bank behind for good.
	 *
	 * @param x world x of the player
	 * @param y world y of the player
	 */
	void forgetFar(int x, int y)
	{
		Iterator<int[]> each = spots.values().iterator();
		while (each.hasNext())
		{
			int[] spot = each.next();
			if (Math.max(Math.abs(spot[0] - x), Math.abs(spot[1] - y)) > FORGET_TILES)
				each.remove();
		}
	}
}
