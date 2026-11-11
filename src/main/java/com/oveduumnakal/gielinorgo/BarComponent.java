/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

import net.runelite.client.ui.ColorScheme;

/** A thin horizontal progress bar painted in a single colour over a dark track. */
final class BarComponent extends JComponent
{
	private double fraction;
	private Color fill;
	private Color track = ColorScheme.DARK_GRAY_COLOR;

	BarComponent(Color fill, int height)
	{
		this.fill = fill;
		setPreferredSize(new Dimension(10, height));
		setMinimumSize(new Dimension(10, height));
	}

	/**
	 * Changes what the bar shows.
	 *
	 * @param fraction how full the bar is, from 0 to 1
	 * @param fill the colour of the filled part
	 */
	void set(double fraction, Color fill)
	{
		this.fraction = Math.max(0.0, Math.min(1.0, fraction));
		this.fill = fill;
		repaint();
	}

	/**
	 * Changes the colour of the empty part of the bar, for a bar that sits on a background the default would
	 * disappear into.
	 *
	 * @param track the colour of the unfilled part
	 */
	void setTrack(Color track)
	{
		this.track = track;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		int width = getWidth();
		int height = getHeight();
		g.setColor(track);
		g.fillRect(0, 0, width, height);
		g.setColor(fill);
		g.fillRect(0, 0, (int) Math.round(width * fraction), height);
	}
}
