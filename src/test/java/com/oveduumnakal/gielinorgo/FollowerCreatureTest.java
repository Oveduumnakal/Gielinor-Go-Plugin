/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The creature that follows the player and leads a fight: the lead, or the first one standing when it fainted. */
public class FollowerCreatureTest
{
	private static final long NOW = 1_000_000L;

	private GoGame game;
	private Creature first;
	private Creature second;
	private Creature third;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB)));
		game = new GoGame(table, new Random(3), () -> NOW, notice ->
		{
		});
		first = new Creature("chicken", 5, NOW);
		second = new Creature("chicken", 6, NOW);
		third = new Creature("chicken", 7, NOW);
		game.data().team.addAll(Arrays.asList(first, second, third));
	}

	@Test
	public void aLeadThatStandsFollows()
	{
		assertSame(first, game.followerCreature());
	}

	@Test
	public void aFaintedLeadIsReplacedByTheFirstCreatureThatStands()
	{
		first.faint(NOW);

		assertSame(second, game.followerCreature());

		second.faint(NOW);

		assertSame(third, game.followerCreature());
	}

	@Test
	public void nobodyFollowsWhenEveryoneHasFainted()
	{
		first.faint(NOW);
		second.faint(NOW);
		third.faint(NOW);

		assertNull(game.followerCreature());
	}

	@Test
	public void anEmptyTeamHasNoFollower()
	{
		game.data().team.clear();

		assertNull(game.followerCreature());
	}

	@Test
	public void makingAFaintedCreatureTheLeadIsAllowedAndTheNextOneStillFollows()
	{
		second.faint(NOW);

		game.setLead(1);

		assertSame(second, game.data().lead());
		assertSame(first, game.followerCreature());
		assertTrue(second.fainted);
	}
}
