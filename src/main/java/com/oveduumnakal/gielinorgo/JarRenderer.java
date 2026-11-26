/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Paints a jar model into a small picture with Java2D: hidden faces culled, the rest painted layer by layer from the
 * back, flat shading, a glow, and a dark ring round the silhouette. It paints several samples per pixel and averages
 * them down, which smooths the edges. Holds no RuneLite types.
 */
final class JarRenderer
{
	/** The colour of the ring round the silhouette, without its alpha. */
	private static final int EDGE_RGB = 0x231C18;

	/** A jar drawn taller than this many pixels has a ring two pixels wide instead of one. */
	private static final double WIDE_RING_ABOVE = 80;

	/** The jar's units across the square a still picture shows, which sets how tall a jar counts as drawn. */
	private static final double FRAME = 150;

	private static final double[] LIGHT = unit(new double[] {-0.5, 0.65, 0.6});

	/** One face as it is seen. */
	private static final class Shown
	{
		private JarModel.Face face;
		private double[][] points;
		private double[] normal;
		private double depth;
		private boolean front;
	}

	/** Everything the painting of one picture shares. */
	private static final class Painting
	{
		private Graphics2D graphics;
		private JarView view;
		private Rectangle area;
		private int samples;
		private double flicker;
		private final List<Shown> faces = new ArrayList<>();
	}

	private JarRenderer()
	{
	}

	/**
	 * Finds the pixels a jar covers.
	 *
	 * @param jar the model
	 * @param view how it is looked at
	 * @return the smallest rectangle round it, with room for the ring
	 */
	static Rectangle bounds(JarModel jar, JarView view)
	{
		double[] low = {Double.MAX_VALUE, Double.MAX_VALUE};
		double[] high = {-Double.MAX_VALUE, -Double.MAX_VALUE};
		for (JarModel.Face face : jar.faces())
		{
			boolean shine = face.layer() == JarModel.Layer.SHINE;
			for (double[] point : face.points())
			{
				double[] at = view.screen(shine ? view.reflection(point) : view.view(point));
				for (int axis = 0; axis < 2; axis++)
				{
					low[axis] = Math.min(low[axis], at[axis]);
					high[axis] = Math.max(high[axis], at[axis]);
				}
			}
		}

		int margin = 3;
		int x = (int) Math.floor(low[0]) - margin;
		int y = (int) Math.floor(low[1]) - margin;
		return new Rectangle(x, y, (int) Math.ceil(high[0]) + margin - x, (int) Math.ceil(high[1]) + margin - y);
	}

	/**
	 * Paints a jar.
	 *
	 * @param jar the model
	 * @param view how it is looked at
	 * @param area the part of the view's pixels the picture covers
	 * @param samples samples along each axis of a pixel
	 * @param flicker from 0 to 1, how bright the glow and the tops of the contents are at this moment
	 * @param edge the alpha of the ring round the silhouette, from 0 to 255
	 * @return the picture, the size of the area
	 */
	static BufferedImage render(JarModel jar, JarView view, Rectangle area, int samples, double flicker, int edge)
	{
		BufferedImage image = new BufferedImage(area.width * samples, area.height * samples,
			BufferedImage.TYPE_INT_ARGB);
		Painting painting = new Painting();
		painting.view = view;
		painting.area = area;
		painting.samples = samples;
		painting.flicker = flicker;
		for (JarModel.Face face : jar.faces())
			painting.faces.add(seen(face, view));

		painting.graphics = image.createGraphics();
		painting.graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		layers(painting, jar);
		painting.graphics.dispose();
		boolean wide = view.pixelsPerUnit() * FRAME > WIDE_RING_ABOVE;
		return shrink(ring(image, wide ? 2 * samples : samples, edge), samples);
	}

	/** Paints the layers from the back of the jar to the front. */
	private static void layers(Painting painting, JarModel jar)
	{
		double[] axis = painting.view.view(new double[] {0, 1, 0});
		boolean corkNearer = painting.view.faces(painting.view.view(new double[] {0, 0, 0}), axis);
		if (!corkNearer)
			stopper(painting, false);

		pass(painting, JarModel.Layer.EMBLEM, false);
		pass(painting, JarModel.Layer.BAND, false);
		pass(painting, JarModel.Layer.GLASS, false);
		if (jar.glow() != null)
			glow(painting, jar.glow());

		if (!jar.liquid())
			pass(painting, JarModel.Layer.CONTENT, true);

		pass(painting, JarModel.Layer.GLASS, true);
		if (jar.liquid())
		{
			pass(painting, JarModel.Layer.LIQUID, true);
			pass(painting, JarModel.Layer.CONTENT, true);
		}

		pass(painting, JarModel.Layer.BAND, true);
		pass(painting, JarModel.Layer.SHINE, true);
		pass(painting, JarModel.Layer.EMBLEM, true);
		if (corkNearer)
			stopper(painting, true);
	}

