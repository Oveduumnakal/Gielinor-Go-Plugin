/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Pins the readability of every label of the glass look (#324): the window is painted over a white, a black, a grey, a
 * yellow, a light blue and a cream scene, with its wash made from that scene as the game makes it; each element is
 * painted inside it without its label, and the average colour behind the label's place must reach the contrast the
 * label's colour needs.
 */
public class GlassContrastTest
{
	private static final int[] SCENES = {0xFFFFFF, 0x000000, 0x808080, 0xFFFF00, 0x80C0FF, 0xF4F1E8};
	private static final int W = 520;
	private static final int H = 420;
	private static final Rectangle WINDOW = new Rectangle(60, 40, 400, 340);

	/** Paints one element without its label. */
	private interface Element
	{
		void paint(Graphics2D g, WindowStyle st, Rectangle2D box);
	}

	private static BufferedImage sceneOf(int rgb)
	{
		BufferedImage image = WidgetKit.canvas(W, H);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(rgb));
		g.fillRect(0, 0, W, H);
		g.dispose();
		return image;
	}

	private static Color average(BufferedImage image, Rectangle2D region)
	{
		long r = 0;
		long gr = 0;
		long b = 0;
		int n = 0;
		for (int y = (int) region.getMinY(); y < region.getMaxY(); y++)
		{
			for (int x = (int) region.getMinX(); x < region.getMaxX(); x++)
			{
				int p = image.getRGB(x, y);
				r += p >> 16 & 0xFF;
				gr += p >> 8 & 0xFF;
				b += p & 0xFF;
				n++;
			}
		}

		return new Color((int) (r / n), (int) (gr / n), (int) (b / n));
	}

	private static void check(String what, ThemeColor text, Color explicit, double minimum, Element element)
	{
		WindowTheme theme = WindowTheme.RUNELITE_GLASS;
		for (int scene : SCENES)
		{
			BufferedImage clean = sceneOf(scene);
			WindowStyle plain = WindowStyle.of(theme, 1.0);
			WindowStyle st = plain.withWashes((name, box) -> RingWash.wash(clean, box));
			BufferedImage image = sceneOf(scene);
			Graphics2D g = WidgetKit.graphics(image);
			st.windowOn(g, WINDOW, "window");
			Rectangle2D box = new Rectangle2D.Double(WINDOW.x + 90, WINDOW.y + 120, 130, 34);
			element.paint(g, st, box);
			g.dispose();
			Rectangle2D label = new Rectangle2D.Double(box.getX() + box.getWidth() * 0.25,
				box.getY() + box.getHeight() * 0.25, box.getWidth() * 0.5, box.getHeight() * 0.5);
			Color under = average(image, label);
			Color color = explicit != null ? explicit : st.color(text);
			double ratio = WindowTheme.contrast(color, under);
			assertTrue(String.format("%s over %06X: %.2f, needs %.1f", what, scene, ratio, minimum),
				ratio >= minimum);
		}
	}

	@Test
	public void theTextOnTheWindowReads()
	{
		check("TEXT", ThemeColor.TEXT, null, 4.5, (g, st, box) -> st.windowOn(g, WINDOW, "window"));
		check("SUBTEXT", ThemeColor.SUBTEXT, null, 3.0, (g, st, box) -> st.windowOn(g, WINDOW, "window"));
	}

	@Test
	public void theTabLabelsRead()
	{
		check("inactive tab", ThemeColor.TAB_TEXT, null, 4.5, (g, st, box) -> st.tab(g, box, "", false, false, 0));
		check("hovered tab", ThemeColor.TAB_TEXT, null, 3.5, (g, st, box) -> st.tab(g, box, "", false, false, 1));
		check("active tab", ThemeColor.TAB_ACTIVE_TEXT, WindowTheme.RUNELITE_GLASS.border().accentHigh(), 4.5,
			(g, st, box) -> st.tab(g, box, "", true, false, 0));
		check("disabled tab", ThemeColor.TAB_DISABLED_TEXT, null, 1.3,
			(g, st, box) -> st.tab(g, box, "", false, true, 0));
	}

	@Test
	public void theButtonAndChipLabelsRead()
	{
		check("plain button", ThemeColor.CHIP_TEXT, null, 4.5,
			(g, st, box) -> st.button(g, box, "", false, false, 0, 0));
		check("primary button", ThemeColor.CHIP_ACCENT_TEXT, null, 4.5,
			(g, st, box) -> st.button(g, box, "", true, false, 0, 0));
		check("pressed button", ThemeColor.CHIP_TEXT, null, 4.5,
			(g, st, box) -> st.button(g, box, "", false, false, 0, 1));
		check("plain chip", ThemeColor.CHIP_TEXT, null, 4.5,
			(g, st, box) -> st.chip(g, (int) box.getX(), (int) box.getY(), "", false));
	}

	@Test
	public void theRowAndPanelLabelsRead()
	{
		check("plain row", ThemeColor.ROW_TEXT, null, 4.5, (g, st, box) -> st.row(g, box, false, 0, 0));
		check("lead row", ThemeColor.LEAD_TEXT, null, 4.5, (g, st, box) -> st.row(g, box, true, 0, 0));
		check("panel text", ThemeColor.TEXT, null, 4.5, (g, st, box) -> st.panel(g, box));
	}
}
