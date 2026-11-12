/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Where a thrown jar is on its way from the player to the creature, counted in client ticks (20 ms each) from the
 * start of the throw. The jar leaves the hand when the throw's arm swing releases it and lands at
 * {@link CatchScene#LAND_TICK}. Holds no RuneLite types.
 */
final class JarFlight
{
	/** The client tick at which the jar leaves the player's hand. */
	static final int RELEASE_TICK = 20;

	/** The height the arc adds in the middle of the flight, in the game's local units: one tile. */
	static final int ARC_HEIGHT = 128;

	private JarFlight()
	{
	}

	/**
	 * Tells whether the jar is in the air and should be drawn.
	 *
	 * @param age client ticks since the throw
	 * @return true from the release up to, but not including, the landing
	 */
	static boolean flying(int age)
	{
		return age >= RELEASE_TICK && age < CatchScene.LAND_TICK;
	}

	/**
	 * Tells how far along the flight is.
	 *
	 * @param age client ticks since the throw
	 * @return 0 at the release and 1 at the landing, held at those values outside the flight
	 */
	static double progress(int age)
	{
		double span = CatchScene.LAND_TICK - RELEASE_TICK;
		return Math.max(0.0, Math.min(1.0, (age - RELEASE_TICK) / span));
	}

	/**
	 * Tells how much lift the arc gives at one point of the flight.
	 *
	 * @param progress how far along the flight is, from 0 to 1
	 * @return 0 at both ends and 1 in the middle
	 */
	static double lift(double progress)
	{
		return 4.0 * progress * (1.0 - progress);
	}

	/**
	 * Finds a point between two values.
	 *
	 * @param from the value at progress 0
	 * @param to the value at progress 1
	 * @param progress how far along the flight is, from 0 to 1
	 * @return the value in between
	 */
	static double between(double from, double to, double progress)
	{
		return from + (to - from) * progress;
	}

	/**
	 * Finds how high above the ground the jar is.
	 *
	 * @param from the height at the player's hand
	 * @param to the height at the creature
	 * @param progress how far along the flight is, from 0 to 1
	 * @return the straight line between them plus {@link #ARC_HEIGHT} times {@link #lift(double)}
	 */
	static double height(double from, double to, double progress)
	{
		return between(from, to, progress) + ARC_HEIGHT * lift(progress);
	}
}
