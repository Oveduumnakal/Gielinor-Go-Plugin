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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Rearranging storage from the Box view: swap, move to the end, stale picks, and the saved order. */
public class GoGameStorageArrangeTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private Creature a;
	private Creature b;
	private Creature c;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, CHICKEN));
		game = new GoGame(table, new Random(5), () -> 1_000_000L, notice -> messages.add(notice.text()));
		a = new Creature("goblin", 5, 0);
		b = new Creature("chicken", 6, 0);
		c = new Creature("goblin", 7, 0);
		game.data().storage.addAll(Arrays.asList(a, b, c));
		game.takeDirty();
	}

	private TradePick pick(int place)
	{
		List<CreatureView> stored = game.snapshot().storage();
		return TradePick.of(Holding.STORAGE, place, stored.get(place));
	}

	@Test
	public void dropOnAnotherCreatureSwapsTheTwoAndSavesTheOrder()
	{
		game.arrangeStorage(pick(0), pick(2));

		assertEquals(Arrays.asList(c, b, a), game.data().storage);
		assertTrue(game.takeDirty());
	}

	@Test
	public void dropOnAnEmptyCellMovesTheCreatureToTheEnd()
	{
		game.arrangeStorage(pick(0), null);

		assertEquals(Arrays.asList(b, c, a), game.data().storage);
		assertTrue(game.takeDirty());
	}

	@Test
	public void theStorageListObjectStaysTheSame()
	{
		List<Creature> before = game.data().storage;

		game.arrangeStorage(pick(1), pick(2));

		assertSame(before, game.data().storage);
	}

	@Test
	public void aStalePickChangesNothingAndSaysSo()
	{
		TradePick stale = pick(0);
		game.data().storage.remove(0);
		game.data().storage.add(0, new Creature("chicken", 40, 0));
		game.takeDirty();

		game.arrangeStorage(stale, pick(2));

		List<Creature> now = game.data().storage;
		assertEquals(Arrays.asList(now.get(0), b, c), now);
		assertEquals(c, game.data().storage.get(2));
		assertTrue(messages.contains("That creature moved. Try again."));
		assertFalse(game.takeDirty());
	}

	@Test
	public void aStaleDropTargetChangesNothingToo()
	{
		TradePick target = pick(2);
		game.data().storage.remove(2);
		game.takeDirty();

		game.arrangeStorage(pick(0), target);

		assertEquals(Arrays.asList(a, b), game.data().storage);
		assertTrue(messages.contains("That creature moved. Try again."));
		assertFalse(game.takeDirty());
	}

	@Test
	public void onlyStoragePicksAreAccepted()
	{
		game.data().team.add(new Creature("goblin", 9, 0));
		List<CreatureView> members = game.snapshot().team();
		TradePick team = TradePick.of(Holding.TEAM, 0, members.get(0));

		TradePick stored = pick(1);
		game.takeDirty();

		game.arrangeStorage(team, stored);
		game.arrangeStorage(stored, team);

		assertEquals(Arrays.asList(a, b, c), game.data().storage);
		assertFalse(game.takeDirty());
	}

	@Test
	public void droppingOnItselfOrTheLastOnTheEndChangesNothing()
	{
		TradePick middle = pick(1);
		TradePick last = pick(2);
		game.takeDirty();

		game.arrangeStorage(middle, middle);
		game.arrangeStorage(last, null);

		assertEquals(Arrays.asList(a, b, c), game.data().storage);
		assertFalse(game.takeDirty());
	}

	@Test
	public void aCreaturePromisedInATradeMayBeMovedAndStaysPromised()
	{
		game.tradeLocks(new GoGame.TradeLocks()
		{
			@Override
			public boolean locked(Creature creature)
			{
				return creature == a;
			}
		});

		game.arrangeStorage(pick(0), pick(2));

		assertEquals(Arrays.asList(c, b, a), game.data().storage);
	}
}
