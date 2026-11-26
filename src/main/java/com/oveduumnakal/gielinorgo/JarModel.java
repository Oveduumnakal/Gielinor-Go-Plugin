/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A jar as a list of flat faces. Coordinates are in the jar's own units: x to the right, y up and z toward the viewer,
 * with the origin at the point the jar turns about. Holds no RuneLite types.
 */
final class JarModel
{
	/** What a face is part of, which decides when and how it is painted. */
	enum Layer
	{
		/** The see-through body and neck. */
		GLASS,
		/** The lip and the cork. */
		STOPPER,
		/** The liquid some jars hold. */
		LIQUID,
		/** What floats or lies inside. */
		CONTENT,
		/** The metal band of a tier jar. */
		BAND,
		/** The painted highlights, which behave like reflections. */
		SHINE,
		/** The emblem of a specialty jar. */
		EMBLEM
	}

	/** One flat face. */
	static final class Face
	{
		private final Layer layer;
		private final Color color;
		private final double[][] points;
		private final int order;
		private final int cap;

		/**
		 * Describes a face.
		 *
		 * @param layer what it is part of
		 * @param color its colour before shading
		 * @param order faces of a layer with a higher order are painted later; on the stopper, 0 is the lip and 1 the
		 *     cork
		 * @param cap for a flat cap of a turned shape 1 when it faces up and -1 when down, else 0
		 * @param points its corners, each an x, y and z
		 */
		Face(Layer layer, Color color, int order, int cap, double[]... points)
		{
			this.layer = layer;
			this.color = color;
			this.order = order;
			this.cap = cap;
			this.points = points;
		}

		/** @return what the face is part of */
		Layer layer()
		{
			return layer;
		}

		/** @return its colour before shading */
		Color color()
		{
			return color;
		}

		/** @return its painting order within the layer */
		int order()
		{
			return order;
		}

		/** @return 1 or -1 for a flat cap facing up or down, else 0 */
		int cap()
		{
			return cap;
		}

		/** @return its corners; callers must not change them */
		double[][] points()
		{
			return points;
		}
	}

	private final List<Face> faces = new ArrayList<>();
	private Color glow;
	private boolean liquid;

	/** @return every face of the jar */
	List<Face> faces()
	{
		return faces;
	}

	/** @return the colour of the glow inside the jar, or null for none */
	Color glow()
	{
		return glow;
	}

	/** @return whether the jar holds liquid, which is painted over the front glass */
	boolean liquid()
	{
		return liquid;
	}

	/**
	 * Lights the jar from inside.
	 *
	 * @param color the colour, its alpha giving the strength
	 */
	void glow(Color color)
	{
		glow = color;
	}

	/** Marks the jar as holding liquid. */
	void fill()
	{
		liquid = true;
	}
}
