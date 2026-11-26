/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The game's own camera looking at a jar in flight. The jar spins about its axis and tumbles end over end toward where
 * it is going, and the camera may look from any side. The projection is the game's: the same sums as
 * {@code Perspective.modelToCanvas}, kept in fractions of a pixel so a small jar does not shake. A test holds the two
 * together. Positions are in local units, with x east, y north and height larger downward, as the game counts them.
 * Holds no RuneLite types.
 */
final class JarLiveView implements JarView
{
	/** The height a jar is drawn at in the game, in local units. */
	static final double JAR_HEIGHT = 56;

	/** Angle units in a full turn of the game's camera. */
	static final int CAMERA_TURN = 16384;

	/** The nearest a thing can be to the camera and still be drawn. */
	private static final double NEAREST = 50;

	/** Local units per unit of the jar. */
	private static final double UNIT = JAR_HEIGHT / JarShapes.HEIGHT;

	/** Where the game's camera is, how it points and where it draws. */
	static final class Camera
	{
		private final double x;
		private final double y;
		private final double height;
		private final double yaw;
		private final double pitch;
		private final double zoom;
		private final double centreX;
		private final double centreY;

		/**
		 * Describes a camera.
		 *
		 * @param position its x, y and height
		 * @param yaw how far it is turned, in radians
		 * @param pitch how far it looks down, in radians
		 * @param zoom the game's scale
		 * @param centreX the middle of the viewport on the canvas
		 * @param centreY the same, downward
		 */
		Camera(double[] position, double yaw, double pitch, double zoom, double centreX, double centreY)
		{
			this.x = position[0];
			this.y = position[1];
			this.height = position[2];
			this.yaw = yaw;
			this.pitch = pitch;
			this.zoom = zoom;
			this.centreX = centreX;
			this.centreY = centreY;
		}

		/**
		 * Turns a direction of the world to the camera.
		 *
		 * @param east how far east
		 * @param north how far north
		 * @param up how far up
		 * @return how far right, up and toward the camera
		 */
		private double[] turn(double east, double north, double up)
		{
			double right = east * Math.cos(yaw) + north * Math.sin(yaw);
			double ahead = north * Math.cos(yaw) - east * Math.sin(yaw);
			double below = -up * Math.cos(pitch) - ahead * Math.sin(pitch);
			double away = ahead * Math.cos(pitch) - up * Math.sin(pitch);
			return new double[] {right, -below, -away};
		}
	}

	/**
	 * Turns one of the game's camera angles into radians.
	 *
	 * @param units the angle in {@link #CAMERA_TURN} units to a turn
	 * @return the angle in radians
	 */
	static double radians(int units)
	{
		return units * 2 * Math.PI / CAMERA_TURN;
	}

	private final Camera camera;
	private final double[] centre;
	private final double spin;
	private final double tumble;
	private final double headingEast;
	private final double headingNorth;
	private final double roll;
	private final double strength;

	/**
	 * Looks at a jar.
	 *
	 * @param camera the game's camera
	 * @param at the jar's turning point as x, y and height
	 * @param turns how far it has spun and tumbled, in radians
	 * @param headingEast the east part of the way it is going
	 * @param headingNorth the north part; both 0 count as north
	 */
	JarLiveView(Camera camera, double[] at, double turns, double headingEast, double headingNorth)
	{
		this.camera = camera;
		double[] turned = camera.turn(at[0] - camera.x, at[1] - camera.y, camera.height - at[2]);
		this.centre = turned;
		this.spin = turns;
		this.tumble = turns;
		double length = Math.hypot(headingEast, headingNorth);
		this.headingEast = length == 0 ? 0 : headingEast / length;
		this.headingNorth = length == 0 ? 1 : headingNorth / length;
		double[] axis = view(new double[] {0, 1, 0});
		this.roll = Math.atan2(-axis[0], axis[1]);
		this.strength = Math.min(1.0, 2 * Math.hypot(axis[0], axis[1]) / UNIT);
	}

	/** @return whether the jar is far enough in front of the camera to be drawn */
	boolean inFront()
	{
		return -centre[2] >= NEAREST;
	}

	@Override
	public double[] view(double[] point)
	{
		double x = point[0] * Math.cos(spin) + point[2] * Math.sin(spin);
		double z = -point[0] * Math.sin(spin) + point[2] * Math.cos(spin);
		double east = x * UNIT;
		double north = -z * UNIT;
		double up = point[1] * UNIT;

		double along = east * headingEast + north * headingNorth;
		double aside = east * headingNorth - north * headingEast;
		double forward = along * Math.cos(tumble) + up * Math.sin(tumble);
		double raised = up * Math.cos(tumble) - along * Math.sin(tumble);
		return camera.turn(forward * headingEast + aside * headingNorth,
			forward * headingNorth - aside * headingEast, raised);
	}

	@Override
	public double[] reflection(double[] point)
	{
		double across = point[0] * Math.cos(roll) - point[1] * Math.sin(roll);
		double up = point[0] * Math.sin(roll) + point[1] * Math.cos(roll);
		double z = point[2];
		double pitch = camera.pitch;
		return new double[] {across * UNIT, (up * Math.cos(pitch) - z * Math.sin(pitch)) * UNIT,
			(up * Math.sin(pitch) + z * Math.cos(pitch)) * UNIT};
	}

	@Override
	public double reflectionStrength()
	{
		return strength;
	}

	@Override
	public double[] screen(double[] turned)
	{
		double away = Math.max(NEAREST, -(centre[2] + turned[2]));
		return new double[] {camera.centreX + (centre[0] + turned[0]) * camera.zoom / away,
			camera.centreY - (centre[1] + turned[1]) * camera.zoom / away};
	}

	@Override
	public boolean faces(double[] at, double[] normal)
	{
		return normal[0] * (centre[0] + at[0]) + normal[1] * (centre[1] + at[1])
			+ normal[2] * (centre[2] + at[2]) < 0;
	}

	@Override
	public double pixelsPerUnit()
	{
		return UNIT * camera.zoom / Math.max(NEAREST, -centre[2]);
	}
}
