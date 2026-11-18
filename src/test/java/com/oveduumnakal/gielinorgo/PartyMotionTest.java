/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks the Party page's motion with a clock that is passed in: rows slide in one after another, bars ease, Make lead
 * swaps the two rows, a creature moved to storage slides out while the rows below close the gap, a sliding row refuses
 * clicks, and Reduced and Off slide nothing.
 */
public class PartyMotionTest
{
	private final Animations animations = new Animations();
	private final PartyMotion motion = new PartyMotion(animations);
	private final PartyLayout layout = layout();

	private static PartyLayout layout()
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, 765, 503);
		WindowLayout window = WindowLayout.of(size.width, size.height);
		return PartyLayout.of(window.body, WindowStyle.of(WindowTheme.DARK_GLASS, window.scale).metrics(),
			window.scale, false);
	}

	private static List<CreatureView> team(String... names)
	{
		List<CreatureView> list = new ArrayList<>();
		for (String name : names)
			list.add(PartyRig.creature(name, 5, 1.0, null));

		return list;
	}

	@Test
	public void rowsSlideInFromTheLeftOneAfterAnotherAndLandAtRest()
	{
		motion.enter(layout.rows[0].width, MotionMode.FULL, 1_000L);

		assertTrue(motion.rowX(0, 1_000L) < 0);
		assertTrue(motion.rowX(0, 1_090L) > motion.rowX(0, 1_000L));
		assertEquals("a later row is still waiting at the start", motion.rowX(0, 1_000L), motion.rowX(3, 1_100L), 0.0);
		assertEquals(0.0, motion.rowX(0, 1_000L + WindowTimes.ROW_IN), 0.0);
		assertEquals(0.0, motion.rowX(5, 1_000L + WindowTimes.ROW_IN + 5 * WindowTimes.ROW_STAGGER), 0.0);
	}

	@Test
	public void rowsAreJustThereInReducedAndOff()
	{
		for (MotionMode mode : new MotionMode[] {MotionMode.REDUCED, MotionMode.OFF})
		{
			PartyMotion calm = new PartyMotion(new Animations());
			calm.enter(layout.rows[0].width, mode, 1_000L);

			assertEquals(mode.toString(), 0.0, calm.rowX(0, 1_000L), 0.0);
			assertEquals(mode.toString(), 0.0, calm.rowX(5, 1_000L), 0.0);
		}
	}

	@Test
	public void theBarsEaseToANewValueAndStartOnTheFirstOne()
	{
		List<CreatureView> first = team("Goblin");
		motion.sync(first, layout, MotionMode.FULL, 0L);

		assertEquals(1.0, motion.hp(0, 0L), 0.0);

		List<CreatureView> hurt = Collections.singletonList(PartyRig.creature("Goblin", 5, 0.4, null));
		motion.sync(hurt, layout, MotionMode.FULL, 1_000L);

		assertEquals(1.0, motion.hp(0, 1_000L), 0.0);
		double mid = motion.hp(0, 1_000L + WindowTimes.HP_BAR / 2);
		assertTrue(mid < 1.0 && mid > 0.4);
		assertEquals(0.4, motion.hp(0, 1_000L + WindowTimes.HP_BAR), 0.0);
	}

	@Test
	public void theBarsJumpInOffMode()
	{
		motion.sync(team("Goblin"), layout, MotionMode.OFF, 0L);
		motion.sync(Collections.singletonList(PartyRig.creature("Goblin", 5, 0.4, null)), layout, MotionMode.OFF, 10L);

		assertEquals(0.4, motion.hp(0, 10L), 0.0);
	}

	@Test
	public void makeLeadSlidesTheTwoRowsToEachOthersPlaceAndTheBarsGoWithThem()
	{
		List<CreatureView> before = Arrays.asList(PartyRig.creature("Goblin", 5, 1.0, null),
			PartyRig.creature("Cow", 5, 0.5, null), PartyRig.creature("Imp", 5, 0.8, null));
		motion.sync(before, layout, MotionMode.FULL, 0L);
		List<CreatureView> after = Arrays.asList(before.get(1), before.get(0), before.get(2));

		motion.sync(after, layout, MotionMode.FULL, 5_000L);

		assertEquals(layout.rows[1].y, motion.rowY(0, layout.rows[0].y, 5_000L), 0.0);
		assertEquals(layout.rows[0].y, motion.rowY(1, layout.rows[1].y, 5_000L), 0.0);
		assertEquals(layout.rows[2].y, motion.rowY(2, layout.rows[2].y, 5_000L), 0.0);
		assertEquals(layout.rows[0].y, motion.rowY(0, layout.rows[0].y, 5_000L + WindowTimes.SLIDE), 0.0);
		assertEquals(0.5, motion.hp(0, 5_000L), 0.0);
		assertEquals(1.0, motion.hp(1, 5_000L), 0.0);
	}

	@Test
	public void aSlidingRowRefusesClicksUntilItLandsAndNoOtherRowDoes()
	{
		List<CreatureView> before = team("Goblin", "Cow", "Imp");
		motion.sync(before, layout, MotionMode.FULL, 0L);
		motion.sync(Arrays.asList(before.get(1), before.get(0), before.get(2)), layout, MotionMode.FULL, 5_000L);

		assertTrue(motion.sliding(0, 5_000L));
		assertTrue(motion.sliding(1, 5_100L));
		assertFalse(motion.sliding(2, 5_000L));
		assertFalse(motion.sliding(0, 5_000L + WindowTimes.SLIDE));
		assertTrue(WindowTimes.SLIDE <= 200L);
	}

	@Test
	public void aCreatureMovedToStorageSlidesOutAndTheRowsBelowCloseTheGap()
	{
		List<CreatureView> before = team("Goblin", "Cow", "Imp");
		motion.sync(before, layout, MotionMode.FULL, 0L);

		motion.sync(Arrays.asList(before.get(0), before.get(2)), layout, MotionMode.FULL, 5_000L);

		List<PartyMotion.Ghost> ghosts = motion.ghosts(5_000L);
		assertEquals(1, ghosts.size());
		assertEquals(1, ghosts.get(0).index());
		assertEquals("Cow", ghosts.get(0).view()
			.name());
		assertEquals(layout.rows[2].y, motion.rowY(1, layout.rows[1].y, 5_000L), 0.0);
		assertTrue(motion.sliding(1, 5_000L));
		assertTrue(motion.ghosts(5_000L + WindowTimes.SLIDE).isEmpty());
	}

	@Test
	public void nothingSlidesOrLeavesAGhostInReducedAndOff()
	{
		for (MotionMode mode : new MotionMode[] {MotionMode.REDUCED, MotionMode.OFF})
		{
			PartyMotion calm = new PartyMotion(new Animations());
			List<CreatureView> before = team("Goblin", "Cow", "Imp");
			calm.sync(before, layout, mode, 0L);
			calm.sync(Arrays.asList(before.get(1), before.get(0)), layout, mode, 5_000L);

			assertFalse(mode.toString(), calm.sliding(0, 5_000L));
			assertEquals(mode.toString(), layout.rows[0].y, calm.rowY(0, layout.rows[0].y, 5_000L), 0.0);
			assertTrue(mode.toString(), calm.ghosts(5_000L).isEmpty());
		}
	}

	@Test
	public void aCreatureThatArrivesSlidesIn()
	{
		List<CreatureView> before = team("Goblin");
		motion.sync(before, layout, MotionMode.FULL, 0L);
		List<CreatureView> after = new ArrayList<>(before);
		after.add(PartyRig.creature("Cow", 5, 1.0, null));

		motion.sync(after, layout, MotionMode.FULL, 5_000L);

		assertTrue(motion.rowX(1, 5_000L) < 0);
		assertEquals(0.0, motion.rowX(1, 5_000L + WindowTimes.ROW_IN), 0.0);
		assertEquals(0.0, motion.rowX(0, 5_000L), 0.0);
	}

	@Test
	public void theHoveredRowNudgesHopsAndTheCursorGlidesToIt()
	{
		motion.sync(team("Goblin", "Cow", "Imp"), layout, MotionMode.FULL, 0L);

		motion.highlight(2, MotionMode.FULL, 1_000L);

		assertEquals(0.0, motion.nudge(2, 1_000L), 0.0);
		assertEquals(1.0, motion.nudge(2, 1_000L + WindowTimes.HOVER_NUDGE), 0.0);
		assertEquals(0.0, motion.cursor(1_000L), 0.0);
		double mid = motion.cursor(1_000L + WindowTimes.CURSOR_GLIDE / 2);
		assertTrue(mid > 0.0 && mid < 2.0);
		assertEquals(2.0, motion.cursor(1_000L + WindowTimes.CURSOR_GLIDE), 0.0);
		assertTrue(motion.hop(2, 1_000L + WindowTimes.PORTRAIT_HOP / 2) > 0.9);
		assertEquals(0.0, motion.hop(2, 1_000L + WindowTimes.PORTRAIT_HOP), 1e-9);

		motion.highlight(-1, MotionMode.FULL, 2_000L);
		assertEquals(0.0, motion.nudge(2, 2_000L + WindowTimes.HOVER_NUDGE), 0.0);
	}

	@Test
	public void hoverNeverNudgesOrHopsInOffMode()
	{
		motion.sync(team("Goblin", "Cow"), layout, MotionMode.OFF, 0L);

		motion.highlight(1, MotionMode.OFF, 1_000L);

		assertEquals(1.0, motion.nudge(1, 1_000L), 0.0);
		assertEquals(0.0, motion.hop(1, 1_000L), 1e-9);
		assertEquals(1.0, motion.cursor(1_000L), 0.0);
	}

	@Test
	public void resetForgetsEverythingSoNothingReplaysOnTheNextOpen()
	{
		motion.sync(team("Goblin", "Cow"), layout, MotionMode.FULL, 0L);
		motion.highlight(1, MotionMode.FULL, 10L);

		motion.reset();

		assertEquals(-1, motion.highlighted());
		assertEquals(0.0, motion.hp(0, 1_000L), 0.0);
	}
}
