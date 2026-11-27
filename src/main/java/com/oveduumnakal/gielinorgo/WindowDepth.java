/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Depth on the window, lit from one angle: each kind of element can have a raised side, a bevelled edge and a cast
 * shadow, and each pane of a page has a height. A pane above the page shadows the page and every lower pane beside it;
 * a pane below the page is shaded inside its lit edges.
 *
 * <p>Every amount is a named value, and all of them start at zero, so the window looks as it always has until one is
 * changed (the UI Lab tunes them). While they are zero, nothing is drawn or allocated. Lengths are in layout units, so
 * they grow with the window's scale.
 *
 * <p>A {@link WindowStyle} owns one of these, and a style is made each frame, so what a frame has placed is forgotten
 * with it.
 */
final class WindowDepth
{
	/** Where the light comes from, in degrees clockwise from the top. */
	static final double LIGHT_ANGLE = 315.0;

	/** The colour of a raised side. */
	static final Color SIDE_COLOR = new Color(20, 14, 8, 224);

	/** How bright the lit edge of a bevel is, from 0 to 1. */
	static final double BEVEL_HIGHLIGHT = 0.45;

	/** How dark the far edge of a bevel is, from 0 to 1. */
	static final double BEVEL_SHADE = 0.45;

	/** How dark a cast shadow is, from 0 to 1. */
	static final double SHADOW_STRENGTH = 0.5;

	/** How far a shadow's edge fades, in layout units. */
	static final double SHADOW_BLUR = 5.0;

	/** The distance between one pane height and the next, in layout units. */
	static final double LAYER_GAP = 4.0;

	/** How much of a raised pane's lift shows as its side, from 0 to 1. */
	static final double LAYER_SIDE = 0.25;

	/** The side of a panel, card or pop-up, in layout units. */
	static final double PANEL_EXTRUDE = 0.0;

	/** The bevelled edge of a panel, card or pop-up, in layout units. */
	static final double PANEL_BEVEL = 0.0;

	/** How far a panel, card or pop-up casts its shadow, in layout units. */
	static final double PANEL_SHADOW = 0.0;

	/** The side of a row, in layout units. */
	static final double ROW_EXTRUDE = 0.0;

	/** The bevelled edge of a row, in layout units. */
	static final double ROW_BEVEL = 0.0;

	/** How far a row casts its shadow, in layout units. */
	static final double ROW_SHADOW = 0.0;

	/** The side of a button or tab, in layout units. */
	static final double BUTTON_EXTRUDE = 0.0;

	/** The bevelled edge of a button or tab, in layout units. */
	static final double BUTTON_BEVEL = 0.0;

	/** How far a button or tab casts its shadow, in layout units. */
	static final double BUTTON_SHADOW = 0.0;

	/** The side of a chip or pill, in layout units. */
	static final double CHIP_EXTRUDE = 0.0;

	/** The bevelled edge of a chip or pill, in layout units. */
	static final double CHIP_BEVEL = 0.0;

	/** How far a chip or pill casts its shadow, in layout units. */
	static final double CHIP_SHADOW = 0.0;

	/** The side of a bubble, in layout units. */
	static final double BUBBLE_EXTRUDE = 0.0;

	/** The bevelled edge of a bubble, in layout units. */
	static final double BUBBLE_BEVEL = 0.0;

	/** How far a bubble casts its shadow, in layout units. */
	static final double BUBBLE_SHADOW = 0.0;

	/** The height of the Summary page's name bar, in layer gaps; below zero it sinks. */
	static final double SUMMARY_NAME_BAR_HEIGHT = 0.0;

	/** The height of the Summary page's portrait, in layer gaps. */
	static final double SUMMARY_PORTRAIT_HEIGHT = 0.0;

	/** The height of the Summary page's type badges, in layer gaps. */
	static final double SUMMARY_TYPE_BADGES_HEIGHT = 0.0;

	/** The height of the Summary page's Stats box, in layer gaps. */
	static final double SUMMARY_STATS_HEIGHT = 0.0;

