/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The drawing of the glass look's elements (#324): a raised surface with two soft outer shadows and a bevel, a lifted
 * (hovered) one, a pressed-in one with inner shadows, a groove for a bar, and the bevel itself. Every length comes from
 * {@link GlassTokens} at scale 1 and is multiplied by the style's scale here. Every method takes the outline to draw,
 * so the same code serves any shape. Holds no RuneLite types.
 */
final class GlassStyle
{
	private GlassStyle()
	{
	}

	/**
	 * Scales a length of the look to the style's scale.
	 *
	 * @param style the style
	 * @param units a length at scale 1
	 * @return the length in whole pixels
	 */
	static int px(WindowStyle style, double units)
	{
		return (int) Math.round(units * style.scale());
	}

	/**
	 * Paints the shadows of a large raised surface, outside its outline: a dark one down and to the right and a light
	 * one up and to the left.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 */
	static void raisedLarge(Graphics2D g, Shape shape, WindowStyle style)
	{
		SoftShadows.outer(g, shape, GlassTokens.black(GlassTokens.LARGE_DARK), px(style, GlassTokens.LARGE_DARK_DX),
			px(style, GlassTokens.LARGE_DARK_DY), Math.max(1, px(style, GlassTokens.LARGE_DARK_BLUR)));
		SoftShadows.outer(g, shape, GlassTokens.white(GlassTokens.LARGE_LIGHT),
			px(style, GlassTokens.LARGE_LIGHT_OFFSET), px(style, GlassTokens.LARGE_LIGHT_OFFSET),
			Math.max(1, px(style, GlassTokens.LARGE_LIGHT_BLUR)));
	}

	/**
	 * Paints a small raised element: two soft shadows outside, a fill and a bevel inside.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 * @param fill the fill colour, with its opacity
	 * @param lift how lifted it is: 1 for resting, {@link GlassTokens#LIFT} when hovered; scales the shadows
	 */
	static void raisedSmall(Graphics2D g, Shape shape, WindowStyle style, Color fill, double lift)
	{
		SoftShadows.outer(g, shape, GlassTokens.black(GlassTokens.SMALL_DARK),
			px(style, GlassTokens.SMALL_DARK_DX * lift), px(style, GlassTokens.SMALL_DARK_DY * lift),
			Math.max(1, px(style, GlassTokens.SMALL_DARK_BLUR * lift)));
		SoftShadows.outer(g, shape, GlassTokens.white(GlassTokens.SMALL_LIGHT),
			px(style, GlassTokens.SMALL_LIGHT_OFFSET), px(style, GlassTokens.SMALL_LIGHT_OFFSET),
			Math.max(1, px(style, GlassTokens.SMALL_LIGHT_BLUR)));
		g.setColor(fill);
		g.fill(shape);
		bevel(g, shape, style, GlassTokens.SMALL_BEVEL, GlassTokens.SMALL_BEVEL_LIGHT,
			GlassTokens.SMALL_BEVEL_DARK);
	}

	/**
	 * Paints a pressed-in element: a fill and two inner shadows.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 * @param fill the fill colour, with its opacity
	 */
	static void pressed(Graphics2D g, Shape shape, WindowStyle style, Color fill)
	{
		g.setColor(fill);
		g.fill(shape);
		SoftShadows.inner(g, shape, GlassTokens.black(GlassTokens.PRESSED_DARK),
			px(style, GlassTokens.PRESSED_DARK_OFFSET), px(style, GlassTokens.PRESSED_DARK_OFFSET),
			Math.max(1, px(style, GlassTokens.PRESSED_DARK_BLUR)));
		SoftShadows.inner(g, shape, GlassTokens.white(GlassTokens.PRESSED_LIGHT),
			px(style, GlassTokens.PRESSED_LIGHT_OFFSET), px(style, GlassTokens.PRESSED_LIGHT_OFFSET),
			Math.max(1, px(style, GlassTokens.PRESSED_LIGHT_BLUR)));
	}

	/**
	 * Paints a groove, the track of a bar: a dark fill with inner shadows.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 */
	static void groove(Graphics2D g, Shape shape, WindowStyle style)
	{
		g.setColor(GlassTokens.black(GlassTokens.GROOVE_FILL));
		g.fill(shape);
		SoftShadows.inner(g, shape, GlassTokens.black(GlassTokens.GROOVE_DARK), px(style, GlassTokens.GROOVE_OFFSET),
			px(style, GlassTokens.GROOVE_OFFSET), Math.max(1, px(style, GlassTokens.GROOVE_BLUR)));
		SoftShadows.inner(g, shape, GlassTokens.white(GlassTokens.GROOVE_LIGHT),
			-px(style, GlassTokens.GROOVE_OFFSET), -px(style, GlassTokens.GROOVE_OFFSET),
			Math.max(1, px(style, GlassTokens.GROOVE_BLUR)));
	}

	/**
	 * Strokes a bevel just inside an outline: light at the top left, dark at the bottom right.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 * @param width the bevel's width at scale 1
	 * @param light the opacity of the light end
	 * @param dark the opacity of the dark end
	 */
	static void bevel(Graphics2D g, Shape shape, WindowStyle style, float width, double light, double dark)
	{
		Rectangle2D b = shape.getBounds2D();
		Shape clip = g.getClip();
		Paint paint = g.getPaint();
		Stroke stroke = g.getStroke();
		g.clip(shape);
		g.setStroke(new BasicStroke((float) (width * style.scale())));
		g.setPaint(new LinearGradientPaint(new Point2D.Double(b.getX(), b.getY()),
			new Point2D.Double(b.getMaxX(), b.getMaxY()), new float[] {0f, 0.45f, 0.55f, 1f},
			new Color[] {GlassTokens.white(light), GlassTokens.white(0), GlassTokens.black(0),
				GlassTokens.black(dark)}));
		g.draw(shape);
		g.setClip(clip);
		g.setPaint(paint);
		g.setStroke(stroke);
	}

	/**
	 * Fills a shape with a vertical gradient, for the fill of a bar.
	 *
	 * @param g the graphics
	 * @param shape the shape
	 * @param top the colour at the top
	 * @param bottom the colour at the bottom
	 */
	static void gradientFill(Graphics2D g, Shape shape, Color top, Color bottom)
	{
		Rectangle2D b = shape.getBounds2D();
		Paint paint = g.getPaint();
		g.setPaint(new GradientPaint(0, (float) b.getY(), top, 0, (float) b.getMaxY(), bottom));
		g.fill(shape);
		g.setPaint(paint);
	}
}
