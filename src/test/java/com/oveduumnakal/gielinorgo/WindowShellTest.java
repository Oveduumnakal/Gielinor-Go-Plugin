/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the shell of the painted window with no RuneLite: opening and closing, the six tabs and the greyed Storage
 * tab, clicks and keys sharing one highlight, the back order, a menu and a dialog over the window, the draft kept when
 * it closes, and the footer buttons.
 */
public class WindowShellTest
{
	private static final int W = 535;
	private static final int H = 348;

	private final List<String> calls = new ArrayList<>();
	private final BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
	private WindowTheme theme = WindowTheme.DARK_GLASS;
	private MotionMode motion = MotionMode.OFF;
	private boolean hints = true;
	private boolean keyboard = true;
	private int depth;
	private List<Hit> pageHits = Collections.emptyList();
	private long now = 1_000_000L;
	private final WindowShell shell = new WindowShell(new WindowShell.Host()
	{
		@Override
		public WindowTheme theme()
		{
			return theme;
		}

		@Override
		public MotionMode motion()
		{
			return motion;
		}

		@Override
		public boolean keyHints()
		{
			return hints;
		}

		@Override
		public boolean keyboard()
		{
			return keyboard;
		}

		@Override
		public String sizeLabel()
		{
			return "Size: Medium";
		}

		@Override
		public void resetWindow()
		{
			calls.add("reset");
		}

		@Override
		public void flipTheme()
		{
			calls.add("flip");
		}

		@Override
		public void nextSize()
		{
			calls.add("size");
		}

		@Override
		public void notice(NoticeMessage message)
		{
			calls.add("notice " + message.name());
		}
	});

	private void withPages()
	{
		shell.pages(new WindowShell.PageActions()
		{
			@Override
			public List<Hit> hits(WindowTab tab, Rectangle body)
			{
				return tab == WindowTab.PARTY ? pageHits : Collections.emptyList();
			}

			@Override
			public void press(Hit hit)
			{
				calls.add("press " + hit.id());
			}

			@Override
			public void menu(Hit hit, int x, int y, boolean viaKeys)
			{
				calls.add("menu " + hit.id() + (viaKeys ? " keys" : " mouse"));
			}

			@Override
			public void wheel(int x, int y, int rotation)
			{
				calls.add("wheel " + rotation);
			}

			@Override
			public int depth()
			{
				return depth;
			}

			@Override
			public void back()
			{
				calls.add("back");
				depth--;
			}
		});
	}

	private static PanelState state(int team)
	{
		List<CreatureView> creatures = new ArrayList<>();
		for (int i = 0; i < team; i++)
			creatures.add(new CreatureView("Goblin", 5, 0.5, 1.0, Rarity.COMMON, CombatType.STAB, false, ""));

		return new PanelState(1, 0.0, 0, new EnumMap<>(JarType.class), JarType.BRONZE, 0, creatures,
			Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(),
			Collections.emptyList(), TodayView.NONE, TaskView.NONE);
	}

	private void snapshot(boolean storage)
	{
		shell.publish(WindowSnapshot.of(1, state(3), storage));
	}

	private boolean paint()
	{
		Graphics2D g = image.createGraphics();
		try
		{
			return shell.paint(g, W, H, now);
		}
		finally
		{
			g.dispose();
		}
	}

	private void openPainted(boolean storage)
	{
		snapshot(storage);
		shell.open(now);
		paint();
	}

	private Hit hit(String id)
	{
		for (Hit hit : shell.hits())
			if (hit.id().equals(id))
				return hit;

		throw new AssertionError("no hit " + id);
	}

	private void click(String id, int button)
	{
		Rectangle b = hit(id).bounds();
		int x = b.x + b.width / 2;
		int y = b.y + b.height / 2;
		shell.onPressed(x, y, button, 0, now);
		shell.onReleased(x, y, button, now);
	}

	private void click(String id)
	{
		click(id, MouseEvent.BUTTON1);
	}

	private void key(int code)
	{
		shell.onKey(code, 0, now);
	}

	@Test
	public void aClosedShellPaintsNothingAndTakesNoKeys()
	{
		assertFalse(paint());
		assertFalse(shell.isOpen());
		assertFalse(shell.isVisible());
		assertEquals(WindowKeyRules.KeyMode.NONE, shell.keyMode());
		assertEquals(0, image.getRGB(W / 2, H / 2) >>> 24);
	}

