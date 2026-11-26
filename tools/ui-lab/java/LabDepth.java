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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Effects the plugin does not have yet: extruded sides, bevels and cast shadows, lit from one angle. The lab's copy of
 * the painters calls these before and after each element's body. Every amount starts at zero, so nothing changes until
 * asked. Keeping a preset that uses them means moving this drawing into the plugin.
 *
 * <p>Named panes also have a height. A pane above the page casts a shadow onto it and onto every lower pane beside it;
 * a pane below the page is shaded inside its lit edges.
 */
final class LabDepth
{
	/** One adjustable amount, with the range its slider offers. */
	static final class Setting
	{
		final String id;
		final String name;
		final String type;
		final double value;
		final double min;
		final double max;
		final double step;
		final String doc;

		private Setting(String id, String name, String type, double value, double min, double max, double step,
			String doc)
		{
			this.id = id;
			this.name = name;
			this.type = type;
			this.value = value;
			this.min = min;
			this.max = max;
			this.step = step;
			this.doc = doc;
		}
	}

	/** An element that has been drawn this frame, where it landed on the canvas, and how high it sits. */
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

	/** The height of the element being drawn, and whether it is a named pane. */
	private static final class Level
	{
		final double height;
		final boolean named;

		private Level(double height, boolean named)
		{
			this.height = height;
			this.named = named;
		}
	}

	private static final String[] PANES = {"Summary: name bar", "Summary: portrait", "Summary: type badges",
		"Summary: Stats", "Summary: Nature", "Summary: hearts", "Summary: Description", "Summary: Moves",
		"Summary: move pills", "Summary: buttons", "Party: eggs", "Party: tasks", "Bag: trainer", "Bag: jars",
		"Bag: healing items", "Bag: eggs", "Collection: summary", "Collection: list", "Collection: details",
		"Tasks: tasks", "Tasks: milestones"};
	private static final List<Placed> PLACED = new ArrayList<>();
	private static final Deque<Level> LEVELS = new ArrayDeque<>();
	private static String pending;

	private static final String[] ELEMENTS = {"Panels", "Rows", "Buttons and tabs", "Chips", "Bubbles"};
	private static final List<Setting> SETTINGS = new ArrayList<>();
	private static final Map<String, Double> VALUES = new LinkedHashMap<>();

	static
	{
		add("light", "Light angle", "double", 315, 360, 5, "Where the light comes from, clockwise from the top");
		add("sideColor", "Side colour", "Color", 0xE0140E08L, 0, 0, "The colour of an extruded side");
		add("highlight", "Bevel highlight", "double", 0.45, 1, 0.01, "How bright the lit edge of a bevel is");
		add("shade", "Bevel shade", "double", 0.45, 1, 0.01, "How dark the far edge of a bevel is");
		add("shadowStrength", "Shadow strength", "double", 0.5, 1, 0.01, "How dark a cast shadow is");
		add("shadowBlur", "Shadow softness", "double", 5, 20, 1, "How far a shadow's edge fades");
		for (String element : ELEMENTS)
		{
			add(element + ".extrude", element + ": extrusion", "double", 0, 16, 0.5, "Thickness of the side");
			add(element + ".bevel", element + ": bevel", "double", 0, 8, 0.5, "Width of the bevelled edge");
			add(element + ".shadow", element + ": shadow distance", "double", 0, 24, 0.5, "How far the shadow falls");
		}

		add("separation", "Layer gap", "double", 4, 16, 0.5, "The distance between one pane height and the next");
		add("layerSide", "Layer side thickness", "double", 0.25, 1, 0.01, "How much side a raised pane shows");
		for (String pane : PANES)
		{
			SETTINGS.add(new Setting("depth:pane." + pane, pane + " height", "double", 0, -5, 5, 0.5,
				"Above zero the pane stands out from the page; below zero it sinks into it"));
			VALUES.put("depth:pane." + pane, 0.0);
		}
	}

	private LabDepth()
	{
	}

	private static void add(String id, String name, String type, double value, double max, double step, String doc)
	{
		SETTINGS.add(new Setting("depth:" + id, name, type, value, 0, max, step, doc));
		VALUES.put("depth:" + id, value);
	}

	static List<Setting> settings()
	{
		return SETTINGS;
	}

	static void set(String id, double value)
	{
		if (!VALUES.containsKey(id))
			throw new IllegalArgumentException("no such control: " + id);

		VALUES.put(id, value);
	}

	static void reset(String id)
	{
		for (Setting setting : SETTINGS)
		{
			if (setting.id.equals(id))
				VALUES.put(id, setting.value);
		}
	}

	private static double get(String id)
	{
		return VALUES.get("depth:" + id);
	}

	/** Forgets what the last frame placed. */
	static void frame()
	{
		PLACED.clear();
		LEVELS.clear();
		pending = null;
	}

	/**
	 * Names the next element drawn, so it takes that pane's height.
	 *
	 * @param <T> whatever is passed through
	 * @param name the pane
	 * @param through an argument of the drawing call, returned unchanged
	 * @return the argument
	 */
	static <T> T pane(String name, T through)
	{
		if (!VALUES.containsKey("depth:pane." + name))
			throw new IllegalStateException("unknown pane: " + name);

		pending = name;
		return through;
	}

