/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import net.runelite.client.input.MouseListener;

/**
 * The mouse side of the painted wild creature menu, a {@link MouseListener} that runs on the UI thread. It never
 * looks at the game: the game thread publishes the wild creature under the pointer as a {@link Spot} on each client
 * tick, and a right click that lands where that spot says is taken, so the game's own menu never opens; the work goes
 * to a {@link Sink} that carries it to the game thread. While the menu is open every click is consumed, so nothing
 * behind it (walking, the camera) happens. Shift held, or RuneLite's drag key, lets a right click through to the
 * game's own menu, which stays the way to Walk here or Examine on that tile. A click that is consumed has its
 * release and its click consumed too. Moving the pointer is never consumed. Holds no RuneLite types beyond the
 * listener interface.
 */
final class WildMenuInput implements MouseListener
{
	/** How many pixels the pointer may have moved since the spot was published and still count as on it. */
	static final int TOLERANCE = 6;

	/** How long after taking a right click the input keeps consuming clicks while the menu is being opened. */
	static final long OPENING_MS = 300;

	private final Supplier<Spot> spot;
	private final BooleanSupplier open;
	private final DragKey drag;
	private final LongSupplier clock;
	private final Sink sink;
	private long openingUntil;
	private boolean releaseTaken;
	private boolean clickTaken;

	/** The wild creature the pointer was over when the game thread last looked. */
	static final class Spot
	{
		private final int wildId;
		private final int x;
		private final int y;

		/**
		 * @param wildId the wild creature's id
		 * @param x the pointer's canvas x when it was published
		 * @param y the pointer's canvas y when it was published
		 */
		Spot(int wildId, int x, int y)
		{
			this.wildId = wildId;
			this.x = x;
			this.y = y;
		}

		/** @return the wild creature's id */
		int wildId()
		{
			return wildId;
		}

		/**
		 * @param px a pointer x
		 * @param py a pointer y
		 * @return whether the pointer is within {@link WildMenuInput#TOLERANCE} of where the spot was published
		 */
		boolean near(int px, int py)
		{
			return Math.abs(px - x) <= TOLERANCE && Math.abs(py - y) <= TOLERANCE;
		}
	}

	/** Where the events go: the game thread side of the menu. Called on the UI thread, which queues each one. */
	interface Sink
	{
		/**
		 * A right click was taken over a wild creature: open the menu.
		 *
		 * @param wildId the wild creature's id
		 * @param x the pointer's canvas x
		 * @param y the pointer's canvas y
		 */
		void open(int wildId, int x, int y);

		/**
		 * The pointer moved while the menu is open.
		 *
		 * @param x the pointer's canvas x
		 * @param y the pointer's canvas y
		 */
		void moved(int x, int y);

		/**
		 * A button went down while the menu is open.
		 *
		 * @param x the pointer's canvas x
		 * @param y the pointer's canvas y
		 * @param button the button, as {@link MouseEvent#getButton}
		 */
		void pressed(int x, int y, int button);
	}

	/**
	 * Creates the listener.
	 *
	 * @param spot the published spot, or null while the pointer is not over a wild creature the menu may take
	 * @param open whether the menu is open, published by the game thread
	 * @param drag RuneLite's drag key
	 * @param clock the time in milliseconds
	 * @param sink where the events go
	 */
	WildMenuInput(Supplier<Spot> spot, BooleanSupplier open, DragKey drag, LongSupplier clock, Sink sink)
	{
		this.spot = spot;
		this.open = open;
		this.drag = drag;
		this.clock = clock;
		this.sink = sink;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (drag.heldIn(event))
			return event;

		int x = event.getX();
		int y = event.getY();
		if (open.getAsBoolean())
		{
			take(event);
			sink.pressed(x, y, event.getButton());
			return event;
		}

		if (clock.getAsLong() < openingUntil)
		{
			take(event);
			return event;
		}

		Spot found = spot.get();
		boolean shift = (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
		if (event.getButton() == MouseEvent.BUTTON3 && !shift && found != null && found.near(x, y))
		{
			take(event);
			openingUntil = clock.getAsLong() + OPENING_MS;
			sink.open(found.wildId(), x, y);
		}

		return event;
	}

	private void take(MouseEvent event)
	{
		releaseTaken = true;
		clickTaken = true;
		event.consume();
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (releaseTaken)
		{
			releaseTaken = false;
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (clickTaken)
		{
			clickTaken = false;
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		return mouseMoved(event);
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		if (open.getAsBoolean())
			sink.moved(event.getX(), event.getY());

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
		return event;
	}

	/** Forgets the opening window, for when the menu is closed from the game thread. */
	void reset()
	{
		openingUntil = 0;
	}
}
