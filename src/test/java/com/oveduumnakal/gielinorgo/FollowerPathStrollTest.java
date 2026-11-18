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

/**
 * Checks the follower's idle stroll as the service drives it: one tick at a time, the player's drawn position fed in
 * first, then the stroll steered, then the follower moved.
 */
public class FollowerPathStrollTest
{
	private static final double CLOSE = 0.0001;
	private static final int PX = 3200;
	private static final int PY = 3200;
	private static final int WALL_AFTER = 3201;
	private static final int GAP = 3199;
	private static final int ONE_REST = FollowerStroll.REST_MAX + 2 * FollowerPath.CLIENT_TICKS;
	private static final FollowerPath.Ground OPEN = (x, y) -> true;

	private FollowerPath path = new FollowerPath(new Random(5));
	private TilePath tiles = StrollField.open().path();
	private FollowerPath.Router router = (fx, fy, tx, ty) -> tiles.route(fx, fy, tx, ty, FollowerPath.MAX_ERRAND);

	private void start(long seed, boolean strolls)
	{
		path = new FollowerPath(new Random(seed));
		path.setStrolling(strolls);
		path.playerAt(PX, PY, 0, OPEN);
	}

	private void tick(double x, double y)
	{
		path.playerAt(x, y, 0, OPEN);
		if (path.rejoinDue())
			path.rejoin(OPEN, router);
		else if (path.strollDue())
			path.startStroll(tiles);

		path.advance();
	}

	private void stand(int clientTicks)
	{
		for (int i = 0; i < clientTicks; i++)
			tick(PX, PY);
	}

	private boolean standUntilWalking()
	{
		for (int i = 0; i < ONE_REST; i++)
		{
			tick(PX, PY);
			if (path.wandering())
				return true;
		}

		return false;
	}

	private void standUntilParked()
	{
		for (int i = 0; i < 12 * FollowerPath.CLIENT_TICKS && (path.walking() || !path.wandering()); i++)
			tick(PX, PY);

		assertFalse(path.walking());
		assertTrue(path.wandering());
	}

	private static double distance(double ax, double ay, double bx, double by)
	{
		return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
	}

	@Test
	public void withNoStrollEverStartedNothingChanges()
	{
		start(1, false);
		double x = path.x();
		double y = path.y();
		for (int i = 0; i < 4 * ONE_REST; i++)
		{
			tick(PX, PY);
			assertFalse(path.strollDue());
			assertFalse(path.wandering());
		}

		assertEquals(x, path.x(), CLOSE);
		assertEquals(y, path.y(), CLOSE);
	}

	@Test
	public void aStoppedFollowerStrollsAfterTheRestAndStaysWithinThreeTiles()
	{
		for (long seed = 0; seed < 40; seed++)
		{
			start(seed, true);
			assertFalse(path.wandering());
			assertTrue("seed " + seed, standUntilWalking());
			for (int i = 0; i < 6 * ONE_REST; i++)
			{
				tick(PX, PY);
				double far = distance(path.x(), path.y(), PX, PY);
				assertTrue("seed " + seed + " too far " + far, far <= FollowerStroll.RADIUS + CLOSE);
				assertTrue("seed " + seed + " on the player " + far, far >= 0.5 - CLOSE);
			}
		}
	}

	@Test
	public void itWalksAtOneTilePerGameTickWhileStrolling()
	{
		start(2, true);
		assertTrue(standUntilWalking());
		double largest = 0;
		double ticksWalking = 0;
		double lastX = path.x();
		double lastY = path.y();
		for (int i = 0; i < 4 * ONE_REST; i++)
		{
			tick(PX, PY);
			if (path.walking())
			{
				double step = distance(path.x(), path.y(), lastX, lastY);
				largest = Math.max(largest, step);
				ticksWalking++;
				assertTrue("a stroll is a walk", step <= (double) FollowerStroll.SPEED / FollowerPath.CLIENT_TICKS
					+ CLOSE);
			}

			lastX = path.x();
			lastY = path.y();
		}

		assertEquals((double) FollowerStroll.SPEED / FollowerPath.CLIENT_TICKS, largest, CLOSE);
		assertTrue(ticksWalking >= FollowerPath.CLIENT_TICKS);
	}

