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
		assertEquals(0.0, JarFlight.progress(20, 20), 0);
		assertEquals(1.0, JarFlight.progress(40, 20), 0);
		assertEquals(0.5, JarFlight.progress(30, 20), 0.001);
		assertEquals(1.0, JarFlight.progress(60, 40), 0);
		assertEquals(0.5, JarFlight.progress(40, 40), 0.001);
	}

	@Test
	public void theJarIsAtThePlayerAtTheReleaseAndAtTheCreatureAtTheLanding()
	{
		assertEquals(10.0, JarFlight.between(10, 90, 0.0), 0);
		assertEquals(90.0, JarFlight.between(10, 90, 1.0), 0);
		assertEquals(100.0, JarFlight.height(100, 40, 0.0, 128), 0);
		assertEquals(40.0, JarFlight.height(100, 40, 1.0, 128), 0);
	}

	@Test
	public void theArcPeaksInTheMiddle()
	{
		assertEquals(0.0, JarFlight.lift(0.0), 0);
		assertEquals(0.0, JarFlight.lift(1.0), 0);
		assertEquals(1.0, JarFlight.lift(0.5), 0);
		assertTrue(JarFlight.lift(0.25) < JarFlight.lift(0.5));
		assertEquals(70.0 + 384, JarFlight.height(100, 40, 0.5, 384), 0);
	}

	@Test
	public void aShortThrowKeepsTheOldTimingAndArc()
	{
		assertEquals(20, JarFlight.flightTicks(0));
		assertEquals(20, JarFlight.flightTicks(4.5));
		assertEquals(0, JarFlight.delay(6.5));
		assertEquals(128, JarFlight.arc(4.5));
		assertEquals(CatchScene.LAND_TICK, JarFlight.RELEASE_TICK + JarFlight.flightTicks(1));
	}

	@Test
	public void aLongThrowTakesLongerAndArcsHigherUpToALimit()
	{
		assertEquals(30, JarFlight.flightTicks(10));
		assertEquals(10, JarFlight.delay(10));
		assertEquals(280, JarFlight.arc(10));
		double diagonal = Math.hypot(10, 10);
		assertEquals(40, JarFlight.flightTicks(diagonal));
		assertEquals(384, JarFlight.arc(diagonal));
		assertEquals(40, JarFlight.flightTicks(100));
		assertEquals(384, JarFlight.arc(100));
	}

	@Test
	public void theJarIsOnlyDrawnWhileItIsInTheAir()
	{
		assertFalse(JarFlight.flying(0, 20));
		assertFalse(JarFlight.flying(19, 20));
		assertTrue(JarFlight.flying(20, 20));
		assertTrue(JarFlight.flying(39, 20));
		assertFalse(JarFlight.flying(40, 20));
		assertTrue(JarFlight.flying(59, 40));
		assertFalse(JarFlight.flying(60, 40));
	}

	@Test
	public void progressIsHeldAtTheEndsOutsideTheFlight()
	{
		assertEquals(0.0, JarFlight.progress(0, 20), 0);
		assertEquals(1.0, JarFlight.progress(CatchScene.REACT_TICK, 20), 0);
	}
}
