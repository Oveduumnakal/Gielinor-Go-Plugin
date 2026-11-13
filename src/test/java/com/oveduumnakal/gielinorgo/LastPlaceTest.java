/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests that a remembered place stays on its world tile when the scene recentres.
 */
public class LastPlaceTest
{
	@Test
	public void aPlaceKeepsItsWorldTileWhenTheSceneBaseMoves()
	{
		assertArrayEquals(new int[]{50 * 128 + 64, 30 * 128 + 64}, LastPlace.sceneOf(3250, 3130, 3200, 3100, 104, 104));
		assertArrayEquals(new int[]{18 * 128 + 64, 30 * 128 + 64}, LastPlace.sceneOf(3250, 3130, 3232, 3100, 104, 104));
	}

	@Test
	public void aPlaceOutsideTheSceneHasNoPosition()
	{
		assertNull(LastPlace.sceneOf(3250, 3130, 3260, 3100, 104, 104));
		assertNull(LastPlace.sceneOf(3250, 3130, 3100, 3100, 104, 104));
		assertNull(LastPlace.sceneOf(3250, 3130, 3200, 3140, 104, 104));
	}
}
