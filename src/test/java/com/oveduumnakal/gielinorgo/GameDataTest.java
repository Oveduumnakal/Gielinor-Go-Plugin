/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the bundled evolution lines and City Champions against the rules the game relies on. */
public class GameDataTest
{
	private static final Type STEPS = new TypeToken<List<EvolutionTable.Step>>()
	{
	}.getType();

	private static final Type CHAMPIONS = new TypeToken<List<Champion>>()
	{
	}.getType();

	private final Gson gson = new Gson();
	private SpeciesTable species;
	private List<EvolutionTable.Step> steps;
	private List<Champion> champions;

	@Before
	public void load() throws IOException
	{
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			species = SpeciesTable.load(gson, reader);
		}

		try (Reader reader = open(EvolutionTable.RESOURCE))
		{
			steps = gson.fromJson(reader, STEPS);
		}

		try (Reader reader = open(ChampionTable.RESOURCE))
		{
			champions = gson.fromJson(reader, CHAMPIONS);
		}
	}

	@Test
	public void everyEvolutionLinksTwoRealSpeciesAndGetsStronger()
	{
		assertTrue("expected at least 50 evolutions, got " + steps.size(), steps.size() >= 50);
		Set<String> froms = new HashSet<>();
		for (EvolutionTable.Step step : steps)
		{
			Species from = species.find(step.from());
			Species to = species.find(step.to());
			String link = step.from() + " to " + step.to();
			assertNotNull(link + ": unknown species " + step.from(), from);
			assertNotNull(link + ": unknown species " + step.to(), to);
			assertEquals(link + ": name must match the species list exactly", step.from(), from.name());
			assertEquals(link + ": name must match the species list exactly", step.to(), to.name());
			assertTrue(link + ": the next stage must be a stronger species", to.level() > from.level());
			assertTrue(link + ": level out of range", step.level() >= 10 && step.level() <= 85);
			assertTrue(step.from() + " evolves twice", froms.add(from.key()));
		}
	}

	@Test
	public void evolutionLinesAreShortAndTheirLevelsRise()
	{
		Map<String, EvolutionTable.Step> byFrom = new HashMap<>();
		for (EvolutionTable.Step step : steps)
			byFrom.put(Species.keyFor(step.from()), step);

		for (EvolutionTable.Step start : steps)
		{
			int stages = 1;
			int lastLevel = 0;
			EvolutionTable.Step step = start;
			while (step != null)
			{
				stages++;
				assertTrue(start.from() + ": line is longer than 4 stages or loops", stages <= 4);
				assertTrue(start.from() + ": levels must rise along the line", step.level() > lastLevel);
				lastLevel = step.level();
				step = byFrom.get(Species.keyFor(step.to()));
			}
		}
	}

	@Test
	public void theEvolutionTableLoadsEveryLink()
	{
		EvolutionTable table = new EvolutionTable(steps, species);

		assertEquals(steps.size(), table.size());
		assertEquals("Lesser demon", table.next("Imp").to());
		assertNull(table.next("Zulrah"));
	}

	@Test
	public void championsAreCompleteAndGetHarder()
	{
		assertEquals(8, champions.size());
		assertEquals("lumbridge", champions.get(0).id());
		Set<String> ids = new HashSet<>();
		int previousTop = 0;
		for (Champion champion : champions)
		{
			assertTrue(champion.id() + ": id must be lowercase letters and hyphens", champion.id().matches("[a-z-]+"));
			assertTrue(champion.id() + " is listed twice", ids.add(champion.id()));
			assertFalse(champion.city().isEmpty());
			assertEquals(champion.id() + ": team size", 3, champion.team().size());
			for (Champion.Member member : champion.team())
			{
				Species found = species.find(member.species());
				assertNotNull(champion.id() + ": unknown species " + member.species(), found);
				assertEquals(member.species(), found.name());
			}

			assertTrue(champion.id() + " must be harder than the city before it", champion.topLevel() > previousTop);
			previousTop = champion.topLevel();
			assertFalse(champion.rewardJar().specialty());
			assertTrue(champion.rewardJars() >= 2 && champion.rewardJars() <= 5);
		}

		assertEquals(champions.size(), new ChampionTable(champions, species).all().size());
	}

	@Test
	public void cityRectanglesDoNotOverlapAndHoldTheirLandmarks()
	{
		for (int i = 0; i < champions.size(); i++)
		{
			for (int j = i + 1; j < champions.size(); j++)
			{
				Champion first = champions.get(i);
				Champion second = champions.get(j);
				assertFalse(first.id() + " overlaps " + second.id(), first.overlaps(second));
			}
		}

		ChampionTable table = new ChampionTable(champions, species);
		assertEquals("lumbridge", table.at(3222, 3218, 0).id());
		assertEquals("varrock", table.at(3212, 3428, 0).id());
		assertEquals("falador", table.at(2965, 3380, 0).id());
		assertEquals("east-ardougne", table.at(2660, 3300, 0).id());
		assertNull("upstairs is not the city floor", table.at(3222, 3218, 1));
		assertNull("the Wilderness has no champion", table.at(3100, 3800, 0));
	}

	@Test
	public void theBlocklistIsNotEmpty() throws IOException
	{
		assertFalse("scripts/species-blocklist.txt has no names", blockedNames().isEmpty());
	}

	@Test
	public void noBlockedSpeciesIsBundled() throws IOException
	{
		for (String name : blockedNames())
			assertNull(name + " is blocked but still bundled", species.find(name));
	}

	private static List<String> blockedNames() throws IOException
	{
		List<String> names = new ArrayList<>();
		for (String line : Files.readAllLines(Paths.get("scripts", "species-blocklist.txt"), StandardCharsets.UTF_8))
		{
			String name = line.split("#", 2)[0].trim();
			if (!name.isEmpty())
				names.add(name);
		}

		return names;
	}

	private static Reader open(String resource)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(resource);
		assertNotNull(resource + " is missing from the resources", in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	@Test
	public void everyFamilyIsMadeOfRealSpeciesAndBigEnough() throws IOException
	{
		List<Family> families;
		try (Reader reader = open(Family.RESOURCE))
		{
			families = Family.load(gson, reader);
		}

		assertTrue("expected about 10 families, got " + families.size(), families.size() >= 8);
		Set<String> ids = new HashSet<>();
		for (Family family : families)
		{
			assertTrue(family.id() + " is listed twice", ids.add(family.id()));
			assertTrue(family.id() + ": jar must be a specialty jar", family.jar().specialty());
			assertTrue(family.id() + ": too few species", family.species().size() >= 3);
			for (String key : family.species())
				assertNotNull(family.id() + ": unknown species " + key, species.find(key));
		}

		Milestones milestones = new Milestones(species, families);
		assertEquals(6 + families.size(), milestones.all().size());
	}

	@Test
	public void theEliteChallengeIsMadeOfRealStrongSpeciesAndGetsHarder() throws IOException
	{
		EliteTable elite;
		try (Reader reader = open(EliteTable.RESOURCE))
		{
			elite = EliteTable.load(gson, reader, species);
		}

		assertTrue("elite.json did not load: every species must exist and there must be 5 teams", elite.available());
		int previousTop = 0;
		for (int i = 0; i < elite.stages().size(); i++)
		{
			Champion stage = elite.stages().get(i);
			boolean grand = i == elite.stages().size() - 1;
			assertEquals(stage.id() + ": team size", grand ? 6 : 5, stage.team().size());
			int last = 0;
			for (Champion.Member member : stage.team())
			{
				Species found = species.find(member.species());
				assertEquals(member.species(), found.name());
				assertTrue(member.species() + " must be Epic or Legendary",
					found.rarity().ordinal() >= Rarity.EPIC.ordinal());
				assertTrue(stage.id() + ": levels must not fall within a team", member.level() >= last);
				assertTrue(stage.id() + ": level range", grand ? member.level() >= 85 && member.level() <= 99
					: member.level() >= 70 && member.level() <= 85);
				last = member.level();
			}

			assertTrue(stage.id() + ": must be stronger than the one before", stage.topLevel() > previousTop);
			previousTop = stage.topLevel();
		}
	}

	@Test
	public void theEliteRectangleOverlapsNoCityAndCoversTheGuildDoor() throws IOException
	{
		EliteTable elite;
		try (Reader reader = open(EliteTable.RESOURCE))
		{
			elite = EliteTable.load(gson, reader, species);
		}

		for (Champion city : champions)
			assertFalse("the Elite rectangle overlaps " + city.id(), elite.overlaps(city));

		assertTrue(elite.contains(3191, 3357, 0));
		assertTrue(elite.contains(3191, 3367, 0));
		assertTrue(elite.contains(3185, 3352, 0));
		assertTrue(elite.contains(3197, 3366, 0));
	}

	private JsonObject movesFile() throws IOException
	{
		try (Reader reader = open(MoveTable.RESOURCE))
		{
			return gson.fromJson(reader, JsonObject.class);
		}
	}

	@Test
	public void everyMoveIsInRangeAndEveryPoolReferenceExists() throws IOException
	{
		JsonObject file = movesFile();
		Set<String> ids = new HashSet<>();
		for (JsonElement element : file.getAsJsonArray("moves"))
		{
			JsonObject move = element.getAsJsonObject();
			String id = move.get("id").getAsString();
			assertTrue("repeated move " + id, ids.add(id));
			double power = move.get("power").getAsDouble();
			double accuracy = move.get("accuracy").getAsDouble();
			assertTrue(id + ": power " + power, power > 0 && power <= 3.0);
			assertTrue(id + ": accuracy " + accuracy, accuracy >= 0.5 && accuracy <= 1.0);
			assertNotNull(id + ": style", CombatType.valueOf(move.get("style").getAsString()));
			assertTrue(id + ": name", move.get("name").getAsString().length() > 0);
			if (move.has("slot"))
				assertTrue(id + ": slot", move.get("slot").getAsString().matches("ATTACK|SECOND"));
		}

		assertTrue("expected at least 50 moves, got " + ids.size(), ids.size() >= 50);
		JsonObject pools = file.getAsJsonObject("pools");
		assertTrue("expected at least 20 pools, got " + pools.size(), pools.size() >= 20);
		for (String name : pools.keySet())
		{
			JsonArray learns = pools.getAsJsonObject(name).getAsJsonArray("learns");
			int last = 0;
			Set<String> inPool = new HashSet<>();
			for (JsonElement learn : learns)
			{
				JsonArray pair = learn.getAsJsonArray();
				String id = pair.get(0).getAsString();
				int level = pair.get(1).getAsInt();
				assertTrue(name + " names the unknown move " + id, ids.contains(id));
				assertTrue(name + " repeats " + id, inPool.add(id));
				assertTrue(name + ": levels must rise at " + id, level > last || (level == 1 && last == 0));
				assertTrue(name + ": level " + level, level >= 1 && level <= 99);
				last = level;
			}

			assertTrue(name + " teaches at most 12 moves", learns.size() >= 1 && learns.size() <= 12);
		}

		JsonObject defaults = file.getAsJsonObject("defaults");
		for (String style : defaults.keySet())
			assertTrue(style + " default pool exists", pools.has(defaults.get(style).getAsString()));

		JsonObject own = file.getAsJsonObject("species");
		for (String key : own.keySet())
			assertTrue(key + " has a pool that exists", pools.has(own.get(key).getAsString()));
	}

	@Test
	public void everyEffectInTheMoveTableNamesARealStatusAndAChanceBetweenZeroAndOne() throws IOException
	{
		int effects = 0;
		for (JsonElement element : movesFile().getAsJsonArray("moves"))
		{
			JsonObject move = element.getAsJsonObject();
			if (!move.has("effect"))
				continue;

			effects++;
			JsonObject effect = move.getAsJsonObject("effect");
			String id = move.get("id").getAsString();
			assertNotNull(id + ": the status is real", Status.named(effect.get("status").getAsString()));
			double chance = effect.get("chance").getAsDouble();
			assertTrue(id + ": chance " + chance, chance > 0.0 && chance <= 1.0);
		}

		assertTrue("expected a dozen moves with effects, got " + effects, effects >= 10);
	}

	@Test
	public void everySpeciesResolvesToAPoolThatStartsWithAPlainMoveAtLevelOne() throws IOException
	{
		MoveTable table;
		try (Reader reader = open(MoveTable.RESOURCE))
		{
			table = MoveTable.load(gson, reader);
		}

		for (Species one : species.all())
		{
			String pool = table.poolOf(one);
			assertNotNull(one.name() + " has no pool", pool);
			List<Move> first = table.movesAt(one, 1);
			assertEquals(one.name() + " knows one move at level 1", 1, first.size());
			assertEquals(one.name() + ": power", 1.0, first.get(0).power(), 0.0);
			assertEquals(one.name() + ": accuracy", 1.0, first.get(0).accuracy(), 0.0);
			assertEquals(one.name() + " knows four moves at level 99", 4, table.movesAt(one, 99).size());
		}

		for (String key : table.assignedSpecies())
			assertNotNull(key + " is not a species", species.find(key));

		for (String pool : table.poolNames())
			assertTrue(pool + " starts at level 1", table.describePool(pool).get(0).endsWith("@1"));
	}

	@Test
	public void dragonsAndWyvernsBreatheWithTheSecondAttack() throws IOException
	{
		MoveTable table;
		try (Reader reader = open(MoveTable.RESOURCE))
		{
			table = MoveTable.load(gson, reader);
		}

		List<Move> green = table.movesAt(species.find("Green dragon"), 40);
		assertEquals("Bite", green.get(0).name());
		assertTrue("Dragonfire is a breath", table.find("dragonfire").second());
		assertEquals(CombatType.MAGIC, table.find("dragonfire").style());
		assertTrue("Icy breath is icy", table.find("icy_breath").icy());
		assertEquals(4, green.size());
		assertEquals("Inferno", green.get(3).name());
		assertEquals("a baby dragon does not breathe", "lesser_dragon",
			table.poolOf(species.find("Baby green dragon")));
		assertEquals("dragon", table.poolOf(species.find("Vorkath")));
		assertEquals("wyvern", table.poolOf(species.find("Spitting Wyvern")));
		assertEquals("wizard", table.poolOf(species.find("Dark wizard")));
		assertEquals("default_melee", table.poolOf(species.find("Man")));
	}
}
