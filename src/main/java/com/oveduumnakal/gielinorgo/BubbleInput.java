/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.MouseEvent;
import java.util.function.Supplier;

import net.runelite.client.input.MouseListener;

/**
 * The mouse side of the wild creature bubbles, a {@link MouseListener} that runs on the UI thread. It never looks at
 * the game or the bubbles' model: the game thread paints a frame and publishes what it made clickable as one
 * immutable {@link BubbleHits}, and a left or right press that lands on a bubble is taken, so the game never walks
 * or opens its menu there; the work goes to a {@link Sink} that carries it to the game thread. A press that lands
 * on nothing passes through, and while a jar cluster is open or the card is pinned the sink is told too, so they
 * close. The drag key lets every press through. A press that is taken has its drag, its release and its click
 * taken too. Moving the pointer is never taken. Holds no RuneLite types beyond the listener interface.
 */
final class BubbleInput implements MouseListener
{
	private final Supplier<BubbleHits> hits;
	private final DragKey drag;
	private final Sink sink;
	private boolean releaseTaken;
	private boolean clickTaken;
	private boolean pressDown;

	/** Where the events go: the game thread side of the bubbles. Called on the UI thread, which queues each one. */
	interface Sink
	{
		/**
		 * A press was taken on a bubble.
		 *
		 * @param generation the generation of the hits the press was made against
		 * @param kind the bubble that was pressed
		 * @param jar the jar of a cluster bubble or the jar bubble, or null
		 * @param button the button, as {@link MouseEvent#getButton}
		 */
		void pressed(int generation, BubbleKind kind, JarType jar, int button);

		/**
		 * A press landed on no bubble while the cluster was open or the card was pinned.
		 *
		 * @param generation the generation of the hits the press was made against
		 */
		void pressedOutside(int generation);
	}

	/**
	 * Creates the listener.
	 *
	 * @param hits the latest published click targets; {@link BubbleHits#EMPTY} when there are none
	 * @param drag RuneLite's drag key
	 * @param sink where the events go
	 */
	BubbleInput(Supplier<BubbleHits> hits, DragKey drag, Sink sink)
	{
		this.hits = hits;
		this.drag = drag;
		this.sink = sink;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		pressDown = false;
		releaseTaken = false;
		clickTaken = false;
		if (drag.heldIn(event))
			return event;

		int button = event.getButton();
		if (button != MouseEvent.BUTTON1 && button != MouseEvent.BUTTON3)
			return event;

		BubbleHits now = hits.get();
		BubbleHits.Hit hit = now.at(event.getX(), event.getY());
		if (hit == null)
		{
			if (now.modal())
				sink.pressedOutside(now.generation());

			return event;
		}

		take(event);
		sink.pressed(now.generation(), hit.kind(), hit.jar(), button);
		return event;
	}

	private void take(MouseEvent event)
	{
		releaseTaken = true;
		clickTaken = true;
		pressDown = true;
		event.consume();
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		pressDown = false;
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
		if (pressDown)
			event.consume();

		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
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
}
