/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import net.runelite.api.gameval.AnimationID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests for the timing and the emotes of a catch. */
public class CatchSceneTest
{
	@Test
	public void theThrowGoesThroughItsPhases()
	{
		assertEquals(CatchScene.Phase.THROWING, CatchScene.phaseAt(0));
		assertEquals(CatchScene.Phase.THROWING, CatchScene.phaseAt(39));
		assertEquals(CatchScene.Phase.WAITING, CatchScene.phaseAt(40));
		assertEquals(CatchScene.Phase.WAITING, CatchScene.phaseAt(83));
		assertEquals(CatchScene.Phase.REACTING, CatchScene.phaseAt(84));
		assertEquals(CatchScene.Phase.DONE, CatchScene.phaseAt(85));
		assertEquals(CatchScene.Phase.DONE, CatchScene.phaseAt(500));
	}

	@Test
	public void theJarLandsAtTickForty()
	{
		assertFalse(CatchScene.landed(39));
		assertTrue(CatchScene.landed(40));
	}

	@Test
	public void eachResultHasItsOwnEmote()
	{
		assertEquals(CatchScene.CHEER, CatchScene.reactionFor(CatchOutcome.CAUGHT));
		assertEquals(CatchScene.ANGRY, CatchScene.reactionFor(CatchOutcome.BROKE_FREE));
		assertEquals(CatchScene.CRY, CatchScene.reactionFor(CatchOutcome.FLED));
	}

	@Test
	public void noJarThrownMeansNoReaction()
	{
		assertEquals(CatchScene.NONE, CatchScene.reactionFor(CatchOutcome.NO_JARS));
		assertEquals(CatchScene.NONE, CatchScene.reactionFor(CatchOutcome.FULL));
	}

	@Test
	public void theReactionComesAfterTheThrowEnds()
	{
		assertTrue(CatchScene.REACT_TICK > CatchScene.THROW_TICKS);
		assertTrue(CatchScene.LAND_TICK < CatchScene.REACT_TICK);
	}

	@Test
	public void theNumbersMatchRuneLitesAnimationNames()
	{
		assertEquals(AnimationID.EMOTE_CHEER, CatchScene.CHEER);
		assertEquals(AnimationID.EMOTE_ANGRY, CatchScene.ANGRY);
		assertEquals(AnimationID.EMOTE_CRY, CatchScene.CRY);
	}
}
