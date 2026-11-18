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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The pinned tracker as a RuneLite overlay on the game screen: a small box with the tasks and eggs the player pinned
 * and their live progress. It sits top left under the jar button and RuneLite owns its place, so it moves with
 * RuneLite's drag key and the place is remembered by RuneLite, not by this plugin. It takes no mouse input of its own:
 * RuneLite overlays only catch the mouse while the drag key is held, so every other click goes through to the game. It
 * hides while a Go battle is on, and while there is nothing to show. What it shows is decided elsewhere: the plugin
 * publishes the lines from {@link TrackerModel} and this class only reads them, so painting never touches the game
 * thread's state.
 */
final class TrackerOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(TrackerOverlay.class);
	private static final String CLEAR_OPTION = "Clear pins";
	private static final String MENU_TARGET = "Gielinor Go";

	private final TrackerPainter painter = new TrackerPainter(PageText.Sink.NONE);
	private final Supplier<WindowTheme> theme;
	private final DoubleSupplier fraction;
	private final IntSupplier canvasWidth;
	private final IntSupplier canvasHeight;
	private final BooleanSupplier enabled;
	private final BooleanSupplier battleOn;
	private final BooleanSupplier loaded;
	private volatile List<TrackerView.Line> frame = Collections.emptyList();

	/**
	 * Creates the tracker, showing nothing.
	 *
	 * @param theme the window's style
	 * @param fraction the window's share of the canvas width, which sets the scale
	 * @param canvasWidth the game canvas width
	 * @param canvasHeight the game canvas height
	 * @param enabled the "Pinned tracker" setting
	 * @param battleOn whether a Go battle is on
	 * @param loaded whether the save is loaded and live
	 * @param clear what the "Clear pins" entry of the right-click menu does
	 */
	TrackerOverlay(Supplier<WindowTheme> theme, DoubleSupplier fraction, IntSupplier canvasWidth,
		IntSupplier canvasHeight, BooleanSupplier enabled, BooleanSupplier battleOn, BooleanSupplier loaded,
		Runnable clear)
	{
		this.theme = theme;
		this.fraction = fraction;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		this.enabled = enabled;
		this.battleOn = battleOn;
		this.loaded = loaded;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(true);
		setResizable(false);
		setPriority(Overlay.PRIORITY_LOW);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY, CLEAR_OPTION, MENU_TARGET, entry -> clear.run());
	}

	/**
	 * Hands the overlay the lines to draw. Call it from the game thread; the overlay only reads it.
	 *
	 * @param lines the lines, from {@link TrackerModel#frame}
	 */
	void publish(List<TrackerView.Line> lines)
	{
		frame = Collections.unmodifiableList(lines);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			List<TrackerView.Line> lines = frame;
			boolean show = TrackerRules.visible(enabled.getAsBoolean(), battleOn.getAsBoolean(),
				loaded.getAsBoolean(), lines.size());
			if (!show)
				return null;

			Dimension window = WindowFit.sizeFor(fraction.getAsDouble(), canvasWidth.getAsInt(),
				canvasHeight.getAsInt());
			WindowStyle style = WindowStyle.of(theme.get(), TrackerRules.scale(WindowFit.scale(window.width,
				window.height)));
			paint(graphics, style, lines);
			return TrackerPainter.size(style, lines.size());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the tracker", e);
			return null;
		}
	}

	private void paint(Graphics2D g, WindowStyle style, List<TrackerView.Line> lines)
	{
		RenderingHints savedHints = g.getRenderingHints();
		Stroke savedStroke = g.getStroke();
		Color savedColor = g.getColor();
		Font savedFont = g.getFont();
		AffineTransform savedTransform = g.getTransform();
		Composite savedComposite = g.getComposite();
		WindowStyle.prepare(g);
		g.setComposite(AlphaComposite.SrcOver);
		try
		{
			painter.paint(g, style, lines);
		}
		finally
		{
			g.setTransform(savedTransform);
			g.setComposite(savedComposite);
			g.setRenderingHints(savedHints);
			g.setStroke(savedStroke);
			g.setColor(savedColor);
			g.setFont(savedFont);
		}
	}
}
