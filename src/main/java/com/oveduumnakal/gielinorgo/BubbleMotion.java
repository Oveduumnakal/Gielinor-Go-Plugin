/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.geom.Point2D;

/**
 * The motion of the action bubbles as pure functions of time: the curve a bubble flies along, how it grows and fades,
 * how it hovers and pulses, and the tweens that drive the group, the jar cluster and the card. The caller passes the
 * time in, so a test can ask for any moment. A bubble's progress {@code t} runs 0 to 1 along a quadratic bezier from
 * the creature's middle to its resting place; past 1 the curve extrapolates, which is the overshoot. Holds no RuneLite
 * types.
 */
final class BubbleMotion
{
	/** How long one bubble takes to pop out, in milliseconds. */
	static final long APPEAR_MS = 500;

	/** The gap between one bubble starting to pop out and the next, left to right. */
	static final long STAGGER_MS = 100;

	/** How long one bubble takes to go back, in milliseconds. */
	static final long RETRACT_MS = 300;

	/** The gap between one bubble starting to go back and the next, right to left. */
	static final long RETRACT_STAGGER_MS = 50;

	/** How long the hover growth glides, in milliseconds. */
	static final long HOVER_MS = 120;

	/** How long the held bubbles take to glide back to their creature, in milliseconds. */
	static final long HOLD_GLIDE_MS = 400;

	/** The period of the knocked-out ring pulse, in milliseconds. */
	static final long PULSE_MS = 1200;

	/** How long one cluster jar takes to pop out. */
	static final long CLUSTER_APPEAR_MS = 450;

	/** The gap between cluster jars popping out, left to right. */
	static final long CLUSTER_STAGGER_MS = 60;

	/** How long one cluster jar takes to go back. */
	static final long CLUSTER_RETRACT_MS = 250;

	/** The gap between cluster jars going back, left to right. */
	static final long CLUSTER_RETRACT_STAGGER_MS = 30;

	/** How long the card takes to show. */
	static final long CARD_MS = 180;

	/** How long the card takes to hide. */
	static final long CARD_FADE_MS = 120;

	/** How much a fully hovered bubble grows. */
	static final double HOVER_GROWTH = 0.15;

	/** How many pixels a fully hovered bubble lifts. */
	static final double HOVER_LIFT = 3.0;

	/** How far toward the resting place the bezier's control point sits in y. */
	private static final double CONTROL_RISE = 0.25;

	private BubbleMotion()
	{
	}

	/**
	 * Gives how far a bubble's path bends sideways.
	 *
	 * @param index 0 for the left bubble, 1 for the middle, 2 for the right
	 * @param radius the bubble's radius
	 * @return the bend: the left one bows left by 1.8 radii, the middle right by 1.4, the right one right by 1.8
	 */
	static double bend(int index, double radius)
	{
		if (index == 0)
			return -1.8 * radius;

		return index == 1 ? 1.4 * radius : 1.8 * radius;
	}

	/**
	 * Works out the bezier's control point: above the resting place's x, a quarter of the way up, bent sideways.
	 *
	 * @param origin where the bubble starts
	 * @param rest where it ends
	 * @param bend how far the control point is moved sideways
	 * @return the control point
	 */
	static Point2D.Double control(Point2D origin, Point2D rest, double bend)
	{
		return new Point2D.Double(rest.getX() + bend, origin.getY() + (rest.getY() - origin.getY()) * CONTROL_RISE);
	}

	/**
	 * Evaluates the quadratic bezier. It is exactly the origin at 0 and exactly the rest at 1, and it carries on past
	 * either end, so a progress above 1 overshoots.
	 *
	 * @param origin the start
	 * @param control the control point
	 * @param rest the end
	 * @param t the progress
	 * @return the point
	 */
	static Point2D.Double bezier(Point2D origin, Point2D control, Point2D rest, double t)
	{
		if (t == 0.0)
			return new Point2D.Double(origin.getX(), origin.getY());

		if (t == 1.0)
			return new Point2D.Double(rest.getX(), rest.getY());

		double u = 1.0 - t;
		return new Point2D.Double(u * u * origin.getX() + 2 * u * t * control.getX() + t * t * rest.getX(),
			u * u * origin.getY() + 2 * u * t * control.getY() + t * t * rest.getY());
	}

	/**
	 * Gives where a bubble is for a progress. When things may not move it sits at its resting place all along.
	 *
	 * @param origin where it starts
	 * @param rest where it rests
	 * @param bend the path's sideways bend
	 * @param t the progress
	 * @param mode the motion mode
	 * @return the centre
	 */
	static Point2D.Double position(Point2D origin, Point2D rest, double bend, double t, MotionMode mode)
	{
		if (!mode.moves())
			return new Point2D.Double(rest.getX(), rest.getY());

		return bezier(origin, control(origin, rest, bend), rest, t);
	}

	/**
	 * @param t the progress
	 * @param mode the motion mode
	 * @return the size as a multiple of the radius: 0.3 growing to 1, or always 1 when things may not move
	 */
	static double scale(double t, MotionMode mode)
	{
		return mode.moves() ? 0.3 + 0.7 * t : 1.0;
	}

	/**
	 * @param t the progress
	 * @param mode the motion mode
	 * @return the opacity: it reaches 1 by 40 percent of the way when moving, and follows the progress otherwise
	 */
	static double alpha(double t, MotionMode mode)
	{
		return clamp(mode.moves() ? 2.5 * t : t);
	}

