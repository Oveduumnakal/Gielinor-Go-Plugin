/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Where a projectile of a battle that is acted out on screen is at each client tick of a turn: from the attacker at
 * {@link #LAUNCH_TICK} to the target at {@link TurnMotion#IMPACT_TICK}, in a straight line along the ground with a
 * small arc in height. Positions are world tiles; heights are local units above the ground. Holds no RuneLite types.
 */
final class ProjectilePath
{
	/** The tick the projectile leaves the attacker. */
	static final int LAUNCH_TICK = TurnMotion.LAUNCH_TICK;

	/** How high the middle of the flight is above the straight line, in local units. */
	static final double ARC_HEIGHT = 24.0;

	private final double fromX;
	private final double fromY;
	private final double fromHeight;
	private final double toX;
	private final double toY;
	private final double toHeight;

	/**
	 * Plans a flight.
	 *
	 * @param fromX the attacker's world x, in tiles
	 * @param fromY the attacker's world y, in tiles
	 * @param fromHeight how high above the ground it leaves, in local units
	 * @param toX the target's world x, in tiles
	 * @param toY the target's world y, in tiles
	 * @param toHeight how high above the ground it lands, in local units
	 */
	ProjectilePath(double fromX, double fromY, double fromHeight, double toX, double toY, double toHeight)
	{
		this.fromX = fromX;
		this.fromY = fromY;
		this.fromHeight = fromHeight;
		this.toX = toX;
		this.toY = toY;
		this.toHeight = toHeight;
	}

	/**
	 * @param age client ticks since the turn began
	 * @return how far along the flight it is: 0 at launch and before, 1 at impact and after
	 */
	static double progress(int age)
	{
		if (age <= LAUNCH_TICK)
			return 0.0;

		if (age >= TurnMotion.IMPACT_TICK)
			return 1.0;

		return (age - LAUNCH_TICK) / (double) (TurnMotion.IMPACT_TICK - LAUNCH_TICK);
	}

	/**
	 * @param age client ticks since the turn began
	 * @return whether the projectile is in the air: from its launch up to, not including, its impact
	 */
	static boolean visible(int age)
	{
		return age >= LAUNCH_TICK && age < TurnMotion.IMPACT_TICK;
	}

	/**
	 * @param age client ticks since the turn began
	 * @return world x, in tiles
	 */
	double x(int age)
	{
		return fromX + (toX - fromX) * progress(age);
	}

	/**
	 * @param age client ticks since the turn began
	 * @return world y, in tiles
	 */
	double y(int age)
	{
		return fromY + (toY - fromY) * progress(age);
	}

	/**
	 * @param age client ticks since the turn began
	 * @return height above the ground, in local units
	 */
	double height(int age)
	{
		double share = progress(age);
		return fromHeight + (toHeight - fromHeight) * share + ARC_HEIGHT * Math.sin(Math.PI * share);
	}

	/** @return the way it flies, as a direction east for x and north for y: the target minus the attacker */
	double[] direction()
	{
		return new double[]{toX - fromX, toY - fromY};
	}
}
