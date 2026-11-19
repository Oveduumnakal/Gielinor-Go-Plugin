/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** The Collection page's search box: it narrows the list by species name and works with the filter button. */
public class CollectionSearchTest
{
	private static LogEntry caught(String name)
	{
		return new LogEntry(name, 10, Rarity.COMMON, CombatType.STAB, 1, true, false);
	}

	private static LogEntry seen(String name)
	{
		return new LogEntry(name, 10, Rarity.COMMON, CombatType.STAB, 0, true, false, false, null,
			Collections.emptyList());
	}

	private static LogEntry unseen(String name)
	{
		return new LogEntry(name, 10, Rarity.COMMON, CombatType.STAB, 0, false, false);
	}

	private static List<LogEntry> log()
	{
		return Arrays.asList(caught("Goblin"), caught("Giant rat"), seen("Giant spider"), seen("Imp"),
			unseen("Giant bat"), unseen("Cow"));
	}

	private static PanelState state()
	{
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, Collections.emptyList(),
			Collections.emptyList(), log(), 2, 4, Collections.emptyList(), Collections.emptyList(), TodayView.NONE,
			TaskView.NONE, new EnumMap<>(ItemType.class)).withGoals(MilestoneView.NONE, EliteView.NONE);
	}

	private static PartyRig page()
	{
		PartyRig rig = new PartyRig();
		rig.width = 1344;
		rig.height = 874;
		rig.publish(state(), true);
		rig.open();
		rig.shell.showTab(WindowTab.COLLECTION, rig.now);
		rig.paint();
		return rig;
	}

	private static void type(PartyRig rig, String text)
	{
		for (char c : text.toCharArray())
			rig.shell.onTyped(c, 0, rig.now);

		rig.paint();
	}

	private static List<String> names(CollectionPage collection)
	{
		List<String> names = new ArrayList<>();
		for (int i = 0; i < collection.shownCount(); i++)
		{
			collection.entryOf(CollectionPage.rowId(i));
			names.add(collection.selectedEntry().name());
		}

		return names;
	}

	@Test
	public void theTextMatchesPartOfAKnownNameWhateverTheCase()
	{
		assertTrue(CollectionFilter.matchesText(caught("Giant rat"), "ANT R"));
		assertTrue(CollectionFilter.matchesText(seen("Imp"), "  imp "));
		assertFalse(CollectionFilter.matchesText(caught("Goblin"), "gobble"));
	}

	@Test
	public void aBlankTextMatchesEverySpecies()
	{
		assertTrue(CollectionFilter.matchesText(unseen("Cow"), ""));
		assertTrue(CollectionFilter.matchesText(unseen("Cow"), "   "));
		assertTrue(CollectionFilter.matchesText(unseen("Cow"), null));
	}

	@Test
	public void aSpeciesNotMetNeverMatchesSoItsNameStaysSecret()
	{
		assertFalse(CollectionFilter.matchesText(unseen("Giant bat"), "bat"));
		assertFalse(CollectionFilter.matchesText(unseen("Giant bat"), "giant"));
	}

	@Test
	public void clickingTheBoxGivesItTheKeysAndTypingNarrowsTheList()
	{
		PartyRig rig = page();
		CollectionPage collection = rig.pages.collectionPage();
		assertEquals(6, collection.shownCount());
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());

		rig.click(CollectionPage.ID_SEARCH);
		assertEquals(WindowKeyRules.KeyMode.FIELD, rig.shell.keyMode());
		type(rig, "giant");

		assertEquals(Arrays.asList("Giant rat", "Giant spider"), names(collection));
		type(rig, " s");
		assertEquals(Collections.singletonList("Giant spider"), names(collection));
	}

	@Test
	public void theSearchCombinesWithTheFilterButton()
	{
		PartyRig rig = page();
		CollectionPage collection = rig.pages.collectionPage();
		rig.click(CollectionPage.ID_SEARCH);
		type(rig, "giant");

		rig.click(CollectionPage.ID_FILTER);
		assertEquals(CollectionFilter.CAUGHT, collection.filter());
		assertEquals(Collections.singletonList("Giant rat"), names(collection));
	}

	@Test
	public void enterLeavesTheBoxAndKeepsTheTextEscClearsIt()
	{
		PartyRig rig = page();
		CollectionPage collection = rig.pages.collectionPage();
		rig.click(CollectionPage.ID_SEARCH);
		type(rig, "imp");
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
		assertEquals("imp", collection.searchBox().text());
		assertEquals(1, collection.shownCount());

		rig.click(CollectionPage.ID_SEARCH);
		rig.key(KeyEvent.VK_ESCAPE);

		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
		assertEquals("", collection.searchBox().text());
		assertEquals(6, collection.shownCount());
	}

	@Test
	public void typingNeverReachesTheListKeysWhileTheBoxHasFocus()
	{
		PartyRig rig = page();
		CollectionPage collection = rig.pages.collectionPage();
		rig.click(CollectionPage.ID_SEARCH);
		rig.key(KeyEvent.VK_DOWN);

		assertNull(collection.selectedEntry());
	}

	@Test
	public void clickingElsewhereOrChangingTabsTakesTheKeysBack()
	{
		PartyRig rig = page();
		rig.click(CollectionPage.ID_SEARCH);
		rig.click(CollectionPage.ID_FILTER);
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());

		rig.click(CollectionPage.ID_SEARCH);
		assertEquals(WindowKeyRules.KeyMode.FIELD, rig.shell.keyMode());
		rig.shell.showTab(WindowTab.PARTY, rig.now);
		rig.paint();
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
	}

	@Test
	public void losingTheWindowFocusKeepsTheTextButDropsTheKeys()
	{
		PartyRig rig = page();
		CollectionPage collection = rig.pages.collectionPage();
		rig.click(CollectionPage.ID_SEARCH);
		type(rig, "cow");
		rig.shell.focusLost(rig.now);

		assertFalse(collection.searchFocused());
		assertEquals("cow", collection.searchBox().text());
	}
}