	/** Turns a face to the viewer and finds which way it faces, pointing its normal out of the jar. */
	private static Shown seen(JarModel.Face face, JarView view)
	{
		boolean shine = face.layer() == JarModel.Layer.SHINE;
		double[][] points = face.points();
		Shown shown = new Shown();
		shown.face = face;
		shown.points = new double[points.length][];
		double[] middle = new double[3];
		double[] turnedMiddle = new double[3];
		for (int i = 0; i < points.length; i++)
		{
			shown.points[i] = shine ? view.reflection(points[i]) : view.view(points[i]);
			for (int k = 0; k < 3; k++)
			{
				middle[k] += points[i][k] / points.length;
				turnedMiddle[k] += shown.points[i][k] / points.length;
			}
		}

		shown.depth = turnedMiddle[2];
		double[] normal = unit(newell(shown.points));
		if (face.layer() == JarModel.Layer.CONTENT)
		{
			if (!view.faces(turnedMiddle, normal))
				normal = new double[] {-normal[0], -normal[1], -normal[2]};
		}
		else
		{
			double[] out = face.cap() != 0
				? new double[] {0, face.cap(), 0}
				: new double[] {middle[0], 0, middle[2]};
			double[] turned = shine ? view.reflection(out) : view.view(out);
			if (normal[0] * turned[0] + normal[1] * turned[1] + normal[2] * turned[2] < 0)
				normal = new double[] {-normal[0], -normal[1], -normal[2]};
		}

		shown.normal = normal;
		shown.front = view.faces(turnedMiddle, normal);
		return shown;
	}

	/** Paints the lip and the cork as whole parts, the farther one first. */
	private static void stopper(Painting painting, boolean corkNearer)
	{
		List<Shown> picked = new ArrayList<>();
		for (Shown shown : painting.faces)
		{
			if (shown.face.layer() == JarModel.Layer.STOPPER && shown.front)
				picked.add(shown);
		}

		Comparator<Shown> part = Comparator.comparingInt(s -> (s.face.order() == 1) == corkNearer ? 1 : 0);
		picked.sort(part.thenComparingDouble(s -> s.depth));
		for (Shown shown : picked)
			fill(painting, shown, shade(shown, shown.face.color()), 1.0, true);
	}

	private static void pass(Painting painting, JarModel.Layer layer, boolean front)
	{
		List<Shown> picked = new ArrayList<>();
		for (Shown shown : painting.faces)
		{
			if (shown.face.layer() == layer && shown.front == front)
				picked.add(shown);
		}

		Comparator<Shown> order = Comparator.comparingInt(s -> s.face.order());
		picked.sort(order.thenComparingDouble(s -> s.depth));
		for (Shown shown : picked)
			paint(painting, shown, front);
	}

	private static void paint(Painting painting, Shown shown, boolean front)
	{
		Color color = shown.face.color();
		double[] normal = shown.normal;
		switch (shown.face.layer())
		{
			case GLASS:
			{
				double rim = 1 - Math.abs(normal[2]);
				double spec = Math.pow(Math.max(0, reflected(normal)), 14);
				Color tinted = mix(mix(shade(shown, color), color, 0.55), Color.WHITE, spec);
				double alpha = Math.min(1, (front ? 0.20 : 0.56) + 0.50 * rim * rim + 0.6 * spec);
				fill(painting, shown, tinted, alpha, false);
				break;
			}
			case LIQUID:
			{
				Color body = normal[1] > 0.8 ? mix(color, Color.WHITE, 0.25) : mix(color, shade(shown, color), 0.55);
				fill(painting, shown, body, 0.82, false);
				break;
			}
			case CONTENT:
			{
				double gleam = 0.18 * painting.flicker * Math.max(0, normal[1]);
				fill(painting, shown, mix(mix(color, shade(shown, color), 0.3), Color.WHITE, gleam), 1.0, true);
				break;
			}
			case SHINE:
				fill(painting, shown, color, color.getAlpha() / 255.0 * painting.view.reflectionStrength(), false);
				break;
			default:
				fill(painting, shown, mix(color, shade(shown, color), 0.35), front ? 1.0 : 0.30, front);
				break;
		}
	}

	private static void glow(Painting painting, Color color)
	{
		double[] at = painting.view.screen(painting.view.view(new double[] {0, 18 - JarLathe.PIVOT, 0}));
		float radius = (float) ((38 + 4 * painting.flicker) * painting.view.pixelsPerUnit() * painting.samples);
		float x = (float) ((at[0] - painting.area.x) * painting.samples);
		float y = (float) ((at[1] - painting.area.y) * painting.samples);
		Color clear = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
		painting.graphics.setComposite(AlphaComposite.SrcAtop);
		painting.graphics.setPaint(new RadialGradientPaint(x, y, radius, new float[] {0f, 1f},
			new Color[] {color, clear}));
		painting.graphics.fill(new Ellipse2D.Float(x - radius, y - radius, 2 * radius, 2 * radius));
	}

