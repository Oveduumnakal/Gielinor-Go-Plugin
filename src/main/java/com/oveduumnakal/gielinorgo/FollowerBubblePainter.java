/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Paints the bubbles over the follower in the same look as the wild creature bubbles: glass or a coloured band round a
 * dark outline. A creature shows its portrait with its health round the rim, an item its picture. Holds no RuneLite
 * types.
 */
final class FollowerBubblePainter
{
	private static final Color SWITCH_BASE = new Color(0x2AA7A0);
	private static final Color ITEM_BASE = new Color(0x4C9A3F);
	private static final Color RUN_BASE = new Color(0xE0A030);
	private static final Color MEMBER_BASE = new Color(0x6C7A89);
	private static final Color GREY = new Color(0x808080);
	private static final Color OUTLINE = new Color(0x141414);
	private static final Color GLYPH = new Color(0xF2F2F2);
	private static final Color HEALTHY = new Color(0x5BC44A);
	private static final Color HURT = new Color(0xE0B030);
	private static final Color LOW = new Color(0xD8463A);
	private static final double INNER = 0.8;

	private final PortraitCache portraits;

	/**
	 * Makes a painter.
	 *
	 * @param portraits where creature pictures come from, or null to draw none
	 */
	FollowerBubblePainter(PortraitCache portraits)
	{
		this.portraits = portraits;
	}

	/**
	 * Paints the bubbles.
	 *
	 * @param g the graphics; its state is restored
	 * @param dots the bubbles, back to front
	 * @param under the one the pointer is on, or null
	 * @param style the window's look
	 */
	void paint(Graphics2D g, List<FollowerBubbles.Dot> dots, FollowerBubbles.Dot under, WindowStyle style)
	{
		Graphics2D copy = (Graphics2D) g.create();
		try
		{
			WindowStyle.prepare(copy);
			for (FollowerBubbles.Dot dot : dots)
			{
				boolean hovered = under != null && under.x() == dot.x() && under.y() == dot.y();
				disc(copy, dot, hovered, style);
			}
		}
		finally
		{
			copy.dispose();
		}
	}

	private void disc(Graphics2D g, FollowerBubbles.Dot dot, boolean hovered, WindowStyle style)
	{
		double cx = Math.round(dot.x());
		double cy = Math.round(dot.y());
		double r = dot.radius() * (hovered ? 1.12 : 1.0);
		Color base = base(dot.kind());
		Color fill = dot.enabled() ? base : WindowTheme.mix(base, GREY, 0.75);
		Composite saved = g.getComposite();
		if (!dot.enabled())
			g.setComposite(AlphaComposite.SrcOver.derive((float) BubblePainter.DISABLED_ALPHA));

		WindowDepth depth = style.depth();
		Shape disc = depth.shows(WindowDepth.Element.BUBBLE) ? circle(cx, cy, r) : null;
		if (disc != null)
			depth.under(g, WindowDepth.Element.BUBBLE, disc, WindowDepth.Pane.NONE);

		if (style.theme().glass())
		{
			GlassStyle.disc(g, style, cx, cy, r, fill, null);
		}
		else
		{
			Area band = new Area(circle(cx, cy, r));
			band.subtract(new Area(circle(cx, cy, r * INNER)));
			g.setColor(fill);
			g.fill(band);
			g.setColor(new Color(0, 0, 0, 110));
			g.fill(circle(cx, cy, r * INNER));
			g.setColor(OUTLINE);
			g.setStroke(new BasicStroke(2f));
			g.draw(circle(cx, cy, r));
		}

		if (disc != null)
			depth.over(g, WindowDepth.Element.BUBBLE, disc, WindowDepth.Pane.NONE);

		face(g, dot, cx, cy, r);
		g.setComposite(saved);
		if (hovered)
		{
			g.setColor(new Color(255, 255, 255, 180));
			g.setStroke(new BasicStroke(2f));
			g.draw(circle(cx, cy, r + 2.5));
		}
	}

	private static Color base(FollowerBubbles.Kind kind)
	{
		switch (kind)
		{
			case SWITCH: return SWITCH_BASE;
			case ITEM: return ITEM_BASE;
			case RUN: return RUN_BASE;
			case POTION: return ITEM_BASE;
			default: return MEMBER_BASE;
		}
	}

	private void face(Graphics2D g, FollowerBubbles.Dot dot, double cx, double cy, double r)
	{
		switch (dot.kind())
		{
			case SWITCH:
				arrows(g, cx, cy, r);
				break;
			case ITEM:
				backpack(g, cx, cy, r);
				break;
			case RUN:
				runner(g, cx, cy, r);
				break;
			case POTION:
				ItemIcons.draw(g, dot.item().type(), cx - r * 0.75, cy - r * 0.75, r * 1.5);
				label(g, Integer.toString(dot.item().count()), cx + r * 0.55, cy + r * 0.85);
				break;
			default:
				member(g, dot.member(), cx, cy, r);
				break;
		}
	}

