/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Supplier;

import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;

/**
 * The mouse side of the painted window: both a {@link MouseListener} and a {@link MouseWheelListener}, because a
 * mouse listener never sees the wheel. It runs on the UI thread, so it decides from the published {@link WindowView}
 * alone whether an event belongs to the window, and hands the work to a {@link Sink} that carries it to the game
 * thread. Clicks and the wheel inside an open window are consumed, so nothing behind them (walking, the camera zoom)
 * happens. Anything with RuneLite's drag key held passes through untouched, so RuneLite's own move and resize work.
 * A click outside the window passes through and tells the sink, which drops the window's key focus. The same rules
 * serve the toggle's icon, which RuneLite gives no click of its own. Moving the pointer is never consumed; the
 * game's menu entries under the window are cleared on the client tick instead. Holds no RuneLite types beyond the
 * listener interfaces.
 */
final class WindowInput implements MouseListener, MouseWheelListener
{
	private final Supplier<WindowView> view;
	private final Supplier<Rectangle> toggle;
	private final DragKey drag;
	private final Sink sink;
	private boolean windowDown;
	private boolean toggleDown;
	private boolean resetDown;
	private boolean inside;

	/** Where the events go: the game thread side of the window. Called on the UI thread, which queues each one. */
	interface Sink
	{
		/**
		 * The pointer moved over the window.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 */
		void moved(int x, int y);

		/** The pointer left the window or went under the drag key, so nothing is hovered any more. */
		void left();

		/**
		 * A mouse button went down inside the window.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 * @param button the button, as {@link MouseEvent#getButton}
		 * @param modifiers the modifiers, as {@link MouseEvent#getModifiersEx}
		 */
		void pressed(int x, int y, int button, int modifiers);

		/**
		 * A button that went down inside the window came up, wherever the pointer is now.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 * @param button the button
		 * @param modifiers the modifiers
		 */
		void released(int x, int y, int button, int modifiers);

		/**
		 * The pointer moved with a button down that went down inside the window.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 */
		void dragged(int x, int y);

		/**
		 * The wheel turned over the window.
		 *
		 * @param x the pointer's x in window pixels
		 * @param y the pointer's y in window pixels
		 * @param rotation the wheel's clicks; positive is toward the player
		 */
		void wheel(int x, int y, int rotation);

		/** A click landed outside the open window, which drops its key focus. */
		void clickedOutside();

		/** The toggle's icon was clicked. */
		void toggleClicked();

		/**
		 * Alt and a right click landed on the window, which asks for its size and place to be reset.
		 *
		 * @return true when this sink takes the request, so the click is consumed
		 */
		default boolean resetRequested()
		{
			return false;
		}
	}

	/**
	 * Creates the listener.
	 *
	 * @param view the published window view, read on every event
	 * @param toggle the toggle icon's rectangle on the canvas, or null while it is not shown
	 * @param drag RuneLite's drag key
	 * @param sink where the events go
	 */
	WindowInput(Supplier<WindowView> view, Supplier<Rectangle> toggle, DragKey drag, Sink sink)
	{
		this.view = view;
		this.toggle = toggle;
		this.drag = drag;
		this.sink = sink;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		WindowView v = view.get();
		int x = event.getX();
		int y = event.getY();
		if (isReset(event) && v.covers(x, y) && sink.resetRequested())
		{
			resetDown = true;
			event.consume();
			return event;
		}

		if (drag.heldIn(event))
			return event;

		if (v.covers(x, y))
		{
			windowDown = true;
			event.consume();
			sink.pressed(v.localX(x), v.localY(y), event.getButton(), event.getModifiersEx());
			return event;
		}

		if (overToggle(x, y))
		{
			toggleDown = true;
			event.consume();
			return event;
		}

		if (v.open())
			sink.clickedOutside();

		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		boolean consumed = false;
		if (resetDown)
		{
			resetDown = false;
			consumed = true;
		}

		if (windowDown)
		{
			windowDown = false;
			consumed = true;
			WindowView v = view.get();
			sink.released(v.localX(event.getX()), v.localY(event.getY()), event.getButton(),
				event.getModifiersEx());
		}

		if (toggleDown)
		{
			toggleDown = false;
			consumed = true;
			if (overToggle(event.getX(), event.getY()) && event.getButton() == MouseEvent.BUTTON1)
				sink.toggleClicked();
		}

		if (consumed)
			event.consume();

		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (isReset(event) && view.get().covers(event.getX(), event.getY()) || !drag.heldIn(event)
			&& (view.get().covers(event.getX(), event.getY()) || overToggle(event.getX(), event.getY())))
			event.consume();

		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (windowDown)
		{
			WindowView v = view.get();
			event.consume();
			sink.dragged(v.localX(event.getX()), v.localY(event.getY()));
		}
		else if (toggleDown)
		{
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		WindowView v = view.get();
		boolean over = !drag.heldIn(event) && v.covers(event.getX(), event.getY());
		if (over)
			sink.moved(v.localX(event.getX()), v.localY(event.getY()));
		else if (inside)
			sink.left();

		inside = over;
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		if (inside)
			sink.left();

		inside = false;
		return event;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		WindowView v = view.get();
		if (drag.heldIn(event) || !v.covers(event.getX(), event.getY()))
			return event;

		event.consume();
		sink.wheel(v.localX(event.getX()), v.localY(event.getY()), event.getWheelRotation());
		return event;
	}

	private static boolean isReset(MouseEvent event)
	{
		return event.getButton() == MouseEvent.BUTTON3 && event.isAltDown();
	}

	private boolean overToggle(int x, int y)
	{
		Rectangle t = toggle.get();
		return t != null && t.contains(x, y);
	}
}
