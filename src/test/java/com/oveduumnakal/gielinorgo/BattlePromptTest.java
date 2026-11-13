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

/** Checks the words of the on-screen prompt and of the hotkey hints. */
public class BattlePromptTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private static BattleView view(boolean forced)
	{
		Combatant mine = new Combatant(GOBLIN, "Bob", 20, 0.6);
		Combatant foe = new Combatant(GOBLIN, "Imp", 8, 1.0);
		return new BattleView(mine, foe, forced, true, JarType.BRONZE, 3, 7, 0.4, Collections.emptyList());
	}

	@Test
	public void theTitleHasTheTimeLeftAndBothCreaturesFollow()
	{
		List<String> lines = BattlePrompt.lines(view(false), "");

		assertEquals(Arrays.asList("Your turn · 7s", "Bob Lv 20 60%", "Imp Lv 8 100%"), lines);
	}

	@Test
	public void aForcedSwitchAsksForTheNextCreature()
	{
		assertEquals("Pick your next creature · 7s", BattlePrompt.lines(view(true), "").get(0));
	}

	@Test
	public void theHotkeysAreAddedWhenSet()
	{
		List<String> lines = BattlePrompt.lines(view(false), "Attack F1 · Run F4");

		assertEquals(4, lines.size());
		assertEquals("Attack F1 · Run F4", lines.get(3));
	}

	@Test
	public void onlyTheKeysThatAreSetAreListedInOrder()
	{
		assertEquals("Attack F1 · Switch F2 · Catch F3 · Run F4",
			BattlePrompt.hotkeys("F1", "F2", "F3", "F4", true, false));
		assertEquals("Switch F2 · Run F4", BattlePrompt.hotkeys(null, "F2", null, "F4", true, false));
		assertEquals("", BattlePrompt.hotkeys(null, null, null, null, true, false));
	}

	@Test
	public void theCatchKeyIsLeftOutWhenNoJarCanBeThrown()
	{
		assertEquals("Attack F1 · Run F4", BattlePrompt.hotkeys("F1", null, "F3", "F4", false, false));
	}

	@Test
	public void afterAFaintOnlyTheSwitchKeyCounts()
	{
		assertEquals("Switch F2", BattlePrompt.hotkeys("F1", "F2", "F3", "F4", true, true));
	}
}
