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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The painted battle controls as a RuneLite overlay on the game screen, shown only while a battle the player steers
 * waits for a choice. It replaces the old text prompt: the same words, but with buttons for Attack, Switch, Catch,
 * Item and Run and the lists they open, so a player with no battle hotkeys can steer every battle without the side
 * panel. RuneLite owns its place, and it moves with RuneLite's drag key. It is drawn at the scale of the painted
 * window, in the window's style. What it knows and does is in {@link BattleControlsModel}; this class is the RuneLite
 * plumbing: the size, the first place, and the published {@link WindowView} the mouse listener reads. The game thread
 * paints it and runs its input.
 */
final class BattleControls extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(BattleControls.class);
	private static final int FIRST_X = 12;
	private static final int FIRST_Y = 12;

	private final BattleControlsModel model;
	private final BattleControlsPainter painter;
	private final Supplier<WindowTheme> theme;
	private final DoubleSupplier fraction;
	private final IntSupplier canvasWidth;
	private final IntSupplier canvasHeight;
	private volatile WindowView view = WindowView.CLOSED;
	private boolean placed;

	/**
	 * Creates the controls, showing nothing.
	 *
	 * @param model what the controls know and do
	 * @param theme the window's style
	 * @param fraction the window's share of the canvas width, which sets the scale
	 * @param canvasWidth the game canvas width
	 * @param canvasHeight the game canvas height
	 */
	@SuppressWarnings("deprecation")
	BattleControls(BattleControlsModel model, Supplier<WindowTheme> theme, DoubleSupplier fraction,
		IntSupplier canvasWidth, IntSupplier canvasHeight)
	{
		this.model = model;
		this.painter = new BattleControlsPainter(PageText.Sink.NONE);
		this.theme = theme;
		this.fraction = fraction;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setResizable(false);
	}

	/** @return the model, which takes the input on the game thread */
	BattleControlsModel model()
	{
		return model;
	}

	/** @return what the UI thread may know about the controls; safe from any thread */
	WindowView view()
	{
		return view;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			if (!model.visible())
			{
				view = WindowView.CLOSED;
				return null;
			}

			Dimension window = WindowFit.sizeFor(fraction.getAsDouble(), canvasWidth.getAsInt(),
				canvasHeight.getAsInt());
			double scale = Math.max(WindowFit.MIN_SCALE, WindowFit.scale(window.width, window.height));
			WindowStyle style = WindowStyle.of(theme.get(), scale);
			BattleControlsModel.Layout layout = model.layout(style);
			paint(graphics, style, layout);
			if (!placed && getPreferredLocation() == null)
				setPreferredLocation(new Point(FIRST_X, FIRST_Y));

			placed = true;
			Rectangle bounds = getBounds();
			view = new WindowView(true, bounds == null ? new Rectangle() : bounds, model.hits(),
				WindowKeyRules.KeyMode.NONE, false);
			return layout.size;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the battle controls", e);
			view = WindowView.CLOSED;
			return null;
		}
	}

	private void paint(Graphics2D g, WindowStyle style, BattleControlsModel.Layout layout)
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
			painter.paint(g, style, model, layout);
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
