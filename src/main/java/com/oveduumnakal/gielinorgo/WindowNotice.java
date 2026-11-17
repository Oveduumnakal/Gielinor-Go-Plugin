/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The pop-up text that tells the player why the window closed or would not open: it pops in, holds about two seconds
 * and fades, as {@link NoticeClock} times it. It is drawn over the whole canvas at the place the plugin gives, which
 * is where the window was or under the toggle that was clicked, and is kept inside the canvas. Only one shows at a
 * time. It paints only while there is a notice, so it costs nothing otherwise.
 */
final class WindowNotice extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(WindowNotice.class);
	private static final double SCALE = 1.0;

	private final Supplier<MotionMode> motion;
	private final Supplier<WindowTheme> theme;
	private final LongSupplier clock;
	private final IntSupplier canvasWidth;
	private final IntSupplier canvasHeight;
	private volatile NoticeSlot slot;

	/**
	 * Creates the overlay, showing nothing.
	 *
	 * @param motion the "Window animations" setting
	 * @param theme the window style
	 * @param clock the time in milliseconds
	 * @param canvasWidth the game canvas width
	 * @param canvasHeight the game canvas height
	 */
	WindowNotice(Supplier<MotionMode> motion, Supplier<WindowTheme> theme, LongSupplier clock,
		IntSupplier canvasWidth, IntSupplier canvasHeight)
	{
		this.motion = motion;
		this.theme = theme;
		this.clock = clock;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/**
	 * Shows a notice, replacing any that is on screen.
	 *
	 * @param message what to say
	 * @param centerX the centre x on the canvas
	 * @param centerY the centre y on the canvas
	 */
	void show(NoticeMessage message, int centerX, int centerY)
	{
		slot = new NoticeSlot(message, clock.getAsLong(), centerX, centerY);
	}

	/** @return the notice on screen, or null for none */
	NoticeSlot current()
	{
		return slot;
	}

	/** Takes the notice away at once, for when the plugin shuts down. */
	void clear()
	{
		slot = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			NoticeSlot shown = slot;
			if (shown == null)
				return null;

			NoticeClock.Frame frame = shown.frame(motion.get(), clock.getAsLong());
			if (!frame.visible)
			{
				slot = null;
				return null;
			}

			WindowStyle style = WindowStyle.of(theme.get(), SCALE);
			RenderingHints savedHints = graphics.getRenderingHints();
			Stroke savedStroke = graphics.getStroke();
			Color savedColor = graphics.getColor();
			Font savedFont = graphics.getFont();
			try
			{
				WindowStyle.prepare(graphics);
				FontMetrics fm = graphics.getFontMetrics(style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS));
				int pad = style.metrics().pad;
				int halfW = (fm.stringWidth(shown.message().text()) + 2 * pad) / 2;
				int halfH = (fm.getHeight() + pad) / 2;
				int cx = NoticeSlot.clampCenter(shown.centerX(), halfW, canvasWidth.getAsInt());
				int cy = NoticeSlot.clampCenter(shown.centerY(), halfH, canvasHeight.getAsInt());
				style.notice(graphics, shown.message().text(), cx, cy, frame.scale, frame.alpha);
			}
			finally
			{
				graphics.setRenderingHints(savedHints);
				graphics.setStroke(savedStroke);
				graphics.setColor(savedColor);
				graphics.setFont(savedFont);
			}

			return null;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the notice", e);
			return null;
		}
	}
}
