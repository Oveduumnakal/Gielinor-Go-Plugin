/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The timing of a catch on the player's screen, counted in client ticks (20 ms each) from the moment the jar is
 * thrown, and which emote the character shows for each result. Holds no RuneLite types: the animation ids are
 * the game's own numbers, and a test checks them against RuneLite's names.
 */
final class CatchScene
{
	/** Client ticks the throw animation lasts. */
	static final int THROW_TICKS = 54;

	/** The client tick at which the jar lands and a caught creature vanishes. */
	static final int LAND_TICK = 40;

	/** The client tick at which the reaction starts: the end of the throw plus one game tick (30 client ticks). */
	static final int REACT_TICK = 84;

	/** What {@link #reactionFor(CatchOutcome)} returns when the character should not react. */
	static final int NONE = -1;

	/** The cheer emote, shown for a catch. */
	static final int CHEER = 862;

	/** The angry emote, shown when the creature breaks free. */
	static final int ANGRY = 859;

	/** The cry emote, shown when the creature runs away. */
	static final int CRY = 860;

	/** Where a throw is in its course. */
	enum Phase
	{
		/** The jar is in the air. */
		THROWING,
		/** The jar has landed and the character waits for the reaction. */
		WAITING,
		/** The moment the reaction starts. */
		REACTING,
		/** Nothing is left to play. */
		DONE
	}

	private CatchScene()
	{
	}

	/**
	 * Tells where a throw is in its course.
	 *
	 * @param age client ticks since the throw
	 * @return the phase at that moment
	 */
	static Phase phaseAt(int age)
	{
		if (age < LAND_TICK)
			return Phase.THROWING;

		if (age < REACT_TICK)
			return Phase.WAITING;

		return age == REACT_TICK ? Phase.REACTING : Phase.DONE;
	}

	/**
	 * Tells whether the jar has landed.
	 *
	 * @param age client ticks since the throw
	 * @return whether a caught creature is gone by now
	 */
	static boolean landed(int age)
	{
		return age >= LAND_TICK;
	}

	/**
	 * Picks the emote for a result.
	 *
	 * @param outcome what the throw led to
	 * @return the animation id, or {@link #NONE} when no jar was thrown
	 */
	static int reactionFor(CatchOutcome outcome)
	{
		switch (outcome)
		{
			case CAUGHT:
				return CHEER;
			case BROKE_FREE:
				return ANGRY;
			case FLED:
				return CRY;
			default:
				return NONE;
		}
	}
}
