/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Decides which ticks of the Storage tab's Select mode survive a refresh of the list. A tick stays while the same
 * creature is still in that place; health does not count, because it keeps rising while a creature rests.
 */
final class StorageTicks
{
	private StorageTicks()
	{
	}

	/**
	 * Picks the ticks that still point at the same creature.
	 *
	 * @param before the list the ticks were made on, or null when none was shown
	 * @param now the list about to be shown
	 * @param ticked the ticked places
	 * @return the ticked places whose creature is unchanged
	 */
	static Set<Integer> kept(List<CreatureView> before, List<CreatureView> now, Set<Integer> ticked)
	{
		Set<Integer> kept = new TreeSet<>();
		if (before == null)
			return kept;

		for (int index : ticked)
		{
			if (index < before.size() && index < now.size() && before.get(index).sameCreature(now.get(index)))
				kept.add(index);
		}

		return kept;
	}
}
