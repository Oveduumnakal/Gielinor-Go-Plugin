/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Rectangle;
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
 * Checks what a creature's menu offers and when each item is dim: Summary first, Make lead dim for the lead and during
 * an Elite run, Move to storage dim unless the team may change here, Use item by the item rules, and Revive only for
 * a fainted creature.
 */
public class ContextMenuRulesTest
{
	private final List<String> calls = new ArrayList<>();
	private final ContextMenuRules.Commands commands = new ContextMenuRules.Commands()
	{
		@Override
		public void summary()
		{
			calls.add("summary");
		}

		@Override
		public void makeLead()
		{
			calls.add("makeLead");
		}

		@Override
		public void moveToStorage()
		{
			calls.add("moveToStorage");
		}

		@Override
		public void rename()
		{
			calls.add("rename");
		}

		@Override
		public void moves()
		{
			calls.add("moves");
		}

		@Override
		public void useItem()
		{
			calls.add("useItem");
		}

		@Override
		public void revive()
		{
			calls.add("revive");
		}
	};

	private static Map<ItemType, Integer> items(ItemType type, int count)
	{
		Map<ItemType, Integer> owned = new EnumMap<>(ItemType.class);
		owned.put(type, count);
		return owned;
	}

	private static ContextMenuRules.Facts facts(int index, double hp, boolean elite, boolean storage,
		Map<ItemType, Integer> items)
	{
		return new ContextMenuRules.Facts(RowModel.of(PartyRig.creature("Goblin", 9, hp, null), index), elite,
			storage, items);
	}

	private static List<String> labels(List<ContextMenu.Item> menu)
	{
		List<String> labels = new ArrayList<>();
		for (ContextMenu.Item item : menu)
			labels.add(item.label());

		return labels;
	}

	private static ContextMenu.Item item(List<ContextMenu.Item> menu, String label)
	{
		for (ContextMenu.Item item : menu)
		{
			if (item.label().equals(label))
				return item;
		}

		throw new AssertionError("no item " + label + " in " + labels(menu));
	}

	@Test
	public void theMenuHasSummaryFirstAndTheItemsInTheAgreedOrder()
	{
		List<ContextMenu.Item> menu = ContextMenuRules.party(facts(2, 1.0, false, true, null), commands);

		assertEquals(Arrays.asList("Summary", "Make lead", "Move to storage", "Rename", "Moves", "Use item"),
			labels(menu));
		assertTrue(menu.get(0).enabled());
	}

	@Test
	public void theSummaryPageMenuIsTheSameWithoutSummary()
	{
		List<ContextMenu.Item> menu = ContextMenuRules.summary(facts(2, 1.0, false, true, null), commands);

		assertEquals(Arrays.asList("Make lead", "Move to storage", "Rename", "Moves", "Use item"), labels(menu));
	}

	@Test
	public void makeLeadIsDimForTheLeadWithTheReason()
	{
		ContextMenu.Item lead = item(ContextMenuRules.party(facts(0, 1.0, false, true, null), commands), "Make lead");

		assertFalse(lead.enabled());
		assertEquals(ContextMenuRules.LEAD_REASON, lead.reason());
		assertTrue(item(ContextMenuRules.party(facts(1, 1.0, false, true, null), commands), "Make lead").enabled());
	}

	@Test
	public void makeLeadAndMoveToStorageAreDimDuringAnEliteRunWithTheReason()
	{
		List<ContextMenu.Item> menu = ContextMenuRules.party(facts(3, 1.0, true, true, null), commands);

		assertFalse(item(menu, "Make lead").enabled());
		assertEquals(ContextMenuRules.ELITE_REASON, item(menu, "Make lead").reason());
		assertFalse(item(menu, "Move to storage").enabled());
		assertEquals(ContextMenuRules.ELITE_REASON, item(menu, "Move to storage").reason());
		assertTrue(item(menu, "Rename").enabled());
	}

	@Test
	public void moveToStorageIsDimUnlessTheTeamMayBeChangedHere()
	{
		ContextMenu.Item away = item(ContextMenuRules.party(facts(3, 1.0, false, false, null), commands),
			"Move to storage");

		assertFalse(away.enabled());
		assertEquals(ContextMenuRules.SWAP_REASON, away.reason());
		assertTrue(item(ContextMenuRules.party(facts(3, 1.0, false, true, null), commands), "Move to storage")
			.enabled());
	}

