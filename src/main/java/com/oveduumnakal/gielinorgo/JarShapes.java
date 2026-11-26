/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * What each jar looks like: one body, lip, cork and set of highlights for all of them,
		a coloured cork and an emblem on
 * a specialty jar, a metal band on a tier jar, and the contents a thrown jar carries. The numbers are the approved
 * designs of issue #376. Holds no RuneLite types.
 */
final class JarShapes
{
	/** How strongly a jar's liquid covers what is behind it. */
	private static final double LIQUID_ALPHA = 0.82;

	/** The height of a whole jar, from its base to the top of the cork, in the jar's units. */
	static final double HEIGHT = 119;

	private static final double[][] LIP = {{20, 83}, {29, 85}, {30, 90}, {26, 92}, {18, 93}};
	private static final double[][] CORK = {{0, 91}, {20, 91}, {21, 96}, {26, 100}, {26, 112}, {20, 117}, {0, 119}};
	/** The long highlight down the left, as distance round the jar and height. */
	static final double[][] SHINE = {{-36, 24}, {-27, 18}, {-26, 46}, {-30, 54}, {-36, 50}};
	/** The small glint under it. */
	static final double[][] GLINT = {{-34, 9}, {-26, 7}, {-25, 13}, {-33, 16}};
	/** The fainter highlight down the right. */
	static final double[][] EDGE = {{31, 14}, {36, 17}, {36, 46}, {31, 49}};

	/** The Ember flame and its core, as distance round the jar and height. */
	private static final double[][] FLAME = {
		{1, 72}, {7, 62}, {9, 52}, {12, 55}, {14, 61}, {20, 47}, {23, 36}, {21, 25}, {14, 17}, {5, 13},
		{-6, 13}, {-15, 17}, {-22, 26}, {-23, 37}, {-18, 50}, {-15, 58}, {-12, 46}, {-9, 44}, {-9, 55}, {-5, 64}};
	private static final double[][] CORE = {
		{1, 45}, {6, 37}, {11, 30}, {11, 22}, {6, 16}, {-5, 16}, {-11, 22}, {-11, 29}, {-6, 36}, {-3, 33}};

	private static final Color GLASS = new Color(0xF4FAFF);
	private static final Color SILVER = new Color(0xB4BEC7);
	private static final int RIM = 0x4C4C52;
	private static final int WHITE = 0xFFFFFF;
	private static final double EMBLEM_RADIUS = 42.8;

	/** A flat piece of an emblem, wrapped onto the glass once the jar is built. */
	private static final class Part
	{
		private final double[][] outline;
		private final int color;
		private final int order;

		private Part(double[][] outline, int color, int order)
		{
			this.outline = outline;
			this.color = color;
			this.order = order;
		}
	}

	private JarShapes()
	{
	}

	/**
	 * Builds one jar.
	 *
	 * @param type which jar
	 * @param thrown true for the jar in flight,
			with contents and an emblem front and back bent to the glass; false for
	 *     a still picture, front on and empty
	 * @return the model
	 */
	static JarModel build(JarType type, boolean thrown)
	{
		JarModel jar = new JarModel();
		List<JarModel.Face> faces = jar.faces();
		JarLathe.lathe(faces, JarLathe.BODY, JarModel.Layer.GLASS, GLASS, 0);
		JarLathe.lathe(faces, LIP, JarModel.Layer.STOPPER, SILVER, 0);
		JarLathe.lathe(faces, CORK, JarModel.Layer.STOPPER, cork(type), 1);

		List<JarModel.Face> decals = new ArrayList<>();
		for (int side = 0; side < (thrown ? 2 : 1); side++)
			emblem(decals, type, side * Math.PI);

		Color band = band(type);
		if (band != null)
		{
			double[][] ring = {{EMBLEM_RADIUS, 28}, {EMBLEM_RADIUS, 48}};
			JarLathe.lathe(faces, ring, JarModel.Layer.BAND, band, 0);
		}

		highlight(decals, SHINE, 245);
		highlight(decals, GLINT, 220);
		highlight(decals, EDGE, 120);
		faces.addAll(thrown ? JarLathe.onGlass(decals) : decals);
		if (thrown)
			contents(jar, type);

		return jar;
	}

