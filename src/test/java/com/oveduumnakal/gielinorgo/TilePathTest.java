/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks routes, free tiles and fighting spots on small hand-drawn maps, and the step rules under them. */
public class TilePathTest
{
	/** A map drawn in text: '#' fills a tile, '.' is open ground. Walls on tile edges are added by hand. */
	private static final class Map2d implements StepRules.Flags
	{
		private final int[][] flags;

		private Map2d(String... rowsTopFirst)
		{
			int height = rowsTopFirst.length;
			int width = rowsTopFirst[0].length();
			flags = new int[width][height];
			for (int row = 0; row < height; row++)
			{
				for (int x = 0; x < width; x++)
				{
					if (rowsTopFirst[row].charAt(x) == '#')
						flags[x][height - 1 - row] = StepRules.FULL;
				}
			}
		}

		private Map2d wallEast(int x, int y)
		{
			flags[x][y] |= StepRules.EAST;
			flags[x + 1][y] |= StepRules.WEST;
			return this;
		}

		private Map2d wallNorth(int x, int y)
		{
			flags[x][y] |= StepRules.NORTH;
			flags[x][y + 1] |= StepRules.SOUTH;
			return this;
		}

		@Override
		public int at(int x, int y)
		{
			boolean inside = x >= 0 && y >= 0 && x < flags.length && y < flags[0].length;
			return inside ? flags[x][y] : StepRules.FULL;
		}

		private TilePath path()
		{
			return new TilePath(new TilePath.Ground()
			{
				@Override
				public boolean canStep(int fromX, int fromY, int toX, int toY)
				{
					return StepRules.canStep(Map2d.this, fromX, fromY, toX, toY);
				}

				@Override
				public boolean standable(int x, int y)
				{
					return StepRules.standable(Map2d.this, x, y);
				}
			});
		}
	}

	private static Map2d open(int size)
	{
		String[] rows = new String[size];
		StringBuilder row = new StringBuilder();
		for (int i = 0; i < size; i++)
			row.append('.');

		for (int i = 0; i < size; i++)
			rows[i] = row.toString();

		return new Map2d(rows);
	}

	@Test
	public void aStraightRouteOnOpenGroundTakesTheShortestWay()
	{
		TilePath path = open(8).path();

		List<int[]> route = path.route(1, 1, 5, 1, 10);

		assertEquals(4, route.size());
		assertEquals(5, route.get(3)[0]);
		assertEquals(1, route.get(3)[1]);
	}

	@Test
	public void aDiagonalCountsAsOneStep()
	{
		TilePath path = open(8).path();

		assertEquals(3, path.route(1, 1, 4, 4, 10).size());
	}

	@Test
	public void theSameTileIsAnEmptyRoute()
	{
		TilePath path = open(4).path();

		assertTrue(path.route(2, 2, 2, 2, 5).isEmpty());
	}

	@Test
	public void aWallBetweenTwoTilesSendsTheRouteRoundIt()
	{
		Map2d map = open(8)
			.wallEast(2, 1)
			.wallEast(2, 2)
			.wallEast(2, 0);

		List<int[]> route = map.path().route(2, 1, 3, 1, 10);

		assertNotNull(route);
		assertTrue("goes round the wall: " + route.size(), route.size() > 1);
		for (int[] tile : route)
			assertFalse(tile[0] == 3 && tile[1] == 1 && route.size() == 1);
	}

	@Test
	public void aLongWallWithNoGapMeansNoRoute()
	{
		Map2d map = open(6);
		for (int y = 0; y < 6; y++)
			map.wallEast(2, y);

		assertNull(map.path().route(0, 3, 5, 3, 40));
		assertFalse(map.path().reachable(0, 3, 5, 3, 40));
	}

	@Test
	public void anObjectInTheWayIsWalkedAround()
	{
		Map2d map = new Map2d(
			".....",
			".....",
			"..#..",
			".....",
			".....");

		List<int[]> route = map.path().route(0, 2, 4, 2, 10);

		assertEquals(4, route.size());
		for (int[] tile : route)
			assertFalse(tile[0] == 2 && tile[1] == 2);
	}

	@Test
	public void aDiagonalPastACornerIsNotAllowed()
	{
		Map2d map = new Map2d(
			"...",
			".#.",
			"...");

		assertFalse(StepRules.canStep(map, 0, 0, 1, 1));
		assertFalse(StepRules.canStep(map, 0, 1, 1, 2));
	}

	@Test
	public void aDiagonalPastAWallEdgeIsNotAllowed()
	{
		Map2d map = open(4).wallNorth(1, 1);

		assertFalse(StepRules.canStep(map, 1, 1, 2, 2));
		assertFalse(StepRules.canStep(map, 1, 1, 1, 2));
		assertTrue(StepRules.canStep(map, 1, 1, 2, 1));
	}

	@Test
	public void theStepLimitCutsALongRoute()
	{
		TilePath path = open(12).path();

		assertNotNull(path.route(0, 0, 8, 0, 8));
		assertNull(path.route(0, 0, 8, 0, 7));
	}

	@Test
	public void aTileOutsideTheMapCannotBeSteppedOnto()
	{
		assertFalse(StepRules.canStep(open(3), 2, 2, 3, 2));
		assertFalse(StepRules.canStep(open(3), 0, 0, -1, 0));
	}

