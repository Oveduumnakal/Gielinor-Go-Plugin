/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The arranged Storage list: filter then stable sort, with every entry keeping its real place. */
public class StorageViewTest
{
	private static CreatureView make(String name, int level, Rarity rarity, CombatType type, String nickname,
		long caught)
	{
		return new CreatureView(name, level, 0.5, 1.0, rarity, type, false, nickname, StatRolls.AVERAGE)
			.withGender(Gender.MALE)
			.withCaught(caught);
	}

	private static List<CreatureView> sample()
	{
		return Arrays.asList(
			make("Goblin", 10, Rarity.COMMON, CombatType.CRUSH, null, 300),
			make("cow", 30, Rarity.UNCOMMON, CombatType.SLASH, "Moo", 100),
			make("Imp", 20, Rarity.EPIC, CombatType.MAGIC, "Apple", 200),
			make("Rat", 20, Rarity.COMMON, CombatType.STAB, null, 400),
			make("Man", 20, Rarity.COMMON, CombatType.STAB, "Zed", 50));
	}

	private static int[] places(List<CreatureView> storage, StorageQuery query)
	{
		return StorageView.places(StorageView.arrange(storage, query));
	}

	private static StorageQuery sorted(StorageQuery.Sort sort, boolean descending)
	{
		StorageQuery query = StorageQuery.NONE.withSort(sort);
		return query.descending() == descending ? query : query.withSort(sort);
	}

	@Test
	public void boxOrderIsTheIdentity()
	{
		assertArrayEquals(new int[] {0, 1, 2, 3, 4}, places(sample(), StorageQuery.NONE));
	}

	@Test
	public void levelSortsBothWaysAndTiesGoBySpecies()
	{
		assertArrayEquals(new int[] {1, 2, 4, 3, 0}, places(sample(), sorted(StorageQuery.Sort.LEVEL, true)));
		assertArrayEquals(new int[] {0, 2, 4, 3, 1}, places(sample(), sorted(StorageQuery.Sort.LEVEL, false)));
	}

	@Test
	public void speciesSortsIgnoringCase()
	{
		assertArrayEquals(new int[] {1, 0, 2, 4, 3}, places(sample(), sorted(StorageQuery.Sort.SPECIES, false)));
		assertArrayEquals(new int[] {3, 4, 2, 0, 1}, places(sample(), sorted(StorageQuery.Sort.SPECIES, true)));
	}

	@Test
	public void raritySortsRarestFirstByDefault()
	{
		assertArrayEquals(new int[] {2, 1, 0, 4, 3}, places(sample(), StorageQuery.NONE
			.withSort(StorageQuery.Sort.RARITY)));
		assertArrayEquals(new int[] {0, 4, 3, 1, 2}, places(sample(), sorted(StorageQuery.Sort.RARITY, false)));
	}

	@Test
	public void dateCaughtSortsNewestFirstByDefault()
	{
		assertArrayEquals(new int[] {3, 0, 2, 1, 4}, places(sample(), StorageQuery.NONE
			.withSort(StorageQuery.Sort.DATE_CAUGHT)));
		assertArrayEquals(new int[] {4, 1, 2, 0, 3}, places(sample(), sorted(StorageQuery.Sort.DATE_CAUGHT, false)));
	}

	@Test
	public void nicknamelessCreaturesComeLastInBothDirections()
	{
		assertArrayEquals(new int[] {2, 1, 4, 0, 3}, places(sample(), sorted(StorageQuery.Sort.NICKNAME, false)));
		assertArrayEquals(new int[] {4, 1, 2, 0, 3}, places(sample(), sorted(StorageQuery.Sort.NICKNAME, true)));
	}

	@Test
	public void equalCreaturesKeepTheirStoredOrder()
	{
		List<CreatureView> twins = Arrays.asList(make("Imp", 5, Rarity.COMMON, CombatType.MAGIC, null, 1),
			make("Imp", 5, Rarity.COMMON, CombatType.MAGIC, null, 1),
			make("Imp", 5, Rarity.COMMON, CombatType.MAGIC, null, 1));

		for (StorageQuery.Sort sort : StorageQuery.Sort.values())
		{
			assertArrayEquals(sort.label(), new int[] {0, 1, 2}, places(twins, StorageQuery.NONE.withSort(sort)));
			assertArrayEquals(sort.label(), new int[] {0, 1, 2}, places(twins, StorageQuery.NONE.withSort(sort)
				.withSort(sort)));
		}
	}

	@Test
	public void aFilteredCreatureKeepsItsRealPlace()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.STYLE, CombatType.STAB.ordinal());

		List<StorageView.Entry> entries = StorageView.arrange(sample(), query);

		assertEquals(2, entries.size());
		assertEquals(3, entries.get(0).place());
		CreatureView first = entries.get(0).view();

		assertEquals("Rat", first.name());
		assertEquals(4, entries.get(1).place());
	}

	@Test
	public void filterAndSortWorkTogether()
	{
		StorageQuery query = sorted(StorageQuery.Sort.LEVEL, true)
			.cycled(StorageQuery.Facet.LEVEL_OVER, 15);

		assertArrayEquals(new int[] {1, 2, 4, 3}, places(sample(), query));
	}

	@Test
	public void noMatchGivesAnEmptyList()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.LEVEL_OVER, 90);

		assertTrue(StorageView.arrange(sample(), query).isEmpty());
		assertEquals(0, StorageView.places(StorageView.arrange(sample(), query)).length);
	}

	@Test
	public void anEmptyStorageGivesAnEmptyList()
	{
		assertTrue(StorageView.arrange(new ArrayList<>(), sorted(StorageQuery.Sort.LEVEL, true)).isEmpty());
	}
}
