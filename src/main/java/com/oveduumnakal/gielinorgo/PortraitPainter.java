/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.runelite.api.JagexColor;

/**
 * Draws a small picture of a 3D model without the game's renderer. Every triangle is filled pixel by pixel into a
 * depth buffer, so nearer surfaces hide farther ones exactly, and colours blend smoothly between a triangle's
 * corners the way the game shades them. The picture is drawn at several times its final size and then averaged
 * down, which smooths the edges.
 *
 * <p>It needs nothing from the running game, only the model's numbers, so it can be tested on its own.
 */
final class PortraitPainter
{
	/** Value of a triangle's third corner colour that marks the triangle as not drawn. */
	static final int HIDDEN = -2;

	/** Value of a triangle's third corner colour that marks the triangle as one flat colour. */
	static final int FLAT = -1;

	/** How far the model is turned away from facing the viewer head-on, in degrees. */
	private static final double TURN_DEGREES = 32;

	/** How far the viewer looks down on the model, in degrees. */
	private static final double TILT_DEGREES = 14;

	/** Samples per pixel along each axis. */
	private static final int SAMPLES = 3;

	/** Share of the picture left empty around the model. */
	private static final double MARGIN = 0.06;

	/** Least share of the picture the model must cover to count as drawn. */
	private static final double MIN_COVERAGE = 0.01;

	private static final double HUE_OFFSET = 1.0 / 128.0;
	private static final double SATURATION_OFFSET = 1.0 / 16.0;

	/** A model's geometry and colours, copied out of the game's own model object. */
	static final class Mesh
	{
		private final float[] x;
		private final float[] y;
		private final float[] z;
		private final int[][] corners;
		private final int[][] colors;
		private final byte[] transparencies;

		/**
		 * Wraps a model's arrays.
		 *
		 * @param x vertex x coordinates; east is positive
		 * @param y vertex y coordinates; up is negative
		 * @param z vertex z coordinates; north is positive
		 * @param corners three arrays: for each triangle, the vertex index of its first, second and third corner
		 * @param colors three arrays: for each triangle, the lit colour at each corner in the game's packed
		 *     hue-saturation-lightness; the third is {@link #FLAT} or {@link #HIDDEN} for those special triangles
		 * @param transparencies for each triangle, 0 for solid up to 255 for invisible; may be null
		 */
		Mesh(float[] x, float[] y, float[] z, int[][] corners, int[][] colors, byte[] transparencies)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.corners = corners;
			this.colors = colors;
			this.transparencies = transparencies;
		}

		private int vertices()
		{
			return Math.min(x.length, Math.min(y.length, z.length));
		}

		private int triangles()
		{
			int count = Integer.MAX_VALUE;
			for (int corner = 0; corner < 3; corner++)
				count = Math.min(count, Math.min(corners[corner].length, colors[corner].length));

			return count;
		}