	@Test
	public void aStepThatIsNotToANeighbourIsRefused()
	{
		assertFalse(StepRules.canStep(open(5), 0, 0, 2, 0));
		assertFalse(StepRules.canStep(open(5), 0, 0, 0, 0));
	}

	@Test
	public void aFreeTileNearAMonsterBehindAFenceIsOnThePlayersSide()
	{
		Map2d map = open(12);
		for (int y = 0; y < 12; y++)
			map.wallEast(6, y);

		for (int seed = 0; seed < 50; seed++)
		{
			List<int[]> route = map.path().pick(2, 5, 40, 7, 5, 1, 2, new Random(seed), (x, y) -> false);

			assertNotNull(route);
			int[] end = route.get(route.size() - 1);
			assertTrue("the fence has no gap, so the tile is on the player's side", end[0] <= 6);
		}
	}

	@Test
	public void aFreeTileNearAMonsterIsWithinTwoOfItAndReachable()
	{
		Map2d map = open(12).wallEast(6, 5);
		for (int seed = 0; seed < 50; seed++)
		{
			List<int[]> route = map.path().pick(2, 5, 40, 7, 5, 1, 2, new Random(seed), (x, y) -> false);

			assertNotNull(route);
			int[] end = route.get(route.size() - 1);
			int distance = Math.max(Math.abs(end[0] - 7), Math.abs(end[1] - 5));
			assertTrue(distance >= 1 && distance <= 2);
		}
	}

	@Test
	public void takenTilesAreNeverPicked()
	{
		Map2d map = open(8);
		for (int seed = 0; seed < 50; seed++)
		{
			List<int[]> route = map.path().pick(0, 0, 20, 4, 4, 1, 1, new Random(seed),
				(x, y) -> x == 3 && y == 3);

			int[] end = route.get(route.size() - 1);
			assertFalse(end[0] == 3 && end[1] == 3);
		}
	}

	@Test
	public void aTileFilledByAnObjectIsNeverPicked()
	{
		Map2d map = new Map2d(
			"...",
			".#.",
			"...");

		for (int seed = 0; seed < 50; seed++)
		{
			List<int[]> route = map.path().pick(0, 0, 10, 1, 1, 1, 1, new Random(seed), (x, y) -> false);

			int[] end = route.get(route.size() - 1);
			assertFalse(end[0] == 1 && end[1] == 1);
		}
	}

	@Test
	public void theTileThePickStartsFromIsNeverTheAnswer()
	{
		for (int seed = 0; seed < 50; seed++)
		{
			List<int[]> route = open(8).path().pick(4, 4, 2, 4, 4, 0, 3, new Random(seed), (x, y) -> false);

			assertFalse(route.isEmpty());
		}
	}

	@Test
	public void aFighterStandsTwoTilesFromTheTargetOnItsOwnSide()
	{
		TilePath path = open(12).path();

		List<int[]> route = path.approach(8, 5, 2, 6, 5, 1, 5, 20);

		int[] end = route.get(route.size() - 1);
		assertEquals(6, end[0]);
		assertEquals(5, end[1]);
	}

	@Test
	public void aFighterDoesNotStandWhereAWallLiesBetweenItAndTheTarget()
	{
		Map2d map = open(12)
			.wallEast(6, 5)
			.wallEast(6, 4)
			.wallEast(6, 6);

		List<int[]> route = map.path().approach(8, 5, 2, 6, 5, 1, 5, 40);

		int[] end = route.get(route.size() - 1);
		assertTrue("the spot must have a clear line to the target", map.path().lineClear(end[0], end[1], 8, 5));
		assertFalse(end[0] == 6 && end[1] == 5);
	}

	@Test
	public void noSpotMeansNoApproach()
	{
		Map2d map = open(12);
		for (int y = 0; y < 12; y++)
			map.wallEast(6, y);

		assertNull(map.path().approach(8, 5, 2, 6, 5, 1, 5, 40));
	}

	@Test
	public void aLineWithAnObjectOnItIsNotClear()
	{
		Map2d map = new Map2d(
			".....",
			".....",
			"..#..",
			".....",
			".....");

		assertFalse(map.path().lineClear(0, 2, 4, 2));
		assertTrue(map.path().lineClear(0, 1, 4, 1));
		assertTrue(map.path().lineClear(2, 2, 2, 2));
	}

	@Test
	public void theLeadFindsASpotOnOpenGround()
	{
		ArenaSpots.Lead lead = ArenaSpots.lead(open(12).path(), 8, 5, 1.0, 2, 5, 12);

		assertNotNull(lead);
		assertTrue(lead.gap >= TurnMotion.CONTACT_TILES);
	}

	@Test
	public void theLeadHasNoSpotWhenAFenceRunsBetweenItAndTheOpponent()
	{
		Map2d map = open(12);
		for (int y = 0; y < 12; y++)
			map.wallEast(5, y);

		assertNull(ArenaSpots.lead(map.path(), 8, 5, 1.0, 2, 5, 12));
	}

	@Test
	public void theLeadHasNoSpotWhenTheOpponentStandsInsideAWall()
	{
		Map2d map = new Map2d("............", "............", "............", "............", "........#...",
			"............", "............", "............", "............", "............", "............",
			"............");

		assertNull(ArenaSpots.lead(map.path(), 8, 7, 1.0, 2, 7, 12));
	}
}
