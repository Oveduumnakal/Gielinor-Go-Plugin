/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks that nothing progresses until a starter is picked, and that nothing earned before is paid after. */
public class StarterLockTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private final Gson gson = new Gson();
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, new Species("Imp", 7, CombatType.SLASH),
			new Species("Gnome Archer", 5, CombatType.RANGED), new Species("Wizard", 9, CombatType.MAGIC)));
		game = new GoGame(table, new Random(11), () -> now, notice -> messages.add(notice.text()));
		game.starterRandom(new Random(5));
	}

	private String saved()
	{
		return gson.toJson(game.data());
	}

	private static WildEntry wildGoblin()
	{
		return new WildEntry(1, GOBLIN, 3, 5);
	}

	@Test
	public void aFreshSaveIsLockedAndAPickUnlocksIt()
	{
		assertTrue(game.locked());
		assertTrue(game.pickStarter(0));
		assertFalse(game.locked());
	}

	@Test
	public void walkingSkillingAndXpChangeNothingWhileLocked()
	{
		game.data().eggs.add(new Egg(Rarity.COMMON));
		String before = saved();

		game.addSteps(50_000);
		game.addActionSteps(50_000);
		game.addOsrsXp(5_000_000);
		for (int i = 0; i < 200; i++)
			game.addSkillXp("MINING", 5_000);

		game.onLevelUp();
		game.addOfflineXp(OfflineXp.compute(Collections.singletonMap("MINING", 1_000),
			Collections.singletonMap("MINING", 9_000_000), new Random(3)));
		game.bossKilled("King Black Dragon");

		assertEquals(before, saved());
		assertFalse(game.carriesEgg());
		assertTrue(messages.isEmpty());
	}

	@Test
	public void aWildCreatureIsRefusedAndNotRecorded()
	{
		String before = saved();

		assertTrue("still labelled as new", game.markSeen(wildGoblin()));
		assertEquals(CatchOutcome.LOCKED, game.tryCatch(wildGoblin()));
		assertNull(game.startAutoBattle(wildGoblin()));
		assertNull(game.startBattle(wildGoblin()));

		assertEquals(before, saved());
		assertEquals(Collections.nCopies(3, GoGame.STARTER_FIRST_TEXT), messages);
	}

	@Test
	public void theXpSnapshotStillKeepsUp()
	{
		assertTrue(game.stampXp(Collections.singletonMap("MINING", 9_000)));
		assertEquals(Integer.valueOf(9_000), game.xpSnapshot().get("MINING"));
	}

	@Test
	public void nothingEarnedWhileLockedIsPaidAfterThePick()
	{
		game.addSteps(GoGame.TILES_PER_JAR * 3 - 1);
		assertTrue(game.pickStarter(0));
		int jars = game.data().jarCount(JarType.BRONZE);
		game.addSteps(1);

		assertEquals(jars, game.data().jarCount(JarType.BRONZE));
		assertEquals(1, game.data().stepProgress);
	}

	@Test
	public void walkingAndCatchingWorkAfterThePick()
	{
		assertTrue(game.pickStarter(0));
		int jars = game.data().jarCount(JarType.BRONZE);
		game.addSteps(GoGame.TILES_PER_JAR);

		assertEquals(jars + 1, game.data().jarCount(JarType.BRONZE));
		assertTrue(game.markSeen(wildGoblin()));
		assertTrue(game.data().seen.contains("goblin"));
		assertTrue(game.tryCatch(wildGoblin()) != CatchOutcome.LOCKED);
	}

	@Test
	public void aSaveWithOnlyACaughtSpeciesIsNotLocked()
	{
		game.data().caught.put("goblin", 1);

		assertFalse(game.locked());
	}

	@Test
	public void aResetLocksAgain()
	{
		assertTrue(game.pickStarter(0));
		game.reset();

		assertTrue(game.locked());
	}

	@Test
	public void tradesAreRefusedWhileLocked()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(GOBLIN, new Species("Imp", 7, CombatType.SLASH),
			new Species("Gnome Archer", 5, CombatType.RANGED), new Species("Wizard", 9, CombatType.MAGIC)));
		List<GielinorGoPartyMessage> fromLocked = new ArrayList<>();
		List<GielinorGoPartyMessage> fromFriend = new ArrayList<>();
		FriendTrades mine = new FriendTrades(table, game, new Random(1), () -> now, fromLocked::add,
			notice -> messages.add(notice.text()));
		GoGame friendGame = new GoGame(table, new Random(2), () -> now, notice ->
		{
		});
		Creature goblin = new Creature("goblin", 5, now);
		friendGame.data().team.add(goblin);
		FriendTrades friend = new FriendTrades(table, friendGame, new Random(3), () -> now, fromFriend::add,
			notice ->
			{
			});
		Egg egg = new Egg(Rarity.COMMON);
		game.data().eggs.add(egg);
		String before = saved();

		mine.offer(22L, "Bob", egg, null);
		assertTrue(fromLocked.isEmpty());
		assertEquals(Collections.singletonList(GoGame.STARTER_FIRST_TEXT), messages);

		friend.offer(11L, "Ann", goblin, null);
		GielinorGoPartyMessage offer = fromFriend.get(0);
		offer.setMemberId(22L);

		assertFalse(mine.receive(offer, 11L, "Bob"));
		assertEquals(GielinorGoPartyMessage.REFUSE, fromLocked.get(0).kind());
		assertEquals("", mine.offerFrom(22L));
		assertEquals(before, saved());
	}

	@Test
	public void aTableWithoutTheStartersNeverLocks()
	{
		GoGame bare = new GoGame(new SpeciesTable(Arrays.asList(GOBLIN)), new Random(1), () -> now,
			notice -> messages.add(notice.text()));

		assertFalse(bare.locked());
	}
}
