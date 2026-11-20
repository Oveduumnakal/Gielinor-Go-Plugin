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
import java.awt.geom.Line2D;
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
		shadowsSmall(g, shape, style, lift);
		g.setColor(fill);
		g.fill(shape);
		bevel(g, shape, style, GlassTokens.SMALL_BEVEL, GlassTokens.SMALL_BEVEL_LIGHT,
			GlassTokens.SMALL_BEVEL_DARK);
	}

	/**
	 * Paints the two soft shadows of a small raised element, outside its outline.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 * @param lift how lifted it is, from 1 (resting) up to {@link GlassTokens#LIFT}
	 */
	static void shadowsSmall(Graphics2D g, Shape shape, WindowStyle style, double lift)
	{
		SoftShadows.outer(g, shape, GlassTokens.black(GlassTokens.SMALL_DARK),
			px(style, GlassTokens.SMALL_DARK_DX * lift), px(style, GlassTokens.SMALL_DARK_DY * lift),
			Math.max(1, px(style, GlassTokens.SMALL_DARK_BLUR * lift)));
		SoftShadows.outer(g, shape, GlassTokens.white(GlassTokens.SMALL_LIGHT),
			px(style, GlassTokens.SMALL_LIGHT_OFFSET), px(style, GlassTokens.SMALL_LIGHT_OFFSET),
			Math.max(1, px(style, GlassTokens.SMALL_LIGHT_BLUR)));
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

	private static double lift(double hover)
	{
		return 1.0 + (GlassTokens.LIFT - 1.0) * clamp(hover);
	}

	private static double clamp(double v)
	{
		return Math.max(0.0, Math.min(1.0, v));
	}

	private static Color raisedFill(double hover)
	{
		double t = clamp(hover);
		return GlassTokens.white(GlassTokens.RAISED_FILL + (GlassTokens.LIFTED_FILL - GlassTokens.RAISED_FILL) * t);
	}

	/**
	 * Paints a tab: the active one pressed in, an inactive one raised, a disabled one flat; a hovered one lifts.
	 *
	 * @param g the graphics
	 * @param shape the tab's outline
	 * @param style the style
	 * @param active whether it is the selected tab
	 * @param disabled whether it cannot be chosen
	 * @param hover how much the pointer is on it, from 0 to 1
	 */
	static void tab(Graphics2D g, Shape shape, WindowStyle style, boolean active, boolean disabled, double hover)
	{
		if (disabled)
		{
			g.setColor(GlassTokens.white(GlassTokens.FLAT_FILL));
			g.fill(shape);
		}
		else if (active)
		{
			pressed(g, shape, style, GlassTokens.black(GlassTokens.PRESSED_TAB_FILL));
		}
		else
		{
			raisedSmall(g, shape, style, raisedFill(hover), lift(hover));
		}
	}

	/**
	 * Paints a button: the main one raised in the accent colour, a plain one raised, a disabled one flat; a hovered one
	 * lifts and a pressed one is pressed in.
	 *
	 * @param g the graphics
	 * @param shape the button's outline
	 * @param style the style
	 * @param primary whether it is the main choice
	 * @param disabled whether it cannot be pressed
	 * @param hover how much the pointer is on it, from 0 to 1
	 * @param press how much it is pressed, from 0 to 1
	 */
	static void button(Graphics2D g, Shape shape, WindowStyle style, boolean primary, boolean disabled, double hover,
		double press)
	{
		if (disabled)
		{
			g.setColor(GlassTokens.white(GlassTokens.FLAT_FILL));
			g.fill(shape);
		}
		else if (press >= 0.5)
		{
			pressed(g, shape, style, primary ? GlassTokens.of(GlassTokens.ACCENT, GlassTokens.ACCENT_ALPHA * 0.8)
				: GlassTokens.white(GlassTokens.PRESSED_BUTTON_FILL));
		}
		else if (primary)
		{
			raisedSmall(g, shape, style, GlassTokens.of(GlassTokens.ACCENT, GlassTokens.ACCENT_ALPHA), lift(hover));
			if (hover > 0)
			{
				g.setColor(GlassTokens.white(0.10 * clamp(hover)));
				g.fill(shape);
			}
		}
		else
		{
			raisedSmall(g, shape, style, raisedFill(hover), lift(hover));
		}
	}

	/**
	 * Paints a chip: raised, in the accent colour when it stands out.
	 *
	 * @param g the graphics
	 * @param shape the chip's outline
	 * @param style the style
	 * @param accent whether it stands out
	 */
	static void chip(Graphics2D g, Shape shape, WindowStyle style, boolean accent)
	{
		raisedSmall(g, shape, style, accent ? GlassTokens.of(GlassTokens.ACCENT, GlassTokens.ACCENT_ALPHA)
			: raisedFill(0), 1.0);
	}

	/**
	 * Paints a row: pressed in when plain, raised in the lead colour when it leads; a hovered row lightens and a
	 * pressed one darkens.
	 *
	 * @param g the graphics
	 * @param shape the row's outline
	 * @param style the style
	 * @param lead whether it is the lead row
	 * @param hover how much the pointer is on it, from 0 to 1
	 * @param press how much it is pressed, from 0 to 1
	 */
	static void row(Graphics2D g, Shape shape, WindowStyle style, boolean lead, double hover, double press)
	{
		if (lead)
		{
			raisedSmall(g, shape, style, GlassTokens.of(style.color(ThemeColor.LEAD_ROW), GlassTokens.ACCENT_ALPHA),
				lift(hover));
		}
		else
		{
			pressed(g, shape, style, GlassTokens.black(GlassTokens.INSET_FILL));
		}

		if (hover > 0)
		{
			g.setColor(GlassTokens.white((lead ? 0.10 : 0.08) * clamp(hover)));
			g.fill(shape);
		}

		if (press > 0)
		{
			g.setColor(GlassTokens.black(0.18 * clamp(press)));
			g.fill(shape);
		}
	}

	/**
	 * Paints a bar: a groove with a vertical-gradient fill one pixel inside it.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param box the bar's box
	 * @param fraction the filled share, from 0 to 1
	 * @param fill the colour name of the fill
	 */
	static void bar(Graphics2D g, WindowStyle style, Rectangle2D box, double fraction, ThemeColor fill)
	{
		double skew = style.skew(WindowStyle.Kind.BAR);
		groove(g, style.outline(WindowStyle.Kind.BAR, box), style);
		if (fraction <= 0)
			return;

		double in = Math.max(1, style.scale());
		Rectangle2D inner = new Rectangle2D.Double(box.getX() + in, box.getY() + in, box.getWidth() - 2 * in,
			box.getHeight() - 2 * in);
		Color bottom = style.color(fill);
		Color top = fill == ThemeColor.XP_FILL ? GlassTokens.XP_TOP
			: WindowTheme.mix(bottom, Color.WHITE, GlassTokens.HP_TOP_LIGHT);
		if (fill == ThemeColor.XP_FILL)
			bottom = GlassTokens.XP_BOTTOM;

		gradientFill(g, style.filled(inner, skew, fraction), top, bottom);
	}

	/**
	 * Paints the focus ring: a soft glow outside the outline and a sharp ring on it.
	 *
	 * @param g the graphics
	 * @param shape the outline to ring
	 * @param style the style
	 * @param ring the ring's colour
	 */
	static void focus(Graphics2D g, Shape shape, WindowStyle style, Color ring)
	{
		SoftShadows.outer(g, shape, GlassTokens.of(ring, GlassTokens.GLOW_ALPHA), 0, 0,
			Math.max(2, px(style, GlassTokens.GLOW_WIDTH)));
		Stroke stroke = g.getStroke();
		g.setColor(GlassTokens.of(ring, GlassTokens.RING_ALPHA));
		g.setStroke(new BasicStroke((float) Math.max(GlassTokens.RING_WIDTH, GlassTokens.RING_WIDTH * style.scale())));
		g.draw(shape);
		g.setStroke(stroke);
	}

	/**
	 * Lays a light over a shape the pointer is on and a dark over one that is pressed, for surfaces without a state
	 * of their own.
	 *
	 * @param g the graphics
	 * @param shape what to shade
	 * @param hover how much the pointer is on it, from 0 to 1
	 * @param press how much it is pressed, from 0 to 1
	 */
	static void shade(Graphics2D g, Shape shape, double hover, double press)
	{
		if (hover > 0)
		{
			g.setColor(GlassTokens.white(0.12 * clamp(hover)));
			g.fill(shape);
		}

		if (press > 0)
		{
			g.setColor(GlassTokens.black(0.22 * clamp(press)));
			g.fill(shape);
		}
	}

	/**
	 * Paints the resize grip as three short grooves: a dark line with a light one beside it.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param box the grip's square
	 * @param active how much the grip is being dragged, from 0 to 1
	 */
	static void grip(Graphics2D g, WindowStyle style, Rectangle2D box, double active)
	{
		Stroke stroke = g.getStroke();
		float width = Math.max(1f, style.metrics().border);
		double step = box.getWidth() / 4.0;
		double right = box.getMaxX() - style.metrics().border;
		double bottom = box.getMaxY() - style.metrics().border;
		for (int i = 1; i <= 3; i++)
		{
			g.setStroke(new BasicStroke(width));
			g.setColor(WindowTheme.mix(GlassTokens.black(0.45), GlassTokens.of(style.color(ThemeColor.ACCENT), 0.9),
				active));
			g.draw(new Line2D.Double(right - i * step, bottom, right, bottom - i * step));
			g.setColor(GlassTokens.white(0.22));
			g.draw(new Line2D.Double(right - i * step - width, bottom - width, right - width, bottom - i * step
				- width));
		}

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
