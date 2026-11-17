/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The timing of the pop-up notice: it pops in, holds, then fades. In {@link MotionMode#FULL} it pops in over 150 ms
 * (scale 0.8 to 1 with an overshoot), holds 2 s and fades over 400 ms. {@link MotionMode#REDUCED} fades in over the
 * same 150 ms with no scale, then holds and fades the same. {@link MotionMode#OFF} shows it at full strength for 2 s
 * and removes it, with no motion. The caller passes the time since the notice appeared. Holds no RuneLite types.
 */
final class NoticeClock
{
	private NoticeClock()
	{
	}

	/** How the notice looks at one moment. */
	static final class Frame
	{
		/** How large to draw the notice, 1 for plain. */
		final double scale;

		/** How solid to draw it, from 0 to 1. */
		final double alpha;

		/** Whether it is still on screen. */
		final boolean visible;

		Frame(double scale, double alpha, boolean visible)
		{
			this.scale = scale;
			this.alpha = alpha;
			this.visible = visible;
		}
	}

	/**
	 * Gives how long the notice is on screen in a mode.
	 *
	 * @param mode the motion mode
	 * @return the time in milliseconds: pop, hold and fade, or only the hold when the mode does not fade
	 */
	static long totalMs(MotionMode mode)
	{
		return mode.fades() ? WindowTimes.NOTICE_POP + WindowTimes.NOTICE_HOLD + WindowTimes.NOTICE_FADE
			: WindowTimes.NOTICE_HOLD;
	}

	/**
	 * Gives the look of the notice some time after it appeared.
	 *
	 * @param mode the motion mode
	 * @param elapsedMs the time since the notice appeared
	 * @return the scale and opacity to draw at, and whether it is still shown
	 */
	static Frame at(MotionMode mode, long elapsedMs)
	{
		if (elapsedMs < 0 || elapsedMs >= totalMs(mode))
			return new Frame(1.0, 0.0, false);

		if (!mode.fades())
			return new Frame(1.0, 1.0, true);

		long pop = WindowTimes.NOTICE_POP;
		if (elapsedMs < pop)
			return popIn(mode, (double) elapsedMs / pop);

		long fadeStart = pop + WindowTimes.NOTICE_HOLD;
		if (elapsedMs < fadeStart)
			return new Frame(1.0, 1.0, true);

		double t = (double) (elapsedMs - fadeStart) / WindowTimes.NOTICE_FADE;
		return new Frame(1.0, 1.0 - Easing.LINEAR.apply(t), true);
	}

	private static Frame popIn(MotionMode mode, double t)
	{
		double alpha = Easing.OUT_CUBIC.apply(t);
		if (!mode.moves())
			return new Frame(1.0, alpha, true);

		double grow = WindowTimes.NOTICE_FROM_SCALE + (1.0 - WindowTimes.NOTICE_FROM_SCALE) * Easing.OUT_BACK.apply(t);
		return new Frame(grow, alpha, true);
	}
}
