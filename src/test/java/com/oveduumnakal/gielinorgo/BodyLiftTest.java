/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks how far a body that sits below its own ground level is lifted. */
public class BodyLiftTest
{
	private static final double CLOSE = 0.0001;

	@Test
	public void theLowestVertexIsTheLargestY()
	{
		assertEquals(17.0, BodyLift.lowest(new float[]{-170, 5, 17, -3}, 4), CLOSE);
		assertEquals(-3.0, BodyLift.lowest(new float[]{-170, -3, -50}, 3), CLOSE);
	}

	@Test
	public void onlyTheFirstCountVerticesAreRead()
	{
		assertEquals(5.0, BodyLift.lowest(new float[]{-170, 5, 900, 900}, 2), CLOSE);
	}

	@Test
	public void aStandingModelIsNotLifted()
	{
		float[] standing = {-150, -80, 0};
		float[] feetAHairLow = {-150, 4};

		assertEquals(0, BodyLift.liftFor(standing, 3, 128));
		assertEquals(0, BodyLift.liftFor(feetAHairLow, 2, 128));
		assertEquals(0.0, BodyLift.restLowest(feetAHairLow, 2), CLOSE);
	}

	@Test
	public void aModelBelowItsOriginIsLiftedByHowFarItSinksAtTheDrawnHeight()
	{
		float[] sunk = {-100, 60};

		assertEquals(60, BodyLift.liftFor(sunk, 2, 128));
		assertEquals(30, BodyLift.liftFor(sunk, 2, 64));
		assertEquals(120, BodyLift.liftFor(sunk, 2, 256));
		assertEquals("rounds up", 11, BodyLift.liftFor(new float[]{17}, 1, 78));
	}

	@Test
	public void aShrunkBodyIsLiftedLess()
	{
		assertEquals(60, BodyLift.lift(60.0, 128));
		assertEquals(30, BodyLift.lift(60.0, 64));
		assertEquals(0, BodyLift.lift(0.0, 128));
	}

	@Test
	public void aPosedBodyBelowTheGroundIsRestedOnIt()
	{
		assertEquals(26, BodyLift.restOnGround(new float[]{-150, 25.2f, 3}, 3));
		assertEquals(40, BodyLift.restOnGround(new float[]{-150, 40}, 2));
	}

	@Test
	public void aPosedBodyOnOrAboveTheGroundIsLeftAlone()
	{
		assertEquals(0, BodyLift.restOnGround(new float[]{-150, 0}, 2));
		assertEquals(0, BodyLift.restOnGround(new float[]{-150, -20, -5}, 3));
		assertEquals(0, BodyLift.restOnGround(new float[]{-150, 4}, 2));
		assertEquals(0, BodyLift.restOnGround(new float[0], 0));
	}

	@Test
	public void aPosedBodyIgnoresVerticesBeyondTheCount()
	{
		assertEquals(0, BodyLift.restOnGround(new float[]{-150, -2, 900}, 2));
	}
}
