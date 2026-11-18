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
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Storage page: its rows and tooltips, Move to team, the Release confirmations, Select mode and the layout. */
public class StoragePageTest
{
	private static final int[][] CANVASES = {{535, 348}, {765, 497}, {1344, 874}};
	private static final String[] NAMES = {"Goblin", "Cow", "Imp", "Bloodthirsty banshee", "Man", "Rat"};

	private static CreatureView gilded(String name)
	{
		return new CreatureView(name, 30, 0.4, 0.9, Rarity.RARE, CombatType.SLASH, true, null,
			new StatRolls(3, -2, 0, 5, Temperament.BRAVE), 0, Arrays.asList(PartyRig.SLASH, PartyRig.BASH),
			Collections.emptyList(), 3).withGender(Gender.MALE);
	}

	private static List<CreatureView> stored(int count)
	{
		List<CreatureView> list = new ArrayList<>();
		for (int i = 0; i < count; i++)
			list.add(PartyRig.creature(NAMES[i % NAMES.length] + (i >= NAMES.length ? " " + i : ""), 3 + i, 0.9, null));

		return list;
	}

	private static PanelState state(List<CreatureView> storage)
	{
		Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
		items.put(ItemType.SMALL_SALVE, 2);
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, PartyRig.team(2), storage,
			Collections.emptyList(), 0, 0, Collections.emptyList(), Collections.emptyList(), TodayView.NONE,
			TaskView.NONE, items);
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

	private static void menuItem(PartyRig rig, String rowId, int place)
	{
		rig.click(rowId, MouseEvent.BUTTON3);
		for (int i = 0; i <= place; i++)
			rig.key(KeyEvent.VK_DOWN);

		rig.key(KeyEvent.VK_ENTER);
	}

	private static void chooseYes(PartyRig rig)
	{
		rig.key(KeyEvent.VK_RIGHT);
		rig.key(KeyEvent.VK_ENTER);
	}

	@Test
	public void theStorageTabHoldsOneRowPerStoredCreatureWithItsTooltip()
	{
		PartyRig rig = page(stored(3));

		assertTrue(rig.ids().contains(StoragePage.rowId(0)));
		assertTrue(rig.ids().contains(StoragePage.rowId(2)));
		assertFalse(rig.ids().contains(StoragePage.rowId(3)));
		String tip = rig.hit(StoragePage.rowId(0)).tip();
		assertTrue(tip, tip.contains("Goblin"));
		assertTrue(tip, tip.contains("Click to move to your team"));
		assertTrue(tip, tip.contains("Releasing pays " + ReleaseRules.xp(stored(1).get(0)) + " XP to your lead"));
		assertFalse("a stored creature is never the lead", tip.contains("lead:"));
	}

	@Test
	public void aNicknameShowsBesideTheSpecies()
	{
		List<CreatureView> list = stored(1);
		list.set(0, PartyRig.creature("Cow", 5, 1.0, "Moo"));
		PartyRig rig = page(list);

		String tip = rig.hit(StoragePage.rowId(0)).tip();
		assertTrue(tip, tip.startsWith("Moo (Cow)"));
	}

	@Test
	public void aLeftClickMovesTheCreatureToTheTeam()
	{
		PartyRig rig = page(stored(3));

		rig.click(StoragePage.rowId(1));

		assertEquals(Collections.singletonList("moveToTeam STORAGE#1"), rig.calls);
	}

	@Test
	public void theMenuMovesRenamesAndUsesItemsAndReleases()
	{
		PartyRig rig = page(stored(2));

		menuItem(rig, StoragePage.rowId(1), 0);
		assertEquals("moveToTeam STORAGE#1", rig.calls.get(0));

		menuItem(rig, StoragePage.rowId(0), 2);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals("useItem STORAGE#0 SMALL_SALVE", rig.calls.get(1));

		menuItem(rig, StoragePage.rowId(0), 1);
		rig.key(KeyEvent.VK_ESCAPE);
		assertEquals("a cancelled Rename changes nothing", 2, rig.calls.size());
	}

	@Test
	public void releaseAsksFirstAndNoIsTheDefault()
	{
		PartyRig rig = page(stored(2));

		menuItem(rig, StoragePage.rowId(1), 3);
		assertTrue("nothing is released before the answer", rig.calls.isEmpty());
		rig.key(KeyEvent.VK_ENTER);

		assertTrue("Enter on the default button keeps the creature", rig.calls.isEmpty());
	}

