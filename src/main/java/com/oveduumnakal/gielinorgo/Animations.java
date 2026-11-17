/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The window's table of running animations, keyed by element id (such as "row:2" or "tab:Bag"). Painting asks it for
 * the value to draw; hit-testing asks it for the final value instead, so a click lands on the layout the player will
 * see, not on a row that is still moving. The one exception is a sliding row (Make lead swap, Move to storage), which
 * is started as blocking and refuses clicks until it lands, so a click never reaches a different creature than the one
 * under the pointer. The game thread owns it. Holds no RuneLite types.
 */
final class Animations
{
	private final Map<String, Running> running = new HashMap<>();

	/**
	 * Starts an animation on an element, replacing any running one.
	 *
	 * @param id the element id
	 * @param tween the movement
	 * @param blocksClicks whether the element ignores clicks until the tween lands
	 */
	void start(String id, Tween tween, boolean blocksClicks)
	{
		running.put(id, new Running(tween, blocksClicks));
	}

	/**
	 * Starts a slide of a row from one position to another, as a blocking animation. In {@link MotionMode#REDUCED}
	 * and {@link MotionMode#OFF} nothing slides, so nothing starts and the row is simply at its final place.
	 *
	 * @param id the element id
	 * @param mode the motion mode
	 * @param from where the row was
	 * @param to where the row goes
	 * @param nowMs the time in milliseconds
	 * @return whether a slide started
	 */
	boolean slide(String id, MotionMode mode, double from, double to, long nowMs)
	{
		long ms = mode.moveMs(WindowTimes.SLIDE);
		if (ms <= 0)
		{
			running.remove(id);
			return false;
		}

		start(id, Tween.of(from, to, nowMs, ms, Easing.OUT_CUBIC), true);
		return true;
	}

	/**
	 * Starts a staggered entrance, such as party rows sliding in one after another. Nothing starts when the mode
	 * does not move things. These do not block clicks: the hit rectangles are the final ones.
	 *
	 * @param id the element id
	 * @param mode the motion mode
	 * @param from where the element starts
	 * @param to where it rests
	 * @param nowMs the time in milliseconds
	 * @param index the element's place in the group, from 0
	 * @return whether an animation started
	 */
	boolean enter(String id, MotionMode mode, double from, double to, long nowMs, int index)
	{
		long ms = mode.moveMs(WindowTimes.ROW_IN);
		if (ms <= 0)
		{
			running.remove(id);
			return false;
		}

		long delay = Tween.stagger(index, WindowTimes.ROW_STAGGER);
		start(id, new Tween(from, to, nowMs, delay, ms, Easing.OUT_CUBIC), false);
		return true;
	}

	/**
	 * Gives the value to paint an element at.
	 *
	 * @param id the element id
	 * @param nowMs the time in milliseconds
	 * @param resting the value the element has when nothing animates it
	 * @return the tween's value while one runs (the start value while it waits out its delay), else the resting value
	 */
	double value(String id, long nowMs, double resting)
	{
		Running r = running.get(id);
		return r == null || r.tween.done(nowMs) ? resting : r.tween.value(nowMs);
	}

	/**
	 * Gives the value a hit rectangle uses: where the element will rest, not where it is now.
	 *
	 * @param id the element id
	 * @param resting the value the element has when nothing animates it
	 * @return the end value of a running animation, else the resting value
	 */
	double finalValue(String id, double resting)
	{
		Running r = running.get(id);
		return r == null ? resting : r.tween.to();
	}

	/**
	 * @param id the element id
	 * @param nowMs the time in milliseconds
	 * @return whether the element still animates, including while it waits out a delay
	 */
	boolean active(String id, long nowMs)
	{
		Running r = running.get(id);
		return r != null && !r.tween.done(nowMs);
	}

	/**
	 * @param id the element id
	 * @param nowMs the time in milliseconds
	 * @return whether a click on the element must be ignored: it is a sliding row that has not landed yet
	 */
	boolean blocksClicks(String id, long nowMs)
	{
		Running r = running.get(id);
		return r != null && r.blocks && !r.tween.done(nowMs);
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return whether anything still animates; the window keeps repainting every frame while this is true
	 */
	boolean anyActive(long nowMs)
	{
		for (Running r : running.values())
			if (!r.tween.done(nowMs))
				return true;

		return false;
	}

	/**
	 * Drops the animations that have landed.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void prune(long nowMs)
	{
		Iterator<Running> it = running.values().iterator();
		while (it.hasNext())
			if (it.next().tween.done(nowMs))
				it.remove();
	}

	/** Drops every animation, for when the window closes. */
	void clear()
	{
		running.clear();
	}

	/** @return how many animations the table holds, landed ones included until they are pruned */
	int size()
	{
		return running.size();
	}

	/** One animation and whether it blocks clicks. */
	private static final class Running
	{
		private final Tween tween;
		private final boolean blocks;

		Running(Tween tween, boolean blocks)
		{
			this.tween = tween;
			this.blocks = blocks;
		}
	}
}
