/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * What the stand-in of a battle that is on stage does when a jar is thrown at it, counted in client ticks (20 ms
 * each) from the throw. It reuses the timing of {@link TargetScript}, with two differences that come from the
 * stand-in being a body that may be lying knocked out: a lying creature does not get up to flinch (it stays down
 * until its second act), and one that breaks free or flees gets up first. A caught creature shrinks to nothing and
 * the stage is cut off without its fade; one that flees fades out and the stage is cut off; one that breaks free
 * attacks and the stage then ends with its normal fade. Holds no RuneLite types, so it can be tested tick by tick.
 */
final class StageCatch
{
	private final CatchOutcome outcome;
	private final boolean lying;
	private int age;

	/**
	 * Starts the scene at the throw.
	 *
	 * @param outcome what the throw led to
	 * @param lying whether the stand-in lies knocked out at the throw
	 */
	StageCatch(CatchOutcome outcome, boolean lying)
	{
		this.outcome = outcome;
		this.lying = lying;
	}

	/**
	 * Tells whether a result is played on the stand-in at all.
	 *
	 * @param outcome what the throw led to
	 * @return false for a result that throws nothing
	 */
	static boolean plays(CatchOutcome outcome)
	{
		return TargetScript.end(outcome) != 0;
	}

	/**
	 * Tells whether the real monster must stay hidden after the stage is cut off: a caught creature for its five
	 * seconds and then the fade back in, a creature that fled until it fades back in. One that broke free is shown
	 * by the stage's own fade.
	 *
	 * @param outcome what the throw led to
	 * @return whether {@link CatchEffects} hides and fades the real monster back after the landing
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

	/** @return whether the stand-in lay knocked out at the throw */
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
	 * Tells whether a lying stand-in gets up on this tick.
	 *
	 * @return true when its second act starts, it was lying and it breaks free or flees
	 */
	boolean getsUp()
	{
		return lying && age == TargetScript.ACT_TICK && TargetScript.turnOf(outcome) != TargetScript.Turn.NONE;
	}

	/** @return how the stand-in turns on this tick, or none: it turns when its second act starts */
	TargetScript.Turn turn()
	{
		return age == TargetScript.ACT_TICK ? TargetScript.turnOf(outcome) : TargetScript.Turn.NONE;
	}

	/** @return how big the stand-in is drawn, from 1 down to 0; only a caught creature shrinks */
	double size()
	{
		return TargetScript.size(outcome, age);
	}

	/** @return how solid the stand-in is, from 1 down to 0; only a fleeing creature fades */
	double opacity()
	{
		return TargetScript.opacity(outcome, age);
	}

	/** @return how far the stand-in has run, in tiles; only a fleeing creature runs */
	double runTiles()
	{
		return TargetScript.runTiles(outcome, age);
	}

	/**
	 * Tells whether the stage must be cut off now, with no fade, because the stand-in is already gone: a caught
	 * creature has shrunk to nothing, a fled one has faded out.
	 *
	 * @return true from the end of the script for a caught or a fled creature
	 */
	boolean cutsStage()
	{
		return leavesHidden(outcome) && age >= TargetScript.end(outcome);
	}

	/**
	 * Tells whether the stage starts its normal fade back to the real monster on this tick.
	 *
	 * @return true on the tick a creature that broke free is done
	 */
	boolean fadesStage()
	{
		return outcome == CatchOutcome.BROKE_FREE && age == TargetScript.end(outcome);
	}
}
