/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * What a page needs to paint one frame, gathered by the shell so each page gets the same things: the style, the motion
 * mode, the time, the snapshot, the page area, which hit has the highlight and whether the window has keyboard focus.
 * Immutable. Holds no RuneLite types.
 */
final class PageContext
{
	/** The style to paint in. */
	final WindowStyle style;

	/** How much the page moves. */
	final MotionMode mode;

	/** The time in milliseconds. */
	final long now;

	/** What the game looks like now. */
	final WindowSnapshot snapshot;

	/** The page area in window pixels. */
	final Rectangle body;

	/** The id of the highlighted hit, or null. */
	final String highlight;

	/** Whether the window has keyboard focus, so the highlight shows a ring. */
	final boolean focused;

	/** The id of the hit being pressed, or null. */
	final String pressed;

	/**
	 * Gathers a frame's facts.
	 *
	 * @param style the style
	 * @param mode the motion mode
	 * @param now the time in milliseconds
	 * @param snapshot the snapshot
	 * @param body the page area
	 * @param highlight the highlighted hit's id, or null
	 * @param focused whether the window has keyboard focus
	 * @param pressed the pressed hit's id, or null
	 */
	PageContext(WindowStyle style, MotionMode mode, long now, WindowSnapshot snapshot, Rectangle body,
		String highlight, boolean focused, String pressed)
	{
		this.style = style;
		this.mode = mode;
		this.now = now;
		this.snapshot = snapshot;
		this.body = new Rectangle(body);
		this.highlight = highlight;
		this.focused = focused;
		this.pressed = pressed;
	}

	/**
	 * @param id a hit's id
	 * @return 1 when it has the highlight, else 0
	 */
	double hoverOf(String id)
	{
		return id.equals(highlight) ? 1.0 : 0.0;
	}

	/**
	 * @param id a hit's id
	 * @return 1 while it is pressed, else 0
	 */
	double pressOf(String id)
	{
		return id.equals(pressed) ? 1.0 : 0.0;
	}

	/**
	 * Multiplies the graphics' opacity, keeping what the window's own fade has set.
	 *
	 * @param g the graphics
	 * @param factor the share of the current opacity to keep, from 0 to 1
	 * @return the composite to put back afterwards
	 */
	static Composite fade(Graphics2D g, double factor)
	{
		Composite saved = g.getComposite();
		float own = saved instanceof AlphaComposite ? ((AlphaComposite) saved).getAlpha() : 1f;
		float next = (float) Math.max(0.0, Math.min(1.0, own * factor));
		g.setComposite(AlphaComposite.SrcOver.derive(next));
		return saved;
	}
}
