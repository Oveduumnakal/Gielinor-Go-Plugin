/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.runelite.client.input.MouseListener;

/**
 * Reorders the party from the pinned party box: a left drag from one row onto another swaps the two. Only a press on a
 * row is taken from the game; the gaps, right clicks, clicks while the drag key is held and clicks something else
 * already took all go through. It has no link to the game client.
 */
final class PartyBoxInput implements MouseListener
{
	private final Supplier<List<Rectangle>> rows;
	private final Supplier<List<CreatureView>> team;
	private final DragKey drag;
	private final Predicate<Point> covered;
	private final Sink sink;
	private volatile int from = -1;
	private volatile int over = -1;
	private List<CreatureView> pressed = Collections.emptyList();
	private boolean releaseTaken;
	private boolean clickTaken;

	/** What a finished drag does. The picks name the creatures as they were listed when the drag began. */
	interface Sink
	{
		/**
		 * @param from the creature whose row was dragged
		 * @param onto the creature whose row it was dropped on
		 */
		void swap(TradePick from, TradePick onto);
	}

	/**
	 * @param rows the canvas boxes of the rows that may be dragged now, in team order; empty for none
	 * @param team the creatures the rows show, in team order
	 * @param drag RuneLite's drag key, which moves the box instead
	 * @param covered whether something else lies over the box at a point
	 * @param sink takes a finished drag
	 */
	PartyBoxInput(Supplier<List<Rectangle>> rows, Supplier<List<CreatureView>> team, DragKey drag,
		Predicate<Point> covered, Sink sink)
	{
		this.rows = rows;
		this.team = team;
		this.drag = drag;
		this.covered = covered;
		this.sink = sink;
	}

	/** @return the place of the row being dragged, or -1 */
	int from()
	{
		return from;
	}

	/** @return the place of the row the drag is over, or -1 */
	int over()
	{
		return over;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		from = -1;
		over = -1;
		releaseTaken = false;
		clickTaken = false;
		if (event.isConsumed() || event.getButton() != MouseEvent.BUTTON1 || drag.heldIn(event)
			|| covered.test(event.getPoint()))
			return event;

		int place = placeAt(event.getPoint());
		if (place < 0)
			return event;

		pressed = team.get();
		from = place;
		over = place;
		releaseTaken = true;
		clickTaken = true;
		event.consume();
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (from >= 0)
		{
			over = placeAt(event.getPoint());
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		int dragged = from;
		from = -1;
		over = -1;
		if (!releaseTaken)
			return event;

		releaseTaken = false;
		event.consume();
		int onto = placeAt(event.getPoint());
		if (dragged >= 0 && onto >= 0 && onto != dragged && Math.max(dragged, onto) < pressed.size())
			sink.swap(pick(dragged), pick(onto));

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

	private TradePick pick(int place)
	{
		return TradePick.of(Holding.TEAM, place, pressed.get(place));
	}

	private int placeAt(Point point)
	{
		List<Rectangle> now = rows.get();
		for (int i = 0; i < now.size(); i++)
		{
			if (now.get(i).contains(point))
				return i;
		}

		return -1;
	}
}
