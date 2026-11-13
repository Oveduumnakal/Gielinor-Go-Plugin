/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Tests for {@link HeldNotices}. */
public class HeldNoticesTest
{
	@Test
	public void openingAnAlreadyOpenListKeepsTheNoticesHeldBeforeTheBattle()
	{
		List<Notice> held = new ArrayList<>();
		held.add(Notice.player("A friend wants to trade", Tone.GOOD));

		List<Notice> opened = HeldNotices.open(held);

		assertSame(held, opened);
		assertEquals(1, opened.size());
	}

	@Test
	public void openingWhenNothingIsHeldGivesAnEmptyList()
	{
		assertTrue(HeldNotices.open(null).isEmpty());
	}

	@Test
	public void closingAListThatWasAlreadyClosedGivesAnEmptyListNotNull()
	{
		assertNotNull(HeldNotices.close(null));
		assertTrue(HeldNotices.close(null).isEmpty());
	}
}
