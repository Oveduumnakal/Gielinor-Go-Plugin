/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the Party and Summary pages inside the real shell with a recording stand-in for the game: a left click opens
 * the Summary, a right click or Enter opens the menu with Summary first, the keyboard back-chain goes menu, Summary,
 * Party, closed, every menu item does what the old panel did, the rename box keeps a draft, and a sliding row refuses
 * clicks.
 */
public class WindowPagesTest
{
	private final PartyRig rig = new PartyRig();

	private void open(int team, boolean storage)
	{
		rig.publish(PartyRig.state(PartyRig.team(team)), storage);
		rig.open();
	}

	private void highlightRow(int place)
	{
		for (int i = 0; i < 12 && !WindowPages.rowId(place).equals(rig.shell.focus().highlight()); i++)
			rig.key(KeyEvent.VK_DOWN);

		assertEquals(WindowPages.rowId(place), rig.shell.focus().highlight());
	}

	private void type(String text)
	{
		for (char c : text.toCharArray())
			rig.shell.onTyped(c, 0, rig.now);

		rig.paint();
	}

	private void openMenuOnRow(int place)
	{
		rig.click(WindowPages.rowId(place), MouseEvent.BUTTON3);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
	}

	private void chooseMenuItem(int downs)
	{
		for (int i = 0; i < downs; i++)
			rig.key(KeyEvent.VK_DOWN);

		rig.key(KeyEvent.VK_ENTER);
	}

	@Test
	public void thereIsARowHitForEveryCreatureAndNoneForAnEmptySlot()
	{
		open(4, true);

		assertTrue(rig.ids().containsAll(Arrays.asList("row:0", "row:1", "row:2", "row:3")));
		assertFalse(rig.ids().contains("row:4"));
		assertEquals(Hit.Kind.ROW, rig.hit("row:2").kind());
		assertEquals(WindowShell.PAGE_ORDER + 2, rig.hit("row:2").order());
		assertTrue(rig.hit("row:1").tip()
			.startsWith("Moo (Cow)"));
	}

