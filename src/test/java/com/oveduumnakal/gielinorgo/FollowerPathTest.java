/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks how the follower is put down, trails the player and turns, one tick at a time. */
public class FollowerPathTest
{
	private static final double CLOSE = 0.0001;
	private static final int SOUTH = 0;
	private static final int WEST = 512;
	private static final int NORTH = 1024;
	private static final int EAST = 1536;
	private static final FollowerPath.Ground OPEN = (x, y) -> true;
	private static final FollowerPath.Ground WALLED_IN = (x, y) -> false;

	private final FollowerPath path = new FollowerPath();

	@Test
	public void nothingIsPlacedBeforeTheFirstReading()
	{
		assertFalse(path.placed());
		path.advance();
		assertFalse(path.placed());
	}

	@Test
	public void theFirstReadingPutsItSouthOfThePlayerFacingThem()
	{
		path.playerAt(3200, 3200, 0, OPEN);

		assertTrue(path.placed());
		assertAt(3200, 3199);
		assertEquals(0, path.plane());
		assertEquals(NORTH, path.orientation());
		assertFalse(path.walking());
	}

	@Test
	public void aBlockedTileIsSkippedWhenPuttingItDown()
	{
		path.playerAt(3200, 3200, 0, (x, y) -> x == 3201 && y == 3200);

		assertAt(3201, 3200);
		assertEquals(WEST, path.orientation());
	}