		private double opacity(int triangle)
		{
			if (transparencies == null || triangle >= transparencies.length)
				return 1.0;

			return 1.0 - (transparencies[triangle] & 255) / 255.0;
		}
	}

	/** The supersampled drawing surface: colour, coverage and depth for every sample. */
	private static final class Canvas
	{
		private final int side;
		private final float[] red;
		private final float[] green;
		private final float[] blue;
		private final float[] coverage;
		private final float[] depth;

		private Canvas(int side)
		{
			this.side = side;
			red = new float[side * side];
			green = new float[side * side];
			blue = new float[side * side];
			coverage = new float[side * side];
			depth = new float[side * side];
			Arrays.fill(depth, Float.NEGATIVE_INFINITY);
		}

		/**
		 * Colours one sample if nothing nearer is already there.
		 *
		 * @param index the sample
		 * @param nearness how close the surface is to the viewer; larger is nearer
		 * @param rgb the surface colour, one value per channel from 0 to 1
		 * @param opacity 1 for a solid surface, less for a see-through one
		 */
		private void plot(int index, float nearness, float[] rgb, float opacity)
		{
			if (nearness <= depth[index])
				return;

			red[index] = rgb[0] * opacity + red[index] * (1 - opacity);
			green[index] = rgb[1] * opacity + green[index] * (1 - opacity);
			blue[index] = rgb[2] * opacity + blue[index] * (1 - opacity);
			coverage[index] = opacity + coverage[index] * (1 - opacity);
			if (opacity >= 1f)
				depth[index] = nearness;
		}
	}

	private PortraitPainter()
	{
	}

	/**
	 * Paints a model onto a transparent square image, seen from its front and slightly to one side.
	 *
	 * @param mesh the model
	 * @param size width and height of the image, in pixels
	 * @return the picture, or null when the model has nothing visible to draw
	 */
	static BufferedImage paint(Mesh mesh, int size)
	{
		int vertices = mesh.vertices();
		int triangles = mesh.triangles();
		if (vertices == 0 || triangles == 0)
			return null;

		double[][] view = view(mesh, vertices);
		Canvas canvas = new Canvas(size * SAMPLES);
		fitToCanvas(view, vertices, canvas.side);
		List<Integer> seeThrough = new ArrayList<>();
		for (int triangle = 0; triangle < triangles; triangle++)
		{
			if (!drawable(mesh, triangle, vertices))
				continue;

			if (mesh.opacity(triangle) >= 1.0)
				fill(canvas, mesh, view, triangle);
			else
				seeThrough.add(triangle);
		}

		seeThrough.sort((first, second) -> Double.compare(nearness(mesh, view, first), nearness(mesh, view, second)));
		for (int triangle : seeThrough)
			fill(canvas, mesh, view, triangle);

		return shrink(canvas, size);
	}

	/**
	 * Converts one of the game's packed colours to red, green and blue.
	 *
	 * @param packed hue in the top 6 bits, saturation in the next 3, lightness in the low 7
	 * @return the colour
	 */
	static Color color(int packed)
	{
		short hsl = (short) packed;
		double hue = JagexColor.unpackHue(hsl) / (JagexColor.HUE_MAX + 1.0) + HUE_OFFSET;
		double saturation = JagexColor.unpackSaturation(hsl) / (JagexColor.SATURATION_MAX + 1.0) + SATURATION_OFFSET;
		double lightness = JagexColor.unpackLuminance(hsl) / (JagexColor.LUMINANCE_MAX + 1.0);
		double brightness = lightness + saturation * Math.min(lightness, 1 - lightness);
		double vividness = brightness <= 0 ? 0 : 2 * (1 - lightness / brightness);
		return new Color(Color.HSBtoRGB((float) hue, (float) vividness, (float) brightness));
	}

	/**
	 * Turns the model to the portrait angle.
	 *
	 * @param mesh the model
	 * @param vertices how many vertices it has
	 * @return three arrays: for each vertex, how far right it is, how far up, and how near the viewer
	 */
	private static double[][] view(Mesh mesh, int vertices)
	{
		double turn = Math.toRadians(TURN_DEGREES);
		double tilt = Math.toRadians(TILT_DEGREES);
		double[][] view = new double[3][vertices];
		for (int i = 0; i < vertices; i++)
		{
			double east = mesh.x[i];
			double up = -mesh.y[i];
			double south = -mesh.z[i];
			double right = east * Math.cos(turn) - south * Math.sin(turn);
			double toward = east * Math.sin(turn) + south * Math.cos(turn);
			view[0][i] = right;
			view[1][i] = up * Math.cos(tilt) - toward * Math.sin(tilt);
			view[2][i] = toward * Math.cos(tilt) + up * Math.sin(tilt);
		}

		return view;
	}

	/**
	 * Moves and scales the turned model so it fills the canvas, with a margin, and puts up at the top.
	 *
	 * @param view the turned vertices; right and up are replaced by canvas coordinates
	 * @param vertices how many vertices there are
	 * @param side the canvas width and height, in samples
	 */
	private static void fitToCanvas(double[][] view, int vertices, int side)
	{
		double[] low = {Double.MAX_VALUE, Double.MAX_VALUE};
		double[] high = {-Double.MAX_VALUE, -Double.MAX_VALUE};
		for (int axis = 0; axis < 2; axis++)
		{
			for (int i = 0; i < vertices; i++)
			{
				low[axis] = Math.min(low[axis], view[axis][i]);
				high[axis] = Math.max(high[axis], view[axis][i]);
			}
		}

		double widest = Math.max(1.0, Math.max(high[0] - low[0], high[1] - low[1]));
		double scale = side * (1 - 2 * MARGIN) / widest;
		for (int i = 0; i < vertices; i++)
		{
			view[0][i] = side / 2.0 + (view[0][i] - (low[0] + high[0]) / 2.0) * scale;
			view[1][i] = side / 2.0 - (view[1][i] - (low[1] + high[1]) / 2.0) * scale;
		}
	}

	private static boolean drawable(Mesh mesh, int triangle, int vertices)
	{
		if (mesh.colors[2][triangle] == HIDDEN || mesh.opacity(triangle) <= 0)
			return false;

		for (int corner = 0; corner < 3; corner++)
		{
			int vertex = mesh.corners[corner][triangle];
			if (vertex < 0 || vertex >= vertices)
				return false;
		}

		return true;
	}

	private static double nearness(Mesh mesh, double[][] view, int triangle)
	{
		double sum = 0;
		for (int corner = 0; corner < 3; corner++)
			sum += view[2][mesh.corners[corner][triangle]];

		return sum;
	}

	/**
	 * Fills one triangle, sample by sample. Each sample's colour and depth are a weighted mix of the three
	 * corners, by how close the sample is to each.
	 *
	 * @param canvas the drawing surface
	 * @param mesh the model
	 * @param view the model's vertices in canvas coordinates
	 * @param triangle the triangle to fill
	 */
	private static void fill(Canvas canvas, Mesh mesh, double[][] view, int triangle)
	{
		double[] px = new double[3];
		double[] py = new double[3];
		float[] near = new float[3];
		float[][] rgb = new float[3][];
		boolean flat = mesh.colors[2][triangle] == FLAT;
		for (int corner = 0; corner < 3; corner++)
		{
			int vertex = mesh.corners[corner][triangle];
			px[corner] = view[0][vertex];
			py[corner] = view[1][vertex];
			near[corner] = (float) view[2][vertex];
			rgb[corner] = color(mesh.colors[flat ? 0 : corner][triangle]).getRGBColorComponents(null);
		}

		double area = cross(px[0], py[0], px[1], py[1], px[2], py[2]);
		if (area == 0)
			return;

		int left = Math.max(0, (int) Math.floor(Math.min(px[0], Math.min(px[1], px[2]))));
		int right = Math.min(canvas.side - 1, (int) Math.ceil(Math.max(px[0], Math.max(px[1], px[2]))));
		int top = Math.max(0, (int) Math.floor(Math.min(py[0], Math.min(py[1], py[2]))));
		int bottom = Math.min(canvas.side - 1, (int) Math.ceil(Math.max(py[0], Math.max(py[1], py[2]))));
		float opacity = (float) mesh.opacity(triangle);
		float[] mixed = new float[3];
		for (int row = top; row <= bottom; row++)
		{
			for (int column = left; column <= right; column++)
			{
				double sx = column + 0.5;
				double sy = row + 0.5;
				double w0 = cross(px[1], py[1], px[2], py[2], sx, sy) / area;
				double w1 = cross(px[2], py[2], px[0], py[0], sx, sy) / area;
				double w2 = 1 - w0 - w1;
				if (w0 < 0 || w1 < 0 || w2 < 0)
					continue;

				for (int channel = 0; channel < 3; channel++)
					mixed[channel] = (float) (w0 * rgb[0][channel] + w1 * rgb[1][channel] + w2 * rgb[2][channel]);

				float nearness = (float) (w0 * near[0] + w1 * near[1] + w2 * near[2]);
				canvas.plot(row * canvas.side + column, nearness, mixed, opacity);
			}
		}
	}

	/**
	 * Twice the signed area of the triangle a-b-c. Its sign says which side of the line a-b the point c is on.
	 *
	 * @param ax first point x
	 * @param ay first point y
	 * @param bx second point x
	 * @param by second point y
	 * @param cx third point x
	 * @param cy third point y
	 * @return the cross product of a-b and a-c
	 */
	private static double cross(double ax, double ay, double bx, double by, double cx, double cy)
	{
		return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
	}

	/**
	 * Averages the samples down to the final picture.
	 *
	 * @param canvas the finished drawing surface
	 * @param size width and height of the picture, in pixels
	 * @return the picture, or null when almost nothing was drawn
	 */
	private static BufferedImage shrink(Canvas canvas, int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double covered = 0;
		for (int py = 0; py < size; py++)
		{
			for (int px = 0; px < size; px++)
			{
				float[] sum = new float[4];
				for (int sample = 0; sample < SAMPLES * SAMPLES; sample++)
				{
					int index = (py * SAMPLES + sample / SAMPLES) * canvas.side + px * SAMPLES + sample % SAMPLES;
					sum[0] += canvas.red[index] * canvas.coverage[index];
					sum[1] += canvas.green[index] * canvas.coverage[index];
					sum[2] += canvas.blue[index] * canvas.coverage[index];
					sum[3] += canvas.coverage[index];
				}

				if (sum[3] <= 0)
					continue;

				float alpha = sum[3] / (SAMPLES * SAMPLES);
				covered += alpha;
				image.setRGB(px, py, new Color(clamp(sum[0] / sum[3]), clamp(sum[1] / sum[3]),
					clamp(sum[2] / sum[3]), clamp(alpha)).getRGB());
			}
		}

		return covered < MIN_COVERAGE * size * size ? null : image;
	}

	private static float clamp(float value)
	{
		return Math.max(0f, Math.min(1f, value));
	}
}
