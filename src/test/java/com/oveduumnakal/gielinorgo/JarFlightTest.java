/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the jar's flight from the player's hand to the creature. */
public class JarFlightTest
{
	@Test
	public void theFlightStartsAtTheReleaseAndEndsAtTheLanding()
	{
		assertEquals(0.0, JarFlight.progress(JarFlight.RELEASE_TICK), 0);
		assertEquals(1.0, JarFlight.progress(CatchScene.LAND_TICK), 0);
		assertEquals(0.5, JarFlight.progress((JarFlight.RELEASE_TICK + CatchScene.LAND_TICK) / 2), 0.001);
	}

	@Test
	public void theJarIsAtThePlayerAtTheReleaseAndAtTheCreatureAtTheLanding()
	{
		assertEquals(10.0, JarFlight.between(10, 90, JarFlight.progress(JarFlight.RELEASE_TICK)), 0);
		assertEquals(90.0, JarFlight.between(10, 90, JarFlight.progress(CatchScene.LAND_TICK)), 0);
		assertEquals(100.0, JarFlight.height(100, 40, 0.0), 0);
		assertEquals(40.0, JarFlight.height(100, 40, 1.0), 0);
	}

	@Test
	public void theArcPeaksInTheMiddleByOneTile()
	{
		assertEquals(0.0, JarFlight.lift(0.0), 0);
		assertEquals(0.0, JarFlight.lift(1.0), 0);
		assertEquals(1.0, JarFlight.lift(0.5), 0);
		assertTrue(JarFlight.lift(0.25) < JarFlight.lift(0.5));
		assertEquals(70.0 + JarFlight.ARC_HEIGHT, JarFlight.height(100, 40, 0.5), 0);
		assertEquals(128, JarFlight.ARC_HEIGHT);
	}

	@Test
	public void theJarIsOnlyDrawnWhileItIsInTheAir()
	{
		assertFalse(JarFlight.flying(0));
		assertFalse(JarFlight.flying(JarFlight.RELEASE_TICK - 1));
		assertTrue(JarFlight.flying(JarFlight.RELEASE_TICK));
		assertTrue(JarFlight.flying(CatchScene.LAND_TICK - 1));
		assertFalse(JarFlight.flying(CatchScene.LAND_TICK));
		assertFalse(JarFlight.flying(CatchScene.REACT_TICK));
	}

	@Test
	public void progressIsHeldAtTheEndsOutsideTheFlight()
	{
		assertEquals(0.0, JarFlight.progress(0), 0);
		assertEquals(1.0, JarFlight.progress(CatchScene.REACT_TICK), 0);
	}
}
