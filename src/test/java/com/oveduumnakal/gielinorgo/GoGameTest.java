/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Plays the game through its service class with a fixed seed and a fake clock. */
public class GoGameTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.MELEE);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);

	private final List<String> messages = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;
	private final List<Notice> notices = new ArrayList<>();

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, CHICKEN, DRAGON));
		game = new GoGame(table, new Random(11), () -> now, this::record);
		game.setBankNear(true);
	}

	@Test
	public void aNewPlayerStartsWithBronzeJarsAndNoCreatures()
	{
		PanelState state = game.snapshot();

		assertEquals(SaveData.STARTING_JARS, (int) state.jars().get(JarType.BRONZE));
		assertEquals(0, (int) state.jars().get(JarType.DRAGON));
		assertTrue(state.team().isEmpty());
		assertEquals(1, state.trainerLevel());
		assertEquals(3, state.totalSpecies());
		assertEquals(JarType.BRONZE, game.jarToThrow());
	}

	@Test
	public void catchingSpendsAJarAndAddsTheCreature()
	{
		CatchOutcome outcome = catchUntilCaught(wild(CHICKEN, 3, 1));

		assertEquals(CatchOutcome.CAUGHT, outcome);
		assertEquals(1, game.data().team.size());
		assertEquals("chicken", lead().species);
		assertEquals(3, lead().level());
		assertEquals(1, (int) game.data().caught.get("chicken"));
		assertTrue(game.data().jarCount(JarType.BRONZE) < SaveData.STARTING_JARS + GoGame.STREAK_JARS);
		assertTrue(game.data().trainerXp > 0);
		assertTrue(messages.contains("Caught Chicken (Lv 3)!"));
		assertEquals("creature news floats over the creature", 1, noticeFor("Caught Chicken (Lv 3)!").wildId());
		assertTrue(game.takeDirty());
		assertFalse(game.takeDirty());
	}

	@Test
	public void aKnockedOutCreatureIsCaughtOnARollThatFailsOnceItIsBackUp()
	{
		double roll = new Random(11).nextDouble();
		double standing = CatchRules.chance(JarType.BRONZE, 90, true, CHICKEN.traits());
		double down = CatchRules.chance(JarType.BRONZE, 90, true, CHICKEN.traits(), true);
		assertTrue("the first roll fails at the weakened odds", roll >= standing);
		assertTrue("and succeeds at the knocked-out odds", roll < down);

		WildEntry beaten = wild(CHICKEN, 3, 90);
		beaten.markBattled(true);

		assertEquals(CatchOutcome.CAUGHT, game.tryCatch(beaten, JarType.BRONZE, true));
		assertEquals(1, game.data().team.size());
	}

	@Test
	public void theSameRollFailsOnABeatenCreatureThatHasGotUp()
	{
		WildEntry beaten = wild(CHICKEN, 3, 90);
		beaten.markBattled(true);

		CatchOutcome outcome = game.tryCatch(beaten, JarType.BRONZE);

		assertTrue(outcome == CatchOutcome.BROKE_FREE || outcome == CatchOutcome.FLED);
		assertTrue(game.data().team.isEmpty());
	}

	@Test
	public void noJarsMeansNoThrow()
	{
		game.data().addJars(JarType.BRONZE, -SaveData.STARTING_JARS);

		assertNull(game.jarToThrow());
		assertEquals(CatchOutcome.NO_JARS, game.tryCatch(wild(CHICKEN, 3, 1)));
		assertTrue(game.data().team.isEmpty());
	}

	@Test
	public void theSelectedJarIsThrownWhileItLasts()
	{
		game.data().addJars(JarType.RUNE, 1);
		game.selectJar(JarType.RUNE);

		assertEquals(JarType.RUNE, game.jarToThrow());

		game.data().addJars(JarType.RUNE, -1);

		assertEquals(JarType.BRONZE, game.jarToThrow());
	}

	@Test
	public void theSeventhCatchGoesToStorage()
	{
		for (int i = 0; i < SaveData.TEAM_SIZE + 1; i++)
		{
			game.data().addJars(JarType.BRONZE, 50);
			catchUntilCaught(wild(CHICKEN, 2, 1));
		}

		assertEquals(SaveData.TEAM_SIZE, game.data().team.size());
		assertEquals(1, game.data().storage.size());
	}

	@Test
	public void aFullCollectionRefusesTheThrow()
	{
		for (int i = 0; i < SaveData.TEAM_SIZE; i++)
			game.data().team.add(new Creature("chicken", 1, now));

		for (int i = 0; i < SaveData.STORAGE_SIZE; i++)
			game.data().storage.add(new Creature("chicken", 1, now));

		assertEquals(CatchOutcome.FULL, game.tryCatch(wild(CHICKEN, 2, 1)));
		assertEquals(SaveData.STARTING_JARS, game.data().jarCount(JarType.BRONZE));
	}

	@Test
	public void osrsXpTrainsTheTeamAtOneTenth()
	{
		game.data().team.add(new Creature("goblin", 1, now));

		game.addOsrsXp(7);
		assertEquals(0, lead().xp);

		game.addOsrsXp(7);
		assertEquals(1, lead().xp);

		game.addOsrsXp(986);
		assertEquals(100, lead().xp);
		assertTrue(messages.contains("Goblin grew to Lv 2!"));
		assertTrue("the player's own news floats over the player", noticeFor("Goblin grew to Lv 2!").overPlayer());
	}

	@Test
	public void trainingASkillTurnsUpItsSpecialtyJar()
	{
		for (int i = 0; i < 200; i++)
			game.addSkillXp("MINING", 2_000);

		int found = game.data().jarCount(JarType.CAVE);
		assertTrue("expected about 20 Cave jars from 400k Mining XP, got " + found, found >= 8 && found <= 35);
		assertEquals(0, game.data().jarCount(JarType.SWIFT));
		assertTrue(messages.contains("Found a Cave jar (Mining)"));
		assertTrue(noticeFor("Found a Cave jar (Mining)").overPlayer());
		assertEquals(Tone.FIND, noticeFor("Found a Cave jar (Mining)").tone());
	}

	@Test
	public void skillsWithoutAJarOnlyTrainTheTeam()
	{
		game.data().team.add(new Creature("goblin", 1, now));

		for (int i = 0; i < 200; i++)
			game.addSkillXp("HITPOINTS", 2_000);

		assertEquals(40_000, lead().xp);
		for (JarType type : JarType.values())
		{
			if (type.specialty())
				assertEquals(0, game.data().jarCount(type));
		}
	}

	@Test
	public void battlesNeverDropSpecialtyJars()
	{
		game.data().trainerXp = XpRules.trainerXpForLevel(XpRules.MAX_LEVEL);
		for (int i = 0; i < 300; i++)
		{
			game.data().team.clear();
			game.data().team.add(new Creature("king black dragon", 40, now));
			game.battle(wild(CHICKEN, 2, 1));
		}

		assertTrue(game.data().jarCount(JarType.DRAGON) > 0);
		for (JarType type : JarType.values())
		{
			if (type.specialty())
				assertEquals(0, game.data().jarCount(type));
		}
	}

	@Test
	public void aMatchingSpecialtyJarIsOfferedAlongsideTheStandardOne()
	{
		Species spectre = new Species("Aberrant spectre", 96, CombatType.MAGIC, Trait.UNHOLY);
		WildEntry caveSpectre = new WildEntry(1, spectre, 20, 96, true);
		game.data().addJars(JarType.CAVE, 2);
		game.data().addJars(JarType.BLESSED, 1);
		game.data().addJars(JarType.SWIFT, 5);

		assertEquals(Arrays.asList(JarType.CAVE, JarType.BLESSED), game.matchingSpecialtyJars(caveSpectre));
		assertTrue(game.matchingSpecialtyJars(wild(CHICKEN, 2, 1)).isEmpty());

		game.selectJar(JarType.CAVE);

		assertEquals(Arrays.asList(JarType.BLESSED), game.matchingSpecialtyJars(caveSpectre));
	}

	@Test
	public void throwingAChosenJarSpendsThatJar()
	{
		game.data().addJars(JarType.CAVE, 1);

		CatchOutcome outcome = game.tryCatch(wild(CHICKEN, 2, 1), JarType.CAVE);

		assertTrue(outcome != CatchOutcome.NO_JARS);
		assertEquals(0, game.data().jarCount(JarType.CAVE));
		int streakJars = outcome == CatchOutcome.CAUGHT ? GoGame.STREAK_JARS : 0;
		assertEquals(SaveData.STARTING_JARS + streakJars, game.data().jarCount(JarType.BRONZE));
		assertEquals(CatchOutcome.NO_JARS, game.tryCatch(wild(CHICKEN, 2, 1), JarType.CAVE));
	}

	@Test
	public void xpWithNoTeamIsIgnored()
	{
		game.addOsrsXp(5000);

		assertTrue(game.data().team.isEmpty());
		assertEquals(0L, game.data().trainerXp);
	}

	@Test
	public void jarFindsReadFoundAJarWithoutAWalkingSource()
	{
		assertEquals("Found a Bronze jar", GoGame.foundJars(JarType.BRONZE, 1, null));
		assertEquals("Found 3 Bronze jars", GoGame.foundJars(JarType.BRONZE, 3, null));
		assertEquals("Found 2 Iron jars (champion)", GoGame.foundJars(JarType.IRON, 2, "champion"));
	}

	@Test
	public void walkingEarnsABronzeJarEvery150Tiles()
	{
		game.addSteps(GoGame.TILES_PER_JAR - 1);
		assertEquals(SaveData.STARTING_JARS, game.data().jarCount(JarType.BRONZE));

		game.addSteps(GoGame.TILES_PER_JAR + 1);

		assertEquals(SaveData.STARTING_JARS + 2, game.data().jarCount(JarType.BRONZE));
		assertEquals(0, game.data().stepProgress);
	}

	@Test
	public void levelUpsGiveJars()
	{
		game.onLevelUp();

		assertEquals(SaveData.STARTING_JARS + GoGame.JARS_PER_LEVEL_UP, game.data().jarCount(JarType.BRONZE));
	}

	@Test
	public void jarFindsCarryTheirJarForThePicture()
	{
		game.addSteps(GoGame.TILES_PER_JAR);
		game.onLevelUp();
		for (int i = 0; i < 200; i++)
			game.addSkillXp("MINING", 5_000);

		assertEquals(JarType.BRONZE, noticeFor("Found a Bronze jar").jar());
		assertEquals(JarType.BRONZE, noticeFor("Found " + GoGame.JARS_PER_LEVEL_UP + " Bronze jars (level up)").jar());
		assertEquals(JarType.CAVE, noticeFor("Found a Cave jar (Mining)").jar());
	}

	@Test
	public void aBattleWinJarCarriesItsJarAndOtherLinesCarryNone()
	{
		game.data().trainerXp = XpRules.trainerXpForLevel(XpRules.MAX_LEVEL);
		for (int i = 0; i < 300; i++)
		{
			game.data().team.clear();
			game.data().team.add(new Creature("king black dragon", 40, now));
			game.battle(wild(CHICKEN, 2, 1));
		}

		int found = 0;
		for (Notice notice : notices)
		{
			if (!notice.text().endsWith(" jar (battle win)"))
			{
				assertNull(notice.text(), notice.jar());
				continue;
			}

			found++;
			assertNotNull(notice.text(), notice.jar());
			assertEquals(notice.text(), "Found a " + notice.jar().label() + " jar (battle win)");
		}

		assertTrue(found > 0);
	}

	@Test
	public void battleNeedsACreature()
	{
		assertNull(game.battle(wild(CHICKEN, 2, 1)));
	}

	@Test
	public void winningABattleRewardsTheLeadAndWeakensTheFoe()
	{
		game.data().team.add(new Creature("king black dragon", 40, now));
		WildEntry foe = wild(CHICKEN, 2, 1);
		long xpBefore = lead().xp;

		BattleResult result = game.battle(foe);

		assertNotNull(result);
		assertTrue(result.won());
		assertEquals(xpBefore + 30, lead().xp);
		assertEquals(1, game.data().battlesWon);
		assertTrue(foe.battled());
		assertTrue(foe.weakened());
		assertNull("only one battle per wild creature", game.battle(foe));
	}

	@Test
	public void losingLeavesTheLeadTooTiredUntilItRests()
	{
		game.data().team.add(new Creature("chicken", 1, now));

		BattleResult result = game.battle(wild(DRAGON, 60, 276));

		assertNotNull(result);
		assertFalse(result.won());
		assertEquals(0, game.data().battlesWon);
		assertNull(game.battle(wild(CHICKEN, 1, 1)));

		now += Creature.FULL_REST_MS;

		assertNotNull(game.battle(wild(CHICKEN, 1, 1)));
	}

	@Test
	public void teamOrderAndStorageMoves()
	{
		game.data().team.add(new Creature("goblin", 5, now));
		game.data().team.add(new Creature("chicken", 2, now));

		game.setLead(1);
		assertEquals("chicken", lead().species);

		game.moveToStorage(0);
		assertEquals("goblin", lead().species);
		assertEquals(1, game.data().storage.size());

		game.moveToTeam(0);
		assertEquals(2, game.data().team.size());

		game.moveToStorage(1);
		game.release(0);
		assertTrue(game.data().storage.isEmpty());
		assertTrue(messages.contains("Released Chicken"));
	}

	@Test
	public void outOfRangeActionsDoNothing()
	{
		game.setLead(3);
		game.moveToStorage(-1);
		game.moveToTeam(9);
		game.release(0);

		assertFalse(game.takeDirty());
	}

	@Test
	public void theLogListIsReusedUntilSomethingChanges()
	{
		List<LogEntry> first = game.snapshot().log();
		assertSame(first, game.snapshot().log());

		game.markSeen(wild(GOBLIN, 4, 13));

		List<LogEntry> second = game.snapshot().log();
		assertTrue(first != second);
		assertEquals(1, game.snapshot().seenSpecies());
		assertTrue(second.stream().anyMatch(entry -> entry.name().equals("Goblin") && entry.seen()));
	}

	@Test
	public void loadingASaveReplacesEverything()
	{
		catchUntilCaught(wild(CHICKEN, 3, 1));
		SaveData other = SaveData.fresh();
		other.trainerXp = XpRules.trainerXpForLevel(12);

		game.load(other);

		assertTrue(game.data().team.isEmpty());
		assertEquals(12, game.trainerLevel());
		assertFalse(game.takeDirty());
	}

	@Test
	public void theCaughtTotalIgnoresSpeciesThatNoLongerExist()
	{
		SaveData other = SaveData.fresh();
		other.caught.put("chicken", 1);
		other.caught.put("durial321", 2);

		game.load(other);

		assertEquals(1, game.snapshot().caughtSpecies());
	}

	@Test
	public void renamingSetsTheNicknameAndTellsThePlayer()
	{
		Creature goblin = new Creature("goblin", 5, now);
		game.data().team.add(goblin);

		game.rename(goblin, "  Bob  ");

		assertEquals("Bob", goblin.nickname);
		assertTrue(messages.contains("Renamed Goblin to Bob"));
		assertTrue(game.takeDirty());
		PanelState state = game.snapshot();
		CreatureView view = state.team().get(0);
		assertEquals("the species name stays the key for portraits", "Goblin", view.name());
		assertEquals("Bob", view.nickname());
	}

	@Test
	public void anEmptyNameGoesBackToTheSpeciesName()
	{
		Creature goblin = new Creature("goblin", 5, now);
		game.data().team.add(goblin);
		game.rename(goblin, "Bob");

		game.rename(goblin, "");

		assertNull(goblin.nickname);
		assertTrue(messages.contains("Bob is called Goblin again"));
		PanelState state = game.snapshot();
		CreatureView first = state.team().get(0);
		assertNull(first.nickname());
	}

	@Test
	public void anInvalidNameChangesNothing()
	{
		Creature goblin = new Creature("goblin", 5, now);
		game.data().team.add(goblin);
		game.rename(goblin, "Bob");
		game.takeDirty();

		game.rename(goblin, "<b>Bob</b>");
		game.rename(goblin, "ThisNameIsFarTooLong");

		assertEquals("Bob", goblin.nickname);
		assertFalse(game.takeDirty());
		assertTrue(messages.contains("Names are 1 to 12 letters, numbers, spaces or -"));
	}

	@Test
	public void aCreatureThePlayerDoesNotOwnCannotBeRenamed()
	{
		Creature stranger = new Creature("goblin", 5, now);

		game.rename(stranger, "Bob");

		assertNull(stranger.nickname);
		assertFalse(game.takeDirty());
		assertTrue(messages.isEmpty());
	}

	@Test
	public void theLinesUseTheNickname()
	{
		Creature goblin = new Creature("goblin", 1, now);
		game.data().team.add(goblin);
		game.rename(goblin, "Bob");
		messages.clear();

		game.addOsrsXp(10 * (int) XpRules.xpForLevel(5));
		goblin.hpFraction = 0.0;
		goblin.hpStampMs = now;
		BattleResult tired = game.battle(wild(CHICKEN, 3, 1));
		goblin.hpFraction = 1.0;
		BattleResult fought = game.battle(wild(CHICKEN, 3, 1));

		assertTrue(goblin.level() > 1);
		assertTrue(messages.contains("Bob grew to Lv " + goblin.level() + "!"));
		assertTrue(messages.contains("Bob is too tired"));
		assertNull(tired);
		assertNotNull(fought);
		assertTrue(fought.lines().stream().anyMatch(line -> line.text().contains("Bob")));
		assertTrue(fought.lines().stream().noneMatch(line -> line.text().contains("Goblin")));
	}

	@Test
	public void aPickGoesStaleWhenTheNicknameChanges()
	{
		Creature goblin = new Creature("goblin", 5, now);
		game.data().team.add(goblin);
		PanelState state = game.snapshot();
		TradePick pick = TradePick.of(Holding.TEAM, 0, state.team().get(0));
		assertTrue(pick.matches(game.snapshot()));

		game.rename(goblin, "Bob");

		assertFalse(pick.matches(game.snapshot()));
	}

	@Test
	public void releasingNamesTheCreatureByItsNickname()
	{
		Creature goblin = new Creature("goblin", 1, now);
		game.data().storage.add(goblin);
		game.rename(goblin, "Bob");

		game.release(0);

		assertTrue(messages.contains("Released Bob"));
	}

	@Test
	public void everyCatchRollsItsOwnMakeUp()
	{
		game.data().addJars(JarType.BRONZE, 500);
		Set<StatRolls> seen = new HashSet<>();
		for (int i = 0; i < 40; i++)
		{
			catchUntilCaught(wild(CHICKEN, 3, 1));
			StatRolls rolls = newest().rolls();

			assertTrue(rolls.valid());
			seen.add(rolls);
		}

		assertTrue("40 catches should not all roll alike, got " + seen.size(), seen.size() > 10);
		List<CreatureView> shown = game.snapshot().team();
		StatRolls first = shown.get(0).rolls();
		assertTrue(first.valid());
	}

	@Test
	public void aRatedCatchSaysSoOnASecondLine()
	{
		game.data().addJars(JarType.BRONZE, 1000);
		boolean rated = false;
		for (int i = 0; i < 60 && !rated; i++)
		{
			catchUntilCaught(wild(CHICKEN, 3, 1));
			StatRolls rolls = newest().rolls();
			rated = rolls.rating() != StatRolls.Rating.AVERAGE;
			if (rated)
			{
				String note = rolls.rating().label() + " (" + rolls.temperament().label() + ")";
				assertTrue(note, messages.contains(note));
				assertTrue("the catch line itself is unchanged", messages.contains("Caught Chicken (Lv 3)!"));
			}
		}

		assertTrue("some of 60 catches should be rated", rated);
	}

	@Test
	public void anAverageCatchAddsNoRatingLine()
	{
		game.data().addJars(JarType.BRONZE, 1000);
		for (int i = 0; i < 60; i++)
		{
			messages.clear();
			catchUntilCaught(wild(CHICKEN, 3, 1));
			if (newest().rolls().rating() == StatRolls.Rating.AVERAGE)
			{
				assertFalse(messages.stream().anyMatch(line -> line.contains("Strong") || line.contains("Weak")));
				return;
			}
		}

		throw new AssertionError("no average catch in 60");
	}

	private Creature newest()
	{
		List<Creature> stored = game.data().storage;
		List<Creature> team = game.data().team;
		return stored.isEmpty() ? team.get(team.size() - 1) : stored.get(stored.size() - 1);
	}

	private void record(Notice notice)
	{
		messages.add(notice.text());
		notices.add(notice);
	}

	private Notice noticeFor(String text)
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

	private CatchOutcome catchUntilCaught(WildEntry entry)
	{
		CatchOutcome outcome = game.tryCatch(entry);
		for (int i = 0; i < 100 && outcome != CatchOutcome.CAUGHT; i++)
			outcome = game.tryCatch(entry);

		return outcome;
	}

	private static WildEntry wild(Species species, int level, int combatLevel)
	{
		return new WildEntry(1, species, level, combatLevel);
	}
}
