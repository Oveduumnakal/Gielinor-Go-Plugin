/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Tasks page: tasks, streak and the scrolling milestone list. */
public class TasksPageTest
{
	private static PanelState state(int milestones)
	{
		TaskView tasks = new TaskView(Arrays.asList(
			new TaskView.Row("Catch 3 Ranged creatures", 1, 3, false, "2 Iron jars", false),
			new TaskView.Row("Win 2 battles against champions", 2, 2, true, "1 Revive draught", false),
			new TaskView.Row("Walk 4,000 tiles with a creature in your team", 1200, 4000, false, "Egg", false),
			new TaskView.Row("Weekly: win 10 battles", 3, 10, false, "Rare egg", true)), 3, 4, 3_600_000L,
			86_400_000L);
		List<MilestoneView.Row> rows = new ArrayList<>();
		for (int i = 0; i < milestones; i++)
			rows.add(new MilestoneView.Row("Log " + (25 * (i + 1)) + " species", Math.min(25 * (i + 1), 80),
				25 * (i + 1), "5 Rune jars", i < 3, i > 8));

		MilestoneView view = new MilestoneView(milestones > 3 ? "Log 100 species" : null, 80, 100, rows);
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, Collections.emptyList(),
			Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(), Collections.emptyList(),
			TodayView.NONE, tasks, new EnumMap<>(ItemType.class)).withGoals(view, EliteView.NONE);
	}

	private static PartyRig page(PanelState state, int width, int height)
	{
		PartyRig rig = new PartyRig();
		rig.width = width;
		rig.height = height;
		rig.publish(state, true);
		rig.open();
		rig.shell.showTab(WindowTab.TASKS, rig.now);
		rig.paint();
		return rig;
	}

	@Test
	public void theTasksAndTheStreakCarryTheirTooltips()
	{
		PartyRig rig = page(state(5), 765, 497);

		assertEquals("Reward: 2 Iron jars", rig.hit("tasks:row:0").tip());
		assertEquals("Reward: Rare egg", rig.hit("tasks:row:3").tip());
		String streak = rig.hit(TasksPage.ID_STREAK).tip();
		assertTrue(streak.contains("Missing a day costs nothing"));
	}

	@Test
	public void aLineSeparatesTheDailyTasksFromTheWeeklyOne()
	{
		TasksPage tasks = new TasksPage(null);
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		PanelState panel = state(5);
		tasks.layout(new Rectangle(0, 0, 700, 400), style, panel);
		Rectangle line = tasks.divider();
		assertTrue(line.width > 0 && line.height > 0);
		PartyRig rig = page(panel, 765, 497);
		Rectangle weekly = rig.hit("tasks:row:3").bounds();
		Rectangle lastDaily = rig.hit("tasks:row:2").bounds();
		assertTrue(lastDaily.y + lastDaily.height <= weekly.y);
	}

	@Test
	public void theHeadingsCountDownToTheResets()
	{
		PartyRig rig = page(state(5), 765, 497);
		List<String> words = new ArrayList<>();
		rig.drawn.clear();
		rig.paint();
		for (PartyRig.Drawn d : rig.drawn)
			words.add(d.text);

		assertTrue(words.toString(), words.contains("Tasks · resets in 1h 0m"));
		assertTrue(words.toString(), words.contains("Weekly · resets in 1d 0h"));
		assertTrue(words.toString(), words.contains("Day 3 · egg in 4 days"));
		assertTrue(words.toString(), words.contains("Done"));
	}

	@Test
	public void theMilestoneListScrollsWithTheWheelAndTheBar()
	{
		PartyRig rig = page(state(30), 535, 348);
		ScrollList list = scroll(rig);
		assertTrue(list.scrollable());
		assertEquals(0, list.offset());

		Rectangle view = list.viewport();
		rig.shell.onWheel(view.x + 5, view.y + 5, 2);
		assertTrue("the wheel scrolls", list.offset() > 0);

		Rectangle thumb = list.thumbBounds();
		int before = list.offset();
		rig.shell.onPressed(thumb.x + 1, thumb.y + 1, MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onDragged(thumb.x + 1, thumb.y + 21);
		rig.shell.onReleased(thumb.x + 1, thumb.y + 21, MouseEvent.BUTTON1, rig.now);
		assertTrue("dragging the handle scrolls", list.offset() > before);
	}

	@Test
	public void aClickOnTheBarsGrooveDoesNotClickTheRowUnderIt()
	{
		PartyRig rig = page(state(30), 535, 348);
		ScrollList list = scroll(rig);
		Rectangle track = list.trackBounds();

		rig.shell.onPressed(track.x + 1, track.y + track.height - 2, MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onReleased(track.x + 1, track.y + track.height - 2, MouseEvent.BUTTON1, rig.now);

		assertTrue("the groove pages the list", list.offset() > 0);
		assertTrue(rig.calls.isEmpty());
	}

	/**
	 * Every string fits its box and starts inside the page area. A string of a list row at the foot may reach below the
	 * page area: the list clips the graphics to its viewport, so the part below is never seen.
	 */
	@Test
	public void everyStringStaysInsideItsBoxAtEverySize()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : new int[][] {{535, 348}, {765, 497}, {1344, 874}})
			for (int milestones : new int[] {0, 3, 30})
			{
				PartyRig rig = page(state(milestones), canvas[0], canvas[1]);
				rig.drawn.clear();
				rig.paint();
				Rectangle body = WindowLayout.of(rig.width, rig.height).body;
				for (PartyRig.Drawn d : rig.drawn)
				{
					boolean inside = d.box.x >= body.x && d.box.x + d.box.width <= body.x + body.width
						&& d.box.y >= body.y;
					if (d.width > d.box.width || d.height > d.box.height + 1 || !inside)
						problems.append(canvas[0] + " " + d + "\n");
				}
			}

		assertEquals(problems.toString(), "", problems.toString());
	}

	private static ScrollList scroll(PartyRig rig)
	{
		return rig.pages.tasksPage().list();
	}

	private static PanelState pinned(PanelState base, int... slots)
	{
		List<TaskView.Row> rows = new ArrayList<>(base.tasks().rows());
		for (int slot : slots)
			rows.set(slot, rows.get(slot).withPinned(true));

		TaskView tasks = new TaskView(rows, 3, 4, 3_600_000L, 86_400_000L);
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, Collections.emptyList(),
			Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(), Collections.emptyList(),
			TodayView.NONE, tasks, new EnumMap<>(ItemType.class)).withGoals(base.milestones(), EliteView.NONE);
	}

	private static TrackerView lines(int count)
	{
		List<TrackerView.Line> list = new ArrayList<>();
		for (int i = 0; i < count; i++)
			list.add(new TrackerView.Line(TrackerView.Kind.EGG, "egg:" + i, "Rare egg", "5 steps", 0.5, Rarity.RARE,
				false));

		return new TrackerView(list);
	}

	@Test
	public void everyTaskRowHasAFocusablePinButton()
	{
		PartyRig rig = page(state(5), 765, 497);

		for (int i = 0; i < 4; i++)
		{
			Hit hit = rig.hit("tasks:pin:" + i);
			assertEquals(Hit.Kind.BUTTON, hit.kind());
			assertTrue(hit.order() >= WindowShell.PAGE_ORDER);
			assertTrue(hit.bounds().intersection(rig.hit("tasks:row:" + i).bounds()).isEmpty());
		}

		assertEquals("Pin to the tracker", rig.hit("tasks:pin:0").tip());
		assertTrue(rig.hit("tasks:pin:0").enabled());
		assertEquals("Pin to the tracker", rig.hit("tasks:pin:3").tip());
	}

	@Test
	public void aPinnedPinLooksClearlyDifferentFromAnUnpinnedOne()
	{
		PartyRig off = page(state(5), 765, 497);
		PartyRig on = page(pinned(state(5), 0), 765, 497);
		Rectangle box = off.hit("tasks:pin:0").bounds();
		BufferedImage before = off.paint();
		BufferedImage after = on.paint();
		int differing = 0;
		for (int x = box.x; x < box.x + box.width; x++)
			for (int y = box.y; y < box.y + box.height; y++)
				if (before.getRGB(x, y) != after.getRGB(x, y))
					differing++;

		assertTrue("pixels differing: " + differing, differing > box.width * box.height / 8);
	}

	@Test
	public void aDoneTaskHasItsPinDisabledWithTheReason()
	{
		PartyRig rig = page(state(5), 765, 497);

		Hit hit = rig.hit("tasks:pin:1");

		assertFalse(hit.enabled());
		assertEquals("Already done", hit.tip());
		rig.click("tasks:pin:1");
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void pressingPinAsksTheGameToPinThatTask()
	{
		PartyRig rig = page(state(5), 765, 497);

		rig.click("tasks:pin:2");

		assertEquals(Collections.singletonList("pinTask 2 true"), rig.calls);
	}

	@Test
	public void aPinnedTaskOffersUnpinEvenWhenTheTrackerIsFull()
	{
		PartyRig rig = page(pinned(state(5), 0).withTracker(lines(5)), 765, 497);

		assertEquals("Unpin from the tracker", rig.hit("tasks:pin:0").tip());
		assertTrue(rig.hit("tasks:pin:0").enabled());
		rig.click("tasks:pin:0");

		assertEquals(Collections.singletonList("pinTask 0 false"), rig.calls);
	}

	@Test
	public void aFullTrackerDisablesTheOtherPinsWithTheReason()
	{
		PartyRig rig = page(state(5).withTracker(lines(5)), 765, 497);

		assertFalse(rig.hit("tasks:pin:2").enabled());
		assertEquals("Tracker is full (5)", rig.hit("tasks:pin:2").tip());
		rig.click("tasks:pin:2");
		assertTrue(rig.calls.isEmpty());
	}
}
