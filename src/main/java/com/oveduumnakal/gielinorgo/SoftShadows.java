/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Soft shadows for the glass look (#324): an outer shadow that falls only outside a shape, like a CSS box-shadow, and
 * an inner shadow that falls only inside it. Each shadow is blurred once into an image keyed by the shape's form, the
 * colour, the offset and the blur, and kept in a small least-recently-used cache, so painting the same shadow again
 * is one image draw. The images carry the shape's cut-out themselves, so no clip is needed. Only the game thread
 * uses it. Holds no RuneLite types.
 */
final class SoftShadows
{
	/** How many shadow images the cache keeps. */
	static final int CAPACITY = 128;

	private static final int PASSES_OUTER = 3;
	private static final int PASSES_INNER = 2;

	private static final Map<Key, BufferedImage> CACHE = new LinkedHashMap<Key, BufferedImage>(CAPACITY + 1, 0.75f,
		true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> eldest)
		{
			return size() > CAPACITY;
		}
	};

	private static int blurs;

	private SoftShadows()
	{
	}

	/**
	 * Paints a blurred shadow of a shape, only outside the shape.
	 *
	 * @param g the graphics
	 * @param shape the shape that casts it
	 * @param color the shadow colour, with its opacity
	 * @param dx how far right the shadow is shifted
	 * @param dy how far down the shadow is shifted
	 * @param blur the blur radius in pixels; 0 for a hard shadow
	 */
	static synchronized void outer(Graphics2D g, Shape shape, Color color, int dx, int dy, int blur)
	{
		draw(g, shape, color, dx, dy, blur, false);
	}

	/**
	 * Paints a blurred shadow that falls from the edge of a shape inward, only inside the shape.
	 *
	 * @param g the graphics
	 * @param shape the shape that is lit
	 * @param color the shadow colour, with its opacity
	 * @param dx how far right the shadow is shifted
	 * @param dy how far down the shadow is shifted
	 * @param blur the blur radius in pixels; 0 for a hard shadow
	 */
	static synchronized void inner(Graphics2D g, Shape shape, Color color, int dx, int dy, int blur)
	{
		draw(g, shape, color, dx, dy, blur, true);
	}

	/** @return how many shadows have been blurred since the start; for tests */
	static synchronized int blurs()
	{
		return blurs;
	}

	/** Empties the cache; for tests. */
	static synchronized void clear()
	{
		CACHE.clear();
	}

	/** @return how many images the cache holds; for tests */
	static synchronized int cached()
	{
		return CACHE.size();
	}

	private static void draw(Graphics2D g, Shape shape, Color color, int dx, int dy, int blur, boolean inner)
	{
		if (color.getAlpha() == 0)
			return;

		Rectangle2D b = shape.getBounds2D();
		Key key = new Key(Shapes.formKey(shape), color.getRGB(), dx, dy, blur, inner);
		BufferedImage image = CACHE.get(key);
		int pad = pad(blur, dx, dy);
		if (image == null)
		{
			image = inner ? makeInner(shape, b, color, dx, dy, blur, pad)
				: makeOuter(shape, b, color, dx, dy, blur, pad);
			CACHE.put(key, image);
			blurs++;
		}

		g.drawImage(image, (int) Math.round(b.getX()) - pad + (inner ? 0 : dx),
			(int) Math.round(b.getY()) - pad + (inner ? 0 : dy), null);
	}

	private static int pad(int blur, int dx, int dy)
	{
		return Math.max(1, blur) * 3 + Math.max(Math.abs(dx), Math.abs(dy)) + 2;
	}

	private static float[] coverage(Shape shape, Rectangle2D b, int pad, int w, int h, int shiftX, int shiftY)
	{
		BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D mg = mask.createGraphics();
		WindowStyle.prepare(mg);
		mg.translate(pad - b.getX() + shiftX, pad - b.getY() + shiftY);
		mg.setColor(Color.WHITE);
		mg.fill(shape);
		mg.dispose();
		int[] px = mask.getRGB(0, 0, w, h, null, 0, w);
		float[] out = new float[w * h];
		for (int i = 0; i < out.length; i++)
			out[i] = (px[i] >>> 24) / 255f;

		return out;
	}

	private static BufferedImage makeOuter(Shape shape, Rectangle2D b, Color color, int dx, int dy, int blur,
		int pad)
	{
		int w = (int) Math.ceil(b.getWidth()) + 2 * pad;
		int h = (int) Math.ceil(b.getHeight()) + 2 * pad;
		float[] alpha = coverage(shape, b, pad, w, h, 0, 0);
		for (int p = 0; p < PASSES_OUTER && blur > 0; p++)
			blurAlpha(alpha, w, h, blur);

		float[] hole = coverage(shape, b, pad, w, h, -dx, -dy);
		for (int i = 0; i < alpha.length; i++)
			alpha[i] *= 1f - hole[i];

		return tint(alpha, w, h, color);
	}

	private static BufferedImage makeInner(Shape shape, Rectangle2D b, Color color, int dx, int dy, int blur,
		int pad)
	{
		int w = (int) Math.ceil(b.getWidth()) + 2 * pad;
		int h = (int) Math.ceil(b.getHeight()) + 2 * pad;
		float[] alpha = coverage(shape, b, pad, w, h, dx, dy);
		for (int i = 0; i < alpha.length; i++)
			alpha[i] = 1f - alpha[i];

		for (int p = 0; p < PASSES_INNER && blur > 0; p++)
			blurAlpha(alpha, w, h, blur);

		float[] inside = coverage(shape, b, pad, w, h, 0, 0);
		for (int i = 0; i < alpha.length; i++)
			alpha[i] *= inside[i];

		return tint(alpha, w, h, color);
	}

	private static BufferedImage tint(float[] alpha, int w, int h, Color color)
	{
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int rgb = color.getRGB() & 0xFFFFFF;
		float opacity = color.getAlpha() / 255f;
		int[] px = new int[w * h];
		for (int i = 0; i < px.length; i++)
		{
			int a = Math.round(Math.max(0f, Math.min(1f, alpha[i])) * opacity * 255f);
			px[i] = a << 24 | rgb;
		}

		image.setRGB(0, 0, w, h, px, 0, w);
		return image;
	}

	private static void blurAlpha(float[] a, int w, int h, int r)
	{
		float[] tmp = new float[a.length];
		float norm = 1f / (2 * r + 1);
		for (int y = 0; y < h; y++)
		{
			float sum = 0;
			for (int x = -r; x <= r; x++)
				sum += x >= 0 && x < w ? a[y * w + x] : 0;

			for (int x = 0; x < w; x++)
			{
				tmp[y * w + x] = sum * norm;
				int out = x - r;
				int in = x + r + 1;
				sum += (in < w ? a[y * w + in] : 0) - (out >= 0 ? a[y * w + out] : 0);
			}
		}

		for (int x = 0; x < w; x++)
		{
			float sum = 0;
			for (int y = -r; y <= r; y++)
				sum += y >= 0 && y < h ? tmp[y * w + x] : 0;

			for (int y = 0; y < h; y++)
			{
				a[y * w + x] = sum * norm;
				int out = y - r;
				int in = y + r + 1;
				sum += (in < h ? tmp[in * w + x] : 0) - (out >= 0 ? tmp[out * w + x] : 0);
			}
		}
	}

	/** What a cached shadow was made for. */
	private static final class Key
	{
		private final int[] form;
		private final int rgba;
		private final int dx;
		private final int dy;
		private final int blur;
		private final boolean inner;

		Key(int[] form, int rgba, int dx, int dy, int blur, boolean inner)
		{
			this.form = form;
			this.rgba = rgba;
			this.dx = dx;
			this.dy = dy;
			this.blur = blur;
			this.inner = inner;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Key))
				return false;

			Key k = (Key) o;
			return rgba == k.rgba && dx == k.dx && dy == k.dy && blur == k.blur && inner == k.inner
				&& Arrays.equals(form, k.form);
		}

		@Override
		public int hashCode()
		{
			return Arrays.hashCode(form) * 31 + (rgba * 31 + dx) * 31 * 31 + dy * 31 + blur + (inner ? 1 : 0);
		}
	}
}
