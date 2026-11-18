/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
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

/**
 * Checks the Summary of a stored creature inside the real shell: it opens from the row menu, has the same boxes as the
 * team Summary with the Storage buttons, steps over the creatures the list shows, goes back to the list with its
 * scroll kept, follows its creature when the list changes, and releases, renames and moves through the same paths
 * the list uses.
 */
public class StoragePageSummaryTest
{
	private static final String[] NAMES = {"Goblin", "Cow", "Imp", "Bloodthirsty banshee", "Man", "Rat"};

	private static List<CreatureView> stored(int count)
	{
		List<CreatureView> list = new ArrayList<>();
		for (int i = 0; i < count; i++)
			list.add(PartyRig.creature(NAMES[i % NAMES.length] + (i >= NAMES.length ? " " + i : ""), 3 + i, 0.9, null));

		return list;
	}

	private static PanelState state(List<CreatureView> storage, int teamSize)
	{
		Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
		items.put(ItemType.SMALL_SALVE, 2);
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, PartyRig.team(teamSize),
			storage, Collections.emptyList(), 0, 0, Collections.emptyList(), Collections.emptyList(), TodayView.NONE,
			TaskView.NONE, items);
	}

	private static PartyRig page(List<CreatureView> storage, int teamSize)
	{
		PartyRig rig = new PartyRig();
		rig.width = 765;
		rig.height = 497;
		rig.publish(state(storage, teamSize), true);
		rig.open();
		rig.shell.showTab(WindowTab.STORAGE, rig.now);
		rig.paint();
		return rig;
	}

	private static PartyRig page(List<CreatureView> storage)
	{
		return page(storage, 2);
	}

	private static void openSummary(PartyRig rig, int place)
	{
		rig.click(StoragePage.rowId(place), MouseEvent.BUTTON3);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);
	}

	private static void chooseYes(PartyRig rig)
	{
		rig.key(KeyEvent.VK_RIGHT);
		rig.key(KeyEvent.VK_ENTER);
	}

	private static List<String> texts(PartyRig rig)
	{
		List<String> texts = new ArrayList<>();
		for (PartyRig.Drawn drawn : rig.drawn)
			texts.add(drawn.text);

		return texts;
	}

	private static List<String> shownNow(PartyRig rig)
	{
		rig.drawn.clear();
		rig.paint();
		return texts(rig);
	}

	private static String nameAt(List<CreatureView> list, int place)
	{
		return list.get(place).name();
	}

	private static PageState summary(PartyRig rig)
	{
		return rig.pages.storedPageState();
	}

	@Test
	public void theMenuOfAStoredRowHasSummaryFirstAndOpensItsPage()
	{
		PartyRig rig = page(stored(3));

		openSummary(rig, 1);

		assertEquals(PageState.Page.SUMMARY, summary(rig).page());
		assertTrue(rig.ids().contains(WindowPages.ID_SUMMARY_MENU));
		assertTrue(rig.ids().contains(WindowPages.ID_MOVES));
		for (int i = 0; i < SummaryLayout.STORED_BUTTONS.length; i++)
			assertTrue(rig.ids().contains(WindowPages.summaryButtonId(i)));

		assertFalse("the list is gone", rig.ids().contains(StoragePage.rowId(0)));
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void theButtonsAreTheStorageOnes()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);

		String back = rig.hit(WindowPages.summaryButtonId(0)).tip();
		String release = rig.hit(WindowPages.summaryButtonId(4)).tip();
		assertTrue(back.startsWith("Back to the list"));
		assertEquals("Move it to your team", rig.hit(WindowPages.summaryButtonId(3)).tip());
		assertTrue(release.startsWith("Release it for good"));
		assertEquals("Use a healing item on it", rig.hit(WindowPages.summaryButtonId(5)).tip());
		for (int i = 0; i < SummaryLayout.STORED_BUTTONS.length; i++)
			assertTrue(rig.hit(WindowPages.summaryButtonId(i)).enabled() || i == 5 || i == 3);
	}

	@Test
	public void theTitleIsTheNicknameWithTheSpeciesUnderItOrJustTheSpecies()
	{
		List<CreatureView> list = stored(3);
		list.set(1, PartyRig.creature("Cow", 5, 0.9, "Moo"));
		PartyRig rig = page(list);

		openSummary(rig, 1);
		List<String> named = shownNow(rig);
		assertTrue(named.toString(), named.contains("Moo"));
		assertTrue(named.toString(), named.contains("Cow"));

		rig.key(KeyEvent.VK_ESCAPE);
		openSummary(rig, 0);
		List<String> plain = shownNow(rig);
		assertTrue(plain.toString(), plain.contains("Goblin"));
		assertFalse(plain.toString(), plain.contains("Moo"));
	}

	@Test
	public void theTitleTooltipEndsWithWhenItWasCaught()
	{
		long ms = 1_794_000_000_000L;
		List<CreatureView> list = stored(2);
		list.set(0, list.get(0).withCaught(ms));
		PartyRig rig = page(list);

		openSummary(rig, 0);

		String tip = rig.hit(WindowPages.ID_SUMMARY_MENU).tip();
		assertTrue(tip, tip.startsWith(SummaryPainter.HEADER_TIP));
		assertTrue(tip, tip.endsWith(SummaryContent.caughtText(ms, ZoneId.systemDefault())));
	}

	@Test
	public void aCreatureWithNoCatchTimeHasTheHeaderTooltipAlone()
	{
		PartyRig rig = page(stored(2));

		openSummary(rig, 0);

		assertEquals(SummaryPainter.HEADER_TIP, rig.hit(WindowPages.ID_SUMMARY_MENU).tip());
	}

	@Test
	public void theStatsBoxHasATooltipWithTheNumbers()
	{
		List<CreatureView> list = stored(2);
		list.set(0, list.get(0).withStats(84, 31, 22, 40));
		PartyRig rig = page(list);

		openSummary(rig, 0);

		String tip = rig.hit(SummaryPainter.ID_STATS).tip();
		assertTrue(tip, tip.startsWith("Health 84: roll +3"));
		List<String> words = shownNow(rig);
		assertTrue(words.toString(), words.contains("84"));
		assertTrue(words.toString(), words.contains("40"));
	}

	@Test
	public void upAndDownMoveOverTheCreaturesTheListShowsInItsOrderAndWrap()
	{
		List<CreatureView> list = stored(4);
		PartyRig rig = page(list);
		rig.pages.storagePage().setQuery(StorageQuery.NONE.withSort(StorageQuery.Sort.LEVEL), list);
		rig.paint();
		int[] shown = rig.pages.storagePage().shownPlaces();
		assertEquals(4, shown.length);
		openSummary(rig, shown[1]);
		assertEquals(1, summary(rig).index());

		rig.key(KeyEvent.VK_DOWN);
		assertEquals(2, summary(rig).index());
		assertTrue(shownNow(rig).contains(nameAt(list, shown[2])));
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);
		assertEquals("wraps to the first", 0, summary(rig).index());
		assertTrue(shownNow(rig).contains(nameAt(list, shown[0])));
		rig.key(KeyEvent.VK_UP);
		assertEquals("wraps to the last", 3, summary(rig).index());
		assertTrue(shownNow(rig).contains(nameAt(list, shown[3])));
	}

	@Test
	public void aFilterLeavesOutWhatTheKeysSkip()
	{
		List<CreatureView> list = stored(4);
		list.set(2, PartyRig.creature("Imp", 40, 0.9, null));
		PartyRig rig = page(list);
		rig.pages.storagePage().setQuery(StorageQuery.NONE.withSort(StorageQuery.Sort.LEVEL), list);
		rig.paint();
		int[] shown = rig.pages.storagePage().shownPlaces();

		openSummary(rig, shown[0]);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);

		assertEquals("four steps over four creatures come back round", 0, summary(rig).index());
	}

	@Test
	public void upAndDownAlsoWorkInTheBoxView()
	{
		List<CreatureView> list = stored(5);
		PartyRig rig = page(list);
		while (rig.pages.storagePage().mode() != StorageMode.BOX)
			rig.click(StoragePage.ID_VIEW);

		openSummary(rig, 2);
		assertEquals(2, summary(rig).index());
		rig.key(KeyEvent.VK_DOWN);

		assertEquals(3, summary(rig).index());
		assertTrue(shownNow(rig).contains(nameAt(list, 3)));
	}

	@Test
	public void enterOnThePageOpensTheCreaturesMenuWithoutSummary()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);

		rig.shell.focus().highlightKey(WindowPages.ID_SUMMARY_MENU);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_ENTER);
		assertEquals("the first item is Move to team", Collections.singletonList("moveToTeam STORAGE#1"),
			rig.calls);
	}

	@Test
	public void theBackChainGoesMenuThenSummaryThenListThenClosed()
	{
		PartyRig rig = page(stored(3));
		rig.click(StoragePage.rowId(1), MouseEvent.BUTTON3);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(1, rig.pages.depth());

		rig.shell.focus().highlightKey(WindowPages.ID_SUMMARY_MENU);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_ESCAPE);
		assertEquals(PageState.Page.SUMMARY, summary(rig).page());

		rig.key(KeyEvent.VK_ESCAPE);
		assertEquals(PageState.Page.PARTY, summary(rig).page());
		assertEquals(0, rig.pages.depth());
		assertTrue(rig.shell.isOpen());

		rig.key(KeyEvent.VK_ESCAPE);
		assertFalse(rig.shell.isOpen());
	}

	@Test
	public void backspaceAndTheBackButtonReturnToTheList()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);
		rig.key(KeyEvent.VK_BACK_SPACE);
		assertEquals(PageState.Page.PARTY, summary(rig).page());
		assertTrue(rig.ids().contains(StoragePage.rowId(1)));

		openSummary(rig, 1);
		rig.click(WindowPages.summaryButtonId(0));
		assertEquals(PageState.Page.PARTY, summary(rig).page());
		assertTrue(rig.ids().contains(StoragePage.rowId(1)));
	}

	@Test
	public void theListKeepsItsScrollOffsetBehindTheSummary()
	{
		PartyRig rig = page(stored(40));
		ScrollList scroll = rig.pages.storagePage().list();
		scroll.setOffset(300);
		rig.paint();
		double offset = scroll.offset();
		String row = null;
		for (String id : rig.ids())
		{
			if (id.startsWith(StoragePage.ROW_PREFIX))
			{
				row = id;
				break;
			}
		}

		assertTrue(offset > 0);
		openSummary(rig, StoragePage.place(row));
		rig.key(KeyEvent.VK_ESCAPE);

		assertEquals(offset, scroll.offset(), 0.0);
	}

	@Test
	public void leavingTheTabForgetsTheSummary()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);

		rig.shell.showTab(WindowTab.PARTY, rig.now);
		rig.paint();
		assertEquals(PageState.Page.PARTY, summary(rig).page());

		rig.shell.showTab(WindowTab.STORAGE, rig.now);
		rig.paint();
		assertTrue(rig.ids().contains(StoragePage.rowId(1)));
		assertFalse(rig.ids().contains(WindowPages.ID_SUMMARY_MENU));
	}

	@Test
	public void whenStorageGreysOutTheShellJumpsToPartyAndTheSummaryIsForgotten()
	{
		List<CreatureView> list = stored(3);
		PartyRig rig = page(list);
		openSummary(rig, 1);

		rig.publish(state(list, 2), false);
		rig.paint();
		rig.publish(state(list, 2), true);
		rig.shell.showTab(WindowTab.STORAGE, rig.now);
		rig.paint();

		assertEquals(PageState.Page.PARTY, summary(rig).page());
		assertTrue(rig.ids().contains(StoragePage.rowId(1)));
	}

	@Test
	public void closingTheWindowForgetsIt()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);

		rig.shell.close(rig.now, ShellMotion.Phase.COMBAT_CLOSE);

		assertEquals(PageState.Page.PARTY, summary(rig).page());
	}

	@Test
	public void toTeamMovesTheCreatureAndTheSummaryClosesWhenItLeavesStorage()
	{
		List<CreatureView> list = stored(3);
		PartyRig rig = page(list);
		openSummary(rig, 1);

		rig.click(WindowPages.summaryButtonId(3));
		assertEquals(Collections.singletonList("moveToTeam STORAGE#1"), rig.calls);

		List<CreatureView> left = new ArrayList<>(list);
		left.remove(1);
		rig.publish(state(left, 3), true);
		rig.paint();

		assertEquals(PageState.Page.PARTY, summary(rig).page());
		assertTrue(rig.ids().contains(StoragePage.rowId(0)));
	}

	@Test
	public void toTeamIsDimWithTheReasonWhenTheTeamIsFull()
	{
		PartyRig rig = page(stored(3), 6);
		openSummary(rig, 1);

		Hit button = rig.hit(WindowPages.summaryButtonId(3));

		assertFalse(button.enabled());
		assertTrue(button.tip(), button.tip().contains("Team is full"));
		rig.click(WindowPages.summaryButtonId(3));
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void releaseAsksWithNoAsTheDefaultAndYesReleases()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);

		rig.click(WindowPages.summaryButtonId(4));
		assertEquals(WindowKeyRules.KeyMode.MODAL, rig.shell.keyMode());
		rig.key(KeyEvent.VK_ENTER);
		assertTrue("Enter on the default keeps the creature", rig.calls.isEmpty());

		rig.click(WindowPages.summaryButtonId(4));
		chooseYes(rig);
		assertEquals(Collections.singletonList("release STORAGE#1"), rig.calls);
	}

	@Test
	public void aGildedCreatureAsksASecondTimeFromTheSummaryToo()
	{
		List<CreatureView> list = stored(3);
		list.set(0, new CreatureView("Man", 30, 0.4, 0.9, Rarity.RARE, CombatType.SLASH, true, null,
			new StatRolls(3, -2, 0, 5, Temperament.BRAVE), 0, Arrays.asList(PartyRig.SLASH, PartyRig.BASH),
			Collections.emptyList(), 3).withGender(Gender.MALE));
		PartyRig rig = page(list);
		openSummary(rig, 0);

		rig.click(WindowPages.summaryButtonId(4));
		chooseYes(rig);
		assertTrue("the first Yes is not enough", rig.calls.isEmpty());
		rig.key(KeyEvent.VK_ENTER);
		assertTrue("the second box also defaults to No", rig.calls.isEmpty());

		rig.click(WindowPages.summaryButtonId(4));
		chooseYes(rig);
		chooseYes(rig);
		assertEquals(Collections.singletonList("release STORAGE#0"), rig.calls);
	}

	@Test
	public void aReleaseForACreatureThatMovedLandsOnNothing()
	{
		List<CreatureView> list = stored(3);
		PartyRig rig = page(list);
		openSummary(rig, 2);
		rig.click(WindowPages.summaryButtonId(4));

		List<CreatureView> changed = stored(3);
		changed.set(2, PartyRig.creature("Dragon", 90, 1.0, null));
		rig.publish(state(changed, 2), true);
		chooseYes(rig);

		assertTrue(rig.calls.toString(), rig.calls.isEmpty());
	}

	@Test
	public void renameOpensTheBoxAndSendsTheNameForTheStoredCreature()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 2);

		rig.click(WindowPages.summaryButtonId(1));
		assertEquals(WindowKeyRules.KeyMode.FIELD, rig.shell.keyMode());
		for (char c : "Gobbo".toCharArray())
			rig.shell.onTyped(c, 0, rig.now);

		rig.paint();
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("rename STORAGE#2 Gobbo"), rig.calls);
	}

	@Test
	public void movesWithAWaitingMoveCanBeSkippedFromTheSummary()
	{
		Move spark = new Move("spark", "Spark", CombatType.MAGIC, 1.2, 0.9);
		List<CreatureView> list = stored(3);
		list.set(2, new CreatureView("Goblin", 9, 0.4, 1.0, Rarity.COMMON, CombatType.SLASH, false, null,
			StatRolls.AVERAGE, 0, Arrays.asList(PartyRig.SLASH, PartyRig.BASH), Collections.singletonList(spark)));
		PartyRig rig = page(list);
		openSummary(rig, 2);

		rig.click(WindowPages.ID_MOVES);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("resolveMove STORAGE#2 -1"), rig.calls);
	}

	@Test
	public void useItemOffersTheHelpfulItemsAndUsesOneOnTheStoredCreature()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);
		assertTrue(rig.hit(WindowPages.summaryButtonId(5)).enabled());

		rig.click(WindowPages.summaryButtonId(5));
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("useItem STORAGE#1 SMALL_SALVE"), rig.calls);
	}

	@Test
	public void aFullHealthCreatureHasUseItemDim()
	{
		List<CreatureView> list = stored(3);
		list.set(1, PartyRig.creature("Cow", 5, 1.0, null));
		PartyRig rig = page(list);
		openSummary(rig, 1);

		assertFalse(rig.hit(WindowPages.summaryButtonId(5)).enabled());
	}

	@Test
	public void aSwapDoneWhileTheSummaryIsOpenKeepsItOnTheSameCreature()
	{
		List<CreatureView> list = stored(4);
		PartyRig rig = page(list);
		openSummary(rig, 1);
		assertEquals(1, summary(rig).index());

		List<CreatureView> swapped = new ArrayList<>(list);
		Collections.swap(swapped, 1, 3);
		rig.publish(state(swapped, 2), true);
		rig.paint();

		assertEquals(PageState.Page.SUMMARY, summary(rig).page());
		assertEquals(3, summary(rig).index());
		assertTrue(shownNow(rig).contains(nameAt(list, 1)));
	}

	@Test
	public void theRowTooltipPointsAtSummary()
	{
		PartyRig rig = page(stored(2));

		String tip = rig.hit(StoragePage.rowId(0)).tip();

		assertTrue(tip, tip.endsWith("Right-click for Summary, rename or release"));
	}

	@Test
	public void aStoredSummaryLeavesTheTeamSummaryAlone()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);

		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());
	}

	@Test
	public void boundsOfEveryStoredButtonAreDistinctAndInsideTheWindow()
	{
		PartyRig rig = page(stored(3));
		openSummary(rig, 1);
		List<Rectangle> seen = new ArrayList<>();
		for (int i = 0; i < SummaryLayout.STORED_BUTTONS.length; i++)
		{
			Rectangle b = rig.hit(WindowPages.summaryButtonId(i)).bounds();
			assertTrue(b.width > 0 && b.height > 0);
			assertFalse(seen.contains(b));
			seen.add(b);
		}
	}
}
