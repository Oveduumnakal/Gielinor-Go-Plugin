/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * The size rules of the painted window. The window is designed in an 800 x 520 unit space and drawn at one scale,
 * {@code min(width / 800, height / 520)}. Its size is stored only as a fraction of the canvas width, so it follows the
 * game window; after a drag-resize the new width becomes the new fraction, and a free-form drag snaps back to the
 * 800:520 shape. The window is never larger than the canvas. Below {@link #MIN_SCALE} the smallest text would drop
 * under the RuneScape fonts' native pixel size, so the window does not shrink past it, except where the canvas itself
 * is smaller. Holds no RuneLite types.
 */
final class WindowFit
{
	/** The width of the design space, in layout units. */
	static final int DESIGN_W = 800;

	/** The height of the design space, in layout units. */
	static final int DESIGN_H = 520;

	/** The share of the canvas width the window takes until the player changes it. */
	static final double DEFAULT_FRACTION = 0.70;

	/** The smallest share the stored fraction can hold. */
	static final double MIN_FRACTION = 0.05;

	/**
	 * The smallest scale: the one at which body text, {@link WindowFonts#BODY_UNITS} units tall, is the native pixel
	 * size of the RuneScape fonts. Below it the text could not be drawn sharp and the window stops shrinking.
	 */
	static final double MIN_SCALE = (double) WindowFonts.NATIVE_PX / WindowFonts.BODY_UNITS;

	private WindowFit()
	{
	}

	/**
	 * Gives the drawing scale of a window.
	 *
	 * @param width the window width in pixels
	 * @param height the window height in pixels
	 * @return how many pixels one layout unit is, never negative
	 */
	static double scale(int width, int height)
	{
		return Math.max(0.0, Math.min((double) width / DESIGN_W, (double) height / DESIGN_H));
	}

	/**
	 * Gives the biggest window of the right shape that fits in a box.
	 *
	 * @param width the box width in pixels
	 * @param height the box height in pixels
	 * @return the size, 800:520 in shape, never larger than the box
	 */
	static Dimension fitSize(int width, int height)
	{
		int w = Math.max(0, width);
		int h = Math.max(0, height);
		if ((long) w * DESIGN_H <= (long) h * DESIGN_W)
			return new Dimension(w, (int) ((long) w * DESIGN_H / DESIGN_W));

		return new Dimension((int) ((long) h * DESIGN_W / DESIGN_H), h);
	}

	/**
	 * Snaps a size to the window's shape, never above the canvas and never below the minimum scale. When the canvas
	 * is smaller than the minimum window, the canvas wins.
	 *
	 * @param width the wanted width in pixels
	 * @param height the wanted height in pixels
	 * @param canvasW the canvas width in pixels
	 * @param canvasH the canvas height in pixels
	 * @return the size to use
	 */
	static Dimension clamp(int width, int height, int canvasW, int canvasH)
	{
		Dimension cap = fitSize(canvasW, canvasH);
		double s = Math.max(scale(width, height), MIN_SCALE);
		int w = (int) Math.round(DESIGN_W * s);
		int h = (int) Math.round(DESIGN_H * s);
		if (w > cap.width || h > cap.height)
			return cap;

		return new Dimension(w, h);
	}

	/**
	 * Gives the window size for a stored fraction of the canvas width.
	 *
	 * @param fraction the share of the canvas width
	 * @param canvasW the canvas width in pixels
	 * @param canvasH the canvas height in pixels
	 * @return the size, snapped and clamped as {@link #clamp} does
	 */
	static Dimension sizeFor(double fraction, int canvasW, int canvasH)
	{
		int w = (int) Math.round(clampFraction(fraction) * Math.max(0, canvasW));
		return clamp(w, (int) Math.round(w * (double) DESIGN_H / DESIGN_W), canvasW, canvasH);
	}

	/**
	 * Gives the fraction a window width stands for.
	 *
	 * @param windowW the window width in pixels
	 * @param canvasW the canvas width in pixels
	 * @return the share of the canvas width, kept between {@link #MIN_FRACTION} and 1
	 */
	static double fractionOf(int windowW, int canvasW)
	{
		if (canvasW <= 0)
			return DEFAULT_FRACTION;

		return clampFraction((double) windowW / canvasW);
	}

	/**
	 * Gives the fraction to store after the player drag-resized the window: the size is snapped back to the window's
	 * shape first, so the fraction is of the shape the player will see.
	 *
	 * @param width the width after the drag, in pixels
	 * @param height the height after the drag, in pixels
	 * @param canvasW the canvas width in pixels
	 * @param canvasH the canvas height in pixels
	 * @return the new fraction of the canvas width
	 */
	static double fractionAfterResize(int width, int height, int canvasW, int canvasH)
	{
		return fractionOf(clamp(width, height, canvasW, canvasH).width, canvasW);
	}

	/**
	 * Gives the top-left corner that centres a window in the canvas.
	 *
	 * @param size the window size
	 * @param canvasW the canvas width in pixels
	 * @param canvasH the canvas height in pixels
	 * @return the corner, never negative
	 */
	static Point centered(Dimension size, int canvasW, int canvasH)
	{
		return new Point(Math.max(0, (canvasW - size.width) / 2), Math.max(0, (canvasH - size.height) / 2));
	}

	/**
	 * Gives the window Alt and a right click on the window restore: the default fraction, centred.
	 *
	 * @param canvasW the canvas width in pixels
	 * @param canvasH the canvas height in pixels
	 * @return the bounds in canvas pixels
	 */
	static Rectangle reset(int canvasW, int canvasH)
	{
		return preset(WindowSizePreset.MEDIUM, canvasW, canvasH);
	}

	/**
	 * Gives the window a size preset asks for, centred.
	 *
	 * @param preset the preset
	 * @param canvasW the canvas width in pixels
	 * @param canvasH the canvas height in pixels
	 * @return the bounds in canvas pixels
	 */
	static Rectangle preset(WindowSizePreset preset, int canvasW, int canvasH)
	{
		Dimension size = sizeFor(preset.fraction(), canvasW, canvasH);
		return new Rectangle(centered(size, canvasW, canvasH), size);
	}

	private static double clampFraction(double fraction)
	{
		return Math.max(MIN_FRACTION, Math.min(1.0, fraction));
	}
}