	/**
	 * @param hover how hovered, from 0 to 1
	 * @param mode the motion mode
	 * @return what the scale is multiplied by: up to 1.15, and 1 when things may not move
	 */
	static double hoverScale(double hover, MotionMode mode)
	{
		return mode.moves() ? 1.0 + HOVER_GROWTH * hover : 1.0;
	}

	/**
	 * @param hover how hovered, from 0 to 1
	 * @param mode the motion mode
	 * @return how many pixels up a hovered bubble lifts, and 0 when things may not move
	 */
	static double hoverLift(double hover, MotionMode mode)
	{
		return mode.moves() ? HOVER_LIFT * hover : 0.0;
	}

	/**
	 * Starts a bubble popping out.
	 *
	 * @param from the progress to start from, 0 to 1; a bubble that was leaving reverses from where it is
	 * @param now the time
	 * @param index 0 left, 1 middle, 2 right; the left one lands first
	 * @param mode the motion mode
	 * @return the tween of the progress
	 */
	static Tween appear(double from, long now, int index, MotionMode mode)
	{
		double left = 1.0 - from;
		if (mode.moves())
			return new Tween(from, 1, now, index * STAGGER_MS, Math.round(APPEAR_MS * left), Easing.OUT_BACK_SOFT);

		return new Tween(from, 1, now, 0, Math.round(mode.fadeMs(APPEAR_MS) * left), Easing.LINEAR);
	}

	/**
	 * Starts a bubble going back to the creature.
	 *
	 * @param from the progress to start from
	 * @param now the time
	 * @param index 0 left, 1 middle, 2 right; the right one goes first
	 * @param mode the motion mode
	 * @return the tween of the progress
	 */
	static Tween retract(double from, long now, int index, MotionMode mode)
	{
		if (mode.moves())
		{
			return new Tween(from, 0, now, (2 - index) * RETRACT_STAGGER_MS, Math.round(RETRACT_MS * from),
				Easing.IN_OUT_SINE);
		}

		return new Tween(from, 0, now, 0, Math.round(mode.fadeMs(RETRACT_MS) * from), Easing.LINEAR);
	}

	/**
	 * Starts a cluster jar popping out.
	 *
	 * @param from the progress to start from
	 * @param now the time
	 * @param rank the jar's place from the left, 0 first
	 * @param mode the motion mode
	 * @return the tween of the progress
	 */
	static Tween clusterAppear(double from, long now, int rank, MotionMode mode)
	{
		double left = 1.0 - from;
		if (mode.moves())
		{
			return new Tween(from, 1, now, rank * CLUSTER_STAGGER_MS, Math.round(CLUSTER_APPEAR_MS * left),
				Easing.OUT_BACK_SOFT);
		}

		return new Tween(from, 1, now, 0, Math.round(mode.fadeMs(CLUSTER_APPEAR_MS) * left), Easing.LINEAR);
	}

	/**
	 * Starts a cluster jar going back to the Catch bubble.
	 *
	 * @param from the progress to start from
	 * @param now the time
	 * @param rank the jar's place from the left, 0 first
	 * @param mode the motion mode
	 * @return the tween of the progress
	 */
	static Tween clusterRetract(double from, long now, int rank, MotionMode mode)
	{
		if (mode.moves())
		{
			return new Tween(from, 0, now, rank * CLUSTER_RETRACT_STAGGER_MS, Math.round(CLUSTER_RETRACT_MS * from),
				Easing.IN_OUT_SINE);
		}

		return new Tween(from, 0, now, 0, Math.round(mode.fadeMs(CLUSTER_RETRACT_MS) * from), Easing.LINEAR);
	}

	/**
	 * Starts the card showing.
	 *
	 * @param from the progress to start from
	 * @param now the time
	 * @param mode the motion mode
	 * @return the tween of the card's progress
	 */
	static Tween cardShow(double from, long now, MotionMode mode)
	{
		double left = 1.0 - from;
		if (mode.moves())
			return new Tween(from, 1, now, 0, Math.round(CARD_MS * left), Easing.OUT_BACK_SOFT);

		return new Tween(from, 1, now, 0, Math.round(mode.fadeMs(CARD_MS) * left), Easing.LINEAR);
	}

	/**
	 * Starts the card hiding.
	 *
	 * @param from the progress to start from
	 * @param now the time
	 * @param mode the motion mode
	 * @return the tween of the card's progress
	 */
	static Tween cardHide(double from, long now, MotionMode mode)
	{
		if (mode.moves())
			return new Tween(from, 0, now, 0, Math.round(CARD_FADE_MS * from), Easing.IN_CUBIC);

		return new Tween(from, 0, now, 0, Math.round(mode.fadeMs(CARD_FADE_MS) * from), Easing.LINEAR);
	}

	/**
	 * @param t the card's progress
	 * @param mode the motion mode
	 * @return the card's size: 0.85 growing to 1, or always 1 when things may not move
	 */
	static double cardScale(double t, MotionMode mode)
	{
		return mode.moves() ? 0.85 + 0.15 * clamp(t) : 1.0;
	}

	/**
	 * Gives the pulse of the ring on a knocked-out creature's Catch bubble.
	 *
	 * @param now the time
	 * @param mode the motion mode
	 * @return 0 to 1 and back every {@link #PULSE_MS}, starting at 0; 0 when nothing may loop
	 */
	static double pulse(long now, MotionMode mode)
	{
		if (!mode.loops())
			return 0.0;

		return 0.5 - 0.5 * Math.cos(2 * Math.PI * (Math.floorMod(now, PULSE_MS)) / PULSE_MS);
	}

	/**
	 * @param value any number
	 * @return the number held between 0 and 1
	 */
	static double clamp(double value)
	{
		return Math.max(0.0, Math.min(1.0, value));
	}
}