	@Test
	public void aLeftClickOnARowOpensTheSummaryOfThatCreature()
	{
		open(4, true);

		rig.click("row:2");

		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());
		assertEquals(2, rig.pages.pageState().index());
		assertEquals(1, rig.pages.depth());
		assertTrue(rig.ids().contains(WindowPages.ID_SUMMARY_MENU));
		assertFalse(rig.ids().contains("row:2"));
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void aRightClickOpensTheMenuAndDownThenEnterChoosesSummaryFirst()
	{
		open(4, true);

		openMenuOnRow(1);
		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());
		chooseMenuItem(1);

		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());
		assertEquals(1, rig.pages.pageState().index());
	}

	@Test
	public void enterOnAHighlightedRowOpensItsMenuWithSummaryHighlighted()
	{
		open(4, true);

		highlightRow(2);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());
		assertEquals(2, rig.pages.pageState().index());
	}

	@Test
	public void spaceOpensTheMenuToo()
	{
		open(3, true);

		highlightRow(0);
		rig.key(KeyEvent.VK_SPACE);

		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
	}

	@Test
	public void theBackChainGoesMenuThenSummaryThenPartyThenClosed()
	{
		open(3, true);
		highlightRow(1);
		rig.key(KeyEvent.VK_ENTER);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());

		rig.key(KeyEvent.VK_ENTER);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());

		rig.key(KeyEvent.VK_ESCAPE);
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());

		rig.key(KeyEvent.VK_ESCAPE);
		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());
		assertTrue(rig.shell.isOpen());

		rig.key(KeyEvent.VK_ESCAPE);
		assertFalse(rig.shell.isOpen());
	}

	@Test
	public void backspaceGoesBackALevelButNeverClosesTheWindow()
	{
		open(3, true);
		rig.click("row:0");

		rig.key(KeyEvent.VK_BACK_SPACE);
		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());

		rig.key(KeyEvent.VK_BACK_SPACE);
		assertTrue(rig.shell.isOpen());
	}

	@Test
	public void theBackButtonOnTheSummaryReturnsToTheList()
	{
		open(3, true);
		rig.click("row:1");

		rig.click(WindowPages.summaryButtonId(0));

		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());
		assertTrue(rig.ids().contains("row:1"));
	}

	@Test
	public void upAndDownOnTheSummaryGoToTheNeighbouringCreatureAndWrap()
	{
		open(3, true);
		rig.click("row:0");

		rig.key(KeyEvent.VK_DOWN);
		assertEquals(1, rig.pages.pageState().index());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);
		assertEquals(0, rig.pages.pageState().index());
		rig.key(KeyEvent.VK_UP);
		assertEquals(2, rig.pages.pageState().index());
		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void enterOnTheSummaryOpensTheCreaturesMenuWithoutSummaryInIt()
	{
		open(3, true);
		rig.click("row:2");

		rig.key(KeyEvent.VK_ENTER);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("setLead TEAM#2"), rig.calls);
	}

	@Test
	public void makeLeadFromTheMenuAsksTheGameForThatCreature()
	{
		open(4, true);

		openMenuOnRow(2);
		chooseMenuItem(2);

		assertEquals(Collections.singletonList("setLead TEAM#2"), rig.calls);
	}

	@Test
	public void makeLeadOnTheLeadAndDuringAnEliteRunDoesNothing()
	{
		open(3, true);
		rig.click("row:0");
		assertFalse(rig.hit(WindowPages.summaryButtonId(3)).enabled());
		assertTrue(rig.hit(WindowPages.summaryButtonId(3)).tip()
			.contains("Already the lead"));
		rig.click(WindowPages.summaryButtonId(3));
		assertTrue(rig.calls.isEmpty());
		rig.click(WindowPages.summaryButtonId(0));

		EliteView running = new EliteView(EliteView.Mode.RUNNING, 8, 2, 4, "Opponent", "Team", "Reward", false);
		rig.publish(PartyRig.state(PartyRig.team(3)).withGoals(MilestoneView.NONE, running), true);
		rig.paint();
		rig.click("row:1");
		assertFalse(rig.hit(WindowPages.summaryButtonId(3)).enabled());
		assertFalse(rig.hit(WindowPages.summaryButtonId(4)).enabled());
		assertTrue(rig.hit(WindowPages.summaryButtonId(3)).tip()
			.contains("Finish the Elite run"));
		rig.click(WindowPages.summaryButtonId(3));
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void moveToStorageIsDimAwayFromABankAndWorksWhereTheTeamMayChange()
	{
		open(3, false);
		rig.click("row:1");
		assertFalse(rig.hit(WindowPages.summaryButtonId(4)).enabled());
		assertTrue(rig.hit(WindowPages.summaryButtonId(4)).tip()
			.contains(ContextMenuRules.SWAP_REASON));
		rig.click(WindowPages.summaryButtonId(4));
		assertTrue(rig.calls.isEmpty());

		rig.publish(PartyRig.state(PartyRig.team(3)), true);
		rig.paint();
		rig.click(WindowPages.summaryButtonId(4));
		assertEquals(Collections.singletonList("moveToStorage TEAM#1"), rig.calls);
	}

	@Test
	public void theSummaryGoesBackToTheListWhenItsCreatureLeavesTheTeam()
	{
		open(3, true);
		List<CreatureView> team = PartyRig.team(3);
		rig.publish(PartyRig.state(team), true);
		rig.paint();
		rig.click("row:1");

		rig.publish(PartyRig.state(Arrays.asList(team.get(0), team.get(2))), true);
		rig.paint();

		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());
		assertEquals(0, rig.pages.depth());
	}

	@Test
	public void theSummaryFollowsACreatureThatBecameTheLead()
	{
		open(3, true);
		List<CreatureView> team = PartyRig.team(3);
		rig.publish(PartyRig.state(team), true);
		rig.paint();
		rig.click("row:2");

		rig.publish(PartyRig.state(Arrays.asList(team.get(2), team.get(0), team.get(1))), true);
		rig.paint();

		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());
		assertEquals(0, rig.pages.pageState().index());
	}

	@Test
	public void renameOpensTheBoxTypingAndEnterSaveTheName()
	{
		open(3, true);
		rig.click("row:0");

		rig.click(WindowPages.summaryButtonId(1));
		assertEquals(WindowKeyRules.KeyMode.FIELD, rig.shell.keyMode());
		type("Gobbo");
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("rename TEAM#0 Gobbo"), rig.calls);
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
	}

	@Test
	public void renameStartsWithTheCurrentNicknameAndAnEmptyNameResetsIt()
	{
		open(3, true);
		rig.click("row:1");
		rig.click(WindowPages.summaryButtonId(1));

		for (int i = 0; i < 3; i++)
			rig.key(KeyEvent.VK_BACK_SPACE);

		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("rename TEAM#1 "), rig.calls);
	}

	@Test
	public void escapeCancelsTheRenameAndAnInvalidNameIsNotSent()
	{
		open(3, true);
		rig.click("row:0");
		rig.click(WindowPages.summaryButtonId(1));
		type("Bad!");
		rig.key(KeyEvent.VK_ENTER);

		assertTrue(rig.calls.isEmpty());
		assertEquals(WindowKeyRules.KeyMode.FIELD, rig.shell.keyMode());

		rig.key(KeyEvent.VK_ESCAPE);

		assertTrue(rig.calls.isEmpty());
		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());
	}

	@Test
	public void ifTheWindowClosesMidRenameTheTextIsKeptAndNothingIsRenamed()
	{
		open(3, true);
		rig.click("row:0");
		rig.click(WindowPages.summaryButtonId(1));
		type("Abc");

		rig.shell.close(rig.now, ShellMotion.Phase.COMBAT_CLOSE);

		assertEquals("Abc", rig.shell.draft());
		assertTrue(rig.calls.isEmpty());
		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());

		rig.open();
		rig.click("row:0");
		rig.click(WindowPages.summaryButtonId(1));
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("rename TEAM#0 Abc"), rig.calls);
		assertEquals("", rig.shell.draft());
	}

	@Test
	public void hotkeysDoNotFireWhileTypingInTheRenameBox()
	{
		open(3, true);
		rig.click("row:0");
		rig.click(WindowPages.summaryButtonId(1));

		assertEquals(WindowKeyRules.KeyMode.FIELD, rig.shell.keyMode());
		assertTrue(WindowKeyRules.uses(WindowKeyRules.KeyMode.FIELD, KeyEvent.VK_A, 0));
		assertTrue(WindowKeyRules.uses(WindowKeyRules.KeyMode.FIELD, KeyEvent.VK_UP, 0));
	}

	@Test
	public void movesWithNoWaitingMoveOnlyShowsAndCloses()
	{
		open(3, true);
		rig.click("row:0");

		rig.click(WindowPages.summaryButtonId(2));
		assertEquals(WindowKeyRules.KeyMode.MODAL, rig.shell.keyMode());
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(WindowKeyRules.KeyMode.WINDOW, rig.shell.keyMode());
		assertTrue(rig.calls.isEmpty());
	}

	private List<CreatureView> withPendingMove()
	{
		Move spark = new Move("spark", "Spark", CombatType.MAGIC, 1.2, 0.9);
		Move slash = new Move("slash", "Slash", CombatType.SLASH, 1.0, 0.95);
		Move bash = new Move("bash", "Bash", CombatType.CRUSH, 1.4, 0.8);
		CreatureView waiting = new CreatureView("Goblin", 9, 0.4, 1.0, Rarity.COMMON, CombatType.SLASH, false, null,
			StatRolls.AVERAGE, 0, Arrays.asList(slash, bash), Collections.singletonList(spark));
		return Arrays.asList(PartyRig.creature("Cow", 5, 1.0, null), waiting);
	}

	@Test
	public void aWaitingMoveCanBeSkippedFromTheMovesDialog()
	{
		rig.publish(PartyRig.state(withPendingMove()), true);
		rig.open();
		rig.click("row:1");

		rig.click(WindowPages.ID_MOVES);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("resolveMove TEAM#1 -1"), rig.calls);
	}

	@Test
	public void aWaitingMoveCanReplaceAKnownOneThroughTheForgetMenu()
	{
		rig.publish(PartyRig.state(withPendingMove()), true);
		rig.open();
		rig.click("row:1");

		rig.click(WindowPages.ID_MOVES);
		rig.key(KeyEvent.VK_RIGHT);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		assertTrue(rig.calls.isEmpty());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("resolveMove TEAM#1 1"), rig.calls);
	}

	@Test
	public void useItemOpensTheListOfHelpfulItemsAndUsesTheChosenOne()
	{
		Map<ItemType, Integer> owned = new EnumMap<>(ItemType.class);
		owned.put(ItemType.SMALL_SALVE, 2);
		rig.publish(new PanelState(1, 0.0, 0, new EnumMap<>(JarType.class), JarType.BRONZE, 0, PartyRig.team(4),
			Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(),
			Collections.emptyList(), TodayView.NONE, TaskView.NONE, owned), true);
		rig.open();

		openMenuOnRow(3);
		chooseMenuItem(6);
		assertEquals(WindowKeyRules.KeyMode.MENU, rig.shell.keyMode());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("useItem TEAM#3 SMALL_SALVE"), rig.calls);
	}

	@Test
	public void theResetBannerAsksAgainWithNoFirstAndDismissHidesIt()
	{
		rig.publish(PartyRig.state(PartyRig.team(2)).withResetSummary("2 creatures"), true);
		rig.open();
		assertTrue(rig.ids().contains(WindowPages.ID_RESET_PROGRESS));

		rig.click(WindowPages.ID_RESET_PROGRESS);
		assertEquals(WindowKeyRules.KeyMode.MODAL, rig.shell.keyMode());
		rig.key(KeyEvent.VK_ENTER);
		assertTrue(rig.calls.isEmpty());

		rig.click(WindowPages.ID_RESET_PROGRESS);
		rig.key(KeyEvent.VK_RIGHT);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(Collections.singletonList("resetProgress"), rig.calls);

		rig.click(WindowPages.ID_DISMISS);
		assertFalse(rig.ids().contains(WindowPages.ID_RESET_PROGRESS));
		assertFalse(rig.ids().contains(WindowPages.ID_DISMISS));
	}

	@Test
	public void noBannerShowsWhenThereIsNothingToReset()
	{
		open(2, true);

		assertFalse(rig.ids().contains(WindowPages.ID_RESET_PROGRESS));
	}

	@Test
	public void aNewPlayerSeesTheStarterCardsAndClickingOneTakesIt()
	{
		List<StarterView> offer = Arrays.asList(
			new StarterView(PartyRig.creature("Imp", 5, 1.0, null), "Grows into Lesser demon (20)"),
			new StarterView(PartyRig.creature("Goblin", 5, 1.0, null), "Does not evolve"));
		rig.publish(PartyRig.state(Collections.emptyList()).withStarters(offer), false);
		rig.open();

		assertTrue(rig.ids().containsAll(Arrays.asList("starter:0", "starter:1")));
		assertFalse(rig.ids().contains("row:0"));
		rig.click("starter:1");

		assertEquals(Collections.singletonList("pickStarter 1"), rig.calls);
	}

	@Test
	public void anEmptyTeamOffersStorageOnlyWhereTheTeamCanBeChanged()
	{
		rig.publish(PartyRig.state(Collections.emptyList()), false);
		rig.open();
		assertFalse(rig.ids().contains(WindowPages.ID_STORAGE));

		rig.publish(PartyRig.state(Collections.emptyList()), true);
		rig.paint();
		assertTrue(rig.ids().contains(WindowPages.ID_STORAGE));
		rig.click(WindowPages.ID_STORAGE);

		assertEquals(WindowTab.STORAGE, rig.shell.tab());
	}

	@Test
	public void leavingTheTabAndComingBackShowsTheListAndClosingForgetsTheSummary()
	{
		open(3, true);
		rig.click("row:1");

		rig.click("tab:BAG");
		assertEquals(0, rig.pages.depth());
		rig.click("tab:PARTY");
		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());

		rig.click("row:1");
		rig.shell.close(rig.now, ShellMotion.Phase.CLOSE);
		rig.open();

		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());
	}

	@Test
	public void upAndDownOnTheListMoveTheHighlightAlongTheRowsAndWrap()
	{
		open(3, true);

		highlightRow(0);
		rig.key(KeyEvent.VK_DOWN);
		assertEquals("row:1", rig.shell.focus().highlight());
		rig.key(KeyEvent.VK_DOWN);
		assertEquals("row:2", rig.shell.focus().highlight());
		rig.key(KeyEvent.VK_UP);
		assertEquals("row:1", rig.shell.focus().highlight());
	}

	@Test
	public void aSlidingRowIgnoresClicksUntilItLandsAndThenTheClickLandsOnTheRightCreature()
	{
		rig.motion = MotionMode.FULL;
		List<CreatureView> team = PartyRig.team(3);
		rig.publish(PartyRig.state(team), true);
		rig.open();
		rig.now += 1_000;
		rig.paint();

		rig.publish(PartyRig.state(Arrays.asList(team.get(1), team.get(0), team.get(2))), true);
		rig.now += 10;
		rig.paint();

		assertFalse(rig.hit("row:0").enabled());
		assertFalse(rig.hit("row:1").enabled());
		assertTrue(rig.hit("row:2").enabled());
		rig.click("row:0");
		assertEquals(PageState.Page.PARTY, rig.pages.pageState().page());

		rig.now += WindowTimes.SLIDE + 20;
		rig.paint();
		assertTrue(rig.hit("row:0").enabled());
		rig.click("row:0");

		assertEquals(PageState.Page.SUMMARY, rig.pages.pageState().page());
		assertEquals(0, rig.pages.pageState().index());
		assertEquals("Moo", RowModel.of(rig.shell.snapshot().state().team().get(0), 0)
			.name());
	}

	@Test
	public void thePicturesAreAskedForOncePerSpeciesAndNotEveryFrame()
	{
		open(3, true);
		rig.paint();
		rig.paint();

		for (int count : rig.askedCounts().values())
			assertEquals(1, count);

		assertTrue(rig.askedCounts().containsKey("Goblin"));
		assertNotNull(rig.askedCounts().get("Imp"));
	}

	@Test
	public void theOtherTabsStillShowTheirPlaceholderAndHaveNoPageHits()
	{
		open(3, true);

		rig.click("tab:BAG");

		for (String id : rig.ids())
			assertFalse(id, id.startsWith("row:"));

		assertEquals(0, rig.pages.depth());
	}

	@Test
	public void aWindowWithNoLoadedSaveShowsNoPageHits()
	{
		rig.shell.open(rig.now);
		rig.paint();

		for (String id : rig.ids())
			assertFalse(id, id.startsWith("row:"));
	}
}
