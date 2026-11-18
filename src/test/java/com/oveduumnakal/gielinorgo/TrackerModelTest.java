/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The tracker's game-thread state: the lines it draws, and the "Done!" and "Hatched!" holds, with a fake clock. */
public class TrackerModelTest
{
	private static TrackerView.Line task(int slot, int progress, int goal, boolean done)
	{
		return new TrackerView.Line(TrackerView.Kind.TASK, TrackerView.taskId(slot), "Task " + slot,
			progress + " / " + goal, progress / (double) goal, null, done);
	}

	private static TrackerView.Line egg(int place, Rarity rarity, int left)
	{
		return new TrackerView.Line(TrackerView.Kind.EGG, TrackerView.eggId(place), rarity.label() + " egg",
			left <= 0 ? "Ready" : left + " steps", 1.0 - left / 1000.0, rarity, false);
	}

	private static List<String> texts(List<TrackerView.Line> lines)
	{
		List<String> out = new ArrayList<>();
		for (TrackerView.Line line : lines)
			out.add(line.label() + "|" + line.valueText());

		return out;
	}

	@Test
	public void nothingPinnedGivesAnEmptyFrame()
	{
		TrackerModel model = new TrackerModel();

		assertTrue(model.frame(TrackerView.NONE, 5L).isEmpty());
		assertTrue(model.expired(5L).isEmpty());
	}

	@Test
	public void linesKeepTasksFirstThenEggsInOrder()
	{
		TrackerModel model = new TrackerModel();
		TrackerView view = new TrackerView(Arrays.asList(task(0, 1, 3, false), task(3, 2, 10, false),
			egg(0, Rarity.RARE, 780), egg(2, Rarity.COMMON, 0)));

		assertEquals(Arrays.asList("Task 0|1 / 3", "Task 3|2 / 10", "Rare egg|780 steps", "Common egg|Ready"),
			texts(model.frame(view, 0L)));
	}

	@Test
	public void aFinishedTaskSaysDoneThenAsksToBeUnpinnedAfterTheHold()
	{
		TrackerModel model = new TrackerModel();
		TrackerView view = new TrackerView(Collections.singletonList(task(1, 3, 3, true)));

		List<TrackerView.Line> first = model.frame(view, 1000L);
		assertEquals("Done!", first.get(0).valueText());
		assertTrue(first.get(0).done());
		assertTrue(model.expired(1000L + PinRules.DONE_HOLD_MS - 1).isEmpty());
		assertEquals(Collections.singletonList("task:1"), model.expired(1000L + PinRules.DONE_HOLD_MS));
	}

	@Test
	public void theHoldStartsWhenTheDoneLineIsFirstSeenNotWhenLaterFramesComeIn()
	{
		TrackerModel model = new TrackerModel();
		TrackerView view = new TrackerView(Collections.singletonList(task(2, 5, 5, true)));

		model.frame(view, 0L);
		model.frame(view, 3000L);

		assertEquals(Collections.singletonList("task:2"), model.expired(PinRules.DONE_HOLD_MS));
	}

	@Test
	public void aTaskThatLeavesTheViewIsForgotten()
	{
		TrackerModel model = new TrackerModel();
		model.frame(new TrackerView(Collections.singletonList(task(1, 3, 3, true))), 0L);
		model.frame(TrackerView.NONE, 10L);

		assertTrue(model.expired(PinRules.DONE_HOLD_MS * 2).isEmpty());
	}

	@Test
	public void aHatchedEggShowsAGhostLineThatLeavesOnItsOwn()
	{
		TrackerModel model = new TrackerModel();
		model.frame(new TrackerView(Collections.singletonList(egg(0, Rarity.RARE, 5))), 0L);
		model.hatched("Rare egg", 100L);

		List<TrackerView.Line> shown = model.frame(TrackerView.NONE, 200L);
		assertEquals(Collections.singletonList("Rare egg|Hatched!"), texts(shown));
		assertEquals(Rarity.RARE, shown.get(0).rarity());
		assertEquals(1, model.frame(TrackerView.NONE, 100L + PinRules.DONE_HOLD_MS - 1).size());
		assertTrue(model.frame(TrackerView.NONE, 100L + PinRules.DONE_HOLD_MS).isEmpty());
		assertTrue("a ghost never asks to unpin anything", model.expired(100L + PinRules.DONE_HOLD_MS).isEmpty());
	}

	@Test
	public void ghostsComeAfterTheLiveLines()
	{
		TrackerModel model = new TrackerModel();
		model.hatched("Epic egg", 0L);
		TrackerView view = new TrackerView(Arrays.asList(task(0, 1, 3, false), egg(0, Rarity.RARE, 9)));

		assertEquals(Arrays.asList("Task 0|1 / 3", "Rare egg|9 steps", "Epic egg|Hatched!"),
			texts(model.frame(view, 1L)));
	}

	@Test
	public void clearForgetsEverything()
	{
		TrackerModel model = new TrackerModel();
		model.hatched("Epic egg", 0L);
		model.frame(new TrackerView(Collections.singletonList(task(1, 3, 3, true))), 0L);

		model.clear();

		assertTrue(model.frame(TrackerView.NONE, 1L).isEmpty());
		assertTrue(model.expired(PinRules.DONE_HOLD_MS * 2).isEmpty());
	}

	@Test
	public void theIdsNameATaskSlotOrAnEgg()
	{
		assertEquals(2, TrackerView.taskSlot("task:2"));
		assertEquals(-1, TrackerView.taskSlot("egg:1"));
		assertEquals(-1, TrackerView.taskSlot(null));
	}
}
