/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The two menus of the Storage page, Sort and Filter, as lists of menu items. Each item says what it does and its own
 * state, and its action hands the edited {@link StorageQuery} to a callback. Pure: no window, no clock. Holds no
 * RuneLite types.
 */
final class StorageMenus
{
	/** The text of the dim Clear filters item when no filter is set. */
	static final String NO_FILTERS = "No filters";

	/** The Clear filters item. */
	static final String CLEAR_FILTERS = "Clear filters";

	private static final int[] OVER = {10, 20, 30, 40, 50, 60, 70, 80, 90};
	private static final int[] UNDER = {20, 30, 40, 50, 60, 70, 80, 90, 99};

	private StorageMenus()
	{
	}

	/**
	 * @param sort a sort
	 * @param query the query now
	 * @return the item's words: the sort and the direction a press gives it
	 */
	static String sortLabel(StorageQuery.Sort sort, StorageQuery query)
	{
		boolean descending = query.sort() == sort ? query.descending() : sort.defaultDescending();
		String words = sort.directionWords(descending);
		return words.isEmpty() ? sort.label() : sort.label() + " (" + words + ")";
	}

	/**
	 * Builds the Sort menu: one item per sort. Picking the sort that is on flips it; another starts at its default.
	 *
	 * @param query the query now
	 * @param apply called with the new query
	 * @return the items
	 */
	static List<ContextMenu.Item> sort(StorageQuery query, Consumer<StorageQuery> apply)
	{
		List<ContextMenu.Item> items = new ArrayList<>();
		for (StorageQuery.Sort sort : StorageQuery.Sort.values())
			items.add(ContextMenu.Item.of(sortLabel(sort, query), () -> apply.accept(query.withSort(sort))));

		return items;
	}

	/**
	 * Builds the Filter menu: submenus for level, style, rarity, rating and gender, the two yes/no filters, and Clear
	 * filters.
	 *
	 * @param query the query now
	 * @param apply called with the new query
	 * @param submenu opens a submenu's items
	 * @return the items
	 */
	static List<ContextMenu.Item> filter(StorageQuery query, Consumer<StorageQuery> apply,
		Consumer<List<ContextMenu.Item>> submenu)
	{
		List<ContextMenu.Item> items = new ArrayList<>();
		items.add(parent("Level over...", levels(query, apply, StorageQuery.Facet.LEVEL_OVER, OVER, "Over "),
			submenu));
		items.add(parent("Level under...", levels(query, apply, StorageQuery.Facet.LEVEL_UNDER, UNDER, "Under "),
			submenu));
		List<ContextMenu.Item> styles = new ArrayList<>();
		for (CombatType type : CombatType.values())
			styles.add(leaf(query, apply, StorageQuery.Facet.STYLE, type.ordinal(), type.label()));

		items.add(parent("Style...", styles, submenu));
		List<ContextMenu.Item> rarities = new ArrayList<>();
		for (Rarity rarity : Rarity.values())
			rarities.add(leaf(query, apply, StorageQuery.Facet.RARITY, rarity.ordinal(), rarity.label()));

		items.add(parent("Rarity...", rarities, submenu));
		List<ContextMenu.Item> ratings = new ArrayList<>();
		for (StatRolls.Rating rating : StatRolls.Rating.values())
			ratings.add(leaf(query, apply, StorageQuery.Facet.RATING, rating.ordinal(), rating.label()));

		items.add(parent("Rating...", ratings, submenu));
		List<ContextMenu.Item> genders = new ArrayList<>();
		for (Gender gender : Gender.values())
		{
			genders.add(leaf(query, apply, StorageQuery.Facet.GENDER, gender.ordinal(),
				StorageQuery.valueName(StorageQuery.Facet.GENDER, gender.ordinal())));
		}

		items.add(parent("Gender...", genders, submenu));
		items.add(leaf(query, apply, StorageQuery.Facet.GILDED, 0, "Gilded"));
		items.add(leaf(query, apply, StorageQuery.Facet.NICKNAMED, 0, "Nicknamed"));
		if (query.active())
			items.add(ContextMenu.Item.of(CLEAR_FILTERS, () -> apply.accept(query.cleared())));
		else
			items.add(ContextMenu.Item.disabled(CLEAR_FILTERS, NO_FILTERS));

		return items;
	}

	private static List<ContextMenu.Item> levels(StorageQuery query, Consumer<StorageQuery> apply,
		StorageQuery.Facet facet, int[] values, String prefix)
	{
		List<ContextMenu.Item> items = new ArrayList<>();
		for (int value : values)
			items.add(leaf(query, apply, facet, value, prefix + value));

		return items;
	}

	private static ContextMenu.Item leaf(StorageQuery query, Consumer<StorageQuery> apply, StorageQuery.Facet facet,
		int value, String base)
	{
		return ContextMenu.Item.of(query.stateWords(facet, value, base), () -> apply.accept(query.cycled(facet,
			value)));
	}

	private static ContextMenu.Item parent(String label, List<ContextMenu.Item> children,
		Consumer<List<ContextMenu.Item>> submenu)
	{
		return ContextMenu.Item.of(label, () -> submenu.accept(children));
	}
}
