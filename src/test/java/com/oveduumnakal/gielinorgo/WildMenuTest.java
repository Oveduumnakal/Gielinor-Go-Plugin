/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
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

	@Test
	public void theSettingDecidesWhoGetsTheLeftClick()
	{
		assertTrue(WildMenu.leftClickAllowed(LeftClickMode.ALWAYS, false));
		assertTrue(WildMenu.leftClickAllowed(LeftClickMode.ALWAYS, true));
		assertTrue(WildMenu.leftClickAllowed(LeftClickMode.OUT_OF_COMBAT, false));
		assertFalse(WildMenu.leftClickAllowed(LeftClickMode.OUT_OF_COMBAT, true));
		assertFalse(WildMenu.leftClickAllowed(LeftClickMode.NEVER, false));
		assertFalse(WildMenu.leftClickAllowed(LeftClickMode.NEVER, true));
	}

	@Test
	public void targetWithoutARecordIsTheOldText()
	{
		assertEquals("<col=4aa3ff>Chicken</col> (Lv 3)", WildMenu.target("", "Chicken", 0x4AA3FF, 3, null));
		assertEquals("Gilded <col=4aa3ff>Chicken</col> (Lv 3)",
			WildMenu.target("Gilded ", "Chicken", 0x4AA3FF, 3, null));
	}

	@Test
	public void targetAddsTheRecordWords()
	{
		assertEquals("<col=4aa3ff>Chicken</col> (Lv 3 - New)",
			WildMenu.target("", "Chicken", 0x4AA3FF, 3, WildRecord.of(0, true)));
		assertEquals("<col=4aa3ff>Chicken</col> (Lv 3 - Seen)",
			WildMenu.target("", "Chicken", 0x4AA3FF, 3, WildRecord.of(0, false)));
		assertEquals("Gilded <col=4aa3ff>Chicken</col> (Lv 12 - Caught)",
			WildMenu.target("Gilded ", "Chicken", 0x4AA3FF, 12, WildRecord.of(4, false)));
	}

	@Test
	public void targetKeepsLeadingZeroInTheHexColour()
	{
		assertEquals("<col=00ff00>Imp</col> (Lv 7 - Seen)",
			WildMenu.target("", "Imp", 0x00FF00, 7, WildRecord.of(0, false)));
	}

	@Test
	public void targetIgnoresTheAlphaByteOfTheColour()
	{
		assertEquals("<col=4aa3ff>Imp</col> (Lv 7)", WildMenu.target("", "Imp", 0xFF4AA3FF, 7, null));
	}

	private static JarSuggest.JarChoice choice(JarType jar, int owned, double chance, boolean matching)
	{
		return new JarSuggest.JarChoice(jar, owned, chance, matching);
	}

	private static List<JarSuggest.JarChoice> jars(int count)
	{
		List<JarSuggest.JarChoice> list = new ArrayList<>();
		JarType[] tiers = {JarType.BRONZE, JarType.IRON, JarType.RUNE, JarType.DRAGON};
		for (int i = 0; i < count; i++)
			list.add(choice(tiers[i], 3, 0.5 + 0.1 * i, false));

		return list;
	}

	private static List<WildMenu.TopKind> kinds(List<WildMenu.Top> tops)
	{
		List<WildMenu.TopKind> list = new ArrayList<>();
		for (WildMenu.Top top : tops)
			list.add(top.kind());

		return list;
	}

	@Test
	public void theSubmenuIsOnByDefault()
	{
		assertTrue(WildMenu.SUBMENU);
	}

	@Test
	public void noJarsGivesTheExplainingCatchEntry()
	{
		List<WildMenu.Top> tops = WildMenu.layout(null, Collections.<JarSuggest.JarChoice>emptyList(), true, true,
			true);

		assertEquals(Arrays.asList(WildMenu.TopKind.CATCH_PLAIN, WildMenu.TopKind.BATTLE), kinds(tops));
		assertNull(tops.get(0).jar());
	}

	@Test
	public void oneJarGivesAPlainCatchWithThatJar()
	{
		List<WildMenu.Top> battleFirst = WildMenu.layout(JarType.BRONZE, jars(1), true, false, true);
		List<WildMenu.Top> catchFirst = WildMenu.layout(JarType.BRONZE, jars(1), true, true, true);

		assertEquals(Arrays.asList(WildMenu.TopKind.BATTLE, WildMenu.TopKind.CATCH_PLAIN), kinds(battleFirst));
		assertSame(JarType.BRONZE, battleFirst.get(1).jar());
		assertEquals(Arrays.asList(WildMenu.TopKind.CATCH_PLAIN, WildMenu.TopKind.BATTLE), kinds(catchFirst));
		assertSame(JarType.BRONZE, catchFirst.get(0).jar());
	}

	@Test
	public void twoJarsWithBattleFirstGiveBattleThenTheList()
	{
		List<WildMenu.Top> tops = WildMenu.layout(JarType.BRONZE, jars(2), true, false, true);

		assertEquals(Arrays.asList(WildMenu.TopKind.BATTLE, WildMenu.TopKind.CATCH_SUBMENU), kinds(tops));
	}

	@Test
	public void twoJarsWithCatchFirstGiveAPlainCatchTheListThenBattle()
	{
		List<WildMenu.Top> tops = WildMenu.layout(JarType.IRON, jars(3), true, true, true);

		assertEquals(Arrays.asList(WildMenu.TopKind.CATCH_PLAIN, WildMenu.TopKind.CATCH_SUBMENU,
			WildMenu.TopKind.BATTLE), kinds(tops));
		assertSame(JarType.IRON, tops.get(0).jar());
	}

	@Test
	public void withoutABattleThereIsNoBattleEntryWhateverTheClick()
	{
		for (boolean catches : new boolean[] {true, false})
		{
			List<WildMenu.Top> tops = WildMenu.layout(JarType.BRONZE, jars(3), false, catches, true);

			assertEquals(Arrays.asList(WildMenu.TopKind.CATCH_PLAIN, WildMenu.TopKind.CATCH_SUBMENU), kinds(tops));
		}
	}

	@Test
	public void theFirstEntryIsAlwaysWhatALeftClickDoes()
	{
		for (int count = 0; count <= 4; count++)
		{
			for (boolean withBattle : new boolean[] {true, false})
			{
				for (boolean catches : new boolean[] {true, false})
				{
					List<WildMenu.Top> tops = WildMenu.layout(count == 0 ? null : JarType.BRONZE, jars(count),
						withBattle, catches, true);
					boolean battleFirst = withBattle && !catches;

					assertEquals(battleFirst ? WildMenu.TopKind.BATTLE : WildMenu.TopKind.CATCH_PLAIN,
						tops.get(0).kind());
					if (!battleFirst)
						assertSame(count == 0 ? null : JarType.BRONZE, tops.get(0).jar());
				}
			}
		}
	}

	@Test
	public void theListIsSuggestedJarsThenALineThenTheRest()
	{
		List<JarSuggest.JarChoice> owned = Arrays.asList(
			choice(JarType.BRONZE, 5, 0.38, false), choice(JarType.IRON, 2, 0.56, false),
			choice(JarType.RUNE, 1, 0.75, false), choice(JarType.DRAGON, 3, 0.95, false),
			choice(JarType.SLASH, 4, 0.94, true));

		List<WildMenu.Row> rows = WildMenu.layout(JarType.BRONZE, owned, true, false, true).get(1).rows();

		assertEquals(6, rows.size());
		assertSame(JarType.DRAGON, rows.get(0).choice().jar());
		assertTrue(rows.get(0).suggested());
		assertSame(JarType.SLASH, rows.get(1).choice().jar());
		assertTrue(rows.get(1).suggested());
		assertEquals(WildMenu.RowKind.SEPARATOR, rows.get(2).kind());
		assertNull(rows.get(2).choice());
		assertSame(JarType.RUNE, rows.get(3).choice().jar());
		assertSame(JarType.IRON, rows.get(4).choice().jar());
		assertSame(JarType.BRONZE, rows.get(5).choice().jar());
		assertFalse(rows.get(3).suggested());
	}

	@Test
	public void noLineWhenEitherGroupIsEmpty()
	{
		List<JarSuggest.JarChoice> close = Arrays.asList(
			choice(JarType.BRONZE, 1, 0.95, false), choice(JarType.IRON, 1, 0.95, false));
		List<WildMenu.Row> all = WildMenu.layout(JarType.BRONZE, close, true, false, true).get(1).rows();

		assertEquals(2, all.size());
		assertTrue(all.stream().allMatch(row -> row.kind() == WildMenu.RowKind.JAR && row.suggested()));
	}

	@Test
	public void withoutTheSubmenuItIsTheFlatMenu()
	{
		List<JarSuggest.JarChoice> owned = Arrays.asList(
			choice(JarType.BRONZE, 5, 0.9, false), choice(JarType.RUNE, 2, 0.9, false),
			choice(JarType.CAVE, 1, 0.9, true), choice(JarType.SLASH, 1, 0.9, true),
			choice(JarType.STAB, 1, 0.9, false));
		List<JarType> specialty = Arrays.asList(JarType.CAVE, JarType.SLASH);

		for (boolean withBattle : new boolean[] {true, false})
		{
			for (boolean catches : new boolean[] {true, false})
			{
				List<WildMenu.Item> flat = WildMenu.topFirst(JarType.BRONZE, specialty, withBattle, catches);
				List<WildMenu.Top> tops = WildMenu.layout(JarType.BRONZE, owned, withBattle, catches, false);

				assertEquals(flat.size(), tops.size());
				for (int i = 0; i < flat.size(); i++)
				{
					assertEquals(flat.get(i).isCatch() ? WildMenu.TopKind.CATCH_PLAIN : WildMenu.TopKind.BATTLE,
						tops.get(i).kind());
					assertSame(flat.get(i).jar(), tops.get(i).jar());
				}
			}
		}
	}

	@Test
	public void theFlatMenuSkipsTheStandardJarWhenItAlsoMatches()
	{
		List<JarSuggest.JarChoice> owned = Arrays.asList(
			choice(JarType.CAVE, 1, 0.9, true), choice(JarType.SLASH, 1, 0.9, true));

		List<WildMenu.Top> tops = WildMenu.layout(JarType.CAVE, owned, false, true, false);

		assertEquals(2, tops.size());
		assertSame(JarType.CAVE, tops.get(0).jar());
		assertSame(JarType.SLASH, tops.get(1).jar());
	}

	@Test
	public void jarWordsAreExact()
	{
		assertEquals("Dragon jar", WildMenu.jarOption(choice(JarType.DRAGON, 3, 0.92, false)));
		assertEquals("92% (x3)", WildMenu.jarTarget(choice(JarType.DRAGON, 3, 0.92, false)));
		assertEquals("95% (x12)", WildMenu.jarTarget(choice(JarType.BRONZE, 12, 0.95, false)));
		assertEquals("Slash jar", WildMenu.jarOption(choice(JarType.SLASH, 1, 0.5, true)));
	}

	@Test
	public void theSeparatorIsEightDashes()
	{
		assertEquals("--------", WildMenu.SEPARATOR);
	}

	@Test
	public void theHeadingHasNoColourTags()
	{
		assertEquals("Chicken (Lv 3)", WildMenu.title("", "Chicken", 3, null));
		assertEquals("Gilded Chicken (Lv 12 - Caught)",
			WildMenu.title("Gilded ", "Chicken", 12, WildRecord.of(4, false)));
	}
}
