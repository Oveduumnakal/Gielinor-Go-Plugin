/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the UI thread may know about the painted window, published whole after each paint in one volatile field: where
 * it is on the canvas, the hit list, which part has the keys and whether it has focus. Immutable, so the mouse and
 * key listeners read a consistent picture without waiting for the game thread, which owns the real window state.
 * Holds no RuneLite types.
 */
final class WindowView
{
	/** The view of a window that is closed: it covers nothing and takes no key. */
	static final WindowView CLOSED = new WindowView(false, new Rectangle(), Collections.emptyList(),
		WindowKeyRules.KeyMode.NONE, false);

	private final boolean open;
	private final Rectangle bounds;
	private final List<Hit> hits;
	private final WindowKeyRules.KeyMode keys;
	private final boolean focused;

	/**
	 * Makes a view.
	 *
	 * @param open whether the window is open and takes the pointer; a window that is fading out is not
	 * @param bounds where the window is on the canvas
	 * @param hits the hits in paint order, in window pixels
	 * @param keys which part of the window has the keys
	 * @param focused whether the window has keyboard focus
	 */
	WindowView(boolean open, Rectangle bounds, List<Hit> hits, WindowKeyRules.KeyMode keys, boolean focused)
	{
		this.open = open;
		this.bounds = new Rectangle(bounds);
		this.hits = Collections.unmodifiableList(new ArrayList<>(hits));
		this.keys = keys;
		this.focused = focused;
	}

	/** @return whether the window is open */
	boolean open()
	{
		return open;
	}

	/** @return a copy of the window's rectangle on the canvas */
	Rectangle bounds()
	{
		return new Rectangle(bounds);
	}

	/** @return the hits in paint order, in window pixels */
	List<Hit> hits()
	{
		return hits;
	}

	/** @return which part of the window has the keys */
	WindowKeyRules.KeyMode keys()
	{
		return keys;
	}

	/** @return whether the window has keyboard focus */
	boolean focused()
	{
		return focused;
	}

	/**
	 * @param x the pointer's x on the canvas
	 * @param y the pointer's y on the canvas
	 * @return whether the window is open and the point is inside it
	 */
	boolean covers(int x, int y)
	{
		return open && bounds.contains(x, y);
	}

	/**
	 * @param x the canvas x
	 * @return the same x in window pixels
	 */
	int localX(int x)
	{
		return x - bounds.x;
	}

	/**
	 * @param y the canvas y
	 * @return the same y in window pixels
	 */
	int localY(int y)
	{
		return y - bounds.y;
	}

	/**
	 * Finds the hit under a point. A hit painted later sits on top, so the list is searched from the end.
	 *
	 * @param x the pointer's x on the canvas
	 * @param y the pointer's y on the canvas
	 * @return the hit, or null when the window is closed or nothing is there
	 */
	Hit hitAt(int x, int y)
	{
		if (!covers(x, y))
			return null;

		int lx = localX(x);
		int ly = localY(y);
		for (int i = hits.size() - 1; i >= 0; i--)
			if (hits.get(i).contains(lx, ly))
				return hits.get(i);

		return null;
	}
}
