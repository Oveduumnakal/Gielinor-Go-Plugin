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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** The Storage views: the View button, the Box grid, the By species list, and Select mode in each. */
public class StorageViewsTest
{
	private static final int[][] CANVASES = {{535, 348}, {765, 497}, {1344, 874}};
	private static final String[] NAMES = {"Goblin", "Cow", "Imp", "Bruise Blamish Snail", "Man"};

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

	private static PartyRig page(List<CreatureView> storage, int width, int height)
	{
		PartyRig rig = new PartyRig();
		rig.width = width;
		rig.height = height;
		rig.publish(state(storage), true);
		rig.open();
		rig.shell.showTab(WindowTab.STORAGE, rig.now);
		rig.paint();
		return rig;
	}

	private static PartyRig page(List<CreatureView> storage, StorageMode mode)
	{
		PartyRig rig = page(storage, 765, 497);
		while (rig.pages.storagePage().mode() != mode)
			rig.click(StoragePage.ID_VIEW);

		return rig;
	}

	private static List<String> rowIds(PartyRig rig)
	{
		List<String> ids = new ArrayList<>();
		for (String id : rig.ids())
		{
			if (id.startsWith(StoragePage.ROW_PREFIX))
				ids.add(id);
		}

		return ids;
	}

	private static List<String> idsWith(PartyRig rig, String prefix)
	{
		List<String> ids = new ArrayList<>();
		for (String id : rig.ids())
		{
			if (id.startsWith(prefix))
				ids.add(id);
		}

		return ids;
	}

	@Test
	public void theViewButtonCyclesListBoxAndBySpecies()
	{
		PartyRig rig = page(stored(10), 765, 497);
		StoragePage storage = rig.pages.storagePage();

		assertEquals(StorageMode.LIST, storage.mode());
		rig.click(StoragePage.ID_VIEW);
		assertEquals(StorageMode.BOX, storage.mode());
		rig.click(StoragePage.ID_VIEW);
		assertEquals(StorageMode.SPECIES, storage.mode());
		rig.click(StoragePage.ID_VIEW);
		assertEquals(StorageMode.LIST, storage.mode());
	}

	@Test
	public void theViewButtonTellsTheCurrentViewAndSitsLeftOfSort()
	{
		PartyRig rig = page(stored(3), StorageMode.BOX);

		Hit view = rig.hit(StoragePage.ID_VIEW);
		Hit sort = rig.hit(StoragePage.ID_SORT);

		assertTrue(view.tip().contains("Now: Box"));
		assertTrue(view.bounds().x + view.bounds().width <= sort.bounds().x);
	}

	@Test
	public void switchingViewKeepsTheSortFilterAndTicks()
	{
		PartyRig rig = page(stored(6), 765, 497);
		StoragePage storage = rig.pages.storagePage();
		List<CreatureView> now = state(stored(6)).storage();
		storage.setQuery(storage.query().withSort(StorageQuery.Sort.LEVEL), now);
		rig.paint();
		rig.click(StoragePage.ID_SELECT);
		rig.click(StoragePage.rowId(2));

		rig.click(StoragePage.ID_VIEW);
		rig.click(StoragePage.ID_VIEW);

		assertEquals(StorageQuery.Sort.LEVEL, storage.query().sort());
		assertEquals(Collections.singletonList(2), new ArrayList<>(storage.ticked()));
		assertTrue(storage.selecting());
	}

	@Test
	public void theBoxHasOneHitPerCreatureWithTheSameIdAsAListRow()
	{
		PartyRig rig = page(stored(12), StorageMode.BOX);

		List<String> rows = rowIds(rig);

		assertEquals(12, rows.size());
		assertTrue(rows.contains(StoragePage.rowId(0)));
		assertTrue(rows.contains(StoragePage.rowId(11)));
		assertEquals(Hit.Kind.ROW, rig.hit(StoragePage.rowId(5)).kind());
		String tip = rig.hit(StoragePage.rowId(5)).tip();
		assertTrue(tip.contains("Click to move to your team"));
	}

