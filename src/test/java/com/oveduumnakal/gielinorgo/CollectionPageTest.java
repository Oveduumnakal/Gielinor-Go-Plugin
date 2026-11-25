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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** The Collection page: the Creature Log list, its record, today's spawns, the filter and the wiki menu. */
public class CollectionPageTest
{
	private static final int SPECIES = 1254;

	private static List<LogEntry> log(int size)
	{
		List<LogEntry> log = new ArrayList<>();
		for (int i = 0; i < size; i++)
		{
			String name = i == 0 ? "Goblin" : i == 1 ? "Imp" : i == 2 ? "Rat" : "Species number " + i;
			List<LogEntry.Stage> line = i == 1 ? Arrays.asList(new LogEntry.Stage("Imp", true),
				new LogEntry.Stage("Lesser demon", true), new LogEntry.Stage("Greater demon", false))
				: Collections.emptyList();
			if (i % 5 == 0)
				log.add(new LogEntry(name, 2 + i % 300, Rarity.values()[i % 5], CombatType.STAB, 1 + i % 9, true,
					i % 10 == 0, i == 5, "A short examine text for " + name + ".", line));
			else if (i % 5 == 1)
				log.add(new LogEntry(name, 2 + i % 300, Rarity.values()[i % 5], CombatType.SLASH, 0, true, false,
					false, "Seen once, quite a long examine text that goes on and on and on and on for a while "
					+ "and may not fit in the details strip of the Collection page.", line));
			else
				log.add(new LogEntry(name, 2 + i % 300, Rarity.values()[i % 5], CombatType.CRUSH, 0, false, false));
		}

		return log;
	}

