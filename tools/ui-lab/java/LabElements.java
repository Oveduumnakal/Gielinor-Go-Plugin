package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The bars, text, icons, chips and portraits inside panes. The lab's copy of the shared drawing methods passes each one
 * through here. An element is known by the line of code that draws it, so moving the health bar in one row moves it in
 * every row.
 */
final class LabElements
{
	/** One element as it was drawn this frame. */
	static final class Drawn
	{
		final String key;
		final String name;
		final Rectangle at;

		private Drawn(String key, String name, Rectangle at)
		{
			this.key = key;
			this.name = name;
			this.at = at;
		}
	}

	/** The drawing methods an element passes through, which are never the element's owner. */
	private static final List<String> SHARED = java.util.Arrays.asList("LabElements", "WindowStyle", "PageText",
		"PortraitFrame", "Icons", "CrispImages", "JarIcons", "ItemIcons", "StyleIcons", "PinButton", "GlassStyle");
	private static final Map<String, List<String>> SOURCES = new HashMap<>();
	private static final StackWalker WALKER = StackWalker.getInstance();
	private static int depth;
	/** How much larger the current element's text is, in pixels, and whether a font has been enlarged for it. */
	private static double larger;
	private static boolean enlarged;
	private static String drawing;

	/** Where the copied sources are, to read the line that draws an element. */
	static Path sources;

	/** Where the scene being drawn collects its elements. */
	static List<Drawn> target = new ArrayList<>();

	private LabElements()
	{
	}

	/** Forgets the last frame's elements. */
	static void frame()
	{
		target.clear();
		depth = 0;
		larger = 0;
		enlarged = false;
	}

	/** Ends the element begun last. */
	static void done()
	{
		if (--depth <= 0)
		{
			depth = 0;
			larger = 0;
			enlarged = false;
		}
	}

	/** @return the font at the size the current element asks for; a font is only ever enlarged once */
	static Font font(Font font)
	{
		if (larger == 0 || enlarged)
			return font;

		enlarged = true;
		return font.deriveFont((float) Math.max(4.0, font.getSize2D() + larger));
	}

	/** Keeps a button's or chip's label centred when its text changes size. */
	static int centred(Graphics2D g, String text, int x, Font was, Font now)
	{
		if (now == was || !"button".equals(drawing) && !"chip".equals(drawing))
			return x;

		FontMetrics before = g.getFontMetrics(was);
		return x - (g.getFontMetrics(now).stringWidth(text) - before.stringWidth(text)) / 2;
	}

	private static String component(String type)
	{
		String name = type.substring(type.lastIndexOf('.') + 1).replaceAll("\\$.*", "");
		if ("BubblePainter".equals(name))
			return "Info card";

		return name.replaceAll("(Painter|Page|Overlay)$", "").replaceAll("(?<=[a-z])(?=[A-Z])", " ");
	}

	private static String line(String type, int number)
	{
		String file = type.substring(type.lastIndexOf('.') + 1).replaceAll("\\$.*", "");
		List<String> lines = SOURCES.computeIfAbsent(file, key ->
		{
			try
			{
				return Files.readAllLines(sources.resolve(key + ".java"), StandardCharsets.UTF_8);
			}
			catch (IOException e)
			{
				return new ArrayList<>();
			}
		});
		int at = Math.min(number, lines.size()) - 1;
		while (at > 0 && !lines.get(at - 1).trim().matches("(.*[;{}]|.*\\)|else|)"))
			at--;

		return at < 0 ? "line " + number : lines.get(at).trim().replaceAll("LabLit_\\w+\\.v\\d+_\\d+", "#");
	}

	/**
	 * Works out which element is being drawn and how it has been changed, and notes where it ends up.
	 *
	 * @return the change as x, y, w and h in pixels, or null when this is part of a larger element or of a pane
	 */
	private static double[] begin(String kind, String sample, Graphics2D g, Rectangle2D box)
	{
		if (depth++ > 0 || "".equals(sample))
			return null;

		for (Rectangle pane : LabBoxes.target.values())
		{
			if (pane.equals(box.getBounds()))
				return null;
		}

		StackWalker.StackFrame owner = WALKER.walk(frames -> frames
			.filter(f -> f.getClassName().startsWith("com.oveduumnakal")
				&& !SHARED.contains(f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1)
					.replaceAll("\\$.*", "")))
			.findFirst()
			.orElse(null));
		if (owner == null)
			return null;

