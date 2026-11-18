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

/** A creature that reaches 0 health in a battle faints, stays down, and only a revive brings it back. */
public class GoGameFaintTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, GOBLIN, DRAGON));
		Champion lumbridge = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250, Arrays.asList(
			new Champion.Member("Chicken", 2), new Champion.Member("Goblin", 3), new Champion.Member("Chicken", 4)),
			JarType.IRON, 3);
		ChampionTable champions = new ChampionTable(Arrays.asList(lumbridge), table);
		game = new GoGame(table, new EvolutionTable(Arrays.asList(), table), champions, new Random(5), () -> now,
			notice -> messages.add(notice.text()));
		game.setBankNear(true);
	}

	private static WildEntry wild(Species species, int level, int combatLevel)
	{
		return new WildEntry(1, species, level, combatLevel);
	}

	private Creature member(int place)
	{
		return game.data().team.get(place);
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
	public void aLostAutoBattleFaintsTheCreatureAndSaysSo()
	{
		game.data().team.add(new Creature("chicken", 1, now));

		BattleResult result = game.battle(wild(DRAGON, 60, 276));

		assertFalse(result.won());
		assertTrue(member(0).fainted);
		assertEquals(0.0, member(0).hpFraction, 1e-9);
		assertTrue(messages.toString(), messages.contains("Chicken fainted. Revive it at a bank"));
	}

	@Test
	public void aFaintedCreatureStaysDownWhileTheClockRunsAndSitsOutUntilItIsRevived()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		game.battle(wild(DRAGON, 60, 276));
		assertNull(game.battle(wild(CHICKEN, 1, 1)));

		now += Creature.FULL_REST_MS * 10;

		assertEquals(0.0, member(0).hpFractionAt(now), 1e-9);
		assertNull("still refused after a long rest", game.battle(wild(CHICKEN, 1, 1)));
		assertTrue(messages.contains(GoGame.FAINTED_TEAM_TEXT));
		assertFalse(game.canBattle(now, true));
		assertFalse(game.canBattle(now, false));

		assertTrue(game.reviveAtBank(member(0)));

		assertNotNull(game.battle(wild(CHICKEN, 1, 1)));
	}

	@Test
	public void aLostSteeredBattleFaintsTheCreature()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		BattleSession session = game.startBattle(wild(DRAGON, 60, 276));

		TurnResult last = playOut(game, session);

		assertEquals(TurnResult.Status.LOST, last.status());
		assertTrue(member(0).fainted);
		assertTrue(messages.contains("Chicken fainted. Revive it at a bank"));
	}

	@Test
	public void aLostChampionBattleFaintsTheTeamInBothWaysItCanBePlayed()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		game.data().team.add(new Creature("chicken", 1, now));
		game.moveTo(3222, 3218, 0);

		TeamBattleResult auto = game.challenge("lumbridge");

		assertFalse(auto.won());
		assertTrue(member(0).fainted);
		assertTrue(member(1).fainted);

		game.reviveAtBank(member(0));
		game.reviveAtBank(member(1));
		BattleSession steered = game.startChallenge("lumbridge");
		assertEquals(TurnResult.Status.LOST, playOut(game, steered).status());
		assertTrue(member(0).fainted);
		assertTrue(member(1).fainted);
	}

	@Test
	public void aWinnerThatSurvivesIsNotFaintedAndOnlyTheOneThatDroppedIs()
	{
		Creature weak = new Creature("chicken", 1, now);
		Creature strong = new Creature("goblin", 70, now);
		game.data().team.add(weak);
		game.data().team.add(strong);

		BattleResult result = game.battle(wild(GOBLIN, 12, 5));

		assertTrue(result.won());
		assertTrue("the weak lead went down", weak.fainted);
		assertFalse("the strong one won", strong.fainted);
	}

	@Test
	public void aBattleThatEndsWithEveryoneUpFaintsNobody()
	{
		game.data().team.add(new Creature("goblin", 70, now));

		BattleResult result = game.battle(wild(CHICKEN, 1, 1));

		assertTrue(result.won());
		assertFalse(member(0).fainted);
		assertTrue(member(0).hpFractionAt(now) > 0.0);
	}

	@Test
	public void aReviveDraughtUsedInTheBattleStandsTheCreatureUpAgain()
	{
		game.data().team.add(new Creature("goblin", 1, now));
		game.data().team.add(new Creature("goblin", 90, now));
		game.data().addItems(ItemType.REVIVE, 1);
		BattleSession session = game.startBattle(wild(GOBLIN, 30, 20));
		assertNotNull(session);
		int guard = 0;
		while (!session.state().needsSwitch() && guard++ < 200)
			assertNotNull(game.act(session, BattleChoice.attack()));

		assertTrue("the first creature fell", member(0).fainted);

		assertNotNull(game.act(session, BattleChoice.switchTo(session.state().firstAlive())));
		TurnResult turn = game.act(session, BattleChoice.item(ItemType.REVIVE, 0));

		assertNotNull(turn);
		assertFalse("the draught cleared the flag", member(0).fainted);
		assertTrue(member(0).hpFraction > 0.0);
	}

	@Test
	public void aFaintedLeadLetsTheFirstLivingCreatureFightAndATiredLeadStillRefuses()
	{
		game.data().team.add(new Creature("goblin", 70, now));
		game.data().team.add(new Creature("goblin", 70, now));
		member(0).faint(now);

		BattleSession session = game.startBattle(wild(CHICKEN, 1, 1));

		assertNotNull(session);
		assertEquals(1, session.fighters().size());
		assertTrue(game.canBattle(now, true));

		BattleSession again = game.startBattle(wild(CHICKEN, 2, 1));
		assertNotNull(again);
		member(0).revive(0.1, now);
		member(0).hpStampMs = now;
		assertNull("a tired, not fainted, lead refuses as before", game.startBattle(wild(CHICKEN, 3, 1)));
		assertTrue(messages.contains("Goblin is too tired"));
	}

	@Test
	public void trainingXpStillReachesAFaintedCreature()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		member(0).faint(now);

		game.addOsrsXp(50_000);

		assertTrue(member(0).xp > XpRules.xpForLevel(1));
		assertTrue(member(0).fainted);
	}
}
