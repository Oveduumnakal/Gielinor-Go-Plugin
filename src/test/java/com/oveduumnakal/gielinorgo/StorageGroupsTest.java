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

/** The By species grouping: headings A to Z, creatures in the order given, counts, best level, heights. */
public class StorageGroupsTest
{
	private static final String[] NAMES = {"Man", "cow", "Goblin", "Rat", "Imp"};

	private static List<StorageView.Entry> entries(CreatureView... views)
	{
		List<StorageView.Entry> list = new ArrayList<>();
		for (int i = 0; i < views.length; i++)
			list.add(new StorageView.Entry(i, views[i]));

		return list;
	}

	private static CreatureView of(String name, int level)
	{
		return PartyRig.creature(name, level, 1.0, null);
	}

	@Test
	public void groupsAreOrderedByNameIgnoringCase()
	{
		StorageGroups groups = StorageGroups.of(entries(of("Man", 3), of("cow", 4), of("Goblin", 5), of("Cow", 6)));

		List<String> headings = new ArrayList<>();
		for (StorageGroups.Item item : groups.items())
		{
			if (item.heading())
				headings.add(item.species());
		}

		assertEquals(Arrays.asList("cow", "Goblin", "Man"), headings);
	}

	@Test
	public void creaturesKeepTheOrderTheQueryGaveThem()
	{
		StorageGroups groups = StorageGroups.of(entries(of("Goblin", 9), of("Cow", 4), of("Goblin", 2),
			of("Goblin", 30)));

		List<Integer> places = new ArrayList<>();
		for (StorageGroups.Item item : groups.items())
		{
			if (!item.heading() && item.species().equals("Goblin"))
				places.add(item.entry().place());
		}

		assertEquals(Arrays.asList(0, 2, 3), places);
	}

	@Test
	public void aHeadingHasTheCountTheBestLevelAndThePlaces()
	{
		StorageGroups groups = StorageGroups.of(entries(of("Goblin", 9), of("Cow", 4), of("Goblin", 24),
			of("Goblin", 2)));

		StorageGroups.Item goblin = groups.items().get(2);

		assertTrue(goblin.heading());
		assertEquals("Goblin", goblin.species());
		assertEquals(3, goblin.count());
		assertEquals(24, goblin.bestLevel());
		assertEquals(Arrays.asList(0, 2, 3), goblin.places());
		assertEquals(Rarity.RARE, goblin.rarity());
		assertEquals(6, groups.items().size());
	}

	@Test
	public void aHeadingIsShorterThanARow()
	{
		StorageGroups groups = StorageGroups.of(entries(of("Goblin", 9), of("Cow", 4)));

		int[] heights = groups.heights(WidgetKit.style(1.0));

		assertEquals(4, heights.length);
		assertEquals(32, heights[0]);
		assertEquals(72, heights[1]);
		assertEquals(32, heights[2]);
		assertEquals(72, heights[3]);
	}

	@Test
	public void fortyCreaturesOfFiveSpeciesMakeFortyFiveItems()
	{
		List<StorageView.Entry> list = new ArrayList<>();
		for (int i = 0; i < 40; i++)
			list.add(new StorageView.Entry(i, of(NAMES[i % NAMES.length], i + 1)));

		StorageGroups groups = StorageGroups.of(list);

		int headings = 0;
		int rows = 0;
		for (StorageGroups.Item item : groups.items())
		{
			if (item.heading())
			{
				headings++;
				assertEquals(8, item.count());
			}
			else
			{
				rows++;
			}
		}

		assertEquals(5, headings);
		assertEquals(40, rows);
		List<StorageGroups.Item> items = groups.items();
		assertTrue(items.get(0).heading());
	}

	@Test
	public void noCreaturesMakeNoItems()
	{
		StorageGroups groups = StorageGroups.of(new ArrayList<>());

		assertTrue(groups.items().isEmpty());
		assertEquals(0, groups.heights(WidgetKit.style(1.0)).length);
		StorageGroups one = StorageGroups.of(entries(of("Rat", 1)));
		assertFalse(one.items().isEmpty());
	}
}
