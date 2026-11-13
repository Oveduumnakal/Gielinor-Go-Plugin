/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks that a reset puts the whole save back to a fresh one, and when it is refused. */
public class ResetProgressTest
{
	private final Gson gson = new Gson();
	private final List<String> messages = new ArrayList<>();
	private GoGame game;
	private long now = 1_000_000L;

	/** A {@link GoPersistence.Store} backed by a plain map. */
	private static final class MapStore implements GoPersistence.Store
	{
		private final Map<String, String> values = new HashMap<>();

		@Override
		public String get(String key)
		{
			return values.get(key);
		}

		@Override
		public void set(String key, String value)
		{
			values.put(key, value);
		}
	}

	@Before
	public void setUp()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(new Species("Mite", 1, CombatType.STAB),
			new Species("Brute", 200, CombatType.STAB)));
		game = new GoGame(species, new Random(7), () -> now, notice -> messages.add(notice.text()));
	}

	private void playALot()
	{
		SaveData data = game.data();
		data.team.add(new Creature("brute", 60, now));
		data.storage.add(new Creature("mite", 3, now));
		data.eggs.add(new Egg());
		data.addJars(JarType.RUNE, 4);
		data.selectedJar = JarType.RUNE.name();
		data.addItems(ItemType.SMALL_SALVE, 2);
		data.trainerXp = 5000;
		data.caught.put("brute", 2);
		data.seen.add("brute");
		data.gilded.add("brute");
		data.sigils.add("city-1");
		data.milestones.add("m1");
		data.eliteClears = 2;
		data.battlesWon = 9;
		data.stepProgress = 40;
		data.tasks.streak = 5;
	}

	@Test
	public void aResetLeavesExactlyAFreshSave()
	{
		playALot();

		game.reset();

		assertEquals(gson.toJson(SaveData.fresh()), gson.toJson(game.data()));
		assertEquals(SaveData.STARTING_JARS, game.data().jarCount(JarType.BRONZE));
		assertTrue(game.takeDirty());
		assertTrue(messages.contains("Progress reset"));
	}

	@Test
	public void theSummaryCountsWhatWillBeLost()
	{
		playALot();

		assertEquals("1 creature in your team, 1 in Storage, 1 species in the Log, 1 sigil, 1 egg",
			game.resetSummary());
		assertEquals(game.resetSummary(), game.snapshot().resetSummary());
	}

	@Test
	public void anOpenTradeBlocksTheReset()
	{
		assertNull(game.resetBlocker());
		game.tradeLocks(new GoGame.TradeLocks()
		{
			@Override
			public boolean open()
			{
				return true;
			}
		});

		assertEquals("Finish or cancel the trade first", game.resetBlocker());
	}

	@Test
	public void anOpenEliteRunBlocksTheReset()
	{
		StringBuilder stages = new StringBuilder();
		for (int i = 1; i <= 5; i++)
		{
			stages.append(i > 1 ? "," : "").append("{\"id\":\"e").append(i).append("\",\"city\":\"Elite ").append(i)
				.append("\",\"team\":[{\"species\":\"Mite\",\"level\":70}]}");
		}

		String json = "{\"minX\":3180,\"minY\":3346,\"maxX\":3202,\"maxY\":3372,\"firstDragonJars\":10,"
			+ "\"firstSpecialtyJars\":3,\"firstEgg\":\"LEGENDARY\",\"laterDragonJars\":2,\"stages\":[" + stages + "]}";
		SpeciesTable species = new SpeciesTable(Arrays.asList(new Species("Mite", 1, CombatType.STAB),
			new Species("Brute", 200, CombatType.STAB)));
		game.elite(EliteTable.load(gson, new StringReader(json), species));
		game.data().team.add(new Creature("brute", 60, now));
		for (int i = 0; i < 8; i++)
			game.data().sigils.add("city-" + i);

		game.moveTo(3191, 3367, 0);

		assertNotNull(game.eliteSteered());
		assertEquals("Finish or forfeit the Elite run first", game.resetBlocker());
	}

	@Test
	public void theBackUpCopiesTheOldSaveAndLeavesTheSaveAlone()
	{
		MapStore store = new MapStore();
		GoPersistence persistence = new GoPersistence(store, gson);
		playALot();
		persistence.save(game.data());
		String before = store.get(GoPersistence.KEY_SAVE);

		persistence.backUp();

		assertEquals(before, store.get(GoPersistence.KEY_BEFORE_RESET));
		assertEquals(before, store.get(GoPersistence.KEY_SAVE));
	}

	@Test
	public void aSecondBackUpReplacesTheFirst()
	{
		MapStore store = new MapStore();
		GoPersistence persistence = new GoPersistence(store, gson);
		persistence.save(SaveData.fresh());
		persistence.backUp();
		playALot();
		persistence.save(game.data());

		persistence.backUp();

		assertEquals(store.get(GoPersistence.KEY_SAVE), store.get(GoPersistence.KEY_BEFORE_RESET));
	}

	@Test
	public void nothingIsCopiedWhenNothingIsSaved()
	{
		MapStore store = new MapStore();

		new GoPersistence(store, gson).backUp();

		assertNull(store.get(GoPersistence.KEY_BEFORE_RESET));
	}
}
