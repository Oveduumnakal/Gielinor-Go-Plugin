/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the notice timing: it pops in, holds, then fades, and what each motion mode drops. */
public class WindowNoticeTest
{
	@Test
	public void fullMotionPopsHoldsAndFades()
	{
		assertEquals(150 + 2_000 + 400, NoticeClock.totalMs(MotionMode.FULL));

		NoticeClock.Frame start = NoticeClock.at(MotionMode.FULL, 0);
		assertEquals(0.8, start.scale, 1e-12);
		assertEquals(0.0, start.alpha, 0.0);
		assertTrue(start.visible);

		NoticeClock.Frame landed = NoticeClock.at(MotionMode.FULL, 150);
		assertEquals(1.0, landed.scale, 0.0);
		assertEquals(1.0, landed.alpha, 0.0);

		NoticeClock.Frame holding = NoticeClock.at(MotionMode.FULL, 1_000);
		assertEquals(1.0, holding.scale, 0.0);
		assertEquals(1.0, holding.alpha, 0.0);

		NoticeClock.Frame lastHold = NoticeClock.at(MotionMode.FULL, 2_149);
		assertEquals(1.0, lastHold.alpha, 0.0);

		NoticeClock.Frame halfFaded = NoticeClock.at(MotionMode.FULL, 2_150 + 200);
		assertEquals(0.5, halfFaded.alpha, 1e-12);
		assertEquals(1.0, halfFaded.scale, 0.0);
		assertTrue(halfFaded.visible);

		NoticeClock.Frame over = NoticeClock.at(MotionMode.FULL, 2_550);
		assertFalse(over.visible);
		assertEquals(0.0, over.alpha, 0.0);
	}

	@Test
	public void thePopOvershootsOnTheWayToFullSize()
	{
		boolean past = false;
		double last = 0.0;
		for (long t = 0; t <= 150; t += 5)
		{
			NoticeClock.Frame f = NoticeClock.at(MotionMode.FULL, t);
			past |= f.scale > 1.0;
			assertTrue("never under the start size", f.scale >= 0.8);
			assertTrue("fades in without dipping", f.alpha >= last);
			last = f.alpha;
		}

		assertTrue("a small overshoot", past);
		assertTrue(NoticeClock.at(MotionMode.FULL, 100).scale < 1.2);
	}

	@Test
	public void reducedFadesInAndOutWithNoScale()
	{
		assertEquals(2_550, NoticeClock.totalMs(MotionMode.REDUCED));
		for (long t = 0; t < 2_550; t += 25)
			assertEquals(1.0, NoticeClock.at(MotionMode.REDUCED, t).scale, 0.0);

		assertEquals(0.0, NoticeClock.at(MotionMode.REDUCED, 0).alpha, 0.0);
		assertTrue(NoticeClock.at(MotionMode.REDUCED, 75).alpha > 0.0);
		assertEquals(1.0, NoticeClock.at(MotionMode.REDUCED, 1_000).alpha, 0.0);
		assertEquals(0.5, NoticeClock.at(MotionMode.REDUCED, 2_350).alpha, 1e-12);
		assertFalse(NoticeClock.at(MotionMode.REDUCED, 2_550).visible);
	}

	@Test
	public void offShowsFullStrengthForTwoSecondsWithNoMotion()
	{
		assertEquals(2_000, NoticeClock.totalMs(MotionMode.OFF));
		for (long t : new long[] {0, 1, 100, 1_000, 1_999})
		{
			NoticeClock.Frame f = NoticeClock.at(MotionMode.OFF, t);
			assertTrue(f.visible);
			assertEquals(1.0, f.alpha, 0.0);
			assertEquals(1.0, f.scale, 0.0);
		}

		assertFalse(NoticeClock.at(MotionMode.OFF, 2_000).visible);
	}

	@Test
	public void aNoticeNotYetStartedIsNotShown()
	{
		for (MotionMode mode : MotionMode.values())
			assertFalse(NoticeClock.at(mode, -1).visible);
	}

	@Test
	public void theMessagesAreTheOnesTheOwnerAsked()
	{
		assertEquals("Gielinor Go closed: you are in combat.", NoticeMessage.CLOSED_COMBAT.text());
		assertEquals("Gielinor Go closed: a battle started.", NoticeMessage.CLOSED_BATTLE.text());
		assertEquals("Can't open during combat.", NoticeMessage.BLOCKED_COMBAT.text());
		assertEquals("Can't open during a battle.", NoticeMessage.BLOCKED_BATTLE.text());
		assertEquals("Still loading your save.", NoticeMessage.LOADING.text());
		assertEquals("Click the jar to choose your first creature.", NoticeMessage.FIRST_RUN.text());
	}

	@Test
	public void theStorageNoticeSaysWhereToGo()
	{
		assertEquals("Go to a city or a bank to open Storage.", NoticeMessage.STORAGE_LOCKED.text());
	}

	@Test
	public void aSlotKeepsItsMessageAndPlaceAndFollowsTheClock()
	{
		NoticeSlot slot = new NoticeSlot(NoticeMessage.CLOSED_COMBAT, 10_000, 300, 200);

		assertEquals(NoticeMessage.CLOSED_COMBAT, slot.message());
		assertEquals(300, slot.centerX());
		assertEquals(200, slot.centerY());
		assertEquals(0.8, slot.frame(MotionMode.FULL, 10_000).scale, 1e-12);
		assertTrue(slot.frame(MotionMode.FULL, 11_000).visible);
		assertFalse(slot.frame(MotionMode.FULL, 10_000 + NoticeClock.totalMs(MotionMode.FULL)).visible);
	}

	@Test
	public void aSlotLastsTheHoldAloneWhenAnimationsAreOff()
	{
		NoticeSlot slot = new NoticeSlot(NoticeMessage.LOADING, 0, 0, 0);

		assertTrue(slot.frame(MotionMode.OFF, 1_999).visible);
		assertEquals(1.0, slot.frame(MotionMode.OFF, 1_999).alpha, 0.0);
		assertFalse(slot.frame(MotionMode.OFF, 2_000).visible);
	}

	@Test
	public void aNoticeIsKeptInsideTheCanvas()
	{
		assertEquals(100, NoticeSlot.clampCenter(0, 100, 765));
		assertEquals(665, NoticeSlot.clampCenter(900, 100, 765));
		assertEquals(300, NoticeSlot.clampCenter(300, 100, 765));
		assertEquals(150, NoticeSlot.clampCenter(40, 200, 300));
	}
}