	private static void fill(Painting painting, Shown shown, Color color, double alpha, boolean seal)
	{
		Path2D path = new Path2D.Double();
		for (int i = 0; i < shown.points.length; i++)
		{
			double[] at = painting.view.screen(shown.points[i]);
			double x = (at[0] - painting.area.x) * painting.samples;
			double y = (at[1] - painting.area.y) * painting.samples;
			if (i == 0)
				path.moveTo(x, y);
			else
				path.lineTo(x, y);
		}

		path.closePath();
		float solid = (float) Math.max(0, Math.min(1, alpha));
		painting.graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, solid));
		painting.graphics.setColor(color);
		painting.graphics.fill(path);
		if (seal)
			painting.graphics.draw(path);
	}

	private static double reflected(double[] normal)
	{
		double d = normal[0] * LIGHT[0] + normal[1] * LIGHT[1] + normal[2] * LIGHT[2];
		return 2 * d * normal[2] - LIGHT[2];
	}

	private static Color shade(Shown shown, Color base)
	{
		double[] n = shown.normal;
		double lit = Math.max(0, n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * LIGHT[2]);
		double f = 0.52 + 0.62 * lit;
		return new Color(channel(base.getRed() * f), channel(base.getGreen() * f), channel(base.getBlue() * f));
	}

	private static int channel(double value)
	{
		return (int) Math.max(0, Math.min(255, value));
	}

	private static Color mix(Color a, Color b, double amount)
	{
		double t = Math.max(0, Math.min(1, amount));
		return new Color(channel(a.getRed() + (b.getRed() - a.getRed()) * t),
			channel(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
			channel(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	/** Puts a dark ring round the silhouette, faded as a whole to the given alpha. */
	private static BufferedImage ring(BufferedImage in, int width, int edge)
	{
		int w = in.getWidth();
		int h = in.getHeight();
		int[] pixels = in.getRGB(0, 0, w, h, null, 0, w);
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = (pixels[i] >>> 24) > 20 ? 0xFF000000 | EDGE_RGB : 0;

		BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		mask.setRGB(0, 0, w, h, pixels, 0, w);
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		for (int i = 0; i < 16; i++)
		{
			double a = i * Math.PI / 8;
			g.drawImage(mask, (int) Math.round(Math.cos(a) * width), (int) Math.round(Math.sin(a) * width), null);
		}

		g.setComposite(AlphaComposite.DstOut);
		g.drawImage(mask, 0, 0, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN, edge / 255f));
		g.fillRect(0, 0, w, h);
		g.setComposite(AlphaComposite.SrcOver);
		g.drawImage(in, 0, 0, null);
		g.dispose();
		return out;
	}

	/** Averages each block of samples into one pixel, weighting colours by how solid each sample is. */
	private static BufferedImage shrink(BufferedImage in, int samples)
	{
		int w = in.getWidth() / samples;
		int h = in.getHeight() / samples;
		int across = in.getWidth();
		int[] source = in.getRGB(0, 0, across, in.getHeight(), null, 0, across);
		int[] result = new int[w * h];
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				long a = 0;
				long r = 0;
				long gr = 0;
				long b = 0;
				for (int j = 0; j < samples; j++)
				{
					for (int i = 0; i < samples; i++)
					{
						int p = source[(y * samples + j) * across + x * samples + i];
						long solid = p >>> 24;
						a += solid;
						r += (p >> 16 & 255) * solid;
						gr += (p >> 8 & 255) * solid;
						b += (p & 255) * solid;
					}
				}

				if (a > 0)
				{
					result[y * w + x] = (int) (a / (samples * samples)) << 24 | (int) (r / a) << 16
						| (int) (gr / a) << 8 | (int) (b / a);
				}
			}
		}

		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		out.setRGB(0, 0, w, h, result, 0, w);
		return out;
	}

	private static double[] newell(double[][] points)
	{
		double[] n = new double[3];
		for (int i = 0; i < points.length; i++)
		{
			double[] a = points[i];
			double[] b = points[(i + 1) % points.length];
			n[0] += (a[1] - b[1]) * (a[2] + b[2]);
			n[1] += (a[2] - b[2]) * (a[0] + b[0]);
			n[2] += (a[0] - b[0]) * (a[1] + b[1]);
		}

		return n;
	}

	private static double[] unit(double[] v)
	{
		double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
		return length == 0 ? v : new double[] {v[0] / length, v[1] / length, v[2] / length};
	}
}
