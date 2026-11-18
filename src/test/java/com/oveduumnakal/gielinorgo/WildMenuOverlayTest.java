/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Paints the wild creature menu overlay offline, and checks it hands a failure back instead of throwing. */
public class WildMenuOverlayTest
{
	private static final int W = 900;
	private static final int H = 600;

	private final WildCatchMenu menu = new WildCatchMenu();
	private final AtomicInteger failures = new AtomicInteger();

	private static List<JarSuggest.JarChoice> jars()
	{
		return Arrays.asList(new JarSuggest.JarChoice(JarType.BRONZE, 5, 0.38, false),
			new JarSuggest.JarChoice(JarType.DRAGON, 3, 0.95, false));
	}

	private WildMenuOverlay overlay(DoubleSupplier fraction)
	{
		return new WildMenuOverlay(menu, () -> WindowTheme.DARK_GLASS, () -> MotionMode.OFF, fraction, () -> W,
			() -> H, () -> 1_000L, failures::incrementAndGet);
	}

	private void open(WildMenuOverlay overlay)
	{
		WindowStyle style = overlay.style();
		menu.open(3, "Chicken (Lv 3 - New)", WildMenu.layout(JarType.BRONZE, jars(), true, false, true), jars(),
			() ->
			{
			}, jar ->
			{
			}, 300, 200, overlay.canvas(), style, WildMenuOverlay.metrics(style), 0);
	}

	@Test
	public void aClosedMenuPaintsNothing()
	{
		WildMenuOverlay overlay = overlay(() -> 0.5);
		BufferedImage image = WidgetKit.canvas(W, H);
		Graphics2D g = WidgetKit.graphics(image);

		assertNull(overlay.render(g));
		g.dispose();

		assertEquals(0, WidgetKit.painted(image, 0, 0, W, H));
		assertEquals(0, failures.get());
	}

	@Test
	public void anOpenMenuPaintsAtItsPlaceInCanvasPixels()
	{
		WildMenuOverlay overlay = overlay(() -> 0.5);
		open(overlay);
		BufferedImage image = WidgetKit.canvas(W, H);
		Graphics2D g = WidgetKit.graphics(image);

		assertNull(overlay.render(g));
		g.dispose();

		Rectangle b = menu.rootMenu().bounds();
		assertEquals(300, b.x);
		assertEquals(200, b.y);
		assertTrue(WidgetKit.painted(image, b.x, b.y, b.width, b.height) > 0);
		assertEquals(0, WidgetKit.painted(image, 0, 0, 100, 100));
		assertEquals(0, failures.get());
	}

	@Test
	public void theMenuOpensInsideTheCanvasWhateverTheScale()
	{
		for (double fraction : new double[] {0.2, 0.5, 0.9})
		{
			WildMenuOverlay overlay = overlay(() -> fraction);
			open(overlay);

			Rectangle bounds = menu.rootMenu().bounds();

			assertTrue(overlay.canvas().contains(bounds));
		}
	}

	@Test
	public void aFailureClosesTheMenuAndTellsThePlugin()
	{
		WildMenuOverlay good = overlay(() -> 0.5);
		open(good);
		WildMenuOverlay broken = overlay(() ->
		{
			throw new IllegalStateException("boom");
		});
		BufferedImage image = WidgetKit.canvas(W, H);
		Graphics2D g = WidgetKit.graphics(image);

		assertNull(broken.render(g));
		g.dispose();

		assertEquals(1, failures.get());
		assertFalse(menu.isOpen());
	}
}