	/** @return a copy of the graphics clipped to everything but the element, so glass faces stay clear */
	private static Graphics2D around(Graphics2D g, Shape shape, double reach)
	{
		Graphics2D copy = (Graphics2D) g.create();
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Area outside = new Area(grown(shape, reach));
		outside.subtract(new Area(shape));
		copy.clip(outside);
		return copy;
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

	/** @return a placed shape in the coordinates the graphics draws in now */
	private static Shape local(Graphics2D g, Shape onCanvas)
	{
		try
		{
			return g.getTransform().createInverse().createTransformedShape(onCanvas);
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

	/** Fills a shape as a shadow, its edge fading over the softness. */
	private static void soft(Graphics2D g, Shape cast, double strength, double blur)
	{
		int layers = Math.max(1, (int) Math.round(blur));
		g.setColor(alpha(0, strength / (layers * 0.6 + 1)));
		for (int i = layers; i >= 1; i--)
		{
			g.setStroke(new BasicStroke(2f * i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(cast);
		}

		g.setColor(alpha(0, strength * 0.6));
		g.fill(cast);
	}

	/** Draws what lies under an element: its shadow, then its side. */
	static void under(Graphics2D g, String element, Shape shape, WindowStyle style)
	{
		String name = pending;
		pending = null;
		double height = name == null ? 0 : get("pane." + name);
		LEVELS.push(new Level(height, name != null));
		double unit = style.px(1000) / 1000.0;
		double gap = get("separation") * unit;
		double raised = Math.max(0, height) * gap;
		double extrude = get(element + ".extrude") * unit + raised * get("layerSide");
		double shadow = get(element + ".shadow") * unit + raised;
		if (extrude <= 0 && shadow <= 0)
			return;

		double angle = Math.toRadians(get("light"));
		double dx = -Math.sin(angle);
		double dy = Math.cos(angle);
		double blur = get("shadowBlur") * unit;
		Graphics2D copy = around(g, shape, extrude + shadow + blur + 4);
		try
		{
			if (height > 0)
				keepOffHigher(g, copy, shape, height, extrude + shadow + blur + 4);

			if (shadow > 0)
				soft(copy, moved(shape, dx * (shadow + extrude), dy * (shadow + extrude)), get("shadowStrength"), blur);

			if (extrude > 0)
			{
				Area side = new Area();
				int steps = (int) Math.ceil(extrude);
				for (int i = 1; i <= steps; i++)
				{
					double at = extrude * i / steps;
					side.add(new Area(moved(shape, dx * at, dy * at)));
				}

				copy.setColor(new Color((int) (long) get("sideColor"), true));
				copy.fill(side);
			}
		}
		finally
		{
			copy.dispose();
		}
	}

	/** A pane's shadow does not fall on a neighbour as high as itself, though it does on whatever it sits on. */
	private static void keepOffHigher(Graphics2D g, Graphics2D copy, Shape shape, double height, double reach)
	{
		Area allowed = new Area(grown(shape, reach));
		for (Placed placed : PLACED)
		{
			Shape other = local(g, placed.shape);
			if (placed.height >= height && !other.getBounds2D().contains(shape.getBounds2D()))
				allowed.subtract(new Area(other));
		}

		copy.clip(allowed);
	}

	/** Draws what lies over an element's body, before what sits on it: its bevel and the shadows that reach it. */
	static void over(Graphics2D g, String element, Shape shape, WindowStyle style)
	{
		Level level = LEVELS.isEmpty() ? new Level(0, false) : LEVELS.pop();
		double unit = style.px(1000) / 1000.0;
		double gap = get("separation") * unit;
		double blur = get("shadowBlur") * unit;
		double angle = Math.toRadians(get("light"));
		double dx = -Math.sin(angle);
		double dy = Math.cos(angle);
		double width = get(element + ".bevel") * unit;
		Graphics2D copy = (Graphics2D) g.create();
		try
		{
			copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (width > 0)
			{
				Area lit = new Area(shape);
				lit.subtract(new Area(moved(shape, dx * width, dy * width)));
				copy.setColor(alpha(0xFFFFFF, get("highlight")));
				copy.fill(lit);
				Area dark = new Area(shape);
				dark.subtract(new Area(moved(shape, -dx * width, -dy * width)));
				copy.setColor(alpha(0, get("shade")));
				copy.fill(dark);
			}

			copy.clip(shape);
			for (Placed placed : PLACED)
			{
				double drop = (placed.height - level.height) * gap;
				if (drop > 0)
					castOnto(copy, local(g, placed.shape), shape, dx * drop, dy * drop, blur);
			}

			double sunk = Math.max(0, -level.height) * gap;
			if (sunk > 0)
			{
				Area rim = new Area(grown(shape, sunk + blur + 4));
				rim.subtract(new Area(moved(shape, dx * sunk, dy * sunk)));
				soft(copy, rim, get("shadowStrength"), blur);
			}
		}
		finally
		{
			copy.dispose();
		}

		if (level.named)
			PLACED.add(new Placed(g.getTransform().createTransformedShape(shape), level.height));
	}

	private static void castOnto(Graphics2D g, Shape higher, Shape receiver, double x, double y, double blur)
	{
		Shape cast = moved(higher, x, y);
		if (!cast.getBounds2D().intersects(receiver.getBounds2D()))
			return;

		Graphics2D copy = (Graphics2D) g.create();
		try
		{
			Area clear = new Area(grown(receiver, 4));
			clear.subtract(new Area(higher));
			copy.clip(clear);
			soft(copy, cast, get("shadowStrength"), blur);
		}
		finally
		{
			copy.dispose();
		}
	}
}
