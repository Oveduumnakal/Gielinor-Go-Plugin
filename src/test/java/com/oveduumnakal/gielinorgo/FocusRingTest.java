/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The highlight follows the shape of what it rings, in both looks (#322). It used to be a gold rectangle round a
 * slanted button, because a call site passed the button's plain box. Every ring now takes the element's outline, so
 * no gold pixel may lie further outside the outline than half the stroke, plus one pixel of antialiasing.
 */
public class FocusRingTest
{
	private static final Rectangle2D BOX = new Rectangle2D.Double(60, 40, 120, 36);

	private static int outside(BufferedImage image, Shape outline, double stroke)
	{
		Shape reach = new BasicStroke((float) (stroke + 2)).createStrokedShape(outline);
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				boolean painted = (image.getRGB(x, y) >>> 24) > 8;
				if (painted && !outline.contains(x + 0.5, y + 0.5) && !reach.contains(x + 0.5, y + 0.5))
					count++;
			}
		}

		return count;
	}

	@Test
	public void theRingOfEveryKindStaysOnItsOutlineInBothLooks()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			for (WindowStyle.Kind kind : WindowStyle.Kind.values())
			{
				WindowStyle style = WindowStyle.of(theme, 1.0);
				Shape outline = style.outline(kind, BOX);
				BufferedImage image = WidgetKit.canvas(260, 140);
				Graphics2D g = WidgetKit.graphics(image);
				style.focusRing(g, outline);
				g.dispose();
				double stroke = Math.max(2, style.metrics().border)
					+ (theme.glass() ? 3 * GlassTokens.GLOW_WIDTH : 0);
				assertEquals(theme + " " + kind, 0, outside(image, outline, stroke));
				assertTrue(theme + " " + kind + " paints something", WidgetKit.painted(image, 0, 0, 260, 140) > 0);
			}
		}
	}

	@Test
	public void aFocusedButtonInTheModalIsRingedOnItsOwnSlant()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			WindowStyle style = WindowStyle.of(theme, 1.0);
			Rectangle area = new Rectangle(0, 0, 800, 520);
			Modal modal = new Modal("Release Rex?", "Rex will leave your storage for good.", List.of("No", "Yes"), 0,
				index ->
				{
				}, () ->
				{
				});
			modal.open(0);
			modal.layout(area, style, WidgetKit.metrics(style, WindowFonts.Face.REGULAR),
				WidgetKit.metrics(style, WindowFonts.Face.BOLD));
			BufferedImage plain = WidgetKit.canvas(800, 520);
			Graphics2D g = WidgetKit.graphics(plain);
			modal.paint(g, style, area, MotionMode.OFF, 1_000);
			g.dispose();
			Rectangle button = modal.buttonBounds(modal.selected());
			Shape outline = style.outline(WindowStyle.Kind.BUTTON, new Rectangle2D.Double(button.x, button.y,
				button.width, button.height));
			Shape reach = new BasicStroke(Math.max(2, style.metrics().border) + 2f).createStrokedShape(outline);
			int ring = style.color(ThemeColor.FOCUS_RING).getRGB() & 0xFFFFFF;
			int stray = 0;
			for (int y = button.y - 8; y < button.y + button.height + 8; y++)
			{
				for (int x = button.x - 8; x < button.x + button.width + 8; x++)
				{
					boolean gold = (plain.getRGB(x, y) & 0xFFFFFF) == ring && (plain.getRGB(x, y) >>> 24) == 255;
					if (gold && !outline.contains(x + 0.5, y + 0.5) && !reach.contains(x + 0.5, y + 0.5))
						stray++;
				}
			}

			assertEquals(theme.toString(), 0, stray);
		}
	}
}
