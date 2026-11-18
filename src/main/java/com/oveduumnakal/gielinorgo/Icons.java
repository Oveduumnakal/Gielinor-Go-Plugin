/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The small pictures of the painted window, drawn in code so they recolour with the theme and need no image file: the
 * gender signs, one mark for each combat style, the seen and caught marks, a heart, a star, the lead arrow and an
 * egg. Each draws in the colour the caller set on the graphics, inside the square whose top-left corner and side it
 * is given, and leaves the graphics' stroke as it found it. Gender and style are told apart by shape, never by
 * colour alone. Holds no RuneLite types.
 */
final class Icons
{
	private Icons()
	{
	}

	/**
	 * Draws the sign of a gender: a ring with an arrow for male, a ring with a cross under it for female.
	 *
	 * @param g the graphics, with the colour set
	 * @param gender the gender
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 */
	static void gender(Graphics2D g, Gender gender, double x, double y, double size)
	{
		Stroke saved = g.getStroke();
		g.setStroke(new BasicStroke((float) Math.max(1.0, size / 8.0), BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND));
		double ring = size * 0.56;
		if (gender == Gender.FEMALE)
		{
			double cx = x + size / 2.0;
			g.draw(new Ellipse2D.Double(cx - ring / 2.0, y + size * 0.04, ring, ring));
			g.draw(new Line2D.Double(cx, y + size * 0.04 + ring, cx, y + size * 0.96));
			g.draw(new Line2D.Double(cx - size * 0.2, y + size * 0.78, cx + size * 0.2, y + size * 0.78));
		}
		else
		{
			double rx = x + size * 0.04;
			double ry = y + size * 0.4;
			g.draw(new Ellipse2D.Double(rx, ry, ring, ring));
			double fromX = rx + ring * 0.85;
			double fromY = ry + ring * 0.15;
			double toX = x + size * 0.94;
			double toY = y + size * 0.06;
			g.draw(new Line2D.Double(fromX, fromY, toX, toY));
			g.draw(new Line2D.Double(toX, toY, toX - size * 0.3, toY));
			g.draw(new Line2D.Double(toX, toY, toX, toY + size * 0.3));
		}

		g.setStroke(saved);
	}

	/**
	 * Draws the mark of a combat style: a dagger for Stab, a curved blade for Slash, a hammer for Crush, a bow for
	 * Ranged and a star of four points for Magic.
	 *
	 * @param g the graphics, with the colour set
	 * @param style the style
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 */
	static void style(Graphics2D g, CombatType style, double x, double y, double size)
	{
		AffineTransform saved = g.getTransform();
		Stroke stroke = g.getStroke();
		g.translate(x, y);
		g.scale(size, size);
		g.setStroke(new BasicStroke((float) (Math.max(1.0, size / 9.0) / size), BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND));
		switch (style)
		{
			case STAB:
				stab(g);
				break;
			case SLASH:
				slash(g);
				break;
			case CRUSH:
				crush(g);
				break;
			case RANGED:
				ranged(g);
				break;
			default:
				magic(g);
				break;
		}

		g.setTransform(saved);
		g.setStroke(stroke);
	}

	private static void stab(Graphics2D g)
	{
		Path2D.Double blade = new Path2D.Double();
		blade.moveTo(0.5, 0.0);
		blade.lineTo(0.68, 0.62);
		blade.lineTo(0.32, 0.62);
		blade.closePath();
		g.fill(blade);
		g.draw(new Line2D.Double(0.2, 0.68, 0.8, 0.68));
		g.draw(new Line2D.Double(0.5, 0.68, 0.5, 1.0));
	}

	private static void slash(Graphics2D g)
	{
		Path2D.Double blade = new Path2D.Double();
		blade.moveTo(1.0, 0.0);
		blade.lineTo(0.78, 0.0);
		blade.lineTo(0.12, 0.66);
		blade.lineTo(0.34, 0.88);
		blade.lineTo(1.0, 0.22);
		blade.closePath();
		g.fill(blade);
		g.draw(new Line2D.Double(0.04, 0.58, 0.42, 0.96));
		g.draw(new Line2D.Double(0.02, 0.98, 0.2, 0.8));
	}

	private static void crush(Graphics2D g)
	{
		g.fill(new Rectangle2D.Double(0.14, 0.04, 0.72, 0.36));
		g.draw(new Line2D.Double(0.5, 0.4, 0.5, 1.0));
	}

	private static void ranged(Graphics2D g)
	{
		g.draw(new Arc2D.Double(0.0, 0.02, 0.76, 0.96, 90, -180, Arc2D.OPEN));
		g.draw(new Line2D.Double(0.38, 0.02, 0.38, 0.98));
		g.draw(new Line2D.Double(0.2, 0.5, 1.0, 0.5));
		g.draw(new Line2D.Double(1.0, 0.5, 0.82, 0.34));
		g.draw(new Line2D.Double(1.0, 0.5, 0.82, 0.66));
	}

