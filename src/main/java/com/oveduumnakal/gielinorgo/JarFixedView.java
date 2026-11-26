/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * A still camera looking a little down at a jar that fills a square picture: the view the designs were approved in, for
 * pictures that do not depend on the game. Holds no RuneLite types.
 */
final class JarFixedView implements JarView
{
	/** How far the camera looks down, in degrees. */
	private static final double PITCH = 24;

	/** How far the camera stands from the jar, in the jar's units. */
	private static final double DISTANCE = 600;

	/** The side of the square, in the jar's units, that the picture shows. */
	private static final double FRAME = 150;

	private final int size;
	private final double spin;
	private final double roll;

	/**
	 * Looks at a jar.
	 *
	 * @param size the side of the picture, in pixels
	 * @param spin how far the jar is turned about its axis, in radians
	 * @param roll how far it is rolled across the picture, in radians
	 */
	JarFixedView(int size, double spin, double roll)
	{
		this.size = size;
		this.spin = spin;
		this.roll = roll;
	}

	@Override
	public double[] view(double[] point)
	{
		return turn(point, spin);
	}

	@Override
	public double[] reflection(double[] point)
	{
		return turn(point, 0);
	}

	@Override
	public double reflectionStrength()
	{
		return 1.0;
	}

	@Override
	public double[] screen(double[] turned)
	{
		double near = DISTANCE / (DISTANCE - turned[2]);
		double scale = pixelsPerUnit();
		return new double[] {size / 2.0 + turned[0] * scale * near, size / 2.0 - turned[1] * scale * near};
	}

	@Override
	public boolean faces(double[] at, double[] normal)
	{
		return normal[2] > 0;
	}

	@Override
	public double pixelsPerUnit()
	{
		return size / FRAME;
	}

	private double[] turn(double[] p, double yaw)
	{
		double pitch = Math.toRadians(PITCH);
		double x = p[0] * Math.cos(yaw) + p[2] * Math.sin(yaw);
		double z = -p[0] * Math.sin(yaw) + p[2] * Math.cos(yaw);
		double across = x * Math.cos(roll) - p[1] * Math.sin(roll);
		double up = x * Math.sin(roll) + p[1] * Math.cos(roll);
		return new double[] {across, up * Math.cos(pitch) - z * Math.sin(pitch),
			up * Math.sin(pitch) + z * Math.cos(pitch)};
	}
}
