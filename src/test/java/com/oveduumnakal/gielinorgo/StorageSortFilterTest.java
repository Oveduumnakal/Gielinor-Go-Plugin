/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Sort and filter on the Storage page: real places under a changed order, ticks, chips, memory and the keys. */
public class StorageSortFilterTest
{
	private static final int[][] CANVASES = {{535, 348}, {765, 497}, {1344, 874}};
	private static final int SORT_LEVEL = 1;
	private static final int SORT_RARITY = 3;
	private static final int FILTER_STYLE = 2;
	private static final int FILTER_GILDED = 6;

	private static CreatureView make(String name, int level, CombatType type, Rarity rarity, boolean gilded)
	{
		return new CreatureView(name, level, 0.4, 0.9, rarity, type, gilded, null,
			new StatRolls(3, -2, 0, 5, Temperament.BRAVE), 0, Arrays.asList(PartyRig.SLASH, PartyRig.BASH),
			Collections.emptyList(), 3).withGender(Gender.MALE);
	}

	private static List<CreatureView> mixed()
	{
		return new ArrayList<>(Arrays.asList(
			make("Goblin", 10, CombatType.CRUSH, Rarity.COMMON, false),
			make("Cow", 40, CombatType.SLASH, Rarity.UNCOMMON, true),
			make("Imp", 25, CombatType.CRUSH, Rarity.EPIC, false),
			make("Man", 5, CombatType.STAB, Rarity.COMMON, false),
			make("Rat", 60, CombatType.CRUSH, Rarity.RARE, true),
			make("Bat", 30, CombatType.MAGIC, Rarity.COMMON, false)));
	}