	private static PanelState state(List<LogEntry> log, boolean today)
	{
		int caught = 0;
		int seen = 0;
		for (LogEntry entry : log)
		{
			caught += entry.caught() && !entry.retired() ? 1 : 0;
			seen += entry.seen() ? 1 : 0;
		}

		TodayView view = today ? new TodayView(Arrays.asList(new TodayView.Pick("Rat", Rarity.COMMON),
			new TodayView.Pick("Goblin", Rarity.UNCOMMON), new TodayView.Pick("Species number 5", Rarity.RARE),
			new TodayView.Pick("Species number 10", Rarity.EPIC),
			new TodayView.Pick("Species number 15", Rarity.LEGENDARY)),
			"Dragon weekend: x2 wild, x2 gilded", true, 312, true) : TodayView.NONE;
		List<MilestoneView.Row> rows = Arrays.asList(new MilestoneView.Row("Log 25 species", 25, 25, "3 Iron jars",
			true, false), new MilestoneView.Row("Log 100 species", 80, 100, "5 Rune jars", false, false));
		MilestoneView milestones = new MilestoneView("Log 100 species", 80, 100, rows);
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, Collections.emptyList(),
			Collections.emptyList(), log, caught, seen, Collections.emptyList(), Collections.emptyList(), view,
			TaskView.NONE, new EnumMap<>(ItemType.class)).withGoals(milestones, EliteView.NONE);
	}

	private static PartyRig page(PanelState state, int width, int height)
	{
		PartyRig rig = new PartyRig();
		rig.width = width;
		rig.height = height;
		rig.publish(state, true);
		rig.open();
		rig.shell.showTab(WindowTab.COLLECTION, rig.now);
		rig.paint();
		return rig;
	}

	private static List<String> words(PartyRig rig)
	{
		rig.drawn.clear();
		rig.paint();
		List<String> words = new ArrayList<>();
		for (PartyRig.Drawn d : rig.drawn)
			words.add(d.text);

		return words;
	}

	@Test
	public void theHeaderShowsTheRecordTheRewardAndTodaysSpawns()
	{
		List<LogEntry> log = log(SPECIES);
		PanelState state = state(log, true);
		PartyRig rig = page(state, 1344, 874);
		List<String> words = words(rig);

		assertTrue(words.toString(), words.contains("Caught " + Milestones.count(state.caughtSpecies()) + " / "
			+ Milestones.count(state.totalSpecies())));
		assertTrue(words.toString(), words.contains("Seen " + Milestones.count(state.seenSpecies())));
		assertTrue(words.toString(), words.contains("Next reward: 5 Rune jars at Log 100 species"));
		assertTrue(words.toString(), words.contains("Today · resets in 5h 12m"));
		assertTrue(words.toString(), words.contains("Goblin"));
		assertTrue(words.toString(), words.contains("Uncommon"));
		assertTrue(words.toString(), words.contains("Dragon weekend: x2 wild, x2 gilded"));
		assertTrue(words.toString(), words.contains("Night: undead and demons x2 wild"));
	}

	@Test
	public void theSpeciesMilestoneListIsNotOnThisPage()
	{
		PartyRig rig = page(state(log(SPECIES), true), 1344, 874);
		for (String word : words(rig))
			assertFalse(word, word.equals("Species milestones"));
	}

	@Test
	public void aKnownRowsTooltipHasLevelStyleAndRarityAndAnUnseenRowsIsEmpty()
	{
		PartyRig rig = page(state(log(SPECIES), false), 765, 497);

		assertEquals("Lv 2 · Stab · Common · gilded owned", rig.hit(CollectionPage.rowId(0)).tip());
		assertEquals("", rig.hit(CollectionPage.rowId(2)).tip());
		assertTrue(CollectionPage.tip(log(SPECIES).get(5)).contains("quest only, no longer catchable"));
	}

	@Test
	public void theDetailStripWritesTheRecordTheExamineAndTheEvolutionLine()
	{
		PartyRig rig = page(state(log(SPECIES), false), 1344, 874);
		rig.click(CollectionPage.rowId(0));
		assertTrue(words(rig).contains("Caught 1 · gilded owned"));

		rig.click(CollectionPage.rowId(1));
		List<String> words = words(rig);
		assertTrue(words.toString(), words.contains("Seen, not caught yet"));
		assertTrue(words.toString(), words.contains("Imp → Lesser demon → ???"));
		assertTrue(rig.hit(CollectionPage.ID_DETAIL).tip().contains("Imp → Lesser demon → ???"));
	}

	@Test
	public void theFilterButtonCyclesThroughTheFiltersAndCountsTheRows()
	{
		List<LogEntry> log = log(SPECIES);
		PartyRig rig = page(state(log, true), 765, 497);
		CollectionPage collection = rig.pages.collectionPage();
		assertEquals(SPECIES, collection.shownCount());

		rig.click(CollectionPage.ID_FILTER);
		assertEquals(CollectionFilter.CAUGHT, collection.filter());
		assertEquals(count(log, CollectionFilter.CAUGHT), collection.shownCount());
		rig.click(CollectionPage.ID_FILTER);
		assertEquals(count(log, CollectionFilter.SEEN_ONLY), collection.shownCount());
		rig.click(CollectionPage.ID_FILTER);
		assertEquals(count(log, CollectionFilter.NOT_SEEN), collection.shownCount());
		rig.click(CollectionPage.ID_FILTER);
		assertEquals(CollectionFilter.FEATURED, collection.filter());
		assertTrue(collection.shownCount() > 0 && collection.shownCount() <= 5);
		assertTrue(words(rig).contains("Show: Featured"));
		rig.click(CollectionPage.ID_FILTER);
		assertEquals(CollectionFilter.ALL, collection.filter());
	}

	private static int count(List<LogEntry> log, CollectionFilter filter)
	{
		int total = 0;
		for (LogEntry entry : log)
			total += filter.matches(entry, Collections.emptySet()) ? 1 : 0;

		return total;
	}

	@Test
	public void onlyTheVisibleRowsAreDeclaredAndPainted()
	{
		PartyRig rig = page(state(log(SPECIES), false), 535, 348);
		int rows = 0;
		for (String id : rig.ids())
			rows += CollectionPage.rowIndex(id) >= 0 ? 1 : 0;

		assertTrue("rows: " + rows, rows > 0 && rows < 20);
		assertTrue(rig.pages.collectionPage().list().scrollable());
	}

	@Test
	public void theListScrollsWithTheWheelTheBarAndTheKeys()
	{
		PartyRig rig = page(state(log(SPECIES), false), 535, 348);
		ScrollList list = rig.pages.collectionPage().list();
		Rectangle view = list.viewport();
		rig.shell.onWheel(view.x + 5, view.y + 5, 2);
		assertTrue("the wheel scrolls", list.offset() > 0);

		Rectangle thumb = list.thumbBounds();
		int before = list.offset();
		rig.shell.onPressed(thumb.x + 1, thumb.y + 1, MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onDragged(thumb.x + 1, thumb.y + 21);
		rig.shell.onReleased(thumb.x + 1, thumb.y + 21, MouseEvent.BUTTON1, rig.now);
		assertTrue("the bar scrolls", list.offset() > before);

		rig.key(KeyEvent.VK_END);
		assertEquals(list.maxOffset(), list.offset());
		assertEquals(SPECIES - 1, rig.pages.collectionPage().list().lastVisible());
		rig.key(KeyEvent.VK_HOME);
		assertEquals(0, list.offset());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);
		assertEquals("Rat", rig.pages.collectionPage().selectedEntry().name());
		rig.key(KeyEvent.VK_PAGE_DOWN);
		rig.key(KeyEvent.VK_PAGE_DOWN);
		assertTrue("the keys scroll the list along", list.offset() > 0);
		int chosen = rig.pages.collectionPage().list().firstVisible();
		assertTrue(chosen >= 0);
		rig.key(KeyEvent.VK_PAGE_UP);
		rig.key(KeyEvent.VK_UP);
		assertNotNull(rig.pages.collectionPage().selectedEntry());
	}

	@Test
	public void aClickOnTheBarsGrooveDoesNotChooseTheRowUnderIt()
	{
		PartyRig rig = page(state(log(SPECIES), false), 535, 348);
		ScrollList list = rig.pages.collectionPage().list();
		Rectangle track = list.trackBounds();

		rig.shell.onPressed(track.x + 1, track.y + track.height - 2, MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onReleased(track.x + 1, track.y + track.height - 2, MouseEvent.BUTTON1, rig.now);

		assertTrue(list.offset() > 0);
		assertEquals(null, rig.pages.collectionPage().selectedEntry());
	}

	@Test
	public void rightClickOnAKnownRowOpensOpenWikiAndAnUnseenRowHasNoMenu()
	{
		PartyRig rig = page(state(log(SPECIES), false), 765, 497);

		rig.click(CollectionPage.rowId(2), MouseEvent.BUTTON3);
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());

		rig.click(CollectionPage.rowId(0), MouseEvent.BUTTON3);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(Collections.singletonList("openWiki Goblin"), rig.calls);
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
	}

	@Test
	public void enterOnTheChosenRowOpensTheMenuFromTheKeyboard()
	{
		PartyRig rig = page(state(log(SPECIES), false), 765, 497);
		rig.click(CollectionPage.rowId(0));
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(Collections.singletonList("openWiki Goblin"), rig.calls);
	}

	@Test
	public void theMenuOfAnUnseenSpeciesIsEmptyAndTheWikiAddressEncodesTheName()
	{
		List<LogEntry> log = log(SPECIES);
		assertTrue(CollectionPage.menuItems(log.get(2), name ->
		{
		}).isEmpty());
		assertEquals(1, CollectionPage.menuItems(log.get(0), name ->
		{
		}).size());
		assertEquals("https://oldschool.runescape.wiki/w/Bruise_Blamish_Snail",
			WikiLinks.urlFor("Bruise Blamish Snail"));
	}

	@Test
	public void anEmptyLogStillPaints()
	{
		PartyRig rig = page(state(Collections.emptyList(), false), 535, 348);
		assertEquals(0, rig.pages.collectionPage().shownCount());
		rig.key(KeyEvent.VK_DOWN);
		assertTrue(words(rig).contains("0 species"));
	}

	@Test
	public void everyStringStaysInsideItsBoxAtEverySizeWithTheFullList()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : new int[][] {{535, 348}, {765, 497}, {1344, 874}})
			for (boolean today : new boolean[] {false, true})
				for (int pick : new int[] {-1, 0, 1})
				{
					PartyRig rig = page(state(log(SPECIES), today), canvas[0], canvas[1]);
					if (pick >= 0)
						rig.click(CollectionPage.rowId(pick));

					rig.pages.collectionPage().list().setOffset(pick > 0 ? 40 : 0);
					rig.drawn.clear();
					rig.paint();
					Rectangle body = WindowLayout.of(rig.width, rig.height).body;
					assertFalse(rig.drawn.isEmpty());
					for (PartyRig.Drawn d : rig.drawn)
					{
						if (d.width > d.box.width || d.height > d.box.height + 1 || !body.contains(d.box))
							problems.append(canvas[0] + " " + d + "\n");
					}

					for (Hit hit : rig.shell.hits())
					{
						if (!body.contains(hit.bounds()) && hit.kind() != Hit.Kind.TAB && hit.order() < 0)
							problems.append(canvas[0] + " hit " + hit.id() + " " + hit.bounds() + "\n");
					}
				}

		assertEquals(problems.toString(), "", problems.toString());
	}
}
