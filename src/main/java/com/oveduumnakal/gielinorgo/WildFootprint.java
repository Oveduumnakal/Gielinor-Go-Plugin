/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Decides which ground a wild creature counts as standing on, for the mouse. Pure arithmetic, no game client. A
 * body one tile wide covers the whole tile it is nearest to, however far through a step it is, so every part of that
 * tile gives its menu entries.
 */
final class WildFootprint
{
	/** Scene units in one tile. */
	private static final int TILE = 128;

	private WildFootprint()
	{
	}

	/**
	 * Works out the middle of the ground a body covers.
	 *
	 * @param x the body's world x, in tiles; a whole number is the middle of a tile
	 * @param y the body's world y, in tiles
	 * @param tiles how many tiles wide the body is
	 * @param baseX the world x of the scene's west edge
	 * @param baseY the world y of the scene's south edge
	 * @return the scene x and y of the middle: the middle of the nearest tile for an odd width, the body's own
	 *     place for an even one
	 */
	static int[] centre(double x, double y, int tiles, int baseX, int baseY)
	{
		double sceneX = x - baseX;
		double sceneY = y - baseY;
		if (tiles % 2 == 1)
		{
			sceneX = Math.rint(sceneX);
			sceneY = Math.rint(sceneY);
		}

		return new int[] {(int) Math.round((sceneX + 0.5) * TILE), (int) Math.round((sceneY + 0.5) * TILE)};
	}
}