	/** The height of the Summary page's Nature box, in layer gaps. */
	static final double SUMMARY_NATURE_HEIGHT = 0.0;

	/** The height of the Summary page's hearts, in layer gaps. */
	static final double SUMMARY_HEARTS_HEIGHT = 0.0;

	/** The height of the Summary page's Description box, in layer gaps. */
	static final double SUMMARY_DESCRIPTION_HEIGHT = 0.0;

	/** The height of the Summary page's Moves box, in layer gaps. */
	static final double SUMMARY_MOVES_HEIGHT = 0.0;

	/** The height of the Summary page's move pills, in layer gaps. */
	static final double SUMMARY_MOVE_PILLS_HEIGHT = 0.0;

	/** The height of the Summary page's buttons, in layer gaps. */
	static final double SUMMARY_BUTTONS_HEIGHT = 0.0;

	/** The height of the Party page's eggs box, in layer gaps. */
	static final double PARTY_EGGS_HEIGHT = 0.0;

	/** The height of the Party page's tasks box, in layer gaps. */
	static final double PARTY_TASKS_HEIGHT = 0.0;

	/** The height of the Bag page's trainer box, in layer gaps. */
	static final double BAG_TRAINER_HEIGHT = 0.0;

	/** The height of the Bag page's jars box, in layer gaps. */
	static final double BAG_JARS_HEIGHT = 0.0;

	/** The height of the Bag page's healing items box, in layer gaps. */
	static final double BAG_HEALING_ITEMS_HEIGHT = 0.0;

	/** The height of the Bag page's eggs box, in layer gaps. */
	static final double BAG_EGGS_HEIGHT = 0.0;

	/** The height of the Collection page's summary box, in layer gaps. */
	static final double COLLECTION_SUMMARY_HEIGHT = 0.0;

	/** The height of the Collection page's list, in layer gaps. */
	static final double COLLECTION_LIST_HEIGHT = 0.0;

	/** The height of the Collection page's details box, in layer gaps. */
	static final double COLLECTION_DETAILS_HEIGHT = 0.0;

	/** The height of the Tasks page's tasks box, in layer gaps. */
	static final double TASKS_TASKS_HEIGHT = 0.0;

	/** The height of the Tasks page's milestones box, in layer gaps. */
	static final double TASKS_MILESTONES_HEIGHT = 0.0;

	/** The margin, in pixels, around what a shadow or side can reach, so a soft edge is never cut. */
	private static final double REACH_MARGIN = 4.0;

	/** How much of a shadow's strength its solid middle takes; the rest goes to the fading edge. */
	private static final double SHADOW_CORE = 0.6;

	/** The kinds of element that take depth, each with its own amounts. */
	enum Element
	{
		PANEL, ROW, BUTTON, CHIP, BUBBLE;

		/** @return the raised side, in layout units */
		double extrude()
		{
			switch (this)
			{
				case PANEL: return PANEL_EXTRUDE;
				case ROW: return ROW_EXTRUDE;
				case BUTTON: return BUTTON_EXTRUDE;
				case CHIP: return CHIP_EXTRUDE;
				default: return BUBBLE_EXTRUDE;
			}
		}

		/** @return the bevelled edge, in layout units */
		double bevel()
		{
			switch (this)
			{
				case PANEL: return PANEL_BEVEL;
				case ROW: return ROW_BEVEL;
				case BUTTON: return BUTTON_BEVEL;
				case CHIP: return CHIP_BEVEL;
				default: return BUBBLE_BEVEL;
			}
		}

		/** @return how far the shadow falls, in layout units */
		double shadow()
		{
			switch (this)
			{
				case PANEL: return PANEL_SHADOW;
				case ROW: return ROW_SHADOW;
				case BUTTON: return BUTTON_SHADOW;
				case CHIP: return CHIP_SHADOW;
				default: return BUBBLE_SHADOW;
			}
		}
	}

