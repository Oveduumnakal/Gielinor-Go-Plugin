/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks which parts of a battle fade, and how long each fade takes. */
public class BattleFadesTest
{
	private static void run(BattleFades fades, boolean onHome, int ticks)
	{
		for (int i = 0; i < ticks; i++)
			fades.tick(onHome);
	}

	@Test
	public void theDurationsAreOneAndThreeGameTicks()
	{
		assertEquals(30, BattleFades.START_FADE_CLIENT_TICKS);
		assertEquals(90, BattleFades.FADE_CLIENT_TICKS);
	}

	@Test
	public void theStartHasTheGhostSolidAndTheStandInUnseen()
	{
		BattleFades fades = new BattleFades(true);
		assertEquals(1.0, fades.ghostOpacity(), 0.0);
		assertEquals(0.0, fades.standInOpacity(), 0.0);
		assertEquals(BattleFades.Stage.STARTING, fades.stage());
	}

	@Test
	public void theStartCrossfadeTakesOneGameTickOnTheStartTile()
	{
		BattleFades fades = new BattleFades(true);
		run(fades, true, 29);
		assertEquals(BattleFades.Stage.STARTING, fades.stage());
		assertTrue(fades.ghostOpacity() > 0.0);
		assertTrue(fades.standInOpacity() < 1.0);
		run(fades, true, 1);
		assertEquals(BattleFades.Stage.FIGHTING, fades.stage());
		assertEquals(0.0, fades.ghostOpacity(), 0.0);
		assertEquals(1.0, fades.standInOpacity(), 0.0);
	}

	@Test
	public void theGhostRestsAtTenPercentOffTheStartTile()
	{
		BattleFades fades = new BattleFades(false);
		run(fades, false, BattleFades.START_FADE_CLIENT_TICKS);
		assertEquals(GhostMirror.OPACITY, fades.ghostOpacity(), 1e-12);
		assertEquals(BattleFades.Stage.FIGHTING, fades.stage());
	}

	@Test
	public void leavingTheTileFadesTheGhostInOverThreeGameTicks()
	{
		BattleFades fades = new BattleFades(true);
		run(fades, true, BattleFades.START_FADE_CLIENT_TICKS);
		run(fades, false, BattleFades.FADE_CLIENT_TICKS - 1);
		assertTrue(fades.ghostOpacity() < GhostMirror.OPACITY);
		assertTrue(fades.ghostOpacity() > 0.0);
		run(fades, false, 1);
		assertEquals(GhostMirror.OPACITY, fades.ghostOpacity(), 1e-12);
	}

	@Test
	public void changingTheTargetMidFadeContinuesFromTheCurrentOpacity()
	{
		BattleFades fades = new BattleFades(true);
		run(fades, true, BattleFades.START_FADE_CLIENT_TICKS);
		run(fades, false, 45);
		double half = fades.ghostOpacity();
		assertEquals(GhostMirror.OPACITY / 2, half, 1e-9);
		run(fades, true, 1);
		assertEquals(half - GhostMirror.OPACITY / BattleFades.FADE_CLIENT_TICKS, fades.ghostOpacity(), 1e-12);
		run(fades, true, BattleFades.FADE_CLIENT_TICKS);
		assertEquals(0.0, fades.ghostOpacity(), 0.0);
	}

	@Test
	public void theStartTileIsWhereTheGhostRestsAtNothing()
	{
		assertEquals(0.0, BattleFades.resting(true), 0.0);
		assertEquals(GhostMirror.OPACITY, BattleFades.resting(false), 0.0);
	}

	@Test
	public void theEndCrossfadeTakesThreeGameTicks()
	{
		BattleFades fades = new BattleFades(false);
		run(fades, false, BattleFades.START_FADE_CLIENT_TICKS);
		run(fades, false, 10);
		fades.beginEnd();
		assertTrue(fades.ending());
		assertFalse(fades.finished());
		run(fades, false, BattleFades.FADE_CLIENT_TICKS - 1);
		assertFalse(fades.finished());
		assertTrue(fades.ghostOpacity() < 1.0);
		assertTrue(fades.standInOpacity() > 0.0);
		run(fades, false, 1);
		assertTrue(fades.finished());
		assertEquals(1.0, fades.ghostOpacity(), 0.0);
		assertEquals(0.0, fades.standInOpacity(), 0.0);
	}

	@Test
	public void theEndFadeStartsFromACurrentGhostOpacityOfZero()
	{
		BattleFades fades = new BattleFades(true);
		run(fades, true, BattleFades.START_FADE_CLIENT_TICKS + 5);
		assertEquals(0.0, fades.ghostOpacity(), 0.0);
		fades.beginEnd();
		run(fades, true, 45);
		assertEquals(0.5, fades.ghostOpacity(), 1e-9);
		assertEquals(0.5, fades.standInOpacity(), 1e-9);
	}

	@Test
	public void theEndCanBeginDuringTheStartFade()
	{
		BattleFades fades = new BattleFades(true);
		run(fades, true, 10);
		fades.beginEnd();
		run(fades, true, BattleFades.FADE_CLIENT_TICKS);
		assertTrue(fades.finished());
		assertEquals(1.0, fades.ghostOpacity(), 0.0);
	}
}