	private static void highlight(List<JarModel.Face> faces, double[][] outline, int alpha)
	{
		JarLathe.wrap(faces, outline, 0, 43.0, new Color(255, 255, 255, alpha), JarModel.Layer.SHINE, 0);
	}

	private static Color cork(JarType type)
	{
		switch (type)
		{
			case CAVE: return new Color(0x585353);
			case SWIFT: return new Color(0x939293);
			case BLESSED: return new Color(0xDBDADE);
			case SLAYER: return new Color(0x434342);
			case VENOM: return new Color(0x2F6B2C);
			case EMBER: return new Color(0xE8590C);
			case GIANT: return new Color(0x6B5235);
			case TIDE: return new Color(0x7EB2D7);
			case STAB:
			case SLASH:
			case CRUSH: return new Color(0xB02221);
			case ARCHER: return new Color(0x61B03B);
			case MYSTIC: return new Color(0x0E61C6);
			default: return SILVER;
		}
	}

	private static Color band(JarType type)
	{
		switch (type)
		{
			case BRONZE: return new Color(0xC08C45);
			case IRON: return new Color(0x7F7F7F);
			case RUNE: return new Color(0x3C91C5);
			case DRAGON: return new Color(0xC42A28);
			default: return null;
		}
	}

	/**
	 * Wraps a jar's emblem onto one side of it.
	 *
	 * @param faces where the faces go
	 * @param type the jar
	 * @param turn which side, in radians round the jar
	 */
	private static void emblem(List<JarModel.Face> faces, JarType type, double turn)
	{
		if (type == JarType.EMBER)
		{
			JarLathe.wrap(faces, fitted(FLAME), turn, EMBLEM_RADIUS, new Color(0xE8590C), JarModel.Layer.EMBLEM, 0);
			JarLathe.wrap(faces, fitted(CORE), turn, 43.4, new Color(0xFFC23D), JarModel.Layer.EMBLEM, 1);
			return;
		}

		for (Part part : parts(type))
		{
			double radius = EMBLEM_RADIUS + part.order * 0.3;
			Color color = new Color(part.color);
			JarLathe.wrap(faces, part.outline, turn, radius, color, JarModel.Layer.EMBLEM, part.order);
		}
	}

	private static List<Part> parts(JarType type)
	{
		List<Part> p = new ArrayList<>();
		switch (type)
		{
			case CAVE:
				pickaxe(p);
				break;
			case SWIFT:
				wings(p);
				break;
			case BLESSED:
				rimmed(p, px(64, 64, 69, 82, 86, 88, 69, 95, 64, 110, 59, 95, 42, 88, 59, 82), WHITE);
				break;
			case SLAYER:
				skull(p);
				break;
			case VENOM:
				p.add(new Part(px(63, 63, 67, 70, 73, 79, 78, 89, 78, 95, 75, 101, 70, 105, 64, 106, 57, 105, 52,
						101, 49,
					95, 49, 89, 53, 81, 58, 74), 0x2F7A2C, 0));
				break;
			case GIANT:
				footprint(p);
				break;
			case TIDE:
				wave(p);
				break;
			case STAB:
				dagger(p);
				break;
			case SLASH:
				sword(p);
				break;
			case CRUSH:
				mace(p);
				break;
			case ARCHER:
				bow(p);
				break;
			case MYSTIC:
				book(p);
				break;
			default:
				break;
		}

		return p;
	}

	private static void pickaxe(List<Part> p)
	{
		p.add(new Part(px(59, 75, 68, 70, 88, 101, 86, 107, 80, 109, 77, 105), 0x625C59, 0));
		p.add(new Part(px(39, 91, 42, 80, 49, 71, 58, 65, 68, 63, 78, 66, 87, 74, 88, 84, 80, 78, 72, 76, 64, 77,
				55, 79,
			47, 84), 0x3E3A3A, 1));
	}

