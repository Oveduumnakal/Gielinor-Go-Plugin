/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks that a standing model ends up lying on the ground. */
public class ModelTiltTest
{
	private static final float CLOSE = 0.0001f;

	@Test
	public void theHeadEndsUpAlongTheGroundAndNothingIsBelowIt()
	{
		float[] y = {0, -100, -50, -50};
		float[] z = {0, 0, 20, -20};

		ModelTilt.lieDown(y, z, 4);

		assertEquals(-100f, z[1], CLOSE);
		float lowest = -Float.MAX_VALUE;
		for (float value : y)
			lowest = Math.max(lowest, value);

		assertEquals("the lowest point rests on the ground", 0f, lowest, CLOSE);
	}

	@Test
	public void theBodyIsAsLowAsItIsDeepAfterwards()
	{
		float[] y = {0, -100, -50, -50};
		float[] z = {0, 0, 20, -20};

		ModelTilt.lieDown(y, z, 4);

		float highest = Float.MAX_VALUE;
		for (float value : y)
			highest = Math.min(highest, value);

		assertEquals(-40f, highest, CLOSE);
	}

	@Test
	public void onlyTheFirstVerticesAreTouchedWhenTheCountIsShort()
	{
		float[] y = {0, -10, -77};
		float[] z = {0, 0, 5};

		ModelTilt.lieDown(y, z, 2);

		assertEquals(-77f, y[2], CLOSE);
		assertEquals(5f, z[2], CLOSE);
	}

	@Test
	public void anEmptyModelIsLeftAlone()
	{
		float[] y = {-100f, -50f};
		float[] z = {10f, 20f};

		ModelTilt.lieDown(y, z, 0);
		ModelTilt.lieDown(new float[0], new float[0], 0);

		assertArrayEquals(new float[] {-100f, -50f}, y, CLOSE);
		assertArrayEquals(new float[] {10f, 20f}, z, CLOSE);
	}
}
