/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Lays a standing model on the ground, for a creature that has no death animation. Works on plain vertex arrays so
 * it can be tested without the game. The game's Y axis points down, so a standing model has its head at a
 * negative Y.
 */
final class ModelTilt
{
	private ModelTilt()
	{
	}

	/**
	 * Tips a model a quarter turn onto its back or front, then lowers it so its lowest point rests on the ground
	 * (Y of 0) and nothing sinks through it. The arrays are changed in place, so they must belong to a copy of the
	 * model and never to one the game shares.
	 *
	 * @param y the vertices' Y values, 0 at the feet and negative upward
	 * @param z the vertices' Z values, the model's depth
	 * @param count how many vertices there are
	 */
	static void lieDown(float[] y, float[] z, int count)
	{
		int n = Math.min(count, Math.min(y.length, z.length));
		float lowest = -Float.MAX_VALUE;
		for (int i = 0; i < n; i++)
		{
			float tippedY = -z[i];
			z[i] = y[i];
			y[i] = tippedY;
			lowest = Math.max(lowest, tippedY);
		}

		for (int i = 0; i < n; i++)
			y[i] -= lowest;
	}
}
