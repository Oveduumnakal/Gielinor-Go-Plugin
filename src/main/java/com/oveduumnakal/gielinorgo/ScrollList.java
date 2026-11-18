/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * A scrolling list of equal-height rows. It paints only the rows that are in view, so a list of 1,254 species costs
 * the same as one of ten. It scrolls with the mouse wheel (consumed whenever the pointer is over the list, even at an
 * end, so the camera does not zoom), by dragging the bar, by clicking the groove (one page), by dragging the rows
 * themselves (a trackpad or touch drag) and by {@link #ensureVisible} when a key moves the highlight. The offset is
 * always clamped to the content. Rows and the bar sit at their final place at once, so hit rectangles never lag the
 * picture. The widget takes plain events from the real listeners and holds no RuneLite types.
 */
final class ScrollList
{
	/** How far a drag on the rows must go before it scrolls instead of clicking, in pixels. */
	static final int DRAG_THRESHOLD = 4;

	/** The width of the scroll bar, in layout units. */
	static final double BAR_UNITS = 10;

	/** The shortest the bar's handle gets, in layout units. */
	static final double MIN_THUMB_UNITS = 24;

	/** How many rows one wheel notch moves. */
	static final double WHEEL_ROWS = 1.5;

	private int rowHeight = 1;
	private int[] tops;
	private boolean rowsScroll = true;
	private int count;
	private int barWidth = 8;
	private int minThumb = 16;
	private Rectangle viewport = new Rectangle();
	private double offset;
	private Drag drag = Drag.NONE;
	private int dragStartY;
	private double dragStartOffset;
	private int dragGrab;
	private boolean scrolledByDrag;

	/** What the pointer is dragging. */
	private enum Drag
	{
		/** Nothing. */
		NONE,

		/** The pointer went down on the rows and has not moved far enough to count as a scroll. */
		PENDING,

		/** The rows follow the pointer. */
		ROWS,

		/** The bar's handle follows the pointer. */
		THUMB
	}

	/**
	 * Sets where and how the list lays out. Call it each frame before painting and before hit tests; the offset is
	 * clamped again, so a window that grows never leaves the list scrolled past its end.
	 *
	 * @param viewport the visible area
	 * @param rowHeight the height of every row in pixels, at least 1
	 * @param count how many rows the list has
	 * @param style the style, for the bar's size
	 */
	void layout(Rectangle viewport, int rowHeight, int count, WindowStyle style)
	{
		this.tops = null;
		this.viewport = new Rectangle(viewport);
		this.rowHeight = Math.max(1, rowHeight);
		this.count = Math.max(0, count);
		this.barWidth = Math.max(4, style.px(BAR_UNITS));
		this.minThumb = Math.max(8, style.px(MIN_THUMB_UNITS));
		setOffset(offset);
	}

	/**
	 * Sets where and how the list lays out when its rows have different heights, such as headings among rows. Call
	 * it in place of {@link #layout} each frame; the next {@link #layout} goes back to equal rows.
	 *
	 * @param viewport the visible area
	 * @param heights the height of each row in pixels; one row for each entry
	 * @param style the style, for the bar's size
	 */
	void layoutRows(Rectangle viewport, int[] heights, WindowStyle style)
	{
		int[] sums = new int[heights.length + 1];
		int tallest = 1;
		for (int i = 0; i < heights.length; i++)
		{
			int each = Math.max(1, heights[i]);
			sums[i + 1] = sums[i] + each;
			tallest = Math.max(tallest, each);
		}

		layout(viewport, tallest, heights.length, style);
		this.tops = sums;
		setOffset(offset);
	}

	/**
	 * Chooses whether a drag that starts on a row scrolls the list. Turn it off where a drag on a row means
	 * something else, such as moving a creature; the bar and the wheel still scroll.
	 *
	 * @param on whether dragging the rows scrolls the list
	 */
	void setRowDragScrolls(boolean on)
	{
		rowsScroll = on;
	}

	private int top(int index)
	{
		return tops == null ? index * rowHeight : tops[index];
	}

	private int heightOf(int index)
	{
		return tops == null ? rowHeight : tops[index + 1] - tops[index];
	}

	private int rowAtOffset(int y)
	{
		if (tops == null)
			return y / rowHeight;

		int low = 0;
		int high = count - 1;
		while (low < high)
		{
			int mid = (low + high + 1) >>> 1;
			if (tops[mid] <= y)
				low = mid;
			else
				high = mid - 1;
		}

		return low;
	}

	/** @return the visible area */
	Rectangle viewport()
	{
		return new Rectangle(viewport);
	}

	/** @return how many rows the list has */
	int count()
	{
		return count;
	}

	/** @return the height of one row in pixels; with rows of different heights, the tallest */
	int rowHeight()
	{
		return rowHeight;
	}

	/** @return the height of all the rows together */
	int contentHeight()
	{
		return tops == null ? count * rowHeight : tops[count];
	}

	/** @return whether there is more than fits, so the bar shows and the list scrolls */
	boolean scrollable()
	{
		return contentHeight() > viewport.height;
	}

	/** @return the most the list can be scrolled, in pixels */
	int maxOffset()
	{
		return Math.max(0, contentHeight() - viewport.height);
	}

	/** @return how far the list is scrolled, in pixels from the top; always from 0 to {@link #maxOffset} */
	int offset()
	{
		return (int) Math.round(offset);
	}

	/**
	 * Scrolls to a position, kept inside the content.
	 *
	 * @param pixels the wanted offset from the top
	 */
	void setOffset(double pixels)
	{
		offset = Math.max(0, Math.min(maxOffset(), pixels));
	}

	/**
	 * Scrolls by a distance, kept inside the content.
	 *
	 * @param pixels how far to move; positive goes down
	 */
	void scrollBy(double pixels)
	{
		setOffset(offset + pixels);
	}

	/** @return the width the rows have: the viewport without the bar when the bar shows */
	int contentWidth()
	{
		return scrollable() ? viewport.width - barWidth : viewport.width;
	}

	/** @return the index of the first row that shows, even in part; 0 when the list is empty */
	int firstVisible()
	{
		return count == 0 ? 0 : Math.min(count - 1, rowAtOffset(offset()));
	}

	/** @return the index of the last row that shows, even in part; -1 when the list is empty */
	int lastVisible()
	{
		if (count == 0)
			return -1;

		return Math.min(count - 1, rowAtOffset(offset() + viewport.height - 1));
	}

	/**
	 * Gives where a row is on screen, by its final place.
	 *
	 * @param index the row
	 * @return the row's box (cut to the viewport), or null when the row is not in view
	 */
	Rectangle rowBounds(int index)
	{
		if (index < 0 || index >= count)
			return null;

		Rectangle box = new Rectangle(viewport.x, viewport.y - offset() + top(index), contentWidth(),
			heightOf(index));
		Rectangle seen = box.intersection(new Rectangle(viewport.x, viewport.y, contentWidth(), viewport.height));
		return seen.isEmpty() ? null : seen;
	}

	/**
	 * Finds the row under a point.
	 *
	 * @param x the point's x
	 * @param y the point's y
	 * @return the row's index, or -1 when the point is not on a row
	 */
	int rowAt(int x, int y)
	{
		if (x < viewport.x || x >= viewport.x + contentWidth() || y < viewport.y || y >= viewport.y + viewport.height)
			return -1;

		int at = y - viewport.y + offset();
		if (count == 0 || at < 0 || at >= contentHeight())
			return -1;

		return rowAtOffset(at);
	}

	/**
	 * Scrolls just far enough to show a whole row, for keyboard follow. A row taller than the view shows its top.
	 *
	 * @param index the row to show
	 */
	void ensureVisible(int index)
	{
		if (index < 0 || index >= count)
			return;

		int top = top(index);
		int bottom = top + heightOf(index);
		if (top < offset)
			setOffset(top);
		else if (bottom > offset + viewport.height)
			setOffset(bottom - viewport.height);
	}

	/**
	 * Paints the visible rows and the bar. The painter is called for no other row.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param painter draws one row into the box it is given; the graphics are clipped to the viewport
	 */
	void paint(Graphics2D g, WindowStyle style, RowPainter painter)
	{
		Shape saved = g.getClip();
		g.clipRect(viewport.x, viewport.y, viewport.width, viewport.height);
		int last = lastVisible();
		for (int i = firstVisible(); i <= last; i++)
			painter.paint(g, i, new Rectangle(viewport.x, viewport.y - offset() + top(i), contentWidth(),
				heightOf(i)));

		g.setClip(saved);
		paintBar(g, style);
	}

	/**
	 * Paints the bar: a groove and a handle. Nothing shows when everything fits.
	 *
	 * @param g the graphics
	 * @param style the style
	 */
	void paintBar(Graphics2D g, WindowStyle style)
	{
		if (!scrollable())
			return;

		Rectangle track = trackBounds();
		int corner = barWidth / 2;
		g.setColor(style.color(ThemeColor.SCROLL_TRACK));
		g.fill(new RoundRectangle2D.Double(track.x, track.y, track.width, track.height, corner * 2, corner * 2));
		Rectangle thumb = thumbBounds();
		g.setColor(style.color(ThemeColor.SCROLL_THUMB));
		g.fill(new RoundRectangle2D.Double(thumb.x, thumb.y, thumb.width, thumb.height, corner * 2, corner * 2));
	}

	/** @return the bar's groove, or an empty box when nothing scrolls */
	Rectangle trackBounds()
	{
		if (!scrollable())
			return new Rectangle();

		return new Rectangle(viewport.x + viewport.width - barWidth, viewport.y, barWidth, viewport.height);
	}

	/** @return the bar's handle, or an empty box when nothing scrolls */
	Rectangle thumbBounds()
	{
		if (!scrollable())
			return new Rectangle();

		double share = (double) viewport.height / contentHeight();
		int length = (int) Math.max(minThumb, Math.round(viewport.height * share));
		int room = viewport.height - length;
		int at = maxOffset() == 0 ? 0 : (int) Math.round(room * (offset / maxOffset()));
		return new Rectangle(viewport.x + viewport.width - barWidth, viewport.y + at, barWidth, length);
	}

	/**
	 * Handles the mouse wheel.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @param notches how far the wheel turned; positive scrolls down. A fraction is fine for a trackpad
	 * @return whether the wheel was consumed: true whenever the pointer is over the list, even when it cannot move
	 */
	boolean onWheel(int x, int y, double notches)
	{
		if (!viewport.contains(x, y))
			return false;

		scrollBy(notches * WHEEL_ROWS * rowHeight);
		return true;
	}

	/**
	 * Handles a press. On the handle it starts a handle drag; on the groove it pages up or down; on a row it
	 * prepares a drag of the rows, which becomes a scroll only after {@link #DRAG_THRESHOLD} pixels.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether the press was on the list; a press on a row is also consumed, and the caller decides about
	 *         the click when the button goes up, so ask {@link #onRelease}
	 */
	boolean onPress(int x, int y)
	{
		scrolledByDrag = false;
		drag = Drag.NONE;
		if (!viewport.contains(x, y))
			return false;

		if (scrollable() && trackBounds().contains(x, y))
		{
			Rectangle thumb = thumbBounds();
			if (thumb.contains(x, y))
			{
				drag = Drag.THUMB;
				dragGrab = y - thumb.y;
			}
			else
			{
				scrollBy(y < thumb.y ? -viewport.height : viewport.height);
			}

			return true;
		}

		if (!rowsScroll)
			return true;

		drag = Drag.PENDING;
		dragStartY = y;
		dragStartOffset = offset;
		return true;
	}

	/**
	 * Handles the pointer moving with the button down.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether the list scrolled or is being dragged, so the caller can repaint
	 */
	boolean onDrag(int x, int y)
	{
		if (drag == Drag.THUMB)
		{
			Rectangle thumb = thumbBounds();
			int room = viewport.height - thumb.height;
			if (room > 0)
				setOffset(maxOffset() * (double) (y - dragGrab - viewport.y) / room);

			return true;
		}

		if (drag == Drag.PENDING && Math.abs(y - dragStartY) >= DRAG_THRESHOLD && scrollable())
			drag = Drag.ROWS;

		if (drag == Drag.ROWS)
		{
			scrolledByDrag = true;
			setOffset(dragStartOffset - (y - dragStartY));
			return true;
		}

		return false;
	}

	/**
	 * Handles the button going up.
	 *
	 * @return whether the press became a scroll (a drag of the rows or of the handle), in which case the caller must
	 *         not treat the release as a click on a row
	 */
	boolean onRelease()
	{
		boolean scrolled = scrolledByDrag || drag == Drag.THUMB;
		drag = Drag.NONE;
		scrolledByDrag = false;
		return scrolled;
	}

	/** @return whether a drag is in progress, the handle's or the rows' */
	boolean dragging()
	{
		return drag == Drag.THUMB || drag == Drag.ROWS;
	}

	/** Draws one row of a {@link ScrollList}. */
	interface RowPainter
	{
		/**
		 * Draws a row.
		 *
		 * @param g the graphics, clipped to the list's viewport
		 * @param index the row's index
		 * @param box where the row is; it may reach past the viewport at the top or bottom edge
		 */
		void paint(Graphics2D g, int index, Rectangle box);
	}
}
