/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The timing of one turn of an acted-out battle, in client ticks (20 ms each) counted from the tick the turn's beat
 * begins. Every turn is one beat ({@link BattleTimeline#BEAT_TICKS} game ticks, {@link #TURN_TICKS} client ticks)
 * and the hit lands on the same tick, {@link #IMPACT_TICK}, whatever the attacker's style:
 * <pre>
 * tick     melee                                    ranged or magic
 * 0-10     stands                                   attack animation starts at 0
 * 10-28    walks up to the target                   the projectile leaves at 12
 * 20       attack animation starts                  the projectile is in flight
 * 30       impact                                   impact
 * 50-80    walks back to its own spot               stands
 * 80-90    stands                                   stands
 * </pre>
 * Every number lives here so the pace can be tuned in one place. Holds no RuneLite types.
 */
final class TurnMotion
{
	/** Client ticks in one game tick. */
	static final int CLIENT_TICKS_PER_GAME_TICK = 30;

	/** Client ticks in one turn: one beat. */
	static final int TURN_TICKS = BattleTimeline.BEAT_TICKS * CLIENT_TICKS_PER_GAME_TICK;

	/** The tick the blow lands on, for every style: the target reacts and the battle line appears. */
	static final int IMPACT_TICK = 30;

	/** The tick a melee attacker starts walking up to the target. */
	static final int DASH_START = 10;

	/** The tick a melee attacker is at the target. */
	static final int DASH_END = 28;

	/** The tick a melee attacker starts walking back. */
	static final int RETURN_START = 50;

	/** The tick a melee attacker is back on its own spot. */
	static final int RETURN_END = 80;

	/** The tick a melee attacker starts its attack animation, a little before the blow lands. */
	static final int STRIKE_TICK = 20;

	/** The tick a ranged or magic attacker starts its attack animation. */
	static final int SHOT_TICK = 0;

	/** The tick a projectile leaves the attacker. */
	static final int LAUNCH_TICK = 12;

	/** Client ticks a body takes to ease back to its own spot when a turn is cut short, for a catch. */
	static final int SETTLE_TICKS = 12;

	/** How close a melee attacker comes to the target's edge, in tiles. */
	static final double CONTACT_TILES = 0.75;

	/** How far apart the lead and the wild creature's stand-in start, edge to lead, in tiles. */
	static final double GAP_TILES = 2.0;

	/** The gap used when the far spot is blocked, in tiles. */
	static final double NEAR_GAP_TILES = 1.0;

	private TurnMotion()
	{
	}

	/**
	 * Works out how far a melee attacker is from its own spot, along the way it faces: out in the dash, held through
	 * the blow, and back again.
	 *
	 * @param age client ticks since the turn began
	 * @param distance how far it goes at the furthest, in tiles
	 * @return the distance from its spot, 0 before {@link #DASH_START} and from {@link #RETURN_END} on, and never
	 *     more than {@code distance}
	 */
	static double offset(int age, double distance)
	{
		if (age <= DASH_START || age >= RETURN_END)
			return 0.0;

		if (age < DASH_END)
			return distance * eased((age - DASH_START) / (double) (DASH_END - DASH_START));

		if (age <= RETURN_START)
			return distance;

		return distance * (1.0 - eased((age - RETURN_START) / (double) (RETURN_END - RETURN_START)));
	}

	/**
	 * @param age client ticks since the turn began
	 * @return whether a melee attacker is on the move: in the dash or on the way back
	 */
	static boolean walking(int age)
	{
		return (age >= DASH_START && age < DASH_END) || (age >= RETURN_START && age < RETURN_END);
	}

	/** @return the tick a melee attacker starts its attack animation */
	static int strikeTick()
	{
		return STRIKE_TICK;
	}

	/**
	 * Works out where a body that was cut off in the middle of a step is while it eases back to its own spot.
	 *
	 * @param from how far it was from its spot when cut off, in tiles
	 * @param age client ticks since it was cut off
	 * @return the distance, {@code from} at age 0 and 0 from {@link #SETTLE_TICKS} on
	 */
	static double settle(double from, int age)
	{
		if (age >= SETTLE_TICKS)
			return 0.0;

		return from * (1.0 - eased(Math.max(0, age) / (double) SETTLE_TICKS));
	}

	/**
	 * @param share how far along from 0 to 1
	 * @return the share eased at both ends
	 */
	private static double eased(double share)
	{
		double clamped = Math.max(0.0, Math.min(1.0, share));
		return clamped * clamped * (3.0 - 2.0 * clamped);
	}
}
