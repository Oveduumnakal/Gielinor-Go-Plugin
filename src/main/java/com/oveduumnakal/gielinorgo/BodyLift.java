/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * How far a creature's body has to be lifted so it stands on the ground. The game's Y axis points down, so a vertex
 * with a positive Y is below the model's origin, which sits on the ground tile. Water creatures are modelled to sit
 * low in the water: read from the cache, the dolphin's lowest vertex is 17 model units below its origin, a bull
 * shark's 33 and a great white shark's 39, where a cow's is 0 and a goblin's is 0. On land that part of the body is
 * under the floor, so it is lifted by the same amount. It holds no game client types.
 */
final class BodyLift
{
	/**
	 * Model units a model may sit below its origin without being lifted: land creatures whose feet are a hair low
	 * are left alone. A tile is 128 units.
	 */
	static final int LIFT_FLOOR = 4;

	private BodyLift()
	{
	}

	/**
	 * Finds the lowest point of a model.
	 *
	 * @param y the vertices' Y values, where more is lower
	 * @param count how many of them are real vertices
	 * @return the largest Y, or negative infinity when there are none
	 */
	static double lowest(float[] y, int count)
	{
		double lowest = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < Math.min(count, y.length); i++)
			lowest = Math.max(lowest, y[i]);

		return lowest;
	}

	/**
	 * Finds how far a model sinks below its origin, if that is worth lifting.
	 *
	 * @param y the vertices' Y values, where more is lower
	 * @param count how many of them are real vertices
	 * @return the lowest Y in model units, or 0 when it is at most {@link #LIFT_FLOOR}
	 */
	static double restLowest(float[] y, int count)
	{
		double lowest = lowest(y, count);
		return lowest > LIFT_FLOOR ? lowest : 0.0;
	}

	/**
	 * Works out the lift for a model drawn at a height.
	 *
	 * @param restLowest how far the model sinks, from {@link #restLowest}
	 * @param heightScale the drawn height, where {@link NpcModels#NORMAL_SCALE} is as modelled
	 * @return scene units to lift it, rounded up, 0 when it does not sink
	 */
	static int lift(double restLowest, int heightScale)
	{
		if (restLowest <= 0.0)
			return 0;

		return (int) Math.ceil(restLowest * heightScale / NpcModels.NORMAL_SCALE);
	}

	/**
	 * Works out the lift for a model at its drawn height.
	 *
	 * @param y the vertices' Y values, where more is lower
	 * @param count how many of them are real vertices
	 * @param heightScale the drawn height, where {@link NpcModels#NORMAL_SCALE} is as modelled
	 * @return scene units to lift it, 0 when it stands on the ground
	 */
	static int liftFor(float[] y, int count, int heightScale)
	{
		return lift(restLowest(y, count), heightScale);
	}
}
