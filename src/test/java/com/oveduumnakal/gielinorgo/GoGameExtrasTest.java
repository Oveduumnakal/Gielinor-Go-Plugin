/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Plays the later features through the game service: gilded creatures, eggs, evolution and City Champions. */
public class GoGameExtrasTest
{
	private static final Species IMP = new Species("Imp", 7, CombatType.MELEE);
	private static final Species LESSER = new Species("Lesser demon", 94, CombatType.MELEE);
	private static final Species GREATER = new Species("Greater demon", 113, CombatType.MELEE);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.MELEE);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);

	private final List<String> messages = new ArrayList<>();
	private final List<Notice> notices = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(IMP, LESSER, GREATER, CHICKEN, DRAGON));
		EvolutionTable evolutions = new EvolutionTable(Arrays.asList(
			new EvolutionTable.Step("Imp", "Lesser demon", 20),
			new EvolutionTable.Step("Lesser demon", "Greater demon", 40)), table);
		Champion lumbridge = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250, Arrays.asList(
			new Champion.Member("Chicken", 2),
			new Champion.Member("Imp", 3),
			new Champion.Member("Chicken", 4)), JarType.IRON, 3);
		ChampionTable champions = new ChampionTable(Arrays.asList(lumbridge), table);
		game = new GoGame(table, evolutions, champions, new Random(5), () -> now,
			notice ->
			{
				messages.add(notice.text());
				notices.add(notice);
			});
	}

	@Test
	public void aGildedCatchIsRememberedOnTheCreatureAndInTheLog()
	{
		WildEntry gilded = new WildEntry(1, CHICKEN, 2, 1, false, true);
		game.data().addJars(JarType.BRONZE, 100);
		CatchOutcome outcome = CatchOutcome.BROKE_FREE;
		for (int i = 0; i < 100 && outcome != CatchOutcome.CAUGHT; i++)
			outcome = game.tryCatch(gilded);

		assertEquals(CatchOutcome.CAUGHT, outcome);
		assertTrue(lead().gilded);
		assertTrue(game.data().gilded.contains("chicken"));
		assertTrue(messages.contains("Caught gilded Chicken (Lv 2)!"));
		PanelState state = game.snapshot();
		CreatureView first = state.team().get(0);
		assertTrue(first.gilded());
		assertTrue(state.log().stream().anyMatch(entry -> entry.name().equals("Chicken") && entry.gilded()));
	}

	@Test
	public void aCreatureEvolvesWhenItReachesTheLevel()
	{
		game.data().team.add(new Creature("imp", 19, now));

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(20) - XpRules.xpForLevel(19)));

		assertEquals("lesser demon", lead().species);
		assertEquals(20, lead().level());
		assertTrue(messages.contains("Imp evolved into Lesser demon!"));
		assertEquals(1, (int) game.data().caught.get("lesser demon"));
		assertTrue(game.data().seen.contains("lesser demon"));
	}

	@Test
	public void evolutionKeepsTheNicknameAndUsesItInTheLine()
	{
		Creature imp = new Creature("imp", 19, now);
		game.data().team.add(imp);
		game.rename(imp, "Spark");

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(20) - XpRules.xpForLevel(19)));

		assertEquals("lesser demon", lead().species);
		assertEquals("Spark", lead().nickname);
		assertTrue(messages.contains("Spark evolved into Lesser demon!"));
		PanelState state = game.snapshot();
		CreatureView first = state.team().get(0);
		assertEquals("Spark", first.nickname());
	}

	@Test
	public void championBattleLinesUseTheNickname()
	{
		Creature dragon = new Creature("king black dragon", 40, now);
		game.data().team.add(dragon);
		game.rename(dragon, "Smaug");
		game.moveTo(3222, 3218, 0);

		TeamBattleResult fought = game.challenge("lumbridge");

		assertTrue(fought.lines().stream().anyMatch(line -> line.text().contains("Smaug")));
		assertTrue(fought.lines().stream().noneMatch(line -> line.text().contains("King Black Dragon")));
	}

	@Test
	public void aBigJumpEvolvesThroughEveryStage()
	{
		game.data().team.add(new Creature("imp", 1, now));

		game.addOsrsXp(10 * (int) XpRules.xpForLevel(45));

		assertEquals("greater demon", lead().species);
		assertTrue(messages.contains("Imp evolved into Lesser demon!"));
		assertTrue(messages.contains("Lesser demon evolved into Greater demon!"));
	}

	@Test
	public void belowTheLevelNothingEvolves()
	{
		game.data().team.add(new Creature("imp", 1, now));

		game.addOsrsXp(10 * (int) XpRules.xpForLevel(19));

		assertEquals("imp", lead().species);
	}

	@Test
	public void battleWinsSometimesDropAnEggUpToTheLimit()
	{
		for (int i = 0; i < 400; i++)
		{
			game.data().team.clear();
			game.data().team.add(new Creature("king black dragon", 40, now));
			game.battle(new WildEntry(1, CHICKEN, 2, 1));
		}

		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());
		assertEquals(Rarity.COMMON, firstEgg().rarityTier());
		assertEquals(Rarity.COMMON.eggTiles(), firstEgg().tilesLeft);
		assertTrue(messages.contains("+1 Common egg (battle win)"));
		PanelState state = game.snapshot();
		assertEquals(SaveData.MAX_EGGS, state.eggs().size());
	}

	@Test
	public void aRoamingWinDropsAnEggMoreOftenAndOneRarityHigher()
	{
		int roamingEggs = 0;
		int plainEggs = 0;
		for (int i = 0; i < 1000; i++)
		{
			game.data().team.clear();
			game.data().team.add(new Creature("king black dragon", 40, now));
			game.data().eggs.clear();
			game.battle(new WildEntry(1, CHICKEN, 12, 1, false, false, true));
			if (!game.data().eggs.isEmpty())
			{
				roamingEggs++;
				assertEquals(Rarity.UNCOMMON, firstEgg().rarityTier());
			}

			game.data().team.clear();
			game.data().team.add(new Creature("king black dragon", 40, now));
			game.data().eggs.clear();
			game.battle(new WildEntry(1, CHICKEN, 2, 1));
			if (!game.data().eggs.isEmpty())
				plainEggs++;
		}

		assertTrue("expected about 300 roaming eggs, got " + roamingEggs, roamingEggs > 240 && roamingEggs < 360);
		assertTrue("expected about 100 plain eggs, got " + plainEggs, plainEggs > 60 && plainEggs < 140);
	}

	@Test
	public void aRoamingLegendaryEggStaysLegendary()
	{
		assertEquals(Rarity.LEGENDARY, Rarity.LEGENDARY.next());
		assertEquals(Rarity.RARE, Rarity.UNCOMMON.next());
	}

	@Test
	public void anEggHatchesAfterEnoughWalking()
	{
		game.data().eggs.add(new Egg(Rarity.EPIC));
		int tiles = Rarity.EPIC.eggTiles();

		game.addSteps(tiles - 1);
		assertEquals(1, game.data().eggs.size());
		assertEquals(1, firstEgg().tilesLeft);
		assertEquals(0.999, firstEggProgress(), 0.001);

		game.addSteps(1);

		assertTrue(game.data().eggs.isEmpty());
		assertEquals(1, game.data().team.size());
		assertEquals("no other species is Epic in this test", "king black dragon", lead().species);
		assertEquals(1, lead().level());
		assertEquals(1, (int) game.data().caught.get("king black dragon"));
	}

	@Test
	public void everyHatchRollsItsOwnMakeUp()
	{
		Set<StatRolls> seen = new HashSet<>();
		for (int i = 0; i < 30; i++)
		{
			game.data().eggs.clear();
			game.data().eggs.add(new Egg(Rarity.EPIC));
			game.addSteps(Rarity.EPIC.eggTiles());
			Creature born = newest();

			assertTrue(born.rolls().valid());
			seen.add(born.rolls());
		}

		assertTrue("30 hatches should not all roll alike, got " + seen.size(), seen.size() > 10);
	}

	@Test
	public void evolutionKeepsTheRollsAndTemperament()
	{
		Creature imp = new Creature("imp", 19, now);
		imp.setRolls(new StatRolls(5, -2, 3, 1, Temperament.FIERCE));
		game.data().team.add(imp);

		game.addOsrsXp(10 * (int) (XpRules.xpForLevel(20) - XpRules.xpForLevel(19)));

		assertEquals("lesser demon", lead().species);
		assertEquals(new StatRolls(5, -2, 3, 1, Temperament.FIERCE), lead().rolls());
		assertEquals("FIERCE", lead().temperament);
	}

	@Test
	public void aSavedCreatureWithRollsOutOfRangeIsPulledBackIn()
	{
		SaveData data = SaveData.fresh();
		Creature odd = new Creature("imp", 5, now);
		odd.rollHp = 40;
		odd.rollSpeed = -40;
		odd.temperament = "GRUMPY";
		data.team.add(odd);

		data.sanitized();

		assertEquals(StatRolls.MAX, odd.rollHp);
		assertEquals(StatRolls.MIN, odd.rollSpeed);
		assertNull(odd.temperament);
		assertTrue(odd.rolls().valid());
	}

	@Test
	public void aReadyEggWaitsForRoom()
	{
		for (int i = 0; i < SaveData.TEAM_SIZE; i++)
			game.data().team.add(new Creature("chicken", 1, now));

		for (int i = 0; i < SaveData.STORAGE_SIZE; i++)
			game.data().storage.add(new Creature("chicken", 1, now));

		game.data().eggs.add(new Egg(Rarity.COMMON));

		game.addSteps(Rarity.COMMON.eggTiles());
		game.addSteps(10);

		assertEquals(1, game.data().eggs.size());
		assertEquals(1, Collections.frequency(messages, "An egg is ready, but storage is full"));

		game.release(0);
		game.addSteps(1);

		assertTrue(game.data().eggs.isEmpty());
		assertEquals(SaveData.STORAGE_SIZE, game.data().storage.size());
	}

	@Test
	public void theChampionCanOnlyBeChallengedInTheCity()
	{
		game.data().team.add(new Creature("king black dragon", 40, now));

		assertFalse("nothing changes while the player stays outside", game.moveTo(3100, 3100, 0));
		assertNull(game.challenge("lumbridge"));
		assertTrue(messages.contains("Go to Lumbridge to challenge its champion"));
		assertFalse(champion().here());

		assertTrue(game.moveTo(3222, 3218, 0));
		assertTrue(champion().here());
		assertFalse("nothing changes while the player stays inside", game.moveTo(3223, 3218, 0));
		assertNull(game.challenge("no-such-city"));
		assertNotNull(game.challenge("lumbridge"));
	}

	@Test
	public void beatingAChampionEarnsTheSigilAndTheRewardOnce()
	{
		game.data().team.add(new Creature("king black dragon", 40, now));
		game.moveTo(3222, 3218, 0);
		long xpBefore = lead().xp;

		TeamBattleResult first = game.challenge("lumbridge");

		assertTrue(first.won());
		assertEquals("an opening, one line per knockout, and a result", 5, first.lines().size());
		assertTrue(game.data().sigils.contains("lumbridge"));
		assertEquals(3, game.data().jarCount(JarType.IRON));
		assertEquals(xpBefore + 105, lead().xp);
		assertTrue(messages.contains("Sigil of Lumbridge earned!"));
		assertTrue(messages.contains("+3 Iron jars (champion)"));
		assertEquals(JarType.IRON, noticeOf("+3 Iron jars (champion)").jar());
		ChampionView view = champion();
		assertTrue(view.beaten());
		assertEquals("Chicken 2, Imp 3, Chicken 4", view.team());
		assertEquals("3 Iron jars", view.reward());

		now += Creature.FULL_REST_MS;
		long xpAfterFirst = lead().xp;
		TeamBattleResult rematch = game.challenge("lumbridge");

		assertTrue(rematch.won());
		assertEquals("a rematch gives no more jars", 3, game.data().jarCount(JarType.IRON));
		assertEquals("a rematch gives half the XP", xpAfterFirst + 52, lead().xp);
	}

	@Test
	public void losingToAChampionEarnsNothingAndTiresTheTeam()
	{
		Species weak = CHICKEN;
		game = new GoGame(new SpeciesTable(Arrays.asList(weak, DRAGON)), EvolutionTable.empty(),
			new ChampionTable(Arrays.asList(new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250,
				Arrays.asList(new Champion.Member("King Black Dragon", 60)), JarType.IRON, 3)),
				new SpeciesTable(Arrays.asList(weak, DRAGON))),
			new Random(5), () -> now, notice -> messages.add(notice.text()));
		game.data().team.add(new Creature("chicken", 1, now));
		game.data().team.add(new Creature("chicken", 1, now));
		game.moveTo(3222, 3218, 0);

		TeamBattleResult result = game.challenge("lumbridge");

		assertFalse(result.won());
		assertTrue(game.data().sigils.isEmpty());
		assertEquals(0, game.data().jarCount(JarType.IRON));
		assertEquals(0.0, healthOf(0), 0);
		assertEquals(0.0, healthOf(1), 0);
		assertNull("a team with no health cannot challenge", game.challenge("lumbridge"));
		assertTrue(messages.contains("Your team is too tired"));
	}

	private Notice noticeOf(String text)
	{
		for (Notice notice : notices)
		{
			if (notice.text().equals(text))
				return notice;
		}

		throw new AssertionError("no notice said: " + text);
	}

	private Creature lead()
	{
		return game.data().lead();
	}

	private Creature newest()
	{
		List<Creature> stored = game.data().storage;
		List<Creature> team = game.data().team;
		return stored.isEmpty() ? team.get(team.size() - 1) : stored.get(stored.size() - 1);
	}

	private Egg firstEgg()
	{
		return game.data().eggs.get(0);
	}

	private double firstEggProgress()
	{
		PanelState state = game.snapshot();
		EggView egg = state.eggs().get(0);
		return egg.progress();
	}

	private ChampionView champion()
	{
		PanelState state = game.snapshot();
		return state.champions().get(0);
	}

	private double healthOf(int teamIndex)
	{
		Creature creature = game.data().team.get(teamIndex);
		return creature.hpFractionAt(now);
	}

	@Test
	public void teamFightsCarryHealthFromOneFoeToTheNext()
	{
		List<Combatant> mine = Arrays.asList(new Combatant(DRAGON, 30, 1.0));
		List<Combatant> theirs = Arrays.asList(
			new Combatant(CHICKEN, 30, 1.0), new Combatant(CHICKEN, 30, 1.0), new Combatant(CHICKEN, 30, 1.0));

		TeamBattleResult result = BattleSim.teamFight(mine, theirs, new Random(1));

		assertTrue(result.won());
		assertEquals(3, result.lines().size());
		assertTrue("the winner took some damage along the way", mine.get(0).hpFraction() < 1.0);
		assertTrue(theirs.stream().allMatch(Combatant::fainted));
	}
}
