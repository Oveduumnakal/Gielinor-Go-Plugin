/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Paints a creature's portrait the way the Party row and the Summary page both show it: on a backdrop, in a slanted
 * clip, with the rarity stripe along its left edge, a gold frame and a light sweep when the creature is gilded, washed
 * out while it rests or has fainted, bobbing a little while idle, and fading in when its picture arrives. A creature
 * whose picture has not arrived (or never will) shows its backdrop alone. Holds no RuneLite types.
 */
final class PortraitFrame
{
	/** The width of the rarity stripe, in layout units. */
	static final double STRIPE_UNITS = 4;

	private static final Map<BufferedImage, BufferedImage> GREY = new WeakHashMap<>();

	private PortraitFrame()
	{
	}

	/**
	 * Paints a portrait.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param pictures where the pictures are
	 * @param row the creature's row
	 * @param box where the portrait goes
	 * @param skew how far the clip's sides lean, in pixels
	 * @param bob how far up the portrait is drawn, in pixels, which is the idle bob and the hop together
	 * @param now the time in milliseconds
	 * @param mode the motion mode
	 */
	static void paint(Graphics2D g, WindowStyle style, PortraitCache pictures, RowModel row, Rectangle2D box,
		double skew, double bob, long now, MotionMode mode)
	{
		Shape clip = Slant.shape(box, skew);
		g.setColor(style.color(ThemeColor.PORTRAIT_BACKDROP));
		g.fill(clip);
		Shape savedClip = g.getClip();
		g.clip(clip);
		BufferedImage image = pictures.image(row.species(), row.view().npcId());
		if (image != null)
		{
			double alpha = pictures.alpha(row.species(), row.view().npcId(), now, mode);
			Composite saved = PageContext.fade(g, alpha);
			BufferedImage shown = row.desaturated() ? grey(image) : image;
			double side = Math.min(box.getWidth(), box.getHeight());
			g.drawImage(shown, (int) Math.round(box.getCenterX() - side / 2.0),
				(int) Math.round(box.getCenterY() - side / 2.0 - bob), (int) Math.round(side), (int) Math.round(side),
				null);
			g.setComposite(saved);
		}

		g.setColor(rarity(row.rarity()));
		double stripe = Math.max(2.0, style.px(STRIPE_UNITS));
		double lean = Math.min(skew, box.getWidth() / 2.0);
		Path2D.Double edge = new Path2D.Double();
		edge.moveTo(box.getX() + lean, box.getY());
		edge.lineTo(box.getX() + lean + stripe, box.getY());
		edge.lineTo(box.getX() + stripe, box.getMaxY());
		edge.lineTo(box.getX(), box.getMaxY());
		edge.closePath();
		g.fill(edge);
		if (row.gilded() && mode.loops())
			sweep(g, style, box, now);

		g.setClip(savedClip);
		if (row.gilded())
		{
			Stroke stroke = g.getStroke();
			g.setColor(style.color(ThemeColor.GOLD));
			g.setStroke(new BasicStroke(Math.max(2f, style.metrics().border)));
			g.draw(Slant.shape(new Rectangle2D.Double(box.getX() + 1, box.getY() + 1, box.getWidth() - 2,
				box.getHeight() - 2), skew));
			g.setStroke(stroke);
		}
	}

	/**
	 * Gives the colour of a rarity's stripe.
	 *
	 * @param rarity the rarity
	 * @return its colour
	 */
	static Color rarity(Rarity rarity)
	{
		return rarity.color();
	}

	/**
	 * Gives how far through its light sweep a gilded portrait is.
	 *
	 * @param now the time in milliseconds
	 * @return from 0 to 1 while a sweep plays, or -1 between sweeps
	 */
	static double sweepAt(long now)
	{
		long into = Math.floorMod(now, WindowTimes.GILDED_EVERY);
		return into >= WindowTimes.GILDED_SWEEP ? -1.0 : (double) into / WindowTimes.GILDED_SWEEP;
	}

	private static void sweep(Graphics2D g, WindowStyle style, Rectangle2D box, long now)
	{
		double t = sweepAt(now);
		if (t < 0)
			return;

		double band = box.getWidth() * 0.35;
		double x = box.getX() - band + (box.getWidth() + 2 * band) * t;
		AffineTransform saved = g.getTransform();
		g.setColor(WindowTheme.fade(style.color(ThemeColor.GOLD), 0.55));
		Path2D.Double stripe = new Path2D.Double();
		stripe.moveTo(x + band * 0.4, box.getY());
		stripe.lineTo(x + band, box.getY());
		stripe.lineTo(x + band * 0.6, box.getMaxY());
		stripe.lineTo(x, box.getMaxY());
		stripe.closePath();
		g.fill(stripe);
		g.setTransform(saved);
	}

	private static synchronized BufferedImage grey(BufferedImage image)
	{
		BufferedImage have = GREY.get(image);
		if (have != null)
			return have;

		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int argb = image.getRGB(x, y);
				int r = argb >> 16 & 0xFF;
				int gr = argb >> 8 & 0xFF;
				int b = argb & 0xFF;
				int lum = (int) Math.round(0.299 * r + 0.587 * gr + 0.114 * b);
				out.setRGB(x, y, argb & 0xFF000000 | lum << 16 | lum << 8 | lum);
			}
		}

		GREY.put(image, out);
		return out;
	}
}
