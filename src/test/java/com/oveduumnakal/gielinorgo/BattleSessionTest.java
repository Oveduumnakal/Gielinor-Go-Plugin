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
import static org.junit.Assert.assertTrue;

/** Plays battles the player steers through the game: rewards, health after every turn, catching and champions. */
public class BattleSessionTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private final List<Notice> notices = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		game = newGame(5);
	}

	private GoGame newGame(long seed)
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, GOBLIN, DRAGON));
		Champion lumbridge = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250, Arrays.asList(
			new Champion.Member("Chicken", 2), new Champion.Member("Goblin", 3), new Champion.Member("Chicken", 4)),
			JarType.IRON, 3);
		ChampionTable champions = new ChampionTable(Arrays.asList(lumbridge), table);
		return new GoGame(table, new EvolutionTable(Arrays.asList(), table), champions, new Random(seed), () -> now,
			notices::add);
	}

	private static WildEntry wild(Species species, int level, int combatLevel)
	{
		return new WildEntry(1, species, level, combatLevel);
	}

	private Creature lead()
	{
		return game.data().lead();
	}

	private Creature member(int place)
	{
		List<Creature> team = game.data().team;
		return team.get(place);
	}

	private static Combatant fighter(BattleSession session, int place)
	{
		List<Combatant> team = session.state().team();
		return team.get(place);
	}

	private static double healthOf(BattleSession session, int place)
	{
		Combatant one = fighter(session, place);
		return one.hpFraction();
	}

	private static String closing(GoGame played, BattleSession session, TurnResult last)
	{
		List<BattleLine> lines = played.closingLines(session, last);
		return lines.get(0).text();
	}

	private static String autoOpening(BattleSession session)
	{
		BattleLine line = session.opening();
		return line.text();
	}

	private static TurnResult playOut(GoGame played, BattleSession session)
	{
		TurnResult last = null;
		while (!session.over())
		{
			BattleState state = session.state();
			BattleChoice next = state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack();
			last = played.act(session, next);
		}

		return last;
	}

	@Test
	public void aSteeredBattleThatAlwaysAttacksPaysWhatAnAutomaticOneDoes()
	{
		for (long seed = 1; seed <= 8; seed++)
		{
			GoGame auto = newGame(seed);
			GoGame steered = newGame(seed);
			auto.data().team.add(new Creature("goblin", 14, now));
			steered.data().team.add(new Creature("goblin", 14, now));
			WildEntry autoFoe = wild(CHICKEN, 12, 1);
			WildEntry steeredFoe = wild(CHICKEN, 12, 1);
			List<String> autoNotices = new ArrayList<>();
			notices.clear();

			BattleResult result = auto.battle(autoFoe);
			notices.forEach(notice -> autoNotices.add(notice.text()));
			notices.clear();
			BattleSession session = steered.startBattle(steeredFoe);
			TurnResult last = playOut(steered, session);
			List<String> steeredNotices = new ArrayList<>();
			notices.forEach(notice -> steeredNotices.add(notice.text()));

			assertEquals("seed " + seed, result.won(), last.status() == TurnResult.Status.WON);
			SaveData autoData = auto.data();
			SaveData steeredData = steered.data();
			assertEquals(autoData.battlesWon, steeredData.battlesWon);
			assertEquals(autoData.trainerXp, steeredData.trainerXp);
			Creature autoLead = autoData.lead();
			Creature steeredLead = steeredData.lead();
			assertEquals(autoLead.xp, steeredLead.xp);
			assertEquals(autoLead.hpFraction, steeredLead.hpFraction, 0.0);
			assertEquals(autoFoe.weakened(), steeredFoe.weakened());
			assertEquals(autoNotices, steeredNotices);
			assertEquals(autoData.eggs.size(), steeredData.eggs.size());
		}
	}

	@Test
	public void healthIsWrittenAfterEveryTurnAndOnlyForCreaturesThatLostSome()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		Creature bench = new Creature("king black dragon", 30, now);
		game.data().team.add(bench);
		BattleSession session = game.startBattle(wild(DRAGON, 25, 276));

		TurnResult turn = game.act(session, BattleChoice.attack());

		assertNotNull(turn);
		assertEquals(healthOf(session, 0), lead().hpFraction, 0.0);
		assertTrue(lead().hpFraction < 1.0);
		assertEquals(now, lead().hpStampMs);
		assertEquals(1.0, bench.hpFraction, 0.0);
		double afterFirst = lead().hpFraction;

		game.act(session, BattleChoice.attack());

		assertTrue(session.over() || lead().hpFraction < afterFirst);
		assertEquals(healthOf(session, 0), lead().hpFraction, 0.0);
		assertTrue(game.takeDirty());
	}

	@Test
	public void winningPaysOnceAndWeakensTheFoe()
	{
		game.data().team.add(new Creature("king black dragon", 40, now));
		WildEntry foe = wild(CHICKEN, 2, 1);
		long xpBefore = lead().xp;
		BattleSession session = game.startBattle(foe);

		TurnResult last = playOut(game, session);

		assertEquals(TurnResult.Status.WON, last.status());
		assertEquals(xpBefore + 30, lead().xp);
		assertEquals(1, game.data().battlesWon);
		assertTrue(foe.battled());
		assertTrue(foe.weakened());
		assertNull("a finished battle takes no more turns", game.act(session, BattleChoice.attack()));
		assertEquals(1, game.data().battlesWon);
		assertEquals("Your King Black Dragon won!", closing(game, session, last));
		assertNull("only one battle per wild creature", game.startBattle(foe));
	}

	@Test
	public void losingEndsTheBattleWithoutRewards()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		WildEntry foe = wild(DRAGON, 60, 276);
		BattleSession session = game.startBattle(foe);

		TurnResult last = playOut(game, session);

		assertEquals(TurnResult.Status.LOST, last.status());
		assertEquals(0, game.data().battlesWon);
		assertTrue(foe.battled());
		assertFalse(foe.weakened());
		assertEquals(0.0, lead().hpFraction, 0.0);
		assertEquals("Your Chicken lost to the King Black Dragon.", closing(game, session, last));
	}

	@Test
	public void runningPaysNothingAndMarksTheCreatureAsBattled()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		WildEntry foe = wild(GOBLIN, 20, 13);
		BattleSession session = game.startBattle(foe);
		game.act(session, BattleChoice.attack());
		double health = lead().hpFraction;

		TurnResult ran = game.act(session, BattleChoice.run());

		assertEquals(TurnResult.Status.RAN, ran.status());
		assertTrue(session.over());
		assertEquals(0, game.data().battlesWon);
		assertTrue(foe.battled());
		assertFalse(foe.weakened());
		assertEquals(health, lead().hpFraction, 0.0);
		assertTrue(game.closingLines(session, ran).isEmpty());
		assertNull("a second forfeit does nothing", game.forfeit(session));
	}

	@Test
	public void leavingTheBattleCountsAsRunning()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		WildEntry foe = wild(GOBLIN, 20, 13);
		BattleSession session = game.startBattle(foe);

		TurnResult forfeited = game.forfeit(session);

		assertEquals(TurnResult.Status.RAN, forfeited.status());
		assertTrue(foe.battled());
		assertNull(game.startBattle(foe));
	}

	@Test
	public void aChoiceThatCannotBeMadeChangesNothing()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		BattleSession session = game.startBattle(wild(GOBLIN, 20, 13));

		assertNull(game.act(session, BattleChoice.switchTo(0)));
		assertNull(game.act(session, BattleChoice.item()));
		assertNull(game.act(session, BattleChoice.switchTo(5)));

		assertFalse(session.over());
		assertEquals(0, session.state().totalAttacks());
		assertEquals(1.0, lead().hpFraction, 0.0);
	}

	@Test
	public void aBattleNeedsAnUntiredLeadAndANewCreature()
	{
		assertNull(game.startBattle(wild(CHICKEN, 2, 1)));
		game.data().team.add(new Creature("chicken", 1, now));
		lead().hpFraction = 0.1;
		lead().hpStampMs = now;

		assertNull(game.startBattle(wild(CHICKEN, 2, 1)));

		now += Creature.FULL_REST_MS;

		assertNotNull(game.startBattle(wild(CHICKEN, 2, 1)));
	}

	@Test
	public void aFaintedCreatureIsReplacedForFreeAndTheTeamCanBeWipedOut()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		game.data().team.add(new Creature("chicken", 1, now));
		BattleSession session = game.startBattle(wild(DRAGON, 60, 276));

		TurnResult fall = game.act(session, BattleChoice.attack());

		assertEquals(TurnResult.Status.NEEDS_SWITCH, fall.status());
		assertEquals(0.0, member(0).hpFraction, 0.0);
		assertNull(game.act(session, BattleChoice.attack()));

		TurnResult freeSwitch = game.act(session, BattleChoice.switchTo(1));

		assertTrue(freeSwitch.hits().isEmpty());
		assertEquals(1.0, member(1).hpFraction, 0.0);

		TurnResult last = playOut(game, session);

		assertEquals(TurnResult.Status.LOST, last.status());
		assertEquals(0.0, member(1).hpFraction, 0.0);
	}

	@Test
	public void aWildBattleOnlyUsesTeamMembersRestedEnoughToFight()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		Creature tired = new Creature("chicken", 5, now);
		tired.hpFraction = 0.1;
		tired.hpStampMs = now;
		game.data().team.add(tired);
		game.data().team.add(new Creature("chicken", 5, now));

		BattleSession session = game.startBattle(wild(GOBLIN, 20, 13));

		List<Combatant> fighters = session.state().team();
		assertEquals(2, fighters.size());
		assertEquals(Arrays.asList(1), session.state().switchTargets());
		assertEquals(Arrays.asList(member(0), member(2)), session.fighters());
	}

	@Test
	public void aJarThrownInBattleSpendsOneAndCatchesAFoeThatIsHurt()
	{
		game.data().team.add(new Creature("king black dragon", 80, now));
		game.data().addJars(JarType.BRONZE, 200);
		WildEntry foe = wild(CHICKEN, 3, 1);
		BattleSession session = game.startBattle(foe);
		int jars = game.data().jarCount(JarType.BRONZE);
		int thrown = 0;
		TurnResult turn = null;

		while (!session.over() && thrown < 150)
		{
			turn = game.act(session, BattleChoice.catching(null));
			thrown++;
		}

		assertEquals(TurnResult.Status.CAUGHT, session.state().ended());
		assertEquals("each throw spends one, the first catch of a day pays the streak",
			jars - thrown + GoGame.STREAK_JARS, game.data().jarCount(JarType.BRONZE));
		assertEquals(2, game.data().team.size());
		assertEquals("chicken", member(1).species);
		assertEquals(1, (int) game.data().caught.get("chicken"));
		assertEquals(0, game.data().battlesWon);
		assertTrue(notices.stream().anyMatch(notice -> notice.text().equals("Caught Chicken (Lv 3)!")));
		assertTrue(game.closingLines(session, turn).isEmpty());
	}

	@Test
	public void aJarThatBreaksFreeLetsTheFoeStrikeBack()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		game.data().addJars(JarType.BRONZE, 200);
		BattleSession session = game.startBattle(wild(DRAGON, 80, 276));

		TurnResult turn = game.act(session, BattleChoice.catching(null));

		assertNotNull(turn);
		if (turn.status() == TurnResult.Status.ONGOING)
		{
			assertEquals(1, turn.hits().size());
			assertTrue(lead().hpFraction < 1.0);
		}
	}

	@Test
	public void noJarsOrNoRoomLeavesTheTurnAndTheJarsAlone()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		BattleSession session = game.startBattle(wild(GOBLIN, 20, 13));
		int jars = game.data().jarCount(JarType.BRONZE);
		game.tradeLocks(new GoGame.TradeLocks()
		{
			@Override
			public int reservedCreatureSlots()
			{
				return SaveData.TEAM_SIZE + SaveData.STORAGE_SIZE;
			}
		});

		TurnResult full = game.act(session, BattleChoice.catching(null));

		assertFalse(full.turnUsed());
		assertEquals(jars, game.data().jarCount(JarType.BRONZE));
		assertEquals(0, session.state().totalAttacks());
		assertEquals(1.0, lead().hpFraction, 0.0);

		game.tradeLocks(null);
		SaveData save = game.data();
		save.addJars(JarType.BRONZE, -save.jarCount(JarType.BRONZE));
		TurnResult empty = game.act(session, BattleChoice.catching(null));

		assertFalse(empty.turnUsed());
		assertEquals(0, session.state().totalAttacks());
		assertTrue(notices.stream().anyMatch(notice -> notice.text().equals("No jars left")));
	}

	@Test
	public void theBattleViewListsWhoCanStepInAndWhatAJarCosts()
	{
		game.data().team.add(new Creature("goblin", 20, now));
		game.data().team.add(new Creature("chicken", 4, now));
		BattleSession session = game.startBattle(wild(GOBLIN, 20, 13));

		BattleView view = game.battleView(session, 7, 0.5);

		assertEquals("Goblin", view.mineName());
		assertEquals("Goblin", view.foeName());
		assertEquals(1, view.options().size());
		BattleView.Option first = view.options().get(0);
		assertEquals("Chicken", first.name());
		assertEquals(1, first.teamIndex());
		assertTrue(view.canCatch());
		assertEquals("Bronze", view.jarLabel());
		assertEquals(7, view.secondsLeft());
		assertEquals(0.5, view.timeLeft(), 0.0);
		assertFalse(view.forcedSwitch());
	}

	@Test
	public void aChampionBattleWorksTheTeamAndPaysTheSigilOnce()
	{
		game.data().team.add(new Creature("king black dragon", 60, now));
		game.moveTo(3222, 3218, 0);
		BattleSession session = game.startChallenge("lumbridge");

		assertNotNull(session);
		assertFalse(session.wild());
		assertTrue(autoOpening(session).startsWith("Challenging the"));
		List<Combatant> opponents = session.state().opponents();
		assertEquals(3, opponents.size());

		TurnResult last = playOut(game, session);

		assertEquals(TurnResult.Status.WON, last.status());
		assertTrue(game.data().sigils.contains("lumbridge"));
		assertEquals(1, game.data().battlesWon);
		assertEquals(3, game.data().jarCount(JarType.IRON));
		assertTrue(closing(game, session, last).startsWith("You beat the"));
	}

	@Test
	public void aChampionBattleNeedsTheRightCityAndARestedTeam()
	{
		game.data().team.add(new Creature("goblin", 20, now));

		assertNull("not in the city", game.startChallenge("lumbridge"));
		assertNull("no such champion", game.startChallenge("nowhere"));

		game.moveTo(3222, 3218, 0);
		lead().hpFraction = 0.1;
		lead().hpStampMs = now;

		assertNull("the team is too tired", game.startChallenge("lumbridge"));
	}

	@Test
	public void aLostChampionBattlePaysNothingAndRunningLeavesTheChampion()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		game.moveTo(3222, 3218, 0);
		BattleSession lost = game.startChallenge("lumbridge");

		TurnResult last = playOut(game, lost);

		assertEquals(TurnResult.Status.LOST, last.status());
		assertFalse(game.data().sigils.contains("lumbridge"));
		assertEquals(0, game.data().battlesWon);
		assertTrue(closing(game, lost, last).endsWith("Rest and try again"));

		now += Creature.FULL_REST_MS;
		BattleSession ran = game.startChallenge("lumbridge");

		assertEquals(TurnResult.Status.RAN, game.act(ran, BattleChoice.run()).status());
		assertFalse(game.data().sigils.contains("lumbridge"));
		assertNotNull("running does not lock the champion", game.startChallenge("lumbridge"));
	}
}