	@Test
	public void escapeKeepsTheCreature()
	{
		PartyRig rig = page(stored(2));

		menuItem(rig, StoragePage.rowId(1), 3);
		rig.key(KeyEvent.VK_ESCAPE);

		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void yesReleasesTheCreature()
	{
		PartyRig rig = page(stored(2));

		menuItem(rig, StoragePage.rowId(1), 3);
		chooseYes(rig);

		assertEquals(Collections.singletonList("release STORAGE#1"), rig.calls);
	}

	@Test
	public void aGildedOrNamedCreatureAsksASecondTime()
	{
		List<CreatureView> list = stored(2);
		list.set(0, gilded("Man"));
		list.set(1, PartyRig.creature("Cow", 5, 0.9, "Moo"));
		for (int place = 0; place < 2; place++)
		{
			PartyRig rig = page(list);
			menuItem(rig, StoragePage.rowId(place), 3);
			chooseYes(rig);
			assertTrue("the first Yes is not enough", rig.calls.isEmpty());
			rig.key(KeyEvent.VK_ENTER);
			assertTrue("the second box also defaults to No", rig.calls.isEmpty());

			menuItem(rig, StoragePage.rowId(place), 3);
			chooseYes(rig);
			chooseYes(rig);
			assertEquals(Collections.singletonList("release STORAGE#" + place), rig.calls);
		}
	}

	@Test
	public void aReleaseForACreatureThatMovedLandsOnNothing()
	{
		PartyRig rig = page(stored(3));
		menuItem(rig, StoragePage.rowId(2), 3);

		List<CreatureView> changed = stored(3);
		changed.set(2, PartyRig.creature("Dragon", 90, 1.0, null));
		rig.publish(state(changed), true);
		chooseYes(rig);

		assertTrue(rig.calls.toString(), rig.calls.isEmpty());
	}

	@Test
	public void selectModeTicksRowsAndOneConfirmReleasesThemAll()
	{
		PartyRig rig = page(stored(4));
		rig.click(StoragePage.ID_SELECT);
		assertTrue(rig.pages.storagePage().selecting());
		assertFalse("Release is off until something is ticked", rig.hit(StoragePage.ID_RELEASE).enabled());

		rig.click(StoragePage.rowId(0));
		rig.click(StoragePage.rowId(2));
		assertTrue("ticking does not move the creature", rig.calls.isEmpty());
		assertEquals(Arrays.asList(0, 2), new ArrayList<>(rig.pages.storagePage().ticked()));
		assertTrue(rig.hit(StoragePage.ID_RELEASE).enabled());

		rig.click(StoragePage.ID_RELEASE);
		assertTrue(rig.calls.isEmpty());
		chooseYes(rig);

		assertEquals(Collections.singletonList("releaseMany [STORAGE#0, STORAGE#2]"), rig.calls);
		assertFalse("Select mode ends after the release", rig.pages.storagePage().selecting());
	}

	@Test
	public void aTickedSpecialCreatureAsksAgainBeforeTheBatchGoes()
	{
		List<CreatureView> list = stored(3);
		list.set(1, gilded("Man"));
		PartyRig rig = page(list);
		rig.click(StoragePage.ID_SELECT);
		rig.click(StoragePage.rowId(0));
		rig.click(StoragePage.rowId(1));
		rig.click(StoragePage.ID_RELEASE);

		chooseYes(rig);
		assertTrue(rig.calls.isEmpty());
		chooseYes(rig);

		assertEquals(Collections.singletonList("releaseMany [STORAGE#0, STORAGE#1]"), rig.calls);
	}

	@Test
	public void decliningTheBatchKeepsTheTicks()
	{
		PartyRig rig = page(stored(3));
		rig.click(StoragePage.ID_SELECT);
		rig.click(StoragePage.rowId(1));
		rig.click(StoragePage.ID_RELEASE);

		rig.key(KeyEvent.VK_ENTER);

		assertTrue(rig.calls.isEmpty());
		assertEquals(Collections.singletonList(1), new ArrayList<>(rig.pages.storagePage().ticked()));
	}

	@Test
	public void doneLeavesSelectModeAndClearsTheTicks()
	{
		PartyRig rig = page(stored(3));
		rig.click(StoragePage.ID_SELECT);
		rig.click(StoragePage.rowId(1));

		rig.click(StoragePage.ID_SELECT);

		assertFalse(rig.pages.storagePage().selecting());
		StoragePage storage = rig.pages.storagePage();
		assertTrue(storage.ticked().isEmpty());
	}

	@Test
	public void aTickStaysOnlyWhileTheSameCreatureIsInThatPlace()
	{
		List<CreatureView> list = stored(4);
		PartyRig rig = page(list);
		rig.click(StoragePage.ID_SELECT);
		rig.click(StoragePage.rowId(0));
		rig.click(StoragePage.rowId(3));

		List<CreatureView> healed = new ArrayList<>(list);
		healed.set(0, new CreatureView(list.get(0).name(), list.get(0).level(), 0.4, 0.5, Rarity.RARE,
			CombatType.SLASH, false, null, new StatRolls(3, -2, 0, 5, Temperament.BRAVE), 0,
			Arrays.asList(PartyRig.SLASH, PartyRig.BASH), Collections.emptyList(), 3).withGender(Gender.FEMALE)
			.withDetails("A small green creature with a bad temper.", "Grows into Hobgoblin (20)"));
		healed.remove(3);
		rig.publish(state(healed), true);
		rig.paint();

		assertEquals("health changes keep a tick; a creature that left drops it", Collections.singletonList(0),
			new ArrayList<>(rig.pages.storagePage().ticked()));
	}

	@Test
	public void theListScrollsWithTheWheelAndTheBar()
	{
		PartyRig rig = page(stored(40), 535, 348);
		ScrollList list = rig.pages.storagePage().list();
		assertTrue(list.scrollable());
		Rectangle view = list.viewport();

		rig.shell.onWheel(view.x + 5, view.y + 5, 2);
		assertTrue("the wheel scrolls", list.offset() > 0);

		Rectangle thumb = list.thumbBounds();
		int before = list.offset();
		rig.shell.onPressed(thumb.x + 1, thumb.y + 1, MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onDragged(thumb.x + 1, thumb.y + 21);
		rig.shell.onReleased(thumb.x + 1, thumb.y + 21, MouseEvent.BUTTON1, rig.now);
		assertTrue("dragging the handle scrolls", list.offset() > before);
		assertTrue("a drag on the bar moves no creature", rig.calls.isEmpty());
	}

	@Test
	public void anEmptyStorageSaysSo()
	{
		PartyRig rig = page(Collections.emptyList());
		List<String> words = new ArrayList<>();
		rig.drawn.clear();
		rig.paint();
		for (PartyRig.Drawn d : rig.drawn)
			words.add(d.text);

		assertTrue(words.toString(), words.contains("Stored  0 / " + SaveData.STORAGE_SIZE));
		assertTrue(words.toString(), words.contains(StoragePage.EMPTY_TEXT)
			|| words.contains("Storage is empty. Creatures go here when your team is full."));
	}

	@Test
	public void theTabIsGreyedUnlessStorageIsEnabled()
	{
		PartyRig rig = new PartyRig();
		rig.publish(state(stored(2)), false);
		rig.open();
		rig.shell.showTab(WindowTab.STORAGE, rig.now);
		rig.paint();

		assertFalse(rig.ids().contains(StoragePage.ID_SELECT));
	}

	@Test
	public void everyStringStaysInsideItsBoxAtEverySize()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : CANVASES)
		{
			for (int count : new int[] {0, 3, 40})
			{
				for (boolean selecting : new boolean[] {false, true})
				{
					PartyRig rig = page(withLongNames(count), canvas[0], canvas[1]);
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
							problems.append(canvas[0] + " " + d + "\n");
					}

					for (Hit hit : rig.shell.hits())
					{
						if (hit.id().startsWith("storage:") && !body.contains(hit.bounds()))
							problems.append(canvas[0] + " hit " + hit.id() + "\n");
					}
				}
			}
		}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void selectingDoesNotChangeTheRowHeightOrPosition()
	{
		for (int[] canvas : CANVASES)
		{
			PartyRig rig = page(stored(4), canvas[0], canvas[1]);
			Rectangle[] before = rows(rig);

			rig.click(StoragePage.ID_SELECT);

			assertRowsEqual(canvas[0] + "x" + canvas[1] + " selecting", before, rows(rig));

			rig.click(StoragePage.ID_SELECT);

			assertRowsEqual(canvas[0] + "x" + canvas[1] + " done", before, rows(rig));
		}
	}

