/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the faces of a jar: turns an outline round the jar's axis, wraps a flat shape onto the glass and bends it to
 * follow the shoulder, and makes the small pieces inside. Holds no RuneLite types.
 */
final class JarLathe
{
	/** How many flat sides a turned shape has. */
	static final int SIDES = 12;

	/** The height, in the jar's units, of the point the jar turns about. */
	static final double PIVOT = 56;

	/** The outline of the body and neck as pairs of radius and height; every jar has the same. */
	static final double[][] BODY = {{0, 0}, {33, 0}, {40, 5}, {42, 14}, {42, 50}, {37, 60}, {22, 67}, {20, 84}};

	/** The widest radius of the body. */
	private static final double BELLY = 42;

	/** The first point of {@link #BODY} of the straight side, where the shoulder is measured from. */
	private static final int SIDE_FROM = 3;

	/** The point of {@link #BODY} where the shoulder starts to slope. */
	private static final int SHOULDER = 4;

	private JarLathe()
	{
	}

	/**
	 * Turns an outline round the axis.
	 *
	 * @param faces where the faces go
	 * @param profile pairs of radius and height, from the bottom up or the inside out
	 * @param layer what the shape is part of
	 * @param color its colour
	 * @param order its painting order
	 */
	static void lathe(List<JarModel.Face> faces, double[][] profile, JarModel.Layer layer, Color color, int order)
	{
		for (int i = 0; i + 1 < profile.length; i++)
		{
			int cap = 0;
			if (profile[i][1] == profile[i + 1][1])
				cap = profile[i][0] < profile[i + 1][0] ? -1 : 1;

			for (int side = 0; side < SIDES; side++)
			{
				double[] a = ring(profile[i], side);
				double[] b = ring(profile[i], side + 1);
				double[] c = ring(profile[i + 1], side + 1);
				double[] d = ring(profile[i + 1], side);
				double[][] points = {a, b, c, d};
				if (profile[i][0] == 0)
					points = new double[][] {a, c, d};
				else if (profile[i + 1][0] == 0)
					points = new double[][] {a, b, c};

				faces.add(new JarModel.Face(layer, color, order, cap, points));
			}
		}
	}

	/**
	 * Wraps a flat outline onto the faceted body by cutting it at each facet edge.
	 *
	 * @param faces where the faces go
	 * @param outline pairs of distance round the jar and height
	 * @param turn how far round the jar the middle of the outline sits, in radians; 0 is the front
	 * @param radius how far from the axis it lies
	 * @param color its colour
	 * @param layer what it is part of
	 * @param order its painting order
	 */
	static void wrap(List<JarModel.Face> faces, double[][] outline, double turn, double radius, Color color,
		JarModel.Layer layer, int order)
	{
		double step = 2 * Math.PI / SIDES;
		double flat = radius * Math.cos(step / 2);
		double width = 2 * radius * Math.sin(step / 2);
		for (int facet = -2; facet <= 2; facet++)
		{
			double mid = facet * width;
			List<double[]> right = clip(Arrays.asList(outline), 0, mid - width / 2, 1);
			List<double[]> piece = clip(right, 0, mid + width / 2, -1);
			if (piece.size() < 3)
				continue;

			double angle = facet * step + turn;
			double[][] points = new double[piece.size()][];
			for (int i = 0; i < points.length; i++)
			{
				double along = piece.get(i)[0] - mid;
				points[i] = new double[] {Math.sin(angle) * flat + Math.cos(angle) * along, piece.get(i)[1] - PIVOT,
					Math.cos(angle) * flat - Math.sin(angle) * along};
			}

			faces.add(new JarModel.Face(layer, color, order, 0, points));
		}
	}