	@Test
	public void withNoFreeTileItStandsOnThePlayersTile()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);

		assertAt(3200, 3200);
	}

	@Test
	public void itWalksOntoTheTileThePlayerJustLeft()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		for (int step = 1; step <= 5; step++)
		{
			path.playerAt(3200 + step, 3200, 0, OPEN);
			assertTrue(path.walking());
			gameTick();
			assertAt(3200 + step - 1, 3200);
		}

		assertEquals(EAST, path.orientation());
	}

	@Test
	public void aStraightWalkKeepsItOneTileBehindTheDrawnCharacterAtEveryClientTick()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);
		for (int tick = 1; tick <= 4 * FollowerPath.CLIENT_TICKS; tick++)
		{
			double drawn = 3200 + (double) tick / FollowerPath.CLIENT_TICKS;
			path.playerAt(drawn, 3200, 0, WALLED_IN);
			path.advance();
			if (tick > FollowerPath.CLIENT_TICKS)
				assertAt(drawn - 1, 3200);
		}

		assertTrue(path.walking());
		assertEquals(EAST, path.orientation());
	}

	@Test
	public void aStraightRunKeepsItOneTileBehindTheDrawnCharacterAtEveryClientTick()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);
		for (int tick = 1; tick <= 4 * FollowerPath.CLIENT_TICKS; tick++)
		{
			double drawn = 3200 + 2.0 * tick / FollowerPath.CLIENT_TICKS;
			path.playerAt(drawn, 3200, 0, WALLED_IN);
			path.advance();
			if (tick > FollowerPath.CLIENT_TICKS)
				assertAt(drawn - 1, 3200);
		}

		assertTrue(path.walking());
	}

	@Test
	public void aCornerIsTakenWhereTheDrawnCharacterTurned()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);
		int ticks = 0;
		for (; ticks < 2 * FollowerPath.CLIENT_TICKS; ticks++)
		{
			path.playerAt(3200 + (ticks + 1.0) / FollowerPath.CLIENT_TICKS, 3200, 0, WALLED_IN);
			path.advance();
		}

		for (int tick = 1; tick <= FollowerPath.CLIENT_TICKS; tick++)
		{
			path.playerAt(3202, 3200 + (double) tick / FollowerPath.CLIENT_TICKS, 0, WALLED_IN);
			path.advance();
		}

		assertAt(3202, 3200);
		assertEquals(EAST, path.orientation());
		for (int tick = 0; tick < 2; tick++)
		{
			path.playerAt(3202, 3201, 0, WALLED_IN);
			path.advance();
		}

		path.playerAt(3202, 3201.5, 0, WALLED_IN);
		for (int tick = 0; tick < FollowerPath.CLIENT_TICKS; tick++)
			path.advance();

		assertAt(3202, 3200.5);
		assertEquals(NORTH, path.orientation());
	}

	@Test
	public void whenTheDrawnCharacterStopsItEndsOneTileBehindNotOnIt()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);
		for (int tick = 1; tick <= 3 * FollowerPath.CLIENT_TICKS; tick++)
		{
			path.playerAt(3200 + (double) tick / FollowerPath.CLIENT_TICKS, 3200, 0, WALLED_IN);
			path.advance();
		}

		for (int tick = 0; tick < 2 * FollowerPath.CLIENT_TICKS; tick++)
		{
			path.playerAt(3203, 3200, 0, WALLED_IN);
			path.advance();
		}

		assertAt(3202, 3200);
		assertFalse(path.walking());
	}

	@Test
	public void itNeverWalksAheadOfTheDrawnCharacter()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		for (int tick = 1; tick <= 3 * FollowerPath.CLIENT_TICKS; tick++)
		{
			double drawn = 3200 + 2.0 * tick / FollowerPath.CLIENT_TICKS;
			path.playerAt(drawn, 3200, 0, OPEN);
			path.advance();
			if (drawn >= 3201)
				assertTrue(path.x() <= drawn - FollowerPath.FOLLOW_GAP + CLOSE);
		}
	}

	@Test
	public void aLagOfMoreThanEightTilesPutsItDownNextToThePlayer()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.playerAt(3202, 3200, 0, OPEN);
		path.playerAt(3204, 3200, 0, OPEN);
		path.playerAt(3206, 3200, 0, OPEN);
		path.playerAt(3208, 3200, 0, OPEN);
		path.playerAt(3210, 3200, 0, OPEN);

		assertAt(3210, 3199);
	}

	@Test
	public void itKeepsUpWithARunningPlayer()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);
		for (int step = 1; step <= 6; step++)
		{
			path.playerAt(3200 + 2 * step, 3200, 0, WALLED_IN);
			gameTick();
			assertAt(3200 + 2 * step - 1, 3200);
		}
	}

	@Test
	public void aDiagonalStepTakesOneGameTick()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);
		path.playerAt(3201, 3201, 0, WALLED_IN);
		gameTick();
		path.playerAt(3202, 3202, 0, WALLED_IN);
		gameTick();

		assertAt(3201, 3201);
	}

	@Test
	public void itTakesTheCornerThePlayerTook()
	{
		path.playerAt(3200, 3200, 0, WALLED_IN);
		path.playerAt(3201, 3200, 0, WALLED_IN);
		gameTick();
		path.playerAt(3201, 3201, 0, WALLED_IN);
		gameTick();
		assertAt(3201, 3200);

		path.playerAt(3201, 3202, 0, WALLED_IN);
		gameTick();
		assertAt(3201, 3201);
		assertEquals(NORTH, path.orientation());
	}

	@Test
	public void itStopsWalkingOnceThePlayerStandsStill()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.playerAt(3201, 3200, 0, OPEN);
		gameTick();
		path.playerAt(3201, 3200, 0, OPEN);

		assertFalse(path.walking());
		gameTick();
		assertAt(3200, 3200);
	}

	@Test
	public void aTeleportPutsItDownNextToThePlayerAgain()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.playerAt(3201, 3200, 0, OPEN);
		path.playerAt(2900, 3400, 0, OPEN);

		assertAt(2900, 3399);
		assertFalse(path.walking());
		gameTick();
		assertAt(2900, 3399);
	}

	@Test
	public void aChangeOfPlanePutsItDownAgain()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.playerAt(3201, 3200, 0, OPEN);
		path.playerAt(3201, 3200, 1, OPEN);

		assertEquals(1, path.plane());
		assertAt(3201, 3199);
	}

	@Test
	public void resetForgetsWhereItWas()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.reset();

		assertFalse(path.placed());
		path.playerAt(3201, 3200, 0, OPEN);
		assertAt(3201, 3199);
	}

	@Test
	public void anglesFollowTheGamesCompass()
	{
		assertEquals(SOUTH, FollowerPath.angle(0, -1));
		assertEquals(WEST, FollowerPath.angle(-1, 0));
		assertEquals(NORTH, FollowerPath.angle(0, 1));
		assertEquals(EAST, FollowerPath.angle(1, 0));
		assertEquals(NORTH + 256, FollowerPath.angle(1, 1));
	}

	@Test
	public void itTurnsALittleEachTickAndTheShortWayRound()
	{
		assertEquals(FollowerPath.TURN_PER_TICK, FollowerPath.turnToward(SOUTH, WEST));
		assertEquals(FollowerPath.FULL_TURN - FollowerPath.TURN_PER_TICK, FollowerPath.turnToward(SOUTH, EAST));
		assertEquals(WEST, FollowerPath.turnToward(WEST - 10, WEST));
		assertEquals(20, FollowerPath.turnToward(FollowerPath.FULL_TURN - 20, 20));
	}

	@Test
	public void itTurnsToFaceTheWayItWalks()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.playerAt(3201, 3200, 0, OPEN);
		gameTick();
		assertAt(3200, 3200);
		assertEquals(NORTH, path.orientation());

		path.playerAt(3202, 3200, 0, OPEN);
		path.advance();
		assertEquals(NORTH + FollowerPath.TURN_PER_TICK, path.orientation());
		gameTick();
		assertEquals(EAST, path.orientation());
	}

	private void gameTick()
	{
		for (int i = 0; i < FollowerPath.CLIENT_TICKS; i++)
			path.advance();
	}

	private void assertAt(double x, double y)
	{
		assertEquals("x", x, path.x(), CLOSE);
		assertEquals("y", y, path.y(), CLOSE);
	}

	@Test
	public void anErrandWalksToTheSpotAndFacesWhatItWasToldTo()
	{
		path.playerAt(3200, 3200, 0, OPEN);

		assertTrue(path.goTo(3200, 3195, 3195, 3195));
		assertTrue(path.onErrand());
		assertFalse(path.arrived());
		for (int tick = 0; tick < 6 * FollowerPath.CLIENT_TICKS && !path.arrived(); tick++)
			path.advance();

		assertTrue(path.arrived());
		assertFalse(path.walking());
		for (int tick = 0; tick < 40; tick++)
			path.advance();

		assertAt(3200, 3195);
		assertEquals(WEST, path.orientation());
	}

	@Test
	public void anErrandIsFasterThanARun()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.goTo(3200, 3190, 3200, 3185);
		for (int tick = 0; tick < FollowerPath.CLIENT_TICKS; tick++)
			path.advance();

		assertEquals(3199 - FollowerPath.ERRAND_SPEED, path.y(), CLOSE);
		assertTrue(path.walking());
	}

	@Test
	public void aTooFarSpotIsRefusedAndNothingStarts()
	{
		path.playerAt(3200, 3200, 0, OPEN);

		assertFalse(path.goTo(3200, 3200 + FollowerPath.MAX_ERRAND + 2, 3200, 3200));
		assertFalse(path.onErrand());
		assertFalse(new FollowerPath().goTo(1, 1, 2, 2));
	}

	@Test
	public void thePlayerWalkingOffDuringAnErrandDoesNotDragTheFollowerAlong()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.goTo(3200, 3195, 3200, 3190);
		path.playerAt(3201, 3200, 0, OPEN);
		path.playerAt(3202, 3200, 0, OPEN);
		for (int tick = 0; tick < 6 * FollowerPath.CLIENT_TICKS; tick++)
			path.advance();

		assertTrue(path.arrived());
		assertAt(3200, 3195);
	}

	@Test
	public void endingAnErrandWalksBackToATileBesideThePlayer()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.goTo(3200, 3196, 3200, 3190);
		for (int tick = 0; tick < 6 * FollowerPath.CLIENT_TICKS; tick++)
			path.advance();

		path.endErrand(OPEN);

		assertFalse(path.onErrand());
		assertTrue(path.walking());
		for (int tick = 0; tick < 6; tick++)
		{
			path.playerAt(3200, 3200, 0, OPEN);
			gameTick();
		}

		assertAt(3200, 3199);
		assertFalse(path.walking());
	}

	@Test
	public void aTeleportDuringAnErrandCancelsIt()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.goTo(3200, 3196, 3200, 3190);

		path.playerAt(3300, 3300, 0, OPEN);

		assertFalse(path.onErrand());
		assertAt(3300, 3299);
	}

	@Test
	public void aFollowerThatWalkedTooLongIsPutOnItsSpot()
	{
		path.playerAt(3200, 3200, 0, OPEN);
		path.goTo(3200, 3190, 3190, 3190);

		path.arriveNow();

		assertTrue(path.arrived());
		assertAt(3200, 3190);
		assertEquals(WEST, path.orientation());
	}
}
