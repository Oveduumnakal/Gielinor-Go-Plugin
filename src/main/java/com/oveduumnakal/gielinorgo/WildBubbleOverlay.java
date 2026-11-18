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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Paints the wild creature bubbles over the game screen, in the painted window's style and at its scale, and
 * publishes what the frame it painted made clickable for the mouse listener on the UI thread. It paints on the game
 * thread, in canvas pixels, the same ones the mouse listener reports. While the plugin is not live, or the bubbles
 * are off or paused, it paints nothing and publishes nothing clickable. If painting ever throws, the failure
 * callback is told so the plugin can fall back to the game's own menu entries.
 */
final class WildBubbleOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(WildBubbleOverlay.class);

	private final Client client;
	private final WildBodies bodies;
	private final WildBubbles model;
	private final BubblePainter painter;
	private final Supplier<WindowTheme> theme;
	private final Supplier<MotionMode> motion;
	private final DoubleSupplier fraction;
	private final BooleanSupplier live;
	private final LongSupplier clock;
	private final Consumer<Throwable> onFailure;
	private volatile BubbleHits hits = BubbleHits.EMPTY;

	/**
	 * Creates the overlay.
	 *
	 * @param client the game client
	 * @param bodies the wild creatures' bodies, which place the bubbles
	 * @param model the bubbles' model
	 * @param painter paints a frame
	 * @param theme the window's style
	 * @param motion the window animation setting
	 * @param fraction the window's share of the canvas width, which sets the scale
	 * @param live whether the plugin is loaded and the bubbles are on
	 * @param clock the time in milliseconds
	 * @param onFailure called when painting threw, after the click targets were cleared
	 */
	WildBubbleOverlay(Client client, WildBodies bodies, WildBubbles model, BubblePainter painter,
		Supplier<WindowTheme> theme, Supplier<MotionMode> motion, DoubleSupplier fraction, BooleanSupplier live,
		LongSupplier clock, Consumer<Throwable> onFailure)
	{
		this.client = client;
		this.bodies = bodies;
		this.model = model;
		this.painter = painter;
		this.theme = theme;
		this.motion = motion;
		this.fraction = fraction;
		this.live = live;
		this.clock = clock;
		this.onFailure = onFailure;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/** @return the click targets of the frame painted last; safe to read from any thread */
	BubbleHits hits()
	{
		return hits;
	}

	/**
	 * Replaces the published click targets.
	 *
	 * @param published the new targets; {@link BubbleHits#EMPTY} for none
	 */
	void publish(BubbleHits published)
	{
		hits = published;
	}

	/** @return the style the bubbles paint in now, at the window's scale */
	WindowStyle style()
	{
		Dimension window = WindowFit.sizeFor(fraction.getAsDouble(), client.getCanvasWidth(),
			client.getCanvasHeight());
		double scale = Math.max(WindowFit.MIN_SCALE, WindowFit.scale(window.width, window.height));
		return WindowStyle.of(theme.get(), scale);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!live.getAsBoolean())
		{
			hits = BubbleHits.EMPTY;
			return null;
		}

		RenderingHints savedHints = graphics.getRenderingHints();
		Stroke savedStroke = graphics.getStroke();
		Color savedColor = graphics.getColor();
		Font savedFont = graphics.getFont();
		AffineTransform savedTransform = graphics.getTransform();
		Composite savedComposite = graphics.getComposite();
		try
		{
			WindowStyle style = style();
			WindowStyle.prepare(graphics);
			graphics.setComposite(AlphaComposite.SrcOver);
			WildCardContent content = model.cardContent();
			Dimension cardSize = content == null ? null : painter.cardSize(graphics, content, style);
			Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
				client.getViewportWidth(), client.getViewportHeight());
			long now = clock.getAsLong();
			BubbleFrame frame = model.frame(bodies::anchorsOf, viewport, cardSize, now, motion.get());
			painter.paint(graphics, frame, style, now);
			hits = frame.hits();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the wild creature bubbles", e);
			hits = BubbleHits.EMPTY;
			onFailure.accept(e);
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
