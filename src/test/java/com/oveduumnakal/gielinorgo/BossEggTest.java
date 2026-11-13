/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks boss eggs in the game: first kill, waiting in a full bag, hatching, saving and trading. */
public class BossEggTest
{
	private static final long ANN = 11L;
	private static final long BOB = 22L;

	private static final Species IMP = new Species("Imp", 7, CombatType.STAB);
	private static final Species VORKATH = new Species("Vorkath", 732, CombatType.STAB);
	private static final Species MOLE = new Species("Giant Mole", 230, CombatType.STAB);

	private final SpeciesTable table = new SpeciesTable(Arrays.asList(IMP, VORKATH, MOLE));
	private final List<String> told = new ArrayList<>();
	private final List<String> bobTold = new ArrayList<>();
	private final List<GielinorGoPartyMessage> fromAnn = new ArrayList<>();
	private BossTable bosses;
	private GoGame game;

	@Before
	public void setUp()
	{
		Map<String, String> names = new HashMap<>();
		names.put("Vorkath", "Vorkath");
		names.put("Giant Mole", "Giant Mole");
		bosses = new BossTable(names, table);
		game = new GoGame(table, new Random(5), () -> 0L, notice -> told.add(notice.text()));
		game.bosses(bosses);
	}

	private void fillBag()
	{
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			game.data().eggs.add(new Egg(Rarity.COMMON));
	}

	@Test
	public void theFirstKillAlwaysGivesTheEggAndIsRecorded()
	{
		game.bossKilled("Vorkath");

		assertEquals(1, game.data().eggs.size());
		Egg egg = game.data().eggs.get(0);
		assertEquals("vorkath", egg.species);
		assertEquals(VORKATH.rarity(), egg.rarityTier());
		assertEquals(VORKATH.rarity().eggTiles(), egg.tilesLeft);
		assertTrue(game.data().bossKills.contains("vorkath"));
		assertEquals("You found a Vorkath egg!", told.get(told.size() - 1));
		assertTrue(game.takeDirty());
	}

	@Test
	public void laterKillsMostlyGiveNothing()
	{
		game.bossKilled("Vorkath");
		game.data().eggs.clear();
		told.clear();

		int eggs = 0;
		for (int i = 0; i < 100; i++)
		{
			game.bossKilled("Vorkath");
			eggs += game.data().eggs.size();
			game.data().eggs.clear();
		}

		assertTrue("eggs " + eggs, eggs > 0 && eggs < 20);
	}

	@Test
	public void aNameNotInTheTableDoesNothing()
	{
		game.bossKilled("Chicken");
		game.bossKilled(null);

		assertTrue(game.data().eggs.isEmpty());
		assertTrue(game.data().bossKills.isEmpty());
		assertTrue(told.isEmpty());
	}

	@Test
	public void aFullBagMakesTheEggWaitUntilASlotIsFree()
	{
		fillBag();
		game.bossKilled("Giant Mole");
		game.bossKilled("Giant Mole");

		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());
		assertTrue("not paid until it is in the bag", game.data().bossKills.isEmpty());
		assertTrue(told.isEmpty());

