/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks that tooltips and popup menus are nearly solid in the Glass look while a dialog keeps the window's tint
 * (#331).
 */
public class PopupTintTest
{
	private static final WindowStyle STYLE = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);

	private static int centreAlpha(String surface)
	{
		GlassPanel.clear();
		BufferedImage canvas = WidgetKit.canvas(260, 200);
		Graphics2D g = WidgetKit.graphics(canvas);
		STYLE.popup(g, new Rectangle2D.Double(40, 40, 180, 110), surface, ThemeColor.TOOLTIP, ThemeColor.PANEL_BORDER,
			false, Collections.emptyList());
		g.dispose();
		return canvas.getRGB(130, 95) >>> 24;
	}

	@Test
	public void theTooltipAndTheMenuAreAboutNinetyFourPercentOpaque()
	{
		int least = (int) Math.round(255 * GlassTokens.POPUP_TINT);
		assertTrue("tooltip " + centreAlpha("tooltip"), centreAlpha("tooltip") >= least);
		assertTrue("menu " + centreAlpha("menu"), centreAlpha("menu") >= least);
	}

	@Test
	public void aDialogKeepsTheWindowTint()
	{
		int dialog = centreAlpha("modal");

		assertTrue("modal " + dialog, dialog < 255 * GlassTokens.POPUP_TINT);
		assertEquals(255 * GlassTokens.TINT_SOLID, dialog, 25);
	}

	@Test
	public void thePopupTintIsAboveTheWindowTintAndBelowSolid()
	{
		assertTrue(GlassTokens.POPUP_TINT > GlassTokens.TINT_SOLID);
		assertTrue(GlassTokens.POPUP_TINT < 1.0);
	}

	@Test
	public void theSolidLookTooltipIsStillAbout96PercentOpaque()
	{
		WindowStyle solid = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		BufferedImage canvas = WidgetKit.canvas(260, 200);
		Graphics2D g = WidgetKit.graphics(canvas);
		solid.popup(g, new Rectangle2D.Double(40, 40, 180, 110), "tooltip", ThemeColor.TOOLTIP, ThemeColor.PANEL_BORDER,
			false, Collections.emptyList());
		g.dispose();

		assertEquals(245, canvas.getRGB(130, 95) >>> 24, 5);
	}
}