	private static Rectangle[] rows(PartyRig rig)
	{
		Rectangle[] rows = new Rectangle[2];
		for (int i = 0; i < rows.length; i++)
			rows[i] = new Rectangle(rig.hit(StoragePage.rowId(i)).bounds());

		return rows;
	}

	private static void assertRowsEqual(String label, Rectangle[] expected, Rectangle[] actual)
	{
		for (int i = 0; i < expected.length; i++)
		{
			assertEquals(label + " row " + i + " height", expected[i].height, actual[i].height);
			assertEquals(label + " row " + i + " y", expected[i].y, actual[i].y);
		}
	}

	private static List<CreatureView> withLongNames(int count)
	{
		List<CreatureView> list = stored(count);
		if (count > 0)
			list.set(0, PartyRig.creature("Bloodthirsty banshee", 99, 0.1, "A very very long nickname"));

		return list;
	}

	@Test
	public void aFaintedStoredCreatureHasAReviveItemThatAsksTheGame()
	{
		List<CreatureView> list = stored(2);
		list.set(0, PartyRig.creature("Goblin", 9, 0.0, null).withFainted(true));
		PartyRig rig = page(list);

		menuItem(rig, StoragePage.rowId(0), 1);

		assertEquals(Collections.singletonList("reviveAtBank STORAGE#0"), rig.calls);
	}

	@Test
	public void aStoredCreatureThatIsUpHasNoReviveItem()
	{
		PartyRig rig = page(stored(2));

		menuItem(rig, StoragePage.rowId(0), 1);

		assertFalse(rig.calls.contains("reviveAtBank STORAGE#0"));
	}
}