		game.addSteps(1);
		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());

		game.giveAway(game.data().eggs.get(0));
		game.addSteps(1);

		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());
		Egg last = game.data().eggs.get(SaveData.MAX_EGGS - 1);
		assertEquals("giant mole", last.species);
		assertTrue(game.data().bossKills.contains("giant mole"));
		assertEquals("You found a Giant Mole egg!", told.get(told.size() - 1));

		game.giveAway(game.data().eggs.get(0));
		game.addSteps(1);
		assertEquals("a waiting egg is there once per boss", SaveData.MAX_EGGS - 1, game.data().eggs.size());
	}

	@Test
	public void aBossEggHatchesIntoThatSpeciesAtLevelOne()
	{
		game.bossKilled("Vorkath");
		Egg carried = game.data().eggs.get(0);
		carried.tilesLeft = 1;

		game.addSteps(1);

		assertTrue(game.data().eggs.isEmpty());
		assertEquals(1, game.data().team.size());
		Creature born = game.data().team.get(0);
		assertEquals("vorkath", born.species);
		assertEquals(1, born.level());
		assertEquals(1, (int) game.data().caught.get("vorkath"));
	}

	@Test
	public void anOrdinaryEggStillHatchesAtRandomOfItsRarity()
	{
		Egg egg = new Egg(Rarity.COMMON);
		egg.tilesLeft = 1;
		game.data().eggs.add(egg);

		game.addSteps(1);

		assertNull(egg.species);
		Creature born = game.data().team.get(0);
		assertEquals("imp", born.species);
	}

	@Test
	public void anOldSaveLoadsWithNoSpeciesAndNoBossKills()
	{
		Gson gson = new Gson();
		String json = "{\"version\":1,\"eggs\":[{\"rarity\":\"RARE\",\"tilesLeft\":5,\"tilesTotal\":9}]}";
		SaveData old = gson.fromJson(json, SaveData.class).sanitized();

		assertNull(old.eggs.get(0).species);
		assertTrue(old.bossKills.isEmpty());
		SaveData none = gson.fromJson("{\"bossKills\":null}", SaveData.class).sanitized();
		assertTrue(none.bossKills.isEmpty());
	}

	@Test
	public void theSaveKeepsTheSpeciesAndTheBossKills()
	{
		Gson gson = new Gson();
		game.bossKilled("Vorkath");

		SaveData loaded = gson.fromJson(gson.toJson(game.data()), SaveData.class).sanitized();

		assertEquals("vorkath", loaded.eggs.get(0).species);
		assertTrue(loaded.bossKills.contains("vorkath"));
	}

	@Test
	public void thePanelNamesABossEgg()
	{
		game.bossKilled("Vorkath");
		game.data().eggs.add(new Egg(Rarity.RARE));

		List<EggView> eggs = game.snapshot().eggs();

		assertEquals("Vorkath egg", eggs.get(0).label());
		assertEquals("Rare egg", eggs.get(1).label());
	}

	private GoGame bobGame()
	{
		GoGame bob = new GoGame(table, new Random(2), () -> 0L, notice -> bobTold.add(notice.text()));
		bob.bosses(bosses);
		return bob;
	}

	private FriendTrades tradesOf(GoGame own, long member, List<String> notes)
	{
		FriendTrades trades = new FriendTrades(table, own, new Random(3), () -> 1_000_000L, message ->
		{
			message.setMemberId(member);
			fromAnn.add(message);
		}, notice -> notes.add(notice.text()));
		own.tradeLocks(trades);
		return trades;
	}

	@Test
	public void aBossEggTradesWithItsSpecies()
	{
		game.bossKilled("Vorkath");
		FriendTrades ann = tradesOf(game, ANN, told);
		GoGame bobGame = bobGame();
		FriendTrades bob = tradesOf(bobGame, BOB, bobTold);

		ann.offer(BOB, "Bob", game.data().eggs.get(0), null);
		assertEquals("Offered Vorkath egg to Bob", told.get(told.size() - 1));
		assertTrue(bob.receive(fromAnn.get(fromAnn.size() - 1), BOB, "Ann"));
		assertEquals("Vorkath egg as a gift", bob.offerFrom(ANN));
	}

	@Test
	public void aTradedBossEggStillHatchesIntoItsSpecies()
	{
		game.bossKilled("Vorkath");
		FriendTrades ann = tradesOf(game, ANN, told);
		GoGame bobGame = bobGame();
		FriendTrades bob = tradesOf(bobGame, BOB, bobTold);
		ann.offer(BOB, "Bob", game.data().eggs.get(0), null);
		bob.receive(fromAnn.get(fromAnn.size() - 1), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(fromAnn.get(fromAnn.size() - 1), ANN, "Bob");
		bob.receive(fromAnn.get(fromAnn.size() - 1), BOB, "Ann");

		assertTrue(game.data().eggs.isEmpty());
		assertEquals(1, bobGame.data().eggs.size());
		Egg arrived = bobGame.data().eggs.get(0);
		assertEquals("vorkath", arrived.species);
	}

	@Test
	public void anEggWithASpeciesNoBossGivesIsRefused()
	{
		FriendTrades bob = tradesOf(bobGame(), BOB, bobTold);

		assertFalse(bob.receive(offerOf(GielinorGoPartyMessage.Item.egg("RARE", 0, "imp")), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(GielinorGoPartyMessage.Item.egg("RARE", 0, "nobody")), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(GielinorGoPartyMessage.Item.egg("COMMON", 0, "vorkath")), BOB, "Ann"));
		assertEquals("", bob.offerFrom(ANN));

		assertTrue(bob.receive(offerOf(GielinorGoPartyMessage.Item.egg(VORKATH.rarity().name(), 0, "vorkath")), BOB,
			"Ann"));
	}

	@Test
	public void anOfferFromAnOldProtocolIsRefused()
	{
		FriendTrades bob = tradesOf(bobGame(), BOB, bobTold);
		Gson gson = new Gson();
		GielinorGoPartyMessage offer = offerOf(GielinorGoPartyMessage.Item.egg("COMMON", 0));
		String wire = gson.toJson(offer).replace("\"version\":" + GielinorGoPartyMessage.PROTOCOL, "\"version\":4");
		GielinorGoPartyMessage old = gson.fromJson(wire, GielinorGoPartyMessage.class);
		old.setMemberId(ANN);

		assertEquals(9, GielinorGoPartyMessage.PROTOCOL);
		assertFalse(bob.receive(old, BOB, "Ann"));
		assertEquals("", bob.offerFrom(ANN));
		assertEquals("Ann runs a different version of Gielinor Go", bobTold.get(bobTold.size() - 1));
	}

	private static GielinorGoPartyMessage offerOf(GielinorGoPartyMessage.Item item)
	{
		GielinorGoPartyMessage offer = new GielinorGoPartyMessage(GielinorGoPartyMessage.OFFER, BOB, 9, 0, 0,
			new String[0], new int[0]);
		offer.giving(item);
		offer.setMemberId(ANN);
		return offer;
	}
}
