package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Moved and resized panes and buttons. The lab's copy of each page's layout hands its rectangles here once it has
 * worked them out; they are changed in place, so painting and clicking follow. Changes are in layout units, so they
 * grow with the window's scale. The fifth side, f, is the text size of an element.
 */
final class LabBoxes
{
	private static final String[] SIDES = {"x", "y", "w", "h", "f"};
	private static final Map<String, double[]> CHANGES = new HashMap<>();

	/** Where the layouts of the scene being drawn leave their rectangles, by name. */
	static Map<String, Rectangle> target = new LinkedHashMap<>();

	/** Pixels per layout unit in the scene being drawn. */
	static double unit = 1.0;

	private LabBoxes()
	{
	}

	/**
	 * Changes one side of one box.
	 *
	 * @param id the box's name, a dot and x, y, w or h
	 * @param units how far it is from where the plugin puts it, or how much larger its text is
	 */
	static void set(String id, double units)
	{
		int dot = id.lastIndexOf('.');
		String name = id.substring(0, dot);
		int side = java.util.Arrays.asList(SIDES).indexOf(id.substring(dot + 1));
		if (side < 0)
			throw new IllegalArgumentException("no such side: " + id);

		CHANGES.computeIfAbsent(name, key -> new double[SIDES.length])[side] = units;
	}

	/** @return how far a box is from where the plugin puts it, as x, y, w and h in layout units */
	static double[] change(String name)
	{
		double[] change = CHANGES.get(name);
		return change == null ? new double[SIDES.length] : change.clone();
	}

	/**
	 * Takes one rectangle of a layout, changes it, and moves what belongs to it along.
	 *
	 * @param name the box's name, the page first
	 * @param box the rectangle, changed in place
	 * @param children rectangles, or arrays or lists of them, that move when the box does
	 */
	static void at(String name, Rectangle box, Object... children)
	{
		if (box == null || box.width <= 0 && box.height <= 0)
			return;

		double[] change = change(name);
		int dx = (int) Math.round(change[0] * unit);
		int dy = (int) Math.round(change[1] * unit);
		box.x += dx;
		box.y += dy;
		box.width = Math.max(0, box.width + (int) Math.round(change[2] * unit));
		box.height = Math.max(0, box.height + (int) Math.round(change[3] * unit));
		for (Object child : children)
			shift(child, dx, dy);

		target.put(name, box);
	}

	/** @return the name of a champion or friend card on the Battle page */
	static String card(BattleBlock block)
	{
		String text = block.text().replace('.', ' ').trim();
		return "Battle: card " + (text.length() > 28 ? text.substring(0, 28) : text);
	}

	private static void shift(Object child, int dx, int dy)
	{
		if (child instanceof Rectangle)
		{
			((Rectangle) child).x += dx;
			((Rectangle) child).y += dy;
		}
		else if (child instanceof Rectangle[])
		{
			for (Rectangle each : (Rectangle[]) child)
				shift(each, dx, dy);
		}
		else if (child instanceof Iterable)
		{
			for (Object each : (Iterable<?>) child)
				shift(each, dx, dy);
		}
	}
}