	private static List<CreatureView> many(int count)
	{
		List<CreatureView> list = new ArrayList<>();
		for (int i = 0; i < count; i++)
			list.add(make("Goblin " + i, 1 + i, CombatType.CRUSH, Rarity.COMMON, false));

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

	private static PartyRig page(List<CreatureView> storage)
	{
		return page(storage, 765, 497);
	}

	private static void open(PartyRig rig, String buttonId)
	{
		Rectangle b = rig.hit(buttonId).bounds();
		rig.shell.onMoved(b.x + b.width / 2, b.y + b.height / 2, rig.now);
		rig.click(buttonId);
	}

	private static void choose(PartyRig rig, String buttonId, int item)
	{
		open(rig, buttonId);
		for (int i = 0; i <= item; i++)
			rig.key(KeyEvent.VK_DOWN);

		rig.key(KeyEvent.VK_ENTER);
	}

	private static void chooseInSubmenu(PartyRig rig, String buttonId, int item, int leaf)
	{
		open(rig, buttonId);
		for (int i = 0; i <= item; i++)
			rig.key(KeyEvent.VK_DOWN);

		rig.key(KeyEvent.VK_ENTER);
		for (int i = 0; i < leaf; i++)
			rig.key(KeyEvent.VK_DOWN);

		rig.key(KeyEvent.VK_ENTER);
	}

	private static List<Integer> shownPlaces(PartyRig rig)
	{
		List<Integer> places = new ArrayList<>();
		for (int place : rig.pages.storagePage().shownPlaces())
			places.add(place);

		return places;
	}

	private static StorageQuery query(PartyRig rig)
	{
		return rig.pages.storagePage().query();
	}

	private static String label(PartyRig rig, int index)
	{
		List<StorageQuery.Criterion> criteria = query(rig).criteria();
		return criteria.get(index).label();
	}

	private static String tip(PartyRig rig, String id)
	{
		return rig.hit(id).tip();
	}

	private static List<String> words(PartyRig rig)
	{
		List<String> words = new ArrayList<>();
		rig.drawn.clear();
		rig.paint();
		for (PartyRig.Drawn d : rig.drawn)
			words.add(d.text);

		return words;
	}

	private static void chooseYes(PartyRig rig)
	{
		rig.key(KeyEvent.VK_RIGHT);
		rig.key(KeyEvent.VK_ENTER);
	}

	@Test
	public void theNewControlsAreButtonsAfterSelect()
	{
		PartyRig rig = page(mixed());

		List<String> ids = rig.ids();

		assertTrue(ids.indexOf(StoragePage.ID_SELECT) < ids.indexOf(StoragePage.ID_FILTER));
		assertTrue(ids.indexOf(StoragePage.ID_FILTER) < ids.indexOf(StoragePage.ID_SORT));
		assertTrue(tip(rig, StoragePage.ID_SORT).startsWith("Sort the list."));
		assertTrue(tip(rig, StoragePage.ID_FILTER).startsWith("Show only some creatures."));
	}

	@Test
	public void sortingByLevelReordersTheRowsButAClickMovesTheRealCreature()
	{
		PartyRig rig = page(mixed());

		choose(rig, StoragePage.ID_SORT, SORT_LEVEL);

		assertEquals(Arrays.asList(4, 1, 5, 2, 0, 3), shownPlaces(rig));
		rig.click(StoragePage.rowId(4));
		assertEquals(Collections.singletonList("moveToTeam STORAGE#4"), rig.calls);
	}

	@Test
	public void sortingAgainFlipsTheDirectionAndBoxOrderGoesBack()
	{
		PartyRig rig = page(mixed());

		choose(rig, StoragePage.ID_SORT, SORT_LEVEL);
		choose(rig, StoragePage.ID_SORT, SORT_LEVEL);
		assertEquals(Arrays.asList(3, 0, 2, 5, 1, 4), shownPlaces(rig));

		choose(rig, StoragePage.ID_SORT, 0);
		assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), shownPlaces(rig));
	}

	@Test
	public void theSortButtonSaysWhichSortIsOn()
	{
		PartyRig rig = page(mixed());
		assertFalse(tip(rig, StoragePage.ID_SORT).contains("Now:"));

		choose(rig, StoragePage.ID_SORT, SORT_RARITY);

		assertTrue(tip(rig, StoragePage.ID_SORT).contains("Now: Rarity, rarest first."));
	}

	@Test
	public void aStyleFilterHidesRowsAndTheirHits()
	{
		PartyRig rig = page(mixed());

		chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());

		assertEquals(Arrays.asList(0, 2, 4), shownPlaces(rig));
		assertTrue(rig.ids().contains(StoragePage.CHIP_PREFIX + 0));
		assertEquals("Crush", label(rig, 0));
	}

	@Test
	public void filteringAndSortingTogetherKeepRealPlaces()
	{
		PartyRig rig = page(mixed());
		chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());

		choose(rig, StoragePage.ID_SORT, SORT_LEVEL);

		assertEquals(Arrays.asList(4, 2, 0), shownPlaces(rig));
		rig.click(StoragePage.rowId(2));
		assertEquals(Collections.singletonList("moveToTeam STORAGE#2"), rig.calls);
	}

	@Test
	public void theChipFlipsBetweenShownAndHiddenAndTheCrossRemovesIt()
	{
		PartyRig rig = page(mixed());
		chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());

		rig.click(StoragePage.CHIP_PREFIX + 0);
		assertEquals("Not Crush", label(rig, 0));
		assertEquals(Arrays.asList(1, 3, 5), shownPlaces(rig));

		rig.click(StoragePage.CHIPX_PREFIX + 0);
		assertFalse(query(rig).active());
		assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), shownPlaces(rig));
	}

	@Test
	public void clearEmptiesEveryFilter()
	{
		PartyRig rig = page(mixed());
		chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());
		choose(rig, StoragePage.ID_FILTER, FILTER_GILDED);
		List<StorageQuery.Criterion> both = query(rig).criteria();

		assertEquals(2, both.size());
		assertEquals(Collections.singletonList(4), shownPlaces(rig));

		rig.click(StoragePage.ID_CLEAR);

		assertFalse(query(rig).active());
		assertEquals(6, shownPlaces(rig).size());
	}

	@Test
	public void aLevelFilterRangeAnswersToTwoChips()
	{
		PartyRig rig = page(mixed());

		chooseInSubmenu(rig, StoragePage.ID_FILTER, 0, 1);
		chooseInSubmenu(rig, StoragePage.ID_FILTER, 1, 3);

		assertEquals("Level over 20", label(rig, 0));
		assertEquals("Level under 50", label(rig, 1));
		assertEquals(Arrays.asList(1, 2, 5), shownPlaces(rig));
	}

	@Test
	public void theHeadingAndTheNoMatchTextFollowTheFilter()
	{
		PartyRig rig = page(mixed());
		chooseInSubmenu(rig, StoragePage.ID_FILTER, 0, 8);

		List<String> words = words(rig);

		assertTrue(words.toString(), words.contains("Showing 0 of 6"));
		assertTrue(words.contains(StoragePage.NO_MATCH_TEXT));
		assertTrue("the controls stay", rig.ids().contains(StoragePage.ID_FILTER));
		assertTrue(shownPlaces(rig).isEmpty());
	}

	@Test
	public void clearingFromTheMenuShowsEveryoneAgain()
	{
		PartyRig rig = page(mixed());
		choose(rig, StoragePage.ID_FILTER, FILTER_GILDED);
		assertEquals(Arrays.asList(1, 4), shownPlaces(rig));

		choose(rig, StoragePage.ID_FILTER, 8);

		assertEquals(6, shownPlaces(rig).size());
		assertTrue(words(rig).contains("Stored  6 / " + SaveData.STORAGE_SIZE));
	}

	@Test
	public void aHiddenCreatureIsNeverReleased()
	{
		PartyRig rig = page(mixed());
		rig.click(StoragePage.ID_SELECT);
		rig.click(StoragePage.rowId(0));
		rig.click(StoragePage.rowId(1));
		assertEquals(Arrays.asList(0, 1), new ArrayList<>(rig.pages.storagePage().ticked()));

		choose(rig, StoragePage.ID_FILTER, FILTER_GILDED);

		assertEquals("the tick of the creature the filter hid is dropped", Collections.singletonList(1),
			new ArrayList<>(rig.pages.storagePage().ticked()));
		rig.click(StoragePage.ID_RELEASE);
		chooseYes(rig);
		chooseYes(rig);
		assertEquals(Collections.singletonList("releaseMany [STORAGE#1]"), rig.calls);
	}

	@Test
	public void tickAllShownTicksOnlyWhatIsShown()
	{
		PartyRig rig = page(mixed());
		chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());
		rig.click(StoragePage.ID_SELECT);

		rig.click(StoragePage.ID_TICK_ALL);

		assertEquals(Arrays.asList(0, 2, 4), new ArrayList<>(rig.pages.storagePage().ticked()));
		rig.click(StoragePage.ID_RELEASE);
		chooseYes(rig);
		chooseYes(rig);
		assertEquals(Collections.singletonList("releaseMany [STORAGE#0, STORAGE#2, STORAGE#4]"), rig.calls);
	}

	@Test
	public void tickAllShownIsOffWhenNothingIsShown()
	{
		PartyRig rig = page(mixed());
		chooseInSubmenu(rig, StoragePage.ID_FILTER, 0, 8);
		rig.click(StoragePage.ID_SELECT);

		assertFalse(rig.hit(StoragePage.ID_TICK_ALL).enabled());
	}

	@Test
	public void theQuerySurvivesTheWindowClosingAndOpeningAgain()
	{
		PartyRig rig = page(mixed());
		choose(rig, StoragePage.ID_SORT, SORT_LEVEL);
		chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());
		rig.click(StoragePage.ID_SELECT);

		rig.shell.close(rig.now, ShellMotion.Phase.CLOSE);
		rig.open();
		rig.shell.showTab(WindowTab.STORAGE, rig.now);
		rig.paint();

		assertFalse("Select mode is forgotten", rig.pages.storagePage().selecting());
		assertEquals(StorageQuery.Sort.LEVEL, query(rig).sort());
		assertEquals(Arrays.asList(4, 2, 0), shownPlaces(rig));
	}

	@Test
	public void aNewListKeepsTheQueryAndRearranges()
	{
		PartyRig rig = page(mixed());
		choose(rig, StoragePage.ID_SORT, SORT_LEVEL);

		List<CreatureView> grown = mixed();
		grown.add(make("Dragon", 99, CombatType.MAGIC, Rarity.LEGENDARY, false));
		rig.publish(state(grown), true);
		rig.paint();

		int first = shownPlaces(rig).get(0);

		assertEquals(6, first);
	}

	@Test
	public void everyStringStaysInsideItsBoxWithAFilterAndSelectOn()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : CANVASES)
		{
			for (boolean selecting : new boolean[] {false, true})
			{
				PartyRig rig = page(mixed(), canvas[0], canvas[1]);
				choose(rig, StoragePage.ID_SORT, SORT_LEVEL);
				chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());
				choose(rig, StoragePage.ID_FILTER, FILTER_GILDED);
				chooseInSubmenu(rig, StoragePage.ID_FILTER, 0, 1);
				if (selecting)
					rig.click(StoragePage.ID_SELECT);

				rig.drawn.clear();
				rig.paint();
				Rectangle body = WindowLayout.of(rig.width, rig.height).body;
				for (PartyRig.Drawn d : rig.drawn)
				{
					if (d.width > d.box.width || d.height > d.box.height + 1 || !body.contains(d.box))
						problems.append(canvas[0] + " " + d + "\n");
				}

				for (Hit hit : rig.shell.hits())
				{
					if (hit.id().startsWith("storage:") && !body.contains(hit.bounds()))
						problems.append(canvas[0] + " hit " + hit.id() + "\n");
				}
			}
		}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void theHitsOfTheControlsNeverOverlapAtAnySize()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : CANVASES)
		{
			PartyRig rig = page(mixed(), canvas[0], canvas[1]);
			choose(rig, StoragePage.ID_SORT, SORT_LEVEL);
			chooseInSubmenu(rig, StoragePage.ID_FILTER, FILTER_STYLE, CombatType.CRUSH.ordinal());
			rig.click(StoragePage.ID_SELECT);
			List<Hit> controls = new ArrayList<>();
			for (Hit hit : rig.shell.hits())
			{
				if (hit.id().startsWith("storage:") && !hit.id().startsWith(StoragePage.ROW_PREFIX))
					controls.add(hit);
			}

			for (int i = 0; i < controls.size(); i++)
			{
				for (int j = i + 1; j < controls.size(); j++)
				{
					if (controls.get(i).bounds().intersects(controls.get(j).bounds()))
						problems.append(canvas[0] + " " + controls.get(i).id() + " x " + controls.get(j).id() + "\n");
				}
			}
		}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void downFromTheLastVisibleRowScrollsAndHighlightsTheNextPlace()
	{
		PartyRig rig = page(many(40), 535, 348);
		ScrollList list = rig.pages.storagePage().list();
		int last = list.lastVisible();
		rig.shell.focus().highlightKey(StoragePage.rowId(last));
		rig.paint();

		rig.key(KeyEvent.VK_DOWN);

		assertEquals(StoragePage.rowId(last + 1), rig.shell.focus().highlight());
		assertTrue("the list scrolled", list.offset() > 0);
		assertTrue("the new row is a hit", rig.ids().contains(StoragePage.rowId(last + 1)));
	}

	@Test
	public void endAndHomeGoToTheLastAndFirstShownPlace()
	{
		PartyRig rig = page(many(40), 535, 348);
		choose(rig, StoragePage.ID_SORT, SORT_LEVEL);
		rig.shell.focus().highlightKey(StoragePage.rowId(39));
		rig.paint();

		rig.key(KeyEvent.VK_END);
		assertEquals("lowest level is last", StoragePage.rowId(0), rig.shell.focus().highlight());
		assertTrue(rig.ids().contains(StoragePage.rowId(0)));

		rig.key(KeyEvent.VK_HOME);
		assertEquals(StoragePage.rowId(39), rig.shell.focus().highlight());
		ScrollList list = rig.pages.storagePage().list();

		assertEquals(0, list.offset());
	}

	@Test
	public void pageDownMovesByAPageOfRows()
	{
		PartyRig rig = page(many(40), 535, 348);
		rig.shell.focus().highlightKey(StoragePage.rowId(0));
		rig.paint();

		rig.key(KeyEvent.VK_PAGE_DOWN);

		int moved = StoragePage.place(rig.shell.focus().highlight());
		assertTrue("moved " + moved, moved >= 1);
		rig.key(KeyEvent.VK_PAGE_UP);
		assertEquals(StoragePage.rowId(0), rig.shell.focus().highlight());
	}

	@Test
	public void upOnTheFirstRowAndDownOnTheLastFallThroughToTheWindow()
	{
		PartyRig rig = page(many(6), 765, 497);
		StoragePage storage = rig.pages.storagePage();

		assertEquals(-1, storage.keyTarget(KeyEvent.VK_UP, StoragePage.rowId(0)));
		assertEquals(-1, storage.keyTarget(KeyEvent.VK_DOWN, StoragePage.rowId(5)));
		assertEquals(1, storage.keyTarget(KeyEvent.VK_DOWN, StoragePage.rowId(0)));
		assertEquals("a key the page does not use", -1, storage.keyTarget(KeyEvent.VK_A, StoragePage.rowId(2)));
		assertEquals("a button is not a row", -1, storage.keyTarget(KeyEvent.VK_DOWN, StoragePage.ID_SELECT));
	}

	@Test
	public void upOnTheFirstShownRowMovesTheHighlightToTheButtons()
	{
		PartyRig rig = page(many(6), 765, 497);
		rig.shell.focus().highlightKey(StoragePage.rowId(0));
		rig.paint();

		rig.key(KeyEvent.VK_UP);

		assertEquals("the window's own move takes over, to the control before the first row", StoragePage.ID_SORT,
			rig.shell.focus().highlight());
	}
}