	private void member(Graphics2D g, FollowerOffer.Member member, double cx, double cy, double r)
	{
		BufferedImage picture = portraits == null ? null : portraits.image(member.species(), member.npcId());
		if (picture != null)
		{
			Shape clip = g.getClip();
			g.clip(circle(cx, cy, r * INNER));
			CrispImages.draw(g, picture, cx - r * INNER, cy - r * INNER, 2 * r * INNER);
			g.setClip(clip);
		}
		else
		{
			label(g, member.name().substring(0, Math.min(2, member.name().length())), cx, cy + 4);
		}

		double health = Math.max(0, Math.min(1, member.health()));
		g.setColor(health > 0.5 ? HEALTHY : health > 0.2 ? HURT : LOW);
		g.setStroke(new BasicStroke(2.5f));
		g.draw(new Arc2D.Double(cx - r + 1.5, cy - r + 1.5, 2 * r - 3, 2 * r - 3, 90, -360 * health, Arc2D.OPEN));
		label(g, Integer.toString(member.level()), cx, cy + r + 3);
	}

	/** Two arrows going round: one to the right on top, one to the left below. */
	private static void arrows(Graphics2D g, double cx, double cy, double r)
	{
		double w = r * 0.5;
		double h = r * 0.28;
		g.setColor(GLYPH);
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int side = -1; side <= 1; side += 2)
		{
			double y = cy + side * h;
			Path2D arrow = new Path2D.Double();
			arrow.moveTo(cx + side * w, y);
			arrow.lineTo(cx - side * w, y);
			arrow.moveTo(cx - side * w * 0.4, y - h * 0.8);
			arrow.lineTo(cx - side * w, y);
			arrow.lineTo(cx - side * w * 0.4, y + h * 0.8);
			g.draw(arrow);
		}
	}

	/** A backpack from the front: a handle, side pockets, the body with its flap seam, buckle and front pocket. */
	private static void backpack(Graphics2D g, double cx, double cy, double r)
	{
		double s = r * 0.62;
		g.setColor(GLYPH);
		g.setStroke(new BasicStroke((float) (0.18 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Arc2D.Double(cx - 0.36 * s, cy - 1.05 * s, 0.72 * s, 0.70 * s, 0, 180, Arc2D.OPEN));
		g.fill(rounded(cx - 0.96 * s, cy + 0.10 * s, 0.36 * s, 0.72 * s, 0.24 * s));
		g.fill(rounded(cx + 0.60 * s, cy + 0.10 * s, 0.36 * s, 0.72 * s, 0.24 * s));
		g.fill(rounded(cx - 0.70 * s, cy - 0.74 * s, 1.40 * s, 1.74 * s, 0.84 * s));
		g.setColor(OUTLINE);
		g.setStroke(new BasicStroke((float) (0.12 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(cx - 0.70 * s, cy - 0.08 * s, cx + 0.70 * s, cy - 0.08 * s));
		g.draw(rounded(cx - 0.42 * s, cy + 0.30 * s, 0.84 * s, 0.48 * s, 0.20 * s));
		g.fill(rounded(cx - 0.14 * s, cy - 0.22 * s, 0.28 * s, 0.30 * s, 0.08 * s));
	}

	private static RoundRectangle2D rounded(double x, double y, double w, double h, double arc)
	{
		return new RoundRectangle2D.Double(x, y, w, h, arc, arc);
	}

	/** A figure running to the right with speed lines behind it, in thick rounded strokes. */
	private static void runner(Graphics2D g, double cx, double cy, double r)
	{
		double s = r * 0.76;
		g.setColor(GLYPH);
		g.fill(circle(cx + 0.34 * s, cy - 0.66 * s, 0.23 * s));
		limb(g, cx, cy, s, 0.34, new double[] {0.14, -0.28, -0.10, 0.14});
		limb(g, cx, cy, s, 0.19, new double[] {0.12, -0.28, 0.40, -0.01, 0.72, -0.10});
		limb(g, cx, cy, s, 0.19, new double[] {0.10, -0.30, -0.24, -0.30, -0.34, -0.02});
		limb(g, cx, cy, s, 0.23, new double[] {-0.08, 0.14, 0.30, 0.38, 0.20, 0.80});
		limb(g, cx, cy, s, 0.23, new double[] {-0.10, 0.14, -0.30, 0.52, -0.70, 0.56});
		limb(g, cx, cy, s, 0.12, new double[] {-1.00, -0.50, -0.58, -0.50});
		limb(g, cx, cy, s, 0.12, new double[] {-1.08, -0.14, -0.70, -0.14});
		limb(g, cx, cy, s, 0.12, new double[] {-0.94, 0.22, -0.56, 0.22});
	}

	/**
	 * Draws one bent stroke.
	 *
	 * @param width its thickness, as a share of the glyph's size
	 * @param points pairs of across and down from the middle, as shares of the glyph's size
	 */
	private static void limb(Graphics2D g, double cx, double cy, double s, double width, double[] points)
	{
		Path2D path = new Path2D.Double();
		path.moveTo(cx + points[0] * s, cy + points[1] * s);
		for (int i = 2; i < points.length; i += 2)
			path.lineTo(cx + points[i] * s, cy + points[i + 1] * s);

		g.setStroke(new BasicStroke((float) (width * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(path);
	}

	private static void label(Graphics2D g, String text, double centre, double baseline)
	{
		g.setFont(g.getFont().deriveFont(Font.BOLD, 10f));
		int x = (int) Math.round(centre - g.getFontMetrics().stringWidth(text) / 2.0);
		int y = (int) Math.round(baseline);
		g.setColor(OUTLINE);
		g.drawString(text, x + 1, y + 1);
		g.setColor(GLYPH);
		g.drawString(text, x, y);
	}

	private static Ellipse2D circle(double cx, double cy, double r)
	{
		return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
	}
}
