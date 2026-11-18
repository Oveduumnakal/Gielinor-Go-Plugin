/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** The ways the Storage page can show the stored creatures. */
enum StorageMode
{
	/** One row for every creature, as the page always did. */
	LIST("List"),

	/** A grid of portraits that can be rearranged by dragging. */
	BOX("Box"),

	/** A list with a heading for every species. */
	SPECIES("By species");

	private final String label;

	StorageMode(String label)
	{
		this.label = label;
	}

	/** @return the name the View button and its menu show */
	String label()
	{
		return label;
	}

	/** @return the next view, wrapping from the last to the first */
	StorageMode next()
	{
		StorageMode[] all = values();
		return all[(ordinal() + 1) % all.length];
	}
}