	private static void wings(List<Part> p)
	{
		double[][] wing = px(61, 88, 55, 77, 47, 70, 38, 66, 39, 76, 48, 79, 40, 82, 43, 90, 51, 89, 46, 96, 51, 101,
			57, 98, 56, 106, 61, 104);
		rimmed(p, wing, WHITE);
		rimmed(p, mirrored(wing), WHITE);
	}

	private static void skull(List<Part> p)
	{
		p.add(new Part(px(64, 66, 73, 68, 78, 72, 80, 78, 80, 86, 83, 90, 82, 96, 76, 98, 74, 99, 74, 108, 54, 108,
			54, 99, 52, 98, 46, 96, 45, 90, 48, 86, 48, 78, 50, 72, 55, 68), 0x3B3B3A, 0));
		double[][] eye = px(51, 89, 54, 86, 59, 87, 61, 91, 58, 94, 53, 93);
		int bone = 0xD6D8DB;
		p.add(new Part(eye, bone, 1));
		p.add(new Part(mirrored(eye), bone, 1));
		p.add(new Part(px(64, 93, 67, 99, 61, 99), bone, 1));
		p.add(new Part(px(58, 102, 61, 102, 61, 108, 58, 108), bone, 1));
		p.add(new Part(px(67, 102, 70, 102, 70, 108, 67, 108), bone, 1));
	}

	private static void footprint(List<Part> p)
	{
		int brown = 0x5E4228;
		p.add(new Part(px(59, 78, 64, 75, 70, 76, 74, 80, 76, 86, 75, 94, 72, 104, 68, 108, 61, 108, 58, 104, 58, 98,
			61, 94, 60, 90, 56, 85, 56, 81), brown, 0));
		p.add(new Part(disc(54, 74, 4.2), brown, 0));
		p.add(new Part(disc(61.5, 71, 3.6), brown, 0));
		p.add(new Part(disc(69, 72, 3.2), brown, 0));
		p.add(new Part(disc(75, 76, 3.0), brown, 0));
	}

	private static void wave(List<Part> p)
	{
		p.add(new Part(px(64, 67, 72, 69, 78, 74, 81, 81, 79, 82, 76, 78, 71, 76, 66, 78, 64, 82, 65, 87, 69, 90,
				74, 92,
			80, 90, 86, 85, 87, 88, 84, 96, 78, 103, 70, 107, 62, 108, 53, 105, 47, 99, 43, 90, 45, 82, 50, 74,
			56, 69), 0x3F7DB5, 0));
		int foam = 0x85BCE2;
		p.add(new Part(px(64, 69, 72, 71, 76, 74, 68, 73, 60, 75, 54, 81, 51, 89, 53, 97, 48, 93, 46, 87, 49, 79,
				56, 72),
			foam, 1));
		p.add(new Part(px(62, 79, 66, 77, 63, 83, 65, 89, 70, 93, 77, 93, 71, 97, 64, 95, 59, 89, 59, 83), foam, 1));
	}

	private static void dagger(List<Part> p)
	{
		p.add(new Part(px(64, 62, 70, 72, 69, 93, 59, 93, 58, 72), 0xB01F20, 0));
		p.add(new Part(px(60.5, 93, 67.5, 93, 67.5, 108, 60.5, 108), 0x8E1819, 0));
		p.add(new Part(disc(64, 108, 4), 0x8E1819, 0));
		guard(p, px(51, 91, 77, 91, 77, 97, 51, 97));
	}

	private static void sword(List<Part> p)
	{
		p.add(new Part(px(83, 60, 88, 70, 87, 80, 82, 90, 74, 97, 65, 104, 56, 93, 64, 88, 72, 81, 78, 73, 80, 65),
			0xB01F20, 0));
		p.add(new Part(px(55, 98, 60, 102, 51, 110, 47, 109, 47, 105), 0x8E1819, 0));
		guard(p, px(51, 93, 55, 90, 66, 105, 62, 108));
	}

