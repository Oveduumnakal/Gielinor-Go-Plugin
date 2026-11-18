/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * What a creature's menu offers, and when each item is dim: one rule set for the menu a click or Enter opens on a Party
 * row and for the Summary page's actions. The items come in a fixed order with Summary first, a dim item says why it
 * is dim beside it, and an item the creature cannot use at all is not offered. Make lead is dim for the lead and during
 * an Elite run; Move to storage is dim during an Elite run and unless {@link TeamSwapRule} allows it here; Use
 * item is dim when no owned item would help; Revive only exists for a fainted creature, is free, and is dim during an
 * Elite run and unless {@link TeamSwapRule} allows it here, while the Revive draught is offered by Use item
 * anywhere. Pure rules over plain values, tested without a window. Holds no RuneLite types.
 */
final class ContextMenuRules
{
	/** The label of the first item, which opens the Summary page. */
	static final String SUMMARY = "Summary";

	/** The label of the item that makes a creature the lead. */
	static final String MAKE_LEAD = "Make lead";

	/** The label of the item that moves a creature to storage. */
	static final String STORAGE = "Move to storage";

	/** The label of the item that opens the Rename box. */
	static final String RENAME = "Rename";

	/** The label of the item that opens the moves of a creature. */
	static final String MOVES = "Moves";

	/** The label of the item that offers the healing items. */
	static final String USE_ITEM = "Use item";

	/** The label of the item that brings a fainted creature back for free. */
	static final String REVIVE = "Revive";

	/** Why Make lead and Move to storage are dim during an Elite run. */
	static final String ELITE_REASON = "Finish the Elite run";

	/** Why Move to storage is dim away from a city or a bank. */
	static final String SWAP_REASON = "Needs a city or bank";

	/** Why Make lead is dim for the lead. */
	static final String LEAD_REASON = "Already the lead";

	/** Why Use item is dim when nothing would help. */
	static final String NO_ITEM_REASON = "Nothing to use";

	/** Why Move to team is dim when the team has no room. */
	static final String TEAM_FULL_REASON = "Team is full";

	/** What the rules look at. Immutable. */
	static final class Facts
	{
		private final RowModel row;
		private final boolean eliteBusy;
		private final boolean storageEnabled;
		private final Map<ItemType, Integer> items;
		private final int teamSize;

		/**
		 * Gathers the facts.
		 *
		 * @param row the creature's row
		 * @param eliteBusy whether an Elite run is open
		 * @param storageEnabled whether the team may be changed here, which is {@link TeamSwapRule}
		 * @param items how many of each healing item the player owns
		 */
		Facts(RowModel row, boolean eliteBusy, boolean storageEnabled, Map<ItemType, Integer> items)
		{
			this(row, eliteBusy, storageEnabled, items, 0);
		}

		/**
		 * Gathers the facts for a stored creature, which also needs to know whether the team has room.
		 *
		 * @param row the creature's row
		 * @param eliteBusy whether an Elite run is open
		 * @param storageEnabled whether the team may be changed here, which is {@link TeamSwapRule}
		 * @param items how many of each healing item the player owns
		 * @param teamSize how many creatures are on the team now
		 */
		Facts(RowModel row, boolean eliteBusy, boolean storageEnabled, Map<ItemType, Integer> items, int teamSize)
		{
			this.teamSize = teamSize;
			this.row = row;
			this.eliteBusy = eliteBusy;
			this.storageEnabled = storageEnabled;
			this.items = items;
		}

		/** @return the creature's row */
		RowModel row()
		{
			return row;
		}
	}

	/** What the items do; the window runs each on the game thread. */
	interface Commands
	{
		/** Opens the Summary page of the creature. */
		void summary();

		/** Makes the creature the lead. */
		void makeLead();

		/** Moves the creature to storage. */
		void moveToStorage();

		/** Opens the Rename box. */
		void rename();

		/** Opens the creature's moves. */
		void moves();

		/** Offers the healing items the creature can use. */
		void useItem();

		/** Brings the fainted creature back for free; the game checks the place. */
		void revive();
	}

	/** What the items of a stored creature's menu do; the window runs each on the game thread. */
	interface StoredCommands
	{
		/** Opens the Summary page of the creature. */
		void summary();

		/** Moves the creature to the team. */
		void moveToTeam();

		/** Revives the fainted creature: the game decides whether it may happen here. */
		void revive();

		/** Opens the Rename box. */
		void rename();

		/** Opens the creature's moves. */
		void moves();

		/** Offers the healing items the creature can use. */
		void useItem();

		/** Starts a release, which asks first. */
		void release();
	}

	private ContextMenuRules()
	{
	}

	/**
	 * Builds the menu of a Party row.
	 *
	 * @param facts what the rules look at
	 * @param commands what the items do
	 * @return the items, Summary first
	 */
	static List<ContextMenu.Item> party(Facts facts, Commands commands)
	{
		List<ContextMenu.Item> list = new ArrayList<>();
		list.add(ContextMenu.Item.of(SUMMARY, commands::summary));
		list.addAll(actions(facts, commands));
		return list;
	}

	/**
	 * Builds the menu of the Summary page: the same items without Summary, since the page is already open.
	 *
	 * @param facts what the rules look at
	 * @param commands what the items do
	 * @return the items
	 */
	static List<ContextMenu.Item> summary(Facts facts, Commands commands)
	{
		return actions(facts, commands);
	}

