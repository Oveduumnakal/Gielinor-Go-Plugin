/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

/**
 * The life of one wild creature's body: it fades in, stands and now and then wanders a step or two, takes part in a
 * battle or a catch, and leaves. All of its timing lives here and runs on a clock that is handed in, so it can be
 * tested with a fake one. A body stays for {@link #STAY_MS} of idle time (the clock does not run while it fights or
 * is being caught), and leaves at once, fading out over {@link #FADE_MS}, when the player is more than
 * {@link #LEAVE_TILES} tiles away or on another plane. A caught or fled creature is gone with no fade, because the
 * catch scene has already made it vanish. Holds no RuneLite types.
 */
final class WildLife
{
	/** How long a wild creature stays when nobody bothers it: three minutes. */
	static final long STAY_MS = 180_000L;

	/** How long the fade in at the start lasts. */
	static final long APPEAR_MS = 1_000L;

	/** How long the fade out at the end lasts: two seconds. */
	static final long FADE_MS = 2_000L;

	/** How far from the creature the player may be before it leaves, counted as the game counts distance. */
	static final int LEAVE_TILES = 25;

	/** Farthest a wandering creature goes from the tile it appeared on, in tiles. */
	static final int WANDER_RADIUS = 3;

	/** Most steps one wander takes. */
	static final int WANDER_STEPS = 2;

	/** Shortest rest between two wanders. */
	static final long WANDER_MIN_MS = 3_000L;

	/** Longest rest between two wanders. */
	static final long WANDER_MAX_MS = 8_000L;

	/** What the creature is doing. */
	enum State
	{
		/** Fading in. */
		APPEARING,

		/** Standing still. */
		IDLE,

		/** Walking to a spot a step or two away. */
		ROAMING,

		/** In a battle that is acted out on screen. */
		IN_BATTLE,

		/** Lying knocked out after losing a battle, open to a jar. */
		KNOCKED_OUT,

		/** A jar has landed and the catch scene plays on the body. */
		IN_CATCH,

		/** Fading out because its time is up or the player went away. */
		LEAVING,

		/** Over: the body is to be removed. */
		GONE
	}

	private final Random random;
	private State state = State.APPEARING;
	private final long bornMs;
	private long lastMs;
	private long stayLeftMs = STAY_MS;
	private long nextWanderMs;
	private long leaveMs;

	/**
	 * Starts a life.
	 *
	 * @param nowMs the clock now, in milliseconds
	 * @param random picks how long it rests between wanders
	 */
	WildLife(long nowMs, Random random)
	{
		this.random = random;
		this.bornMs = nowMs;
		this.lastMs = nowMs;
		this.nextWanderMs = nowMs + APPEAR_MS + restMs();
	}

	/**
	 * Moves the life on to a new moment. Called every game tick.
	 *
	 * @param nowMs the clock now, in milliseconds
	 * @param distanceTiles how far the player is, counted as the game counts distance
	 * @param samePlane whether the player is on the creature's plane
	 */
	void update(long nowMs, int distanceTiles, boolean samePlane)
	{
		long spent = Math.max(0L, nowMs - lastMs);
		lastMs = nowMs;
		if (state == State.GONE)
			return;

		if (state == State.LEAVING)
		{
			if (nowMs - leaveMs >= FADE_MS)
				state = State.GONE;

			return;
		}

		if (state == State.APPEARING && nowMs - bornMs >= APPEAR_MS)
			state = State.IDLE;

		if (!paused())
			stayLeftMs -= spent;

		if (distanceTiles > LEAVE_TILES || !samePlane || stayLeftMs <= 0)
			startLeaving(nowMs);
	}

	private boolean paused()
	{
		return state == State.IN_BATTLE || state == State.KNOCKED_OUT || state == State.IN_CATCH;
	}

	private void startLeaving(long nowMs)
	{
		state = State.LEAVING;
		leaveMs = nowMs;
	}

	/** @return what the creature is doing */
	State state()
	{
		return state;
	}

