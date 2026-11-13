/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Checks moves in the turn engine: power, accuracy, the choice of a move, the pick and the order of random draws. */
public class MoveBattleTest
{
	private static final Species FIGHTER = new Species("Fighter", 20, CombatType.MELEE);
	private static final Species DUMMY = new Species("Dummy", 20, CombatType.MELEE);
	private static final Move WEAK = new Move("weak", "Weak", CombatType.MELEE, 1.0, 1.0);
	private static final Move STRONG = new Move("strong", "Strong", CombatType.MELEE, 2.0, 1.0);
	private static final Move SHAKY = new Move("shaky", "Shaky", CombatType.MELEE, 2.0, 0.5);

	/** A random source that returns scripted doubles first and counts every draw. */
	private static final class Scripted extends Random
	{
		private final Deque<Double> queue = new ArrayDeque<>();
		private int draws;

		private Scripted(Double... values)
		{
			queue.addAll(Arrays.asList(values));
		}

		@Override
		public double nextDouble()
		{
			draws++;
			return queue.isEmpty() ? 0.5 : queue.poll();
		}

		@Override
		public boolean nextBoolean()
		{
			return true;
		}
	}

	private static Combatant fighter(int level, Move... moves)
	{
		return new Combatant(FIGHTER, "Fighter", level, 1.0).moves(Arrays.asList(moves));
	}

	private static Combatant dummy(int level)
	{
		return new Combatant(DUMMY, "Dummy", level, 1.0);
	}

	private static BattleState duel(Combatant mine, Combatant foe, Random random)
	{
		return new BattleState(Collections.singletonList(mine), Collections.singletonList(foe), random,
			BattleState.Mode.SINGLE);
	}

	private static BattleLine firstHit(TurnResult turn)
	{
		List<BattleLine> hits = turn.hits();
		return hits.get(0);
	}

	private static int damageOf(BattleLine line)
	{
		String text = line.text();
		int start = text.indexOf(": -") + 3;
		return Integer.parseInt(text.substring(start, text.indexOf(' ', start)));
	}

	@Test
	public void powerScalesTheDamageAYouChooseToDeal()
	{
		Combatant dummy = dummy(20);
		BattleState weak = duel(fighter(30, WEAK, STRONG), dummy, new Random(4));
		BattleState strong = duel(fighter(30, WEAK, STRONG), dummy(20), new Random(4));

		BattleLine light = firstHit(weak.step(BattleChoice.attack(0)));
		BattleLine heavy = firstHit(strong.step(BattleChoice.attack(1)));

		assertTrue(light.text(), light.text().startsWith("Fighter uses Weak: -"));
		assertTrue(heavy.text(), heavy.text().startsWith("Fighter uses Strong: -"));
		assertTrue(damageOf(heavy) > damageOf(light));
	}

	@Test
	public void aChosenMoveIsUsedWithoutAPickDraw()
	{
		Scripted random = new Scripted();
		BattleState state = duel(fighter(30, WEAK, STRONG), dummy(10), random);

		TurnResult turn = state.step(BattleChoice.attack(1));

		assertEquals("only the damage spread of each blow is drawn", turn.hits().size(), random.draws);
	}

	@Test
	public void aMoveThatDoesNotExistIsRefused()
	{
		BattleState state = duel(fighter(30, WEAK), dummy(10), new Random(1));

		try
		{
			state.step(BattleChoice.attack(1));
			fail("a creature with one move has no move 1");
		}
		catch (IllegalArgumentException expected)
		{
			assertEquals(0, state.totalAttacks());
		}
	}

	@Test
	public void theRandomDrawsComeInTheOrderPickAccuracyDamage()
	{
		Scripted random = new Scripted(0.99, 0.4, 0.5);
		Combatant mine = fighter(30, WEAK, SHAKY);
		Combatant foe = dummy(10);
		BattleState state = duel(mine, foe, random);

		BattleLine line = firstHit(state.step(BattleChoice.attack()));

		assertTrue("0.99 of the weights picks the last move, Shaky: " + line.text(),
			line.text().startsWith("Fighter uses Shaky: -"));
		assertFalse("0.4 is under its accuracy of 0.5, so it hits", line.miss());
		assertTrue("the third draw was the damage spread", random.queue.isEmpty());
	}

