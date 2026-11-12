/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Whether a creature can step from one tile to a neighbouring one, worked out from the game's collision flags the
 * way the game itself does: the tile stepped onto must be free, a wall on the edge between the two tiles blocks the
 * step, and a diagonal step needs both of the two straight routes round its corner to be open. The flag values are
 * the game's own ({@code CollisionDataFlag}); this class holds no RuneLite types, so it can be tested on small
 * hand-drawn maps.
 */
final class StepRules
{
	/** The flag for a wall on the north-west corner of a tile. */
	static final int NORTH_WEST = 0x1;

	/** The flag for a wall on the north edge of a tile. */
	static final int NORTH = 0x2;

	/** The flag for a wall on the north-east corner of a tile. */
	static final int NORTH_EAST = 0x4;

	/** The flag for a wall on the east edge of a tile. */
	static final int EAST = 0x8;

	/** The flag for a wall on the south-east corner of a tile. */
	static final int SOUTH_EAST = 0x10;

	/** The flag for a wall on the south edge of a tile. */
	static final int SOUTH = 0x20;

	/** The flag for a wall on the south-west corner of a tile. */
	static final int SOUTH_WEST = 0x40;

	/** The flag for a wall on the west edge of a tile. */
	static final int WEST = 0x80;

	/** The flags that make a whole tile impassable: an object, a floor decoration or the floor itself. */
	static final int FULL = 0x100 | 0x40000 | 0x200000;

	/** Reads the collision flags of one tile. */
	interface Flags
	{
		/**
		 * Reads one tile.
		 *
		 * @param x the tile's x
		 * @param y the tile's y
		 * @return its flags; a tile outside the known map must read as {@link StepRules#FULL}
		 */
		int at(int x, int y);
	}

	private StepRules()
	{
	}

	/**
	 * Tells whether a tile can be stood on at all.
	 *
	 * @param flags the map
	 * @param x the tile's x
	 * @param y the tile's y
	 * @return true when nothing fills the tile
	 */
	static boolean standable(Flags flags, int x, int y)
	{
		return (flags.at(x, y) & FULL) == 0;
	}

	/**
	 * Tells whether one step to a neighbouring tile is open.
	 *
	 * @param flags the map
	 * @param fromX the tile stepped from, x
	 * @param fromY the tile stepped from, y
	 * @param toX the tile stepped onto, x; at most one away from {@code fromX}
	 * @param toY the tile stepped onto, y; at most one away from {@code fromY}
	 * @return true when the step is open; false for a step that is not to a neighbour
	 */
	static boolean canStep(Flags flags, int fromX, int fromY, int toX, int toY)
	{
		int dx = toX - fromX;
		int dy = toY - fromY;
		if ((dx == 0 && dy == 0) || Math.abs(dx) > 1 || Math.abs(dy) > 1)
			return false;

		if (!standable(flags, toX, toY))
			return false;

		if (dx == 0 || dy == 0)
			return straight(flags, fromX, fromY, dx, dy);

		return corner(flags, fromX, fromY, dx, dy);
	}

	private static boolean straight(Flags flags, int fromX, int fromY, int dx, int dy)
	{
		int from = flags.at(fromX, fromY);
		int to = flags.at(fromX + dx, fromY + dy);
		if (dx > 0)
			return (from & EAST) == 0 && (to & WEST) == 0;

		if (dx < 0)
			return (from & WEST) == 0 && (to & EAST) == 0;

		if (dy > 0)
			return (from & NORTH) == 0 && (to & SOUTH) == 0;

		return (from & SOUTH) == 0 && (to & NORTH) == 0;
	}

	private static boolean corner(Flags flags, int fromX, int fromY, int dx, int dy)
	{
		int from = flags.at(fromX, fromY);
		int to = flags.at(fromX + dx, fromY + dy);
		int fromCorner = dx > 0 ? (dy > 0 ? NORTH_EAST : SOUTH_EAST) : (dy > 0 ? NORTH_WEST : SOUTH_WEST);
		int toCorner = dx > 0 ? (dy > 0 ? SOUTH_WEST : NORTH_WEST) : (dy > 0 ? SOUTH_EAST : NORTH_EAST);
		if ((from & fromCorner) != 0 || (to & toCorner) != 0)
			return false;

		return canStep(flags, fromX, fromY, fromX + dx, fromY)
			&& canStep(flags, fromX + dx, fromY, fromX + dx, fromY + dy)
			&& canStep(flags, fromX, fromY, fromX, fromY + dy)
			&& canStep(flags, fromX, fromY + dy, fromX + dx, fromY + dy);
	}
}