	/** @return whether the body is to be removed */
	boolean gone()
	{
		return state == State.GONE;
	}

	/** @return whether the creature is on its way out or already gone, so nothing new may start with it */
	boolean leaving()
	{
		return state == State.LEAVING || state == State.GONE;
	}

	/**
	 * Tells how solid the body is drawn.
	 *
	 * @param nowMs the clock now, in milliseconds
	 * @return 0 to 1: rising while it appears, falling while it leaves, else 1; 0 once gone
	 */
	double opacity(long nowMs)
	{
		if (state == State.GONE)
			return 0.0;

		if (state == State.LEAVING)
			return clamp(1.0 - (nowMs - leaveMs) / (double) FADE_MS);

		if (state == State.APPEARING)
			return clamp((nowMs - bornMs) / (double) APPEAR_MS);

		return 1.0;
	}

	/** @return how much idle time is left before the creature leaves, in milliseconds */
	long stayLeftMs()
	{
		return stayLeftMs;
	}

	/**
	 * Tells whether a wander is due: the creature stands, and has rested long enough.
	 *
	 * @param nowMs the clock now, in milliseconds
	 * @return true when the body should pick a spot to walk to
	 */
	boolean wanderDue(long nowMs)
	{
		return state == State.IDLE && nowMs >= nextWanderMs;
	}

	/** Notes that the body set off on a wander. */
	void startWander()
	{
		if (state == State.IDLE)
			state = State.ROAMING;
	}

	/**
	 * Notes that the wander is over, or that none was possible, and picks the next rest.
	 *
	 * @param nowMs the clock now, in milliseconds
	 */
	void endWander(long nowMs)
	{
		if (state == State.ROAMING)
			state = State.IDLE;

		nextWanderMs = nowMs + restMs();
	}

	private long restMs()
	{
		return WANDER_MIN_MS + (long) (random.nextDouble() * (WANDER_MAX_MS - WANDER_MIN_MS));
	}

	/**
	 * Notes that a battle starts on the body.
	 *
	 * @return false when the creature is leaving, in a battle or being caught, so no battle may start
	 */
	boolean beginBattle()
	{
		if (state != State.APPEARING && state != State.IDLE && state != State.ROAMING)
			return false;

		state = State.IN_BATTLE;
		return true;
	}

	/** Notes that the creature lost the battle and lies knocked out. */
	void knockOut()
	{
		if (state == State.IN_BATTLE)
			state = State.KNOCKED_OUT;
	}

	/**
	 * Notes that the battle is over and the creature stands again.
	 *
	 * @param nowMs the clock now, in milliseconds
	 */
	void endBattle(long nowMs)
	{
		if (state == State.IN_BATTLE || state == State.KNOCKED_OUT)
		{
			state = State.IDLE;
			nextWanderMs = nowMs + restMs();
		}
	}

	/** @return whether the creature lies knocked out */
	boolean knockedOut()
	{
		return state == State.KNOCKED_OUT;
	}

	/**
	 * Notes that a jar landed and the catch scene starts.
	 *
	 * @return false when the creature is already leaving, so there is nothing to catch
	 */
	boolean beginCatch()
	{
		if (leaving())
			return false;

		state = State.IN_CATCH;
		return true;
	}

	/**
	 * Notes that the catch scene is over.
	 *
	 * @param stays true when the creature broke free and is still there; false when it was caught or fled, so it
	 *     is gone
	 * @param nowMs the clock now, in milliseconds
	 */
	void endCatch(boolean stays, long nowMs)
	{
		if (state != State.IN_CATCH)
			return;

		if (stays)
		{
			state = State.IDLE;
			nextWanderMs = nowMs + restMs();
		}
		else
		{
			state = State.GONE;
		}
	}

	/** Ends the creature at once with no fade, because it was caught or fled and no scene plays. */
	void remove()
	{
		state = State.GONE;
	}

	private static double clamp(double value)
	{
		return Math.max(0.0, Math.min(1.0, value));
	}
}
