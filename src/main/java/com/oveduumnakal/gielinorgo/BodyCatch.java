/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * What the body of a wild creature does when a jar is thrown at it, counted in client ticks (20 ms each) from the
 * throw. It reuses the timing of {@link TargetScript}, with two differences that come from the body possibly lying
 * knocked out: a lying creature does not get up to flinch (it stays down until its second act), and one that breaks
 * free or flees gets up first. A caught creature shrinks to nothing and its body is removed; one that flees fades out
 * and its body is removed; one that breaks free attacks and then stands again. Holds no RuneLite types, so it can be
 * tested tick by tick.
 */
final class BodyCatch
{
	private final CatchOutcome outcome;
	private final boolean lying;
	private int age;

	/**
	 * Starts the scene at the throw.
	 *
	 * @param outcome what the throw led to
	 * @param lying whether the body lies knocked out at the throw
	 */
	BodyCatch(CatchOutcome outcome, boolean lying)
	{
		this.outcome = outcome;
		this.lying = lying;
	}

	/**
	 * Tells whether a result is played on the body at all.
	 *
	 * @param outcome what the throw led to
	 * @return false for a result that throws nothing
	 */
	static boolean plays(CatchOutcome outcome)
	{
		return TargetScript.end(outcome) != 0;
	}

	/**
	 * Tells whether the creature is gone for good when the scene ends: a caught creature has shrunk away and one
	 * that fled has run off. One that broke free is still there.
	 *
	 * @param outcome what the throw led to
	 * @return whether the body is removed at the end of the scene
	 */
	static boolean leavesHidden(CatchOutcome outcome)
	{
		return outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED;
	}

	/** Moves the scene on by one client tick. */
	void tick()
	{
		age++;
	}

	/** @return client ticks since the throw */
	int age()
	{
		return age;
	}

	/** @return what the throw led to */
	CatchOutcome outcome()
	{
		return outcome;
	}

	/** @return whether the body lay knocked out at the throw */
	boolean startedLying()
	{
		return lying;
	}

	/**
	 * Tells which act starts on this tick. A lying creature does not flinch.
	 *
	 * @return the flinch at the landing for a standing creature, the attack of one that breaks free, else none
	 */
	TargetScript.Step step()
	{
		TargetScript.Step step = TargetScript.stepAt(outcome, age);
		return step == TargetScript.Step.HIT && lying ? TargetScript.Step.NONE : step;
	}

	/**
	 * Tells whether a lying body gets up on this tick.
	 *
	 * @return true when its second act starts, it was lying and it breaks free or flees
	 */
	boolean getsUp()
	{
		return lying && age == TargetScript.ACT_TICK && TargetScript.turnOf(outcome) != TargetScript.Turn.NONE;
	}

	/** @return how the body turns on this tick, or none: it turns when its second act starts */
	TargetScript.Turn turn()
	{
		return age == TargetScript.ACT_TICK ? TargetScript.turnOf(outcome) : TargetScript.Turn.NONE;
	}

	/** @return how big the body is drawn, from 1 down to 0; only a caught creature shrinks */
	double size()
	{
		return TargetScript.size(outcome, age);
	}

	/** @return how solid the body is, from 1 down to 0; only a fleeing creature fades */
	double opacity()
	{
		return TargetScript.opacity(outcome, age);
	}

	/** @return how far the body has run, in tiles; only a fleeing creature runs */
	double runTiles()
	{
		return TargetScript.runTiles(outcome, age);
	}

	/**
	 * Tells whether the body must be removed now, because it is already gone: a caught creature has shrunk to
	 * nothing, a fled one has faded out.
	 *
	 * @return true from the end of the script for a caught or a fled creature
	 */
	boolean removesBody()
	{
		return leavesHidden(outcome) && age >= TargetScript.end(outcome);
	}

	/**
	 * Tells whether the stage starts its normal fade back to the real monster on this tick.
	 *
	 * @return true on the tick a creature that broke free is done
	 */
	boolean standsAgain()
	{
		return outcome == CatchOutcome.BROKE_FREE && age == TargetScript.end(outcome);
	}
}
