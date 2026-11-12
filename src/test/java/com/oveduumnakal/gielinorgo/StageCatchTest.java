/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks what the stand-in does when a jar lands on it, for a lying and a standing start. */
public class StageCatchTest
{
	private static StageCatch at(CatchOutcome outcome, boolean lying, int age)
	{
		StageCatch scene = new StageCatch(outcome, lying);
		for (int i = 0; i < age; i++)
			scene.tick();

		return scene;
	}

	@Test
	public void aStandingCreatureFlinchesAtTheLanding()
	{
		for (CatchOutcome outcome : new CatchOutcome[] {CatchOutcome.CAUGHT, CatchOutcome.BROKE_FREE,
			CatchOutcome.FLED})
			assertEquals(TargetScript.Step.HIT, at(outcome, false, TargetScript.LAND).step());
	}

	@Test
	public void aLyingCreatureDoesNotFlinch()
	{
		for (CatchOutcome outcome : new CatchOutcome[] {CatchOutcome.CAUGHT, CatchOutcome.BROKE_FREE,
			CatchOutcome.FLED})
			for (int age = 0; age <= TargetScript.CAUGHT_END; age++)
				assertFalse(outcome + " " + age, at(outcome, true, age).step() == TargetScript.Step.HIT);
	}

	@Test
	public void aLyingCreatureThatBreaksFreeGetsUpAndAttacksOnItsSecondAct()
	{
		StageCatch scene = at(CatchOutcome.BROKE_FREE, true, TargetScript.ACT_TICK);
		assertTrue(scene.getsUp());
		assertEquals(TargetScript.Step.ATTACK, scene.step());
		assertEquals(TargetScript.Turn.TO_PLAYER, scene.turn());
		assertFalse(at(CatchOutcome.BROKE_FREE, true, TargetScript.ACT_TICK - 1).getsUp());
		assertFalse(at(CatchOutcome.BROKE_FREE, true, TargetScript.ACT_TICK + 1).getsUp());
	}

	@Test
	public void aLyingCreatureThatFleesGetsUpAndTurnsAway()
	{
		StageCatch scene = at(CatchOutcome.FLED, true, TargetScript.ACT_TICK);
		assertTrue(scene.getsUp());
		assertEquals(TargetScript.Turn.AWAY, scene.turn());
	}

	@Test
	public void aStandingCreatureNeverNeedsToGetUp()
	{
		for (CatchOutcome outcome : new CatchOutcome[] {CatchOutcome.CAUGHT, CatchOutcome.BROKE_FREE,
			CatchOutcome.FLED})
			for (int age = 0; age <= TargetScript.FLED_END; age++)
				assertFalse(at(outcome, false, age).getsUp());
	}

	@Test
	public void aLyingCaughtCreatureStaysDownAndShrinks()
	{
		for (int age = 0; age <= TargetScript.CAUGHT_END; age++)
		{
			StageCatch scene = at(CatchOutcome.CAUGHT, true, age);
			assertFalse(scene.getsUp());
			assertEquals(TargetScript.Step.NONE, scene.step());
			assertEquals(TargetScript.Turn.NONE, scene.turn());
		}

		assertEquals(1.0, at(CatchOutcome.CAUGHT, true, TargetScript.ACT_TICK).size(), 0.0);
		assertEquals(0.0, at(CatchOutcome.CAUGHT, true, TargetScript.CAUGHT_END).size(), 0.0);
	}

	@Test
	public void theStageIsCutOffWithoutAFadeForACaughtAndAFledCreature()
	{
		assertFalse(at(CatchOutcome.CAUGHT, true, TargetScript.CAUGHT_END - 1).cutsStage());
		assertTrue(at(CatchOutcome.CAUGHT, true, TargetScript.CAUGHT_END).cutsStage());
		assertFalse(at(CatchOutcome.FLED, false, TargetScript.FLED_END - 1).cutsStage());
		assertTrue(at(CatchOutcome.FLED, false, TargetScript.FLED_END).cutsStage());
		assertEquals(0.0, at(CatchOutcome.FLED, false, TargetScript.FLED_END).opacity(), 0.0);
		assertEquals(TargetScript.RUN_TILES, at(CatchOutcome.FLED, false, TargetScript.FLED_END).runTiles(), 0.0);
		assertFalse(at(CatchOutcome.CAUGHT, false, TargetScript.CAUGHT_END).fadesStage());
		assertFalse(at(CatchOutcome.FLED, false, TargetScript.FLED_END).fadesStage());
	}

	@Test
	public void aCreatureThatBrokeFreeEndsWithTheStagesOwnFadeOnce()
	{
		for (int age = 0; age < TargetScript.BROKE_FREE_END + 5; age++)
		{
			StageCatch scene = at(CatchOutcome.BROKE_FREE, false, age);
			assertFalse(scene.cutsStage());
			assertEquals(age == TargetScript.BROKE_FREE_END, scene.fadesStage());
		}
	}

	@Test
	public void onlyCaughtAndFledCreaturesStayHiddenAfterTheStage()
	{
		assertTrue(StageCatch.leavesHidden(CatchOutcome.CAUGHT));
		assertTrue(StageCatch.leavesHidden(CatchOutcome.FLED));
		assertFalse(StageCatch.leavesHidden(CatchOutcome.BROKE_FREE));
		assertFalse(StageCatch.leavesHidden(CatchOutcome.NO_JARS));
		assertFalse(StageCatch.leavesHidden(CatchOutcome.FULL));
	}

	@Test
	public void aResultThatThrowsNothingIsNotPlayed()
	{
		assertFalse(StageCatch.plays(CatchOutcome.NO_JARS));
		assertFalse(StageCatch.plays(CatchOutcome.FULL));
		assertTrue(StageCatch.plays(CatchOutcome.CAUGHT));
		assertTrue(StageCatch.plays(CatchOutcome.BROKE_FREE));
		assertTrue(StageCatch.plays(CatchOutcome.FLED));
	}

	@Test
	public void theTotalHiddenTimeAfterTheLandingIsTheUsualOne()
	{
		int age = TargetScript.LAND + CatchEffects.HIDDEN_TICKS;
		assertEquals(CatchEffects.HIDDEN, CatchEffects.shadeAt(CatchOutcome.CAUGHT, age));
		assertTrue(CatchEffects.shadeAt(CatchOutcome.CAUGHT, age + 1) >= 0);
		assertTrue(at(CatchOutcome.CAUGHT, true, 0).startedLying());
	}
}
