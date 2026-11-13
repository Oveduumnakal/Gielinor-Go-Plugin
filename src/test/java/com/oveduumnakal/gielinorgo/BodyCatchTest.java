/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks what the body does when a jar lands on it, for a lying and a standing start. */
public class BodyCatchTest
{
	private static BodyCatch at(CatchOutcome outcome, boolean lying, int age)
	{
		BodyCatch scene = new BodyCatch(outcome, lying);
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
		BodyCatch scene = at(CatchOutcome.BROKE_FREE, true, TargetScript.ACT_TICK);
		assertTrue(scene.getsUp());
		assertEquals(TargetScript.Step.ATTACK, scene.step());
		assertEquals(TargetScript.Turn.TO_PLAYER, scene.turn());
		assertFalse(at(CatchOutcome.BROKE_FREE, true, TargetScript.ACT_TICK - 1).getsUp());
		assertFalse(at(CatchOutcome.BROKE_FREE, true, TargetScript.ACT_TICK + 1).getsUp());
	}

	@Test
	public void aLyingCreatureThatFleesGetsUpAndTurnsAway()
	{
		BodyCatch scene = at(CatchOutcome.FLED, true, TargetScript.ACT_TICK);
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
			BodyCatch scene = at(CatchOutcome.CAUGHT, true, age);
			assertFalse(scene.getsUp());
			assertEquals(TargetScript.Step.NONE, scene.step());
			assertEquals(TargetScript.Turn.NONE, scene.turn());
		}

		assertEquals(1.0, at(CatchOutcome.CAUGHT, true, TargetScript.ACT_TICK).size(), 0.0);
		assertEquals(0.0, at(CatchOutcome.CAUGHT, true, TargetScript.CAUGHT_END).size(), 0.0);
	}

	@Test
	public void theBodyIsRemovedForACaughtAndAFledCreature()
	{
		assertFalse(at(CatchOutcome.CAUGHT, true, TargetScript.CAUGHT_END - 1).removesBody());
		assertTrue(at(CatchOutcome.CAUGHT, true, TargetScript.CAUGHT_END).removesBody());
		assertFalse(at(CatchOutcome.FLED, false, TargetScript.FLED_END - 1).removesBody());
		assertTrue(at(CatchOutcome.FLED, false, TargetScript.FLED_END).removesBody());
		assertEquals(0.0, at(CatchOutcome.FLED, false, TargetScript.FLED_END).opacity(), 0.0);
		assertEquals(TargetScript.RUN_TILES, at(CatchOutcome.FLED, false, TargetScript.FLED_END).runTiles(), 0.0);
		assertFalse(at(CatchOutcome.CAUGHT, false, TargetScript.CAUGHT_END).standsAgain());
		assertFalse(at(CatchOutcome.FLED, false, TargetScript.FLED_END).standsAgain());
	}

	@Test
	public void aCreatureThatBrokeFreeStandsAgainOnce()
	{
		for (int age = 0; age < TargetScript.BROKE_FREE_END + 5; age++)
		{
			BodyCatch scene = at(CatchOutcome.BROKE_FREE, false, age);
			assertFalse(scene.removesBody());
			assertEquals(age == TargetScript.BROKE_FREE_END, scene.standsAgain());
		}
	}

	@Test
	public void onlyCaughtAndFledCreaturesAreGoneAfterTheScene()
	{
		assertTrue(BodyCatch.leavesHidden(CatchOutcome.CAUGHT));
		assertTrue(BodyCatch.leavesHidden(CatchOutcome.FLED));
		assertFalse(BodyCatch.leavesHidden(CatchOutcome.BROKE_FREE));
		assertFalse(BodyCatch.leavesHidden(CatchOutcome.NO_JARS));
		assertFalse(BodyCatch.leavesHidden(CatchOutcome.FULL));
	}

	@Test
	public void aResultThatThrowsNothingIsNotPlayed()
	{
		assertFalse(BodyCatch.plays(CatchOutcome.NO_JARS));
		assertFalse(BodyCatch.plays(CatchOutcome.FULL));
		assertTrue(BodyCatch.plays(CatchOutcome.CAUGHT));
		assertTrue(BodyCatch.plays(CatchOutcome.BROKE_FREE));
		assertTrue(BodyCatch.plays(CatchOutcome.FLED));
	}

	@Test
	public void theSceneRemembersWhetherTheBodyLay()
	{
		assertTrue(at(CatchOutcome.CAUGHT, true, 0).startedLying());
		assertFalse(at(CatchOutcome.CAUGHT, false, 0).startedLying());
	}
}
