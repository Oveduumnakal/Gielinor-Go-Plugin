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

	/** The opacity of the white sheen at the top edge. */
	static final double SHEEN_TOP = 0.08;

	/** The opacity of the white sheen at the bottom edge. */
	static final double SHEEN_BOTTOM = 0.04;

	/** The opacity of the grain tile laid over the fill. */
	static final double GRAIN_ALPHA = 0.035;

	/** The opacity of the light edge round the outside. */
	static final double EDGE_ALPHA = 0.35;

	/** The opacity of the highlight line just inside the top edge. */
	static final double TOP_LINE_ALPHA = 0.5;

	/** The side of the square grain tile, in pixels. */
	static final int GRAIN_SIZE = 64;

	private static final BufferedImage GRAIN = grain();

	private Key key;
	private BufferedImage still;
	private int paints;

	/**
	 * Gives the fill colour exactly as it is painted: the theme's window colour at {@link #FILL_ALPHA}.
	 *
	 * @param style the style the panel is drawn in
	 * @return the fill colour, with its alpha
	 */
	static Color fill(WindowStyle style)
	{
		Color window = style.color(ThemeColor.WINDOW);
		return new Color(window.getRed(), window.getGreen(), window.getBlue(), alpha(FILL_ALPHA));
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
	 * @param backdrop the blurred scene behind the panel, stretched to {@code box}, or null to show the scene itself
	 */
	void paint(Graphics2D g, Rectangle box, WindowStyle style, BufferedImage backdrop)
	{
		if (box.width <= 0 || box.height <= 0)
			return;

		if (backdrop != null)
		{
			Shape clip = g.getClip();
			Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			try
			{
				g.clip(body(box, style));
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(backdrop, box.x, box.y, box.width, box.height, null);
			}
			finally
			{
				g.setClip(clip);
				if (interpolation != null)
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
			}
		}

		g.drawImage(still(box.width, box.height, style), box.x, box.y, null);
	}

	/** Forgets the cached still layer, so the next {@link #paint} paints it again. For the cost test. */
	void clear()
	{
		key = null;
		still = null;
	}

	/** @return how many times the still layer has been painted; for tests */
	int paints()
	{
		return paints;
	}

	private BufferedImage still(int width, int height, WindowStyle style)
	{
		Key wanted = new Key(width, height, Math.round(style.scale() * 1000), style.theme());
		if (still != null && wanted.equals(key))
			return still;

		int shadow = Math.max(0, style.metrics().shadow);
		BufferedImage image = new BufferedImage(width, height + shadow, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			WindowStyle.prepare(g);
			paintStill(g, new Rectangle(0, 0, width, height), style);
		}
		finally
		{
			g.dispose();
		}

		paints++;
		key = wanted;
		still = image;
		return image;
	}

	private static void paintStill(Graphics2D g, Rectangle box, WindowStyle style)
	{
		WindowStyle.Metrics m = style.metrics();
		RoundRectangle2D body = body(box, style);
		Area shadow = new Area(round(new Rectangle2D.Double(box.x, box.y + m.shadow, box.width, box.height),
			m.corner));
		shadow.subtract(new Area(body));
		g.setColor(style.color(ThemeColor.SHADOW));
		g.fill(shadow);

		g.setColor(fill(style));
		g.fill(body);
		g.setPaint(new GradientPaint(0, box.y, sheen(SHEEN_TOP), 0, box.y + box.height, sheen(SHEEN_BOTTOM)));
		g.fill(body);

		Composite composite = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) GRAIN_ALPHA));
		g.setPaint(new TexturePaint(GRAIN, new Rectangle(0, 0, GRAIN_SIZE, GRAIN_SIZE)));
		g.fill(body);
		g.setComposite(composite);

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

	private static BufferedImage grain()
	{
		Random random = new Random(294);
		BufferedImage tile = new BufferedImage(GRAIN_SIZE, GRAIN_SIZE, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < GRAIN_SIZE; y++)
		{
			for (int x = 0; x < GRAIN_SIZE; x++)
			{
				int v = random.nextInt(256);
				tile.setRGB(x, y, 0xFF000000 | v << 16 | v << 8 | v);
			}
		}

		return tile;
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