	@Test
	public void itFacesThePlayerOnceItArrives()
	{
		start(3, true);
		assertTrue(standUntilWalking());
		standUntilParked();
		stand(40);

		int wanted = FollowerPath.angle(PX - path.x(), PY - path.y());
		assertEquals(wanted, path.orientation());
	}

	@Test
	public void itStaysWhereItArrivedWhileThePlayerStandsStill()
	{
		start(4, true);
		assertTrue(standUntilWalking());
		standUntilParked();
		double x = path.x();
		double y = path.y();
		for (int i = 0; i < FollowerStroll.REST_MIN - 5; i++)
		{
			tick(PX, PY);
			assertEquals(x, path.x(), CLOSE);
			assertEquals(y, path.y(), CLOSE);
			assertFalse(path.walking());
		}
	}

	@Test
	public void aParkedFollowerStrollsAgainAfterAnotherRest()
	{
		start(4, true);
		assertTrue(standUntilWalking());
		standUntilParked();
		int strolls = 0;
		boolean was = false;
		for (int i = 0; i < 8 * ONE_REST; i++)
		{
			tick(PX, PY);
			if (path.walking() && !was)
				strolls++;

			was = path.walking();
		}

		assertTrue("strolls " + strolls, strolls >= 3);
	}

	@Test
	public void noStrollStartsWhileThePlayerWalks()
	{
		start(6, true);
		for (int step = 1; step <= 40 * FollowerPath.CLIENT_TICKS; step++)
		{
			tick(PX + (double) step / FollowerPath.CLIENT_TICKS, PY);
			assertFalse(path.wandering());
			assertFalse(path.rejoinDue());
		}
	}

	@Test
	public void thePlayerSettingOffMakesItRejoinByARouteAroundAWall()
	{
		boolean crossed = false;
		for (long seed = 0; seed < 400 && !crossed; seed++)
		{
			tiles = StrollField.walled(WALL_AFTER, GAP).path();
			start(seed, true);
			if (!standUntilWalking())
				continue;

			standUntilParked();
			if (path.x() < WALL_AFTER + 1)
				continue;

			assertTrue(path.wandering());
			crossed = walkAwayWatchingTheWall();
		}

		assertTrue("some seed parks it on the far side of the wall", crossed);
	}

	private boolean walkAwayWatchingTheWall()
	{
		double wallLine = WALL_AFTER + 0.5;
		boolean crossed = false;
		tick(PX - 1.0 / FollowerPath.CLIENT_TICKS, PY);
		assertFalse("the move cancels the stroll", path.wandering());
		for (int i = 2; i <= 8 * FollowerPath.CLIENT_TICKS; i++)
		{
			double before = path.x();
			tick(PX - (double) i / FollowerPath.CLIENT_TICKS, PY);
			boolean over = (before - wallLine) * (path.x() - wallLine) < 0;
			if (over)
			{
				crossed = true;
				assertTrue("only through the gap, at y " + path.y(), Math.abs(path.y() - GAP) <= 0.5 + CLOSE);
			}
		}

		assertTrue("it came back to the player's side", path.x() < WALL_AFTER + 0.5);
		return crossed;
	}

	@Test
	public void aRejoinedFollowerEndsOneTileBehindThePlayerAndNeverOnThem()
	{
		for (long seed = 20; seed < 60; seed++)
		{
			start(seed, true);
			assertTrue(standUntilWalking());
			standUntilParked();
			int away = path.x() >= PX ? -1 : 1;
			for (int i = 1; i <= 8 * FollowerPath.CLIENT_TICKS; i++)
			{
				double drawn = PX + away * (double) i / FollowerPath.CLIENT_TICKS;
				tick(drawn, PY);
				assertTrue("seed " + seed, distance(path.x(), path.y(), drawn, PY) >= 0.5 - CLOSE);
			}

			for (int i = 0; i < 4 * FollowerPath.CLIENT_TICKS; i++)
				tick(PX + away * 8, PY);

			assertEquals("seed " + seed, 1.0, distance(path.x(), path.y(), PX + away * 8, PY), CLOSE);
			assertFalse(path.walking());
		}
	}

