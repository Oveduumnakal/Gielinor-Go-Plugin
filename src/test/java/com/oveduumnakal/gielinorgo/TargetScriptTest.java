/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the timing of what the creature does when a jar lands on it. */
public class TargetScriptTest
{
	private static final CatchOutcome[] REACTING =
		{CatchOutcome.CAUGHT, CatchOutcome.BROKE_FREE, CatchOutcome.FLED};

	@Test
	public void nothingIsHiddenBeforeTheJarLands()
	{
		for (CatchOutcome outcome : REACTING)
		{
			for (int age = 0; age < TargetScript.LAND; age++)
			{
				assertFalse(outcome + " " + age, TargetScript.hidden(outcome, age));
				assertFalse(outcome + " " + age, TargetScript.copyShown(outcome, age));
				assertEquals(TargetScript.Step.NONE, TargetScript.stepAt(outcome, age));
			}

			assertTrue(outcome.toString(), TargetScript.hidden(outcome, TargetScript.LAND));
			assertTrue(outcome.toString(), TargetScript.copyShown(outcome, TargetScript.LAND));
		}
	}

	@Test
	public void theCreatureFlinchesAtTheLanding()
	{
		for (CatchOutcome outcome : REACTING)
			assertEquals(TargetScript.Step.HIT, TargetScript.stepAt(outcome, TargetScript.LAND));
	}

	@Test
	public void theSecondActIsTheRightOne()
	{
		int at = TargetScript.LAND + 20;
		assertEquals(TargetScript.ACT_TICK, at);
		assertEquals(TargetScript.Step.ATTACK, TargetScript.stepAt(CatchOutcome.BROKE_FREE, at));
		assertEquals(TargetScript.Step.NONE, TargetScript.stepAt(CatchOutcome.CAUGHT, at));
		assertEquals(TargetScript.Step.NONE, TargetScript.stepAt(CatchOutcome.FLED, at));
		assertEquals(TargetScript.Turn.TO_PLAYER, TargetScript.turnOf(CatchOutcome.BROKE_FREE));
		assertEquals(TargetScript.Turn.AWAY, TargetScript.turnOf(CatchOutcome.FLED));
		assertEquals(TargetScript.Turn.NONE, TargetScript.turnOf(CatchOutcome.CAUGHT));
		assertEquals(TargetScript.Step.NONE, TargetScript.stepAt(CatchOutcome.BROKE_FREE, at + 1));
	}

	@Test
	public void aCaughtCreatureShrinksFromOneToNothingWithoutGrowing()
	{
		assertEquals(1.0, TargetScript.size(CatchOutcome.CAUGHT, TargetScript.LAND), 0.0);
		assertEquals(1.0, TargetScript.size(CatchOutcome.CAUGHT, TargetScript.ACT_TICK), 0.0);
		assertEquals(0.0, TargetScript.size(CatchOutcome.CAUGHT, TargetScript.CAUGHT_END), 0.0);
		double last = 1.0;
		for (int age = 0; age <= TargetScript.CAUGHT_END + 10; age++)
		{
			double size = TargetScript.size(CatchOutcome.CAUGHT, age);
			assertTrue("age " + age, size <= last && size >= 0.0);
			last = size;
		}

		assertEquals(25, TargetScript.SHRINK_TICKS);
		assertEquals(TargetScript.LAND + 45, TargetScript.CAUGHT_END);
	}

	@Test
	public void aCaughtCreatureHandsOverToTheVanishAtTheEnd()
	{
		assertTrue(TargetScript.hidden(CatchOutcome.CAUGHT, TargetScript.CAUGHT_END - 1));
		assertFalse(TargetScript.hidden(CatchOutcome.CAUGHT, TargetScript.CAUGHT_END));
		assertTrue(TargetScript.done(CatchOutcome.CAUGHT, TargetScript.CAUGHT_END));
		assertFalse(TargetScript.copyShown(CatchOutcome.CAUGHT, TargetScript.CAUGHT_END));
		assertEquals(CatchEffects.HIDDEN, CatchEffects.shadeAt(CatchOutcome.CAUGHT, TargetScript.CAUGHT_END));
		assertEquals(CatchEffects.shadeAt(TargetScript.LAND + 100),
			CatchEffects.shadeAt(CatchOutcome.CAUGHT, TargetScript.LAND + 100));
		assertEquals(CatchEffects.DONE, CatchEffects.shadeAt(CatchOutcome.CAUGHT,
			TargetScript.LAND + CatchEffects.HIDDEN_TICKS + CatchEffects.FADE_TICKS + 1));
	}

