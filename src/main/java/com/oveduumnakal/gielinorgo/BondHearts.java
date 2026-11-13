/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

import net.runelite.client.ui.ColorScheme;

/** Five small hearts drawn in code, filled up to a creature's bond. A full bond draws them gold. */
final class BondHearts extends JComponent
{
	private static final int SIZE = 9;
	private static final int GAP = 2;
	private static final Color FILLED = new Color(0xE5484D);
	private static final Color EMPTY = ColorScheme.MEDIUM_GRAY_COLOR;

	private int hearts;

	BondHearts()
	{
		setOpaque(false);
		Dimension size = new Dimension(BondRules.MAX_HEARTS * (SIZE + GAP), SIZE + 1);
		setPreferredSize(size);
		setMaximumSize(size);
	}

	/**
	 * Sets how many hearts are filled.
	 *
	 * @param filled the hearts, 0 to {@value BondRules#MAX_HEARTS}
	 */
	void set(int filled)
	{
		if (filled == hearts)
			return;

		hearts = filled;
		repaint();
	}

	/**
	 * Builds the outline of one heart.
	 *
	 * @param x left edge
	 * @param y top edge
	 * @param size width and height
	 * @return the heart
	 */
	static Path2D.Double shape(double x, double y, double size)
	{
		Path2D.Double heart = new Path2D.Double();
		heart.moveTo(x + size / 2, y + size);
		heart.curveTo(x - size * 0.2, y + size * 0.45, x + size * 0.15, y - size * 0.15, x + size / 2, y + size * 0.3);
		heart.curveTo(x + size * 0.85, y - size * 0.15, x + size * 1.2, y + size * 0.45, x + size / 2, y + size);
		heart.closePath();
		return heart;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D graphics = (Graphics2D) g.create();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color full = hearts >= BondRules.MAX_HEARTS ? PanelUi.GOLD : FILLED;
			for (int i = 0; i < BondRules.MAX_HEARTS; i++)
			{
				graphics.setColor(i < hearts ? full : EMPTY);
				graphics.fill(shape(i * (SIZE + GAP), 0, SIZE));
			}
		}
		finally
		{
			graphics.dispose();
		}
	}
}
