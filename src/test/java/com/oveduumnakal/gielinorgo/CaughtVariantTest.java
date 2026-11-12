/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** A caught creature remembers the NPC variant it was caught as, and everything that draws it uses that id. */
public class CaughtVariantTest
{
	private static final Gson GSON = new Gson();
	private static final Species GUARD = species("Monkey Guard", 167, "[5275,5276,5271]");
	private static final Species IMP = species("Imp", 7, "[100,101,102]");
	private static final Species LESSER = species("Lesser demon", 82, "[200,201]");
	private static final Species NO_IDS = new Species("Mystery", 5, CombatType.MELEE);

	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GUARD, IMP, LESSER, NO_IDS));
		EvolutionTable evolutions = new EvolutionTable(
			Collections.singletonList(new EvolutionTable.Step("Imp", "Lesser demon", 20)), table);
		game = new GoGame(table, evolutions, ChampionTable.empty(), new Random(5), () -> 1_000_000L, notice ->
		{
		});
		game.data().addJars(JarType.BRONZE, 200);
	}

	@Test
	public void aSavedIdThatTheSpeciesHasIsUsed()
	{
		assertEquals(5271, GUARD.npcIdFor(5271));
		assertEquals(5276, GUARD.npcIdFor(5276));
	}

	@Test
	public void anUnknownIdFallsBackToTheFirstId()
	{
		assertEquals(5275, GUARD.npcIdFor(9999));
		assertEquals(5275, GUARD.npcIdFor(-4));
	}

	@Test
	public void noSavedIdIsTheFirstId()
	{
		assertEquals(5275, GUARD.npcIdFor(0));
		assertEquals(0, NO_IDS.npcIdFor(5271));
	}

	@Test
	public void aCatchStoresTheIdOfTheNpcThatWasCaught()
	{
		WildEntry small = new WildEntry(1, GUARD, 5, 100).withNpcId(5271);

		assertEquals(CatchOutcome.CAUGHT, catchUntilCaught(small));

		assertEquals(5271, only().npcId);
		assertEquals(5271, GUARD.npcIdFor(only().npcId));
	}

	@Test
	public void aCatchFromAnEntryWithNoIdStoresZero()
	{
		assertEquals(CatchOutcome.CAUGHT, catchUntilCaught(new WildEntry(1, GUARD, 5, 100)));

		assertEquals(0, only().npcId);
		assertEquals(5275, GUARD.npcIdFor(only().npcId));
	}

	@Test
	public void theTrackerGivesItsEntryTheMonstersId()
	{
		WildTracker tracker = new WildTracker(new Random(1));

		WildEntry entry = tracker.offer(7, 5271, GUARD, 100, false, 1_000_000L, 1.0, 500, false);

		assertEquals(5271, entry.npcId());
	}

	@Test
	public void anOldSaveLoadsWithZeroAndLooksAsBefore()
	{
		String json = "{\"team\":[{\"species\":\"monkey guard\",\"xp\":10,\"hpFraction\":1.0}]}";

		SaveData loaded = GSON.fromJson(json, SaveData.class).sanitized();

		Creature creature = loaded.team.get(0);
		assertEquals(0, creature.npcId);
		assertEquals(5275, GUARD.npcIdFor(creature.npcId));
	}

	@Test
	public void aDamagedNegativeIdIsCleaned()
	{
		SaveData loaded = GSON.fromJson("{\"team\":[{\"species\":\"imp\",\"npcId\":-3}]}", SaveData.class)
			.sanitized();

		assertEquals(0, loaded.team.get(0).npcId);
	}

	@Test
	public void theIdSurvivesASaveAndLoad()
	{
		Creature creature = new Creature("monkey guard", 5, 1L);
		creature.npcId = 5271;
		SaveData data = SaveData.fresh();
		data.team.add(creature);

		SaveData back = GSON.fromJson(GSON.toJson(data), SaveData.class).sanitized();

		assertEquals(5271, back.team.get(0).npcId);
	}

	@Test
	public void evolvingClearsTheId()
	{
		Creature imp = new Creature("imp", 19, 1_000_000L);
		imp.npcId = 101;
		game.data().team.add(imp);

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(20) - XpRules.xpForLevel(19)));

		Creature grown = game.data().team.get(0);
		assertEquals("lesser demon", grown.species);
		assertEquals(0, grown.npcId);
		assertEquals(200, LESSER.npcIdFor(grown.npcId));
	}

	@Test
	public void thePanelViewCarriesTheResolvedId()
	{
		Creature small = new Creature("monkey guard", 5, 1_000_000L);
		small.npcId = 5271;
		Creature old = new Creature("monkey guard", 5, 1_000_000L);
		game.data().team.add(small);
		game.data().team.add(old);

		List<CreatureView> team = game.snapshot().team();

		assertEquals(5271, team.get(0).npcId());
		assertEquals(5275, team.get(1).npcId());
		assertNotEquals(team.get(0), team.get(1));
	}

	@Test
	public void aCombatantAndItsDuelCarryTheId()
	{
		Combatant small = new Combatant(GUARD, 9, 1.0).npc(5271);
		Combatant plain = new Combatant(GUARD, 9, 1.0);
		Combatant stale = new Combatant(GUARD, 9, 1.0).npc(4);

		assertEquals(5271, small.npcId());
		assertEquals(0, plain.npcId());
		assertEquals(5275, stale.npcId());
		Duel duel = new Duel(small, 0, new Combatant(IMP, 9, 1.0), 0, new ArrayList<>(),
			new BattleLine("Won", Tone.GOOD, true), true);
		assertEquals(5271, duel.mineNpcId());
		TeamScript script = TeamScript.of(Collections.singletonList(duel), false);
		TeamScript.Entry mine = script.entries().get(1);
		assertEquals(5271, mine.npcId());
		assertEquals(5271, TeamScript.enterMine("monkey guard", false, 5271).npcId());
		assertEquals(0, TeamScript.enterMine("monkey guard", false).npcId());
	}

	@Test
	public void twoVariantsOfOneSpeciesHaveTwoPortraitKeys()
	{
		String small = PortraitSource.cacheKey("Monkey Guard", 5271);
		String big = PortraitSource.cacheKey("Monkey Guard", 5275);

		assertNotEquals(small, big);
		assertEquals("monkey guard#5271", small);
		assertEquals("monkey guard", PortraitSource.cacheKey("Monkey Guard", 0));
		assertEquals(PortraitSource.cacheKey("monkey guard", 5271), small);
	}

	@Test
	public void thePortraitTriesTheCaughtVariantFirstThenTheRest()
	{
		assertArrayEquals(new int[] {5271, 5275, 5276}, PortraitService.tryOrder(GUARD, 5271));
		assertArrayEquals(new int[] {5275, 5276, 5271}, PortraitService.tryOrder(GUARD, 0));
		assertArrayEquals(new int[] {5275, 5276, 5271}, PortraitService.tryOrder(GUARD, 12));
		assertEquals(0, PortraitService.tryOrder(NO_IDS, 5).length);
	}

	private Creature only()
	{
		List<Creature> all = new ArrayList<>(game.data().team);
		all.addAll(game.data().storage);
		assertEquals(1, all.size());
		return all.get(0);
	}

	private CatchOutcome catchUntilCaught(WildEntry entry)
	{
		CatchOutcome outcome = game.tryCatch(entry);
		for (int i = 0; i < 100 && outcome != CatchOutcome.CAUGHT; i++)
			outcome = game.tryCatch(entry);

		assertTrue(outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED);
		return outcome;
	}

	private static Species species(String name, int level, String ids)
	{
		return GSON.fromJson("{\"name\":\"" + name + "\",\"level\":" + level + ",\"style\":\"MELEE\",\"ids\":" + ids
			+ "}", Species.class);
	}
}
