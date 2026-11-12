/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Shape;
import java.util.Collection;

/** Hit tests on the canvas shapes of a stand-in. Pure geometry, no game client. */
final class ShapeHits
{
	private ShapeHits()
	{
	}

	/**
	 * Tells whether a point is inside any of some shapes.
	 *
	 * @param shapes the shapes; null entries are skipped
	 * @param x the point's canvas x
	 * @param y the point's canvas y
	 * @return true when at least one shape contains the point
	 */
	static boolean anyContains(Collection<? extends Shape> shapes, int x, int y)
	{
		for (Shape shape : shapes)
		{
			if (shape != null && shape.contains(x, y))
				return true;
		}

		return false;
	}
}
