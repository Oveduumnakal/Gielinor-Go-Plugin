/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The one pop-up notice on screen: what it says, when it appeared and where it is centred. A newer notice replaces the
 * older one. The overlay holds one in a volatile field and asks it for the look of the moment. Immutable, and the
 * time is passed in, like {@link NoticeClock}. Holds no RuneLite types.
 */
final class NoticeSlot
{
	private final NoticeMessage message;
	private final long startMs;
	private final int centerX;
	private final int centerY;

	/**
	 * Makes a notice.
	 *
	 * @param message what it says
	 * @param startMs when it appeared, in milliseconds
	 * @param centerX the centre x on the canvas, where the window was or under the toggle
	 * @param centerY the centre y on the canvas
	 */
	NoticeSlot(NoticeMessage message, long startMs, int centerX, int centerY)
	{
		this.message = message;
		this.startMs = startMs;
		this.centerX = centerX;
		this.centerY = centerY;
	}

	/** @return what the notice says */
	NoticeMessage message()
	{
		return message;
	}

	/** @return the centre x on the canvas */
	int centerX()
	{
		return centerX;
	}

	/** @return the centre y on the canvas */
	int centerY()
	{
		return centerY;
	}

	/**
	 * Gives the look of the notice at a time.
	 *
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 * @return the scale and opacity, and whether it is still on screen
	 */
	NoticeClock.Frame frame(MotionMode mode, long nowMs)
	{
		return NoticeClock.at(mode, nowMs - startMs);
	}

	/**
	 * Keeps a notice's centre far enough from the canvas edges that all of it shows.
	 *
	 * @param center the wanted centre
	 * @param half half the notice's size along this axis
	 * @param canvas the canvas size along this axis
	 * @return the centre to draw at; the middle of the canvas when the notice is wider than the canvas
	 */
	static int clampCenter(int center, int half, int canvas)
	{
		if (2 * half >= canvas)
			return canvas / 2;

		return Math.max(half, Math.min(canvas - half, center));
	}
}
