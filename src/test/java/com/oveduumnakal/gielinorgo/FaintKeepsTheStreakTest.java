/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Fainting is bookkeeping after the fight: it draws no random number and changes no battle. */
public class FaintKeepsTheStreakTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private static List<String> play(boolean benchFainted, long[] next)
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, DRAGON));
		Random random = new Random(99);
		GoGame game = new GoGame(table, random, () -> 1_000_000L, notice ->
		{
		});
		game.data().team.add(new Creature("goblin", 40, 1_000_000L));
		if (benchFainted)
		{
			Creature bench = new Creature("goblin", 40, 1_000_000L);
			bench.faint(1_000_000L);
			game.data().team.add(bench);
		}

		BattleResult result = game.battle(new WildEntry(1, DRAGON, 60, 276));
		List<String> lines = new ArrayList<>(result.log());
		next[0] = random.nextLong();
		return lines;
	}

	@Test
	public void aFaintedCreatureOnTheTeamLeavesTheBattleLinesAndTheNextDrawUntouched()
	{
		long[] plain = new long[1];
		long[] withFainted = new long[1];

		List<String> alone = play(false, plain);
		List<String> withBench = play(true, withFainted);

		assertTrue(alone.size() > 1);
		assertEquals(alone, withBench);
		assertEquals(plain[0], withFainted[0]);
	}
}
