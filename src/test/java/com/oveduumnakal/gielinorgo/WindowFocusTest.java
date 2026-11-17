/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the keyboard focus: the one shared highlight, wrapping, skipped items and the back order. */
public class WindowFocusTest
{
	private static Hit hit(String id, Hit.Kind kind, int order, boolean enabled)
	{
		return new Hit(id, kind, new Rectangle(0, order * 10, 50, 8), order, enabled, "");
	}

	private static WindowFocus focusOn(Hit... hits)
	{
		WindowFocus focus = new WindowFocus();
		focus.setHits(Arrays.asList(hits));
		return focus;
	}

	private static WindowFocus page()
	{
		return focusOn(hit("tab:A", Hit.Kind.TAB, 0, true), hit("row:1", Hit.Kind.ROW, 10, true),
			hit("row:2", Hit.Kind.ROW, 11, true), hit("row:3", Hit.Kind.ROW, 12, false),
			hit("row:4", Hit.Kind.ROW, 13, true), hit("btn", Hit.Kind.BUTTON, 100, true));
	}

	@Test
	public void downAndUpWrapAlongTheFocusOrder()
	{
		WindowFocus focus = page();

		assertEquals("row:1", focus.move(1).id());
		assertEquals("row:2", focus.move(1).id());
		assertEquals("row:4", focus.move(1).id());
		assertEquals("btn", focus.move(1).id());
		assertEquals("row:1", focus.move(1).id());
		assertEquals("btn", focus.move(-1).id());
		assertEquals("row:4", focus.move(-1).id());
	}

	@Test
	public void upWithNothingHighlightedGoesToTheLastAndDownToTheFirst()
	{
		Hit last = page().move(-1);
		Hit first = page().move(1);

		assertEquals("btn", last.id());
		assertEquals("row:1", first.id());
	}

	@Test
	public void disabledHitsAndTabsAreSkippedByUpAndDown()
	{
		WindowFocus focus = page();
		List<String> seen = new ArrayList<>();
		for (int i = 0; i < 8; i++)
			seen.add(focus.move(1).id());

		assertFalse(seen.contains("row:3"));
		assertFalse(seen.contains("tab:A"));
	}

	@Test
	public void aPageWithNothingToLandOnGivesNull()
	{
		WindowFocus focus = focusOn(hit("tab:A", Hit.Kind.TAB, 0, true));

		assertNull(focus.move(1));
		assertNull(focus.highlight());
	}

	@Test
	public void leftAndRightWrapAndSkipAGreyedStorageTab()
	{
		boolean[] withStorage = {true, true, true, true, true, true};
		boolean[] noStorage = {true, true, false, true, true, true};

		assertEquals(1, WindowFocus.nextTab(withStorage, 0, 1));
		assertEquals(5, WindowFocus.nextTab(withStorage, 0, -1));
		assertEquals(0, WindowFocus.nextTab(withStorage, 5, 1));
		assertEquals(2, WindowFocus.nextTab(withStorage, 1, 1));
		assertEquals(3, WindowFocus.nextTab(noStorage, 1, 1));
		assertEquals(1, WindowFocus.nextTab(noStorage, 3, -1));
	}

	@Test
	public void nextTabStaysPutWhenNoOtherTabCanBeChosen()
	{
		assertEquals(2, WindowFocus.nextTab(new boolean[] {false, false, true, false}, 2, 1));
		assertEquals(0, WindowFocus.nextTab(new boolean[] {true}, 0, -1));
	}

	@Test
	public void enterOnARowOpensItsMenuAndOnAnythingElseItPresses()
	{
		WindowFocus focus = page();
		assertEquals(WindowFocus.Activation.NONE, focus.activate());

		focus.move(1);
		assertEquals(WindowFocus.Activation.MENU, focus.activate());

		focus.move(-1);
		assertEquals(WindowFocus.Activation.PRESS, focus.activate());

		focus.highlightKey("tab:A");
		assertEquals(WindowFocus.Activation.PRESS, focus.activate());
	}

	@Test
	public void theMouseAndTheKeysShareOneHighlight()
	{
		WindowFocus focus = page();
		Hit second = focus.move(1);
		assertEquals("row:1", second.id());
		assertTrue(focus.keyboardLed());

		focus.hover(new Hit("row:2", Hit.Kind.ROW, new Rectangle(), 11, true, ""));
		assertEquals("row:2", focus.highlight());
		assertFalse(focus.keyboardLed());

		assertEquals("row:4", focus.move(1).id());
		assertTrue(focus.keyboardLed());
	}

	@Test
	public void aPointerMoveTakesOverEvenOverNothing()
	{
		WindowFocus focus = page();
		focus.move(1);

		assertEquals("row:1", focus.highlight());
		focus.hover(null);
		assertNull(focus.highlight());
		assertFalse(focus.keyboardLed());
	}

	@Test
	public void hoveringADisabledHitHighlightsNothing()
	{
		WindowFocus focus = page();
		focus.hover(hit("row:3", Hit.Kind.ROW, 12, false));

		assertNull(focus.highlight());
	}

	@Test
	public void losingFocusDropsAHighlightTheKeysPutThere()
	{
		WindowFocus focus = page();
		focus.focus(true);
		focus.move(1);
		focus.focus(false);

		assertFalse(focus.focused());
		assertNull(focus.highlight());
	}

	@Test
	public void losingFocusKeepsAHighlightThePointerPutThere()
	{
		WindowFocus focus = page();
		focus.focus(true);
		focus.hover(hit("row:2", Hit.Kind.ROW, 11, true));
		focus.focus(false);

		assertEquals("row:2", focus.highlight());
	}

	@Test
	public void theHighlightFollowsItsIdThroughANewLayoutAndGoesWhenTheHitIsGone()
	{
		WindowFocus focus = page();
		focus.move(1);
		focus.move(1);
		assertEquals("row:2", focus.highlight());

		focus.setHits(Arrays.asList(hit("row:2", Hit.Kind.ROW, 11, true), hit("btn", Hit.Kind.BUTTON, 100, true)));
		assertEquals("row:2", focus.highlight());
		assertEquals("row:2", focus.highlighted().id());

		focus.setHits(Arrays.asList(hit("btn", Hit.Kind.BUTTON, 100, true)));
		assertNull(focus.highlight());
		assertNull(focus.highlighted());
	}

	@Test
	public void backGoesMenuThenPageThenClosed()
	{
		assertEquals(WindowFocus.Step.MENU, WindowFocus.back(true, 1, true));
		assertEquals(WindowFocus.Step.PAGE, WindowFocus.back(false, 1, true));
		assertEquals(WindowFocus.Step.PAGE, WindowFocus.back(false, 2, true));
		assertEquals(WindowFocus.Step.CLOSE, WindowFocus.back(false, 0, true));
	}

	@Test
	public void backspaceStepsBackButNeverCloses()
	{
		assertEquals(WindowFocus.Step.MENU, WindowFocus.back(true, 0, false));
		assertEquals(WindowFocus.Step.PAGE, WindowFocus.back(false, 1, false));
		assertEquals(WindowFocus.Step.NONE, WindowFocus.back(false, 0, false));
	}
}
