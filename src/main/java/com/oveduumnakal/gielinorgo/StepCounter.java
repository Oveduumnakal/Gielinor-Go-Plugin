/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** Counts the tiles the player walks, ignoring teleports and plane changes. */
final class StepCounter
{
	/** Farthest the player can move in one game tick on foot (running). */
	static final int MAX_STEP = 2;

	private boolean known;
	private int lastX;
	private int lastY;
	private int lastPlane;

	/**
	 * Records where the player is this tick.
	 *
	 * @param x world x
	 * @param y world y
	 * @param plane world plane
	 * @return the tiles walked since the last tick: 0 for a first reading, standing still, or a teleport
	 */
	int advance(int x, int y, int plane)
	{
		int distance = Math.max(Math.abs(x - lastX), Math.abs(y - lastY));
		boolean walked = known && plane == lastPlane && distance <= MAX_STEP;
		known = true;
		lastX = x;
		lastY = y;
		lastPlane = plane;
		return walked ? distance : 0;
	}

	/** Forgets the last position, so the next reading counts no steps. */
	void reset()
	{
		known = false;
	}
}