	@Test
	public void aRejoinWithNoRoutePutsItNextToThePlayer()
	{
		start(9, true);
		assertTrue(standUntilWalking());
		standUntilParked();
		router = (fx, fy, tx, ty) -> null;

		tick(PX + 1.0 / FollowerPath.CLIENT_TICKS, PY);

		assertFalse(path.wandering());
		assertFalse(path.rejoinDue());
		assertTrue(distance(path.x(), path.y(), PX, PY) <= 1.1);
		assertFalse(path.walking());
	}

	@Test
	public void anErrandCancelsAStroll()
	{
		start(10, true);
		assertTrue(standUntilWalking());
		assertTrue(path.wandering());

		assertTrue(path.goTo(PX, PY - 4, PX, PY - 8));

		assertFalse(path.wandering());
		assertTrue(path.onErrand());
		for (int i = 0; i < ONE_REST; i++)
		{
			tick(PX, PY);
			assertFalse(path.strollDue());
		}

		assertTrue(path.arrived());
		assertEquals(PY - 4, path.y(), CLOSE);
	}

	@Test
	public void endingTheErrandTrailsAgainAndThenStrollsAgain()
	{
		start(11, true);
		path.goTo(PX, PY - 4, PX, PY - 8);
		for (int i = 0; i < 6 * FollowerPath.CLIENT_TICKS; i++)
			tick(PX, PY);

		path.endErrand(OPEN, router);

		assertFalse(path.onErrand());
		assertFalse(path.wandering());
		assertTrue(path.walking());
		stand(6 * FollowerPath.CLIENT_TICKS);
		assertFalse(path.walking());
		assertTrue("a fresh rest, then a stroll", standUntilWalking());
	}

	@Test
	public void aTeleportAndAPlaneChangeCancelAStroll()
	{
		start(12, true);
		assertTrue(standUntilWalking());

		path.playerAt(PX + 300, PY, 0, OPEN);

		assertFalse(path.wandering());
		assertFalse(path.rejoinDue());
		assertEquals(PX + 300, path.x(), 1.0 + CLOSE);

		start(13, true);
		assertTrue(standUntilWalking());

		path.playerAt(PX, PY, 1, OPEN);

		assertFalse(path.wandering());
		assertEquals(1, path.plane());
		assertTrue(distance(path.x(), path.y(), PX, PY) <= 1.0 + CLOSE);
	}

	@Test
	public void turningStrollsOffMidStrollBringsItHome()
	{
		start(14, true);
		assertTrue(standUntilWalking());
		stand(10);
		assertTrue(path.wandering());

		path.setStrolling(false);

		assertTrue(path.rejoinDue());
		assertFalse(path.wandering());
		stand(10 * FollowerPath.CLIENT_TICKS);
		assertFalse(path.rejoinDue());
		assertEquals(1.0, distance(path.x(), path.y(), PX, PY), CLOSE);
		assertFalse(path.walking());
		for (int i = 0; i < 2 * ONE_REST; i++)
		{
			tick(PX, PY);
			assertFalse(path.strollDue());
		}
	}

	@Test
	public void strollsStayOffWhenDisabled()
	{
		start(15, false);
		for (int i = 0; i < 4 * ONE_REST; i++)
		{
			tick(PX, PY);
			assertFalse(path.strollDue());
		}

		path.setStrolling(true);
		assertTrue(standUntilWalking());
		path.setStrolling(false);
		stand(10 * FollowerPath.CLIENT_TICKS);
		for (int i = 0; i < ONE_REST; i++)
		{
			tick(PX, PY);
			assertFalse(path.strollDue());
		}
	}

	@Test
	public void aFollowerWithNothingToWalkToKeepsWaiting()
	{
		tiles = StrollField.single(PX, PY - 1).path();
		start(16, true);
		for (int i = 0; i < 4 * ONE_REST; i++)
		{
			tick(PX, PY);
			assertFalse(path.wandering());
		}

		assertEquals(PY - 1, path.y(), CLOSE);
	}

	@Test
	public void resetForgetsAStroll()
	{
		start(17, true);
		assertTrue(standUntilWalking());

		path.reset();

		assertFalse(path.wandering());
		assertFalse(path.placed());
		assertFalse(path.rejoinDue());
	}
}
