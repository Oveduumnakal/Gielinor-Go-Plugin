/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.CollisionData;
import net.runelite.api.WorldView;

/**
 * Feeds the game's collision flags of one plane of the scene to {@link TilePath}. A tile outside the loaded scene
 * counts as full, so nothing is ever placed or sent there. A thin wrapper over client types; the rules live in
 * {@link StepRules}. Read on the game thread only.
 */
final class CollisionGround implements TilePath.Ground, StepRules.Flags
{
	private final int[][] flags;
	private final int baseX;
	private final int baseY;

	/**
	 * Reads one plane of a world view.
	 *
	 * @param view the world view
	 * @param plane the plane
	 */
	CollisionGround(WorldView view, int plane)
	{
		CollisionData[] maps = view.getCollisionMaps();
		boolean known = maps != null && plane >= 0 && plane < maps.length && maps[plane] != null;
		this.flags = known ? maps[plane].getFlags() : null;
		this.baseX = view.getBaseX();
		this.baseY = view.getBaseY();
	}

	@Override
	public int at(int x, int y)
	{
		int sceneX = x - baseX;
		int sceneY = y - baseY;
		if (flags == null || sceneX < 0 || sceneY < 0 || sceneX >= flags.length || sceneY >= flags[sceneX].length)
			return StepRules.FULL;

		return flags[sceneX][sceneY];
	}

	@Override
	public boolean canStep(int fromX, int fromY, int toX, int toY)
	{
		return StepRules.canStep(this, fromX, fromY, toX, toY);
	}

	@Override
	public boolean standable(int x, int y)
	{
		return StepRules.standable(this, x, y);
	}

	/**
	 * Builds a finder over this plane.
	 *
	 * @return the finder
	 */
	TilePath path()
	{
		return new TilePath(this);
	}
}
