/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The pin button on a task row and on an egg row: where it sits, how it is drawn, its hit and its words. The square
 * sits at the right end of the top line of the row, and the row's own words and bar stop short of it. Holds no
 * RuneLite types.
 */
final class PinButton
{
	/** The side of the button's square in layout units. */
	static final double UNITS = 22;

	/** The words on a button that pins. */
	static final String PIN_TIP = "Pin to the tracker";

	/** The words on a button that unpins. */
	static final String UNPIN_TIP = "Unpin from the tracker";

	private static final double GAP_UNITS = 6;
	private static final double DIM = 0.4;
	private static final double UNPINNED_TILT = 40;

	private PinButton()
	{
	}

	/**
	 * @param style the style
	 * @param line the height of the line of words at the top of the row, which the button sits in
	 * @return the width at the right end of the row that the button takes, gap included
	 */
	static int column(WindowStyle style, int line)
	{
		return side(style, line) + style.px(GAP_UNITS);
	}

	/**
	 * @param style the style
	 * @param row a row's box
	 * @param line the height of the line of words at the top of the row
	 * @return the button's square, at the right end of that line
	 */
	static Rectangle bounds(WindowStyle style, Rectangle row, int line)
	{
		int side = side(style, line);
		return new Rectangle(row.x + row.width - side, row.y + Math.max(0, (line - side) / 2), side, side);
	}

	/**
	 * @param style the style
	 * @param row a row's box
	 * @param line the height of the line of words at the top of the row
	 * @return the part of the row the button leaves free, for the row's own words, bar and hit
	 */
	static Rectangle rest(WindowStyle style, Rectangle row, int line)
	{
		return new Rectangle(row.x, row.y, Math.max(0, row.width - column(style, line)), row.height);
	}

	/**
	 * Decides why a button cannot be pressed.
	 *
	 * @param pinned whether the thing is pinned now
	 * @param finished whether it is a finished task
	 * @param used how many things are pinned
	 * @return null when the button can be pressed, otherwise the reason
	 */
	static String refusal(boolean pinned, boolean finished, int used)
	{
		return PinRules.canPin(used, pinned, finished);
	}

	/**
	 * Makes the button's hit.
	 *
	 * @param id the hit's id
	 * @param bounds the button's square
	 * @param order the place in the focus order
	 * @param pinned whether the thing is pinned now
	 * @param refusal why the button cannot be pressed, or null
	 * @return the hit; a disabled one carries the reason as its tooltip
	 */
	static Hit hit(String id, Rectangle bounds, int order, boolean pinned, String refusal)
	{
		return new Hit(id, Hit.Kind.BUTTON, bounds, order, refusal == null,
			refusal != null ? refusal : pinned ? UNPIN_TIP : PIN_TIP);
	}

	/**
	 * Paints the button: a row-coloured square with a push pin: filled in the accent colour and upright when pinned,
	 * outlined and tilted when not, dim when it cannot be pressed.
	 *
	 * @param g the graphics
	 * @param ctx what the frame needs
	 * @param box the button's square
	 * @param id the hit's id
	 * @param pinned whether the thing is pinned now
	 * @param enabled whether the button can be pressed
	 */
	static void paint(Graphics2D g, PageContext ctx, Rectangle box, String id, boolean pinned, boolean enabled)
	{
		WindowStyle st = ctx.style;
		Composite saved = enabled ? g.getComposite() : PageContext.fade(g, DIM);
		Shape shape = st.row(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height), pinned,
			enabled ? ctx.hoverOf(id) : 0, enabled ? ctx.pressOf(id) : 0);
		tack(g, st, box, pinned);
		g.setComposite(saved);
		if (enabled && ctx.focused && id.equals(ctx.highlight))
			st.focusRing(g, shape);
	}

	private static void tack(Graphics2D g, WindowStyle st, Rectangle box, boolean pinned)
	{
		double s = box.width;
		AffineTransform saved = g.getTransform();
		g.translate(box.x, box.y);
		g.scale(s, s);
		if (!pinned)
			g.rotate(Math.toRadians(UNPINNED_TILT), 0.5, 0.5);

		Path2D head = new Path2D.Double();
		head.append(new RoundRectangle2D.Double(0.35, 0.1, 0.3, 0.3, 0.1, 0.1), false);
		Path2D flange = new Path2D.Double();
		flange.append(new RoundRectangle2D.Double(0.22, 0.4, 0.56, 0.12, 0.06, 0.06), false);
		Line2D needle = new Line2D.Double(0.5, 0.52, 0.5, 0.9);
		g.setColor(st.color(pinned ? ThemeColor.ACCENT : ThemeColor.SUBTEXT));
		g.setStroke(new BasicStroke(Math.max(1.5f / (float) s, 0.09f), BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND));
		if (pinned)
		{
			g.fill(head);
			g.fill(flange);
		}
		else
		{
			g.draw(head);
			g.draw(flange);
		}

		g.draw(needle);
		g.setTransform(saved);
	}

	private static int side(WindowStyle style, int line)
	{
		return Math.max(1, Math.min(style.px(UNITS), line));
	}
}
