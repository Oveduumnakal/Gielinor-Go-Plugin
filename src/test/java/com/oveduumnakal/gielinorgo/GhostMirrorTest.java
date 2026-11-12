/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the see-through maths, the animation choice and the stand-in size. */
public class GhostMirrorTest
{
	@Test
	public void aSolidFaceBecomesNinetyPercentSeeThrough()
	{
		assertEquals(229, GhostMirror.fade(0, GhostMirror.OPACITY));
	}

	@Test
	public void anUnseenFaceStaysUnseen()
	{
		assertEquals(255, GhostMirror.fade(255, GhostMirror.OPACITY));
	}

	@Test
	public void aHalfSeeThroughFaceBecomesFainter()
	{
		assertEquals(255 - Math.round(127 * 0.10f), GhostMirror.fade(128, GhostMirror.OPACITY));
	}

	@Test
	public void noFaceGetsMoreSolid()
	{
		for (int t = 0; t <= 255; t++)
			assertTrue(GhostMirror.fade(t, GhostMirror.OPACITY) >= t);
	}

	@Test
	public void fadeAllReadsBytesAsUnsigned()
	{
		byte[] faces = {0, (byte) 200, (byte) 255};

		GhostMirror.fadeAll(faces);

		assertEquals(229, faces[0] & 0xFF);
		assertEquals(249, faces[1] & 0xFF);
		assertEquals(255, faces[2] & 0xFF);
		GhostMirror.fadeAll(null);
	}

	@Test
	public void theActionAnimationWinsOverThePose()
	{
		GhostMirror.Pick pick = GhostMirror.pick(422, 3, 808, 7);

		assertEquals(422, pick.animation());
		assertEquals(3, pick.frame());
	}

	@Test
	public void thePoseShowsWhenThereIsNoAction()
	{
		GhostMirror.Pick pick = GhostMirror.pick(-1, 0, 808, 7);

		assertEquals(808, pick.animation());
		assertEquals(7, pick.frame());
	}

	@Test
	public void noAnimationAtAllStandsStill()
	{
		assertEquals(-1, GhostMirror.pick(-1, 5, -1, 6).animation());
	}

	@Test
	public void theAnimationOnlyChangesWithTheId()
	{
		assertFalse(GhostMirror.animationChanged(808, true, 808));
		assertTrue(GhostMirror.animationChanged(808, true, 422));
		assertTrue(GhostMirror.animationChanged(808, true, -1));
		assertTrue(GhostMirror.animationChanged(808, false, 808));
		assertFalse(GhostMirror.animationChanged(-1, false, -1));
	}

	@Test
	public void aFrameOutOfStepIsFixed()
	{
		assertFalse(GhostMirror.drifted(4, 4));
		assertTrue(GhostMirror.drifted(4, 5));
	}

	@Test
	public void theStandInIsThreeQuartersOfTheRealSize()
	{
		assertEquals(96, BattleStage.shrunk(128));
		assertEquals(192, BattleStage.shrunk(256));
		assertEquals(1, BattleStage.shrunk(0));
	}
}
