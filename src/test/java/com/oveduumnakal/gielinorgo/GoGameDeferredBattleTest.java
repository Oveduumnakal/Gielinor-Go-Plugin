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

/** A battle that plays itself writes its result only when it is finished, once, after the screen has shown it. */
public class GoGameDeferredBattleTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, DRAGON));
		game = new GoGame(table, new EvolutionTable(Arrays.asList(), table), new ChampionTable(Arrays.asList(), table),
			new Random(5), () -> now, notice -> messages.add(notice.text()));
		game.setBankNear(true);
	}

	private static WildEntry wild(Species species, int level, int combatLevel)
	{
		return new WildEntry(1, species, level, combatLevel);
	}

	private Creature first()
	{
		return game.data().team.get(0);
	}

	@Test
	public void aWonBattleWritesNothingUntilItIsFinished()
	{
		game.data().team.add(new Creature("chicken", 90, now));
		WildEntry foe = wild(CHICKEN, 1, 1);
		long xp = first().xp;

		BattleResult result = game.battleDeferred(foe);

		assertTrue(result.won());
		assertNotNull(result.pending());
		assertFalse("not battled yet", foe.battled());
		assertEquals(xp, first().xp);
		assertTrue(messages.toString(), messages.isEmpty());

		assertTrue(game.finishDeferred(result.pending()));

		assertTrue(foe.battled());
		assertTrue(first().xp > xp);
	}

	@Test
	public void aLostBattleFaintsTheCreatureOnlyWhenItIsFinished()
	{
		game.data().team.add(new Creature("chicken", 1, now));
		WildEntry foe = wild(DRAGON, 60, 276);

		BattleResult result = game.battleDeferred(foe);

		assertFalse(result.won());
		assertFalse("still up on screen", first().fainted);
		assertTrue(messages.isEmpty());

		game.finishDeferred(result.pending());

		assertTrue(first().fainted);
		assertTrue(messages.toString(), messages.contains("Chicken fainted. Revive it at a bank"));
	}

	@Test
	public void theResultIsWrittenExactlyOnce()
	{
		game.data().team.add(new Creature("chicken", 90, now));
		BattleResult result = game.battleDeferred(wild(CHICKEN, 1, 1));

		assertTrue(game.finishDeferred(result.pending()));
		long xp = first().xp;
		int told = messages.size();

		assertFalse(game.finishDeferred(result.pending()));
		assertEquals(xp, first().xp);
		assertEquals(told, messages.size());
	}

	@Test
	public void aBattleThatWasNotDeferredHasNothingPending()
	{
		game.data().team.add(new Creature("chicken", 90, now));

		BattleResult result = game.battle(wild(CHICKEN, 1, 1));

		assertNull(result.pending());
	}

	@Test
	public void aDeferredBattleWorksOutTheSameResultAsAnImmediateOne()
	{
		GoGame other = new GoGame(new SpeciesTable(Arrays.asList(CHICKEN, DRAGON)),
			new EvolutionTable(Arrays.asList(), new SpeciesTable(Arrays.asList(CHICKEN, DRAGON))),
			new ChampionTable(Arrays.asList(), new SpeciesTable(Arrays.asList(CHICKEN, DRAGON))), new Random(5),
			() -> now, notice -> messages.add(notice.text()));
		other.data().team.add(new Creature("chicken", 20, now));
		game.data().team.add(new Creature("chicken", 20, now));

		BattleResult now1 = other.battle(wild(DRAGON, 30, 100));
		BattleResult later = game.battleDeferred(wild(DRAGON, 30, 100));

		assertEquals(now1.won(), later.won());
		assertEquals(now1.log(), later.log());
	}

	@Test
	public void aResultSettledBeforeAProfileChangeIsKeptAndNotWrittenAgain()
	{
		game.data().team.add(new Creature("chicken", 90, now));
		WildEntry foe = wild(CHICKEN, 1, 1);
		BattleResult result = game.battleDeferred(foe);
		BattleSession owed = result.pending();
		long before = first().xp;

		assertTrue("settled before the other profile loads", game.finishDeferred(owed));
		long paid = first().xp;
		assertTrue(paid > before);

		assertFalse("a second settle after the reload pays nothing", game.finishDeferred(owed));
		assertEquals(paid, first().xp);
	}
}