		String method = owner.getMethodName().replaceAll("^lambda\\$(\\w+)\\$\\d+$", "$1");
		String type = owner.getClassName().substring(owner.getClassName().lastIndexOf('.') + 1)
			.replaceAll("\\$.*", "");
		String key = "el|" + type + "." + method + "|" + kind + "|" + line(owner.getClassName(), owner.getLineNumber());
		double[] units = LabBoxes.change(key);
		double[] change = new double[units.length];
		for (int i = 0; i < units.length; i++)
			change[i] = units[i] * LabBoxes.unit;

		larger = change[4];
		enlarged = false;
		drawing = kind;

		Rectangle2D moved = new Rectangle2D.Double(box.getX() + change[0], box.getY() + change[1],
			Math.max(1, box.getWidth() + change[2]), Math.max(1, box.getHeight() + change[3]));
		String shown = sample == null ? "" : " \"" + (sample.length() > 18 ? sample.substring(0, 18) + "..." : sample)
			+ "\"";
		target.add(new Drawn(key, component(owner.getClassName()) + " " + method + ": " + kind + shown,
			g.getTransform().createTransformedShape(moved).getBounds()));
		return change;
	}

	static Rectangle2D rect(String kind, Graphics2D g, Rectangle2D box)
	{
		double[] c = begin(kind, null, g, box);
		return c == null ? box : new Rectangle2D.Double(box.getX() + c[0], box.getY() + c[1],
			Math.max(1, box.getWidth() + c[2]), Math.max(1, box.getHeight() + c[3]));
	}

	static Rectangle box(String kind, String sample, Graphics2D g, Rectangle box)
	{
		double[] c = begin(kind, sample, g, box);
		return c == null ? box : new Rectangle(box.x + (int) Math.round(c[0]), box.y + (int) Math.round(c[1]),
			Math.max(0, box.width + (int) Math.round(c[2])), Math.max(0, box.height + (int) Math.round(c[3])));
	}

	static Shape shape(String kind, Graphics2D g, Shape shape)
	{
		Rectangle2D b = shape.getBounds2D();
		double[] c = begin(kind, null, g, b);
		if (c == null || c[0] == 0 && c[1] == 0 && c[2] == 0 && c[3] == 0)
			return shape;

		AffineTransform change = AffineTransform.getTranslateInstance(b.getX() + c[0], b.getY() + c[1]);
		change.scale(Math.max(1, b.getWidth() + c[2]) / Math.max(1, b.getWidth()),
			Math.max(1, b.getHeight() + c[3]) / Math.max(1, b.getHeight()));
		change.translate(-b.getX(), -b.getY());
		return change.createTransformedShape(shape);
	}

	/** @return the x, y and side of a square picture; whichever of width and height was dragged more sets the side */
	static double[] square(String kind, String sample, Graphics2D g, double x, double y, double size)
	{
		double[] c = begin(kind, sample, g, new Rectangle2D.Double(x, y, size, size));
		if (c == null)
			return new double[] {x, y, size};

		return new double[] {x + c[0], y + c[1], Math.max(1, size + (Math.abs(c[2]) > Math.abs(c[3]) ? c[2] : c[3]))};
	}

	/** @return a copy of the graphics that stretches what is drawn at this spot the way the element has been changed */
	static Graphics2D stretched(String kind, String sample, Graphics2D g, double x, double y, double w, double h)
	{
		double[] c = begin(kind, sample, g, new Rectangle2D.Double(x, y, w, h));
		Graphics2D copy = (Graphics2D) g.create();
		if (c != null)
		{
			copy.translate(x + c[0], y + c[1]);
			copy.scale(Math.max(1, w + c[2]) / Math.max(1, w), Math.max(1, h + c[3]) / Math.max(1, h));
			copy.translate(-x, -y);
		}

		return copy;
	}
}
