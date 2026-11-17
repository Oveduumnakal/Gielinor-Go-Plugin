/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The right-click menu of a row, and the menu Enter or Space opens on a highlighted row. It holds a list of items,
 * each with a label, an enabled state, a reason shown beside a disabled one (such as "Coming with the tracker") and
 * an action. One highlight is shared by the pointer and the keys: moving the pointer over an enabled item highlights
 * it, Up and Down move it (wrapping, skipping disabled items), Enter or Space runs it, Esc or Backspace closes the
 * menu. While open the menu consumes every click: a click on an enabled item runs it and closes the menu, a click
 * on a disabled item does nothing, and a click anywhere else closes the menu and is still consumed, so it never
 * reaches the row beneath. The menu scales out from the click point over 100 ms in full motion; its hit rectangles
 * are the final ones from the first frame. Actions run on the caller's thread: the caller wraps them for the game
 * thread. Holds no RuneLite types.
 */
final class ContextMenu
{
	private List<Item> items = Collections.emptyList();
	private boolean open;
	private int highlight = -1;
	private Point anchor = new Point();
	private Rectangle bounds = new Rectangle();
	private List<Rectangle> boxes = Collections.emptyList();
	private int itemHeight;
	private long openedMs;

	/** One entry of the menu. */
	static final class Item
	{
		private final String label;
		private final boolean enabled;
		private final String reason;
		private final Runnable action;

		private Item(String label, boolean enabled, String reason, Runnable action)
		{
			this.label = label;
			this.enabled = enabled;
			this.reason = reason == null ? "" : reason;
			this.action = action;
		}

		/**
		 * @param label the text
		 * @param action what to do when it is chosen
		 * @return an enabled item
		 */
		static Item of(String label, Runnable action)
		{
			return new Item(label, true, null, action);
		}

		/**
		 * @param label the text
		 * @param reason why it cannot be chosen, shown beside it; may be empty
		 * @return a dim item that does nothing
		 */
		static Item disabled(String label, String reason)
		{
			return new Item(label, false, reason, null);
		}

		/** @return the text */
		String label()
		{
			return label;
		}

		/** @return whether it can be chosen */
		boolean enabled()
		{
			return enabled;
		}

		/** @return why it cannot be chosen, or an empty text */
		String reason()
		{
			return reason;
		}
	}

	/**
	 * @param style the style
	 * @return the font the menu uses, a whole multiple of the regular font's native size
	 */
	static Font font(WindowStyle style)
	{
		return style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
	}

	/**
	 * Opens the menu at a point.
	 *
	 * @param list the items, in order
	 * @param x the click's x, where the menu's corner goes
	 * @param y the click's y
	 * @param canvas the canvas bounds; the menu flips to the other side of the point, and is kept inside
	 * @param style the style
	 * @param fm the measures of {@link #font}
	 * @param keyboard whether a key opened it: the first enabled item (Summary) is then highlighted, otherwise
	 *        nothing is until the pointer or a key moves
	 * @param nowMs the time in milliseconds
	 */
	void open(List<Item> list, int x, int y, Rectangle canvas, WindowStyle style, FontMetrics fm, boolean keyboard,
		long nowMs)
	{
		items = Collections.unmodifiableList(new ArrayList<>(list));
		open = !items.isEmpty();
		anchor = new Point(x, y);
		openedMs = nowMs;
		highlight = keyboard ? next(-1, 1) : -1;
		layout(canvas, style, fm);
	}

	/** Closes the menu. */
	void close()
	{
		open = false;
		highlight = -1;
	}

	/** @return whether the menu is open */
	boolean isOpen()
	{
		return open;
	}

	/** @return the items, in order; empty when never opened */
	List<Item> items()
	{
		return items;
	}

	/** @return the index of the highlighted item, or -1 for none */
	int highlight()
	{
		return highlight;
	}

	/** @return the point the menu was opened at */
	Point anchor()
	{
		return new Point(anchor);
	}

	/**
	 * Works the boxes out again, for a new scale or canvas. Painting calls it each frame.
	 *
	 * @param canvas the canvas bounds
	 * @param style the style
	 * @param fm the measures of {@link #font}
	 */
	void layout(Rectangle canvas, WindowStyle style, FontMetrics fm)
	{
		int pad = style.metrics().pad;
		itemHeight = fm.getHeight() + pad / 2;
		int gap = 2 * pad;
		int widest = 0;
		for (Item item : items)
		{
			int w = fm.stringWidth(item.label) + (item.reason.isEmpty() ? 0 : gap + fm.stringWidth(item.reason));
			widest = Math.max(widest, w);
		}

		Dimension size = new Dimension(widest + 2 * pad, items.size() * itemHeight + pad);
		bounds = Tooltip.place(size, anchor, canvas, 0);
		List<Rectangle> list = new ArrayList<>();
		for (int i = 0; i < items.size(); i++)
			list.add(new Rectangle(bounds.x, bounds.y + pad / 2 + i * itemHeight, bounds.width, itemHeight));

		boxes = list;
	}

