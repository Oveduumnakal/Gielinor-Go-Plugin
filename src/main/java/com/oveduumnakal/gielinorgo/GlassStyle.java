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
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The drawing of the glass look's elements (#324): a raised surface with two soft outer shadows and a bevel, a lifted
 * (hovered) one, a pressed-in one with inner shadows, a groove for a bar, and the bevel itself. Every length comes from
 * {@link GlassTokens} at scale 1 and is multiplied by the style's scale here. Every method takes the outline to draw,
 * so the same code serves any shape. Holds no RuneLite types.
 */
final class GlassStyle
{
	private static final int KIND_RAISED = 1;
	private static final int KIND_PRESSED = 2;
	private static final int KIND_GROOVE = 3;
	private static final int KIND_METAL = 4;
	private static final int KIND_METAL_PRESSED = 5;
	private static final int KIND_HEART = 6;
	private static final int KIND_HEART_EMPTY = 7;

	private static final Map<SpriteKey, BufferedImage> SPRITES = new LinkedHashMap<SpriteKey, BufferedImage>(257,
		0.75f, true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<SpriteKey, BufferedImage> eldest)
		{
			return size() > 256;
		}
	};

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
		int pad = px(style, 3 * GlassTokens.SMALL_DARK_BLUR * lift + GlassTokens.SMALL_DARK_DY * lift + 2);
		sprite(g, shape, style, KIND_RAISED, fill.getRGB(), (int) Math.round(lift * 100), pad, (sg, local) ->
		{
			shadowsSmall(sg, local, style, lift);
			sg.setColor(fill);
			sg.fill(local);
			bevel(sg, local, style, GlassTokens.SMALL_BEVEL, GlassTokens.SMALL_BEVEL_LIGHT,
				GlassTokens.SMALL_BEVEL_DARK);
		});
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
		sprite(g, shape, style, KIND_PRESSED, fill.getRGB(), 0, 0, (sg, local) ->
		{
			sg.setColor(fill);
			sg.fill(local);
			SoftShadows.inner(sg, local, GlassTokens.black(GlassTokens.PRESSED_DARK),
				px(style, GlassTokens.PRESSED_DARK_OFFSET), px(style, GlassTokens.PRESSED_DARK_OFFSET),
				Math.max(1, px(style, GlassTokens.PRESSED_DARK_BLUR)));
			SoftShadows.inner(sg, local, GlassTokens.white(GlassTokens.PRESSED_LIGHT),
				px(style, GlassTokens.PRESSED_LIGHT_OFFSET), px(style, GlassTokens.PRESSED_LIGHT_OFFSET),
				Math.max(1, px(style, GlassTokens.PRESSED_LIGHT_BLUR)));
		});
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
		sprite(g, shape, style, KIND_GROOVE, 0, 0, 0, (sg, local) ->
		{
			sg.setColor(GlassTokens.black(GlassTokens.GROOVE_FILL));
			sg.fill(local);
			SoftShadows.inner(sg, local, GlassTokens.black(GlassTokens.GROOVE_DARK),
				px(style, GlassTokens.GROOVE_OFFSET), px(style, GlassTokens.GROOVE_OFFSET),
				Math.max(1, px(style, GlassTokens.GROOVE_BLUR)));
			SoftShadows.inner(sg, local, GlassTokens.white(GlassTokens.GROOVE_LIGHT),
				-px(style, GlassTokens.GROOVE_OFFSET), -px(style, GlassTokens.GROOVE_OFFSET),
				Math.max(1, px(style, GlassTokens.GROOVE_BLUR)));
		});
	}

	private static void sprite(Graphics2D g, Shape shape, WindowStyle style, int kind, int rgba, int lift, int pad,
		Sprite painter)
	{
		Rectangle2D b = shape.getBounds2D();
		SpriteKey key = new SpriteKey(Shapes.formKey(shape), kind, rgba, lift, (int) Math.round(style.scale() * 1000));
		BufferedImage image;
		synchronized (SPRITES)
		{
			image = SPRITES.get(key);
			if (image == null)
			{
				image = new BufferedImage((int) Math.ceil(b.getWidth()) + 2 * pad,
					(int) Math.ceil(b.getHeight()) + 2 * pad, BufferedImage.TYPE_INT_ARGB);
				Graphics2D sg = image.createGraphics();
				WindowStyle.prepare(sg);
				Shape local = AffineTransform.getTranslateInstance(pad - b.getX(), pad - b.getY())
					.createTransformedShape(shape);
				painter.paint(sg, local);
				sg.dispose();
				SPRITES.put(key, image);
			}
		}

		g.drawImage(image, (int) Math.round(b.getX()) - pad, (int) Math.round(b.getY()) - pad, null);
	}

	/** Forgets every cached element picture; for tests. */
	static void clearSprites()
	{
		synchronized (SPRITES)
		{
			SPRITES.clear();
		}
	}

	/** Paints one element into its own picture. */
	private interface Sprite
	{
		/**
		 * Paints the element.
		 *
		 * @param g the picture's graphics
		 * @param shape the outline, moved so the picture holds it
		 */
		void paint(Graphics2D g, Shape shape);
	}

	/** What a cached element picture was painted for. */
	private static final class SpriteKey
	{
		private final int[] form;
		private final int kind;
		private final int rgba;
		private final int lift;
		private final int scale;

		SpriteKey(int[] form, int kind, int rgba, int lift, int scale)
		{
			this.form = form;
			this.kind = kind;
			this.rgba = rgba;
			this.lift = lift;
			this.scale = scale;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof SpriteKey))
				return false;

			SpriteKey k = (SpriteKey) o;
			return kind == k.kind && rgba == k.rgba && lift == k.lift && scale == k.scale
				&& Arrays.equals(form, k.form);
		}

		@Override
		public int hashCode()
		{
			return Arrays.hashCode(form) * 31 + ((kind * 31 + rgba) * 31 + lift) * 31 + scale;
		}
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
			border(g, shape, style, GlassTokens.BORDER_DISABLED);
		}
		else if (active)
		{
			pressed(g, shape, style, GlassTokens.black(GlassTokens.PRESSED_TAB_FILL));
			borderInset(g, shape, style, 1.0);
		}
		else
		{
			raisedSmall(g, shape, style, raisedFill(hover), lift(hover));
			border(g, shape, style, 1.0);
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
			border(g, shape, style, GlassTokens.BORDER_DISABLED);
		}
		else if (press >= 0.5)
		{
			if (primary)
			{
				pressedMetal(g, shape, style);
			}
			else
			{
				pressed(g, shape, style, GlassTokens.white(GlassTokens.PRESSED_BUTTON_FILL));
				borderInset(g, shape, style, 1.0);
			}
		}
		else if (primary)
		{
			raisedMetal(g, shape, style, lift(hover));
			if (hover > 0)
			{
				g.setColor(GlassTokens.white(0.10 * clamp(hover)));
				g.fill(shape);
			}
		}
		else
		{
			raisedSmall(g, shape, style, raisedFill(hover), lift(hover));
			border(g, shape, style, 1.0);
		}
	}

	/**
	 * Strokes the thin metal border of the style's theme round a raised element: a 1 pixel line that fades from the
	 * light metal at the top edge to the dark at the bottom.
	 *
	 * @param g the graphics; its paint and stroke are put back
	 * @param shape the outline of the button or pop-up
	 * @param style the style, for the scale and the border colours
	 * @param strength how much of the border shows, from 0 to 1
	 */
	static void border(Graphics2D g, Shape shape, WindowStyle style, double strength)
	{
		GlassBorder metal = style.theme().border();
		stroke(g, shape, style, strength, false, metal.high(), metal.middle(), metal.low());
	}

	/**
	 * Strokes the metal border of a pressed-in element: the same metal with the light and the dark swapped, so the
	 * edge reads as sunk into the surface.
	 *
	 * @param g the graphics; its paint and stroke are put back
	 * @param shape the outline
	 * @param style the style
	 * @param strength how much of the border shows, from 0 to 1
	 */
	static void borderInset(Graphics2D g, Shape shape, WindowStyle style, double strength)
	{
		GlassBorder metal = style.theme().border();
		stroke(g, shape, style, strength, true, metal.high(), metal.middle(), metal.low());
	}

	/**
	 * Strokes a metal outline in any three colours (light, middle, dark), raised or inset.
	 *
	 * @param g the graphics; its paint and stroke are put back
	 * @param shape the outline
	 * @param style the style, for the scale
	 * @param strength how much of the outline shows, from 0 to 1
	 * @param inset whether the element is pressed in, which swaps the light and the dark
	 * @param high the light colour
	 * @param middle the middle colour
	 * @param low the dark colour
	 */
	static void stroke(Graphics2D g, Shape shape, WindowStyle style, double strength, boolean inset, Color high,
		Color middle, Color low)
	{
		Rectangle2D b = shape.getBounds2D();
		if (b.getHeight() <= 0 || strength <= 0)
			return;

		Color[] colors = inset
			? new Color[] {GlassTokens.of(low, strength), GlassTokens.of(middle, strength),
				GlassTokens.of(high, strength)}
			: new Color[] {GlassTokens.of(high, strength), GlassTokens.of(middle, strength),
				GlassTokens.of(low, strength)};
		float[] stops = {0f, (float) GlassTokens.BORDER_MIDDLE, 1f};
		Paint savedPaint = g.getPaint();
		Stroke savedStroke = g.getStroke();
		g.setPaint(new LinearGradientPaint(0f, (float) b.getY(), 0f, (float) b.getMaxY(), stops, colors));
		g.setStroke(new BasicStroke((float) Math.max(1.0, GlassTokens.BORDER_WIDTH * style.scale())));
		g.draw(shape);
		g.setStroke(savedStroke);
		g.setPaint(savedPaint);
	}

	/**
	 * Paints a small raised element filled with the accent metal: light at the top, dark at the bottom, with shadows
	 * outside and a bevel inside. It has no border of its own, the metal is its edge.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 * @param lift how lifted it is: 1 for resting, {@link GlassTokens#LIFT} when hovered
	 */
	static void raisedMetal(Graphics2D g, Shape shape, WindowStyle style, double lift)
	{
		GlassBorder metal = style.theme().border();
		int pad = px(style, 3 * GlassTokens.SMALL_DARK_BLUR * lift + GlassTokens.SMALL_DARK_DY * lift + 2);
		sprite(g, shape, style, KIND_METAL, metal.accentMid().getRGB(), (int) Math.round(lift * 100), pad,
			(sg, local) ->
			{
				shadowsSmall(sg, local, style, lift);
				metalFill(sg, local, metal, GlassTokens.METAL_ALPHA);
				bevel(sg, local, style, GlassTokens.SMALL_BEVEL, GlassTokens.SMALL_BEVEL_LIGHT,
					GlassTokens.SMALL_BEVEL_DARK);
			});
	}

	/**
	 * Paints a pressed-in element filled with the accent metal, a little darker, with inner shadows.
	 *
	 * @param g the graphics
	 * @param shape the outline
	 * @param style the style
	 */
	static void pressedMetal(Graphics2D g, Shape shape, WindowStyle style)
	{
		GlassBorder metal = style.theme().border();
		sprite(g, shape, style, KIND_METAL_PRESSED, metal.accentMid().getRGB(), 0, 0, (sg, local) ->
		{
			metalFill(sg, local, metal, GlassTokens.METAL_ALPHA * 0.9);
			sg.setColor(GlassTokens.black(GlassTokens.METAL_PRESSED_SHADE));
			sg.fill(local);
			SoftShadows.inner(sg, local, GlassTokens.black(GlassTokens.PRESSED_DARK),
				px(style, GlassTokens.PRESSED_DARK_OFFSET), px(style, GlassTokens.PRESSED_DARK_OFFSET),
				Math.max(1, px(style, GlassTokens.PRESSED_DARK_BLUR)));
			SoftShadows.inner(sg, local, GlassTokens.white(GlassTokens.PRESSED_LIGHT),
				px(style, GlassTokens.PRESSED_LIGHT_OFFSET), px(style, GlassTokens.PRESSED_LIGHT_OFFSET),
				Math.max(1, px(style, GlassTokens.PRESSED_LIGHT_BLUR)));
		});
	}

	/**
	 * Fills a shape with the accent metal: a vertical gradient of the light, body and dark colours with a thin bright
	 * line along the top.
	 *
	 * @param g the graphics
	 * @param shape the shape
	 * @param metal the metal
	 * @param opacity how opaque the metal is, from 0 to 1
	 */
	static void metalFill(Graphics2D g, Shape shape, GlassBorder metal, double opacity)
	{
		metalFill(g, shape, metal.accentHigh(), metal.accentMid(), metal.accentLow(), opacity);
	}

	/**
	 * Fills a shape with a metal of any three colours: a vertical gradient with a thin bright sheen over its top half.
	 *
	 * @param g the graphics
	 * @param shape the shape
	 * @param high the light colour at the top
	 * @param middle the body colour half way down
	 * @param low the dark colour at the bottom
	 * @param opacity how opaque the metal is, from 0 to 1
	 */
	static void metalFill(Graphics2D g, Shape shape, Color high, Color middle, Color low, double opacity)
	{
		Rectangle2D b = shape.getBounds2D();
		Color[] colors = {GlassTokens.of(high, opacity), GlassTokens.of(middle, opacity),
			GlassTokens.of(low, opacity)};
		float[] stops = {0f, (float) GlassTokens.METAL_MIDDLE, 1f};
		Paint saved = g.getPaint();
		g.setPaint(new LinearGradientPaint(0f, (float) b.getY(), 0f, (float) b.getMaxY(), stops, colors));
		g.fill(shape);
		g.setPaint(new GradientPaint(0f, (float) b.getY(), GlassTokens.white(GlassTokens.METAL_SHEEN), 0f,
			(float) (b.getY() + b.getHeight() * 0.5), GlassTokens.white(0)));
		g.fill(shape);
		g.setPaint(saved);
	}

	/**
	 * Paints a bond heart. A full one is raised metal, red or gold when the bond is complete, with a shadow, a bevel
	 * and a bright rim. An empty one keeps a grey fill and is pressed in, with a dull metal red border.
	 *
	 * @param g the graphics
	 * @param shape the heart's outline
	 * @param style the style
	 * @param filled whether this heart is earned
	 * @param gold whether the bond is complete, so the earned hearts are gold
	 */
	static void heart(Graphics2D g, Shape shape, WindowStyle style, boolean filled, boolean gold)
	{
		int pad = px(style, 3 * GlassTokens.SMALL_DARK_BLUR + GlassTokens.SMALL_DARK_DY + 2);
		if (filled)
		{
			Color[] metal = gold ? GlassTokens.HEART_GOLD : GlassTokens.HEART_RED;
			sprite(g, shape, style, KIND_HEART, metal[1].getRGB(), 100, pad, (sg, local) ->
			{
				shadowsSmall(sg, local, style, 1.0);
				metalFill(sg, local, metal[0], metal[1], metal[2], 1.0);
				bevel(sg, local, style, GlassTokens.SMALL_BEVEL, GlassTokens.SMALL_BEVEL_LIGHT,
					GlassTokens.SMALL_BEVEL_DARK);
				stroke(sg, local, style, 0.9, false, metal[0], metal[1], metal[2]);
			});
			return;
		}

		sprite(g, shape, style, KIND_HEART_EMPTY, 0, 0, 0, (sg, local) ->
		{
			sg.setColor(GlassTokens.EMPTY_HEART);
			sg.fill(local);
			SoftShadows.inner(sg, local, GlassTokens.black(GlassTokens.PRESSED_DARK),
				px(style, GlassTokens.PRESSED_DARK_OFFSET), px(style, GlassTokens.PRESSED_DARK_OFFSET),
				Math.max(1, px(style, GlassTokens.PRESSED_DARK_BLUR)));
			Color[] dull = GlassTokens.HEART_DULL;
			stroke(sg, local, style, 1.0, true, dull[0], dull[1], dull[2]);
		});
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
		chip(g, shape, style, accent, null);
	}

	/**
	 * Paints a chip whose metal outline has its own colours, such as a move's pill coloured by the kind of attack.
	 *
	 * @param g the graphics
	 * @param shape the chip's outline
	 * @param style the style
	 * @param accent whether it stands out
	 * @param outline the three colours (light, middle, dark) of the outline, or null for the theme's border
	 */
	static void chip(Graphics2D g, Shape shape, WindowStyle style, boolean accent, Color[] outline)
	{
		if (accent)
		{
			raisedMetal(g, shape, style, 1.0);
			return;
		}

		raisedSmall(g, shape, style, raisedFill(0), 1.0);
		if (outline == null)
			border(g, shape, style, 1.0);
		else
			stroke(g, shape, style, 1.0, false, outline[0], outline[1], outline[2]);
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
			raisedMetal(g, shape, style, lift(hover));
		}
		else
		{
			pressed(g, shape, style, GlassTokens.black(GlassTokens.INSET_FILL));
			borderInset(g, shape, style, 1.0);
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
		GlassBorder metal = style.theme().border();
		Color bottom = style.color(fill);
		Color top = fill == ThemeColor.XP_FILL ? metal.accentHigh()
			: WindowTheme.mix(bottom, Color.WHITE, GlassTokens.HP_TOP_LIGHT);
		if (fill == ThemeColor.XP_FILL)
			bottom = metal.accentLow();

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
	 * Gives how far a bubble's shadows reach beyond its edge, which is the room its wash is made for.
	 *
	 * @param style the style
	 * @return the pad in pixels
	 */
	static int discPad(WindowStyle style)
	{
		return px(style, 3 * GlassTokens.SMALL_DARK_BLUR + GlassTokens.SMALL_DARK_DY + 2);
	}

	/**
	 * Paints the glass of a round wild-creature bubble: two soft shadows, the scene wash, the window tint, the
	 * bubble's own colour, a soft highlight at its top left, a bevel and a thin rim in its colour. The
	 * icon is painted by the caller on top.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param cx the centre x
	 * @param cy the centre y
	 * @param r the radius
	 * @param bubble the bubble's colour
	 * @param wash the scene wash round the bubble, or null
	 */
	static void disc(Graphics2D g, WindowStyle style, double cx, double cy, double r, Color bubble,
		BufferedImage wash)
	{
		Ellipse2D body = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
		shadowsSmall(g, body, style, 1.0);
		Shape clip = g.getClip();
		g.clip(body);
		GlassPanel.washWithin(g, body, style, wash, Readability.BODY, discPad(style));
		g.setColor(GlassPanel.tint(style, wash != null ? GlassTokens.TINT_OVER_WASH : GlassTokens.TINT_SOLID));
		g.fill(body);
		g.setColor(GlassTokens.of(bubble, GlassTokens.BUBBLE_TINT));
		g.fill(body);
		g.setPaint(new RadialGradientPaint((float) (cx + r * GlassTokens.BUBBLE_HIGHLIGHT_X),
			(float) (cy + r * GlassTokens.BUBBLE_HIGHLIGHT_Y), (float) (r * GlassTokens.BUBBLE_HIGHLIGHT_RADIUS),
			new float[] {0f, 1f}, new Color[] {GlassTokens.white(GlassTokens.BUBBLE_HIGHLIGHT),
				GlassTokens.white(0)}));
		g.fill(body);
		g.setClip(clip);
		bevel(g, body, style, GlassTokens.BUBBLE_BEVEL, GlassTokens.BUBBLE_BEVEL_LIGHT, GlassTokens.BUBBLE_BEVEL_DARK);
		Stroke stroke = g.getStroke();
		g.setStroke(new BasicStroke(GlassTokens.BUBBLE_RIM));
		g.setColor(GlassTokens.of(WindowTheme.mix(bubble, Color.WHITE, GlassTokens.BUBBLE_RIM_WHITE),
			GlassTokens.BUBBLE_RIM_ALPHA));
		double inset = GlassTokens.BUBBLE_RIM / 2.0;
		g.draw(new Ellipse2D.Double(cx - r + inset, cy - r + inset, 2 * r - 2 * inset, 2 * r - 2 * inset));
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
