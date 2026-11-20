/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Random;

/**
 * Paints a frosted-glass panel: a soft shadow, an optional blurred backdrop, a see-through tinted fill, a light
 * sheen fading down, a faint grain and a thin light edge. Everything but the backdrop is the same frame to frame, so
 * it is painted once into an image (keyed by size, scale and theme) and only drawn after that; the backdrop, when
 * there is one, is drawn under it each frame, cut to the panel's rounded corners. Used by the wild creature Info card
 * only (#294). Only the game thread uses it. Holds no RuneLite types.
 */
final class GlassPanel
{
	/** The opacity of the theme's window colour laid over the scene; high enough that text reads over white. */
	static final double FILL_ALPHA = 0.78;

	/**
	 * The opacity of the window colour over a backdrop. Lower than {@link #FILL_ALPHA}, so the backdrop's colours show;
	 * {@link #tame} darkens any backdrop colour that would leave the text hard to read.
	 */
	static final double BACKDROP_FILL_ALPHA = 0.29;

	/**
	 * The opacity the backdrop is drawn at, so the real scene still shows faintly through it. The readability guard
	 * takes the scene there to be about the backdrop's own colour, which holds for the ring wash (it is made from the
	 * scene round the card) but not for small bright spots in a busy scene.
	 */
	static final double WASH_ALPHA = 0.68;

	/** The lowest contrast allowed between the main text and what is behind it. */
	static final double TEXT_CONTRAST = 4.5;

	/** The lowest contrast allowed between the faint text and what is behind it. */
	static final double SUBTEXT_CONTRAST = 3.0;

	/** The opacity of the white sheen at the top edge. */
	static final double SHEEN_TOP = 0.08;

	/** The opacity of the white sheen at the bottom edge. */
	static final double SHEEN_BOTTOM = 0.04;

	/**
	 * The opacity of the strongest grain speck. Each speck is light or dark and has its own opacity, from clear up to
	 * this, so the grain reads as soft frost instead of even static.
	 */
	static final double GRAIN_PEAK_ALPHA = 0.14;

	/**
	 * The least share of its opacity a speck keeps after its random jitter, from 0 to 1. Lower makes the grain
	 * patchier.
	 */
	static final double GRAIN_FLOOR = 0.6;

	/** How many times the grain's light and dark are averaged with their neighbours, so specks clump softly. */
	static final int GRAIN_SOFTEN = 2;

	/** The opacity of the light edge round the outside. */
	static final double EDGE_ALPHA = 0.35;

	/** The opacity of the highlight line just inside the top edge. */
	static final double TOP_LINE_ALPHA = 0.5;

	/** The side of the square grain tile, in pixels. */
	static final int GRAIN_SIZE = 64;

	private static final BufferedImage GRAIN = grain();

	private final Key[] keys = new Key[2];
	private final BufferedImage[] stills = new BufferedImage[2];
	private int paints;
	private Look look;
	private BufferedImage rawBackdrop;
	private WindowTheme tamedTheme;
	private BufferedImage tamedBackdrop;

	/**
	 * Gives the fill colour exactly as it is painted: the theme's window colour at {@link #FILL_ALPHA}.
	 *
	 * @param style the style the panel is drawn in
	 * @return the fill colour, with its alpha
	 */
	static Color fill(WindowStyle style)
	{
		return fill(style, FILL_ALPHA);
	}

	/**
	 * Gives the fill colour at an opacity.
	 *
	 * @param style the style the panel is drawn in
	 * @param opacity from 0 to 1
	 * @return the theme's window colour at that opacity
	 */
	static Color fill(WindowStyle style, double opacity)
	{
		Color window = style.color(ThemeColor.WINDOW);
		return new Color(window.getRed(), window.getGreen(), window.getBlue(), alpha(opacity));
	}

	/**
	 * Darkens a backdrop colour just enough that both kinds of text still read over the fill and the strongest sheen
	 * laid on it, with the backdrop at {@link #WASH_ALPHA} over a scene of the same colour. Colours that already read
	 * are kept as they are.
	 *
	 * @param color the backdrop colour, taken as opaque
	 * @param style the style the panel is drawn in
	 * @return the colour, scaled toward black as little as needed
	 */
	static Color tame(Color color, WindowStyle style)
	{
		if (reads(seen(color, color), style))
			return color;

		double low = 0.0;
		double high = 1.0;
		for (int i = 0; i < 12; i++)
		{
			double mid = (low + high) / 2;
			if (reads(seen(color, scaled(color, mid)), style))
				low = mid;
			else
				high = mid;
		}

		return scaled(color, low);
	}

