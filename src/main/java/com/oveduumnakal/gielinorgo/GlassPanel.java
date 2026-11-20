/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Paints the large raised glass body of a window, a box, the Info card, a modal, a menu or a tooltip (#324): the soft
 * dark and light shadows outside, the scene wash drawn each frame inside the outline, then a see-through tint, a
 * sheen fading down, a faint grain and a bevel. The shape is the caller's outline, so the same code paints a slanted
 * window with soft tips or any other body. Everything but the wash is the same from frame to frame, so it is painted
 * once into an image (kept in a small least-recently-used cache keyed by the outline's form, the scale, the theme and
 * whether a wash is under it) and only drawn after that. With no wash yet the tint is thicker, so text still reads.
 * Only the game thread uses it. Holds no RuneLite types.
 */
final class GlassPanel
{
	/** How many still layers the cache keeps. */
	static final int CAPACITY = 24;

	private static final BufferedImage GRAIN = grain();

	private static final Map<Key, BufferedImage> STILLS = new LinkedHashMap<Key, BufferedImage>(CAPACITY + 1, 0.75f,
		true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> eldest)
		{
			return size() > CAPACITY;
		}
	};

	private static final Map<TamedKey, BufferedImage> TAMED = new LinkedHashMap<TamedKey, BufferedImage>(17, 0.75f,
		true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<TamedKey, BufferedImage> eldest)
		{
			return size() > 16;
		}
	};

	private static final Map<BakedKey, BufferedImage> BAKED = new LinkedHashMap<BakedKey, BufferedImage>(17, 0.75f,
		true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<BakedKey, BufferedImage> eldest)
		{
			return size() > 12;
		}
	};

	private static int paints;

	private GlassPanel()
	{
	}

	/**
	 * Gives the tint colour as it is painted: the theme's window colour at an opacity.
	 *
	 * @param style the style the panel is drawn in
	 * @param opacity from 0 to 1
	 * @return the theme's window colour at that opacity
	 */
	static Color tint(WindowStyle style, double opacity)
	{
		return GlassTokens.of(style.color(ThemeColor.WINDOW), opacity);
	}

	/**
	 * Gives how far a body's still layer reaches beyond its outline on every side, for the shadows.
	 *
	 * @param style the style the panel is drawn in
	 * @return the pad in pixels
	 */
	static int pad(WindowStyle style)
	{
		return Math.max(GlassStyle.px(style, GlassTokens.LARGE_DARK_BLUR) * 3 + GlassStyle.px(style,
			Math.max(GlassTokens.LARGE_DARK_DX, GlassTokens.LARGE_DARK_DY)) + 2, 1);
	}

	/**
	 * Paints the body. The graphics' transform and composite apply, so a scaled or fading body works; the colour,
	 * paint, stroke and clip are left as they were found.
	 *
	 * @param g the graphics
	 * @param outline the body's outline, on whole pixels
	 * @param style the style the body is drawn in
	 * @param wash the scene wash behind the body, stretched over the outline's bounds, or null for none yet; it is
	 *        darkened where the text would not read over it, and drawn at {@link GlassTokens#WASH_ALPHA}
	 * @param needs the text colours that sit on the body, for the readability guard
	 */
	static synchronized void paint(Graphics2D g, Shape outline, WindowStyle style, BufferedImage wash,
		List<Readability.Need> needs)
	{
		Rectangle2D b = outline.getBounds2D();
		if (b.getWidth() <= 0 || b.getHeight() <= 0)
			return;

		wash(g, outline, style, wash, needs);
		still(g, outline, style, wash != null);
	}

	/**
	 * Paints only the wash of a body, clipped to its outline; the still layer goes on top of it (see {@link #still}).
	 * A caller that caches the still layer paints the wash itself each frame with this.
	 *
	 * @param g the graphics
	 * @param outline the body's outline, on whole pixels
	 * @param style the style the body is drawn in
	 * @param wash the scene wash, or null to paint nothing
	 * @param needs the text colours that sit on the body
	 */
	static synchronized void wash(Graphics2D g, Shape outline, WindowStyle style, BufferedImage wash,
		List<Readability.Need> needs)
	{
		Rectangle2D b = outline.getBounds2D();
		if (wash == null || b.getWidth() <= 0 || b.getHeight() <= 0)
			return;

		drawWash(g, outline, tamed(wash, style, needs), pad(style));
	}

	/**
	 * Paints a wash that was made for a footprint of a given room round the outline, clipped to the outline. A small
	 * surface, such as a bubble, has less shadow room than a window, so it reads the scene nearer to it.
	 *
	 * @param g the graphics
	 * @param outline the surface's outline
	 * @param style the style the surface is drawn in
	 * @param wash the scene wash, or null to paint nothing
	 * @param needs the text colours that sit on the surface
	 * @param pad how far the footprint reaches beyond the outline on every side, in pixels
	 */
	static synchronized void washWithin(Graphics2D g, Shape outline, WindowStyle style, BufferedImage wash,
		List<Readability.Need> needs, int pad)
	{
		Rectangle2D b = outline.getBounds2D();
		if (wash == null || b.getWidth() <= 0 || b.getHeight() <= 0)
			return;

		drawWash(g, outline, tamed(wash, style, needs), pad);
	}

	/**
	 * Paints only the still layer of a body: shadows, tint, sheen, grain and bevel.
	 *
	 * @param g the graphics
	 * @param outline the body's outline, on whole pixels
	 * @param style the style the body is drawn in
	 * @param overWash whether a wash lies under it, which makes the tint thinner
	 */
	static synchronized void still(Graphics2D g, Shape outline, WindowStyle style, boolean overWash)
	{
		Rectangle2D b = outline.getBounds2D();
		if (b.getWidth() <= 0 || b.getHeight() <= 0)
			return;

		int pad = pad(style);
		g.drawImage(still(outline, b, style, overWash, pad), (int) Math.round(b.getX()) - pad,
			(int) Math.round(b.getY()) - pad, null);
	}

	/**
	 * Gives the area a wash is made for and stretched over: the body's bounds and the room its shadows take, so the
	 * wash reads the scene from outside the shadows.
	 *
	 * @param box the body's bounds
	 * @param style the style the panel is drawn in
	 * @return the box grown by {@link #pad} on every side
	 */
	static Rectangle footprint(Rectangle box, WindowStyle style)
	{
		int pad = pad(style);
		return new Rectangle(box.x - pad, box.y - pad, box.width + 2 * pad, box.height + 2 * pad);
	}

	private static Rectangle2D footprint(Rectangle2D b, int pad)
	{
		return new Rectangle2D.Double(b.getX() - pad, b.getY() - pad, b.getWidth() + 2 * pad,
			b.getHeight() + 2 * pad);
	}

	private static void drawWash(Graphics2D g, Shape outline, BufferedImage tamed, int pad)
	{
		Rectangle2D body = outline.getBounds2D();
		BakedKey key = new BakedKey(tamed, Shapes.formKey(outline), pad);
		BufferedImage baked = BAKED.get(key);
		if (baked == null)
		{
			baked = bake(outline, body, tamed, pad);
			BAKED.put(key, baked);
		}

		g.drawImage(baked, (int) Math.round(body.getX()), (int) Math.round(body.getY()), null);
	}

	/**
	 * Bakes a wash into the picture that is drawn each frame: the tamed wash stretched over the footprint, cut to the
	 * outline with soft edges, at {@link GlassTokens#WASH_ALPHA}. A plain picture draws far faster than a stretched,
	 * clipped one, and a wash changes only a few times a second.
	 */
	private static BufferedImage bake(Shape outline, Rectangle2D body, BufferedImage tamed, int pad)
	{
		int w = (int) Math.ceil(body.getWidth());
		int h = (int) Math.ceil(body.getHeight());
		BufferedImage stretched = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = stretched.createGraphics();
		sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		sg.drawImage(tamed, -pad, -pad, w + 2 * pad, h + 2 * pad, null);
		sg.dispose();
		BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D mg = mask.createGraphics();
		WindowStyle.prepare(mg);
		mg.translate(-body.getX(), -body.getY());
		mg.setColor(Color.WHITE);
		mg.fill(outline);
		mg.dispose();
		int[] color = stretched.getRGB(0, 0, w, h, null, 0, w);
		int[] cut = mask.getRGB(0, 0, w, h, null, 0, w);
		for (int i = 0; i < color.length; i++)
		{
			int alpha = (int) Math.round((cut[i] >>> 24) * GlassTokens.WASH_ALPHA);
			color[i] = alpha << 24 | color[i] & 0xFFFFFF;
		}

		BufferedImage baked = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		baked.setRGB(0, 0, w, h, color, 0, w);
		return baked;
	}

	private static BufferedImage tamed(BufferedImage wash, WindowStyle style, List<Readability.Need> needs)
	{
		TamedKey key = new TamedKey(wash, style.theme(), needs);
		BufferedImage cached = TAMED.get(key);
		if (cached == null)
		{
			cached = Readability.tame(wash, style, Readability.body(style), needs);
			TAMED.put(key, cached);
		}

		return cached;
	}

	/** Forgets every cached still layer and tamed wash; for tests and the cost test. */
	static synchronized void clear()
	{
		STILLS.clear();
		TAMED.clear();
		BAKED.clear();
	}

	/** @return how many times a still layer has been painted since the start; for tests */
	static synchronized int paints()
	{
		return paints;
	}

	private static BufferedImage still(Shape outline, Rectangle2D b, WindowStyle style, boolean overWash, int pad)
	{
		Key wanted = new Key(Shapes.formKey(outline), Math.round(style.scale() * 1000), style.theme(), overWash);
		BufferedImage cached = STILLS.get(wanted);
		if (cached != null)
			return cached;

		int w = (int) Math.ceil(b.getWidth()) + 2 * pad;
		int h = (int) Math.ceil(b.getHeight()) + 2 * pad;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			WindowStyle.prepare(g);
			g.translate(pad - b.getX(), pad - b.getY());
			paintStill(g, outline, style, overWash);
		}
		finally
		{
			g.dispose();
		}

		paints++;
		STILLS.put(wanted, image);
		return image;
	}

	private static void paintStill(Graphics2D g, Shape body, WindowStyle style, boolean overWash)
	{
		GlassStyle.raisedLarge(g, body, style);
		g.setColor(tint(style, overWash ? GlassTokens.TINT_OVER_WASH : GlassTokens.TINT_SOLID));
		g.fill(body);
		Rectangle2D b = body.getBounds2D();
		g.setPaint(new GradientPaint(0, (float) b.getY(), GlassTokens.white(GlassTokens.SHEEN_TOP), 0,
			(float) b.getMaxY(), GlassTokens.white(GlassTokens.SHEEN_BOTTOM)));
		g.fill(body);
		int pad = pad(style);
		g.setPaint(new TexturePaint(GRAIN, new Rectangle2D.Double(b.getX() - pad, b.getY() - pad,
			GlassTokens.GRAIN_SIZE, GlassTokens.GRAIN_SIZE)));
		g.fill(body);
		GlassStyle.bevel(g, body, style, GlassTokens.LARGE_BEVEL, GlassTokens.LARGE_BEVEL_LIGHT,
			GlassTokens.LARGE_BEVEL_DARK);
	}

	/**
	 * Gives the grain tile's shared copy, for painting glass elsewhere with the same frost.
	 *
	 * @return the tile
	 */
	static BufferedImage grainTile()
	{
		return GRAIN;
	}

	/**
	 * Makes the grain tile: soft light and dark specks, each with its own opacity. The tile wraps at its edges, so it
	 * repeats without seams.
	 *
	 * @return the tile, white and black pixels at varied opacity
	 */
	static BufferedImage grain()
	{
		int size = GlassTokens.GRAIN_SIZE;
		Random random = new Random(294);
		int count = size * size;
		double[] tone = new double[count];
		double[] jitter = new double[count];
		for (int i = 0; i < count; i++)
			tone[i] = random.nextGaussian();

		for (int i = 0; i < count; i++)
			jitter[i] = random.nextDouble();

		for (int p = 0; p < GlassTokens.GRAIN_SOFTEN; p++)
			tone = soften(tone);

		double peak = 0;
		for (double t : tone)
			peak = Math.max(peak, Math.abs(t));

		BufferedImage tile = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				int i = y * size + x;
				double t = peak == 0 ? 0 : tone[i] / peak;
				double opacity = GlassTokens.GRAIN_PEAK_ALPHA * Math.abs(t)
					* (GlassTokens.GRAIN_FLOOR + (1 - GlassTokens.GRAIN_FLOOR) * jitter[i]);
				int v = t >= 0 ? 255 : 0;
				tile.setRGB(x, y, GlassTokens.of(new Color(v, v, v), opacity).getRGB());
			}
		}

		return tile;
	}

	private static double[] soften(double[] tone)
	{
		int size = GlassTokens.GRAIN_SIZE;
		double[] out = new double[tone.length];
		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				double sum = 0;
				for (int dy = -1; dy <= 1; dy++)
				{
					for (int dx = -1; dx <= 1; dx++)
					{
						int yy = Math.floorMod(y + dy, size);
						int xx = Math.floorMod(x + dx, size);
						sum += tone[yy * size + xx];
					}
				}

				out[y * size + x] = sum / 9;
			}
		}

		return out;
	}

	/** What a cached still layer was painted for. */
	private static final class Key
	{
		private final int[] form;
		private final long scale;
		private final WindowTheme theme;
		private final boolean overWash;

		Key(int[] form, long scale, WindowTheme theme, boolean overWash)
		{
			this.form = form;
			this.scale = scale;
			this.theme = theme;
			this.overWash = overWash;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Key))
				return false;

			Key k = (Key) o;
			return scale == k.scale && theme == k.theme && overWash == k.overWash && Arrays.equals(form, k.form);
		}

		@Override
		public int hashCode()
		{
			return Arrays.hashCode(form) * 31 + (int) scale * 7 + theme.hashCode() + (overWash ? 1 : 0);
		}
	}

	/** What a baked wash was made from: the tamed wash picture itself, the outline's form and the footprint room. */
	private static final class BakedKey
	{
		private final BufferedImage tamed;
		private final int[] form;
		private final int pad;

		BakedKey(BufferedImage tamed, int[] form, int pad)
		{
			this.tamed = tamed;
			this.form = form;
			this.pad = pad;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof BakedKey))
				return false;

			BakedKey k = (BakedKey) o;
			return tamed == k.tamed && pad == k.pad && Arrays.equals(form, k.form);
		}

		@Override
		public int hashCode()
		{
			return System.identityHashCode(tamed) * 31 + Arrays.hashCode(form) + pad;
		}
	}

	/** What a tamed wash was made from: the wash picture itself, the theme and the text colours. */
	private static final class TamedKey
	{
		private final BufferedImage wash;
		private final WindowTheme theme;
		private final List<Readability.Need> needs;

		TamedKey(BufferedImage wash, WindowTheme theme, List<Readability.Need> needs)
		{
			this.wash = wash;
			this.theme = theme;
			this.needs = needs;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof TamedKey))
				return false;

			TamedKey k = (TamedKey) o;
			return wash == k.wash && theme == k.theme && needs == k.needs;
		}

		@Override
		public int hashCode()
		{
			return System.identityHashCode(wash) * 31 + theme.hashCode() + System.identityHashCode(needs);
		}
	}
}
