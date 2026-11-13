/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.StringReader;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Plays the Elite challenge through the game service and the run itself, with a fake clock and fixed seeds. */
public class EliteRunTest
{
	private static final long HOUR = 3_600_000L;

	private final List<String> messages = new ArrayList<>();
	private SpeciesTable species;
	private EliteTable weak;
	private EliteTable strong;
	private GoGame game;
	private long now = 1_000_000L;

	private static String json(String species, int level)
	{
		StringBuilder stages = new StringBuilder();
		for (int i = 1; i <= 5; i++)
		{
			stages.append(i > 1 ? "," : "").append("{\"id\":\"e").append(i).append("\",\"city\":\"Elite ").append(i)
				.append("\",\"team\":[{\"species\":\"").append(species).append("\",\"level\":").append(level)
				.append("}]}");
		}

		return "{\"minX\":3180,\"minY\":3346,\"maxX\":3202,\"maxY\":3372,\"firstDragonJars\":10,"
			+ "\"firstSpecialtyJars\":3,\"firstEgg\":\"LEGENDARY\",\"laterDragonJars\":2,\"stages\":[" + stages + "]}";
	}

	@Before
	public void setUp()
	{
		species = new SpeciesTable(Arrays.asList(new Species("Mite", 1, CombatType.STAB),
			new Species("Brute", 200, CombatType.STAB), new Species("Titan", 400, CombatType.STAB)));
		weak = EliteTable.load(new Gson(), new StringReader(json("Mite", 70)), species);
		strong = EliteTable.load(new Gson(), new StringReader(json("Titan", 99)), species);
		game = new GoGame(species, new Random(7), () -> now, notice -> messages.add(notice.text()));
		game.elite(weak);
		game.data().team.add(new Creature("brute", 60, now));
		for (int i = 0; i < 8; i++)
			game.data().sigils.add("city-" + i);

		game.moveTo(3191, 3367, 0);
	}

	private EliteView view()
	{
		return game.snapshot().elite();
	}

	private Rarity eggRarity()
	{
		Egg egg = game.data().eggs.get(0);
		return egg.rarityTier();
	}

	private static String firstLine(TeamBattleResult result)
	{
		BattleLine line = result.lines().get(0);
		return line.text();
	}

	private Creature lead()
	{
		return game.data().lead();
	}

	private boolean said(String text)
	{
		return messages.stream().anyMatch(line -> line.contains(text));
	}

	@Test
	public void theTableLoadsFiveTeamsOrNothing()
	{
		assertTrue(weak.available());
		assertEquals(5, weak.stages().size());
		assertFalse(EliteTable.load(new Gson(), new StringReader(json("Nobody", 5)), species).available());
		assertFalse(EliteTable.load(new Gson(), new StringReader("{not json"), species).available());
		assertFalse(EliteTable.empty().available());
	}

	@Test
	public void itIsLockedBelowEightSigils()
	{
		game.data().sigils.remove("city-0");

		assertNull(game.eliteChallenge());

		assertTrue(said("Earn all 8 sigils first (7 / 8)"));
		assertEquals(EliteView.Mode.LOCKED, view().mode());
		assertFalse(game.eliteOpen());
	}

	@Test
	public void itIsRefusedAwayFromTheGuild()
	{
		game.moveTo(3200, 3200, 0);
		assertEquals(EliteView.Mode.AWAY, view().mode());

		assertNull(game.eliteChallenge());

		assertTrue(said("Go to the Champions' Guild"));
		assertFalse(game.eliteOpen());
	}

	@Test
	public void theGuildRectangleCoversTheGroundOutsideItsDoorOnlyOnThePlaneOfTheSurface()
	{
		assertTrue(weak.contains(3191, 3367, 0));
		assertTrue(weak.contains(3191, 3371, 0));
		assertFalse(weak.contains(3191, 3367, 1));
		assertFalse(weak.contains(3191, 3380, 0));
	}

	@Test
	public void itIsRefusedWithAFullEggBag()
	{
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			game.data().eggs.add(new Egg(Rarity.COMMON));

		assertNull(game.eliteChallenge());

		assertTrue(said("Free an egg slot first"));
		assertFalse(game.eliteOpen());
	}

	@Test
	public void itIsRefusedWithATiredTeam()
	{
		lead().hpFraction = 0.1;
		lead().hpStampMs = now;

		assertNull(game.eliteChallenge());

		assertTrue(said("Your team is too tired"));
	}

	@Test
	public void healthCarriesFromOneBattleToTheNextAndNothingRests()
	{
		TeamBattleResult first = game.eliteChallenge();

		assertNotNull(first);
		assertTrue(first.won());
		assertTrue(game.eliteOpen());
		double after = lead().hpFraction;
		assertTrue("the battle should cost health, left " + after, after < 1.0);
		now += 10 * HOUR;
		EliteView view = view();
		assertEquals(EliteView.Mode.RUNNING, view.mode());
		assertEquals(2, view.stage());
		assertEquals(after, lead().hpFractionAt(now), 0.0);
		TeamBattleResult second = game.eliteContinue();
		assertTrue(second.won());
		assertTrue(lead().hpFraction < after);
	}

	@Test
	public void restingStartsAgainWhenTheRunEnds()
	{
		game.eliteChallenge();
		double after = lead().hpFraction;

		game.eliteForfeit();
		now += HOUR;

		assertFalse(game.eliteOpen());
		assertTrue(lead().hpFractionAt(now) > after);
	}