	@Test
	public void theBoxHasEightColumnsAndEmptyCellsAreDropTargetsOnly()
	{
		PartyRig rig = page(stored(10), StorageMode.BOX);

		Rectangle first = rig.hit(StoragePage.rowId(0)).bounds();
		Rectangle eighth = rig.hit(StoragePage.rowId(7)).bounds();
		Rectangle ninth = rig.hit(StoragePage.rowId(8)).bounds();

		assertEquals(first.y, eighth.y);
		assertEquals(first.x, ninth.x);
		assertTrue(ninth.y > first.y);
		assertTrue(eighth.x > first.x);
		Hit empty = rig.hit(StoragePage.SLOT_PREFIX + 10);
		assertEquals(Hit.Kind.AREA, empty.kind());
		assertFalse(empty.focusable());
		assertEquals(Collections.emptyList(), idsWith(rig, StoragePage.SLOT_PREFIX + 9));
	}

	@Test
	public void aLeftClickOnABoxCellStillMovesTheCreatureToTheTeam()
	{
		PartyRig rig = page(stored(10), StorageMode.BOX);

		rig.click(StoragePage.rowId(3));

		assertEquals(Collections.singletonList("moveToTeam STORAGE#3"), rig.calls);
	}

	@Test
	public void theBoxMenuOpensOnARightClick()
	{
		PartyRig rig = page(stored(10), StorageMode.BOX);

		rig.click(StoragePage.rowId(1), MouseEvent.BUTTON3);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals("Move to team is second, after Summary", Collections.singletonList("moveToTeam STORAGE#1"),
			rig.calls);
	}

	@Test
	public void bySpeciesHasAHeadingPerSpeciesAndTheRowsKeepTheirPlaces()
	{
		PartyRig rig = page(stored(10), StorageMode.SPECIES);

		List<String> headings = idsWith(rig, StoragePage.GROUP_PREFIX);

		assertEquals(3, headings.size());
		Hit first = rig.hit(headings.get(0));
		assertEquals(Hit.Kind.AREA, first.kind());
		assertTrue(first.tip().contains("2 stored"));
		assertTrue(rig.ids().contains(StoragePage.rowId(3)));
		rig.click(StoragePage.rowId(3));
		assertEquals(Collections.singletonList("moveToTeam STORAGE#3"), rig.calls);
	}

	@Test
	public void aHeadingIsShorterThanARow()
	{
		PartyRig rig = page(stored(10), StorageMode.SPECIES);

		List<String> headings = idsWith(rig, StoragePage.GROUP_PREFIX);

		Rectangle heading = rig.hit(headings.get(0)).bounds();
		Rectangle row = rig.hit(StoragePage.rowId(3)).bounds();
		assertTrue(heading.height < row.height);
	}

	@Test
	public void bySpeciesListsCreaturesUnderTheirSpeciesAtoZ()
	{
		PartyRig rig = page(stored(10), StorageMode.SPECIES);

		List<String> ids = rowIds(rig);

		assertEquals(StoragePage.rowId(3), ids.get(0));
		assertEquals(StoragePage.rowId(8), ids.get(1));
	}

	private static List<CreatureView> twoSpecies()
	{
		return new ArrayList<>(Arrays.asList(PartyRig.creature("Bee", 3, 0.9, null),
			PartyRig.creature("Ant", 4, 0.9, null), PartyRig.creature("Bee", 5, 0.9, null),
			PartyRig.creature("Ant", 6, 0.9, null)));
	}

