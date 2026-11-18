/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Sort and Filter menus: their items and the words each one says. */
public class StorageMenusTest
{
	private static List<String> labels(List<ContextMenu.Item> items)
	{
		List<String> labels = new ArrayList<>();
		for (ContextMenu.Item item : items)
			labels.add(item.label());

		return labels;
	}

	@Test
	public void theSortMenuHasSixItemsEachWithItsDefaultDirection()
	{
		List<ContextMenu.Item> items = StorageMenus.sort(StorageQuery.NONE, query ->
		{
		});

		assertEquals(Arrays.asList("Box order", "Level (high to low)", "Species (A to Z)", "Rarity (rarest first)",
			"Date caught (newest first)", "Nickname (A to Z)"), labels(items));
	}

	@Test
	public void theActiveSortShowsItsCurrentDirection()
	{
		StorageQuery flipped = StorageQuery.NONE.withSort(StorageQuery.Sort.LEVEL).withSort(StorageQuery.Sort.LEVEL);

		List<String> labels = labels(StorageMenus.sort(flipped, query ->
		{
		}));

		assertEquals("Level (low to high)", labels.get(1));
		assertEquals("Species (A to Z)", labels.get(2));
	}

	@Test
	public void theFilterMenuListsTheFacetsAndAClearItem()
	{
		List<ContextMenu.Item> items = StorageMenus.filter(StorageQuery.NONE, query ->
		{
		}, children ->
		{
		});

		assertEquals(Arrays.asList("Level over...", "Level under...", "Style...", "Rarity...", "Rating...",
			"Gender...", "Gilded", "Nicknamed", "Clear filters"), labels(items));
		ContextMenu.Item clear = items.get(items.size() - 1);
		assertFalse("nothing to clear", clear.enabled());
		assertEquals(StorageMenus.NO_FILTERS, clear.reason());
	}

	@Test
	public void clearFiltersIsLiveOnceAFilterIsSet()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.GILDED, 0);

		List<ContextMenu.Item> items = StorageMenus.filter(query, next ->
		{
		}, children ->
		{
		});

		assertTrue(items.get(items.size() - 1).enabled());
		assertEquals("Gilded (only)", items.get(6).label());
	}
}
