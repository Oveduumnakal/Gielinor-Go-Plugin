/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks that the follower keeps a gap, lags a little, then catches up, one client tick at a time. */
public class FollowerPaceTest
{
	private static final int TICKS = FollowerPath.CLIENT_TICKS;
	private static final double CLOSE = 0.01;
	private static final FollowerPath.Ground OPEN = (x, y) -> true;

	private FollowerPath path = new FollowerPath(new Random(7));

	private double drawn = 3200;

	private void walk(double tilesPerGameTick, int clientTicks)
	{
		for (int i = 0; i < clientTicks; i++)
		{
			drawn += tilesPerGameTick / TICKS;
			path.playerAt(drawn, 3200, 0, OPEN);
			path.advance();
		}
	}

	private void stand(int clientTicks)
	{
		for (int i = 0; i < clientTicks; i++)
		{
			path.playerAt(drawn, 3200, 0, OPEN);
			path.advance();
		}
	}

	@Test
	public void aWalkingPlayerNeverHasItOnTheirHeelsOrFarBehind()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		for (int i = 0; i < 20 * TICKS; i++)
		{
			walk(1, 1);
			if (drawn > 3204)
			{
				double gap = drawn - path.x();
				assertTrue("gap " + gap, gap >= FollowerPath.FOLLOW_GAP - CLOSE);
				assertTrue("gap " + gap, gap <= FollowerPath.FOLLOW_GAP + FollowerPace.LAG_MAX + CLOSE);
			}
		}
	}

	@Test
	public void itKeepsMovingWhileThePlayerKeepsRunning()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		walk(2, 6 * TICKS);
		for (int i = 0; i < 10 * TICKS; i++)
		{
			walk(2, 1);
			assertTrue("stopped at client tick " + i, path.walking());
		}
	}

	@Test
	public void itWaitsForTheGapToGrowBeforeSettingOff()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		double start = path.x();
		walk(1, TICKS / 2);

		assertEquals(start, path.x(), CLOSE);
	}

	@Test
	public void itsSpeedNeverJumps()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		double lastX = path.x();
		double lastY = path.y();
		double speed = 0;
		for (int i = 0; i < 10 * TICKS; i++)
		{
			walk(2, 1);
			double now = Math.max(Math.abs(path.x() - lastX), Math.abs(path.y() - lastY));
			assertTrue("sped up by " + (now - speed), now - speed <= 0.1 / TICKS + 1e-6);
			speed = now;
			lastX = path.x();
			lastY = path.y();
		}
	}

	@Test
	public void doublingBackDoesNotSendItOutToTheTurningPoint()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		walk(2, 6 * TICKS);
		double turn = drawn;
		walk(-2, 2 * TICKS);
		double farthest = path.x();
		for (int i = 0; i < 2 * TICKS; i++)
		{
			stand(1);
			farthest = Math.max(farthest, path.x());
		}

		assertTrue("walked to " + farthest + " with the turn at " + turn, farthest < turn - 1);
	}

	@Test
	public void whenThePlayerStopsItCatchesUpToTheGap()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		walk(2, 8 * TICKS);
		stand(2 * TICKS);

		assertEquals(FollowerPath.FOLLOW_GAP, drawn - path.x(), CLOSE);
		assertFalse(path.walking());
	}

	@Test
	public void aTeleportPutsItBackNextToThePlayerAtOnce()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		walk(2, 4 * TICKS);
		drawn = 3300;
		path.playerAt(drawn, 3200, 0, OPEN);

		assertTrue(Math.max(Math.abs(path.x() - 3300), Math.abs(path.y() - 3200)) <= 1);
	}

	@Test
	public void aNewPlaneOrAMapLoadPutsItBackNextToThePlayerAtOnce()
	{
		path.playerAt(drawn, 3200, 0, OPEN);
		walk(2, 4 * TICKS);
		path.playerAt(drawn, 3200, 1, OPEN);

		assertEquals(1, path.plane());
		assertTrue(Math.max(Math.abs(path.x() - drawn), Math.abs(path.y() - 3200)) <= 1);
	}

	@Test
	public void theLagFromOneSeedRepeats()
	{
		FollowerPace a = new FollowerPace(new Random(3), true);
		FollowerPace b = new FollowerPace(new Random(3), true);
		assertEquals(a.lag(), b.lag());
		assertTrue(a.lag() >= FollowerPace.LAG_MIN && a.lag() <= FollowerPace.LAG_MAX);
	}

	@Test
	public void aPaceWithNoLagSetsOffAtOnce()
	{
		FollowerPace pace = new FollowerPace(new Random(3), false);

		assertTrue(pace.moving(0.1, true));
		assertFalse(pace.moving(0, true));
	}

	@Test
	public void afterCatchingUpItStillStrollsWhenThePlayerStandsStill()
	{
		path.setStrolling(true);
		path.playerAt(drawn, 3200, 0, OPEN);
		walk(2, 4 * TICKS);
		boolean due = false;
		for (int i = 0; i < FollowerStroll.REST_MAX + 3 * TICKS && !due; i++)
		{
			stand(1);
			due = path.strollDue();
		}

		assertTrue(due);
	}
}
