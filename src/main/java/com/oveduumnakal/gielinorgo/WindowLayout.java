/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

/**
 * Where everything of the painted window's shell sits, worked out from the window's size alone: the header with its six
 * tabs and the Close button, the page area, the hint line, the footer buttons (Change log and Feedback on the right)
 * and the resize grip in the bottom-right corner. Every length is a number of layout units in the 800 x 520 design
 * space times the one window scale, so nothing is positioned in raw pixels except the least sizes of the resize
 * zones. The page area takes whatever room the other parts leave, so a window a pixel off the design shape has no
 * gap. Coordinates are window pixels. Holds no RuneLite types.
 */
final class WindowLayout
{
	/** The gap around the window's parts, in layout units. */
	static final double MARGIN = 12;

	/** The height of the header bar, in layout units. */
	static final double HEADER_UNITS = 64;

	/** The top of the tabs inside the header, in layout units. */
	static final double TAB_TOP_UNITS = 10;

	/** The width of a tab, in layout units. */
	static final double TAB_UNITS = 114;

	/** The gap between tabs, in layout units. */
	static final double TAB_GAP_UNITS = 2;

	/** The width of the Close button, in layout units. */
	static final double CLOSE_UNITS = 78;

	/** The height of the hint line, in layout units. */
	static final double HINT_UNITS = 28;

	/** The height of a footer button, in layout units. */
	static final double BUTTON_UNITS = 40;

	/** The gap between footer buttons, in layout units. */
	static final double BUTTON_GAP_UNITS = 8;

	/** The width of the Change log button, in layout units. */
	static final double CHANGELOG_UNITS = 134;

	/** The width of the Feedback button, in layout units. */
	static final double FEEDBACK_UNITS = 112;

	/** The least width of the resize strip along an edge, in pixels, however small the window. */
	static final int EDGE_MIN_PX = 4;

	/** The least side of the corner grip, in pixels. */
	static final int GRIP_MIN_PX = 12;

	/** The window scale everything is drawn at. */
	final double scale;

	/** The whole window. */
	final Rectangle window;

	/** The header bar. */
	final Rectangle header;

	/** The tabs, in {@link WindowTab} order. */
	final Rectangle[] tabs;

	/** The Close button. */
	final Rectangle close;

	/** The page area. */
	final Rectangle body;

	/** The hint line under the page. */
	final Rectangle hints;

	/** The grip in the bottom-right corner that resizes the window. */
	final Rectangle grip;

	/** The width of the strip along the right and bottom edges that resizes the window, in pixels. */
	final int edge;

	/** The Change log button, at the right end of the footer. */
	final Rectangle changelog;

	/** The Feedback button, at the right edge of the footer. */
	final Rectangle feedback;

	private WindowLayout(int width, int height)
	{
		scale = WindowFit.scale(width, height);
		window = new Rectangle(0, 0, width, height);
		int m = px(MARGIN);
		int headerH = px(HEADER_UNITS);
		header = new Rectangle(0, 0, width, headerH);
		int tabW = px(TAB_UNITS);
		int tabH = px(WindowStyle.Metrics.TAB_UNITS);
		tabs = new Rectangle[WindowTab.values().length];
		for (int i = 0; i < tabs.length; i++)
			tabs[i] = new Rectangle(m + i * (tabW + px(TAB_GAP_UNITS)), px(TAB_TOP_UNITS), tabW, tabH);

		close = new Rectangle(width - m - px(CLOSE_UNITS), px(TAB_TOP_UNITS), px(CLOSE_UNITS), tabH);
		int buttonH = px(BUTTON_UNITS);
		int hintH = px(HINT_UNITS);
		int buttonsTop = height - m - buttonH;
		int hintsTop = buttonsTop - px(BUTTON_GAP_UNITS / 2) - hintH;
		hints = new Rectangle(m, hintsTop, width - 2 * m, hintH);
		body = new Rectangle(m, headerH + m / 2, width - 2 * m, Math.max(0, hintsTop - headerH - m));
		int gap = px(BUTTON_GAP_UNITS);
		edge = Math.max(EDGE_MIN_PX, px(MARGIN / 2));
		int gripSide = Math.max(GRIP_MIN_PX, px(MARGIN));
		grip = new Rectangle(width - gripSide, height - gripSide, gripSide, gripSide);
		feedback = new Rectangle(width - m - px(FEEDBACK_UNITS), buttonsTop, px(FEEDBACK_UNITS), buttonH);
		changelog = new Rectangle(feedback.x - gap - px(CHANGELOG_UNITS), buttonsTop, px(CHANGELOG_UNITS), buttonH);
	}

	/** Which part of the window a press resizes. */
	enum ResizeZone
	{
		/** Not a resize press. */
		NONE,

		/** The right edge, which sets the width. */
		RIGHT,

		/** The bottom edge, which sets the height. */
		BOTTOM,

		/** The corner grip, which sets the width and the height together. */
		CORNER
	}

	/**
	 * Tells which resize zone a point is in. The corner grip wins where it meets an edge strip.
	 *
	 * @param x the point's x in window pixels
	 * @param y the point's y in window pixels
	 * @return the zone, or {@link ResizeZone#NONE} when the point is not on the window's right or bottom edge
	 */
	ResizeZone resizeZoneAt(int x, int y)
	{
		if (!window.contains(x, y))
			return ResizeZone.NONE;

		if (grip.contains(x, y))
			return ResizeZone.CORNER;

		if (x >= window.width - edge)
			return ResizeZone.RIGHT;

		return y >= window.height - edge ? ResizeZone.BOTTOM : ResizeZone.NONE;
	}

	/**
	 * Works the layout out for a window size.
	 *
	 * @param width the window width in pixels
	 * @param height the window height in pixels
	 * @return the layout
	 */
	static WindowLayout of(int width, int height)
	{
		return new WindowLayout(width, height);
	}

	/**
	 * @param units a length in layout units
	 * @return the length in pixels at this layout's scale, rounded
	 */
	int px(double units)
	{
		return (int) Math.round(units * scale);
	}
}