	@Test
	public void aFailedAccuracyRollIsAMissWithNoDamageAndNoSpreadDraw()
	{
		Scripted random = new Scripted(0.99, 0.9);
		Combatant foe = dummy(10);
		BattleState state = duel(fighter(30, WEAK, SHAKY), foe, random);

		TurnResult turn = state.step(BattleChoice.attack());

		BattleLine line = turn.hits().get(0);
		assertTrue(line.miss());
		assertEquals("Fighter's Shaky missed", line.text());
		assertEquals(foe.maxHp(), foe.hp(), 0.0);
		assertEquals("pick and accuracy are drawn, the foe's own blow then draws its spread", 3, random.draws);
	}

	@Test
	public void aCreatureWithOnePlainMoveMakesTheOldDraws()
	{
		Scripted random = new Scripted();
		BattleState state = duel(new Combatant(FIGHTER, 30, 1.0), dummy(10), random);

		TurnResult turn = state.step(BattleChoice.attack());

		assertEquals("one spread draw per blow and nothing else", turn.hits().size(), random.draws);
		BattleLine first = firstHit(turn);
		assertTrue(first.text().startsWith("-"));
	}

	@Test
	public void theAutoPickFavoursTheStrongerMove()
	{
		int strong = 0;
		int weak = 0;
		for (int seed = 0; seed < 400; seed++)
		{
			BattleState state = duel(fighter(60, WEAK, STRONG), dummy(60), new Random(seed));
			for (BattleLine line : state.step(BattleChoice.attack()).hits())
			{
				if (line.text().startsWith("Fighter uses Strong"))
					strong++;
				else if (line.text().startsWith("Fighter uses Weak"))
					weak++;
			}
		}

		assertTrue("Strong has twice the weight: " + strong + " against " + weak, strong > weak * 1.5);
		assertTrue("Weak still comes up: " + weak, weak > 50);
	}

	@Test
	public void theAutoPickWeighsTheCombatTriangle()
	{
		Move magic = new Move("zap", "Zap", CombatType.MAGIC, 1.0, 1.0);
		int zaps = 0;
		for (int seed = 0; seed < 400; seed++)
		{
			BattleState state = duel(fighter(60, WEAK, magic), dummy(60), new Random(seed));
			for (BattleLine line : state.step(BattleChoice.attack()).hits())
				zaps += line.text().startsWith("Fighter uses Zap") ? 1 : 0;
		}

		assertTrue("magic beats the melee dummy, so Zap is favoured: " + zaps, zaps > 400 * 0.5);
	}

	@Test
	public void aBattleWithMovesIsTheSameForTheSameSeed()
	{
		for (long seed = 0; seed < 10; seed++)
			assertEquals(play(seed), play(seed));
	}

	private static List<String> play(long seed)
	{
		BattleState state = duel(fighter(25, WEAK, STRONG, SHAKY), dummy(30), new Random(seed));
		List<String> texts = new ArrayList<>();
		while (state.ended() == null)
		{
			for (BattleLine line : state.step(BattleChoice.attack()).lines())
				texts.add(line.text());
		}

		return texts;
	}

	@Test
	public void aMissIsAcknowledgedInTheTimelineAsAMissedBeat()
	{
		BattleLine miss = BattleSim.miss(fighter(10, SHAKY), true, SHAKY);
		BattleLine hit = new BattleLine("-3", Tone.GOOD, false, BattleLine.Kind.LEAD_HIT);

		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(Arrays.asList(miss, hit));

		assertTrue(steps.get(0).missed());
		assertFalse(steps.get(1).missed());
		assertEquals(BattleTimeline.Beat.LEAD_ATTACKS, steps.get(0).beat());
	}

	@Test
	public void aSecondSlotMoveMakesASpecialBeatWithTheIcyLook()
	{
		Combatant wyvern = new Combatant(FIGHTER, "Wyvern", 30, 1.0).moves(TestMoves.wyvern());
		BattleLine breath = BattleSim.strike(wyvern, dummy(10), 5, true, TestMoves.ICY_BREATH);

		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(Collections.singletonList(breath));

		assertEquals(BattleTimeline.Beat.LEAD_SPECIAL, steps.get(0).beat());
		assertTrue(breath.special());
		assertTrue(breath.icy());
		assertEquals(CombatType.MAGIC, breath.style());
	}

	@Test
	public void theChoiceRemembersTheMove()
	{
		assertEquals(BattleChoice.attack(2), BattleChoice.attack(2));
		assertFalse(BattleChoice.attack(2).equals(BattleChoice.attack(1)));
		assertFalse(BattleChoice.attack(0).equals(BattleChoice.attack()));
		assertEquals(-1, BattleChoice.attack().moveIndex());
		assertEquals(2, BattleChoice.attack(2).moveIndex());
	}
}
