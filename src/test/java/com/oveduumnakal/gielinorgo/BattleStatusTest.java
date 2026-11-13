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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks statuses inside the turn engine: where they are rolled, when they bite, the lines and the random draws. */
public class BattleStatusTest
{
	private static final Species FIGHTER = new Species("Fighter", 20, CombatType.STAB);
	private static final Species DUMMY = new Species("Dummy", 20, CombatType.STAB);
	private static final Species SPIDER = new Species("Spider", 20, CombatType.STAB, Trait.POISONOUS);
	private static final Move WEAK = new Move("weak", "Weak", CombatType.STAB, 1.0, 1.0);

	/** A random source that returns scripted values first and counts every draw. */
	private static final class Scripted extends Random
	{
		private final Deque<Double> doubles = new ArrayDeque<>();
		private final Deque<Integer> ints = new ArrayDeque<>();
		private int draws;

		private Scripted(Double... values)
		{
			doubles.addAll(Arrays.asList(values));
		}

		@Override
		public double nextDouble()
		{
			draws++;
			return doubles.isEmpty() ? 0.5 : doubles.poll();
		}

		@Override
		public int nextInt(int bound)
		{
			draws++;
			return ints.isEmpty() ? 0 : ints.poll();
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

	private static List<String> texts(TurnResult turn)
	{
		List<String> out = new ArrayList<>();
		for (BattleLine line : turn.lines())
			out.add(line.text());

		return out;
	}

	@Test
	public void afterAnItemTheHigherLevelCreatureTakesPoisonFirstWhenSpeedsTie()
	{
		Combatant mine = new Combatant(FIGHTER, "Fighter", 20, 0.05);
		Combatant foe = new Combatant(DUMMY, "Dummy", 21, 0.05);
		assertEquals(mine.speed(), foe.speed());
		mine.afflict(Status.POISON, new Random(1));
		foe.afflict(Status.POISON, new Random(1));
		BattleState state = duel(mine, foe, new Random(4));

		TurnResult turn = state.step(BattleChoice.item(ItemType.SMALL_SALVE, 0));

		List<String> hurt = new ArrayList<>();
		for (String text : texts(turn))
		{
			if (text.contains("hurt by poison"))
				hurt.add(text);
		}

		assertTrue(hurt.toString(), hurt.get(0).startsWith("Dummy"));
	}

	@Test
	public void aPoisonedHitPutsThePoisonOnAndItBitesAtTheEndOfTheRound()
	{
		Combatant foe = dummy(10);
		BattleState state = duel(fighter(30, TestMoves.causing("sting", Status.POISON, 1.0)), foe, new Random(4));

		TurnResult turn = state.step(BattleChoice.attack());

		List<String> texts = texts(turn);
		assertTrue(texts.toString(), texts.contains("Dummy is poisoned!"));
		assertTrue(texts.toString(), texts.stream().anyMatch(t -> t.startsWith("Dummy is hurt by poison -")));
		assertEquals(Status.POISON, foe.status());
		assertTrue("the poison line comes after the blow", texts.indexOf("Dummy is poisoned!") > 0);
	}

	@Test
	public void poisonRunsFourRoundsAndThenTheCreatureShakesItOff()
	{
		Combatant foe = dummy(40);
		foe.afflict(Status.POISON, new Random(1));
		BattleState state = duel(fighter(40, WEAK), foe, new Random(4));

		int hurt = 0;
		boolean shook = false;
		for (int round = 0; round < 5 && state.ended() == null; round++)
		{
			for (String text : texts(state.step(BattleChoice.attack())))
			{
				hurt += text.startsWith("Dummy is hurt by poison") ? 1 : 0;
				shook |= text.equals("Dummy shook it off");
			}
		}

		assertEquals(4, hurt);
		assertTrue(shook);
		assertNull(foe.status());
	}

	@Test
	public void theTagsOfAChallengedPlayersDuelAreFlippedLikeTheWording()
	{
		Combatant theirs = dummy(40);
		Combatant challenger = dummy(40);
		theirs.afflict(Status.POISON, new Random(1));
		BattleState state = new BattleState(Collections.singletonList(challenger),
			Collections.singletonList(theirs), new Random(4), BattleState.Mode.duel(false));

		TurnResult turn = state.step(BattleChoice.attack());

		BattleLine hurt = null;
		for (BattleLine line : turn.hits())
			if (line.kind() == BattleLine.Kind.STATUS_DAMAGE)
				hurt = line;

		assertTrue(hurt != null && hurt.overPlayer());
		assertEquals(Status.POISON, hurt.mineStatus());
		assertNull(hurt.foeStatus());
	}

	@Test
	public void aFrozenCreatureLosesItsTurnAndTheLineSaysSo()
	{
		Combatant foe = dummy(10);
		Combatant mine = fighter(30, TestMoves.causing("ice", Status.FREEZE, 1.0));
		BattleState state = duel(mine, foe, new Random(4));

		List<String> all = new ArrayList<>();
		for (int round = 0; round < 3 && state.ended() == null; round++)
			all.addAll(texts(state.step(BattleChoice.attack())));

		assertTrue(all.toString(), all.contains("Dummy is frozen!"));
		assertTrue(all.toString(), all.contains("Dummy is frozen solid"));
		assertTrue(all.toString(), all.contains("Dummy shook it off"));
	}

	@Test
	public void theRandomDrawsComeInTheOrderPickSpreadEffectThenTheFoesBlow()
	{
		Scripted random = new Scripted(0.99, 0.5, 0.3, 0.5);
		Combatant mine = fighter(30, WEAK, TestMoves.causing("sting", Status.POISON, 0.5));
		Combatant foe = dummy(10);
		BattleState state = duel(mine, foe, random);

		state.step(BattleChoice.attack());

		assertEquals("pick, spread, effect, then the foe's own spread", 4, random.draws);
		assertTrue("every scripted value was used", random.doubles.isEmpty() && random.ints.isEmpty());
		assertEquals("0.3 is under the chance of 0.5", Status.POISON, foe.status());
	}

	@Test
	public void aStunnedCreatureMakesTheStunRollBeforeThePick()
	{
		Scripted random = new Scripted(0.1);
		Combatant mine = fighter(30, WEAK);
		mine.afflict(Status.STUN, new Random(1));
		BattleState state = duel(mine, dummy(10), random);

		TurnResult turn = state.step(BattleChoice.attack());

		List<String> said = texts(turn);
		assertTrue(said.toString(), said.contains("Fighter is stunned and cannot move"));
		assertEquals("one stun roll, then the foe's blow: one spread", 2, random.draws);
	}

	@Test
	public void aMoveWithNoEffectMakesNoEffectDraw()
	{
		Scripted random = new Scripted();
		BattleState state = duel(fighter(30, WEAK), dummy(10), random);

		TurnResult turn = state.step(BattleChoice.attack());

		assertEquals("one spread draw per blow and nothing else", turn.hits().size(), random.draws);
	}

	@Test
	public void aPoisonousCreatureIsImmuneAndTheRollIsNotMade()
	{
		Scripted random = new Scripted();
		Combatant mine = fighter(30, TestMoves.causing("sting", Status.POISON, 1.0));
		Combatant spider = new Combatant(SPIDER, "Spider", 10, 1.0);
		BattleState state = duel(mine, spider, random);

		state.step(BattleChoice.attack());

		assertNull(spider.status());
		assertEquals("my spread and the spider's spread; its hit has nothing to roll against a fresh dummy", 3,
			random.draws);
	}

	@Test
	public void aPoisonousCreaturesMeleeHitPoisonsAboutAFifthOfTheTime()
	{
		int poisoned = 0;
		for (int seed = 0; seed < 1000; seed++)
		{
			Combatant victim = dummy(60);
			Combatant spider = new Combatant(SPIDER, "Spider", 60, 1.0);
			BattleState state = duel(spider, victim, new Random(seed * 7919L + 5));
			state.step(BattleChoice.attack());
			poisoned += victim.status() == Status.POISON ? 1 : 0;
		}

		double share = poisoned / 1000.0;
		assertTrue("about 20%: " + share, share > 0.15 && share < 0.25);
	}

	@Test
	public void aMagicHitFromAPoisonousCreatureDoesNotPoison()
	{
		Move spit = new Move("spit", "Spit", CombatType.MAGIC, 1.0, 1.0);
		int poisoned = 0;
		for (int seed = 0; seed < 200; seed++)
		{
			Combatant victim = dummy(60);
			Combatant spider = new Combatant(SPIDER, "Spider", 60, 1.0).moves(Collections.singletonList(spit));
			duel(spider, victim, new Random(seed)).step(BattleChoice.attack());
			poisoned += victim.status() == null ? 0 : 1;
		}

		assertEquals(0, poisoned);
	}

	@Test
	public void aCreatureCanFaintFromPoisonAndThatEndsTheDuelNormally()
	{
		Combatant mine = new Combatant(FIGHTER, "Fighter", 99, 1.0)
			.moves(Collections.singletonList(new Move("tap", "Tap", CombatType.STAB, 0.1, 1.0)));
		Combatant foe = new Combatant(DUMMY, "Dummy", 60, 0.04);
		foe.afflict(Status.POISON, new Random(1));
		BattleState state = new BattleState(Collections.singletonList(mine), Collections.singletonList(foe),
			new Random(2), BattleState.Mode.TEAM);

		TurnResult turn = state.step(BattleChoice.attack());

		assertTrue(foe.fainted());
		assertEquals(TurnResult.Status.WON, turn.status());
		assertTrue(texts(turn).toString(), texts(turn).stream().anyMatch(t -> t.startsWith("Dummy is hurt by poison")));
		assertNull("a status ends with the pairing", foe.status());
	}

	@Test
	public void switchingOutClearsTheStatus()
	{
		Combatant first = fighter(30, WEAK);
		Combatant second = new Combatant(FIGHTER, "Second", 30, 1.0);
		first.afflict(Status.BURN, new Random(1));
		BattleState state = new BattleState(Arrays.asList(first, second), Collections.singletonList(dummy(10)),
			new Random(3), BattleState.Mode.WILD);

		state.step(BattleChoice.switchTo(1));

		assertNull(first.status());
		Combatant entered = state.team().get(1);
		assertNull(entered.status());
	}

	@Test
	public void aBattleFullOfStatusesIsTheSameForTheSameSeed()
	{
		for (long seed = 0; seed < 20; seed++)
			assertEquals(play(seed), play(seed));

		boolean any = false;
		for (long seed = 0; seed < 20; seed++)
			any |= play(seed).stream().anyMatch(t -> t.contains("hurt by") || t.contains("frozen"));

		assertTrue("statuses really are in play", any);
	}

	private static List<String> play(long seed)
	{
		List<Move> moves = Arrays.asList(WEAK, TestMoves.causing("sting", Status.POISON, 0.4),
			TestMoves.causing("ice", Status.FREEZE, 0.3), TestMoves.causing("bind", Status.STUN, 0.4),
			TestMoves.causing("burn", Status.BURN, 0.3), TestMoves.causing("bite", Status.VENOM, 0.3));
		BattleState state = duel(new Combatant(FIGHTER, "Fighter", 25, 1.0).moves(moves),
			new Combatant(DUMMY, "Dummy", 30, 1.0).moves(moves), new Random(seed * 31 + 7));
		List<String> texts = new ArrayList<>();
		while (state.ended() == null)
			texts.addAll(texts(state.step(BattleChoice.attack())));

		return texts;
	}

	@Test
	public void statusLinesCarryTheTagsAndBecomeBeats()
	{
		Combatant foe = dummy(40);
		BattleState state = duel(fighter(40, TestMoves.causing("sting", Status.POISON, 1.0)), foe, new Random(4));

		List<BattleLine> lines = new ArrayList<>(state.step(BattleChoice.attack()).hits());
		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(lines);

		BattleTimeline.Step afflicted = null;
		BattleTimeline.Step dot = null;
		for (BattleTimeline.Step step : steps)
		{
			if (step.beat() == BattleTimeline.Beat.TAG && afflicted == null)
				afflicted = step;
			else if (step.beat() == BattleTimeline.Beat.DOT)
				dot = step;
		}

		assertEquals("one beat for each line that is a beat", lines.size(), steps.size());
		assertTrue(afflicted != null && afflicted.tagged());
		assertEquals(Status.POISON, afflicted.foeStatus());
		assertFalse("the poisoned one is the other creature", afflicted.onLead());
		assertTrue(dot != null && !dot.onLead());
		assertEquals(Status.POISON, dot.foeStatus());
	}

	@Test
	public void aSkippedTurnIsASkipBeatOverTheCreatureThatCannotMove()
	{
		Combatant mine = fighter(30, WEAK);
		mine.afflict(Status.FREEZE, new Random(1));
		BattleState state = duel(mine, dummy(10), new Random(4));

		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(state.step(BattleChoice.attack()).hits());

		assertEquals(BattleTimeline.Beat.SKIP, steps.get(0).beat());
		assertTrue(steps.get(0).onLead());
	}
}