	/**
	 * Keeps the part of a polygon on one side of a value of one coordinate.
	 *
	 * @param in the polygon's corners
	 * @param axis which coordinate
	 * @param at the value to cut at
	 * @param keep 1 to keep what lies above the value, -1 what lies below
	 * @return the corners of what is left
	 */
	static List<double[]> clip(List<double[]> in, int axis, double at, int keep)
	{
		List<double[]> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++)
		{
			double[] p = in.get(i);
			double[] q = in.get((i + 1) % in.size());
			double dp = (p[axis] - at) * keep;
			double dq = (q[axis] - at) * keep;
			if (dp >= 0)
				out.add(p);

			if (dp * dq < 0)
			{
				double t = dp / (dp - dq);
				double[] cut = new double[p.length];
				for (int k = 0; k < p.length; k++)
					cut[k] = p[k] + (q[k] - p[k]) * t;

				out.add(cut);
			}
		}

		return out;
	}

	/**
	 * Cuts each wrapped shape where the body bends, then pulls it in to the body's radius at that height.
	 *
	 * @param in the wrapped faces
	 * @return the faces bent to the glass, for views from the side
	 */
	static List<JarModel.Face> onGlass(List<JarModel.Face> in)
	{
		double[] cuts = {-1000, BODY[SHOULDER][1] - PIVOT, BODY[SHOULDER + 1][1] - PIVOT, 1000};
		List<JarModel.Face> out = new ArrayList<>();
		for (JarModel.Face face : in)
		{
			for (int part = 0; part + 1 < cuts.length; part++)
			{
				List<double[]> above = clip(Arrays.asList(face.points()), 1, cuts[part], 1);
				List<double[]> piece = clip(above, 1, cuts[part + 1], -1);
				if (piece.size() < 3)
					continue;

				double[][] points = new double[piece.size()][];
				for (int i = 0; i < points.length; i++)
				{
					double[] p = piece.get(i);
					double pull = radius(p[1] + PIVOT) / BELLY;
					points[i] = new double[] {p[0] * pull, p[1], p[2] * pull};
				}

				out.add(new JarModel.Face(face.layer(), face.color(), face.order(), face.cap(), points));
			}
		}

		return out;
	}

	/**
	 * Makes a small eight-sided piece, such as a pebble, a spark or a shard.
	 *
	 * @param faces where the faces go
	 * @param at its middle as x, height and z, the height counted from the bottom of the jar
	 * @param size half its width, height and depth
	 * @param lean how far it is tipped, in radians
	 * @param color its colour
	 */
	static void gem(List<JarModel.Face> faces, double[] at, double[] size, double lean, Color color)
	{
		double[][] v = {{size[0], 0, 0}, {-size[0], 0, 0}, {0, size[1], 0}, {0, -size[1], 0}, {0, 0, size[2]},
			{0, 0, -size[2]}};
		for (double[] p : v)
		{
			double a = p[0] * Math.cos(lean) - p[1] * Math.sin(lean);
			double b = p[0] * Math.sin(lean) + p[1] * Math.cos(lean);
			p[0] = at[0] + a;
			p[1] = at[1] - PIVOT + b;
			p[2] += at[2];
		}

		int[][] sides = {{0, 2, 4}, {4, 2, 1}, {1, 2, 5}, {5, 2, 0}, {4, 3, 0}, {1, 3, 4}, {5, 3, 1}, {0, 3, 5}};
		for (int[] i : sides)
			faces.add(new JarModel.Face(JarModel.Layer.CONTENT, color, 0, 0, v[i[0]], v[i[1]], v[i[2]]));
	}

	private static double[] ring(double[] point, int side)
	{
		double angle = (side - 0.5) * 2 * Math.PI / SIDES;
		return new double[] {Math.sin(angle) * point[0], point[1] - PIVOT, Math.cos(angle) * point[0]};
	}

	private static double radius(double height)
	{
		for (int i = SIDE_FROM; i + 1 < BODY.length; i++)
		{
			if (height <= BODY[i + 1][1])
			{
				double along = Math.max(0, height - BODY[i][1]) / (BODY[i + 1][1] - BODY[i][1]);
				return Math.min(BELLY, BODY[i][0] + (BODY[i + 1][0] - BODY[i][0]) * along);
			}
		}

		return BODY[BODY.length - 1][0];
	}
}
