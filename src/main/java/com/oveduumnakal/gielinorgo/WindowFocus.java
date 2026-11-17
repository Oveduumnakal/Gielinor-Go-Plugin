/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Keyboard focus and the one shared highlight of the painted window. The window as a whole has focus or not: it gets
 * it when opened by the toggle or the hotkey and when clicked, and loses it on a click in the world, when chat typing
 * starts and when it closes. Within the window the highlight sits on one {@link Hit}. The pointer and the keys move
 * the same highlight: hovering a hit highlights it, an arrow key continues from there and keeps the highlight until
 * the pointer moves again. Up and Down move along the page's hits in focus order (wrapping, skipping disabled ones),
 * and Left and Right choose tabs (wrapping, skipping a greyed Storage). Pages need no key code of their own beyond
 * declaring their hits and what Enter does on each. Holds no RuneLite types.
 */
final class WindowFocus
{
	/** What Enter or Space does on the highlighted hit. */
	enum Activation
	{
		/** Nothing is highlighted. */
		NONE,

		/** Press it, as a left click does: choose a tab, press a button, go to a row's page. */
		PRESS,

		/** Open the row's menu, with its first item (Summary) highlighted. */
		MENU
	}

	/** What Esc or Backspace does. */
	enum Step
	{
		/** Close the open menu. */
		MENU,

		/** Go back one page, such as Summary to Party. */
		PAGE,

		/** Close the window. */
		CLOSE,

		/** Nothing: Backspace never closes the window. */
		NONE
	}

	private List<Hit> hits = Collections.emptyList();
	private String highlight;
	private boolean focused;
	private boolean keyboardLed;

	/**
	 * Replaces the hits after a layout. The highlight stays on the same id while it is still there.
	 *
	 * @param list every hit of the window in paint order
	 */
	void setHits(List<Hit> list)
	{
		hits = Collections.unmodifiableList(new ArrayList<>(list));
		if (highlight != null && find(highlight) == null)
			highlight = null;
	}

	/** @return whether the window has keyboard focus */
	boolean focused()
	{
		return focused;
	}

	/**
	 * Gives or takes the window's focus. Losing it also drops a highlight the keys put there.
	 *
	 * @param on whether the window is focused
	 */
	void focus(boolean on)
	{
		focused = on;
		if (!on && keyboardLed)
		{
			highlight = null;
			keyboardLed = false;
		}
	}

	/** @return the id of the highlighted hit, or null for none */
	String highlight()
	{
		return highlight;
	}

	/** @return the highlighted hit, or null for none */
	Hit highlighted()
	{
		return highlight == null ? null : find(highlight);
	}

	/** @return whether the keys, not the pointer, put the highlight where it is */
	boolean keyboardLed()
	{
		return keyboardLed;
	}

	/**
	 * Handles the pointer moving: the hit under it takes the highlight, and the pointer is in charge again.
	 *
	 * @param hit the hit under the pointer, or null when it is over nothing
	 */
	void hover(Hit hit)
	{
		keyboardLed = false;
		highlight = hit != null && hit.focusable() ? hit.id() : null;
	}

	/**
	 * Puts the highlight on a hit by the keys' doing, such as the tab the window opens on.
	 *
	 * @param id the hit's id, or null for none
	 */
	void highlightKey(String id)
	{
		keyboardLed = id != null;
		highlight = id;
	}

	/**
	 * Moves the highlight along the page's hits, which are every focusable hit that is not a tab, in focus order.
	 * With nothing highlighted, Down goes to the first and Up to the last.
	 *
	 * @param delta 1 for Down, -1 for Up
	 * @return the hit now highlighted, or null when the page has none
	 */
	Hit move(int delta)
	{
		List<Hit> stops = stops();
		if (stops.isEmpty())
			return null;

		int at = -1;
		for (int i = 0; i < stops.size(); i++)
			if (stops.get(i).id().equals(highlight))
				at = i;

		int to = at < 0 ? (delta > 0 ? 0 : stops.size() - 1) : Math.floorMod(at + delta, stops.size());
		Hit hit = stops.get(to);
		highlight = hit.id();
		keyboardLed = true;
		return hit;
	}

	/**
	 * Finds the tab Left or Right goes to: it wraps and skips a tab that cannot be chosen.
	 *
	 * @param enabled for each tab, in order, whether it can be chosen
	 * @param current the index of the tab now selected
	 * @param delta 1 for Right, -1 for Left
	 * @return the index to go to; the current one when no other tab can be chosen
	 */
	static int nextTab(boolean[] enabled, int current, int delta)
	{
		int n = enabled.length;
		for (int step = 1; step <= n; step++)
		{
			int candidate = Math.floorMod(current + delta * step, n);
			if (enabled[candidate])
				return candidate;
		}

		return current;
	}

	/** @return what Enter or Space does on the highlighted hit: a row opens its menu, anything else is pressed */
	Activation activate()
	{
		Hit hit = highlighted();
		if (hit == null || !hit.enabled())
			return Activation.NONE;

		return hit.kind() == Hit.Kind.ROW ? Activation.MENU : Activation.PRESS;
	}

	/**
	 * Decides what Esc or Backspace does: back one level, menu first, then a page, then the window itself.
	 *
	 * @param menuOpen whether a menu is open
	 * @param pageDepth how many pages deep the window is, 0 on a tab's own page
	 * @param closes true for Esc, which closes the window at the end; false for Backspace, which never does
	 * @return the step to take
	 */
	static Step back(boolean menuOpen, int pageDepth, boolean closes)
	{
		if (menuOpen)
			return Step.MENU;

		if (pageDepth > 0)
			return Step.PAGE;

		return closes ? Step.CLOSE : Step.NONE;
	}

	private List<Hit> stops()
	{
		List<Hit> stops = new ArrayList<>();
		for (Hit hit : hits)
			if (hit.kind() != Hit.Kind.TAB && hit.focusable())
				stops.add(hit);

		stops.sort(Comparator.comparingInt(Hit::order));
		return stops;
	}

	private Hit find(String id)
	{
		for (Hit hit : hits)
			if (hit.id().equals(id))
				return hit;

		return null;
	}
}