	@Test
	public void useItemFollowsTheItemRulesAndNeedsAnOwnedItemThatHelps()
	{
		ContextMenu.Item nothing = item(ContextMenuRules.party(facts(3, 0.5, false, true, null), commands),
			"Use item");
		ContextMenu.Item healthy = item(ContextMenuRules.party(facts(3, 1.0, false, true,
			items(ItemType.SMALL_SALVE, 2)), commands), "Use item");
		ContextMenu.Item hurt = item(ContextMenuRules.party(facts(3, 0.5, false, true,
			items(ItemType.SMALL_SALVE, 2)), commands), "Use item");

		assertFalse(nothing.enabled());
		assertEquals(ContextMenuRules.NO_ITEM_REASON, nothing.reason());
		assertFalse(healthy.enabled());
		assertTrue(hurt.enabled());
	}

	@Test
	public void theUseItemMenuListsTheHelpfulItemsWithTheirCountsAndUsesTheChosenOne()
	{
		Map<ItemType, Integer> owned = items(ItemType.SMALL_SALVE, 2);
		owned.put(ItemType.LARGE_SALVE, 1);
		List<ItemType> used = new ArrayList<>();
		List<ContextMenu.Item> menu = ContextMenuRules.useItems(facts(1, 0.4, false, true, owned), used::add);

		assertEquals(Arrays.asList("Small salve  x2", "Large salve  x1"), labels(menu));
		assertEquals(Arrays.asList(ItemType.SMALL_SALVE, ItemType.LARGE_SALVE),
			ContextMenuRules.usable(facts(1, 0.4, false, true, owned)));
	}

	@Test
	public void reviveIsOnlyOfferedForAFaintedCreatureAndIsFreeButNeedsAPlaceAndNoEliteRun()
	{
		ContextMenuRules.Facts well = facts(1, 0.8, false, true, items(ItemType.REVIVE, 1));
		RowModel down = RowModel.of(PartyRig.creature("Goblin", 9, 0.8, null).withFainted(true), 1);

		assertFalse(labels(ContextMenuRules.party(well, commands)).contains("Revive"));
		List<ContextMenu.Item> ready = ContextMenuRules.party(new ContextMenuRules.Facts(down, false, true,
			Collections.emptyMap()), commands);
		assertTrue("no draught is needed", item(ready, "Revive").enabled());
		List<ContextMenu.Item> away = ContextMenuRules.party(new ContextMenuRules.Facts(down, false, false,
			items(ItemType.REVIVE, 1)), commands);
		assertFalse(item(away, "Revive").enabled());
		assertEquals(ContextMenuRules.SWAP_REASON, item(away, "Revive").reason());
		List<ContextMenu.Item> running = ContextMenuRules.party(new ContextMenuRules.Facts(down, true, true,
			Collections.emptyMap()), commands);
		assertFalse(item(running, "Revive").enabled());
		assertEquals(ContextMenuRules.ELITE_REASON, item(running, "Revive").reason());
	}

	@Test
	public void aFaintedCreatureCanUseAReviveDraughtAnywhere()
	{
		RowModel down = RowModel.of(PartyRig.creature("Goblin", 9, 0.0, null).withFainted(true), 1);
		ContextMenuRules.Facts away = new ContextMenuRules.Facts(down, true, false, items(ItemType.REVIVE, 1));

		assertEquals(Collections.singletonList(ItemType.REVIVE), ContextMenuRules.usable(away));
		assertTrue(item(ContextMenuRules.party(away, commands), "Use item").enabled());
	}

	@Test
	public void aFaintedCreatureHasNoHealingItemsToUse()
	{
		RowModel down = RowModel.of(PartyRig.creature("Goblin", 9, 0.2, null).withFainted(true), 1);
		ContextMenuRules.Facts facts = new ContextMenuRules.Facts(down, false, true, items(ItemType.SMALL_SALVE, 3));

		assertTrue(ContextMenuRules.usable(facts).isEmpty());
	}

