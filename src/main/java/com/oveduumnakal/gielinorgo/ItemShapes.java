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
 * What each healing item looks like, in the same style as the jars: a turned glass body, a silver lip, a cork, the
 * liquid inside, the jar's painted highlights narrowed to the body, and for two of them a white mark. The salves are
 * round-bellied pots and the other two are tall vials. The numbers are the approved designs of issue #378. Holds no
 * RuneLite types.
 */
final class ItemShapes
{
	/** How strongly the liquid covers the front glass; drawn weaker, the colours wash out. */
	private static final double LIQUID_ALPHA = 0.92;

	private static final double[][] POT = {{0, 0}, {24, 0}, {40, 8}, {46, 30}, {46, 56}, {38, 74}, {20, 83}, {18, 88}};
	private static final double[][] POT_LIP = {{18, 87}, {26, 89}, {27, 93}, {23, 95}, {16, 96}};
	private static final double[][] POT_CORK = {{0, 94}, {17, 94}, {19, 112}, {16, 114}, {0, 114}};
	private static final double[][] POT_FILL = {{0, 4}, {22, 4}, {36, 10}, {42, 30}, {42, 52}, {0, 52}};
	private static final double[][] VIAL = {{0, 0}, {22, 0}, {30, 8}, {31, 66}, {24, 78}, {13, 84}, {12, 90}};
	private static final double[][] VIAL_LIP = {{12, 89}, {19, 91}, {20, 95}, {17, 97}, {11, 98}};
	private static final double[][] VIAL_CORK = {{0, 96}, {11, 96}, {12, 116}, {0, 118}};
	private static final double[][] VIAL_FILL = {{0, 4}, {20, 4}, {27, 10}, {27.5, 62}, {0, 62}};

	/** The widest radius of each body. */
	private static final double POT_WIDE = 46;
	private static final double VIAL_WIDE = 31;

	/** The jar's belly radius, which its highlights were drawn for. */
	private static final double JAR_BELLY = 42;

	/** The Antidote's cross, as distance right of the middle and height. */
	private static final double[][] CROSS = {{-5, 49}, {5, 49}, {5, 38}, {15, 38}, {15, 27}, {5, 27}, {5, 16},
		{-5, 16}, {-5, 27}, {-15, 27}, {-15, 38}, {-5, 38}};

	/** The cross of a small picture, where one pixel is about 9.4 units, so its arms are two pixels thick. */
	private static final double[][] SMALL_CROSS = {{-9.4, 55}, {9.4, 55}, {9.4, 40}, {24, 40}, {24, 21}, {9.4, 21},
		{9.4, 6}, {-9.4, 6}, {-9.4, 21}, {-24, 21}, {-24, 40}, {-9.4, 40}};

	/** The Revive draught's wing, which stands outside the vial, flat and facing the viewer. */
	private static final double[][] WING = {{26, 55}, {57, 86}, {62, 59}, {48, 64}, {57, 42}, {40, 46}, {44, 24},
		{26, 33}};

	private ItemShapes()
	{
	}

	/**
	 * Builds one item.
	 *
	 * @param type which item
	 * @param small whether it is for a small picture, which thickens the cross and enlarges the wing
	 * @return the model
	 */
	static JarModel build(ItemType type, boolean small)
	{
		boolean pot = type == ItemType.SMALL_SALVE || type == ItemType.LARGE_SALVE;
		double[][] body = pot ? POT : VIAL;
		double wide = pot ? POT_WIDE : VIAL_WIDE;
		JarModel item = new JarModel();
		List<JarModel.Face> faces = item.faces();
		JarLathe.lathe(faces, body, JarModel.Layer.GLASS, glass(type), 0);
		JarLathe.lathe(faces, pot ? POT_LIP : VIAL_LIP, JarModel.Layer.STOPPER, new Color(0xB4BEC7), 0);
		JarLathe.lathe(faces, pot ? POT_CORK : VIAL_CORK, JarModel.Layer.STOPPER, new Color(0x966E46), 1);
		JarLathe.lathe(faces, pot ? POT_FILL : VIAL_FILL, JarModel.Layer.LIQUID, liquid(type), 0);
		item.fill(LIQUID_ALPHA);

		highlight(faces, JarShapes.SHINE, body, wide, 245);
		highlight(faces, JarShapes.GLINT, body, wide, 220);
		highlight(faces, JarShapes.EDGE, body, wide, 120);
		if (type == ItemType.ANTIDOTE)
			onGlass(faces, small ? SMALL_CROSS : CROSS, body, new double[] {wide, 32.4}, Color.WHITE, 1);

		if (type == ItemType.REVIVE)
			wing(faces, small ? 1.12 : 1.0);

		return item;
	}

