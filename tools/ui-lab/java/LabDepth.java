package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Effects the plugin does not have yet: extruded sides, bevels and cast shadows, lit from one angle. The lab's copy of
 * the painters calls these before and after each element's body. Every amount starts at zero, so nothing changes until
 * asked. Keeping a preset that uses them means moving this drawing into the plugin.
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

		private Setting(String id, String name, String type, double value, double max, double step, String doc)
		{
			this.id = id;
			this.name = name;
			this.type = type;
			this.value = value;
			this.min = 0;
			this.max = max;
			this.step = step;
			this.doc = doc;
		}
	}

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
	}

	private LabDepth()
	{
	}

	private static void add(String id, String name, String type, double value, double max, double step, String doc)
	{
		SETTINGS.add(new Setting("depth:" + id, name, type, value, max, step, doc));
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

	/** @return a copy of the graphics clipped to everything but the element, so glass faces stay clear */
	private static Graphics2D around(Graphics2D g, Shape shape, double reach)
	{
		Graphics2D copy = (Graphics2D) g.create();
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Rectangle2D b = shape.getBounds2D();
		Area outside = new Area(new Rectangle2D.Double(b.getX() - reach, b.getY() - reach, b.getWidth() + 2 * reach,
			b.getHeight() + 2 * reach));
		outside.subtract(new Area(shape));
		copy.clip(outside);
		return copy;
	}

	private static Shape moved(Shape shape, double x, double y)
	{
		return AffineTransform.getTranslateInstance(x, y).createTransformedShape(shape);
	}

	private static Color alpha(int rgb, double amount)
	{
		int a = (int) Math.round(255 * Math.max(0.0, Math.min(1.0, amount)));
		return new Color(a << 24 | rgb, true);
	}

	/** Draws what lies under an element: its shadow, then its side. */
	static void under(Graphics2D g, String element, Shape shape, WindowStyle style)
	{
		double unit = style.px(1000) / 1000.0;
		double extrude = get(element + ".extrude") * unit;
		double shadow = get(element + ".shadow") * unit;
		if (extrude <= 0 && shadow <= 0)
			return;

		double angle = Math.toRadians(get("light"));
		double dx = -Math.sin(angle);
		double dy = Math.cos(angle);
		double blur = get("shadowBlur") * unit;
		Graphics2D copy = around(g, shape, extrude + shadow + blur + 4);
		try
		{
			if (shadow > 0)
			{
				Shape cast = moved(shape, dx * (shadow + extrude), dy * (shadow + extrude));
				int layers = Math.max(1, (int) Math.round(blur));
				copy.setColor(alpha(0, get("shadowStrength") / (layers * 0.6 + 1)));
				for (int i = layers; i >= 1; i--)
				{
					copy.setStroke(new BasicStroke(2f * i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					copy.draw(cast);
				}

				copy.setColor(alpha(0, get("shadowStrength") * 0.6));
				copy.fill(cast);
			}

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

	/** Draws the bevel over an element's body, before what sits on it. */
	static void over(Graphics2D g, String element, Shape shape, WindowStyle style)
	{
		double width = get(element + ".bevel") * style.px(1000) / 1000.0;
		if (width <= 0)
			return;

		double angle = Math.toRadians(get("light"));
		double dx = -Math.sin(angle) * width;
		double dy = Math.cos(angle) * width;
		Graphics2D copy = (Graphics2D) g.create();
		try
		{
			copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Area lit = new Area(shape);
			lit.subtract(new Area(moved(shape, dx, dy)));
			copy.setColor(alpha(0xFFFFFF, get("highlight")));
			copy.fill(lit);
			Area dark = new Area(shape);
			dark.subtract(new Area(moved(shape, -dx, -dy)));
			copy.setColor(alpha(0, get("shade")));
			copy.fill(dark);
		}
		finally
		{
			copy.dispose();
		}
	}
}