	@Test
	public void openingGivesKeyFocusAndPaintsTheWindow()
	{
		openPainted(false);

		assertTrue(shell.isOpen());
		assertTrue(shell.focus().focused());
		assertEquals(WindowTab.PARTY, shell.tab());
		assertEquals(WindowKeyRules.KeyMode.WINDOW, shell.keyMode());
		assertNotEquals(0, image.getRGB(W / 2, H / 2) >>> 24);
	}

	@Test
	public void thereAreSixTabsAndFourFooterAndHeaderButtonsInOrder()
	{
		openPainted(true);
		List<String> ids = new ArrayList<>();
		for (Hit hit : shell.hits())
			ids.add(hit.id());

		assertEquals(Arrays.asList("tab:PARTY", "tab:BAG", "tab:STORAGE", "tab:COLLECTION", "tab:TASKS", "tab:BATTLE",
			"btn:close", "btn:reset", "btn:theme", "btn:size"), ids);
		for (Hit hit : shell.hits())
			assertTrue(hit.id(), new Rectangle(0, 0, W, H).contains(hit.bounds()));
	}

	@Test
	public void clickingATabChoosesItAndARightClickDoesNot()
	{
		openPainted(true);

		click("tab:BAG", MouseEvent.BUTTON3);
		assertEquals(WindowTab.PARTY, shell.tab());

		click("tab:BAG");
		assertEquals(WindowTab.BAG, shell.tab());
		click("tab:STORAGE");
		assertEquals(WindowTab.STORAGE, shell.tab());
	}

	@Test
	public void storageIsGreyedAwayFromABankAndClickingItShakesAndSaysWhy()
	{
		openPainted(false);

		assertFalse(hit("tab:STORAGE").enabled());
		assertEquals(TeamSwapRule.REFUSAL, hit("tab:STORAGE").tip());
		click("tab:STORAGE");

		assertEquals(WindowTab.PARTY, shell.tab());
		assertEquals(List.of("notice STORAGE_LOCKED"), calls);
	}

