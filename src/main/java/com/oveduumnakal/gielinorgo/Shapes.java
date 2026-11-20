/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * The glass look's outlines: the slanted shape of {@link Slant} with soft, rounded tips. A tip is rounded by cutting
 * the corner and joining the cut with a curve whose control point is the corner itself, so a rounded outline always
 * lies inside the plain one. Hit tests keep using {@link Slant#contains} on the plain outline, so rounding a tip
 * never moves what can be clicked. Holds no RuneLite types.
 */
final class Shapes
{
	private Shapes()
	{
	}

	/**
	 * Builds the slanted shape of a box with its four tips rounded.
	 *
	 * @param box the box the shape fits in
	 * @param skew how far the sides lean, in pixels; limited to half the box width
	 * @param radius how far from each corner the rounding starts, in pixels
	 * @return the rounded outline
	 */
	static Path2D roundedSlant(Rectangle2D box, double skew, double radius)
	{
		double s = Math.max(0, Math.min(skew, box.getWidth() / 2));
		double[][] p = {
			{box.getMinX() + s, box.getMinY()}, {box.getMaxX(), box.getMinY()},
			{box.getMaxX() - s, box.getMaxY()}, {box.getMinX(), box.getMaxY()}
		};
		return roundedPoly(p, radius);
	}

	/**
	 * Builds the filled part of a slanted bar with its tips rounded, as {@link Slant#filled} does for sharp ones.
	 *
	 * @param box the box of the whole bar
	 * @param skew how far the sides lean, in pixels
	 * @param fraction the filled share, kept between 0 and 1
	 * @param radius how far from each corner the rounding starts, in pixels
	 * @return the rounded outline of the filled part
	 */
	static Path2D roundedFilled(Rectangle2D box, double skew, double fraction, double radius)
	{
		double f = Math.max(0, Math.min(1, fraction));
		double s = Math.max(0, Math.min(skew, box.getWidth() / 2));
		double edge = box.getMinX() + s + (box.getWidth() - s) * f;
		double[][] p = {
			{box.getMinX() + s, box.getMinY()}, {Math.max(box.getMinX() + s, edge), box.getMinY()},
			{Math.max(box.getMinX(), edge - s), box.getMaxY()}, {box.getMinX(), box.getMaxY()}
		};
		return roundedPoly(p, radius);
	}

	/**
	 * Rounds the corners of a polygon.
	 *
	 * @param p the corners, in order, each as x and y
	 * @param radius how far from each corner, along its sides, the rounding starts; never more than half a side
	 * @return the outline with each corner replaced by a curve
	 */
	static Path2D roundedPoly(double[][] p, double radius)
	{
		Path2D.Double path = new Path2D.Double();
		int n = p.length;
		for (int i = 0; i < n; i++)
		{
			double[] cur = p[i];
			double[] prev = p[(i - 1 + n) % n];
			double[] next = p[(i + 1) % n];
			double la = Math.hypot(prev[0] - cur[0], prev[1] - cur[1]);
			double lb = Math.hypot(next[0] - cur[0], next[1] - cur[1]);
			double da = la == 0 ? 0 : Math.min(radius, la / 2) / la;
			double db = lb == 0 ? 0 : Math.min(radius, lb / 2) / lb;
			double ax = cur[0] + (prev[0] - cur[0]) * da;
			double ay = cur[1] + (prev[1] - cur[1]) * da;
			double bx = cur[0] + (next[0] - cur[0]) * db;
			double by = cur[1] + (next[1] - cur[1]) * db;
			if (i == 0)
				path.moveTo(ax, ay);
			else
				path.lineTo(ax, ay);

			path.quadTo(cur[0], cur[1], bx, by);
		}

		path.closePath();
		return path;
	}

	/**
	 * Gives a key for the form of a shape that does not depend on where it is: its outline measured from the corner
	 * of its bounds, to 1/8 pixel. Two shapes with the same key look the same wherever they sit, so what is drawn for
	 * one can be reused for the other.
	 *
	 * @param shape the shape
	 * @return the key; compare with {@link Arrays#equals(int[], int[])}
	 */
	static int[] formKey(Shape shape)
	{
		Rectangle2D b = shape.getBounds2D();
		int[] key = new int[64];
		int n = 0;
		double[] c = new double[6];
		for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next())
		{
			int type = it.currentSegment(c);
			if (n + 7 > key.length)
				key = Arrays.copyOf(key, key.length * 2);

			key[n++] = type;
			int count = type == PathIterator.SEG_CLOSE ? 0 : type == PathIterator.SEG_QUADTO ? 4
				: type == PathIterator.SEG_CUBICTO ? 6 : 2;
			for (int i = 0; i < count; i += 2)
			{
				key[n++] = (int) Math.round((c[i] - b.getX()) * 8);
				key[n++] = (int) Math.round((c[i + 1] - b.getY()) * 8);
			}
		}

		return Arrays.copyOf(key, n);
	}
}
