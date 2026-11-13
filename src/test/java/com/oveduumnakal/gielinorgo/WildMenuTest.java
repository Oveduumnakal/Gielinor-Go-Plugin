/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks the entries a wild creature's body offers in the menu, and which one a left click does. */
public class WildMenuTest
{
	private static final JarType STANDARD = JarType.values()[0];

	private static final JarType SPECIALTY = JarType.values()[1];

	@Test
	public void aLeftClickBattlesByDefault()
	{
		assertFalse(WildMenu.LEFT_CLICK_CATCHES);
	}

	@Test
	public void aKnockedOutCreatureIsCaughtByALeftClick()
	{
		assertTrue(WildMenu.leftClickCatches(true, true));
		assertTrue(WildMenu.leftClickCatches(true, false));
		assertEquals(WildMenu.LEFT_CLICK_CATCHES, WildMenu.leftClickCatches(false, true));
	}

	@Test
	public void aLeftClickCatchesWheneverABattleWouldBeRefused()
	{
		assertTrue(WildMenu.leftClickCatches(false, false));
	}

	@Test
	public void whenLeftClickCatchesTheStandardJarIsOnTopAndBattleIsLast()
	{
		List<WildMenu.Item> items = WildMenu.topFirst(STANDARD, Collections.singletonList(SPECIALTY), true, true);

		assertEquals(3, items.size());
		assertTrue(items.get(0).isCatch());
		assertSame(STANDARD, items.get(0).jar());
		assertSame(SPECIALTY, items.get(1).jar());
		assertEquals(WildMenu.BATTLE, items.get(2).option());
		assertNull(items.get(2).jar());
	}

	@Test
	public void whenLeftClickBattlesBattleIsOnTop()
	{
		List<WildMenu.Item> items = WildMenu.topFirst(STANDARD, Collections.singletonList(SPECIALTY), true, false);

		assertEquals(WildMenu.BATTLE, items.get(0).option());
		assertSame(STANDARD, items.get(1).jar());
		assertSame(SPECIALTY, items.get(2).jar());
	}

	@Test
	public void withNoBattleAllowedOnlyCatchesRemain()
	{
		List<WildMenu.Item> items = WildMenu.topFirst(STANDARD, Collections.<JarType>emptyList(), false, true);

		assertEquals(1, items.size());
		assertTrue(items.get(0).isCatch());
	}

	@Test
	public void withoutABattleEntryCatchIsFirstAndThereIsNoBattle()
	{
		List<WildMenu.Item> items = WildMenu.topFirst(STANDARD, Collections.singletonList(SPECIALTY), false,
			WildMenu.leftClickCatches(false, false));

		assertEquals(2, items.size());
		assertTrue(items.get(0).isCatch());
		assertSame(STANDARD, items.get(0).jar());
		assertTrue(items.stream().noneMatch(item -> WildMenu.BATTLE.equals(item.option())));
	}

	@Test
	public void aPlayerWithNoJarsStillGetsACatchEntryToExplain()
	{
		List<WildMenu.Item> items = WildMenu.topFirst(null, Collections.<JarType>emptyList(), true, true);

		assertTrue(items.get(0).isCatch());
		assertNull(items.get(0).jar());
	}

	@Test
	public void onlyOurOwnEntriesAreRecognised()
	{
		assertTrue(WildMenu.isOurs("Catch"));
		assertTrue(WildMenu.isOurs("Battle"));
		assertFalse(WildMenu.isOurs("Attack"));
		assertFalse(WildMenu.isOurs(null));
		assertEquals(Arrays.asList("Catch", "Battle"), Arrays.asList(WildMenu.CATCH, WildMenu.BATTLE));
	}
}
