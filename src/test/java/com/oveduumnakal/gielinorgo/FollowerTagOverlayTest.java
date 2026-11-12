/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks which name the follower's tag shows. */
public class FollowerTagOverlayTest
{
	@Test
	public void showsTheNicknameWhenThereIsOne()
	{
		assertEquals("Rex", FollowerTagOverlay.nameFor("Rex", "Imp"));
	}

	@Test
	public void showsTheSpeciesNameWithoutANickname()
	{
		assertEquals("Imp", FollowerTagOverlay.nameFor(null, "Imp"));
		assertEquals("Imp", FollowerTagOverlay.nameFor("", "Imp"));
	}

	@Test
	public void tagIsHalfOpaque()
	{
		assertEquals(128, FollowerTagOverlay.TAG_ALPHA);
	}
}
