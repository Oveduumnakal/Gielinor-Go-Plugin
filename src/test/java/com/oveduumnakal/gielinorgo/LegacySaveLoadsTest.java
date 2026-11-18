/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** An older save, written before pins existed, loads with nothing pinned. */
public class LegacySaveLoadsTest
{
	private static final String OLD = "{\"version\":1,\"eggs\":[{\"rarity\":\"RARE\",\"tilesLeft\":5,"
		+ "\"tilesTotal\":9}],\"tasks\":{\"day\":20000,\"daily\":[1,2,3],\"dailyPaid\":[false,false,true],"
		+ "\"week\":2857,\"weekly\":4,\"weeklyPaid\":false,\"streak\":3,\"lastCatchDay\":19999}}";

	@Test
	public void anOldSaveHasNothingPinned()
	{
		SaveData old = new Gson().fromJson(OLD, SaveData.class).sanitized();

		assertFalse(old.eggs.get(0).pinned);
		assertEquals(TaskCalendar.DAILY_COUNT, old.tasks.pinnedDaily.length);
		assertFalse(old.tasks.pinnedDaily[0] || old.tasks.pinnedDaily[1] || old.tasks.pinnedDaily[2]);
		assertFalse(old.tasks.pinnedWeekly);
		assertEquals(4, old.tasks.weekly);
		assertTrue(old.tasks.dailyPaid[2]);
	}

	@Test
	public void aSaveWithNoTaskProgressAtAllStillPadsThePins()
	{
		SaveData bare = new Gson().fromJson("{\"version\":1}", SaveData.class).sanitized();

		assertEquals(TaskCalendar.DAILY_COUNT, bare.tasks.pinnedDaily.length);
	}

	@Test
	public void aDamagedPinArrayIsRepaired()
	{
		SaveData odd = new Gson().fromJson("{\"tasks\":{\"pinnedDaily\":[true]}}", SaveData.class).sanitized();
		SaveData none = new Gson().fromJson("{\"tasks\":{\"pinnedDaily\":null}}", SaveData.class).sanitized();

		assertEquals(TaskCalendar.DAILY_COUNT, odd.tasks.pinnedDaily.length);
		assertTrue(odd.tasks.pinnedDaily[0]);
		assertEquals(TaskCalendar.DAILY_COUNT, none.tasks.pinnedDaily.length);
	}

	@Test
	public void pinsSurviveASaveAndLoad()
	{
		Gson gson = new Gson();
		SaveData data = new SaveData();
		data.eggs.add(new Egg(Rarity.EPIC));
		data.eggs.get(0).pinned = true;
		data.tasks.pinnedDaily[1] = true;
		data.tasks.pinnedWeekly = true;

		SaveData back = gson.fromJson(gson.toJson(data), SaveData.class).sanitized();

		assertTrue(back.eggs.get(0).pinned);
		assertTrue(back.tasks.pinnedDaily[1]);
		assertFalse(back.tasks.pinnedDaily[0]);
		assertTrue(back.tasks.pinnedWeekly);
	}
}
