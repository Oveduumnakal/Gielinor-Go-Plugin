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
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The pinned party as a RuneLite overlay on the game screen: a small box with the team's portraits, names, levels and
 * health. Like the pinned tracker it sits top left and RuneLite owns its place, so it moves with RuneLite's drag key
 * and the place is remembered by RuneLite. It takes no mouse input of its own, so every click goes through to the
 * game. It hides while a Go battle is on and while there is no team. What it shows is decided elsewhere: the plugin
 * publishes the rows from {@link PartyBoxModel} and this class only reads them.
 */
final class PartyBoxOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(PartyBoxOverlay.class);

	private final PartyBoxPainter painter;
	private final Supplier<WindowTheme> theme;
	private final DoubleSupplier fraction;
	private final IntSupplier canvasWidth;
	private final IntSupplier canvasHeight;
	private final BooleanSupplier enabled;
	private final BooleanSupplier battleOn;
	private final BooleanSupplier loaded;
	private final LongSupplier clock;
	private volatile List<RowModel> frame = Collections.emptyList();

	/**
	 * Creates the box, showing nothing.
	 *
	 * @param pictures where creature pictures are
	 * @param theme the window's style
	 * @param fraction the window's share of the canvas width, which sets the scale
	 * @param canvasWidth the game canvas width
	 * @param canvasHeight the game canvas height
	 * @param enabled the "Pin Party to UI" setting
	 * @param battleOn whether a Go battle is on
	 * @param loaded whether the save is loaded and live
	 * @param clock the time in milliseconds
	 */
	PartyBoxOverlay(PortraitCache pictures, Supplier<WindowTheme> theme, DoubleSupplier fraction,
		IntSupplier canvasWidth, IntSupplier canvasHeight, BooleanSupplier enabled, BooleanSupplier battleOn,
		BooleanSupplier loaded, LongSupplier clock)
	{
		this.painter = new PartyBoxPainter(pictures, PageText.Sink.NONE);
		this.theme = theme;
		this.fraction = fraction;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		this.enabled = enabled;
		this.battleOn = battleOn;
		this.loaded = loaded;
		this.clock = clock;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(true);
		setResizable(false);
		setPriority(Overlay.PRIORITY_LOW);
	}

	/**
	 * Hands the overlay the rows to draw. Call it from the game thread; the overlay only reads it.
	 *
	 * @param rows the rows, from {@link PartyBoxModel#rows}
	 */
	void publish(List<RowModel> rows)
	{
		frame = Collections.unmodifiableList(rows);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			List<RowModel> rows = frame;
			boolean show = PartyBoxModel.visible(enabled.getAsBoolean(), battleOn.getAsBoolean(),
				loaded.getAsBoolean(), rows.size());
			if (!show)
				return null;

			Dimension window = WindowFit.sizeFor(fraction.getAsDouble(), canvasWidth.getAsInt(),
				canvasHeight.getAsInt());
			WindowStyle style = WindowStyle.of(theme.get(), TrackerRules.scale(WindowFit.scale(window.width,
				window.height)));
			paint(graphics, style, rows);
			return PartyBoxPainter.size(style, rows.size());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the party box", e);
			return null;
		}
	}

	private void paint(Graphics2D g, WindowStyle style, List<RowModel> rows)
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
			painter.paint(g, style, rows, clock.getAsLong());
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