	@Test
	public void aLossEndsTheRunAndTheNextAttemptStartsAtTheFirstTrainer()
	{
		game.elite(strong);
		TeamBattleResult lost = game.eliteChallenge();
		assertNotNull(lost);
		assertFalse(lost.won());
		assertFalse(game.eliteOpen());
		assertEquals(0, game.data().eliteClears);
		assertTrue(lost.lines().stream().anyMatch(line -> line.text().contains("The Elite challenge is over")));

		lead().hpFraction = 1.0;
		TeamBattleResult again = game.eliteChallenge();

		assertTrue(firstLine(again).startsWith("Elite challenge 1 of 5"));
	}

	@Test
	public void fiveWinsPayTheFirstClearOnceAndTheSmallerRewardAfter()
	{
		clearOnce();

		assertEquals(1, game.data().eliteClears);
		assertEquals(10, game.data().jarCount(JarType.DRAGON));
		for (JarType type : JarType.values())
		{
			if (type.specialty())
				assertEquals(type.name(), 3, game.data().jarCount(type));
		}

		assertEquals(1, game.data().eggs.size());
		assertEquals(Rarity.LEGENDARY, eggRarity());
		assertTrue(view().titled());
		assertTrue(said("Grand Champion"));
		assertFalse(game.eliteOpen());

		clearOnce();

		assertEquals(2, game.data().eliteClears);
		assertEquals(12, game.data().jarCount(JarType.DRAGON));
		assertEquals(1, game.data().eggs.size());
		assertEquals(3, game.data().jarCount(JarType.CAVE));
	}

	private void clearOnce()
	{
		lead().hpFraction = 1.0;
		lead().hpStampMs = now;
		assertNotNull(game.eliteChallenge());
		for (int i = 0; i < 4; i++)
		{
			lead().hpFraction = 1.0;
			assertTrue(game.eliteContinue().won());
		}
	}

	@Test
	public void teamChangesAndOtherBattlesAreRefusedDuringARun()
	{
		game.data().storage.add(new Creature("mite", 5, now));
		game.eliteChallenge();
		assertTrue(game.eliteOpen());

		game.moveToTeam(0);
		game.moveToStorage(0);

		assertEquals(1, game.data().team.size());
		assertEquals(1, game.data().storage.size());
		assertTrue(said("Finish or give up the Elite challenge first"));
		assertNull(game.challenge("anyone"));
		assertNull(game.battle(new WildEntry(1, species.find("Mite"), 3, 3)));
	}

	@Test
	public void loggingInAgainGivesTheRunUp()
	{
		game.eliteChallenge();

		game.load(game.data());

		assertFalse(game.eliteOpen());
	}

	@Test
	public void aBattleWonPaysHalfTheCreatureXpAfterTheFirstClear()
	{
		long before = lead().xp;
		game.eliteChallenge();
		long firstGain = lead().xp - before;
		game.eliteForfeit();
		game.data().eliteClears = 1;
		lead().hpFraction = 1.0;
		before = lead().xp;

		game.eliteChallenge();

		long laterGain = lead().xp - before;
		assertTrue(laterGain > 0);
		assertTrue(laterGain < firstGain);
	}

	@Test
	public void aManualFightCanBePluggedIn()
	{
		List<Boolean> results = new ArrayList<>(Arrays.asList(true, true, false));
		List<Integer> mineSeen = new ArrayList<>();
		EliteRun run = new EliteRun(weak, species, (mine, theirs, random) ->
		{
			mineSeen.add(theirs.size());
			return new TeamBattleResult(results.remove(0), Collections.emptyList());
		});

		assertEquals(0, run.stage());
		assertNotNull(run.fight(Collections.emptyList(), new Random(1)));
		assertEquals(1, run.stage());
		assertNotNull(run.fight(Collections.emptyList(), new Random(1)));
		assertEquals(2, run.stage());
		run.fight(Collections.emptyList(), new Random(1));

		assertEquals(EliteRun.State.LOST, run.state());
		assertEquals(0, run.stage());
		assertNull(run.fight(Collections.emptyList(), new Random(1)));
		assertEquals(Arrays.asList(1, 1, 1), mineSeen);
	}

	@Test
	public void theCardFitsThePanelInEveryMode()
	{
		PanelActions none = null;
		for (EliteView.Mode mode : EliteView.Mode.values())
		{
			if (mode == EliteView.Mode.HIDDEN)
				continue;

			EliteView view = new EliteView(mode, 5, mode == EliteView.Mode.RUNNING ? 3 : 0, 5, "Elite of Might",
				"Dagannoth Rex 80, Sarachnis 81, Cerberus 83, Skotizo 84, Abyssal Sire 85",
				"10 Dragon jars, 3 of every specialty jar, a Legendary egg", mode == EliteView.Mode.HERE);
			EliteCard card = new EliteCard(view, none);
			card.setSize(new Dimension(209, 400));
			card.doLayout();
			card.setSize(new Dimension(209, card.getPreferredSize().height));
			card.doLayout();
			BufferedImage image = new BufferedImage(209, card.getHeight(), BufferedImage.TYPE_INT_ARGB);
			card.printAll(image.getGraphics());

			assertTrue(mode + " is too wide", card.getPreferredSize().width <= 209);
		}
	}
}
