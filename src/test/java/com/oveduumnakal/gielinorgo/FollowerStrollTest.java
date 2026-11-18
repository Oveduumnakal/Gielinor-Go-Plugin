/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks where an idle follower strolls to, and how long it rests, on small hand-drawn maps and a seeded random. */
public class FollowerStrollTest
{
	private static final int PLAYER = 4;
	private static final int SEEDS = 200;

	private static TilePath open(int size)
	{
		return StrollField.square(size).path();
	}

	private static TilePath boxedIn()
	{
		return StrollField.single(3, 3).path();
	}

	private static int distance(int[] tile, int x, int y)
	{
		return Math.max(Math.abs(tile[0] - x), Math.abs(tile[1] - y));
	}

	@Test
	public void restIsRolledInTheRangeAndOnlyCountsDownWhileSettled()
	{
		for (int seed = 0; seed < SEEDS; seed++)
		{
			FollowerStroll stroll = new FollowerStroll(new Random(seed));
			for (int tick = 0; tick < 50; tick++)
				assertFalse(stroll.restOver(false));

			assertEquals(-1, stroll.restLeft());
			int ticks = 1;
			while (!stroll.restOver(true))
			{
				ticks++;
				assertTrue(ticks <= FollowerStroll.REST_MAX);
			}

			assertTrue("rest of " + ticks, ticks >= FollowerStroll.REST_MIN && ticks <= FollowerStroll.REST_MAX);
		}
	}

	@Test
	public void unsettledTicksDoNotCountDown()
	{
		FollowerStroll stroll = new FollowerStroll(new Random(1));
		stroll.restOver(true);
		int left = stroll.restLeft();
		for (int tick = 0; tick < 30; tick++)
			assertFalse(stroll.restOver(false));

		assertEquals(left, stroll.restLeft());
	}

	@Test
	public void aStrollNeverEndsOnThePlayersTile()
	{
		TilePath tiles = open(9);
		for (int seed = 0; seed < SEEDS; seed++)
		{
			FollowerStroll stroll = new FollowerStroll(new Random(seed));
			assertTrue(stroll.begin(tiles, PLAYER, PLAYER - 1, PLAYER, PLAYER));
			int[] end = last(stroll);
			assertFalse("seed " + seed, end[0] == PLAYER && end[1] == PLAYER);
		}
	}

	@Test
	public void aStrollNeverWalksThroughThePlayersTile()
	{
		TilePath tiles = open(9);
		for (int seed = 0; seed < SEEDS; seed++)
		{
			FollowerStroll stroll = new FollowerStroll(new Random(seed));
			if (!stroll.begin(tiles, PLAYER, PLAYER - 1, PLAYER, PLAYER))
				continue;

			for (double[] tile : stroll.route())
				assertFalse("seed " + seed, tile[0] == PLAYER && tile[1] == PLAYER);
		}
	}

	@Test
	public void aStrollNeverEndsMoreThanThreeTilesFromThePlayer()
	{
		TilePath tiles = open(9);
		for (int seed = 0; seed < SEEDS; seed++)
		{
			FollowerStroll stroll = new FollowerStroll(new Random(seed));
			assertTrue(stroll.begin(tiles, PLAYER + 1, PLAYER + 1, PLAYER, PLAYER));
			int[] end = last(stroll);
			assertTrue("seed " + seed, distance(end, PLAYER, PLAYER) <= FollowerStroll.RADIUS);
			assertTrue("seed " + seed, distance(end, PLAYER, PLAYER) >= 1);
		}
	}

	@Test
	public void aStrollNeverCrossesAWall()
	{
		StrollField field = StrollField.walledSquare(9, 4);
		TilePath tiles = field.path();
		for (int seed = 0; seed < SEEDS; seed++)
		{
			FollowerStroll stroll = new FollowerStroll(new Random(seed));
			if (!stroll.begin(tiles, 4, 3, 4, 2))
				continue;

			int x = 4;
			int y = 3;
			for (double[] tile : stroll.route())
			{
				assertTrue("seed " + seed, field.canStep(x, y, (int) tile[0], (int) tile[1]));
				x = (int) tile[0];
				y = (int) tile[1];
			}
		}
	}

