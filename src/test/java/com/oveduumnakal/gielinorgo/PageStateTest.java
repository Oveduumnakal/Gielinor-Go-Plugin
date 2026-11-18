/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks which page of the Party tab is shown and which creature the Summary is on. */
public class PageStateTest
{
	private final PageState state = new PageState();

	@Test
	public void theTabStartsOnTheListAtDepthZero()
	{
		assertEquals(PageState.Page.PARTY, state.page());
		assertEquals(0, state.depth());
		assertEquals(-1, state.cameFrom());
	}

	@Test
	public void openingASummaryGoesOneDeepAndRemembersTheRowItCameFrom()
	{
		assertTrue(state.open(2, 4, 500L));

		assertEquals(PageState.Page.SUMMARY, state.page());
		assertEquals(1, state.depth());
		assertEquals(2, state.index());
		assertEquals(2, state.cameFrom());
		assertEquals(500L, state.openedMs());
	}

	@Test
	public void aCreatureThatIsNotThereCannotBeOpened()
	{
		assertFalse(state.open(4, 4, 0L));
		assertFalse(state.open(-1, 4, 0L));
		assertFalse(state.open(0, 0, 0L));
		assertEquals(PageState.Page.PARTY, state.page());
	}

	@Test
	public void backGoesToTheListAndDoesNothingThere()
	{
		state.open(1, 3, 0L);
		state.back();
		state.back();

		assertEquals(PageState.Page.PARTY, state.page());
		assertEquals(0, state.depth());
		assertEquals(-1, state.cameFrom());
	}

	@Test
	public void upAndDownStepThroughTheTeamAndWrapAtTheEnds()
	{
		state.open(0, 3, 0L);

		assertEquals(2, state.step(-1, 3, 10L));
		assertEquals(0, state.step(1, 3, 20L));
		assertEquals(1, state.step(1, 3, 30L));
		assertEquals(30L, state.openedMs());
		assertEquals(-1, state.cameFrom());
	}

	@Test
	public void steppingDoesNothingOnTheListOrWithAnEmptyTeam()
	{
		assertEquals(0, state.step(1, 3, 0L));
		assertEquals(PageState.Page.PARTY, state.page());

		state.open(1, 3, 0L);
		assertEquals(1, state.step(1, 0, 0L));
	}

	@Test
	public void aShrinkingTeamKeepsTheSummaryOnARealCreatureOrGoesBack()
	{
		state.open(3, 4, 0L);

		state.clamp(3);
		assertEquals(2, state.index());
		assertEquals(PageState.Page.SUMMARY, state.page());

		state.clamp(0);
		assertEquals(PageState.Page.PARTY, state.page());
	}
}
