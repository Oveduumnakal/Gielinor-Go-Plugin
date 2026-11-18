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

/** Old saves load with nobody fainted, and a new save keeps the flag. */
public class FaintLegacySaveTest
{
	private static final double EPSILON = 1e-9;
	private static final Gson GSON = new Gson();

	private static SaveData load(String creatureJson)
	{
		return GSON.fromJson("{\"team\":[" + creatureJson + "]}", SaveData.class).sanitized();
	}

	@Test
	public void aCreatureSavedBeforeFaintingLoadsUnfainted()
	{
		SaveData loaded = load("{\"species\":\"chicken\",\"xp\":0,\"hpFraction\":0.8}");

		assertFalse(loaded.team.get(0).fainted);
		assertEquals(0.8, loaded.team.get(0).hpFraction, EPSILON);
	}

	@Test
	public void anOldCreatureSavedAtZeroHealthStillRestsBackUp()
	{
		SaveData loaded = load("{\"species\":\"chicken\",\"xp\":0,\"hpFraction\":0.0,\"hpStampMs\":1000}");
		Creature creature = loaded.team.get(0);

		assertFalse(creature.fainted);
		assertEquals(1.0, creature.hpFractionAt(1_000L + Creature.FULL_REST_MS), EPSILON);
	}

	@Test
	public void aFaintedCreatureLoadsWithNoHealthWhateverTheSaveSays()
	{
		SaveData loaded = load("{\"species\":\"chicken\",\"xp\":0,\"hpFraction\":0.7,\"fainted\":true}");
		Creature creature = loaded.team.get(0);

		assertTrue(creature.fainted);
		assertEquals(0.0, creature.hpFraction, EPSILON);
		assertEquals(0.0, creature.hpFractionAt(Long.MAX_VALUE / 2), EPSILON);
	}

	@Test
	public void theFlagSurvivesASaveAndLoad()
	{
		SaveData data = SaveData.fresh();
		Creature creature = new Creature("chicken", 2, 1_000L);
		creature.faint(5_000L);
		data.team.add(creature);

		SaveData back = GSON.fromJson(GSON.toJson(data), SaveData.class).sanitized();

		assertTrue(back.team.get(0).fainted);
		assertEquals(5_000L, back.team.get(0).hpStampMs);
	}
}
