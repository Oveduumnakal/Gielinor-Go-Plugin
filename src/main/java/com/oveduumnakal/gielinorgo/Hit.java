/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

/**
 * One thing in the painted window the pointer or a key can reach: a tab, a button, a row. Every page declares its
 * hits in paint order, and the window publishes the whole list in one immutable value, so the UI thread can decide
 * whether a click belongs to the window without waiting for the game thread. A hit sits where it will rest (the
 * final layout), never where an animation currently draws it. Coordinates are window pixels, with (0, 0) at the
 * window's top-left corner. Holds no RuneLite types.
 */
final class Hit
{
	/** What a hit is, which decides what Enter does on it. */
	enum Kind
	{
		/** A tab header. Left and Right move between tabs; Enter chooses the highlighted one. */
		TAB,

		/** A button. Enter presses it. */
		BUTTON,

		/** A row of a list, such as a party member. Left click goes to its page; Enter and Space open its menu. */
		ROW,

		/** An area that takes the pointer but is not reached by the keys, such as a panel. */
		AREA
	}

	private final String id;
	private final Kind kind;
	private final Rectangle bounds;
	private final int order;
	private final boolean enabled;
	private final String tip;

	/**
	 * Makes a hit.
	 *
	 * @param id a stable name such as {@code tab:BAG}, which the highlight is remembered by
	 * @param kind what it is
	 * @param bounds where it rests, in window pixels
	 * @param order its place in the focus order, or -1 when the keys do not reach it
	 * @param enabled whether it can be chosen; a disabled hit still takes the pointer, so the click is not lost to
	 *        the game, but the keys skip it
	 * @param tip the words for a tooltip, or an empty text for none
	 */
	Hit(String id, Kind kind, Rectangle bounds, int order, boolean enabled, String tip)
	{
		this.id = id;
		this.kind = kind;
		this.bounds = new Rectangle(bounds);
		this.order = order;
		this.enabled = enabled;
		this.tip = tip == null ? "" : tip;
	}

	/** @return the stable name */
	String id()
	{
		return id;
	}

	/** @return what the hit is */
	Kind kind()
	{
		return kind;
	}

	/** @return a copy of the hit's rectangle, in window pixels */
	Rectangle bounds()
	{
		return new Rectangle(bounds);
	}

	/** @return the place in the focus order, or -1 for none */
	int order()
	{
		return order;
	}

	/** @return whether the hit can be chosen */
	boolean enabled()
	{
		return enabled;
	}

	/** @return the tooltip text, empty for none */
	String tip()
	{
		return tip;
	}

	/** @return whether the keys can land on this hit: it has a place in the focus order and is enabled */
	boolean focusable()
	{
		return order >= 0 && enabled;
	}

	/**
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @return whether the point is inside the hit's rectangle
	 */
	boolean contains(int x, int y)
	{
		return bounds.contains(x, y);
	}
}
