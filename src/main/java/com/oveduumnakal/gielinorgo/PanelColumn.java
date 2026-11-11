/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * A vertical stack that always matches the width of the scroll pane it sits in, so nothing ever scrolls sideways.
 * Children added to {@link #stack()} keep their preferred height and fill the width.
 */
final class PanelColumn extends JPanel implements Scrollable
{
	private final JPanel stack = new JPanel();

	PanelColumn()
	{
		super(new BorderLayout());
		stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
		stack.setOpaque(false);
		setOpaque(false);
		add(stack, BorderLayout.NORTH);
	}

	/** @return the panel to add children to */
	JPanel stack()
	{
		return stack;
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 16;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		if (orientation == SwingConstants.VERTICAL)
			return visibleRect.height;

		return visibleRect.width;
	}

	@Override
	public boolean getScrollableTracksViewportWidth()
	{
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}
}
