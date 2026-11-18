/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Turns the stored list and a {@link StorageQuery} into the creatures the Storage page shows, in order. Every entry
 * keeps its real place in storage, because a place, never a position in the shown list, is what a click, a tick, a
 * move and a release use. Every Storage view reads the same arranged list. Holds no RuneLite types.
 */
final class StorageView
{
	private StorageView()
	{
	}

	/** One creature the page shows: where it really is in storage, and what it looks like. */
	static final class Entry
	{
		private final int place;
		private final CreatureView view;

		/**
		 * @param place the creature's index in storage
		 * @param view the creature
		 */
		Entry(int place, CreatureView view)
		{
			this.place = place;
			this.view = view;
		}

		/** @return the creature's index in storage */
		int place()
		{
			return place;
		}

		/** @return the creature */
		CreatureView view()
		{
			return view;
		}
	}

	/**
	 * Filters a stored list and sorts what is left. The sort is stable: ties go by species name, then level high
	 * first, then place.
	 *
	 * @param storage the stored creatures, in storage order
	 * @param query what to show and how to order it
	 * @return the entries to show, in display order
	 */
	static List<Entry> arrange(List<CreatureView> storage, StorageQuery query)
	{
		List<Entry> entries = new ArrayList<>();
		for (int place = 0; place < storage.size(); place++)
		{
			if (query.accepts(storage.get(place)))
				entries.add(new Entry(place, storage.get(place)));
		}

		if (query.sort() != StorageQuery.Sort.BOX_ORDER)
			entries.sort(comparator(query));

		return entries;
	}

	/**
	 * @param entries the shown entries
	 * @return their places, in display order
	 */
	static int[] places(List<Entry> entries)
	{
		int[] places = new int[entries.size()];
		for (int i = 0; i < places.length; i++)
			places[i] = entries.get(i).place();

		return places;
	}

	private static Comparator<Entry> comparator(StorageQuery query)
	{
		Comparator<Entry> key = keyOf(query.sort(), query.descending());
		return key.thenComparing(Comparator.comparing((Entry e) -> lower(e.view().name())))
			.thenComparing(Comparator.comparingInt((Entry e) -> -e.view().level()))
			.thenComparingInt(Entry::place);
	}

	private static Comparator<Entry> keyOf(StorageQuery.Sort sort, boolean descending)
	{
		Comparator<Entry> natural;
		switch (sort)
		{
			case LEVEL:
				natural = Comparator.comparingInt(e -> e.view().level());
				break;
			case SPECIES:
				natural = Comparator.comparing(e -> lower(e.view().name()));
				break;
			case RARITY:
				natural = Comparator.comparingInt(e -> e.view().rarity().ordinal());
				break;
			case DATE_CAUGHT:
				natural = Comparator.comparingLong(e -> e.view().caughtAtMs());
				break;
			default:
				return nicknames(descending);
		}

		return descending ? natural.reversed() : natural;
	}

	private static Comparator<Entry> nicknames(boolean descending)
	{
		Comparator<String> names = Comparator.comparing(StorageView::lower);
		Comparator<String> ordered = descending ? names.reversed() : names;
		return (a, b) ->
		{
			String x = a.view().nickname();
			String y = b.view().nickname();
			if (x == null || y == null)
				return x == null ? y == null ? 0 : 1 : -1;

			return ordered.compare(x, y);
		};
	}

	private static String lower(String text)
	{
		return text.toLowerCase(Locale.ROOT);
	}
}
