/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The state of one drag of a creature in the Storage Box view. A press on a cell only becomes a drag once the
 * pointer has moved a few pixels, so a plain click is still a click. When the button comes up the drag ends as a
 * click, a drop on another cell, or a cancel. Pure: the page feeds it pointer events and the cell under the pointer.
 * Holds no RuneLite types.
 */
final class StorageDrag
{
	/** What a button coming up meant. */
	enum Kind
	{
		/** No press was being tracked. */
		NONE,

		/** The pointer never moved far: an ordinary click. */
		CLICK,

		/** The creature was dropped on another cell. */
		DROP,

		/** The creature was dragged and put back, or dropped outside the grid. */
		CANCEL
	}

	/** What a button coming up meant, and for a drop, where from and to. */
	static final class Result
	{
		private final Kind kind;
		private final int from;
		private final int to;

		private Result(Kind kind, int from, int to)
		{
			this.kind = kind;
			this.from = from;
			this.to = to;
		}

		/** @return what happened */
		Kind kind()
		{
			return kind;
		}

		/** @return the cell the creature was dragged from, or -1 */
		int from()
		{
			return from;
		}

		/** @return the cell it was dropped on, or -1 */
		int to()
		{
			return to;
		}
	}

	private static final int NO_SLOT = -1;

	private final int threshold;
	private int from = NO_SLOT;
	private int startX;
	private int startY;
	private int pointerX;
	private int pointerY;
	private boolean dragging;

	/**
	 * Creates the state machine.
	 *
	 * @param threshold how far the pointer must move before a press is a drag, in pixels
	 */
	StorageDrag(int threshold)
	{
		this.threshold = Math.max(1, threshold);
	}

	/**
	 * @param style the style
	 * @return how far the pointer must move to start a drag: 6 layout units, at least 4 pixels
	 */
	static int threshold(WindowStyle style)
	{
		return Math.max(4, style.px(6));
	}

	/**
	 * A button went down.
	 *
	 * @param slot the cell under the pointer, or -1 when it is not on a creature
	 * @param x the pointer's x
	 * @param y the pointer's y
	 */
	void press(int slot, int x, int y)
	{
		from = slot;
		startX = x;
		startY = y;
		pointerX = x;
		pointerY = y;
		dragging = false;
	}

	/**
	 * The pointer moved with the button down.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether a creature is being dragged now
	 */
	boolean moved(int x, int y)
	{
		pointerX = x;
		pointerY = y;
		if (from != NO_SLOT && !dragging && Math.max(Math.abs(x - startX), Math.abs(y - startY)) >= threshold)
			dragging = true;

		return dragging;
	}

	/**
	 * The button came up. The state resets.
	 *
	 * @param dropSlot the cell under the pointer, or -1 when it is outside the grid
	 * @return what it meant
	 */
	Result release(int dropSlot)
	{
		int source = from;
		boolean moved = dragging;
		cancel();
		if (source == NO_SLOT)
			return new Result(Kind.NONE, NO_SLOT, NO_SLOT);

		if (!moved)
			return new Result(Kind.CLICK, source, NO_SLOT);

		if (dropSlot < 0 || dropSlot == source)
			return new Result(Kind.CANCEL, source, NO_SLOT);

		return new Result(Kind.DROP, source, dropSlot);
	}

	/** Forgets the press, as Esc or a lost press does. */
	void cancel()
	{
		from = NO_SLOT;
		dragging = false;
	}

	/** @return whether a creature is being dragged */
	boolean dragging()
	{
		return dragging;
	}

	/** @return whether a press on a creature is being tracked, dragging or not */
	boolean pressed()
	{
		return from != NO_SLOT;
	}

	/** @return the cell being dragged from, or -1 */
	int from()
	{
		return from;
	}

	/** @return the pointer's x */
	int pointerX()
	{
		return pointerX;
	}

	/** @return the pointer's y */
	int pointerY()
	{
		return pointerY;
	}
}
