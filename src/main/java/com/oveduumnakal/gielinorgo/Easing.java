/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Easing curves for the window's motion. Each maps progress from 0 to 1 onto a value that is exactly 0 at the start
 * and exactly 1 at the end, whatever the rounding of the maths in between. Holds no RuneLite types.
 */
enum Easing
{
	/** A steady pace. */
	LINEAR
	{
		@Override
		double curve(double t)
		{
			return t;
		}
	},

	/** Fast at first, then settling. Never goes past 1, so it is the one for bars. */
	OUT_CUBIC
	{
		@Override
		double curve(double t)
		{
			double u = 1.0 - t;
			return 1.0 - u * u * u;
		}
	},

	/** Slow at first, then speeding up; the way out for a window that closes. */
	IN_CUBIC
	{
		@Override
		double curve(double t)
		{
			return t * t * t;
		}
	},

	/** Goes a little past 1 and comes back, for a pop. */
	OUT_BACK
	{
		@Override
		double curve(double t)
		{
			double c1 = 1.70158;
			double c3 = c1 + 1.0;
			double u = t - 1.0;
			return 1.0 + c3 * u * u * u + c1 * u * u;
		}
	},

	/** Goes only a little past 1 (peak about 1.037) and comes back; the pop for a path that extrapolates. */
	OUT_BACK_SOFT
	{
		@Override
		double curve(double t)
		{
			double c1 = 1.0;
			double c3 = 2.0;
			double u = t - 1.0;
			return 1.0 + c3 * u * u * u + c1 * u * u;
		}
	},

	/** Gentle at both ends, for a highlight that glides. */
	IN_OUT_SINE
	{
		@Override
		double curve(double t)
		{
			return -(Math.cos(Math.PI * t) - 1.0) / 2.0;
		}
	};

	/**
	 * Eases a progress value.
	 *
	 * @param t how far along, from 0 to 1; outside that range it counts as the nearer end
	 * @return the eased value: exactly 0 at 0 and exactly 1 at 1
	 */
	double apply(double t)
	{
		if (t <= 0.0)
			return 0.0;

		if (t >= 1.0)
			return 1.0;

		return curve(t);
	}

	abstract double curve(double t);
}