	@Test
	public void inSelectModeAHeadingTicksAndUnticksItsWholeGroup()
	{
		PartyRig rig = page(twoSpecies(), StorageMode.SPECIES);
		rig.click(StoragePage.ID_SELECT);
		String ants = idsWith(rig, StoragePage.GROUP_PREFIX).get(0);
		StoragePage storage = rig.pages.storagePage();
		assertEquals(Hit.Kind.BUTTON, rig.hit(ants).kind());
		assertTrue(rig.hit(ants).focusable());

		rig.click(ants);
		assertEquals(Arrays.asList(1, 3), new ArrayList<>(storage.ticked()));
		assertTrue(rig.calls.isEmpty());

		rig.click(ants);
		assertTrue(storage.ticked().isEmpty());
		rig.click(StoragePage.rowId(1));
		rig.click(ants);
		assertEquals("a part ticked group is ticked all the way", Arrays.asList(1, 3),
			new ArrayList<>(storage.ticked()));
	}

	@Test
	public void outsideSelectModeAHeadingDoesNothing()
	{
		PartyRig rig = page(stored(10), StorageMode.SPECIES);

		rig.click(idsWith(rig, StoragePage.GROUP_PREFIX).get(0));

		StoragePage storage = rig.pages.storagePage();
		assertTrue(rig.calls.isEmpty());
		assertTrue(storage.ticked().isEmpty());
	}

	@Test
	public void selectModeTicksAndReleasesThroughTheSameConfirmInEveryView()
	{
		for (StorageMode mode : StorageMode.values())
		{
			PartyRig rig = page(stored(4), mode);
			rig.click(StoragePage.ID_SELECT);
			rig.click(StoragePage.rowId(1));
			rig.click(StoragePage.rowId(3));
			StoragePage storage = rig.pages.storagePage();
			assertTrue(mode.label(), rig.calls.isEmpty());
			assertEquals(mode.label(), Arrays.asList(1, 3), new ArrayList<>(storage.ticked()));

			rig.click(StoragePage.ID_RELEASE);
			assertTrue(mode.label(), rig.calls.isEmpty());
			rig.key(KeyEvent.VK_RIGHT);
			rig.key(KeyEvent.VK_ENTER);

			assertEquals(mode.label(), Collections.singletonList("releaseMany [STORAGE#1, STORAGE#3]"), rig.calls);
		}
	}

	@Test
	public void selectingDoesNotChangeTheRowOrCellRectanglesInAnyView()
	{
		for (int[] canvas : CANVASES)
		{
			for (StorageMode mode : StorageMode.values())
			{
				PartyRig rig = page(stored(10), canvas[0], canvas[1]);
				while (rig.pages.storagePage().mode() != mode)
					rig.click(StoragePage.ID_VIEW);

				List<Rectangle> before = boxes(rig);

				rig.click(StoragePage.ID_SELECT);
				assertEquals(canvas[0] + " " + mode.label() + " selecting", before, boxes(rig));

				rig.click(StoragePage.ID_SELECT);
				assertEquals(canvas[0] + " " + mode.label() + " done", before, boxes(rig));
			}
		}
	}

	private static List<Rectangle> boxes(PartyRig rig)
	{
		List<Rectangle> boxes = new ArrayList<>();
		for (Hit hit : rig.shell.hits())
		{
			if (hit.id().startsWith(StoragePage.ROW_PREFIX) || hit.id().startsWith(StoragePage.GROUP_PREFIX)
				|| hit.id().startsWith(StoragePage.SLOT_PREFIX))
				boxes.add(hit.bounds());
		}

		return boxes;
	}

	@Test
	public void anEmptyStorageSaysSoInEveryView()
	{
		for (StorageMode mode : StorageMode.values())
		{
			PartyRig rig = page(new ArrayList<>(), mode);

			rig.drawn.clear();
			rig.paint();

			boolean said = false;
			for (PartyRig.Drawn d : rig.drawn)
				said |= d.text.startsWith("Storage is empty");

			assertTrue(mode.label(), said);
			assertTrue(mode.label(), rowIds(rig).isEmpty());
		}
	}