	private static void magic(Graphics2D g)
	{
		Path2D.Double star = new Path2D.Double();
		star.moveTo(0.5, 0.0);
		star.lineTo(0.62, 0.38);
		star.lineTo(1.0, 0.5);
		star.lineTo(0.62, 0.62);
		star.lineTo(0.5, 1.0);
		star.lineTo(0.38, 0.62);
		star.lineTo(0.0, 0.5);
		star.lineTo(0.38, 0.38);
		star.closePath();
		g.fill(star);
	}

	/**
	 * Draws the mark that tells what the Creature Log knows of a wild creature's species: a filled star for new, an
	 * eye for seen and a jar for caught.
	 *
	 * @param g the graphics, with the colour set
	 * @param kind which mark
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 */
	static void mark(Graphics2D g, WildRecord.Kind kind, double x, double y, double size)
	{
		switch (kind)
		{
			case NEW:
				g.fill(star(x + size / 2.0, y + size * 0.54, size * 0.46));
				break;
			case SEEN:
				eye(g, x, y, size);
				break;
			default:
				jar(g, x, y, size);
				break;
		}
	}

	/**
	 * Draws an eye: an almond outline with a filled pupil.
	 *
	 * @param g the graphics, with the colour set
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 */
	static void eye(Graphics2D g, double x, double y, double size)
	{
		Stroke saved = g.getStroke();
		g.setStroke(new BasicStroke((float) Math.max(1.0, size / 8.0), BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND));
		double cy = y + size / 2.0;
		Path2D.Double almond = new Path2D.Double();
		almond.moveTo(x + size * 0.08, cy);
		almond.quadTo(x + size * 0.5, y + size * 0.0, x + size * 0.92, cy);
		almond.quadTo(x + size * 0.5, y + size * 1.0, x + size * 0.08, cy);
		almond.closePath();
		g.draw(almond);
		double pupil = size * 0.3;
		g.fill(new Ellipse2D.Double(x + (size - pupil) / 2.0, cy - pupil / 2.0, pupil, pupil));
		g.setStroke(saved);
	}

	/**
	 * Draws a jar: a lid bar, a narrow neck and a rounded body, all filled.
	 *
	 * @param g the graphics, with the colour set
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 */
	static void jar(Graphics2D g, double x, double y, double size)
	{
		g.fill(new Rectangle2D.Double(x + size * 0.24, y + size * 0.04, size * 0.52, size * 0.16));
		g.fill(new Rectangle2D.Double(x + size * 0.32, y + size * 0.2, size * 0.36, size * 0.14));
		g.fill(new RoundRectangle2D.Double(x + size * 0.1, y + size * 0.32, size * 0.8, size * 0.64, size * 0.3,
			size * 0.3));
	}

	/**
	 * Builds a heart.
	 *
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 * @return the outline
	 */
	static Path2D.Double heart(double x, double y, double size)
	{
		Path2D.Double path = new Path2D.Double();
		path.moveTo(x + size * 0.5, y + size * 0.92);
		path.curveTo(x - size * 0.2, y + size * 0.4, x + size * 0.15, y - size * 0.1, x + size * 0.5,
			y + size * 0.28);
		path.curveTo(x + size * 0.85, y - size * 0.1, x + size * 1.2, y + size * 0.4, x + size * 0.5,
			y + size * 0.92);
		path.closePath();
		return path;
	}

	/**
	 * Builds a five-pointed star.
	 *
	 * @param cx the centre x
	 * @param cy the centre y
	 * @param radius the distance from the centre to a point
	 * @return the outline
	 */
	static Path2D.Double star(double cx, double cy, double radius)
	{
		Path2D.Double path = new Path2D.Double();
		for (int i = 0; i < 10; i++)
		{
			double r = i % 2 == 0 ? radius : radius * 0.45;
			double a = -Math.PI / 2 + i * Math.PI / 5;
			double px = cx + Math.cos(a) * r;
			double py = cy + Math.sin(a) * r;
			if (i == 0)
				path.moveTo(px, py);
			else
				path.lineTo(px, py);
		}

		path.closePath();
		return path;
	}

	/**
	 * Builds the lead arrow, a triangle that points right.
	 *
	 * @param x the left edge
	 * @param cy the vertical centre
	 * @param size the height and width
	 * @return the shape
	 */
	static Path2D.Double arrow(double x, double cy, double size)
	{
		Path2D.Double path = new Path2D.Double();
		path.moveTo(x, cy - size / 2.0);
		path.lineTo(x + size, cy);
		path.lineTo(x, cy + size / 2.0);
		path.closePath();
		return path;
	}

	/**
	 * Builds an egg.
	 *
	 * @param x the left edge of the square
	 * @param y the top edge
	 * @param size the side of the square
	 * @return the shape
	 */
	static Shape egg(double x, double y, double size)
	{
		Path2D.Double path = new Path2D.Double();
		path.moveTo(x + size * 0.5, y);
		path.curveTo(x + size * 0.95, y, x + size * 0.98, y + size * 0.97, x + size * 0.5, y + size * 0.97);
		path.curveTo(x + size * 0.02, y + size * 0.97, x + size * 0.05, y, x + size * 0.5, y);
		path.closePath();
		return path;
	}
}
