/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * What the creature that a jar was thrown at does on the player's screen, counted in client ticks (20 ms each)
 * from the throw. When the jar lands the creature flinches; after that a caught creature shrinks to nothing, one
 * that breaks free turns to the player and attacks, and one that flees turns away and runs three tiles while it
 * fades out. Holds no RuneLite types, so the whole timing can be tested without a client.
 */
final class TargetScript
{
	/** The client tick at which the creature flinches: the moment the jar lands. */
	static final int LAND = CatchScene.LAND_TICK;

	/** Client ticks between the landing and the second act: the shrink, the turn to the player or the run. */
	static final int ACT_DELAY = 20;

	/** The client tick at which the second act starts. */
	static final int ACT_TICK = LAND + ACT_DELAY;

	/** Client ticks a caught creature takes to shrink from full size to nothing. */
	static final int SHRINK_TICKS = 25;

	/** The client tick at which a caught creature has shrunk to nothing and the plain vanish takes over. */
	static final int CAUGHT_END = ACT_TICK + SHRINK_TICKS;

	/** The client tick at which a creature that broke free is the real monster again. */
	static final int BROKE_FREE_END = LAND + 60;

	/** The client tick at which a fleeing creature has faded out and the real monster starts to fade in. */
	static final int FLED_END = LAND + 80;

	/** How far a fleeing creature runs, in tiles. */
	static final double RUN_TILES = 3.0;

	/** The act that starts on a tick. */
	enum Step
	{
		/** Nothing new starts. */
		NONE,
		/** The creature flinches. */
		HIT,
		/** The creature plays its attack. */
		ATTACK
	}

	/** The way a creature turns when its second act starts. */
	enum Turn
	{
		/** It keeps its facing. */
		NONE,
		/** It faces the player. */
		TO_PLAYER,
		/** It faces away from the player. */
		AWAY
	}

	private TargetScript()
	{
	}

	/**
	 * Gives the tick at which the script is over.
	 *
	 * @param outcome what the throw led to
	 * @return the client tick, or 0 for a result that never touches the creature
	 */
	static int end(CatchOutcome outcome)
	{
		switch (outcome)
		{
			case CAUGHT:
				return CAUGHT_END;
			case BROKE_FREE:
				return BROKE_FREE_END;
			case FLED:
				return FLED_END;
			default:
				return 0;
		}
	}

	/**
	 * Tells whether the real monster is out of sight because the script is acting in its place.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return true from the landing until the script is over
	 */
	static boolean hidden(CatchOutcome outcome, int age)
	{
		return age >= LAND && age < end(outcome);
	}

	/**
	 * Tells whether the script is over.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return whether the copy is gone and the plain vanish or the real monster takes over
	 */
	static boolean done(CatchOutcome outcome, int age)
	{
		return age >= end(outcome);
	}

	/**
	 * Tells whether the copy of the creature is drawn.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return whether the script is running and the copy has any size and opacity left
	 */
	static boolean copyShown(CatchOutcome outcome, int age)
	{
		return hidden(outcome, age) && size(outcome, age) > 0 && opacity(outcome, age) > 0;
	}

	/**
	 * Tells which act starts on a tick.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return the flinch at the landing, the attack of a creature that breaks free when its turn comes, else none
	 */
	static Step stepAt(CatchOutcome outcome, int age)
	{
		if (end(outcome) == 0)
			return Step.NONE;

		if (age == LAND)
			return Step.HIT;

		return age == ACT_TICK && outcome == CatchOutcome.BROKE_FREE ? Step.ATTACK : Step.NONE;
	}

	/**
	 * Tells how the creature turns when its second act starts.
	 *
	 * @param outcome what the throw led to
	 * @return the turn
	 */
	static Turn turnOf(CatchOutcome outcome)
	{
		switch (outcome)
		{
			case BROKE_FREE:
				return Turn.TO_PLAYER;
			case FLED:
				return Turn.AWAY;
			default:
				return Turn.NONE;
		}
	}

	/**
	 * Gives how big the copy is.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return 1 for full size down to 0 for nothing; only a caught creature shrinks
	 */
	static double size(CatchOutcome outcome, int age)
	{
		if (outcome != CatchOutcome.CAUGHT)
			return 1.0;

		return 1.0 - progress(age, ACT_TICK, CAUGHT_END);
	}

	/**
	 * Gives how solid the copy is.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return 1 for solid down to 0 for unseen; only a fleeing creature fades, while it runs
	 */
	static double opacity(CatchOutcome outcome, int age)
	{
		if (outcome != CatchOutcome.FLED)
			return 1.0;

		return 1.0 - progress(age, ACT_TICK, FLED_END);
	}

	/**
	 * Gives how far the copy has run.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return the distance in tiles, from 0 up to {@link #RUN_TILES}; only a fleeing creature runs
	 */
	static double runTiles(CatchOutcome outcome, int age)
	{
		if (outcome != CatchOutcome.FLED)
			return 0.0;

		return RUN_TILES * progress(age, ACT_TICK, FLED_END);
	}

	private static double progress(int age, int from, int to)
	{
		return Math.max(0.0, Math.min(1.0, (age - from) / (double) (to - from)));
	}
}
