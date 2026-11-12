/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
		assertTrue(game.data().jarCount(JarType.BRONZE) < SaveData.STARTING_JARS);
		assertTrue(game.data().trainerXp > 0);
		assertTrue(messages.contains("Caught Chicken (Lv 3)!"));
		assertEquals("creature news floats over the creature", 1, noticeFor("Caught Chicken (Lv 3)!").npcIndex());
		assertTrue(game.takeDirty());
		assertFalse(game.takeDirty());
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
		assertTrue(messages.contains("+1 Cave jar (Mining)"));
		assertTrue(noticeFor("+1 Cave jar (Mining)").overPlayer());
		assertEquals(Tone.FIND, noticeFor("+1 Cave jar (Mining)").tone());
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
		assertEquals(SaveData.STARTING_JARS, game.data().jarCount(JarType.BRONZE));
		assertEquals(CatchOutcome.NO_JARS, game.tryCatch(wild(CHICKEN, 2, 1), JarType.CAVE));
	}

	@Test
	public void xpWithNoTeamIsIgnored()
	{
		game.addOsrsXp(5000);

		assertFalse(game.takeDirty());
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