	private static void mace(List<Part> p)
	{
		p.add(new Part(px(66, 81, 71, 85, 56, 106, 52, 108, 49, 104, 50, 101), 0x8E1819, 0));
		p.add(new Part(px(75, 61, 76, 67, 83, 65, 81, 71, 87, 75, 81, 78, 84, 84, 78, 83, 77, 90, 72, 85, 66, 89,
				66, 83,
			59, 82, 63, 78, 59, 72, 65, 72, 64, 65, 71, 68), 0xB92322, 1));
	}

	private static void bow(List<Part> p)
	{
		double[] arc = new double[36];
		for (int i = 0; i < 9; i++)
		{
			double a = Math.toRadians(-80 + i * 20);
			arc[2 * i] = 56 + 27 * Math.cos(a);
			arc[2 * i + 1] = 87 + 25 * Math.sin(a);
			arc[34 - 2 * i] = 56 + 18 * Math.cos(a);
			arc[35 - 2 * i] = 87 + 21 * Math.sin(a);
		}

		p.add(new Part(px(arc), 0x2F6F22, 0));
		p.add(new Part(px(57, 63, 61, 63, 61, 111, 57, 111), 0x5E9C4C, 0));
		p.add(new Part(px(41, 84, 78, 84, 78, 90, 41, 90), 0x66BC44, 1));
		p.add(new Part(px(90, 87, 76, 77, 76, 97), 0x66BC44, 1));
		p.add(new Part(px(40, 78, 47, 78, 53, 87, 47, 96, 40, 96, 46, 87), 0x66BC44, 1));
	}

	private static void book(List<Part> p)
	{
		p.add(new Part(px(43, 74, 47, 73, 48, 97, 64, 100, 80, 97, 80, 72, 84, 73, 87, 101, 65, 104, 44, 103),
				0x0B468F,
			0));
		p.add(new Part(px(46, 70, 55, 70, 63, 75, 63, 98, 56, 96, 47, 97), 0x1E74DC, 1));
		p.add(new Part(px(66, 75, 73, 69, 81, 68, 83, 96, 74, 96, 66, 98), 0x1E74DC, 1));
	}

	/** A red cross guard with a dark edge: the same outline nudged eight ways, painted underneath. */
	private static void guard(List<Part> parts, double[][] outline)
	{
		for (int i = 0; i < 8; i++)
			parts.add(new Part(nudged(outline, i, 1.5), 0x4A0A0B, 2));

		parts.add(new Part(outline, 0xE8534A, 3));
	}

	/** A dark edge for pale emblems: the same outline nudged eight ways, painted underneath. */
	private static void rimmed(List<Part> parts, double[][] outline, int color)
	{
		for (int i = 0; i < 8; i++)
			parts.add(new Part(nudged(outline, i, 2.4), RIM, 0));

		parts.add(new Part(outline, color, 1));
	}

	private static double[][] nudged(double[][] outline, int eighth, double by)
	{
		double[][] moved = new double[outline.length][];
		for (int i = 0; i < outline.length; i++)
		{
			moved[i] = new double[] {outline[i][0] + by * Math.cos(eighth * Math.PI / 4),
				outline[i][1] + by * Math.sin(eighth * Math.PI / 4)};
		}

		return moved;
	}

	/** Turns an outline traced on the 128 pixel icons, in their pixels, into the jar's units. */
	private static double[][] px(double... xy)
	{
		double[][] out = new double[xy.length / 2][];
		for (int i = 0; i < out.length; i++)
			out[i] = new double[] {(xy[2 * i] - 64) * 1.2, 35 - (xy[2 * i + 1] - 87) * 1.2};

		return out;
	}

	private static double[][] mirrored(double[][] outline)
	{
		double[][] out = new double[outline.length][];
		for (int i = 0; i < out.length; i++)
			out[i] = new double[] {-outline[i][0], outline[i][1]};

		return out;
	}

