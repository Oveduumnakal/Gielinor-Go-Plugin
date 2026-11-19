/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks when the painted window may open, why it closes, and when its Storage tab can be chosen. */
public class WindowRulesTest
{
	@Test
	public void aBattleIsOnWhenAnySceneOrDirectorIsOn()
	{
		assertFalse(WindowRules.battleOn(false, false, false));
		assertTrue(WindowRules.battleOn(true, false, false));
		assertTrue(WindowRules.battleOn(false, true, false));
		assertTrue(WindowRules.battleOn(false, false, true));
	}

	@Test
	public void opensOnlyWhenLoadedAndNothingIsFighting()
	{
		assertTrue(WindowRules.mayOpen(false, false, true));
		assertFalse(WindowRules.mayOpen(true, false, true));
		assertFalse(WindowRules.mayOpen(false, true, true));
		assertFalse(WindowRules.mayOpen(true, true, true));
		assertFalse(WindowRules.mayOpen(false, false, false));
	}

	@Test
	public void everyClosedCombinationIsRefused()
	{
		for (int bits = 0; bits < 8; bits++)
		{
			boolean combat = (bits & 1) != 0;
			boolean battle = (bits & 2) != 0;
			boolean loaded = (bits & 4) != 0;
			boolean fine = loaded && !combat && !battle;

			assertEquals("combat " + combat + " battle " + battle + " loaded " + loaded, fine,
				WindowRules.mayOpen(combat, battle, loaded));
			assertEquals(fine, WindowRules.blockedNotice(combat, battle, loaded) == null);
		}
	}

	@Test
	public void theBlockedNoticeNamesTheReason()
	{
		assertEquals(NoticeMessage.LOADING, WindowRules.blockedNotice(false, false, false));
		assertEquals(NoticeMessage.LOADING, WindowRules.blockedNotice(true, true, false));
		assertEquals(NoticeMessage.BLOCKED_COMBAT, WindowRules.blockedNotice(true, false, true));
		assertEquals(NoticeMessage.BLOCKED_BATTLE, WindowRules.blockedNotice(false, true, true));
		assertEquals(NoticeMessage.BLOCKED_BATTLE, WindowRules.blockedNotice(true, true, true));
		assertNull(WindowRules.blockedNotice(false, false, true));
	}

	@Test
	public void theClosedNoticeNamesTheReasonAndABattleWins()
	{
		assertEquals(NoticeMessage.CLOSED_COMBAT, WindowRules.closedNotice(true, false));
		assertEquals(NoticeMessage.CLOSED_BATTLE, WindowRules.closedNotice(false, true));
		assertEquals(NoticeMessage.CLOSED_BATTLE, WindowRules.closedNotice(true, true));
		assertNull(WindowRules.closedNotice(false, false));
	}

	@Test
	public void storageFollowsTheTeamSwapRule()
	{
		assertFalse(WindowRules.storageEnabled(false, false));
		assertTrue(WindowRules.storageEnabled(true, false));
		assertTrue(WindowRules.storageEnabled(false, true));
		assertTrue(WindowRules.storageEnabled(true, true));

		for (int bits = 0; bits < 4; bits++)
		{
			boolean city = (bits & 1) != 0;
			boolean bank = (bits & 2) != 0;
			assertEquals(TeamSwapRule.allowed(city, bank, false), WindowRules.storageEnabled(city, bank));
		}
	}

	@Test
	public void aDeferredResultWaitsForTheSceneAndTheLines()
	{
		assertFalse(WindowRules.deferredDue(10, 300, true, true));
		assertFalse(WindowRules.deferredDue(10, 300, false, false));
		assertTrue(WindowRules.deferredDue(10, 300, false, true));
	}

	@Test
	public void aDeferredResultIsWrittenAfterTheLimitEvenIfTheSceneNeverEnds()
	{
		assertFalse(WindowRules.deferredDue(300, 300, true, false));
		assertTrue(WindowRules.deferredDue(301, 300, true, false));
	}
}
