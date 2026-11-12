/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks where a projectile is at each moment of a turn. */
public class ProjectilePathTest
{
	private static final double CLOSE = 0.0001;

	private final ProjectilePath path = new ProjectilePath(10.0, 20.0, 60.0, 12.0, 24.0, 40.0);

	@Test
	public void itLeavesTheAttackerAtTheLaunchTick()
	{
		assertEquals(10.0, path.x(ProjectilePath.LAUNCH_TICK), CLOSE);
		assertEquals(20.0, path.y(ProjectilePath.LAUNCH_TICK), CLOSE);
		assertEquals(60.0, path.height(ProjectilePath.LAUNCH_TICK), CLOSE);
		assertEquals(0.0, ProjectilePath.progress(ProjectilePath.LAUNCH_TICK), CLOSE);
	}

	@Test
	public void itIsAtTheTargetAtTheImpactTick()
	{
		assertEquals(12.0, path.x(TurnMotion.IMPACT_TICK), CLOSE);
		assertEquals(24.0, path.y(TurnMotion.IMPACT_TICK), CLOSE);
		assertEquals(40.0, path.height(TurnMotion.IMPACT_TICK), CLOSE);
		assertEquals(1.0, ProjectilePath.progress(TurnMotion.IMPACT_TICK), CLOSE);
	}

	@Test
	public void itNeverMovesBackward()
	{
		double last = 0.0;
		for (int age = 0; age <= TurnMotion.TURN_TICKS; age++)
		{
			double now = ProjectilePath.progress(age);
			assertTrue("age " + age, now >= last);
			assertTrue(now >= 0.0 && now <= 1.0);
			assertTrue(path.x(age) >= 10.0 - CLOSE && path.x(age) <= 12.0 + CLOSE);
			last = now;
		}
	}

	@Test
	public void itIsSeenOnlyInTheAir()
	{
		assertFalse(ProjectilePath.visible(0));
		assertFalse(ProjectilePath.visible(ProjectilePath.LAUNCH_TICK - 1));
		assertTrue(ProjectilePath.visible(ProjectilePath.LAUNCH_TICK));
		assertTrue(ProjectilePath.visible(TurnMotion.IMPACT_TICK - 1));
		assertFalse(ProjectilePath.visible(TurnMotion.IMPACT_TICK));
		assertFalse(ProjectilePath.visible(TurnMotion.TURN_TICKS));
	}

	@Test
	public void itRisesInTheMiddleOfTheFlight()
	{
		int middle = (ProjectilePath.LAUNCH_TICK + TurnMotion.IMPACT_TICK) / 2;

		assertTrue(path.height(middle) > 50.0);
	}

	@Test
	public void itPointsFromTheAttackerToTheTarget()
	{
		double[] way = path.direction();

		assertEquals(2.0, way[0], CLOSE);
		assertEquals(4.0, way[1], CLOSE);
	}

	@Test
	public void withGroundHeightsTheFlightGoesFromBodyToBody()
	{
		ProjectilePath raised = new ProjectilePath(0, 0, 50, -300, 4, 0, 50, 0);

		assertTrue(raised.hasGround());
		assertEquals(-300 - 50, raised.z(ProjectilePath.LAUNCH_TICK), CLOSE);
		assertEquals(0 - 50, raised.z(TurnMotion.IMPACT_TICK), CLOSE);
		assertFalse(path.hasGround());
	}

	@Test
	public void aFlightWithOneGroundUnknownFollowsTheTerrain()
	{
		assertFalse(new ProjectilePath(0, 0, 50, Double.NaN, 4, 0, 50, 0).hasGround());
	}
}