	/** The panes of the pages that have a height of their own. {@link #NONE} is anything else, at the page's level. */
	enum Pane
	{
		NONE, SUMMARY_NAME_BAR, SUMMARY_PORTRAIT, SUMMARY_TYPE_BADGES, SUMMARY_STATS, SUMMARY_NATURE, SUMMARY_HEARTS,
		SUMMARY_DESCRIPTION, SUMMARY_MOVES, SUMMARY_MOVE_PILLS, SUMMARY_BUTTONS, PARTY_EGGS, PARTY_TASKS, BAG_TRAINER,
		BAG_JARS, BAG_HEALING_ITEMS, BAG_EGGS, COLLECTION_SUMMARY, COLLECTION_LIST, COLLECTION_DETAILS, TASKS_TASKS,
		TASKS_MILESTONES;

		private static final Pane[] ALL = values();

		/** @return the height, in layer gaps: above zero the pane stands out, below zero it sinks */
		double height()
		{
			switch (this)
			{
				case SUMMARY_NAME_BAR: return SUMMARY_NAME_BAR_HEIGHT;
				case SUMMARY_PORTRAIT: return SUMMARY_PORTRAIT_HEIGHT;
				case SUMMARY_TYPE_BADGES: return SUMMARY_TYPE_BADGES_HEIGHT;
				case SUMMARY_STATS: return SUMMARY_STATS_HEIGHT;
				case SUMMARY_NATURE: return SUMMARY_NATURE_HEIGHT;
				case SUMMARY_HEARTS: return SUMMARY_HEARTS_HEIGHT;
				case SUMMARY_DESCRIPTION: return SUMMARY_DESCRIPTION_HEIGHT;
				case SUMMARY_MOVES: return SUMMARY_MOVES_HEIGHT;
				case SUMMARY_MOVE_PILLS: return SUMMARY_MOVE_PILLS_HEIGHT;
				case SUMMARY_BUTTONS: return SUMMARY_BUTTONS_HEIGHT;
				case PARTY_EGGS: return PARTY_EGGS_HEIGHT;
				case PARTY_TASKS: return PARTY_TASKS_HEIGHT;
				case BAG_TRAINER: return BAG_TRAINER_HEIGHT;
				case BAG_JARS: return BAG_JARS_HEIGHT;
				case BAG_HEALING_ITEMS: return BAG_HEALING_ITEMS_HEIGHT;
				case BAG_EGGS: return BAG_EGGS_HEIGHT;
				case COLLECTION_SUMMARY: return COLLECTION_SUMMARY_HEIGHT;
				case COLLECTION_LIST: return COLLECTION_LIST_HEIGHT;
				case COLLECTION_DETAILS: return COLLECTION_DETAILS_HEIGHT;
				case TASKS_TASKS: return TASKS_TASKS_HEIGHT;
				case TASKS_MILESTONES: return TASKS_MILESTONES_HEIGHT;
				default: return 0.0;
			}
		}

		/** @return whether any pane has a height other than zero */
		static boolean anyLayered()
		{
			for (Pane pane : ALL)
			{
				if (pane.height() != 0)
					return true;
			}

			return false;
		}
	}

	/** A pane drawn this frame: where it landed on the canvas, and how high it sits. */
	private static final class Placed
	{
		final Shape shape;
		final double height;

		private Placed(Shape shape, double height)
		{
			this.shape = shape;
			this.height = height;
		}
	}

	private final double scale;
	private final boolean layered;
	private List<Placed> placed;

	/**
	 * @param scale the window scale, the pixels in one layout unit
	 */
	WindowDepth(double scale)
	{
		this.scale = scale;
		this.layered = Pane.anyLayered();
	}

	/**
	 * @param element the kind of element
	 * @return whether an element of this kind would draw any depth, so a caller can skip making its outline
	 */
	boolean shows(Element element)
	{
		return layered || element.extrude() > 0 || element.bevel() > 0 || element.shadow() > 0;
	}

