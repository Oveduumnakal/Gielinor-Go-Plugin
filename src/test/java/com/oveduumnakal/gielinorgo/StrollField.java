/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * A small hand-made world for the follower's stroll tests: open ground that can be cut to a square, split by a wall
 * line, or shrunk to one standable tile.
 */
final class StrollField implements TilePath.Ground
{
	private int size = Integer.MAX_VALUE;
	private int wallAfterX = Integer.MIN_VALUE;
	private int gapY = Integer.MIN_VALUE;
	private boolean hasWall;
	private boolean hasOnly;
	private int onlyX;
	private int onlyY;

	/** @return open ground without an edge */
	static StrollField open()
	{
		return new StrollField();
	}

	/**
	 * Makes a square of open tiles starting at 0, 0.
	 *
	 * @param tiles the side of the square
	 * @return the field
	 */
	static StrollField square(int tiles)
	{
		return new StrollField().within(tiles);
	}

	/**
	 * Makes open ground split by a wall.
	 *
	 * @param afterX the column west of the wall
	 * @param gap the row in which a step may cross, or {@link Integer#MIN_VALUE} for none
	 * @return the field
	 */
	static StrollField walled(int afterX, int gap)
	{
		return new StrollField().wall(afterX, gap);
	}

	/**
	 * Makes ground of which only one tile is free.
	 *
	 * @param x the free tile's x
	 * @param y the free tile's y
	 * @return the field
	 */
	static StrollField single(int x, int y)
	{
		return new StrollField().only(x, y);
	}

	/**
	 * Makes a square of tiles split by a wall with no gap.
	 *
	 * @param tiles the side of the square
	 * @param afterX the column west of the wall
	 * @return the field
	 */
	static StrollField walledSquare(int tiles, int afterX)
	{
		return new StrollField().within(tiles).wall(afterX, Integer.MIN_VALUE);
	}

	/**
	 * Cuts the ground to a square of tiles starting at 0, 0.
	 *
	 * @param tiles the side of the square
	 * @return this field
	 */
	StrollField within(int tiles)
	{
		size = tiles;
		return this;
	}

	/**
	 * Puts a wall between the column {@code afterX} and the next one, the whole way, except in one row.
	 *
	 * @param afterX the column west of the wall
	 * @param gap the row in which a step may cross, or {@link Integer#MIN_VALUE} for none
	 * @return this field
	 */
	StrollField wall(int afterX, int gap)
	{
		wallAfterX = afterX;
		gapY = gap;
		hasWall = true;
		return this;
	}

	/**
	 * Fills every tile but one.
	 *
	 * @param x the one tile's x
	 * @param y the one tile's y
	 * @return this field
	 */
	StrollField only(int x, int y)
	{
		hasOnly = true;
		onlyX = x;
		onlyY = y;
		return this;
	}

	/** @return a finder over this field */
	TilePath path()
	{
		return new TilePath(this);
	}

	@Override
	public boolean canStep(int fromX, int fromY, int toX, int toY)
	{
		if (!standable(toX, toY))
			return false;

		boolean crosses = hasWall && (fromX <= wallAfterX) != (toX <= wallAfterX);
		return !crosses || fromY == gapY && toY == gapY;
	}

	@Override
	public boolean standable(int x, int y)
	{
		if (hasOnly)
			return x == onlyX && y == onlyY;

		return x >= 0 && y >= 0 && x < size && y < size;
	}
}
