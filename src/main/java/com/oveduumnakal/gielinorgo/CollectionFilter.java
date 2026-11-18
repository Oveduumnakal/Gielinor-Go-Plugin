/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Set;

/** Which species the Collection page's list shows. Holds no RuneLite types. */
enum CollectionFilter
{
	/** Every species. */
	ALL("All"),

	/** Species caught at least once. */
	CAUGHT("Caught"),

	/** Species seen but never caught. */
	SEEN_ONLY("Seen only"),

	/** Species not met yet. */
	NOT_SEEN("Not seen"),

	/** Today's featured species. */
	FEATURED("Featured");

	private final String label;

	CollectionFilter(String label)
	{
		this.label = label;
	}

	/** @return the words on the filter button */
	String label()
	{
		return label;
	}

	/** @return the next filter, wrapping round after the last */
	CollectionFilter next()
	{
		CollectionFilter[] all = values();
		return all[(ordinal() + 1) % all.length];
	}

	/**
	 * Tells whether the player has met a species.
	 *
	 * @param entry the species
	 * @return true when it was seen or caught, so its name and details show
	 */
	static boolean known(LogEntry entry)
	{
		return entry.seen() || entry.caught();
	}

	/**
	 * Tests one species against this filter.
	 *
	 * @param entry the species
	 * @param featured the keys of today's featured species
	 * @return whether the list should show it
	 */
	boolean matches(LogEntry entry, Set<String> featured)
	{
		switch (this)
		{
			case FEATURED:
				return featured.contains(Species.keyFor(entry.name()));
			case CAUGHT:
				return entry.caught();
			case SEEN_ONLY:
				return entry.seen() && !entry.caught();
			case NOT_SEEN:
				return !known(entry);
			default:
				return true;
		}
	}
}