	private static Color glass(ItemType type)
	{
		switch (type)
		{
			case SMALL_SALVE: return new Color(0xE2C49C);
			case LARGE_SALVE: return new Color(0xC4ACDC);
			default: return new Color(0xF4FAFF);
		}
	}

	private static Color liquid(ItemType type)
	{
		switch (type)
		{
			case SMALL_SALVE: return new Color(0x60BE5A);
			case LARGE_SALVE: return new Color(0x468CDC);
			case REVIVE: return new Color(0xE24660);
			default: return new Color(0x46BEAA);
		}
	}

	private static void highlight(List<JarModel.Face> faces, double[][] outline, double[][] body, double wide,
		int alpha)
	{
		double[][] narrowed = new double[outline.length][];
		for (int i = 0; i < outline.length; i++)
			narrowed[i] = new double[] {outline[i][0] * wide / JAR_BELLY, outline[i][1]};

		onGlass(faces, narrowed, body, new double[] {wide, wide + 1}, new Color(255, 255, 255, alpha), 0);
	}

	/**
	 * Wraps a flat outline onto the front of a body, cut at the body's bends and drawn in to the glass at each height.
	 * It is painted unshaded, with the highlights, so a white mark stays white.
	 *
	 * @param faces where the faces go
	 * @param outline pairs of distance round the body and height
	 * @param body the body's outline
	 * @param radii the body's widest radius, and the radius the outline is wrapped at before it is drawn in
	 * @param color its colour
	 * @param order 0 for a highlight, 1 for a mark painted over the highlights
	 */
	private static void onGlass(List<JarModel.Face> faces, double[][] outline, double[][] body, double[] radii,
		Color color, int order)
	{
		List<JarModel.Face> flat = new ArrayList<>();
		JarLathe.wrap(flat, outline, 0, radii[1], color, JarModel.Layer.SHINE, order);
		for (JarModel.Face face : flat)
		{
			double low = -1000;
			for (int band = 2; band <= body.length; band++)
			{
				double high = band < body.length ? body[band][1] - JarLathe.PIVOT : 1000;
				List<double[]> above = JarLathe.clip(Arrays.asList(face.points()), 1, low, 1);
				List<double[]> piece = JarLathe.clip(above, 1, high, -1);
				low = high;
				if (piece.size() < 3)
					continue;

				double[][] points = new double[piece.size()][];
				for (int i = 0; i < points.length; i++)
				{
					double[] p = piece.get(i);
					double pull = Math.min(1, radiusAt(body, p[1] + JarLathe.PIVOT) / radii[0]);
					points[i] = new double[] {p[0] * pull, p[1], p[2] * pull};
				}

				faces.add(new JarModel.Face(JarModel.Layer.SHINE, color, order, 0, points));
			}
		}
	}

	private static double radiusAt(double[][] body, double height)
	{
		for (int i = 1; i + 1 < body.length; i++)
		{
			if (height <= body[i + 1][1])
			{
				double rise = Math.max(1e-6, body[i + 1][1] - body[i][1]);
				return body[i][0] + (body[i + 1][0] - body[i][0]) * Math.max(0, height - body[i][1]) / rise;
			}
		}

		return body[body.length - 1][0];
	}

	private static void wing(List<JarModel.Face> faces, double grow)
	{
		double[][] points = new double[WING.length][];
		for (int i = 0; i < points.length; i++)
		{
			points[i] = new double[] {26 + (WING[i][0] - 26) * grow,
				55 + (WING[i][1] - 55) * grow - JarLathe.PIVOT, 6};
		}

		faces.add(new JarModel.Face(JarModel.Layer.SHINE, Color.WHITE, 1, 0, points));
	}
}