	/** @return the menu's box, by its final place */
	Rectangle bounds()
	{
		return new Rectangle(bounds);
	}

	/**
	 * @param index the item
	 * @return the item's box, by its final place, or null when there is no such item
	 */
	Rectangle itemBounds(int index)
	{
		return index < 0 || index >= boxes.size() ? null : new Rectangle(boxes.get(index));
	}

	/**
	 * Finds the item under a point.
	 *
	 * @param x the point's x
	 * @param y the point's y
	 * @return the item's index, or -1 when the point is not on an item
	 */
	int itemAt(int x, int y)
	{
		for (int i = 0; i < boxes.size(); i++)
			if (boxes.get(i).contains(x, y))
				return i;

		return -1;
	}

	/**
	 * Moves the highlight, wrapping and skipping disabled items. With no enabled item nothing moves.
	 *
	 * @param delta 1 for down, -1 for up
	 */
	void move(int delta)
	{
		int step = delta < 0 ? -1 : 1;
		int at = next(highlight < 0 && step < 0 ? 0 : highlight, step);
		if (at >= 0)
			highlight = at;
	}

	private int next(int from, int step)
	{
		int n = items.size();
		for (int i = 1; i <= n; i++)
		{
			int at = Math.floorMod(from + step * i, n);
			if (items.get(at).enabled)
				return at;
		}

		return -1;
	}

	/**
	 * Runs the highlighted item and closes the menu.
	 *
	 * @return whether an item ran; false when nothing enabled is highlighted
	 */
	boolean activate()
	{
		if (!open || highlight < 0 || !items.get(highlight).enabled)
			return false;

		Runnable action = items.get(highlight).action;
		close();
		if (action != null)
			action.run();

		return true;
	}

	/**
	 * Handles the pointer moving: it highlights an enabled item under it.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether the pointer is over the menu, so the caller does not hover what lies beneath
	 */
	boolean onMouseMoved(int x, int y)
	{
		if (!open)
			return false;

		int at = itemAt(x, y);
		if (at >= 0 && items.get(at).enabled)
			highlight = at;

		return bounds.contains(x, y);
	}

	/**
	 * Handles a mouse button going down.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @param button the button, as {@link MouseEvent#getButton}
	 * @return whether the click was consumed: always while the menu is open
	 */
	boolean onMousePressed(int x, int y, int button)
	{
		if (!open)
			return false;

		int at = itemAt(x, y);
		if (at < 0)
		{
			close();
			return true;
		}

		if (button == MouseEvent.BUTTON1 && items.get(at).enabled)
		{
			highlight = at;
			activate();
		}

		return true;
	}

	/**
	 * Tells whether the menu uses a key, so a key listener can consume its pressed, typed and released events.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @return whether the menu is open and the key is Up, Down, Enter, Space, Esc or Backspace
	 */
	boolean uses(int keyCode)
	{
		return open && (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_ENTER
			|| keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE);
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

		switch (keyCode)
		{
			case KeyEvent.VK_UP:
				move(-1);
				break;
			case KeyEvent.VK_DOWN:
				move(1);
				break;
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_SPACE:
				activate();
				break;
			default:
				close();
				break;
		}

		return true;
	}

	/**
	 * Paints the menu: scaled out from the click point and fading in while it opens, then plain.
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

		Font font = font(style);
		FontMetrics fm = g.getFontMetrics(font);
		layout(canvas, style, fm);
		AffineTransform saved = g.getTransform();
		Composite savedComposite = g.getComposite();
		long scaleMs = mode.moveMs(WindowTimes.MENU);
		long fadeMs = mode.fadeMs(WindowTimes.MENU);
		double scale = 1.0;
		if (scaleMs > 0)
			scale = 0.85 + 0.15 * Easing.OUT_CUBIC.apply((double) (nowMs - openedMs) / scaleMs);

		if (fadeMs > 0)
		{
			float alpha = (float) Easing.OUT_CUBIC.apply((double) (nowMs - openedMs) / fadeMs);
			g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
		}

		g.translate(anchor.x, anchor.y);
		g.scale(scale, scale);
		g.translate(-anchor.x, -anchor.y);
		style.panel(g, new Rectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height));
		int pad = style.metrics().pad;
		for (int i = 0; i < items.size(); i++)
		{
			Item item = items.get(i);
			Rectangle box = boxes.get(i);
			if (i == highlight)
			{
				g.setColor(style.color(ThemeColor.SELECTION));
				g.fillRect(box.x + style.metrics().border, box.y, box.width - 2 * style.metrics().border, box.height);
			}

			int baseline = box.y + (box.height + fm.getAscent() - fm.getDescent()) / 2;
			style.text(g, item.label, box.x + pad, baseline, font,
				item.enabled ? ThemeColor.TEXT : ThemeColor.TAB_DISABLED_TEXT);
			if (!item.reason.isEmpty())
				style.text(g, item.reason, box.x + box.width - pad - fm.stringWidth(item.reason), baseline, font,
					ThemeColor.SUBTEXT);
		}

		g.setTransform(saved);
		g.setComposite(savedComposite);
	}
}
