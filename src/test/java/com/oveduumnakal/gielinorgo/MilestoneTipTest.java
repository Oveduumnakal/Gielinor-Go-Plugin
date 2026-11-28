/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks that only a reward with an egg says it waits for an egg slot. */
public class MilestoneTipTest
{
	@Test
	public void aRewardWithoutAnEggNeverWaitsForAnEggSlot()
	{
		MilestoneView.Row jars = new MilestoneView.Row("25 species", 25, 25, "5 Iron jars", false, false);

		assertEquals("5 Iron jars", TasksPage.milestoneTip(jars));
		assertEquals("5 Iron jars (waiting for an egg slot)", TasksPage.milestoneTip(jars.givingEgg()));
	}
}
