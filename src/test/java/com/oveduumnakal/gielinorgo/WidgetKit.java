/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Shared set-up of the widget tests: a style, a headless canvas and font measures. */
final class WidgetKit
{
	private WidgetKit()
	{
	}

	/**
	 * @param scale the window scale
	 * @return the default style at the scale
	 */
	static WindowStyle style(double scale)
	{
		return WindowStyle.of(WindowTheme.DARK_GLASS, scale);
	}

	/**
	 * @param width the width
	 * @param height the height
	 * @return a transparent canvas
	 */
	static BufferedImage canvas(int width, int height)
	{
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	/**
	 * @param image the canvas
	 * @return graphics set up as the kit expects
	 */
	static Graphics2D graphics(BufferedImage image)
	{
		Graphics2D g = image.createGraphics();
		WindowStyle.prepare(g);
		return g;
	}

	/**
	 * @param style the style
	 * @param face the font
	 * @return the measures of the face at body size
	 */
	static FontMetrics metrics(WindowStyle style, WindowFonts.Face face)
	{
		Graphics2D g = graphics(canvas(1, 1));
		FontMetrics fm = g.getFontMetrics(style.font(face, WindowFonts.BODY_UNITS));
		g.dispose();
		return fm;
	}

	/**
	 * @param image the canvas
	 * @param x the left edge
	 * @param y the top edge
	 * @param w the width
	 * @param h the height
	 * @return how many pixels in the box are not fully transparent
	 */
	static int painted(BufferedImage image, int x, int y, int w, int h)
	{
		int count = 0;
		for (int i = x; i < x + w; i++)
			for (int j = y; j < y + h; j++)
				if ((image.getRGB(i, j) >>> 24) != 0)
					count++;

		return count;
	}
}
