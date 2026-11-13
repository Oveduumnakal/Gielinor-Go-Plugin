/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks that a save lands in the account the game was loaded from, even when the current account has moved on. */
public class SaveFlushTest
{
	private static final Species IMP = new Species("Imp", 7, CombatType.STAB);

	private final Gson gson = new Gson();

	/** A store with two accounts, like the config manager, of which one is current. */
	private static final class ProfileStore implements GoPersistence.Store
	{
		private final Map<String, String> values = new HashMap<>();
		private String current = "old";

		@Override
		public String get(String key)
		{
			return values.get(current + "/" + key);
		}

		@Override
		public void set(String key, String value)
		{
			values.put(current + "/" + key, value);
		}

		@Override
		public void setIn(String profileKey, String key, String value)
		{
			values.put(profileKey + "/" + key, value);
		}
	}

	private GoGame dirtyGame()
	{
		GoGame game = new GoGame(new SpeciesTable(Arrays.asList(IMP)), new Random(5), () -> 0L, notice ->
		{
		});
		Egg egg = new Egg(Rarity.COMMON);
		game.data().eggs.add(egg);
		game.giveAway(egg);
		game.data().trainerXp = 777;
		return game;
	}

	@Test
	public void aSaveMadeAfterTheProfileChangedStillGoesToTheOldProfile()
	{
		ProfileStore store = new ProfileStore();
		GoPersistence persistence = new GoPersistence(store, gson);
		GoGame game = dirtyGame();
		store.current = "new";

		assertTrue(SaveFlush.write(game, persistence, "old", false));

		assertNull(store.values.get("new/" + GoPersistence.KEY_SAVE));
		assertTrue(store.values.get("old/" + GoPersistence.KEY_SAVE).contains("777"));
	}

	@Test
	public void nothingIsWrittenWhenNothingChanged()
	{
		ProfileStore store = new ProfileStore();
		GoPersistence persistence = new GoPersistence(store, gson);
		GoGame game = dirtyGame();
		game.takeDirty();

		assertFalse(SaveFlush.write(game, persistence, "old", false));
		assertTrue(store.values.isEmpty());
	}

	@Test
	public void theGameGivenIsTheOneWritten()
	{
		ProfileStore store = new ProfileStore();
		GoPersistence persistence = new GoPersistence(store, gson);
		GoGame running = dirtyGame();
		GoGame replacement = new GoGame(new SpeciesTable(Arrays.asList(IMP)), new Random(5), () -> 0L, notice ->
		{
		});

		assertTrue(SaveFlush.write(running, persistence, "old", true));

		assertEquals(777, gson.fromJson(store.values.get("old/" + GoPersistence.KEY_SAVE), SaveData.class).trainerXp);
		assertEquals(0, replacement.data().trainerXp);
	}

	@Test
	public void aMissingProfileKeyWritesToTheCurrentProfile()
	{
		ProfileStore store = new ProfileStore();
		store.current = "other";

		assertTrue(SaveFlush.write(dirtyGame(), new GoPersistence(store, gson), null, false));

		assertTrue(store.values.containsKey("other/" + GoPersistence.KEY_SAVE));
	}
}
