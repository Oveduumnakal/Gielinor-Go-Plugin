/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks the size a wild creature's body is drawn at. */
public class WildBodyTest
{
	@Test
	public void theBodyIsThreeQuartersOfTheRealSize()
	{
		assertEquals(96, WildBody.shrunk(128));
		assertEquals(192, WildBody.shrunk(256));
		assertEquals(1, WildBody.shrunk(0));
	}
}