	/**
	 * Draws what lies under an element, before its body: its shadow, then its side. Nothing is drawn on the element
	 * itself, so a glass face stays clear.
	 *
	 * @param g the graphics
	 * @param element the kind of element
	 * @param shape the element's outline
	 * @param pane the pane it is, or {@link Pane#NONE}
	 */
	void under(Graphics2D g, Element element, Shape shape, Pane pane)
	{
		double height = pane.height();
		double raised = Math.max(0, height) * LAYER_GAP * scale;
		double extrude = element.extrude() * scale + raised * LAYER_SIDE;
		double shadow = element.shadow() * scale + raised;
		if (extrude > 0 || shadow > 0)
			under(g, shape, height, extrude, shadow);
	}

	/**
	 * Draws a shadow and a side under a shape, in given amounts.
	 *
	 * @param g the graphics
	 * @param shape the element's outline
	 * @param height the element's pane height, in layer gaps
	 * @param extrude the side, in pixels
	 * @param shadow how far the shadow falls, in pixels
	 */
	void under(Graphics2D g, Shape shape, double height, double extrude, double shadow)
	{
		double angle = Math.toRadians(LIGHT_ANGLE);
		double dx = -Math.sin(angle);
		double dy = Math.cos(angle);
		double blur = SHADOW_BLUR * scale;
		double reach = extrude + shadow + blur + REACH_MARGIN;
		Graphics2D copy = around(g, shape, reach);
		try
		{
			if (height > 0)
				keepOffHigher(g, copy, shape, height, reach);

			if (shadow > 0)
				soft(copy, moved(shape, dx * (shadow + extrude), dy * (shadow + extrude)), SHADOW_STRENGTH, blur);

			if (extrude > 0)
			{
				Area side = new Area();
				int steps = (int) Math.ceil(extrude);
				for (int i = 1; i <= steps; i++)
				{
					double at = extrude * i / steps;
					side.add(new Area(moved(shape, dx * at, dy * at)));
				}

				copy.setColor(SIDE_COLOR);
				copy.fill(side);
			}
		}
		finally
		{
			copy.dispose();
		}
	}

	/**
	 * Draws what lies over an element's body, before what sits on it: its bevel, the shadows of higher panes that
	 * reach it, and the shade inside a sunk pane. A named pane is then remembered for the panes drawn after it.
	 *
	 * @param g the graphics
	 * @param element the kind of element
	 * @param shape the element's outline
	 * @param pane the pane it is, or {@link Pane#NONE}
	 */
	void over(Graphics2D g, Element element, Shape shape, Pane pane)
	{
		double width = element.bevel() * scale;
		if (width <= 0 && !layered)
			return;

		over(g, shape, pane.height(), width);
		if (layered && pane != Pane.NONE)
			remember(g, shape, pane.height());
	}

	/**
	 * Draws a bevel in a given width over a shape, and the shade a pane at this height takes from higher panes drawn
	 * before it or from sinking.
	 *
	 * @param g the graphics
	 * @param shape the element's outline
	 * @param height the element's pane height, in layer gaps
	 * @param width the bevel, in pixels
	 */
	void over(Graphics2D g, Shape shape, double height, double width)
	{
		double gap = LAYER_GAP * scale;
		double blur = SHADOW_BLUR * scale;
		double angle = Math.toRadians(LIGHT_ANGLE);
		double dx = -Math.sin(angle);
		double dy = Math.cos(angle);
		Graphics2D copy = (Graphics2D) g.create();
		try
		{
			copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (width > 0)
				bevel(copy, shape, width, dx, dy);

			copy.clip(shape);
			if (placed != null)
			{
				for (Placed higher : placed)
				{
					double drop = (higher.height - height) * gap;
					if (drop > 0)
						castOnto(copy, local(g, higher.shape), shape, dx * drop, dy * drop, blur);
				}
			}

			double sunk = Math.max(0, -height) * gap;
			if (sunk > 0)
			{
				Area rim = new Area(grown(shape, sunk + blur + REACH_MARGIN));
				rim.subtract(new Area(moved(shape, dx * sunk, dy * sunk)));
				soft(copy, rim, SHADOW_STRENGTH, blur);
			}
		}
		finally
		{
			copy.dispose();
		}
	}