	@Test
	public void everyEnabledItemRunsItsOwnCommandAndADimOneRunsNothing()
	{
		List<ContextMenu.Item> menu = ContextMenuRules.party(facts(2, 0.5, false, true,
			items(ItemType.SMALL_SALVE, 1)), commands);
		WindowStyle style = WidgetKit.style(1.0);
		FontMetrics fm = WidgetKit.metrics(style, WindowFonts.Face.REGULAR);
		for (int i = 0; i < menu.size(); i++)
		{
			if (!menu.get(i).enabled())
				continue;

			ContextMenu context = new ContextMenu();
			context.open(menu, 10, 10, new Rectangle(0, 0, 800, 600), style, fm, true, 0L);
			for (int step = 0; step < menu.size() && context.highlight() != i; step++)
				context.move(1);

			assertEquals(menu.get(i).label(), i, context.highlight());
			assertTrue(context.activate());
		}

		assertEquals(Arrays.asList("summary", "makeLead", "moveToStorage", "rename", "moves", "useItem"), calls);
	}

	private final ContextMenuRules.StoredCommands stored = new ContextMenuRules.StoredCommands()
	{
		@Override
		public void summary()
		{
			calls.add("s-summary");
		}

		@Override
		public void moveToTeam()
		{
			calls.add("s-moveToTeam");
		}

		@Override
		public void revive()
		{
			calls.add("s-revive");
		}

		@Override
		public void rename()
		{
			calls.add("s-rename");
		}

		@Override
		public void moves()
		{
			calls.add("s-moves");
		}

		@Override
		public void useItem()
		{
			calls.add("s-useItem");
		}

		@Override
		public void release()
		{
			calls.add("s-release");
		}
	};

	private static ContextMenuRules.Facts storedFacts(double hp, int teamSize, Map<ItemType, Integer> items)
	{
		return new ContextMenuRules.Facts(RowModel.of(PartyRig.creature("Goblin", 9, hp, null), -1), false, true,
			items, teamSize);
	}

	@Test
	public void aStoredCreatureMenuIsSummaryMoveToTeamRenameMovesUseItemRelease()
	{
		List<ContextMenu.Item> menu = ContextMenuRules.stored(storedFacts(1.0, 2, null), stored);

		assertEquals(Arrays.asList("Summary", "Move to team", "Rename...", "Moves", "Use item", "Release..."),
			labels(menu));
		assertTrue(menu.get(0).enabled());
	}

	@Test
	public void theStoredSummaryPageMenuHasNoSummary()
	{
		List<ContextMenu.Item> menu = ContextMenuRules.storedSummary(storedFacts(1.0, 2, null), stored);

		assertEquals(Arrays.asList("Move to team", "Rename...", "Moves", "Use item", "Release..."), labels(menu));
	}

	@Test
	public void moveToTeamIsDimWithTeamIsFullAtSixAndOnlyThen()
	{
		ContextMenu.Item full = item(ContextMenuRules.stored(storedFacts(1.0, 6, null), stored), "Move to team");
		ContextMenu.Item room = item(ContextMenuRules.stored(storedFacts(1.0, 5, null), stored), "Move to team");

		assertFalse(full.enabled());
		assertEquals(ContextMenuRules.TEAM_FULL_REASON, full.reason());
		assertEquals("Team is full", ContextMenuRules.TEAM_FULL_REASON);
		assertTrue(room.enabled());
	}

	@Test
	public void aStoredUseItemIsDimWithNothingToUseAndLiveWithAHelpfulItem()
	{
		ContextMenu.Item none = item(ContextMenuRules.stored(storedFacts(0.5, 2, null), stored), "Use item");
		ContextMenu.Item some = item(ContextMenuRules.stored(storedFacts(0.5, 2, items(ItemType.SMALL_SALVE, 1)),
			stored), "Use item");

		assertFalse(none.enabled());
		assertEquals(ContextMenuRules.NO_ITEM_REASON, none.reason());
		assertTrue(some.enabled());
	}

	@Test
	public void everyStoredItemRunsItsOwnCommand()
	{
		List<ContextMenu.Item> menu = ContextMenuRules.stored(storedFacts(0.5, 2, items(ItemType.SMALL_SALVE, 1)),
			stored);
		WindowStyle style = WidgetKit.style(1.0);
		FontMetrics fm = WidgetKit.metrics(style, WindowFonts.Face.REGULAR);
		for (int i = 0; i < menu.size(); i++)
		{
			ContextMenu context = new ContextMenu();
			context.open(menu, 10, 10, new Rectangle(0, 0, 800, 600), style, fm, true, 0L);
			for (int step = 0; step < menu.size() && context.highlight() != i; step++)
				context.move(1);

			assertTrue(menu.get(i).label(), context.activate());
		}

		assertEquals(Arrays.asList("s-summary", "s-moveToTeam", "s-rename", "s-moves", "s-useItem", "s-release"),
			calls);
	}
}
