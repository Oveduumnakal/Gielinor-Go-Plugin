/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Dragging and carrying creatures in the Storage Box: swaps, drops on empty cells, refusals, keys and scrolling. */
public class StorageBoxDragTest
{
	private static final int CARRY_ITEM = 5;
	private static final int END_ITEM = 6;
	private static final String[] NAMES = {"Goblin", "Cow", "Imp", "Man", "Rat"};

	private static List<CreatureView> stored(int count)
	{
		List<CreatureView> list = new ArrayList<>();
		for (int i = 0; i < count; i++)
			list.add(PartyRig.creature(NAMES[i % NAMES.length], 3 + i, 0.9, null));

		return list;
	}

	private static PanelState state(List<CreatureView> storage)
	{
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, PartyRig.team(2), storage,
			Collections.emptyList(), 0, 0, Collections.emptyList(), Collections.emptyList(), TodayView.NONE,
			TaskView.NONE, new EnumMap<>(ItemType.class));
	}

	private static PartyRig box(List<CreatureView> storage, StorageMode mode)
	{
		PartyRig rig = new PartyRig();
		rig.publish(state(storage), true);
		rig.open();
		rig.shell.showTab(WindowTab.STORAGE, rig.now);
		rig.paint();
		while (rig.pages.storagePage().mode() != mode)
			rig.click(StoragePage.ID_VIEW);

		return rig;
	}

	private static PartyRig box(int count)
	{
		return box(stored(count), StorageMode.BOX);
	}

	private static int[] centre(PartyRig rig, String id)
	{
		Rectangle b = rig.hit(id).bounds();
		return new int[] {b.x + b.width / 2, b.y + b.height / 2};
	}

	private static void drag(PartyRig rig, String from, String to)
	{
		int[] a = centre(rig, from);
		int[] b = centre(rig, to);
		drag(rig, a[0], a[1], b[0], b[1]);
	}

	private static void drag(PartyRig rig, int x0, int y0, int x1, int y1)
	{
		rig.shell.onPressed(x0, y0, MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onDragged((x0 + x1) / 2, (y0 + y1) / 2);
		rig.shell.onDragged(x1, y1);
		rig.paint();
		rig.shell.onReleased(x1, y1, MouseEvent.BUTTON1, rig.now);
		rig.paint();
	}

	private static boolean said(PartyRig rig, String text)
	{
		rig.drawn.clear();
		rig.paint();
		for (PartyRig.Drawn d : rig.drawn)
		{
			if (d.text.startsWith(text))
				return true;
		}

		return false;
	}

	private static void sortByLevel(PartyRig rig, int count)
	{
		StoragePage storage = rig.pages.storagePage();
		StorageQuery sorted = storage.query().withSort(StorageQuery.Sort.LEVEL);
		storage.setQuery(sorted, state(stored(count)).storage());
		rig.paint();
	}

	@Test
	public void draggingOneCreatureOntoAnotherAsksForASwapAndNotAMove()
	{
		PartyRig rig = box(10);

		drag(rig, StoragePage.rowId(1), StoragePage.rowId(4));

		assertEquals(Collections.singletonList("arrangeStorage STORAGE#1 STORAGE#4"), rig.calls);
	}

	@Test
	public void draggingOntoAnEmptyCellAsksToPutItLast()
	{
		PartyRig rig = box(10);

		drag(rig, StoragePage.rowId(1), StoragePage.SLOT_PREFIX + 12);

		assertEquals(Collections.singletonList("arrangeStorage STORAGE#1 null"), rig.calls);
	}

	@Test
	public void aPressThatBarelyMovesIsStillAClick()
	{
		PartyRig rig = box(10);
		int[] at = centre(rig, StoragePage.rowId(2));

		drag(rig, at[0], at[1], at[0] + 2, at[1] + 1);

		assertEquals(Collections.singletonList("moveToTeam STORAGE#2"), rig.calls);
	}

	@Test
	public void droppingOnTheSameCellOrOutsideTheGridDoesNothing()
	{
		PartyRig rig = box(10);
		int[] at = centre(rig, StoragePage.rowId(2));

		drag(rig, at[0], at[1], at[0] + 12, at[1]);
		drag(rig, at[0], at[1], 3, 3);

		assertTrue(rig.calls.toString(), rig.calls.isEmpty());
	}

	@Test
	public void pressingAnEmptyCellDoesNothing()
	{
		PartyRig rig = box(10);
		int[] at = centre(rig, StoragePage.SLOT_PREFIX + 12);

		drag(rig, at[0], at[1], at[0], at[1] + 30);
		rig.click(StoragePage.SLOT_PREFIX + 12);

		assertTrue(rig.calls.toString(), rig.calls.isEmpty());
	}

	@Test
	public void whenSortedADragDoesNothingAndTheLineSaysWhy()
	{
		PartyRig rig = box(10);
		sortByLevel(rig, 10);
		assertFalse(rig.pages.storagePage().arrangeAllowed());

		drag(rig, StoragePage.rowId(1), StoragePage.rowId(4));

		assertTrue(rig.calls.toString(), rig.calls.isEmpty());
		assertTrue(said(rig, StoragePage.REFUSE_SORT));
	}

	@Test
	public void theHintGoesWhenTheNextPressStarts()
	{
		PartyRig rig = box(10);
		sortByLevel(rig, 10);
		drag(rig, StoragePage.rowId(1), StoragePage.rowId(4));
		assertTrue(said(rig, StoragePage.REFUSE_SORT));

		rig.click(StoragePage.rowId(1));

		assertFalse(said(rig, StoragePage.REFUSE_SORT));
	}

	@Test
	public void withAFilterOnADragDoesNothing()
	{
		PartyRig rig = box(10);
		StoragePage storage = rig.pages.storagePage();
		int style = CombatType.SLASH.ordinal();
		StorageQuery withFilter = StorageQuery.NONE.cycled(StorageQuery.Facet.STYLE, style);
		storage.setQuery(withFilter, state(stored(10)).storage());
		rig.paint();
		assertTrue(withFilter.active());
		assertFalse(storage.arrangeAllowed());
		List<String> rows = new ArrayList<>();
		for (String id : rig.ids())
		{
			if (id.startsWith(StoragePage.ROW_PREFIX))
				rows.add(id);
		}

		drag(rig, rows.get(0), rows.get(1));

		assertTrue(rig.calls.toString(), rig.calls.isEmpty());
	}

	@Test
	public void inSelectModeADragNeitherTicksNorArranges()
	{
		PartyRig rig = box(10);
		rig.click(StoragePage.ID_SELECT);

		drag(rig, StoragePage.rowId(1), StoragePage.rowId(4));

		StoragePage storage = rig.pages.storagePage();
		assertTrue(rig.calls.toString(), rig.calls.isEmpty());
		assertTrue(storage.ticked().isEmpty());
		assertTrue(said(rig, StoragePage.REFUSE_SELECT));
	}

	@Test
	public void inTheListAndBySpeciesADragNeverArranges()
	{
		for (StorageMode mode : new StorageMode[] {StorageMode.LIST, StorageMode.SPECIES})
		{
			PartyRig rig = box(stored(4), mode);
			List<String> rows = new ArrayList<>();
			for (String id : rig.ids())
			{
				if (id.startsWith(StoragePage.ROW_PREFIX))
					rows.add(id);
			}

			drag(rig, rows.get(0), rows.get(1));

			assertFalse(mode.label(), rig.calls.toString().contains("arrangeStorage"));
		}
	}

	@Test
	public void aDragAtTheBottomEdgeScrollsTheGrid()
	{
		PartyRig rig = box(100);
		ScrollList list = rig.pages.storagePage().list();
		int[] at = centre(rig, StoragePage.rowId(0));
		Rectangle view = list.viewport();

		rig.shell.onPressed(at[0], at[1], MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onDragged(at[0], view.y + view.height - 3);
		for (int i = 0; i < 6; i++)
			rig.paint();

		assertTrue("the grid scrolled down", list.offset() > 0);
		int down = list.offset();
		rig.shell.onDragged(at[0], view.y + 3);
		for (int i = 0; i < 6; i++)
			rig.paint();

		assertTrue("and back up", list.offset() < down);
		rig.shell.onReleased(at[0], view.y + 3, MouseEvent.BUTTON1, rig.now);
	}

	@Test
	public void aPlainPressInTheMiddleDoesNotScrollTheGrid()
	{
		PartyRig rig = box(100);
		ScrollList list = rig.pages.storagePage().list();
		int[] at = centre(rig, StoragePage.rowId(9));

		rig.shell.onPressed(at[0], at[1], MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onDragged(at[0], at[1] + 40);
		rig.paint();
		rig.shell.onReleased(at[0], at[1] + 40, MouseEvent.BUTTON1, rig.now);

		assertEquals("a drag on a cell is never a scroll", 0, list.offset());
	}

	/**
	 * Chooses the nth enabled item of a creature's menu: Summary, Move to team, Rename, Moves, Release (Use item is
	 * greyed out with no items), then the two Box items.
	 */
	private static void menuItem(PartyRig rig, String id, int item)
	{
		rig.click(id, MouseEvent.BUTTON3);
		for (int i = 0; i <= item; i++)
			rig.key(KeyEvent.VK_DOWN);

		rig.key(KeyEvent.VK_ENTER);
	}

	@Test
	public void theBoxMenuOffersMoveWithinBoxAndMoveToTheEndOnlyWhenArranging()
	{
		PartyRig rig = box(10);

		menuItem(rig, StoragePage.rowId(2), END_ITEM);

		assertEquals(Collections.singletonList("arrangeStorage STORAGE#2 null"), rig.calls);
	}

	@Test
	public void moveWithinBoxCarriesTheCreatureAndEnterPutsItDown()
	{
		PartyRig rig = box(20);
		menuItem(rig, StoragePage.rowId(2), CARRY_ITEM);
		StoragePage storage = rig.pages.storagePage();
		assertEquals(2, storage.carrying());
		assertTrue(said(rig, "Carrying"));

		rig.shell.focus().highlightKey(StoragePage.rowId(2));
		rig.paint();
		rig.key(KeyEvent.VK_RIGHT);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("arrangeStorage STORAGE#2 STORAGE#11"), rig.calls);
		assertEquals(-1, storage.carrying());
	}

	@Test
	public void escapeCancelsCarryingBeforeItLeavesTheWindow()
	{
		PartyRig rig = box(20);
		menuItem(rig, StoragePage.rowId(2), CARRY_ITEM);
		StoragePage storage = rig.pages.storagePage();
		assertEquals(1, rig.pages.depth());

		rig.key(KeyEvent.VK_ESCAPE);

		assertEquals(-1, storage.carrying());
		assertEquals(0, rig.pages.depth());
		assertTrue("the window stayed open", rig.shell.isOpen());
		assertTrue(rig.calls.isEmpty());
		rig.key(KeyEvent.VK_ESCAPE);
		assertFalse("the next Esc closes it", rig.shell.isOpen());
	}

	@Test
	public void aClickWhileCarryingPutsTheCreatureThereInsteadOfMovingItToTheTeam()
	{
		PartyRig rig = box(20);
		menuItem(rig, StoragePage.rowId(2), CARRY_ITEM);

		rig.click(StoragePage.rowId(5));

		assertEquals(Collections.singletonList("arrangeStorage STORAGE#2 STORAGE#5"), rig.calls);
		assertEquals(-1, rig.pages.storagePage().carrying());
	}

	@Test
	public void enterOnTheCarriedCreatureAloneCancels()
	{
		PartyRig rig = box(20);
		menuItem(rig, StoragePage.rowId(2), CARRY_ITEM);
		rig.shell.focus().highlightKey(StoragePage.rowId(2));
		rig.paint();

		rig.key(KeyEvent.VK_ENTER);

		assertTrue(rig.calls.isEmpty());
		assertEquals(-1, rig.pages.storagePage().carrying());
	}

	@Test
	public void carryingStopsWhenTheViewOrTheSortChanges()
	{
		PartyRig rig = box(20);
		menuItem(rig, StoragePage.rowId(2), CARRY_ITEM);
		rig.click(StoragePage.ID_VIEW);

		assertEquals(-1, rig.pages.storagePage().carrying());
		assertEquals(Arrays.asList(), rig.calls);
	}
}
