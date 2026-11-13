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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the Creature Log rewards at 750 and 1,000 species, the complete Log and the two titles. */
public class LogRewardsTest
{
	private static final int CATCHABLE = 1010;

	private Milestones milestones;
	private GoGame game;

	@Before
	public void setUp()
	{
		List<Species> all = new ArrayList<>();
		for (int i = 0; i < CATCHABLE; i++)
			all.add(new Species(String.format("Beast %04d", i), 1 + i, CombatType.MELEE));

		all.add(new Species("Quest Beast", 5, CombatType.MELEE).asQuestOnly());
		SpeciesTable table = new SpeciesTable(all);
		milestones = new Milestones(table, Collections.emptyList());
		game = new GoGame(table, new Random(3), () -> 0L, notice ->
		{
		});
		game.milestones(milestones);
	}

	private static SaveData saveWithCaught(int count, String... paid)
	{
		SaveData data = SaveData.fresh();
		for (int i = 0; i < count; i++)
			data.caught.put(String.format("beast %04d", i), 1);

		data.milestones.addAll(Arrays.asList(paid));
		return data;
	}

	@Test
	public void theNewStepsAreReachedAtTheirCount()
	{
		assertFalse(milestones.reached(saveWithCaught(749)).contains("species-750"));
		assertTrue(milestones.reached(saveWithCaught(750)).contains("species-750"));
		assertFalse(milestones.reached(saveWithCaught(999)).contains("species-1000"));
		assertTrue(milestones.reached(saveWithCaught(1000)).contains("species-1000"));
		assertEquals("10 Dragon jars + Epic egg", milestones.rewardFor("species-750").text());
		assertEquals("3 of every specialty jar + Legendary egg", milestones.rewardFor("species-1000").text());
		assertEquals("1,000 species", milestones.byId("species-1000").label());
	}

	@Test
	public void theCompleteLogNeedsEveryCatchableSpeciesAndIgnoresQuestOnlyOnes()
	{
		assertFalse(milestones.reached(saveWithCaught(CATCHABLE - 1)).contains(Milestones.COMPLETE_ID));
		SaveData full = saveWithCaught(CATCHABLE);

		assertTrue(milestones.reached(full).contains(Milestones.COMPLETE_ID));
		assertEquals(CATCHABLE, milestones.byId(Milestones.COMPLETE_ID).target());
		assertTrue(milestones.rewardFor(Milestones.COMPLETE_ID).title());
		assertEquals(Rarity.LEGENDARY, milestones.rewardFor(Milestones.COMPLETE_ID).egg());
		SaveData questOnly = saveWithCaught(CATCHABLE - 1);
		questOnly.caught.put("quest beast", 1);

		assertFalse(milestones.reached(questOnly).contains(Milestones.COMPLETE_ID));
	}

	@Test
	public void theNextGoalFollowsTheLogToItsEnd()
	{
		assertEquals("Next: 1,000 species", milestones.progress(saveWithCaught(987)).nextLabel());
		assertEquals(1000, milestones.progress(saveWithCaught(987)).nextTarget());
		assertEquals("Next: complete Log", milestones.progress(saveWithCaught(1000)).nextLabel());
		assertEquals(CATCHABLE, milestones.progress(saveWithCaught(1000)).nextTarget());
		SaveData done = saveWithCaught(CATCHABLE, Milestones.COMPLETE_ID);

		assertNull(milestones.progress(done).nextLabel());
	}

	@Test
	public void the750StepPaysOnceWithTenDragonJarsAndAnEpicEgg()
	{
		SaveData data = saveWithCaught(750, "species-25", "species-50", "species-100", "species-250", "species-500");
		int dragon = data.jarCount(JarType.DRAGON);
		game.load(data);

		assertTrue(game.data().milestones.contains("species-750"));
		assertEquals(dragon + 10, game.data().jarCount(JarType.DRAGON));
		assertEquals(1, game.data().eggs.size());
		Egg epic = game.data().eggs.get(0);
		assertEquals(Rarity.EPIC, epic.rarityTier());
		game.load(game.data());

		assertEquals(dragon + 10, game.data().jarCount(JarType.DRAGON));
		assertEquals(1, game.data().eggs.size());
	}

	@Test
	public void the1000StepPaysThreeOfEverySpecialtyJar()
	{
		SaveData data = saveWithCaught(1000, "species-25", "species-50", "species-100", "species-250", "species-500",
			"species-750");
		game.load(data);

		assertTrue(game.data().milestones.contains("species-1000"));
		for (JarType type : JarType.values())
		{
			if (type.specialty())
				assertEquals(3, game.data().jarCount(type));
		}

		Egg legendary = game.data().eggs.get(0);
		assertEquals(Rarity.LEGENDARY, legendary.rarityTier());
	}

	@Test
	public void completingTheLogEarnsTheTitleAndKeepsItWhenSpeciesAreAdded()
	{
		SaveData data = saveWithCaught(CATCHABLE, "species-25", "species-50", "species-100", "species-250",
			"species-500", "species-750", "species-1000");
		game.load(data);

		assertEquals(Arrays.asList(GoGame.TITLE_MASTER_COLLECTOR), game.titles());
		assertEquals(GoGame.TITLE_MASTER_COLLECTOR, game.snapshot().shownTitle());
		game.data().caught.remove("beast 0005");

		assertEquals(Arrays.asList(GoGame.TITLE_MASTER_COLLECTOR), game.titles());
	}

	@Test
	public void bothTitlesAreListedInOrderAndThePickedOneShows()
	{
		game.load(saveWithCaught(0));
		game.data().eliteClears = 1;
		game.data().milestones.add(Milestones.COMPLETE_ID);

		assertEquals(Arrays.asList(GoGame.TITLE_GRAND_CHAMPION, GoGame.TITLE_MASTER_COLLECTOR), game.titles());
		assertEquals(GoGame.TITLE_MASTER_COLLECTOR, game.shownTitle());
		game.pickTitle(GoGame.TITLE_GRAND_CHAMPION);

		PanelState state = game.snapshot();
		assertEquals(GoGame.TITLE_GRAND_CHAMPION, state.shownTitle());
		assertEquals(2, state.titles().size());
		game.pickTitle("NOT_A_TITLE");

		assertEquals(GoGame.TITLE_GRAND_CHAMPION, game.data().title);
	}

	@Test
	public void theChoiceSurvivesASaveAndALoadAndAnOldSaveHasNone()
	{
		SaveData data = SaveData.fresh();
		data.eliteClears = 1;
		data.milestones.add(Milestones.COMPLETE_ID);
		data.title = GoGame.TITLE_GRAND_CHAMPION;
		Gson gson = new Gson();
		SaveData loaded = gson.fromJson(gson.toJson(data), SaveData.class).sanitized();
		game.load(loaded);

		assertEquals(GoGame.TITLE_GRAND_CHAMPION, game.shownTitle());
		SaveData old = gson.fromJson("{}", SaveData.class).sanitized();

		assertNull(old.title);
	}

	@Test
	public void aPlayerWithNoTitleShowsNone()
	{
		game.load(saveWithCaught(0));

		assertTrue(game.titles().isEmpty());
		assertNull(game.shownTitle());
	}
}
