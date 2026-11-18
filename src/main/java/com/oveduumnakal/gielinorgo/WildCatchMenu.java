/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The painted menu of a wild creature: a heading with its name and level, then Battle and Catch. With two or more
 * jars Catch is an item with an arrow that opens a second list beside the first: the suggested jars, a thin line, then
 * the other jars, each with its odds and how many are owned. The entries come from {@link WildMenu#layout}, the same
 * rule the game's own menu uses, so both menus offer the same things in the same order. The pointer and the keys share
 * the highlight. Hovering the Catch item, or Right or Enter on it, opens the list; Left closes it; Esc closes both.
 * While the menu is open every click is its own: a click on an item runs it and closes the menu, a click elsewhere
 * closes the menu. It runs on the game thread, and the actions it is given run there too. {@link #isOpen()} is safe
 * to read from the UI thread. Holds no RuneLite types.
 */
final class WildCatchMenu
{
	private final ContextMenu root = new ContextMenu();
	private final ContextMenu sub = new ContextMenu();
	private volatile boolean open;
	private int wildId = -1;
	private int openerIndex = -1;
	private boolean listActive;
	private boolean openedByKey;
	private Rectangle lastCanvas = new Rectangle();
	private WindowStyle lastStyle;
	private FontMetrics lastMetrics;
	private long lastNowMs;

	/**
	 * Opens the menu for one wild creature.
	 *
	 * @param wild the wild creature's id, so the caller can close the menu when the creature is gone
	 * @param title the heading, such as {@code Chicken (Lv 3 - New)}
	 * @param tops the entries, from {@link WildMenu#layout}
	 * @param owned every owned jar with its odds, for the plain Catch entry
	 * @param battle what Battle does
	 * @param throwJar what a jar does when it is chosen
	 * @param x the pointer's x on the canvas
	 * @param y the pointer's y on the canvas
	 * @param canvas the canvas bounds
	 * @param style the style
	 * @param fm the measures of {@link ContextMenu#font}
	 * @param nowMs the time in milliseconds
	 */
	void open(int wild, String title, List<WildMenu.Top> tops, List<JarSuggest.JarChoice> owned, Runnable battle,
		Consumer<JarType> throwJar, int x, int y, Rectangle canvas, WindowStyle style, FontMetrics fm, long nowMs)
	{
		close();
		List<ContextMenu.Item> rootItems = new ArrayList<>();
		List<ContextMenu.Item> listItems = new ArrayList<>();
		rootItems.add(ContextMenu.Item.header(title));
		openerIndex = -1;
		for (WildMenu.Top top : tops)
		{
			switch (top.kind())
			{
				case BATTLE:
					rootItems.add(ContextMenu.Item.of(WildMenu.BATTLE, battle));
					break;
				case CATCH_PLAIN:
					rootItems.add(plainCatch(top.jar(), owned, throwJar));
					break;
				default:
					openerIndex = rootItems.size();
					rootItems.add(ContextMenu.Item.opener(WildMenu.CATCH, this::showList));
					listItems.addAll(listItems(top.rows(), throwJar));
					break;
			}
		}

		wildId = wild;
		remember(canvas, style, fm, nowMs);
		root.open(rootItems, x, y, canvas, style, fm, false, nowMs);
		sub.open(listItems, 0, 0, canvas, style, fm, false, nowMs);
		sub.close();
		open = root.isOpen();
	}

	private static ContextMenu.Item plainCatch(JarType jar, List<JarSuggest.JarChoice> owned,
		Consumer<JarType> throwJar)
	{
		if (jar == null)
			return ContextMenu.Item.disabled(WildMenu.CATCH, "No jars");

		int pct = 0;
		for (JarSuggest.JarChoice choice : owned)
			if (choice.jar() == jar)
				pct = choice.pct();

		return ContextMenu.Item.detailed(WildMenu.CATCH, jar.label() + " jar " + pct + "%",
			() -> throwJar.accept(jar));
	}

	private static List<ContextMenu.Item> listItems(List<WildMenu.Row> rows, Consumer<JarType> throwJar)
	{
		List<ContextMenu.Item> items = new ArrayList<>();
		for (WildMenu.Row row : rows)
		{
			if (row.kind() == WildMenu.RowKind.SEPARATOR)
			{
				items.add(ContextMenu.Item.line());
				continue;
			}

			JarSuggest.JarChoice choice = row.choice();
			JarType jar = choice.jar();
			items.add(ContextMenu.Item.detailed(WildMenu.jarOption(choice), WildMenu.jarTarget(choice),
				() -> throwJar.accept(jar)));
		}

		return items;
	}

	/** Closes the menu and the list. */
	void close()
	{
		open = false;
		listActive = false;
		openedByKey = false;
		root.close();
		sub.close();
	}

	/** @return whether the menu is open; safe to read from any thread */
	boolean isOpen()
	{
		return open;
	}

	/** @return the id of the wild creature the menu is for, or -1 when it was never opened */
	int wildId()
	{
		return wildId;
	}

	/** @return whether the list of jars is showing */
	boolean listOpen()
	{
		return sub.isOpen();
	}

	/** @return the first menu, for tests and painting */
	ContextMenu rootMenu()
	{
		return root;
	}

	/** @return the list of jars, for tests and painting */
	ContextMenu listMenu()
	{
		return sub;
	}

	/** @return the index in the first menu of the item that opens the list, or -1 when there is none */
	int openerIndex()
	{
		return openerIndex;
	}

	private void showList()
	{
		if (sub.items().isEmpty() || openerIndex < 0)
			return;

		boolean byKey = openedByKey;
		openedByKey = false;
		listActive = byKey;
		if (!sub.isOpen())
			sub.open(sub.items(), 0, 0, lastCanvas, lastStyle, lastMetrics, byKey, lastNowMs);
		else if (byKey && sub.highlight() < 0)
			sub.move(1);

		placeList();
	}

	private void placeList()
	{
		Rectangle opener = root.itemBounds(openerIndex);
		if (opener == null || lastStyle == null || lastMetrics == null)
			return;

		Rectangle first = root.bounds();
		Dimension size = sub.size(lastStyle, lastMetrics);
		int x = first.x + first.width;
		if (x + size.width > lastCanvas.x + lastCanvas.width)
			x = Math.max(lastCanvas.x, first.x - size.width);

		int y = Math.min(opener.y, lastCanvas.y + lastCanvas.height - size.height);
		sub.anchorAt(x, Math.max(lastCanvas.y, y), lastCanvas, lastStyle, lastMetrics);
	}

	private void hideList()
	{
		sub.close();
		listActive = false;
	}

	private boolean overOpener()
	{
		int at = root.highlight();
		return at >= 0 && at < root.items().size() && root.items().get(at).opens();
	}

	/**
	 * Handles the pointer moving.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether the pointer is over the menu or the list
	 */
	boolean onMoved(int x, int y)
	{
		if (!open)
			return false;

		if (sub.isOpen() && sub.bounds().contains(x, y))
		{
			listActive = true;
			return sub.onMouseMoved(x, y);
		}

		if (root.bounds().contains(x, y))
		{
			listActive = false;
			root.onMouseMoved(x, y);
			if (overOpener())
				showList();
			else
				hideList();

			return true;
		}

		return false;
	}

	/**
	 * Handles a mouse button going down.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @param button the button, as {@link java.awt.event.MouseEvent#getButton}
	 * @return whether the click was consumed: always while the menu is open
	 */
	boolean onPressed(int x, int y, int button)
	{
		if (!open)
			return false;

		if (sub.isOpen() && sub.bounds().contains(x, y))
		{
			sub.onMousePressed(x, y, button);
			if (!sub.isOpen())
				close();

			return true;
		}

		if (root.bounds().contains(x, y))
		{
			int at = root.itemAt(x, y);
			if (at >= 0 && at == openerIndex)
				listActive = false;

			root.onMousePressed(x, y, button);
			if (!root.isOpen())
				close();

			return true;
		}

		close();
		return true;
	}

	/**
	 * Tells whether the menu uses a key, so a key listener can consume its pressed, typed and released events.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @return whether the menu is open and the key is an arrow, Enter, Space, Esc or Backspace
	 */
	boolean uses(int keyCode)
	{
		return open && (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT
			|| keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE
			|| keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE);
	}

	/**
	 * Handles a key going down.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @return whether the key was used and must be consumed
	 */
	boolean onKeyPressed(int keyCode)
	{
		if (!uses(keyCode))
			return false;

		ContextMenu active = listActive && sub.isOpen() ? sub : root;
		switch (keyCode)
		{
			case KeyEvent.VK_UP:
				active.move(-1);
				break;
			case KeyEvent.VK_DOWN:
				active.move(1);
				break;
			case KeyEvent.VK_RIGHT:
				if (active == root && overOpener())
				{
					openedByKey = true;
					showList();
				}

				break;
			case KeyEvent.VK_LEFT:
				if (active == sub)
					hideList();

				break;
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_SPACE:
				enter(active);
				break;
			default:
				close();
				break;
		}

		return true;
	}

	private void enter(ContextMenu active)
	{
		openedByKey = active == root;
		active.activate();
		openedByKey = false;
		if (!root.isOpen() || active == sub && !sub.isOpen())
			close();
	}

	/**
	 * Remembers what the list needs to place itself and to open. Called by {@link #open} and by every paint, so the
	 * list follows a new style or canvas.
	 *
	 * @param canvas the canvas bounds
	 * @param style the style
	 * @param fm the measures of {@link ContextMenu#font}
	 * @param nowMs the time in milliseconds
	 */
	void remember(Rectangle canvas, WindowStyle style, FontMetrics fm, long nowMs)
	{
		lastCanvas = canvas;
		lastStyle = style;
		lastMetrics = fm;
		lastNowMs = nowMs;
	}

	/**
	 * Paints the menu, and the list beside it when it is open.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param canvas the canvas bounds
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 */
	void paint(Graphics2D g, WindowStyle style, Rectangle canvas, MotionMode mode, long nowMs)
	{
		if (!open)
			return;

		FontMetrics fm = g.getFontMetrics(ContextMenu.font(style));
		remember(canvas, style, fm, nowMs);
		root.layout(canvas, style, fm);
		if (sub.isOpen())
			placeList();

		root.paint(g, style, canvas, mode, nowMs);
		sub.paint(g, style, canvas, mode, nowMs);
	}
}
