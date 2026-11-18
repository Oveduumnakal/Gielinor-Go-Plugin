/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks the words of the "Wild creature menu" setting. */
public class WildMenuStyleTest
{
	@Test
	public void theChoicesReadGameThenPainted()
	{
		assertEquals(2, WildMenuStyle.values().length);
		assertEquals("Game", WildMenuStyle.GAME.toString());
		assertEquals("Painted", WildMenuStyle.PAINTED.toString());
	}

	@Test
	public void theGameMenuComesFirstSoItIsTheDefaultChoice()
	{
		assertEquals(WildMenuStyle.GAME, WildMenuStyle.values()[0]);
	}
}
