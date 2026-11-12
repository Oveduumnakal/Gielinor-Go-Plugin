/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the bundled species list and that progress survives a save and a load. */
public class DataTest
{
	private final Gson gson = new Gson();

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

	@Test
	public void theBundledSpeciesListLoads() throws IOException
	{
		SpeciesTable table = bundled();

		assertTrue("expected over 1,000 species, got " + table.size(), table.size() > 1000);
		assertNotNull(table.find("Goblin"));
		assertNotNull(table.find("  hill giant "));
		assertEquals(CombatType.RANGED, table.find("Zulrah").style());
		assertEquals(Rarity.LEGENDARY, table.find("Vorkath").rarity());
		assertNull(table.find("Banker"));
		Species demon = table.find("Abyssal demon");
		Species dragon = table.find("Blue dragon");
		Species zulrah = table.find("Zulrah");
		assertEquals(EnumSet.of(Trait.MELEE, Trait.UNHOLY, Trait.SLAYER), demon.traits());
		assertTrue(dragon.traits().contains(Trait.FIERY));
		assertTrue(zulrah.traits().contains(Trait.POISONOUS));
		assertEquals(EnumSet.of(Trait.MELEE), table.find("Goblin").traits());
	}

	@Test
	public void everySpeciesTraitIsOneTheGameKnows()
	{
		assertEquals(EnumSet.of(Trait.MELEE), SpeciesTable.load(gson, new StringReader(
			"[{\"name\":\"Oddity\",\"level\":5,\"style\":\"MELEE\",\"traits\":[\"NOT_A_TRAIT\"]}]"))
			.find("Oddity")
			.traits());
	}

	@Test
	public void everySpeciesIsCompleteAndUnique() throws IOException
	{
		Set<String> keys = new HashSet<>();
		for (Species species : bundled().all())
		{
			assertTrue(species.name() + " has no level", species.level() >= 1);
			assertNotNull(species.style());
			assertNotNull(species.rarity());
			assertTrue(species.name() + " is listed twice", keys.add(species.key()));
			assertEquals(species.name(), species.name().trim());
		}
	}

	@Test
	public void theListIsOrderedWeakestFirst() throws IOException
	{
		int previous = 0;
		for (Species species : bundled().all())
		{
			assertTrue(species.level() >= previous);
			previous = species.level();
		}
	}

	@Test
	public void badSpeciesJsonGivesAnEmptyTable()
	{
		assertEquals(0, SpeciesTable.load(gson, new StringReader("{not json")).size());
		assertEquals(0, SpeciesTable.load(gson, new StringReader("")).size());
	}

	@Test
	public void aSaveSurvivesARoundTrip()
	{
		MapStore store = new MapStore();
		GoPersistence persistence = new GoPersistence(store, gson);
		SaveData data = SaveData.fresh();
		data.team.add(new Creature("goblin", 7, 123L));
		data.storage.add(new Creature("chicken", 2, 456L));
		data.addJars(JarType.RUNE, 4);
		data.selectedJar = JarType.RUNE.name();
		data.trainerXp = 999;
		data.caught.put("goblin", 2);
		data.seen.add("goblin");
		data.stepProgress = 42;
		data.battlesWon = 3;

		persistence.save(data);
		SaveData loaded = persistence.load();

		assertEquals(1, loaded.team.size());
		assertEquals("goblin", loaded.lead().species);
		assertEquals(7, loaded.lead().level());
		assertEquals(123L, loaded.lead().caughtAtMs);
		assertEquals(1, loaded.storage.size());
		assertEquals(4, loaded.jarCount(JarType.RUNE));
		assertEquals(SaveData.STARTING_JARS, loaded.jarCount(JarType.BRONZE));
		assertEquals(JarType.RUNE.name(), loaded.selectedJar);
		assertEquals(999, loaded.trainerXp);
		assertEquals(2, (int) loaded.caught.get("goblin"));
		assertTrue(loaded.seen.contains("goblin"));
		assertEquals(42, loaded.stepProgress);
		assertEquals(3, loaded.battlesWon);
	}

