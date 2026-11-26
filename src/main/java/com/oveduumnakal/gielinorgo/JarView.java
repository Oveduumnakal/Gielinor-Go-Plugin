/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * How a jar is looked at: how its points are turned toward the viewer and where they land on the picture.
 * {@link JarFixedView} is the still camera of a picture and {@link JarLiveView} the game's own. A turned point has x to
 * the right, y up and z toward the viewer. Holds no RuneLite types.
 */
interface JarView
{
	/**
	 * Turns a point or a direction of the jar as the jar and the camera are turned.
	 *
	 * @param point x, y and z in the jar's units
	 * @return the turned point, measured from the jar's turning point
	 */
	double[] view(double[] point);

	/**
	 * Turns a point or a direction of a highlight. A highlight tips with the jar but does not spin with it, so it stays
	 * on the side that faces the viewer, like a reflection.
	 *
	 * @param point x, y and z in the jar's units
	 * @return the turned point
	 */
	double[] reflection(double[] point);

	/** @return how strongly highlights show, from 0 to 1 */
	double reflectionStrength();

	/**
	 * Places a turned point on the picture.
	 *
	 * @param turned a point from {@link #view(double[])} or {@link #reflection(double[])}
	 * @return x and y in pixels
	 */
	double[] screen(double[] turned);

	/**
	 * Tells whether a surface can be seen.
	 *
	 * @param at a turned point on it
	 * @param normal the turned direction it faces
	 * @return whether it faces the viewer
	 */
	boolean faces(double[] at, double[] normal);

	/** @return how many pixels one of the jar's units covers at the jar */
	double pixelsPerUnit();
}