	@Test
	public void everyStringStaysInsideItsBoxInEveryViewAtEverySize()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : CANVASES)
		{
			for (StorageMode mode : StorageMode.values())
			{
				for (int count : new int[] {0, 3, 40, 120})
				{
					for (boolean selecting : new boolean[] {false, true})
					{
						List<CreatureView> list = stored(count);
						if (count > 0)
							list.set(0, PartyRig.creature("Bruise Blamish Snail", 99, 0.1, "A very long nickname"));

						PartyRig rig = page(list, canvas[0], canvas[1]);
						while (rig.pages.storagePage().mode() != mode)
							rig.click(StoragePage.ID_VIEW);

						if (selecting)
						{
							rig.click(StoragePage.ID_SELECT);
							if (count > 0)
								rig.click(StoragePage.rowId(0));
						}

						rig.drawn.clear();
						rig.paint();
						Rectangle body = WindowLayout.of(rig.width, rig.height).body;
						for (PartyRig.Drawn d : rig.drawn)
						{
							if (d.width > d.box.width || d.height > d.box.height + 1 || !body.contains(d.box))
								problems.append(canvas[0] + " " + mode + " " + d + "\n");
						}

						for (Hit hit : rig.shell.hits())
						{
							if (hit.id().startsWith("storage:") && !body.contains(hit.bounds()))
								problems.append(canvas[0] + " " + mode + " hit " + hit.id() + "\n");
						}
					}
				}
			}
		}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void theBoxCardFillsWhenACreatureIsHighlighted()
	{
		PartyRig rig = page(stored(10), StorageMode.BOX);
		rig.shell.focus().highlightKey(StoragePage.rowId(3));

		rig.drawn.clear();
		rig.paint();

		boolean named = false;
		for (PartyRig.Drawn d : rig.drawn)
			named |= d.text.contains("Click to move");

		assertTrue("the card shows the click hint", named);
	}

	@Test
	public void theBoxScrollsByTheWheelOnceThereAreMoreThanFourRows()
	{
		PartyRig rig = page(stored(100), StorageMode.BOX);
		ScrollList list = rig.pages.storagePage().list();

		assertTrue(list.scrollable());
		Rectangle before = rig.hit(StoragePage.rowId(0)).bounds();
		list.setOffset(list.maxOffset());
		rig.paint();

		assertTrue(rig.ids().contains(StoragePage.rowId(99)));
		assertFalse(rig.ids().contains(StoragePage.rowId(0)));
		assertNotEquals(before, rig.hit(StoragePage.rowId(99)).bounds());
	}

	@Test
	public void theArrowKeysMoveTheBoxHighlightByCellAndRow()
	{
		PartyRig rig = page(stored(20), StorageMode.BOX);
		rig.shell.focus().highlightKey(StoragePage.rowId(9));
		rig.paint();

		rig.key(KeyEvent.VK_RIGHT);
		assertEquals(StoragePage.rowId(10), rig.shell.focus().highlight());
		rig.key(KeyEvent.VK_LEFT);
		rig.key(KeyEvent.VK_LEFT);
		assertEquals(StoragePage.rowId(8), rig.shell.focus().highlight());
		rig.key(KeyEvent.VK_DOWN);
		assertEquals(StoragePage.rowId(16), rig.shell.focus().highlight());
		rig.key(KeyEvent.VK_UP);
		rig.key(KeyEvent.VK_UP);
		assertEquals(StoragePage.rowId(0), rig.shell.focus().highlight());
	}

	@Test
	public void leftInTheFirstColumnChangesTabAndRightAtTheEndOfARowDoes()
	{
		PartyRig rig = page(stored(20), StorageMode.BOX);
		rig.shell.focus().highlightKey(StoragePage.rowId(8));
		rig.paint();

		rig.key(KeyEvent.VK_LEFT);

		assertNotEquals("the window changed tab", WindowTab.STORAGE, rig.shell.tab());
	}

	@Test
	public void bySpeciesUpAndDownFollowTheGroupedOrder()
	{
		PartyRig rig = page(stored(10), StorageMode.SPECIES);
		rig.shell.focus().highlightKey(StoragePage.rowId(3));
		rig.paint();

		rig.key(KeyEvent.VK_DOWN);

		assertEquals("the next row under the heading is the same species", StoragePage.rowId(8),
			rig.shell.focus().highlight());
	}
}
