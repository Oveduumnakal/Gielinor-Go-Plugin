/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * Where a wild creature was last drawn, kept as a place in the world rather than in the scene, so it stays right
 * when the scene recentres. The scene position is worked out each time it is wanted.
 */
final class LastPlace
{
	/** Local units in one tile. */
	static final int TILE = 128;

	private final double x;
	private final double y;
	private final int plane;
	private final int height;

	/**
	 * Remembers a place.
	 *
	 * @param x the world x of the ground under the body, in tiles, where a tile's middle is a whole number
	 * @param y the world y, likewise
	 * @param plane the plane it stands on
	 * @param height how tall the body is drawn, in local units
	 */
	LastPlace(double x, double y, int plane, int height)
	{
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.height = height;
	}

	/**
	 * Works out where a world place is in a scene.
	 *
	 * @param worldX the world x, in tiles, where a tile's middle is a whole number
	 * @param worldY the world y, likewise
	 * @param baseX the scene's base x, in tiles
	 * @param baseY the scene's base y, in tiles
	 * @param sizeX the scene's width, in tiles
	 * @param sizeY the scene's depth, in tiles
	 * @return the scene position in local units, {@code {x, y}}; null when it lies outside the scene
	 */
	static int[] sceneOf(double worldX, double worldY, int baseX, int baseY, int sizeX, int sizeY)
	{
		int sceneX = (int) Math.round((worldX - baseX + 0.5) * TILE);
		int sceneY = (int) Math.round((worldY - baseY + 0.5) * TILE);
		if (sceneX < 0 || sceneY < 0 || sceneX >= sizeX * TILE || sceneY >= sizeY * TILE)
			return null;

		return new int[]{sceneX, sceneY};
	}

	/**
	 * Gives the place as a spot in the scene that is loaded now.
	 *
	 * @param view the world view, or null
	 * @return the spot; null when there is no view or the place lies outside the scene
	 */
	StageAnchors.Spot in(WorldView view)
	{
		if (view == null)
			return null;

		int[] scene = sceneOf(x, y, view.getBaseX(), view.getBaseY(), view.getSizeX(), view.getSizeY());
		return scene == null ? null : new StageAnchors.Spot(new LocalPoint(scene[0], scene[1], view), plane, height);
	}
}
