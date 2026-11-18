/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/**
 * The rule for rearranging the stored creatures in the Storage Box view: dropping a creature on another one swaps
 * the two, and dropping it on an empty cell puts it last. The storage order is what is saved, so this is all the
 * Box needs; it never stores a position of its own. Holds no RuneLite types.
 */
final class StorageArrange
{
	private StorageArrange()
	{
	}

	/**
	 * Applies one drag.
	 *
	 * @param list the stored list
	 * @param from the place of the creature that was dragged
	 * @param to the place of the creature it was dropped on, or any place at or past the size of the list for an
	 *        empty cell
	 * @param <T> what the list holds
	 * @return a new list: the two swapped, or {@code from} moved to the end; an equal copy when nothing changes
	 */
	static <T> List<T> apply(List<T> list, int from, int to)
	{
		List<T> next = new ArrayList<>(list);
		int size = list.size();
		if (from < 0 || from >= size || to < 0 || from == to)
			return next;

		if (to < size)
		{
			next.set(from, list.get(to));
			next.set(to, list.get(from));
			return next;
		}

		next.add(next.remove(from));
		return next;
	}
}