	/**
	 * Gives what shows through the panel's fill: the backdrop at {@link #WASH_ALPHA} over the scene.
	 *
	 * @param scene the scene colour behind the panel
	 * @param backdrop the backdrop colour drawn over it
	 * @return the opaque mix
	 */
	static Color seen(Color scene, Color backdrop)
	{
		return WindowTheme.mix(scene, backdrop, WASH_ALPHA);
	}

	/**
	 * Tames every pixel of a backdrop.
	 *
	 * @param backdrop the backdrop
	 * @param style the style the panel is drawn in
	 * @return a new picture with each pixel passed through {@link #tame(Color, WindowStyle)}
	 */
	static BufferedImage tame(BufferedImage backdrop, WindowStyle style)
	{
		int w = backdrop.getWidth();
		int h = backdrop.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
				out.setRGB(x, y, tame(new Color(backdrop.getRGB(x, y)), style).getRGB());
		}

		return out;
	}

	/**
	 * Gives what the text is drawn on where the panel is lightest: a backdrop colour under the backdrop fill and the
	 * strongest sheen.
	 *
	 * @param color the backdrop colour, taken as opaque
	 * @param style the style the panel is drawn in
	 * @return the opaque colour behind the text
	 */
	static Color behindText(Color color, WindowStyle style)
	{
		Color filled = WindowTheme.over(fill(style, BACKDROP_FILL_ALPHA), color);
		return WindowTheme.over(sheen(SHEEN_TOP), filled);
	}

	private static boolean reads(Color color, WindowStyle style)
	{
		Color under = behindText(color, style);
		return WindowTheme.contrast(style.color(ThemeColor.TEXT), under) >= TEXT_CONTRAST
			&& WindowTheme.contrast(style.color(ThemeColor.SUBTEXT), under) >= SUBTEXT_CONTRAST;
	}

	private static Color scaled(Color color, double factor)
	{
		return new Color((int) Math.round(color.getRed() * factor), (int) Math.round(color.getGreen() * factor),
			(int) Math.round(color.getBlue() * factor));
	}

	/**
	 * Gives the sheen colour at an opacity.
	 *
	 * @param opacity from 0 to 1
	 * @return white at that opacity
	 */
	static Color sheen(double opacity)
	{
		return new Color(255, 255, 255, alpha(opacity));
	}

	/**
	 * Paints the panel. The graphics' transform and composite apply, so a scaled or fading panel works; the colour,
	 * paint, stroke and clip are left as they were found.
	 *
	 * @param g the graphics
	 * @param box the panel bounds
	 * @param style the style the panel is drawn in
	 * @param backdrop the blurred scene behind the panel, stretched to {@code box}, or null to show the scene itself;
	 *        it is darkened where the text would not read over it, drawn at {@link #WASH_ALPHA} so the scene shows
	 *        faintly, and the fill over it is thinner
	 */
	void paint(Graphics2D g, Rectangle box, WindowStyle style, BufferedImage backdrop)
	{
		if (box.width <= 0 || box.height <= 0)
			return;

		if (look != null)
		{
			look.paint(g, box, style, backdrop);
			return;
		}

		if (backdrop != null)
		{
			Shape clip = g.getClip();
			Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			Composite composite = g.getComposite();
			try
			{
				g.clip(body(box, style));
				float alpha = composite instanceof AlphaComposite ? ((AlphaComposite) composite).getAlpha() : 1f;
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * (float) WASH_ALPHA));
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(tamed(backdrop, style), box.x, box.y, box.width, box.height, null);
			}
			finally
			{
				g.setComposite(composite);
				g.setClip(clip);
				if (interpolation != null)
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
			}
		}

		g.drawImage(still(box.width, box.height, style, backdrop != null), box.x, box.y, null);
	}

	private BufferedImage tamed(BufferedImage backdrop, WindowStyle style)
	{
		if (backdrop != rawBackdrop || style.theme() != tamedTheme)
		{
			rawBackdrop = backdrop;
			tamedTheme = style.theme();
			tamedBackdrop = tame(backdrop, style);
		}

		return tamedBackdrop;
	}

	/**
	 * Swaps in another background, so other glass styles can be compared on the same card. Spike only (#294): the
	 * plugin never sets one.
	 *
	 * @param look the background to paint instead, or null for the frosted glass
	 */
	void look(Look look)
	{
		this.look = look;
	}

	/**
	 * Paints a chip in the swapped-in style, if there is one.
	 *
	 * @param g the graphics
	 * @param x the left edge
	 * @param y the top edge
	 * @param label the chip's text
	 * @param accent whether it stands out
	 * @param style the style the panel is drawn in
	 * @return false when there is no other style, so the caller paints its own chip
	 */
	boolean chip(Graphics2D g, int x, int y, String label, boolean accent, WindowStyle style)
	{
		return look != null && look.chip(g, x, y, label, accent, style);
	}

	/**
	 * Paints a bar in the swapped-in style, if there is one.
	 *
	 * @param g the graphics
	 * @param box the bar's box
	 * @param fraction the filled share, from 0 to 1
	 * @param style the style the panel is drawn in
	 * @return false when there is no other style, so the caller paints its own bar
	 */
	boolean bar(Graphics2D g, Rectangle2D box, double fraction, WindowStyle style)
	{
		return look != null && look.bar(g, box, fraction, style);
	}

	/** Forgets the cached still layers, so the next {@link #paint} paints it again. For the cost test. */
	void clear()
	{
		keys[0] = null;
		keys[1] = null;
		stills[0] = null;
		stills[1] = null;
	}

	/** @return how many times the still layer has been painted; for tests */
	int paints()
	{
		return paints;
	}

	private BufferedImage still(int width, int height, WindowStyle style, boolean overBackdrop)
	{
		int slot = overBackdrop ? 1 : 0;
		Key wanted = new Key(width, height, Math.round(style.scale() * 1000), style.theme());
		if (stills[slot] != null && wanted.equals(keys[slot]))
			return stills[slot];

		int shadow = Math.max(0, style.metrics().shadow);
		BufferedImage image = new BufferedImage(width, height + shadow, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			WindowStyle.prepare(g);
			paintStill(g, new Rectangle(0, 0, width, height), style, overBackdrop ? BACKDROP_FILL_ALPHA : FILL_ALPHA);
		}
		finally
		{
			g.dispose();
		}

		paints++;
		keys[slot] = wanted;
		stills[slot] = image;
		return image;
	}

	private static void paintStill(Graphics2D g, Rectangle box, WindowStyle style, double fillAlpha)
	{
		WindowStyle.Metrics m = style.metrics();
		RoundRectangle2D body = body(box, style);
		Area shadow = new Area(round(new Rectangle2D.Double(box.x, box.y + m.shadow, box.width, box.height),
			m.corner));
		shadow.subtract(new Area(body));
		g.setColor(style.color(ThemeColor.SHADOW));
		g.fill(shadow);

		g.setColor(fill(style, fillAlpha));
		g.fill(body);
		g.setPaint(new GradientPaint(0, box.y, sheen(SHEEN_TOP), 0, box.y + box.height, sheen(SHEEN_BOTTOM)));
		g.fill(body);

		g.setPaint(new TexturePaint(GRAIN, new Rectangle(0, 0, GRAIN_SIZE, GRAIN_SIZE)));
		g.fill(body);

		float edge = Math.max(1f, style.px(1));
		g.setStroke(new BasicStroke(edge));
		g.setColor(sheen(EDGE_ALPHA));
		g.draw(round(new Rectangle2D.Double(box.x + edge / 2.0, box.y + edge / 2.0, box.width - edge,
			box.height - edge), m.corner));
		g.setColor(sheen(TOP_LINE_ALPHA));
		double top = box.y + edge * 1.5;
		g.draw(new Line2D.Double(box.x + m.corner, top, box.x + box.width - m.corner, top));
	}

	private static RoundRectangle2D body(Rectangle box, WindowStyle style)
	{
		return round(box, style.metrics().corner);
	}

	private static RoundRectangle2D round(Rectangle2D box, double corner)
	{
		return new RoundRectangle2D.Double(box.getX(), box.getY(), box.getWidth(), box.getHeight(), corner * 2,
			corner * 2);
	}

	private static int alpha(double opacity)
	{
		return (int) Math.round(Math.max(0.0, Math.min(1.0, opacity)) * 255);
	}

	/**
	 * Makes the grain tile: soft light and dark specks, each with its own opacity. The tile wraps at its edges, so it
	 * repeats without seams.
	 *
	 * @return the tile, white and black pixels at varied opacity
	 */
	static BufferedImage grain()
	{
		Random random = new Random(294);
		int count = GRAIN_SIZE * GRAIN_SIZE;
		double[] tone = new double[count];
		double[] jitter = new double[count];
		for (int i = 0; i < count; i++)
			tone[i] = random.nextGaussian();

		for (int i = 0; i < count; i++)
			jitter[i] = random.nextDouble();

		for (int p = 0; p < GRAIN_SOFTEN; p++)
			tone = soften(tone);

		double peak = 0;
		for (double t : tone)
			peak = Math.max(peak, Math.abs(t));

		BufferedImage tile = new BufferedImage(GRAIN_SIZE, GRAIN_SIZE, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < GRAIN_SIZE; y++)
		{
			for (int x = 0; x < GRAIN_SIZE; x++)
			{
				int i = y * GRAIN_SIZE + x;
				double t = peak == 0 ? 0 : tone[i] / peak;
				double opacity = GRAIN_PEAK_ALPHA * Math.abs(t) * (GRAIN_FLOOR + (1 - GRAIN_FLOOR) * jitter[i]);
				int v = t >= 0 ? 255 : 0;
				tile.setRGB(x, y, alpha(opacity) << 24 | v << 16 | v << 8 | v);
			}
		}

		return tile;
	}

	private static double[] soften(double[] tone)
	{
		double[] out = new double[tone.length];
		for (int y = 0; y < GRAIN_SIZE; y++)
		{
			for (int x = 0; x < GRAIN_SIZE; x++)
			{
				double sum = 0;
				for (int dy = -1; dy <= 1; dy++)
				{
					for (int dx = -1; dx <= 1; dx++)
					{
						int yy = Math.floorMod(y + dy, GRAIN_SIZE);
						int xx = Math.floorMod(x + dx, GRAIN_SIZE);
						sum += tone[yy * GRAIN_SIZE + xx];
					}
				}

				out[y * GRAIN_SIZE + x] = sum / 9;
			}
		}

		return out;
	}

	/** A whole panel background painted instead of the frosted glass, for comparing styles (#294 spike). */
	interface Look
	{
		/**
		 * Paints the background.
		 *
		 * @param g the graphics
		 * @param box the panel bounds
		 * @param style the style the panel is drawn in
		 * @param backdrop the scene backdrop as given, not yet darkened, or null
		 */
		void paint(Graphics2D g, Rectangle box, WindowStyle style, BufferedImage backdrop);

		/**
		 * Paints a chip in this style.
		 *
		 * @param g the graphics
		 * @param x the left edge
		 * @param y the top edge
		 * @param label the chip's text
		 * @param accent whether it stands out
		 * @param style the style the panel is drawn in
		 * @return false to let the caller paint its own chip
		 */
		default boolean chip(Graphics2D g, int x, int y, String label, boolean accent, WindowStyle style)
		{
			return false;
		}

		/**
		 * Paints a bar in this style.
		 *
		 * @param g the graphics
		 * @param box the bar's box
		 * @param fraction the filled share, from 0 to 1
		 * @param style the style the panel is drawn in
		 * @return false to let the caller paint its own bar
		 */
		default boolean bar(Graphics2D g, Rectangle2D box, double fraction, WindowStyle style)
		{
			return false;
		}
	}

	/** What the cached still layer was painted for. */
	private static final class Key
	{
		private final int width;
		private final int height;
		private final long scale;
		private final WindowTheme theme;

		Key(int width, int height, long scale, WindowTheme theme)
		{
			this.width = width;
			this.height = height;
			this.scale = scale;
			this.theme = theme;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Key))
				return false;

			Key k = (Key) o;
			return width == k.width && height == k.height && scale == k.scale && theme == k.theme;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(width, height, scale, theme);
		}
	}
}