	private static double[][] disc(double x, double y, double r)
	{
		double[] xy = new double[16];
		for (int i = 0; i < 8; i++)
		{
			xy[2 * i] = x + r * Math.cos(i * Math.PI / 4);
			xy[2 * i + 1] = y + r * Math.sin(i * Math.PI / 4);
		}

		return px(xy);
	}

	private static double[][] fitted(double[][] outline)
	{
		double[][] out = new double[outline.length][];
		for (int i = 0; i < out.length; i++)
			out[i] = new double[] {outline[i][0] * 1.10, 35 + (outline[i][1] - 40) * 0.92};

		return out;
	}

	/** Puts the contents in a thrown jar. The seed is fixed, so they never jump between frames or throws. */
	private static void contents(JarModel jar, JarType type)
	{
		List<JarModel.Face> f = jar.faces();
		Random r = new Random(7);
		switch (type)
		{
			case BRONZE:
			case IRON:
			case RUNE:
			case DRAGON:
				motes(jar, type, r);
				break;
			case CAVE:
				scatter(f, r, 8, new double[] {7, 16}, new double[] {9, 7, 8}, 2, 0x4A4545, 0x6A6262);
				scatter(f, r, 5, new double[] {18, 28}, new double[] {3, 9, 3}, 1, 0xCBE6EE);
				break;
			case SWIFT:
				scatter(f, r, 11, new double[] {10, 58}, new double[] {3, 11, 1.2}, 2.4, WHITE, 0xDADAE0);
				break;
			case BLESSED:
				stars(jar, r);
				break;
			case SLAYER:
				scatter(f, r, 9, new double[] {7, 24}, new double[] {5, 10, 4}, 2.2, 0x2E2E2E, 0x484848);
				scatter(f, r, 6, new double[] {18, 56}, new double[] {2.4, 2.4, 2.4}, 0, 0xE02420);
				jar.glow(new Color(200, 20, 20, 90));
				break;
			case VENOM:
				liquid(jar, 0x3FA330, 30);
				scatter(f, r, 9, new double[] {10, 52}, new double[] {3.4, 3.4, 3.4}, 0, 0xB5F08A);
				jar.glow(new Color(90, 220, 60, 130));
				break;
			case EMBER:
				coals(jar, r);
				break;
			case GIANT:
				scatter(f, r, 5, new double[] {10, 20}, new double[] {13, 10, 12}, 1.5, 0x7A5C3A, 0x62482C);
				break;
			case TIDE:
				liquid(jar, 0x3E8FD0, 38);
				scatter(f, r, 10, new double[] {10, 54}, new double[] {3.2, 3.2, 3.2}, 0, 0xE4F6FF);
				jar.glow(new Color(80, 170, 255, 120));
				break;
			case STAB:
				scatter(f, r, 12, new double[] {10, 48}, new double[] {1.8, 14, 1.8}, 2.2, 0xC82826, 0xE8534A);
				break;
			case SLASH:
				scatter(f, r, 10, new double[] {10, 50}, new double[] {15, 2.6, 1.2}, 2.6, 0xC82826, 0xE8534A);
				break;
			case CRUSH:
				spikes(jar, r);
				break;
			case ARCHER:
				scatter(f, r, 11, new double[] {9, 50}, new double[] {4.5, 10, 2}, 2.2, 0x58B03A, 0x8AD866);
				break;
			case MYSTIC:
				scatter(f, r, 8, new double[] {8, 20}, new double[] {5, 5, 5}, 3, 0x2E86F0, 0x8CC4FF);
				scatter(f, r, 9, new double[] {24, 60}, new double[] {2.6, 2.6, 2.6}, 0, 0x8CC4FF);
				jar.glow(new Color(60, 130, 255, 190));
				break;
			default:
				break;
		}
	}

	private static void motes(JarModel jar, JarType type, Random r)
	{
		Color band = band(type);
		int c = band.getRGB() & 0xFFFFFF;
		scatter(jar.faces(), r, 7, new double[] {9, 20}, new double[] {7, 7, 7}, 3, c);
		scatter(jar.faces(), r, 7, new double[] {48, 60}, new double[] {3.4, 3.4, 3.4}, 3, c);
		int strength = type == JarType.DRAGON ? 170 : type == JarType.RUNE ? 130 : 80;
		jar.glow(new Color(band.getRed(), band.getGreen(), band.getBlue(), strength));
	}

