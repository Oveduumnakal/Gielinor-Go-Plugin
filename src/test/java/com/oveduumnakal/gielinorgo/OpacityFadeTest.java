/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the opacity levels and how an opacity moves toward a target. */
public class OpacityFadeTest
{
	@Test
	public void theExtremesAreHiddenAndSolid()
	{
		assertEquals(OpacityFade.HIDDEN, OpacityFade.levelOf(0.0));
		assertEquals(OpacityFade.SOLID, OpacityFade.levelOf(1.0));
		assertEquals(OpacityFade.SOLID, OpacityFade.levelOf(0.97));
	}

	@Test
	public void theGhostsRestingOpacityIsALevelOfItsOwn()
	{
		assertEquals(OpacityFade.FAINT, OpacityFade.opacityOf(OpacityFade.levelOf(OpacityFade.FAINT)), 0.0);
	}

	@Test
	public void thereAreAtMostADozenSeeThroughLevels()
	{
		assertEquals(12, OpacityFade.SOLID - OpacityFade.HIDDEN);
	}

	@Test
	public void aTinyOpacityIsStillHidden()
	{
		assertEquals(OpacityFade.HIDDEN, OpacityFade.levelOf(0.01));
		assertTrue(OpacityFade.levelOf(0.03) > OpacityFade.HIDDEN);
	}

	@Test
	public void movesAtTheSetRateAndStopsOnTheTarget()
	{
		OpacityFade fade = new OpacityFade(0.0);
		fade.moveAt(0.1, 0.1 / 90);
		for (int i = 0; i < 89; i++)
			fade.tick();

		assertFalse(fade.settled());
		assertEquals(0.1 - 0.1 / 90, fade.opacity(), 1e-9);
		fade.tick();
		fade.tick();
		assertTrue(fade.settled());
		assertEquals(0.1, fade.opacity(), 0.0);
		fade.tick();
		assertEquals(0.1, fade.opacity(), 0.0);
	}

	@Test
	public void aNewTargetContinuesFromTheCurrentOpacity()
	{
		OpacityFade fade = new OpacityFade(0.0);
		fade.moveAt(0.1, 0.1 / 90);
		for (int i = 0; i < 45; i++)
			fade.tick();

		double half = fade.opacity();
		assertEquals(0.05, half, 1e-9);
		fade.moveAt(0.0, 0.1 / 90);
		fade.tick();
		assertEquals(half - 0.1 / 90, fade.opacity(), 1e-12);
	}

	@Test
	public void moveToArrivesInTheGivenTicks()
	{
		OpacityFade fade = new OpacityFade(1.0);
		fade.moveTo(0.1, 30);
		for (int i = 0; i < 29; i++)
			fade.tick();

		assertFalse(fade.settled());
		fade.tick();
		assertTrue(fade.settled());
	}

	@Test
	public void opacityIsKeptBetweenZeroAndOne()
	{
		OpacityFade fade = new OpacityFade(5.0);
		assertEquals(1.0, fade.opacity(), 0.0);
		fade.moveTo(-2.0, 1);
		fade.tick();
		assertEquals(0.0, fade.opacity(), 0.0);
	}

	@Test
	public void aSolidFaceBecomesNinetyPercentSeeThrough()
	{
		assertEquals(229, OpacityFade.fade(0, OpacityFade.FAINT));
	}

	@Test
	public void anUnseenFaceStaysUnseen()
	{
		assertEquals(255, OpacityFade.fade(255, OpacityFade.FAINT));
	}

	@Test
	public void noFaceGetsMoreSolid()
	{
		for (int t = 0; t <= 255; t++)
			assertTrue(OpacityFade.fade(t, OpacityFade.FAINT) >= t);
	}

	@Test
	public void fadeAllReadsBytesAsUnsigned()
	{
		byte[] faces = {0, (byte) 200, (byte) 255};

		OpacityFade.fadeAll(faces, OpacityFade.FAINT);

		assertEquals(229, faces[0] & 0xFF);
		assertEquals(249, faces[1] & 0xFF);
		assertEquals(255, faces[2] & 0xFF);
		OpacityFade.fadeAll(null, OpacityFade.FAINT);
	}
}