	@Test
	public void aNicknameSurvivesARoundTrip()
	{
		GoPersistence persistence = new GoPersistence(new MapStore(), gson);
		SaveData data = SaveData.fresh();
		Creature goblin = new Creature("goblin", 7, 123L);
		goblin.nickname = "Mr-Bob 2";
		data.team.add(goblin);
		data.storage.add(new Creature("chicken", 2, 456L));

		persistence.save(data);
		SaveData loaded = persistence.load();

		assertEquals("Mr-Bob 2", loaded.team.get(0).nickname);
		assertNull(loaded.storage.get(0).nickname);
	}

	@Test
	public void aBadNicknameInASaveLoadsAsNone()
	{
		MapStore store = new MapStore();
		GoPersistence persistence = new GoPersistence(store, gson);
		SaveData data = SaveData.fresh();
		Creature tagged = new Creature("goblin", 7, 123L);
		tagged.nickname = "<b>Bob</b>";
		Creature long40 = new Creature("goblin", 7, 123L);
		long40.nickname = "0123456789012345678901234567890123456789";
		data.team.add(tagged);
		data.storage.add(long40);

		persistence.save(data);
		SaveData loaded = persistence.load();

		assertNull(loaded.team.get(0).nickname);
		assertNull(loaded.storage.get(0).nickname);
	}

	@Test
	public void anOldSaveLoadsWithNoMilestonesPaid()
	{
		MapStore store = new MapStore();
		store.set(GoPersistence.KEY_SAVE, "{\"version\":1,\"team\":[{\"species\":\"goblin\",\"xp\":5}]}");

		SaveData loaded = new GoPersistence(store, gson).load();

		assertTrue(loaded.milestones.isEmpty());
	}

	@Test
	public void anOldSaveWithoutANicknameStillLoads()
	{
		MapStore store = new MapStore();
		store.set(GoPersistence.KEY_SAVE, "{\"version\":1,\"team\":[{\"species\":\"goblin\",\"xp\":5}]}");

		SaveData loaded = new GoPersistence(store, gson).load();

		assertEquals("goblin", loaded.lead().species);
		assertNull(loaded.lead().nickname);
	}

	@Test
	public void anOldSaveLoadsWithAverageRollsAndNoTemperament()
	{
		MapStore store = new MapStore();
		store.set(GoPersistence.KEY_SAVE, "{\"version\":1,\"team\":[{\"species\":\"goblin\",\"xp\":5}]}");

		SaveData loaded = new GoPersistence(store, gson).load();
		Creature lead = loaded.lead();

		assertEquals(0, lead.rollHp + lead.rollAttack + lead.rollDefence + lead.rollSpeed);
		assertNull(lead.temperament);
		assertEquals(StatRolls.AVERAGE, lead.rolls());
	}

	@Test
	public void anOldSaveStartsWithFreshTasksAndNoStreak()
	{
		MapStore store = new MapStore();
		store.set(GoPersistence.KEY_SAVE, "{\"version\":1,\"team\":[{\"species\":\"goblin\",\"xp\":5}]}");

		SaveData loaded = new GoPersistence(store, gson).load();

		assertNotNull(loaded.tasks);
		assertEquals(0, loaded.tasks.streak);
		assertEquals(3, loaded.tasks.daily.length);
		assertEquals(3, loaded.tasks.dailyPaid.length);
		assertEquals(0, loaded.tasks.weekly);
		assertEquals(-1L, loaded.tasks.lastCatchDay);
	}

