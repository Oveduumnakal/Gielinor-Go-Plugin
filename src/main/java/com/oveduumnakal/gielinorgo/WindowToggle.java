/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

/**
 * The jar icon on the game screen that opens and closes the painted window. It is an overlay RuneLite can move (hold
 * the drag key and drag it), top left by default. RuneLite gives an overlay no left click, so {@link WindowInput} reads
 * {@link #bounds()} and reports the click. While the window cannot open (combat, a battle) the icon is grey and shakes
 * when clicked; a pulsing dot shows when something is waiting. It is drawn in the window's style and shows only while
 * the "Painted window (preview)" setting is on.
 */
final class WindowToggle extends Overlay
{
	/** The side of the icon's square in pixels. */
	static final int SIZE = 44;

	private static final Logger log = LoggerFactory.getLogger(WindowToggle.class);
	private static final int ICON_PX = 36;
	private static final int DOT_PX = 11;
	private static final float LOCKED_ALPHA = 0.55f;
	private static final double SHAKE_PX = 4;

	private final BooleanSupplier shown;
	private final Supplier<WindowTheme> theme;
	private final Supplier<MotionMode> motion;
	private final LongSupplier clock;
	private final BufferedImage icon;
	private final BufferedImage greyIcon;
	private volatile boolean locked;
	private volatile boolean waiting;
	private volatile long shakeMs = Long.MIN_VALUE / 2;
	private volatile Rectangle bounds;

	/**
	 * Creates the toggle.
	 *
	 * @param shown whether the icon is on screen, which follows the preview setting
	 * @param theme the window style
	 * @param motion the "Window animations" setting
	 * @param clock the time in milliseconds
	 * @param icon the jar picture, or null to draw a plain square
	 */
	WindowToggle(BooleanSupplier shown, Supplier<WindowTheme> theme, Supplier<MotionMode> motion, LongSupplier clock,
		BufferedImage icon)
	{
		this.shown = shown;
		this.theme = theme;
		this.motion = motion;
		this.clock = clock;
		this.icon = icon;
		this.greyIcon = icon == null ? null : ImageUtil.grayscaleImage(icon);
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(true);
	}

	/**
	 * Greys the icon while the window cannot open.
	 *
	 * @param value true while the window is locked
	 */
	void locked(boolean value)
	{
		locked = value;
	}

	/** @return whether the window cannot open now */
	boolean isLocked()
	{
		return locked;
	}

	/**
	 * Shows or hides the pulsing dot.
	 *
	 * @param value true while something waits for the player
	 */
	void waiting(boolean value)
	{
		waiting = value;
	}

	/** @return whether the dot is on */
	boolean isWaiting()
	{
		return waiting;
	}

	/** Starts the short shake a click on the greyed icon makes. */
	void shake()
	{
		shakeMs = clock.getAsLong();
	}

	/** @return where the icon is on the canvas, or null while it is not shown, for hit-testing a click */
	Rectangle bounds()
	{
		Rectangle b = bounds;
		return b == null ? null : new Rectangle(b);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			if (!shown.getAsBoolean())
			{
				bounds = null;
				return null;
			}

			Rectangle placed = getBounds();
			bounds = placed == null || placed.isEmpty() ? null : new Rectangle(placed.x, placed.y, SIZE, SIZE);
			paintIcon(graphics, clock.getAsLong());
			return new Dimension(SIZE, SIZE);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the window toggle", e);
			return null;
		}
	}

	private void paintIcon(Graphics2D g, long nowMs)
	{
		RenderingHints savedHints = g.getRenderingHints();
		Stroke savedStroke = g.getStroke();
		Color savedColor = g.getColor();
		Composite savedComposite = g.getComposite();
		AffineTransform savedTransform = g.getTransform();
		try
		{
			WindowTheme look = theme.get();
			MotionMode mode = motion.get();
			WindowStyle.prepare(g);
			g.translate(ShellMotion.shake(mode, nowMs - shakeMs, SHAKE_PX), 0);
			paintFace(g, look, locked);
			g.setTransform(savedTransform);
			if (waiting && !locked)
				paintDot(g, look, mode, nowMs);
		}
		finally
		{
			g.setTransform(savedTransform);
			g.setComposite(savedComposite);
			g.setRenderingHints(savedHints);
			g.setStroke(savedStroke);
			g.setColor(savedColor);
		}
	}

	private void paintFace(Graphics2D g, WindowTheme look, boolean off)
	{
		if (off)
			g.setComposite(AlphaComposite.SrcOver.derive(LOCKED_ALPHA));

		RoundRectangle2D box = new RoundRectangle2D.Double(0.5, 0.5, SIZE - 1, SIZE - 1, 10, 10);
		g.setColor(look.color(ThemeColor.HEADER));
		g.fill(box);
		g.setColor(look.color(off ? ThemeColor.PANEL_BORDER : ThemeColor.FRAME));
		g.setStroke(new BasicStroke(2f));
		g.draw(box);
		BufferedImage picture = off ? greyIcon : icon;
		if (picture != null)
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(picture, (SIZE - ICON_PX) / 2, (SIZE - ICON_PX) / 2, ICON_PX, ICON_PX, null);
		}
	}

	private void paintDot(Graphics2D g, WindowTheme look, MotionMode mode, long nowMs)
	{
		double pulse = mode.loops() ? Loop.pulse(nowMs, WindowTimes.DOT_PULSE, 0) : 1.0;
		double strength = 0.55 + 0.45 * pulse;
		g.setColor(WindowTheme.fade(look.color(ThemeColor.ACCENT), strength));
		g.fill(new Ellipse2D.Double(SIZE - DOT_PX - 1, 1, DOT_PX, DOT_PX));
		g.setColor(look.color(ThemeColor.FOCUS_RING));
		g.setStroke(new BasicStroke(1.5f));
		g.draw(new Ellipse2D.Double(SIZE - DOT_PX - 1, 1, DOT_PX, DOT_PX));
	}
}
