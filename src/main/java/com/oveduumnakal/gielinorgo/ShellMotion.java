/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The motion of the window shell: opening, closing and the locked shake. Each is a function of the motion mode and the
 * time since it started, so tests need no clock. Full motion scales (0.94 to 1) and fades, Reduced only fades over
 * 80 ms, and Off is instant. Holds no RuneLite types.
 */
final class ShellMotion
{
	private ShellMotion()
	{
	}

	/** What the shell is doing. */
	enum Phase
	{
		/** Opening. */
		OPEN,

		/** Closing because the player closed it. */
		CLOSE,

		/** Closing because of combat or a battle: a quick fade with no scale. */
		COMBAT_CLOSE
	}

	/** How the shell looks at one moment. */
	static final class Frame
	{
		/** How large to draw the window, 1 for plain. */
		final double scale;

		/** How solid to draw the window, from 0 to 1. */
		final double alpha;

		/** Whether the motion is over: an open window rests at alpha 1, a closed one is gone. */
		final boolean done;

		Frame(double scale, double alpha, boolean done)
		{
			this.scale = scale;
			this.alpha = alpha;
			this.done = done;
		}
	}

	/**
	 * Gives how long a phase lasts in a mode.
	 *
	 * @param mode the motion mode
	 * @param phase what the shell is doing
	 * @return the time in milliseconds; 0 in {@link MotionMode#OFF}
	 */
	static long durationMs(MotionMode mode, Phase phase)
	{
		long full = phase == Phase.OPEN ? WindowTimes.OPEN
			: phase == Phase.CLOSE ? WindowTimes.CLOSE : WindowTimes.COMBAT_CLOSE;
		return mode.fadeMs(full);
	}

	/**
	 * Gives the look of the shell some time after a phase started.
	 *
	 * @param mode the motion mode
	 * @param phase what the shell is doing
	 * @param elapsedMs the time since the phase started
	 * @return the scale and opacity to draw at, and whether the motion is over
	 */
	static Frame frame(MotionMode mode, Phase phase, long elapsedMs)
	{
		long total = durationMs(mode, phase);
		boolean done = elapsedMs >= total;
		double t = total <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, (double) elapsedMs / total));
		boolean opening = phase == Phase.OPEN;
		boolean scales = mode.moves() && phase != Phase.COMBAT_CLOSE;
		double eased = Easing.OUT_CUBIC.apply(t);
		double alpha = opening ? eased : 1.0 - Easing.LINEAR.apply(t);
		double shrink = (1.0 - WindowTimes.OPEN_FROM_SCALE) * eased;
		double scale = !scales ? 1.0 : opening ? WindowTimes.OPEN_FROM_SCALE + shrink : 1.0 - shrink;
		double rest = opening ? 1.0 : 0.0;
		return new Frame(done ? 1.0 : scale, done ? rest : alpha, done);
	}

	/**
	 * Gives the sideways offset of the shake a locked toggle or tab makes when clicked.
	 *
	 * @param mode the motion mode; only {@link MotionMode#FULL} shakes
	 * @param elapsedMs the time since the click
	 * @param amplitude how far the shake moves at most, in pixels
	 * @return the offset in pixels, 0 when the shake is over or the mode does not shake
	 */
	static double shake(MotionMode mode, long elapsedMs, double amplitude)
	{
		if (!mode.loops() || elapsedMs < 0 || elapsedMs >= WindowTimes.SHAKE)
			return 0.0;

		double t = (double) elapsedMs / WindowTimes.SHAKE;
		return Math.sin(t * 2.0 * Math.PI * 2.0) * amplitude * (1.0 - t);
	}
}