	private static void stars(JarModel jar, Random r)
	{
		Color gold = new Color(0xFFF3BC);
		for (int i = 0; i < 7; i++)
		{
			double a = r.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(r.nextDouble()) * 25;
			double y = 10 + r.nextDouble() * 46;
			double s = 5 + r.nextDouble() * 4;
			double[] at = {Math.cos(a) * d, y, Math.sin(a) * d};
			JarLathe.gem(jar.faces(), at, new double[] {s, s * 0.22, s * 0.22}, 0, gold);
			JarLathe.gem(jar.faces(), at, new double[] {s * 0.22, s, s * 0.22}, 0, gold);
		}

		jar.glow(new Color(255, 244, 200, 170));
	}

	private static void coals(JarModel jar, Random r)
	{
		int[] heat = {0xFFD04A, 0xFF7A18, 0xD8340C};
		for (int i = 0; i < 16; i++)
		{
			double a = r.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(r.nextDouble()) * 27;
			double y = i < 10 ? 6 + r.nextDouble() * 8 : 24 + r.nextDouble() * 38;
			double s = i < 10 ? 6 + r.nextDouble() * 3 : 1.4 + r.nextDouble() * 1.2;
			double[] at = {Math.cos(a) * d, y, Math.sin(a) * d};
			JarLathe.gem(jar.faces(), at, new double[] {s, s, s}, 0, new Color(heat[i % 3]));
		}

		jar.glow(new Color(255, 140, 30, 190));
	}

	private static void spikes(JarModel jar, Random r)
	{
		for (int i = 0; i < 6; i++)
		{
			double a = r.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(r.nextDouble()) * 24;
			double y = 10 + r.nextDouble() * (i < 4 ? 12 : 40);
			double[] at = {Math.cos(a) * d, y, Math.sin(a) * d};
			JarLathe.gem(jar.faces(), at, new double[] {11, 3, 3}, 0, new Color(0xC82826));
			JarLathe.gem(jar.faces(), at, new double[] {3, 11, 3}, 0, new Color(0xA81E1E));
			JarLathe.gem(jar.faces(), at, new double[] {3, 3, 11}, 0, new Color(0xE04038));
		}
	}

	/**
	 * Scatters pieces of one shape between two heights.
	 *
	 * @param faces where the faces go
	 * @param random the seeded source of their places
	 * @param count how many
	 * @param heights the lowest and highest middle
	 * @param size half the width, height and depth of one, before each is grown or shrunk a little
	 * @param lean how far they may tip either way, in radians
	 * @param colors used in turn
	 */
	private static void scatter(List<JarModel.Face> faces, Random random, int count, double[] heights, double[] size,
		double lean, int... colors)
	{
		for (int i = 0; i < count; i++)
		{
			double a = random.nextDouble() * Math.PI * 2;
			double r = Math.sqrt(random.nextDouble()) * 27;
			double y = heights[0] + random.nextDouble() * (heights[1] - heights[0]);
			double grow = 0.8 + random.nextDouble() * 0.4;
			double[] at = {Math.cos(a) * r, y, Math.sin(a) * r};
			double[] grown = {size[0] * grow, size[1] * grow, size[2] * grow};
			double tip = (random.nextDouble() - 0.5) * lean;
			JarLathe.gem(faces, at, grown, tip, new Color(colors[i % colors.length]));
		}
	}

	private static void liquid(JarModel jar, int color, double top)
	{
		double[][] profile = {{0, 4}, {31, 4}, {38, 8}, {39.5, 14}, {39.5, top}, {0, top}};
		JarLathe.lathe(jar.faces(), profile, JarModel.Layer.LIQUID, new Color(color), 0);
		jar.fill(LIQUID_ALPHA);
	}
}