	@Test
	public void aFleeingCreatureFadesWithoutRisingAndRunsThreeTiles()
	{
		assertEquals(1.0, TargetScript.opacity(CatchOutcome.FLED, TargetScript.ACT_TICK), 0.0);
		assertEquals(0.0, TargetScript.opacity(CatchOutcome.FLED, TargetScript.FLED_END), 0.0);
		assertEquals(0.0, TargetScript.runTiles(CatchOutcome.FLED, TargetScript.ACT_TICK), 0.0);
		assertEquals(3.0, TargetScript.runTiles(CatchOutcome.FLED, TargetScript.FLED_END), 1e-9);
		assertEquals(3.0, TargetScript.runTiles(CatchOutcome.FLED, TargetScript.FLED_END + 50), 1e-9);
		double opacity = 1.0;
		double run = 0.0;
		for (int age = 0; age <= TargetScript.FLED_END + 10; age++)
		{
			double now = TargetScript.opacity(CatchOutcome.FLED, age);
			double ran = TargetScript.runTiles(CatchOutcome.FLED, age);
			assertTrue("age " + age, now <= opacity && ran >= run && ran <= 3.0);
			opacity = now;
			run = ran;
		}

		assertEquals(TargetScript.LAND + 80, TargetScript.FLED_END);
	}

	@Test
	public void aFleeingCreatureIsHiddenUntilTheRealOneFadesIn()
	{
		CatchOutcome fled = CatchOutcome.FLED;
		assertEquals(CatchEffects.VISIBLE, CatchEffects.shadeAt(fled, TargetScript.LAND - 1));
		assertEquals(CatchEffects.HIDDEN, CatchEffects.shadeAt(fled, TargetScript.LAND));
		assertEquals(CatchEffects.HIDDEN, CatchEffects.shadeAt(fled, TargetScript.FLED_END - 1));
		assertEquals(0, CatchEffects.shadeAt(fled, TargetScript.FLED_END));
		int last = 0;
		for (int age = TargetScript.FLED_END; age < TargetScript.FLED_END + CatchEffects.FADE_TICKS; age++)
		{
			int shade = CatchEffects.shadeAt(fled, age);
			assertTrue("age " + age, shade >= last && shade < CatchEffects.FADE_STEPS);
			last = shade;
		}

		assertEquals(CatchEffects.FADE_STEPS - 1, last);
		assertEquals(CatchEffects.DONE,
			CatchEffects.shadeAt(fled, TargetScript.FLED_END + CatchEffects.FADE_TICKS));
	}

	@Test
	public void aCreatureThatBrokeFreeComesBackAtTheEnd()
	{
		CatchOutcome broke = CatchOutcome.BROKE_FREE;
		assertEquals(TargetScript.LAND + 60, TargetScript.BROKE_FREE_END);
		assertEquals(CatchEffects.HIDDEN, CatchEffects.shadeAt(broke, TargetScript.BROKE_FREE_END - 1));
		assertEquals(CatchEffects.DONE, CatchEffects.shadeAt(broke, TargetScript.BROKE_FREE_END));
		assertEquals(1.0, TargetScript.size(broke, TargetScript.ACT_TICK + 10), 0.0);
		assertEquals(1.0, TargetScript.opacity(broke, TargetScript.ACT_TICK + 10), 0.0);
	}

	@Test
	public void everyScriptReachesDone()
	{
		for (CatchOutcome outcome : REACTING)
		{
			assertFalse(outcome.toString(), TargetScript.done(outcome, TargetScript.LAND));
			assertTrue(outcome.toString(), TargetScript.done(outcome, TargetScript.end(outcome)));
			assertTrue(outcome.toString(), TargetScript.done(outcome, 100000));
			assertFalse(outcome.toString(), TargetScript.hidden(outcome, 100000));
		}
	}

	@Test
	public void throwsThatNeverHitHideNothing()
	{
		for (CatchOutcome outcome : new CatchOutcome[] {CatchOutcome.NO_JARS, CatchOutcome.FULL})
		{
			for (int age = 0; age < 400; age++)
			{
				assertFalse(TargetScript.hidden(outcome, age));
				assertFalse(TargetScript.copyShown(outcome, age));
				assertEquals(TargetScript.Step.NONE, TargetScript.stepAt(outcome, age));
				assertEquals(CatchEffects.VISIBLE, CatchEffects.shadeAt(outcome, age));
			}

			assertTrue(TargetScript.done(outcome, 0));
		}
	}
}
