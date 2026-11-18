/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Paints the {@link WildCatchMenu} over the game screen, in the painted window's style and at its scale. It draws
 * in canvas pixels, the same ones the mouse listener reports, and draws nothing while the menu is closed. The game
 * thread paints it. If painting ever throws, the failure callback is told so the plugin can fall back to the game's
 * own menu. Holds no RuneLite types beyond the overlay.
 */
final class WildMenuOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(WildMenuOverlay.class);

	private final WildCatchMenu menu;
	private final Supplier<WindowTheme> theme;
	private final Supplier<MotionMode> motion;
	private final DoubleSupplier fraction;
	private final IntSupplier canvasWidth;
	private final IntSupplier canvasHeight;
	private final LongSupplier clock;
	private final Runnable onFailure;

	/**
	 * Creates the overlay.
	 *
	 * @param menu the menu to paint
	 * @param theme the window's style
	 * @param motion the window animation setting
	 * @param fraction the window's share of the canvas width, which sets the scale
	 * @param canvasWidth the game canvas width
	 * @param canvasHeight the game canvas height
	 * @param clock the time in milliseconds
	 * @param onFailure called when painting threw, after the menu was closed
	 */
	WildMenuOverlay(WildCatchMenu menu, Supplier<WindowTheme> theme, Supplier<MotionMode> motion,
		DoubleSupplier fraction, IntSupplier canvasWidth, IntSupplier canvasHeight, LongSupplier clock,
		Runnable onFailure)
	{
		this.menu = menu;
		this.theme = theme;
		this.motion = motion;
		this.fraction = fraction;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		this.clock = clock;
		this.onFailure = onFailure;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/** @return the style the menu paints in now, at the window's scale */
	WindowStyle style()
	{
		Dimension window = WindowFit.sizeFor(fraction.getAsDouble(), canvasWidth.getAsInt(), canvasHeight.getAsInt());
		double scale = Math.max(WindowFit.MIN_SCALE, WindowFit.scale(window.width, window.height));
		return WindowStyle.of(theme.get(), scale);
	}

	/** @return the canvas as a rectangle at the origin */
	Rectangle canvas()
	{
		return new Rectangle(0, 0, canvasWidth.getAsInt(), canvasHeight.getAsInt());
	}

	/**
	 * Measures the menu's font without a screen to draw on.
	 *
	 * @param style the style
	 * @return the measures of {@link ContextMenu#font}
	 */
	static FontMetrics metrics(WindowStyle style)
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			Font font = ContextMenu.font(style);
			return g.getFontMetrics(font);
		}
		finally
		{
			g.dispose();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!menu.isOpen())
			return null;

		RenderingHints savedHints = graphics.getRenderingHints();
		Stroke savedStroke = graphics.getStroke();
		Color savedColor = graphics.getColor();
		Font savedFont = graphics.getFont();
		AffineTransform savedTransform = graphics.getTransform();
		Composite savedComposite = graphics.getComposite();
		try
		{
			WindowStyle.prepare(graphics);
			graphics.setComposite(AlphaComposite.SrcOver);
			menu.paint(graphics, style(), canvas(), motion.get(), clock.getAsLong());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the wild creature menu", e);
			menu.close();
			onFailure.run();
		}
		finally
		{
			graphics.setTransform(savedTransform);
			graphics.setComposite(savedComposite);
			graphics.setRenderingHints(savedHints);
			graphics.setStroke(savedStroke);
			graphics.setColor(savedColor);
			graphics.setFont(savedFont);
		}

		return null;
	}
}