	/**
	 * Remembers a pane drawn this frame, so it shadows the lower panes drawn after it.
	 *
	 * @param g the graphics the pane was drawn with
	 * @param shape the pane's outline
	 * @param height the pane's height, in layer gaps
	 */
	void remember(Graphics2D g, Shape shape, double height)
	{
		if (placed == null)
			placed = new ArrayList<>();

		placed.add(new Placed(g.getTransform().createTransformedShape(shape), height));
	}

	private static void bevel(Graphics2D g, Shape shape, double width, double dx, double dy)
	{
		Area lit = new Area(shape);
		lit.subtract(new Area(moved(shape, dx * width, dy * width)));
		g.setColor(alpha(0xFFFFFF, BEVEL_HIGHLIGHT));
		g.fill(lit);
		Area dark = new Area(shape);
		dark.subtract(new Area(moved(shape, -dx * width, -dy * width)));
		g.setColor(alpha(0, BEVEL_SHADE));
		g.fill(dark);
	}

	/** A pane's shadow does not fall on a neighbour as high as itself, though it does on whatever it sits on. */
	private void keepOffHigher(Graphics2D g, Graphics2D copy, Shape shape, double height, double reach)
	{
		if (placed == null)
			return;

		Area allowed = new Area(grown(shape, reach));
		for (Placed other : placed)
		{
			Shape there = local(g, other.shape);
			if (other.height >= height && !there.getBounds2D().contains(shape.getBounds2D()))
				allowed.subtract(new Area(there));
		}

		copy.clip(allowed);
	}

	private static void castOnto(Graphics2D g, Shape higher, Shape receiver, double x, double y, double blur)
	{
		Shape cast = moved(higher, x, y);
		if (!cast.getBounds2D().intersects(receiver.getBounds2D()))
			return;

		Graphics2D copy = (Graphics2D) g.create();
		try
		{
			Area clear = new Area(grown(receiver, REACH_MARGIN));
			clear.subtract(new Area(higher));
			copy.clip(clear);
			soft(copy, cast, SHADOW_STRENGTH, blur);
		}
		finally
		{
			copy.dispose();
		}
	}

	/** @return a copy of the graphics clipped to everything but the element, so its face stays clear */
	private static Graphics2D around(Graphics2D g, Shape shape, double reach)
	{
		Graphics2D copy = (Graphics2D) g.create();
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Area outside = new Area(grown(shape, reach));
		outside.subtract(new Area(shape));
		copy.clip(outside);
		return copy;
	}

	/** Fills a shape as a shadow, its edge fading over the blur. */
	private static void soft(Graphics2D g, Shape cast, double strength, double blur)
	{
		int layers = Math.max(1, (int) Math.round(blur));
		g.setColor(alpha(0, strength / (layers * SHADOW_CORE + 1)));
		for (int i = layers; i >= 1; i--)
		{
			g.setStroke(new BasicStroke(2f * i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(cast);
		}

		g.setColor(alpha(0, strength * SHADOW_CORE));
		g.fill(cast);
	}

	private static Rectangle2D grown(Shape shape, double reach)
	{
		Rectangle2D b = shape.getBounds2D();
		return new Rectangle2D.Double(b.getX() - reach, b.getY() - reach, b.getWidth() + 2 * reach,
			b.getHeight() + 2 * reach);
	}

	private static Shape moved(Shape shape, double x, double y)
	{
		return AffineTransform.getTranslateInstance(x, y).createTransformedShape(shape);
	}

	/** @return a shape placed on the canvas, in the coordinates the graphics draws in now */
	private static Shape local(Graphics2D g, Shape onCanvas)
	{
		try
		{
			AffineTransform toLocal = g.getTransform().createInverse();
			return toLocal.createTransformedShape(onCanvas);
		}
		catch (NoninvertibleTransformException e)
		{
			return onCanvas;
		}
	}

	private static Color alpha(int rgb, double amount)
	{
		int a = (int) Math.round(255 * Math.max(0.0, Math.min(1.0, amount)));
		return new Color(a << 24 | rgb, true);
	}
}