	@Test
	public void aNullOrDamagedTaskBlockIsRepaired()
	{
		SaveData nulled = new SaveData();
		nulled.tasks = null;
		assertEquals(0, nulled.sanitized().tasks.streak);

		MapStore store = new MapStore();
		store.set(GoPersistence.KEY_SAVE, "{\"version\":1,\"tasks\":{\"day\":5,\"daily\":[1],"
			+ "\"dailyPaid\":[true,true,true,true,true],\"weekly\":-4,\"streak\":-2}}");

		TaskProgress loaded = new GoPersistence(store, gson).load().tasks;

		assertEquals(5L, loaded.day);
		assertEquals(3, loaded.daily.length);
		assertEquals(1, loaded.daily[0]);
		assertEquals(0, loaded.daily[2]);
		assertEquals(3, loaded.dailyPaid.length);
		assertTrue(loaded.dailyPaid[2]);
		assertEquals(0, loaded.weekly);
		assertEquals(0, loaded.streak);
	}

	@Test
	public void taskProgressAndTheStreakSurviveARoundTrip()
	{
		GoPersistence persistence = new GoPersistence(new MapStore(), gson);
		SaveData data = SaveData.fresh();
		data.tasks.day = 20_000L;
		data.tasks.daily = new int[] {2, 0, 5};
		data.tasks.dailyPaid = new boolean[] {false, false, true};
		data.tasks.week = 2857L;
		data.tasks.weekly = 9;
		data.tasks.weeklyPaid = false;
		data.tasks.streak = 4;
		data.tasks.lastCatchDay = 19_999L;

		persistence.save(data);
		TaskProgress loaded = persistence.load().tasks;

		assertEquals(20_000L, loaded.day);
		assertEquals(5, loaded.daily[2]);
		assertTrue(loaded.dailyPaid[2]);
		assertEquals(2857L, loaded.week);
		assertEquals(9, loaded.weekly);
		assertEquals(4, loaded.streak);
		assertEquals(19_999L, loaded.lastCatchDay);
	}

	@Test
	public void rollsAndTemperamentSurviveARoundTrip()
	{
		GoPersistence persistence = new GoPersistence(new MapStore(), gson);
		SaveData data = SaveData.fresh();
		Creature goblin = new Creature("goblin", 7, 123L);
		goblin.setRolls(new StatRolls(4, -3, 0, 5, Temperament.HASTY));
		data.team.add(goblin);

		persistence.save(data);
		SaveData loaded = persistence.load();

		assertEquals(new StatRolls(4, -3, 0, 5, Temperament.HASTY), loaded.team.get(0).rolls());
		assertEquals("HASTY", loaded.team.get(0).temperament);
	}

	@Test
	public void noSaveMeansAFreshStart()
	{
		SaveData loaded = new GoPersistence(new MapStore(), gson).load();

		assertEquals(SaveData.STARTING_JARS, loaded.jarCount(JarType.BRONZE));
		assertTrue(loaded.team.isEmpty());
	}

	@Test
	public void anUnreadableSaveIsSetAsideNotLost()
	{
		MapStore store = new MapStore();
		store.set(GoPersistence.KEY_SAVE, "{broken");

		SaveData loaded = new GoPersistence(store, gson).load();

		assertTrue(loaded.team.isEmpty());
		assertEquals("{broken", store.get(GoPersistence.KEY_UNREADABLE));
	}

	@Test
	public void aSaveWithMissingFieldsIsRepaired()
	{
		MapStore store = new MapStore();
		store.set(GoPersistence.KEY_SAVE, "{\"team\":[null,{\"species\":\"goblin\",\"xp\":50}],\"jars\":null}");

		SaveData loaded = new GoPersistence(store, gson).load();

		assertEquals(1, loaded.team.size());
		assertEquals(2, loaded.lead().level());
		assertEquals(0, loaded.jarCount(JarType.BRONZE));
		assertNotNull(loaded.storage);
		assertNotNull(loaded.caught);
		assertNotNull(loaded.seen);
		assertEquals(JarType.BRONZE.name(), loaded.selectedJar);
	}

	private SpeciesTable bundled() throws IOException
	{
		try (InputStream in = SpeciesTable.class.getResourceAsStream(SpeciesTable.RESOURCE))
		{
			assertNotNull("species.json is missing from the resources", in);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return SpeciesTable.load(gson, reader);
			}
		}
	}
}
