/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Where a thrown jar is on its way from the player to the creature, counted in client ticks (20 ms each) from the
 * start of the throw. The jar leaves the hand when the throw's arm swing releases it. A longer throw stays in the air
 * longer and arcs higher, so it lands at {@link CatchScene#LAND_TICK} only when it is short; {@link #delay(double)}
 * says how much later everything that follows the landing happens. Holds no RuneLite types.
 */
final class JarFlight
{
	/** The client tick at which the jar leaves the player's hand. */
	static final int RELEASE_TICK = 20;

	/** The shortest time in the air, in client ticks: a short throw lands at {@link CatchScene#LAND_TICK}. */
	static final int MIN_FLIGHT = CatchScene.LAND_TICK - RELEASE_TICK;

	/** The longest time in the air, in client ticks. */
	static final int MAX_FLIGHT = 40;

	/** The lowest height the arc adds, in the game's local units: one tile. */
	static final int MIN_ARC = 128;

	/** The greatest height the arc adds, in local units: three tiles. */
	static final int MAX_ARC = 384;

	private static final double TICKS_PER_TILE = 3.0;
	private static final double ARC_PER_TILE = 28.0;

	private JarFlight()
	{
	}

	/**
	 * Gives how long a throw is in the air.
	 *
	 * @param tiles the straight-line distance from the player to the creature, in tiles
	 * @return client ticks, from {@link #MIN_FLIGHT} to {@link #MAX_FLIGHT}
	 */
	static int flightTicks(double tiles)
	{
		return clamp(Math.round(TICKS_PER_TILE * tiles), MIN_FLIGHT, MAX_FLIGHT);
	}

	/**
	 * Gives how much later than a short throw a throw lands.
	 *
	 * @param tiles the straight-line distance, in tiles
	 * @return client ticks to add to everything timed from the landing
	 */
	static int delay(double tiles)
	{
		return flightTicks(tiles) - MIN_FLIGHT;
	}

	/**
	 * Gives how high a throw arcs.
	 *
	 * @param tiles the straight-line distance, in tiles
	 * @return the height added in the middle of the flight, from {@link #MIN_ARC} to {@link #MAX_ARC}
	 */
	static int arc(double tiles)
	{
		return clamp(Math.round(ARC_PER_TILE * tiles), MIN_ARC, MAX_ARC);
	}

	/**
	 * Tells whether the jar is in the air and should be drawn.
	 *
	 * @param age client ticks since the throw
	 * @param flightTicks how long this throw is in the air
	 * @return true from the release up to, but not including, the landing
	 */
	static boolean flying(int age, int flightTicks)
	{
		return age >= RELEASE_TICK && age < RELEASE_TICK + flightTicks;
	}

	/**
	 * Tells how far along the flight is.
	 *
	 * @param age client ticks since the throw
	 * @param flightTicks how long this throw is in the air
	 * @return 0 at the release and 1 at the landing, held at those values outside the flight
	 */
	static double progress(int age, int flightTicks)
	{
		return Math.max(0.0, Math.min(1.0, (age - RELEASE_TICK) / (double) flightTicks));
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
	 * Finds how high the jar is. Both ends are absolute heights, so the arc does not follow hills and ledges on the
	 * way.
	 *
	 * @param from the height of the player's hand, larger being higher
	 * @param to the height of the creature
	 * @param progress how far along the flight is, from 0 to 1
	 * @param arc the height this throw arcs, from {@link #arc(double)}
	 * @return the straight line between them plus the arc
	 */
	static double height(double from, double to, double progress, int arc)
	{
		return between(from, to, progress) + arc * lift(progress);
	}

	private static int clamp(long value, int low, int high)
	{
		return (int) Math.max(low, Math.min(high, value));
	}
}
