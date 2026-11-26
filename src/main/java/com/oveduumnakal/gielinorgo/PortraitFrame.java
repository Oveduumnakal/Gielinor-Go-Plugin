/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
 * clip, a gold frame and a light sweep when the creature is gilded, washed out while it rests or has fainted,
 * bobbing a little while idle, and fading in when its picture arrives. A creature whose picture has not arrived (or
 * never will) shows its backdrop alone. Holds no RuneLite types.
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
		paint(g, style, pictures, row, box, skew, bob, now, mode, null);
	}

	/**
	 * Paints a portrait that may be moving.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param pictures where the pictures are
	 * @param row the creature's row
	 * @param box where the portrait goes
	 * @param skew how far the clip's sides lean, in pixels
	 * @param bob how far up the portrait is drawn, in pixels
	 * @param now the time in milliseconds
	 * @param mode the motion mode
	 * @param idle the creature's idle loop, drawn instead of the still picture, or null for the still one
	 */
	static void paint(Graphics2D g, WindowStyle style, PortraitCache pictures, RowModel row, Rectangle2D box,
		double skew, double bob, long now, MotionMode mode, IdleStrip idle)
	{
		Shape clip = style.slant(box, skew);
		g.setColor(style.color(ThemeColor.PORTRAIT_BACKDROP));
		g.fill(clip);
		Shape savedClip = g.getClip();
		g.clip(clip);
		BufferedImage image = idle == null ? pictures.image(row.species(), row.view().npcId()) : null;
		if (idle != null)
		{
			double side = Math.min(box.getWidth(), box.getHeight());
			BufferedImage frame = idle.at(now);
			scaled(g, row.desaturated() ? grey(frame) : frame, box.getCenterX() - side / 2.0,
				box.getCenterY() - side / 2.0, side);
		}
		else if (image != null)
		{
			double alpha = pictures.alpha(row.species(), row.view().npcId(), now, mode);
			Composite saved = PageContext.fade(g, alpha);
			BufferedImage shown = row.desaturated() ? grey(image) : image;
			double side = Math.min(box.getWidth(), box.getHeight());
			CrispImages.draw(g, shown, box.getCenterX() - side / 2.0, box.getCenterY() - side / 2.0 - bob, side);
			g.setComposite(saved);
		}

		g.setClip(savedClip);
		border(g, style, row, box, clip, now, mode);
	}

	/**
	 * Draws a square picture of a moving creature, scaling as it draws: a strip has too many pictures to keep a copy
	 * of each at every size.
	 *
	 * @param g the graphics
	 * @param image the picture
	 * @param x the left edge
	 * @param y the top edge
	 * @param side the side to draw it at
	 */
	static void scaled(Graphics2D g, BufferedImage image, double x, double y, double side)
	{
		Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, (int) Math.round(x), (int) Math.round(y), (int) Math.round(side), (int) Math.round(side),
			null);
		if (hint != null)
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
	}

	/**
	 * Paints what goes round a portrait: the light sweep and gold frame of a gilded creature, or the glass border.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param row the creature's row
	 * @param box the portrait box
	 * @param clip its slanted shape
	 * @param now the time in milliseconds
	 * @param mode the motion mode
	 */
	static void border(Graphics2D g, WindowStyle style, RowModel row, Rectangle2D box, Shape clip, long now,
		MotionMode mode)
	{
		double skew = style.metrics().slant / 2.0;
		if (row.gilded() && mode.loops())
		{
			Shape saved = g.getClip();
			g.clip(clip);
			sweep(g, style, box, now);
			g.setClip(saved);
		}

		if (style.theme().glass() && !row.gilded())
			GlassStyle.borderInset(g, clip, style, 1.0);

		if (row.gilded())
		{
			Stroke stroke = g.getStroke();
			g.setColor(style.color(ThemeColor.GOLD));
			g.setStroke(new BasicStroke(Math.max(2f, style.metrics().border)));
			g.draw(style.slant(new Rectangle2D.Double(box.getX() + 1, box.getY() + 1, box.getWidth() - 2,
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

	/**
	 * @param image a portrait
	 * @return the same picture without colour, kept for next time
	 */
	static synchronized BufferedImage grey(BufferedImage image)
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