	@Test
	public void storageOpensAgainOnceTheTeamMayBeChanged()
	{
		openPainted(false);
		snapshot(true);
		paint();

		assertTrue(hit("tab:STORAGE").enabled());
		click("tab:STORAGE");
		assertEquals(WindowTab.STORAGE, shell.tab());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void leftAndRightWrapAroundTheTabsAndSkipAGreyedStorage()
	{
		openPainted(false);

		key(KeyEvent.VK_LEFT);
		assertEquals(WindowTab.BATTLE, shell.tab());
		key(KeyEvent.VK_RIGHT);
		assertEquals(WindowTab.PARTY, shell.tab());
		key(KeyEvent.VK_RIGHT);
		assertEquals(WindowTab.BAG, shell.tab());
		key(KeyEvent.VK_RIGHT);
		assertEquals(WindowTab.COLLECTION, shell.tab());
		key(KeyEvent.VK_LEFT);
		assertEquals(WindowTab.BAG, shell.tab());
		assertEquals("tab:BAG", shell.focus().highlight());
	}

	@Test
	public void rightLandsOnStorageWhenItIsOpen()
	{
		openPainted(true);

		key(KeyEvent.VK_RIGHT);
		key(KeyEvent.VK_RIGHT);
		assertEquals(WindowTab.STORAGE, shell.tab());
	}

	@Test
	public void storageGoingAwayMovesTheWindowBackToTheFirstTab()
	{
		openPainted(true);
		click("tab:STORAGE");
		snapshot(false);
		paint();

		assertEquals(WindowTab.PARTY, shell.tab());
	}

	@Test
	public void reopeningOnAGreyedStorageTabStartsOnParty()
	{
		openPainted(true);
		click("tab:STORAGE");
		shell.close(now, ShellMotion.Phase.CLOSE);
		snapshot(false);
		shell.open(now);

		assertEquals(WindowTab.PARTY, shell.tab());
	}

	@Test
	public void upAndDownWalkTheButtonsAndEnterPressesTheHighlightedOne()
	{
		openPainted(true);

		key(KeyEvent.VK_DOWN);
		assertEquals("btn:close", shell.focus().highlight());
		key(KeyEvent.VK_DOWN);
		assertEquals("btn:reset", shell.focus().highlight());
		key(KeyEvent.VK_ENTER);
		key(KeyEvent.VK_DOWN);
		key(KeyEvent.VK_SPACE);
		key(KeyEvent.VK_DOWN);
		key(KeyEvent.VK_ENTER);
		key(KeyEvent.VK_DOWN);
		assertEquals("btn:close", shell.focus().highlight());

		assertEquals(List.of("reset", "flip", "size"), calls);
	}

	@Test
	public void clickingTheFooterButtonsCallsTheHost()
	{
		openPainted(true);

		click("btn:reset");
		click("btn:theme");
		click("btn:size");

		assertEquals(List.of("reset", "flip", "size"), calls);
	}

	@Test
	public void clickingCloseClosesTheWindow()
	{
		openPainted(true);
		click("btn:close");

		assertFalse(shell.isOpen());
		assertEquals(WindowKeyRules.KeyMode.NONE, shell.keyMode());
	}

	@Test
	public void escClosesButBackspaceNeverDoes()
	{
		openPainted(true);

		key(KeyEvent.VK_BACK_SPACE);
		assertTrue(shell.isOpen());
		key(KeyEvent.VK_ESCAPE);
		assertFalse(shell.isOpen());
	}

	@Test
	public void backGoesMenuThenPageThenClosed()
	{
		withPages();
		depth = 1;
		openPainted(true);
		shell.openMenu(List.of(ContextMenu.Item.of("Summary", () -> calls.add("summary"))), 40, 40, true, now);
		assertEquals(WindowKeyRules.KeyMode.MENU, shell.keyMode());

		key(KeyEvent.VK_ESCAPE);
		assertEquals(WindowKeyRules.KeyMode.WINDOW, shell.keyMode());
		assertTrue(calls.isEmpty());

		key(KeyEvent.VK_ESCAPE);
		assertEquals(List.of("back"), calls);
		assertTrue(shell.isOpen());

		key(KeyEvent.VK_BACK_SPACE);
		assertTrue(shell.isOpen());

		key(KeyEvent.VK_ESCAPE);
		assertFalse(shell.isOpen());
	}

	private void rows()
	{
		withPages();
		pageHits = Arrays.asList(
			new Hit("row:0", Hit.Kind.ROW, new Rectangle(30, 90, 300, 40), WindowShell.PAGE_ORDER, true, ""),
			new Hit("row:1", Hit.Kind.ROW, new Rectangle(30, 140, 300, 40), WindowShell.PAGE_ORDER + 1, true, ""),
			new Hit("row:2", Hit.Kind.ROW, new Rectangle(30, 190, 300, 40), WindowShell.PAGE_ORDER + 2, false, ""));
		openPainted(true);
	}

	@Test
	public void aLeftClickOnARowGoesStraightToItsPageAndARightClickAsksForItsMenu()
	{
		rows();

		click("row:1");
		click("row:1", MouseEvent.BUTTON3);

		assertEquals(List.of("press row:1", "menu row:1 mouse"), calls);
	}

	@Test
	public void enterAndSpaceOnAHighlightedRowOpenItsMenuWithTheFirstItemHighlighted()
	{
		rows();

		key(KeyEvent.VK_DOWN);
		assertEquals("btn:close", shell.focus().highlight());
		key(KeyEvent.VK_DOWN);
		assertEquals("row:0", shell.focus().highlight());
		key(KeyEvent.VK_ENTER);
		key(KeyEvent.VK_SPACE);

		assertEquals(List.of("menu row:0 keys", "menu row:0 keys"), calls);
	}

	@Test
	public void theMouseAndTheKeysShareOneHighlightAndDisabledRowsAreSkipped()
	{
		rows();
		Rectangle b = hit("row:1").bounds();
		shell.onMoved(b.x + 5, b.y + 5, now);
		assertEquals("row:1", shell.focus().highlight());

		key(KeyEvent.VK_DOWN);
		assertEquals("btn:reset", shell.focus().highlight());
		key(KeyEvent.VK_UP);
		assertEquals("row:1", shell.focus().highlight());

		Rectangle off = hit("row:2").bounds();
		shell.onMoved(off.x + 5, off.y + 5, now);
		assertNull(shell.focus().highlight());
	}

	@Test
	public void aMenuTakesTheKeysAndAClickOutsideItClosesItWithoutPressingWhatLiesBeneath()
	{
		rows();
		shell.openMenu(List.of(ContextMenu.Item.of("Summary", () -> calls.add("summary")),
			ContextMenu.Item.disabled("Pin", "soon"), ContextMenu.Item.of("Move", () -> calls.add("move"))),
			200, 100, true, now);

		key(KeyEvent.VK_DOWN);
		key(KeyEvent.VK_ENTER);
		assertEquals(List.of("move"), calls);
		assertEquals(WindowKeyRules.KeyMode.WINDOW, shell.keyMode());

		calls.clear();
		shell.openMenu(List.of(ContextMenu.Item.of("Summary", () -> calls.add("summary"))), 400, 250, false, now);
		click("row:0");
		assertTrue(calls.isEmpty());
		assertEquals(WindowKeyRules.KeyMode.WINDOW, shell.keyMode());
	}

	private Modal renameModal(TextBox box)
	{
		return new Modal("Rename", "", Arrays.asList("Cancel", "Save"), 1, index -> calls.add("choice " + index),
			() -> calls.add("cancel")).withField(box, 1);
	}

	@Test
	public void aDialogWithATextBoxTakesEveryKeyAndSavesOnEnter()
	{
		openPainted(true);
		TextBox box = new TextBox();
		shell.openModal(renameModal(box), box, now);

		assertEquals(WindowKeyRules.KeyMode.FIELD, shell.keyMode());
		shell.onTyped('B', 0, now);
		shell.onTyped('o', 0, now);
		shell.onTyped('b', 0, now);
		key(KeyEvent.VK_ENTER);

		assertEquals(List.of("choice 1"), calls);
		assertEquals("Bob", box.text());
		assertEquals(WindowKeyRules.KeyMode.WINDOW, shell.keyMode());
	}

	@Test
	public void closingWithAnOpenTextBoxKeepsTheTextAsADraftAndRunsNothing()
	{
		openPainted(true);
		TextBox box = new TextBox();
		shell.openModal(renameModal(box), box, now);
		shell.onTyped('Z', 0, now);
		shell.onTyped('e', 0, now);

		shell.close(now, ShellMotion.Phase.COMBAT_CLOSE);

		assertTrue(calls.isEmpty());
		assertEquals("Ze", shell.draft());
		assertEquals("Ze", shell.takeDraft());
		assertEquals("", shell.draft());
		assertEquals(WindowKeyRules.KeyMode.NONE, shell.keyMode());
	}

	@Test
	public void aDialogWithoutABoxTakesLeftAndRightAndDefaultsToTheFirstButton()
	{
		openPainted(true);
		Modal modal = new Modal("Release?", "Gone for good.", Arrays.asList("No", "Yes"), 0,
			index -> calls.add("choice " + index), () -> calls.add("cancel"));
		shell.openModal(modal, null, now);

		assertEquals(WindowKeyRules.KeyMode.MODAL, shell.keyMode());
		key(KeyEvent.VK_RIGHT);
		key(KeyEvent.VK_LEFT);
		key(KeyEvent.VK_ENTER);

		assertEquals(List.of("choice 0"), calls);
	}

	@Test
	public void aClosedDialogNeverRunsItsChoiceAfterTheWindowClosed()
	{
		openPainted(true);
		Modal modal = new Modal("Release?", "", Arrays.asList("No", "Yes"), 0, index -> calls.add("choice " + index),
			() -> calls.add("cancel"));
		shell.openModal(modal, null, now);
		shell.close(now, ShellMotion.Phase.COMBAT_CLOSE);
		modal.choose(1);

		assertTrue(calls.isEmpty());
	}

	@Test
	public void losingFocusStopsTheKeysAndAClickInsideGivesItBack()
	{
		openPainted(true);
		shell.focusLost(now);

		assertFalse(shell.view(new Rectangle(0, 0, W, H)).focused());
		shell.onPressed(5, 5, MouseEvent.BUTTON1, 0, now);
		assertTrue(shell.view(new Rectangle(0, 0, W, H)).focused());
	}

	@Test
	public void focusLostKeepsATextBoxsTextAndTakesItsKeys()
	{
		openPainted(true);
		TextBox box = new TextBox();
		shell.openModal(renameModal(box), box, now);
		shell.onTyped('A', 0, now);
		shell.focusLost(now);

		assertEquals("A", box.text());
		assertFalse(box.focused());
		assertEquals(WindowKeyRules.KeyMode.NONE, shell.keyMode());
	}

	@Test
	public void theWheelReachesThePagesOnlyWhileNothingIsOverTheWindow()
	{
		withPages();
		openPainted(true);
		shell.onWheel(50, 50, -1);
		shell.openMenu(List.of(ContextMenu.Item.of("Summary", () -> calls.add("summary"))), 40, 40, true, now);
		shell.onWheel(50, 50, 1);

		assertEquals(List.of("wheel -1"), calls);
	}

	@Test
	public void theViewCarriesTheBoundsTheHitsTheKeyModeAndTheFocus()
	{
		openPainted(true);
		WindowView view = shell.view(new Rectangle(10, 20, W, H));

		assertTrue(view.open());
		assertEquals(10, view.bounds().x);
		List<Hit> hits = shell.hits();
		assertEquals(hits.size(), view.hits().size());
		assertEquals(WindowKeyRules.KeyMode.WINDOW, view.keys());
		assertTrue(view.focused());
		Rectangle bag = hit("tab:BAG").bounds();
		assertEquals("tab:BAG", view.hitAt(10 + bag.x + 3, 20 + bag.y + 3).id());
	}

	@Test
	public void leavingTheWindowClearsAHoverButNotAKeyHighlight()
	{
		openPainted(true);
		Rectangle b = hit("btn:reset").bounds();
		shell.onMoved(b.x + 3, b.y + 3, now);
		assertEquals("btn:reset", shell.focus().highlight());
		shell.onLeft();
		assertNull(shell.focus().highlight());

		key(KeyEvent.VK_DOWN);
		shell.onLeft();
		assertEquals("btn:close", shell.focus().highlight());
	}

	@Test
	public void theStyleSwitchesOnTheNextPaint()
	{
		openPainted(true);
		int dark = image.getRGB(W / 2, 3);
		theme = WindowTheme.RUNELITE_DARK;
		paint();

		assertNotEquals(dark, image.getRGB(W / 2, 3));
	}

	@Test
	public void fullMotionSlidesTheTabBarAndOffDoesNot()
	{
		motion = MotionMode.FULL;
		openPainted(true);
		click("tab:BAG");
		assertTrue(shell.animations().active("tabBar", now));

		now += 500;
		paint();
		assertFalse(shell.animations().active("tabBar", now));

		motion = MotionMode.OFF;
		click("tab:TASKS");
		assertFalse(shell.animations().active("tabBar", now));
	}

	@Test
	public void closingFadesOutInFullMotionAndGoesAtOnceWhenOff()
	{
		motion = MotionMode.FULL;
		openPainted(true);
		shell.close(now, ShellMotion.Phase.CLOSE);
		now += 40;

		assertTrue(paint());
		assertTrue(shell.isVisible());
		assertFalse(shell.isOpen());

		now += 200;
		assertFalse(paint());
		assertFalse(shell.isVisible());

		motion = MotionMode.OFF;
		shell.open(now);
		paint();
		shell.close(now, ShellMotion.Phase.CLOSE);
		assertFalse(paint());
	}

	@Test
	public void aWindowThatIsClosedAndFadingTakesNoInput()
	{
		motion = MotionMode.FULL;
		openPainted(true);
		shell.close(now, ShellMotion.Phase.CLOSE);
		Rectangle b = hit("tab:BAG").bounds();
		shell.onPressed(b.x + 3, b.y + 3, MouseEvent.BUTTON1, 0, now);
		shell.onReleased(b.x + 3, b.y + 3, MouseEvent.BUTTON1, now);
		key(KeyEvent.VK_RIGHT);

		assertEquals(WindowTab.PARTY, shell.tab());
	}

	@Test
	public void somethingWaitingShowsOnlyWhileTheWindowIsClosed()
	{
		List<EggView> eggs = Collections.singletonList(new EggView(Rarity.COMMON, 0, 100));
		PanelState ready = new PanelState(1, 0.0, 0, new EnumMap<>(JarType.class), JarType.BRONZE, 0,
			Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, 0, eggs,
			Collections.emptyList(), TodayView.NONE, TaskView.NONE);
		shell.publish(WindowSnapshot.of(2, ready, false));

		assertTrue(shell.waiting());
		shell.open(now);
		assertFalse(shell.waiting());
	}

	@Test
	public void aTaskDoneSinceTheLastLookShowsTheDotUntilTheWindowOpens()
	{
		shell.publish(WindowSnapshot.of(1, state(2), false));
		assertFalse(shell.waiting());

		TaskView done = new TaskView(List.of(new TaskView.Row("Catch 3", 3, 3, true, "2 Iron jars", false)), 1, 3, 0L,
			0L);
		PanelState finished = new PanelState(1, 0.0, 0, new EnumMap<>(JarType.class), JarType.BRONZE, 0,
			Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(),
			Collections.emptyList(), TodayView.NONE, done);
		shell.publish(WindowSnapshot.of(2, finished, false));
		assertTrue(shell.waiting());

		shell.open(now);
		paint();
		shell.close(now, ShellMotion.Phase.CLOSE);
		assertFalse(shell.waiting());
	}

	@Test
	public void theHintLineChangesWithFocusAndSettingsWithoutBreakingThePaint()
	{
		openPainted(true);
		shell.focusLost(now);
		assertTrue(paint());

		hints = false;
		assertTrue(paint());

		hints = true;
		keyboard = false;
		assertTrue(paint());
	}
}
