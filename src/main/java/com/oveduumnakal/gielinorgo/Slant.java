/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The slanted shape of a row, a tab or a bar: a rectangle with both sides leaning like a "/". The top edge starts
 * {@code skew} to the right of the box's left side, and the bottom edge ends {@code skew} to the left of its right
 * side, so the shape always fits inside its box. Used for painting, as a clip for a portrait, and for hit tests, so
 * what is clicked is what is drawn. Holds no RuneLite types.
 */
final class Slant
{
	private Slant()
	{
	}

	/**
	 * Builds the slanted shape of a box.
	 *
	 * @param box the box the shape fits in
	 * @param skew how far the sides lean, in pixels; limited to half the box width
	 * @return the shape, a parallelogram
	 */
	static Path2D.Double shape(Rectangle2D box, double skew)
	{
		double s = limit(box, skew);
		Path2D.Double path = new Path2D.Double();
		path.moveTo(box.getMinX() + s, box.getMinY());
		path.lineTo(box.getMaxX(), box.getMinY());
		path.lineTo(box.getMaxX() - s, box.getMaxY());
		path.lineTo(box.getMinX(), box.getMaxY());
		path.closePath();
		return path;
	}

	/**
	 * Builds the slanted shape of a box given as numbers.
	 *
	 * @param x the left edge of the box
	 * @param y the top edge
	 * @param w the width
	 * @param h the height
	 * @param skew how far the sides lean, in pixels
	 * @return the shape, a parallelogram
	 */
	static Path2D.Double shape(double x, double y, double w, double h, double skew)
	{
		return shape(new Rectangle2D.Double(x, y, w, h), skew);
	}

	/**
	 * Tells whether a point is inside the slanted shape of a box, for hit tests.
	 *
	 * @param px the point's x
	 * @param py the point's y
	 * @param box the box the shape fits in
	 * @param skew how far the sides lean, in pixels
	 * @return true when the point is inside the shape
	 */
	static boolean contains(double px, double py, Rectangle2D box, double skew)
	{
		if (box.getHeight() <= 0 || py < box.getMinY() || py > box.getMaxY())
			return false;

		return px >= leftAt(py, box, skew) && px <= rightAt(py, box, skew);
	}

	/**
	 * @param y a height inside the box
	 * @param box the box the shape fits in
	 * @param skew how far the sides lean, in pixels
	 * @return the x of the shape's left edge at that height
	 */
	static double leftAt(double y, Rectangle2D box, double skew)
	{
		return box.getMinX() + limit(box, skew) * (1.0 - along(y, box));
	}

	/**
	 * @param y a height inside the box
	 * @param box the box the shape fits in
	 * @param skew how far the sides lean, in pixels
	 * @return the x of the shape's right edge at that height
	 */
	static double rightAt(double y, Rectangle2D box, double skew)
	{
		return box.getMaxX() - limit(box, skew) * along(y, box);
	}

	/**
	 * Gives the part of a slanted box that a bar fills, still slanted the same way.
	 *
	 * @param box the box of the whole bar
	 * @param skew how far the sides lean, in pixels
	 * @param fraction the filled share, kept between 0 and 1
	 * @return the filled shape; its right edge leans like the sides
	 */
	static Path2D.Double filled(Rectangle2D box, double skew, double fraction)
	{
		double f = Math.max(0.0, Math.min(1.0, fraction));
		double s = limit(box, skew);
		double edge = box.getMinX() + s + (box.getWidth() - s) * f;
		Path2D.Double path = new Path2D.Double();
		path.moveTo(box.getMinX() + s, box.getMinY());
		path.lineTo(Math.max(box.getMinX() + s, edge), box.getMinY());
		path.lineTo(Math.max(box.getMinX(), edge - s), box.getMaxY());
		path.lineTo(box.getMinX(), box.getMaxY());
		path.closePath();
		return path;
	}

	private static double limit(Rectangle2D box, double skew)
	{
		return Math.max(0.0, Math.min(skew, box.getWidth() / 2.0));
	}

	private static double along(double y, Rectangle2D box)
	{
		return box.getHeight() <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (y - box.getMinY()) / box.getHeight()));
	}
}
