/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The By species view's list: a heading for every species, in name order A to Z, with that species' creatures under
 * it in the order the Storage query gave them. A heading is shorter than a creature row. Pure: it takes the arranged
 * entries and returns items, so it is tested without a canvas. Holds no RuneLite types.
 */
final class StorageGroups
{
	/** The height of a species heading, in layout units. */
	static final double HEADING_UNITS = 32;

	/** The height of a creature row, in layout units. */
	static final double ROW_UNITS = 72;

	private final List<Item> items;

	private StorageGroups(List<Item> items)
	{
		this.items = Collections.unmodifiableList(items);
	}

	/** One line of the list: a species heading or a creature. */
	static final class Item
	{
		private final String species;
		private final int count;
		private final int bestLevel;
		private final Rarity rarity;
		private final List<Integer> places;
		private final StorageView.Entry entry;

		private Item(String species, int count, int bestLevel, Rarity rarity, List<Integer> places,
			StorageView.Entry entry)
		{
			this.species = species;
			this.count = count;
			this.bestLevel = bestLevel;
			this.rarity = rarity;
			this.places = places;
			this.entry = entry;
		}

		/** @return whether this is a species heading, not a creature */
		boolean heading()
		{
			return entry == null;
		}

		/** @return the species name of a heading */
		String species()
		{
			return species;
		}

		/** @return how many creatures of the species a heading covers */
		int count()
		{
			return count;
		}

		/** @return the highest level among them, for a heading */
		int bestLevel()
		{
			return bestLevel;
		}

		/** @return the species' rarity, for a heading's stripe */
		Rarity rarity()
		{
			return rarity;
		}

		/** @return the places in storage of the creatures a heading covers, in display order */
		List<Integer> places()
		{
			return places;
		}

		/** @return the creature of a row, or null for a heading */
		StorageView.Entry entry()
		{
			return entry;
		}
	}

	/**
	 * Groups the shown creatures by species.
	 *
	 * @param entries the arranged entries
	 * @return the list: each heading is followed by its creatures
	 */
	static StorageGroups of(List<StorageView.Entry> entries)
	{
		Map<String, List<StorageView.Entry>> groups = new LinkedHashMap<>();
		for (StorageView.Entry entry : entries)
			groups.computeIfAbsent(entry.view().name().toLowerCase(Locale.ROOT), key -> new ArrayList<>())
				.add(entry);

		List<String> keys = new ArrayList<>(groups.keySet());
		Collections.sort(keys);
		List<Item> items = new ArrayList<>();
		for (String key : keys)
		{
			List<StorageView.Entry> group = groups.get(key);
			int best = 0;
			List<Integer> places = new ArrayList<>();
			for (StorageView.Entry entry : group)
			{
				best = Math.max(best, entry.view().level());
				places.add(entry.place());
			}

			CreatureView first = group.get(0).view();
			items.add(new Item(first.name(), group.size(), best, first.rarity(), places, null));
			for (StorageView.Entry entry : group)
				items.add(new Item(first.name(), group.size(), best, first.rarity(), places, entry));
		}

		return new StorageGroups(items);
	}

	/** @return the headings and creatures, in display order */
	List<Item> items()
	{
		return items;
	}

	/**
	 * @param style the style
	 * @return the height of each item in pixels: a heading is shorter than a creature row
	 */
	int[] heights(WindowStyle style)
	{
		int heading = Math.max(1, style.px(HEADING_UNITS));
		int row = Math.max(1, style.px(ROW_UNITS));
		int[] heights = new int[items.size()];
		for (int i = 0; i < heights.length; i++)
			heights[i] = items.get(i).heading() ? heading : row;

		return heights;
	}
}
