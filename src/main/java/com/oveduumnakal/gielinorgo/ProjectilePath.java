/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Where a projectile of a battle that is acted out on screen is at each client tick of a turn: from the attacker at
 * {@link #LAUNCH_TICK} to the target at {@link TurnMotion#IMPACT_TICK}, in a straight line along the ground with a
 * small arc in height. Positions are world tiles; heights are local units above the ground. When the bodies' own
 * ground heights are known ({@link #hasGround()}), the flight goes from the one to the other in a straight line, so a
 * creature on raised ground shoots from its own height; otherwise the height follows the terrain under the flight.
 * Holds no RuneLite types.
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
	private final double fromGround;
	private final double toGround;

	/**
	 * Plans a flight over the terrain under it.
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
		this(fromX, fromY, fromHeight, Double.NaN, toX, toY, toHeight, Double.NaN);
	}

	/**
	 * Plans a flight between two bodies at the heights they are drawn at.
	 *
	 * @param fromX the attacker's world x, in tiles
	 * @param fromY the attacker's world y, in tiles
	 * @param fromHeight how high above its own ground it leaves, in local units
	 * @param fromGround the scene's z of the attacker's feet; NaN when unknown
	 * @param toX the target's world x, in tiles
	 * @param toY the target's world y, in tiles
	 * @param toHeight how high above its own ground it lands, in local units
	 * @param toGround the scene's z of the target's feet; NaN when unknown. The scene's z grows downward, so a
	 *     higher ground has a smaller z.
	 */
	ProjectilePath(double fromX, double fromY, double fromHeight, double fromGround, double toX, double toY,
		double toHeight, double toGround)
	{
		this.fromGround = fromGround;
		this.toGround = toGround;
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

	/** @return whether both bodies' ground heights are known, so {@link #z(int)} gives the whole height */
	boolean hasGround()
	{
		return !Double.isNaN(fromGround) && !Double.isNaN(toGround);
	}

	/**
	 * @param age client ticks since the turn began
	 * @return the scene's z of the projectile: the straight line between the two bodies' ground, less
	 *     {@link #height(int)}; meaningful only when {@link #hasGround()}
	 */
	double z(int age)
	{
		double share = progress(age);
		return fromGround + (toGround - fromGround) * share - height(age);
	}

	/** @return the way it flies, as a direction east for x and north for y: the target minus the attacker */
	double[] direction()
	{
		return new double[]{toX - fromX, toY - fromY};
	}
}
