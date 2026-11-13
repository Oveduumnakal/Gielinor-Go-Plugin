/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Checks the turn engine: each choice, the order of a round, forced switches, the team wipe and the limits. */
public class BattleStateTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);
	private static final Species TANK_MAGE = new Species("Tank mage", 1, CombatType.STAB);
	private static final Species TANK_ARCHER = new Species("Tank archer", 1, CombatType.RANGED);

	private static List<Combatant> team(Combatant... members)
	{
		return new ArrayList<>(Arrays.asList(members));
	}

	private static BattleLine.Kind firstBlow(BattleState state)
	{
		TurnResult turn = state.step(BattleChoice.attack());
		List<BattleLine> hits = turn.hits();
		return hits.get(0).kind();
	}

	private static String lineText(TurnResult turn, int index)
	{
		List<BattleLine> lines = turn.lines();
		return lines.get(index).text();
	}

	private static BattleLine.Kind hitKind(TurnResult turn, int index)
	{
		List<BattleLine> hits = turn.hits();
		return hits.get(index).kind();
	}

	private static void assertUntouched(Combatant one)
	{
		assertEquals(one.maxHp(), one.hp(), 0.0);
	}

	private static BattleState wild(List<Combatant> mine, Combatant foe, long seed, BattleState.CatchJudge judge)
	{
		return new BattleState(mine, team(foe), new Random(seed), BattleState.Mode.WILD, judge);
	}

	@Test
	public void attackPlaysOneRoundOfTwoBlows()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0)), new Combatant(GOBLIN, 20, 1.0), 1, null);

		TurnResult turn = state.step(BattleChoice.attack());

		assertEquals(TurnResult.Status.ONGOING, turn.status());
		assertEquals(2, turn.hits().size());
		assertEquals(2, state.attacks());
		assertTrue(turn.turnUsed());
		Combatant fighter = state.mine();
		Combatant opponent = state.foe();
		assertTrue(fighter.hp() < fighter.maxHp());
		assertTrue(opponent.hp() < opponent.maxHp());
		assertNull(state.ended());
	}

	@Test
	public void theFasterCreatureAttacksFirst()
	{
		BattleState mineFaster = wild(team(new Combatant(GOBLIN, 30, 1.0)), new Combatant(GOBLIN, 10, 1.0), 1, null);
		BattleState foeFaster = wild(team(new Combatant(GOBLIN, 10, 1.0)), new Combatant(GOBLIN, 30, 1.0), 1, null);

		assertEquals(BattleLine.Kind.LEAD_HIT, firstBlow(mineFaster));
		assertEquals(BattleLine.Kind.FOE_HIT, firstBlow(foeFaster));
	}

	@Test
	public void aRoundStopsWhenSomeoneFaints()
	{
		BattleState state = wild(team(new Combatant(DRAGON, 80, 1.0)), new Combatant(GOBLIN, 3, 1.0), 4, null);

		TurnResult turn = state.step(BattleChoice.attack());

		assertEquals(TurnResult.Status.WON, turn.status());
		assertEquals(1, turn.hits().size());
		assertTrue(turn.foeFainted());
		assertTrue(state.foe().fainted());
		assertEquals(TurnResult.Status.WON, state.ended());
	}

	@Test
	public void aSwitchCostsTheTurnAndTheFoeStrikesTheNewCreature()
	{
		Combatant first = new Combatant(GOBLIN, 20, 1.0);
		Combatant second = new Combatant(ARCHER, 20, 1.0);
		BattleState state = wild(team(first, second), new Combatant(GOBLIN, 20, 1.0), 2, null);

		TurnResult turn = state.step(BattleChoice.switchTo(1));

		assertSame(second, state.mine());
		assertSame(second, turn.enteredMine());
		assertTrue(turn.voluntarySwitch());
		assertEquals(1, turn.hits().size());
		assertEquals(BattleLine.Kind.FOE_HIT, hitKind(turn, 0));
		assertEquals(first.maxHp(), first.hp(), 0.0);
		assertTrue(second.hp() < second.maxHp());
		assertUntouched(state.foe());
		assertEquals("Goblin comes back", lineText(turn, 0));
		assertEquals("Go, Archer!", lineText(turn, 1));
	}

	@Test
	public void aFaintedCreatureForcesAFreeSwitch()
	{
		Combatant weak = new Combatant(GOBLIN, 2, 1.0);
		Combatant spare = new Combatant(ARCHER, 30, 1.0);
		BattleState state = wild(team(weak, spare), new Combatant(DRAGON, 60, 1.0), 3, null);

		TurnResult fall = state.step(BattleChoice.attack());

		assertEquals(TurnResult.Status.NEEDS_SWITCH, fall.status());
		assertTrue(fall.mineFainted());
		assertTrue(state.needsSwitch());
		assertEquals("Choose your next creature", lineText(fall, fall.lines().size() - 1));
		assertEquals(Collections.singletonList(1), state.switchTargets());
		double foeHealth = state.foe().hp();

		TurnResult free = state.step(BattleChoice.switchTo(1));

		assertEquals(TurnResult.Status.ONGOING, free.status());
		assertTrue(free.hits().isEmpty());
		assertFalse(free.voluntarySwitch());
		assertEquals(foeHealth, state.foe().hp(), 0.0);
		assertEquals(spare.maxHp(), spare.hp(), 0.0);
		assertFalse(state.needsSwitch());
		assertEquals("Go, Archer!", lineText(free, 0));
	}

	@Test
	public void onlyASwitchIsAllowedAfterAFaint()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 2, 1.0), new Combatant(ARCHER, 30, 1.0)),
			new Combatant(DRAGON, 60, 1.0), 3, null);
		state.step(BattleChoice.attack());

		for (BattleChoice wrong : new BattleChoice[]{BattleChoice.attack(), BattleChoice.run(),
			BattleChoice.catching(null)})
		{
			try
			{
				state.step(wrong);
				fail("allowed " + wrong);
			}
			catch (IllegalArgumentException expected)
			{
				assertTrue(state.needsSwitch());
			}
		}
	}

	@Test
	public void theBattleIsLostWhenTheWholeTeamIsDown()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 2, 1.0), new Combatant(ARCHER, 2, 1.0)),
			new Combatant(DRAGON, 80, 1.0), 5, null);

		TurnResult first = state.step(BattleChoice.attack());
		TurnResult second = state.step(BattleChoice.switchTo(1));
		TurnResult last = second.status().over() ? second : state.step(BattleChoice.attack());

		assertEquals(TurnResult.Status.NEEDS_SWITCH, first.status());
		assertEquals(TurnResult.Status.LOST, last.status());
		assertEquals(TurnResult.Status.LOST, state.ended());
		List<Combatant> members = state.team();
		assertTrue(members.get(0).fainted());
		assertTrue(members.get(1).fainted());
		assertEquals(-1, state.firstAlive());
	}

	@Test
	public void aSwitchToAFaintedOrCurrentCreatureIsRefused()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0), new Combatant(ARCHER, 20, 0.0)),
			new Combatant(GOBLIN, 20, 1.0), 1, null);

		for (int place : new int[]{0, 1, 2, -1})
		{
			try
			{
				state.step(BattleChoice.switchTo(place));
				fail("allowed " + place);
			}
			catch (IllegalArgumentException expected)
			{
				assertEquals(0, state.myIndex());
			}
		}

		assertTrue(state.switchTargets().isEmpty());
	}

	@Test
	public void runningEndsTheBattleAtOnce()
	{
		Combatant mine = new Combatant(GOBLIN, 20, 1.0);
		BattleState state = wild(team(mine), new Combatant(GOBLIN, 20, 1.0), 1, null);

		TurnResult turn = state.step(BattleChoice.run());

		assertEquals(TurnResult.Status.RAN, turn.status());
		assertTrue(turn.hits().isEmpty());
		assertEquals("You ran away", lineText(turn, 0));
		assertEquals(mine.maxHp(), mine.hp(), 0.0);
		try
		{
			state.step(BattleChoice.attack());
			fail("stepped a finished battle");
		}
		catch (IllegalStateException expected)
		{
			assertEquals(TurnResult.Status.RAN, state.ended());
		}
	}

	@Test
	public void anItemWithNoneNamedIsRefused()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0)), new Combatant(GOBLIN, 20, 1.0), 1, null);

		try
		{
			state.step(BattleChoice.item());
			fail("an item with none picked was allowed");
		}
		catch (IllegalArgumentException expected)
		{
			assertNull(state.ended());
		}
	}

	@Test
	public void theJudgeSeesTheHealthTheFoeHasLeft()
	{
		List<Double> seen = new ArrayList<>();
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0)), new Combatant(GOBLIN, 20, 0.4), 1,
			(jar, healthLeft) ->
			{
				seen.add(healthLeft);
				return CatchOutcome.CAUGHT;
			});

		TurnResult turn = state.step(BattleChoice.catching(JarType.BRONZE));

		assertEquals(1, seen.size());
		assertEquals(0.4, seen.get(0), 1e-9);
		assertEquals(TurnResult.Status.CAUGHT, turn.status());
		assertEquals(TurnResult.Status.CAUGHT, state.ended());
		assertEquals(0.6, state.foeHealthLost(), 1e-9);
		assertTrue(turn.hits().isEmpty());
	}

	@Test
	public void aJarThatBreaksFreeLetsTheFoeAttack()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0)), new Combatant(GOBLIN, 20, 1.0), 1,
			(jar, healthLeft) -> CatchOutcome.BROKE_FREE);

		TurnResult turn = state.step(BattleChoice.catching(null));

		assertEquals(TurnResult.Status.ONGOING, turn.status());
		assertEquals(1, turn.hits().size());
		assertEquals(BattleLine.Kind.FOE_HIT, hitKind(turn, 0));
		assertUntouched(state.foe());
	}

	@Test
	public void aCreatureThatRunsOffEndsTheBattle()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0)), new Combatant(GOBLIN, 20, 1.0), 1,
			(jar, healthLeft) -> CatchOutcome.FLED);

		assertEquals(TurnResult.Status.FOE_FLED, state.step(BattleChoice.catching(null)).status());
	}

	@Test
	public void noJarsOrNoRoomUsesNoTurn()
	{
		for (CatchOutcome outcome : new CatchOutcome[]{CatchOutcome.NO_JARS, CatchOutcome.FULL})
		{
			BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0)), new Combatant(GOBLIN, 20, 1.0), 1,
				(jar, healthLeft) -> outcome);

			TurnResult turn = state.step(BattleChoice.catching(null));

			assertFalse(turn.turnUsed());
			assertTrue(turn.hits().isEmpty());
			assertEquals(TurnResult.Status.ONGOING, turn.status());
			assertEquals(0, state.totalAttacks());
		}
	}

	@Test
	public void aBattleWithNoJudgeCannotThrowJars()
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0)), new Combatant(GOBLIN, 20, 1.0), 1, null);

		try
		{
			state.step(BattleChoice.catching(null));
			fail("a jar was thrown without a judge");
		}
		catch (IllegalArgumentException expected)
		{
			assertNull(state.ended());
		}
	}

	@Test
	public void theCatchChanceGrowsWithTheHealthTheFoeHasLost()
	{
		int level = 120;
		Species species = new Species("Test", 20, CombatType.STAB);
		double plain = CatchRules.chance(JarType.BRONZE, level, false, species.traits());

		double full = CatchRules.inBattle(JarType.BRONZE, level, species.traits(), 1.0);
		double half = CatchRules.inBattle(JarType.BRONZE, level, species.traits(), 0.5);
		double nearZero = CatchRules.inBattle(JarType.BRONZE, level, species.traits(), 0.0);

		assertEquals(plain, full, 1e-12);
		assertEquals(plain * 1.25, half, 1e-12);
		assertEquals(plain * 1.5, nearZero, 1e-12);
		assertTrue(half > full && nearZero > half);
		assertEquals(CatchRules.MAX_CHANCE, CatchRules.inBattle(JarType.BRONZE, 0, EnumSet.noneOf(Trait.class), 0.0),
			1e-12);
	}

	@Test
	public void aStalledBattleEndsOnHealthLeftAfterTheAttackLimit()
	{
		for (long seed = 0; seed < 4; seed++)
		{
			Combatant mine = new Combatant(TANK_MAGE, "Mage", 2, 1.0, StatRolls.of(new int[]{5, -5, 5, 0}, "HARDY"))
				.moves(TestMoves.alwaysMagic());
			Combatant foe = new Combatant(TANK_ARCHER, "Archer", 2, 1.0,
				StatRolls.of(new int[]{5, -5, 5, 0}, "HARDY"));
			BattleState state = new BattleState(team(mine), team(foe), new Random(seed), BattleState.Mode.SINGLE);
			int rounds = 0;
			while (state.ended() == null)
			{
				state.step(BattleChoice.attack());
				rounds++;
			}

			assertEquals(BattleSim.MAX_TURNS, state.totalAttacks());
			assertEquals(BattleSim.MAX_TURNS / 2, rounds);
			boolean ahead = mine.hpFraction() >= foe.hpFraction();
			assertEquals(ahead ? TurnResult.Status.WON : TurnResult.Status.LOST, state.ended());
		}
	}

	@Test
	public void theSameSeedAndTheSameChoicesGiveTheSameBattle()
	{
		BattleChoice[] choices = {BattleChoice.attack(), BattleChoice.switchTo(1), BattleChoice.attack(),
			BattleChoice.attack(), BattleChoice.attack(), BattleChoice.attack()};
		for (long seed = 0; seed < 6; seed++)
			assertEquals(play(seed, choices), play(seed, choices));

		assertFalse(play(1, choices).equals(play(2, choices)));
	}

	private static String play(long seed, BattleChoice[] choices)
	{
		BattleState state = wild(team(new Combatant(GOBLIN, 20, 1.0), new Combatant(ARCHER, 22, 1.0)),
			new Combatant(GOBLIN, 21, 1.0), seed, null);
		StringBuilder text = new StringBuilder();
		for (BattleChoice choice : choices)
		{
			if (state.ended() != null)
				break;

			BattleChoice use = state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive()) : choice;
			if (use.kind() == BattleChoice.Kind.SWITCH && !state.needsSwitch()
				&& !state.switchTargets().contains(use.teamIndex()))
				use = BattleChoice.attack();

			TurnResult turn = state.step(use);
			for (BattleLine line : turn.lines())
				text.append(line.text()).append('|');

			text.append(turn.status()).append(';');
		}

		return text.toString();
	}

	@Test
	public void aTeamFightFeedsTheNextOpponentIn()
	{
		BattleState state = new BattleState(team(new Combatant(DRAGON, 60, 1.0)),
			team(new Combatant(GOBLIN, 5, 1.0), new Combatant(ARCHER, 5, 1.0)), new Random(2),
			BattleState.Mode.CHAMPION);

		TurnResult first = state.step(BattleChoice.attack());

		assertEquals(TurnResult.Status.ONGOING, first.status());
		assertTrue(first.foeFainted());
		assertSame(state.opponents().get(1), first.enteredFoe());
		assertEquals(1, state.foeIndex());
		assertEquals(0, state.attacks());
		BattleLine out = first.duel().knockOut();
		assertEquals("King Black Dragon beat Goblin (Lv 5)", out.text());

		TurnResult second = state.step(BattleChoice.attack());

		assertEquals(TurnResult.Status.WON, second.status());
	}
}