	/**
	 * Builds the menu of a stored creature: Summary first, then Move to team, Rename, Moves, Use item and Release.
	 *
	 * @param facts what the rules look at, with the team size
	 * @param commands what the items do
	 * @return the items, Summary first
	 */
	static List<ContextMenu.Item> stored(Facts facts, StoredCommands commands)
	{
		List<ContextMenu.Item> list = new ArrayList<>();
		list.add(ContextMenu.Item.of(SUMMARY, commands::summary));
		list.addAll(storedActions(facts, commands));
		return list;
	}

	/**
	 * Builds the menu of a stored creature's Summary page: the same items without Summary, since the page is open.
	 *
	 * @param facts what the rules look at, with the team size
	 * @param commands what the items do
	 * @return the items
	 */
	static List<ContextMenu.Item> storedSummary(Facts facts, StoredCommands commands)
	{
		return storedActions(facts, commands);
	}

	private static List<ContextMenu.Item> storedActions(Facts facts, StoredCommands commands)
	{
		List<ContextMenu.Item> list = new ArrayList<>();
		if (facts.teamSize >= SaveData.TEAM_SIZE)
			list.add(ContextMenu.Item.disabled(StoragePageFlow.MOVE_TO_TEAM, TEAM_FULL_REASON));
		else
			list.add(ContextMenu.Item.of(StoragePageFlow.MOVE_TO_TEAM, commands::moveToTeam));

		if (facts.row.state() == RowModel.State.FAINTED)
			list.add(ContextMenu.Item.of(REVIVE, commands::revive));

		list.add(ContextMenu.Item.of(StoragePageFlow.RENAME, commands::rename));
		list.add(ContextMenu.Item.of(MOVES, commands::moves));
		list.add(usableItems(facts) ? ContextMenu.Item.of(StoragePageFlow.USE_ITEM, commands::useItem)
			: ContextMenu.Item.disabled(StoragePageFlow.USE_ITEM, NO_ITEM_REASON));
		list.add(ContextMenu.Item.of(StoragePageFlow.RELEASE, commands::release));
		return list;
	}

	private static boolean usableItems(Facts facts)
	{
		return !usable(facts).isEmpty();
	}

	private static List<ContextMenu.Item> actions(Facts facts, Commands commands)
	{
		List<ContextMenu.Item> list = new ArrayList<>();
		list.add(makeLead(facts, commands));
		list.add(moveToStorage(facts, commands));
		list.add(ContextMenu.Item.of(RENAME, commands::rename));
		list.add(ContextMenu.Item.of(MOVES, commands::moves));
		list.add(useItem(facts, commands));
		if (facts.row.state() == RowModel.State.FAINTED)
			list.add(revive(facts, commands));

		return list;
	}

	private static ContextMenu.Item makeLead(Facts facts, Commands commands)
	{
		if (facts.eliteBusy)
			return ContextMenu.Item.disabled(MAKE_LEAD, ELITE_REASON);

		if (facts.row.lead())
			return ContextMenu.Item.disabled(MAKE_LEAD, LEAD_REASON);

		return ContextMenu.Item.of(MAKE_LEAD, commands::makeLead);
	}

	private static ContextMenu.Item moveToStorage(Facts facts, Commands commands)
	{
		if (facts.eliteBusy)
			return ContextMenu.Item.disabled(STORAGE, ELITE_REASON);

		if (!facts.storageEnabled)
			return ContextMenu.Item.disabled(STORAGE, SWAP_REASON);

		return ContextMenu.Item.of(STORAGE, commands::moveToStorage);
	}

	private static ContextMenu.Item useItem(Facts facts, Commands commands)
	{
		boolean any = !usable(facts).isEmpty();
		return any ? ContextMenu.Item.of(USE_ITEM, commands::useItem)
			: ContextMenu.Item.disabled(USE_ITEM, NO_ITEM_REASON);
	}

	private static ContextMenu.Item revive(Facts facts, Commands commands)
	{
		if (facts.eliteBusy)
			return ContextMenu.Item.disabled(REVIVE, ELITE_REASON);

		if (!facts.storageEnabled)
			return ContextMenu.Item.disabled(REVIVE, SWAP_REASON);

		return ContextMenu.Item.of(REVIVE, commands::revive);
	}

	/**
	 * Lists the items a creature can use outside battle.
	 *
	 * @param facts what the rules look at
	 * @return the items, in the order of {@link ItemType#values()}; a fainted creature can only use a Revive draught
	 */
	static List<ItemType> usable(Facts facts)
	{
		boolean fainted = facts.row.state() == RowModel.State.FAINTED;
		return ItemMenu.usable(facts.items, facts.row.view().hpFraction(), fainted);
	}

	/**
	 * Builds the second menu, the one Use item opens: one item per helpful healing item with its count.
	 *
	 * @param facts what the rules look at
	 * @param use called with the item the player chooses
	 * @return the items
	 */
	static List<ContextMenu.Item> useItems(Facts facts, Consumer<ItemType> use)
	{
		List<ContextMenu.Item> list = new ArrayList<>();
		for (ItemType type : usable(facts))
			list.add(ContextMenu.Item.of(type.label() + "  x" + facts.items.get(type), () -> use.accept(type)));

		return list;
	}
}