	@Test
	public void aWallThatCannotBeGoneRoundIsNeverChosen()
	{
		StrollField field = StrollField.walledSquare(9, 4);
		TilePath tiles = field.path();
		for (int seed = 0; seed < SEEDS; seed++)
		{
			FollowerStroll stroll = new FollowerStroll(new Random(seed));
			assertTrue(stroll.begin(tiles, 4, 3, 4, 2));
			int[] end = last(stroll);
			assertTrue("seed " + seed, end[0] <= 4);
		}
	}

	@Test
	public void aBoxedInFollowerGetsNoStrollAndRollsANewRest()
	{
		FollowerStroll stroll = new FollowerStroll(new Random(7));
		assertFalse(stroll.begin(boxedIn(), 3, 3, 3, 2));

		assertEquals(FollowerStroll.Phase.OFF, stroll.phase());
		assertTrue(stroll.restLeft() >= FollowerStroll.REST_MIN && stroll.restLeft() <= FollowerStroll.REST_MAX);
		assertTrue(stroll.route().isEmpty());
	}

	@Test
	public void aStrollRouteIsAtMostFiveSteps()
	{
		TilePath tiles = open(15);
		for (int seed = 0; seed < SEEDS; seed++)
		{
			FollowerStroll stroll = new FollowerStroll(new Random(seed));
			assertTrue(stroll.begin(tiles, 7, 6, 7, 7));
			assertTrue("seed " + seed, stroll.route().size() <= FollowerStroll.MAX_STEPS);
			assertFalse(stroll.route().isEmpty());
		}
	}

	@Test
	public void beginningAStrollStartsWalking()
	{
		FollowerStroll stroll = new FollowerStroll(new Random(3));

		assertEquals(FollowerStroll.Phase.OFF, stroll.phase());
		assertTrue(stroll.begin(open(9), PLAYER, PLAYER - 1, PLAYER, PLAYER));
		assertEquals(FollowerStroll.Phase.WALKING, stroll.phase());
	}

	@Test
	public void arrivingStartsANewRest()
	{
		FollowerStroll stroll = new FollowerStroll(new Random(3));
		stroll.begin(open(9), PLAYER, PLAYER - 1, PLAYER, PLAYER);

		stroll.arrived();

		assertEquals(FollowerStroll.Phase.STANDING, stroll.phase());
		assertTrue(stroll.route().isEmpty());
		assertTrue(stroll.restLeft() >= FollowerStroll.REST_MIN && stroll.restLeft() <= FollowerStroll.REST_MAX);
		assertFalse(stroll.restOver(false));
	}

	@Test
	public void stoppingGoesBackToOffAndForgetsTheRest()
	{
		FollowerStroll stroll = new FollowerStroll(new Random(3));
		stroll.begin(open(9), PLAYER, PLAYER - 1, PLAYER, PLAYER);

		stroll.stop();

		assertEquals(FollowerStroll.Phase.OFF, stroll.phase());
		assertTrue(stroll.route().isEmpty());
		assertEquals(-1, stroll.restLeft());
	}

	@Test
	public void theSameSeedGivesTheSameStroll()
	{
		FollowerStroll one = new FollowerStroll(new Random(11));
		FollowerStroll two = new FollowerStroll(new Random(11));
		one.begin(open(9), PLAYER, PLAYER - 1, PLAYER, PLAYER);
		two.begin(open(9), PLAYER, PLAYER - 1, PLAYER, PLAYER);

		assertEquals(last(one)[0], last(two)[0]);
		assertEquals(last(one)[1], last(two)[1]);
	}

	private static int[] last(FollowerStroll stroll)
	{
		double[] end = null;
		for (double[] tile : stroll.route())
			end = tile;

		assertNotNull(end);
		return new int[]{(int) end[0], (int) end[1]};
	}
}
