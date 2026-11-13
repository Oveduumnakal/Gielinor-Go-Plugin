/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.oveduumnakal.gielinorgo.GielinorGoPartyMessage.Item;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Trades creatures and eggs between two players whose messages are handed to each other by the test. */
public class FriendTradesTest
{
	private static final long ANN = 11L;
	private static final long BOB = 22L;
	private static final long CAT = 33L;
	private static final Species IMP = new Species("Imp", 7, CombatType.MELEE);
	private static final Species COW = new Species("Cow", 2, CombatType.MELEE);

	private final SpeciesTable table = new SpeciesTable(Arrays.asList(IMP, COW));
	private final List<GielinorGoPartyMessage> fromAnn = new ArrayList<>();
	private final List<GielinorGoPartyMessage> fromBob = new ArrayList<>();
	private final List<String> annTold = new ArrayList<>();
	private final List<String> bobTold = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame annGame;
	private GoGame bobGame;
	private FriendTrades ann;
	private FriendTrades bob;
	private Creature annImp;
	private Creature bobCow;

	@Before
	public void setUp()
	{
		annGame = new GoGame(table, new Random(1), () -> now, notice -> annTold.add(notice.text()));
		bobGame = new GoGame(table, new Random(2), () -> now, notice -> bobTold.add(notice.text()));
		annGame.setBankNear(true);
		bobGame.setBankNear(true);
		ann = new FriendTrades(table, annGame, new Random(3), () -> now, message -> fromAnn.add(sentBy(message, ANN)),
			notice -> annTold.add(notice.text()));
		bob = new FriendTrades(table, bobGame, new Random(4), () -> now, message -> fromBob.add(sentBy(message, BOB)),
			notice -> bobTold.add(notice.text()));
		annGame.tradeLocks(ann);
		bobGame.tradeLocks(bob);
		annImp = new Creature("imp", 20, now);
		annImp.gilded = true;
		annImp.bond = 4000;
		annGame.data().team.add(annImp);
		bobCow = new Creature("cow", 5, now);
		bobGame.data().team.add(bobCow);
		annGame.takeDirty();
		bobGame.takeDirty();
	}

	@Test
	public void aSwapMovesBothCreaturesOnceBothSidesAgree()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		assertEquals("Offered gilded Imp (Lv 20) to Bob", last(annTold));
		assertTrue(ann.waitingOn(BOB));

		assertTrue(bob.receive(last(fromAnn), BOB, "Ann"));
		assertEquals("Ann offers a trade. See the Battle tab", last(bobTold));
		assertEquals("gilded Imp (Lv 20) for your Cow (Lv 5)", bob.offerFrom(ANN));
		assertSame("nothing moves before Bob accepts", bobCow, bobGame.data().team.get(0));

		bob.answer(ANN, "Ann", true);
		assertEquals(GielinorGoPartyMessage.AGREE, last(fromBob).kind());
		assertSame("nothing moves before Ann confirms", bobCow, bobGame.data().team.get(0));
		assertEquals("", bob.offerFrom(ANN));

		assertTrue(ann.receive(last(fromBob), ANN, "Bob"));
		assertEquals("Traded gilded Imp (Lv 20) for Bob's Cow (Lv 5)", last(annTold));
		assertEquals(GielinorGoPartyMessage.CONFIRM, last(fromAnn).kind());
		assertFalse(ann.waitingOn(BOB));
		assertTrue(bob.receive(last(fromAnn), BOB, "Ann"));
		assertEquals("Traded Cow (Lv 5) for Ann's gilded Imp (Lv 20)", last(bobTold));

		Creature annGot = only(annGame.data().team);
		Creature bobGot = only(bobGame.data().team);
		assertEquals("cow", annGot.species);
		assertEquals(5, annGot.level());
		assertEquals("imp", bobGot.species);
		assertEquals(annImp.xp, bobGot.xp);
		assertTrue("a gilded creature stays gilded", bobGot.gilded);
		assertEquals("a traded creature arrives with no bond", 0, bobGot.bond);
		assertTrue(annGame.takeDirty());
		assertTrue(bobGame.takeDirty());
		assertTrue(ann.takeTraded());
		assertTrue(bob.takeTraded());
		assertFalse(bob.takeTraded());
	}

	@Test
	public void aTradedCreatureArrivesWithNoSourceNpc()
	{
		annImp.npcId = 101;

		swap();

		Creature bobGot = only(bobGame.data().team);
		assertEquals(0, bobGot.npcId);
		assertEquals(101, annImp.npcId);
	}

	@Test
	public void aTradedCreatureCountsInTheCreatureLog()
	{
		swap();

		assertEquals(Integer.valueOf(1), bobGame.data().caught.get("imp"));
		assertTrue(bobGame.data().gilded.contains("imp"));
		assertEquals(Integer.valueOf(1), annGame.data().caught.get("cow"));
	}

	@Test
	public void aGiftAsksForNothingBack()
	{
		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		assertEquals("gilded Imp (Lv 20) as a gift", bob.offerFrom(ANN));

		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		assertEquals("Gave gilded Imp (Lv 20) to Bob", last(annTold));
		assertEquals("Ann gave you gilded Imp (Lv 20)", last(bobTold));
		assertTrue(annGame.data().team.isEmpty());
		assertEquals(2, bobGame.data().team.size());
	}

	@Test
	public void aTradedCreatureArrivesWithoutItsNickname()
	{
		annImp.nickname = "Spark";

		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		Creature bobGot = only(bobGame.data().team);
		assertEquals("imp", bobGot.species);
		assertNull(bobGot.nickname);
		assertEquals("Spark", annImp.nickname);
	}

	@Test
	public void anEggCanBeTradedForACreature()
	{
		Egg egg = new Egg(Rarity.RARE);
		egg.tilesLeft = 300;
		annGame.data().eggs.add(egg);

		ann.offer(BOB, "Bob", egg, Item.creature("cow", 5, 0, false));
		assertEquals("Offered Rare egg to Bob", last(annTold));
		bob.receive(last(fromAnn), BOB, "Ann");
		assertEquals("Rare egg for your Cow (Lv 5)", bob.offerFrom(ANN));
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		assertTrue(annGame.data().eggs.isEmpty());
		assertEquals(2, annGame.data().team.size());
		Egg moved = only(bobGame.data().eggs);
		assertEquals(Rarity.RARE, moved.rarityTier());
		assertEquals("an egg keeps the walking already done", 300, moved.tilesLeft);
		assertEquals(Rarity.RARE.eggTiles(), moved.tilesTotal);
		assertTrue(bobGame.data().team.isEmpty());
	}

	@Test
	public void anEggCanBeAskedFor()
	{
		bobGame.data().eggs.add(new Egg(Rarity.EPIC));

		ann.offer(BOB, "Bob", annImp, Item.egg("EPIC", 0));
		bob.receive(last(fromAnn), BOB, "Ann");
		assertEquals("gilded Imp (Lv 20) for your Epic egg", bob.offerFrom(ANN));
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		Egg got = only(annGame.data().eggs);
		assertEquals(Rarity.EPIC, got.rarityTier());
		assertTrue(bobGame.data().eggs.isEmpty());
	}

	@Test
	public void decliningChangesNothing()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", false);

		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromBob).kind());
		assertTrue(ann.receive(last(fromBob), ANN, "Bob"));
		assertEquals("Bob turned the trade down", last(annTold));
		assertFalse(ann.waitingOn(BOB));
		assertSame(annImp, only(annGame.data().team));
		assertSame(bobCow, only(bobGame.data().team));
		assertFalse(ann.takeTraded());
	}

	@Test
	public void anOfferForSomethingTheFriendDoesNotHaveIsTurnedDownAtOnce()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 99, 0, false));

		assertFalse(bob.receive(last(fromAnn), BOB, "Ann"));
		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromBob).kind());
		assertEquals("", bob.offerFrom(ANN));
		assertTrue(bobTold.isEmpty());
	}

	@Test
	public void theTradeFallsThroughWhenTheOfferedCreatureIsGone()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		annGame.data().team.clear();

		assertTrue(ann.receive(last(fromBob), ANN, "Bob"));
		assertEquals("The trade with Bob fell through", last(annTold));
		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromAnn).kind());
		assertTrue(bob.receive(last(fromAnn), BOB, "Ann"));
		assertEquals("The trade with Ann fell through", last(bobTold));
		assertSame("Bob keeps his creature", bobCow, only(bobGame.data().team));
		assertTrue(annGame.data().team.isEmpty());
	}

	@Test
	public void acceptingFailsWhenTheAskedCreatureIsGone()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bobGame.data().team.clear();
		bob.answer(ANN, "Ann", true);

		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromBob).kind());
		assertEquals("You no longer have what Ann asked for", last(bobTold));
	}

	@Test
	public void theFriendCannotSwitchWhatTheyGive()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		GielinorGoPartyMessage offer = last(fromAnn);
		GielinorGoPartyMessage cheaper = sentBy(new GielinorGoPartyMessage(GielinorGoPartyMessage.AGREE, ANN,
			offer.battle(), 0, 0, new String[0], new int[0]), BOB);
		cheaper.giving(Item.creature("cow", 1, 0, false));

		assertTrue(ann.receive(cheaper, ANN, "Bob"));
		assertEquals("The trade with Bob fell through", last(annTold));
		assertSame(annImp, only(annGame.data().team));
	}

	@Test
	public void anAgreementNobodyAskedForDoesNothing()
	{
		GielinorGoPartyMessage forged = sentBy(new GielinorGoPartyMessage(GielinorGoPartyMessage.AGREE, ANN, 7, 0, 0,
			new String[0], new int[0]), BOB);
		forged.giving(Item.creature("imp", 99, XpRules.xpForLevel(99), true));

		assertFalse(ann.receive(forged, ANN, "Bob"));
		assertEquals(1, annGame.data().team.size());
	}

	@Test
	public void aConfirmationNobodyAgreedToDoesNothing()
	{
		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		GielinorGoPartyMessage forged = sentBy(new GielinorGoPartyMessage(GielinorGoPartyMessage.CONFIRM, BOB,
			last(fromAnn).battle(), 0, 0, new String[0], new int[0]), ANN);

		assertFalse("Bob has not clicked Accept", bob.receive(forged, BOB, "Ann"));
		assertEquals(1, bobGame.data().team.size());
	}

	@Test
	public void anAnswerFromSomeoneElseDoesNothing()
	{
		ann.offer(BOB, "Bob", annImp, null);
		GielinorGoPartyMessage fromCat = sentBy(new GielinorGoPartyMessage(GielinorGoPartyMessage.AGREE, ANN,
			last(fromAnn).battle(), 0, 0, new String[0], new int[0]), CAT);

		assertFalse(ann.receive(fromCat, ANN, "Cat"));
		assertSame(annImp, only(annGame.data().team));
		assertTrue(ann.waitingOn(BOB));
	}

	@Test
	public void anOfferThatMakesNoSenseIsIgnored()
	{
		assertFalse(bob.receive(offerOf(null), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("dragon of doom", 5, 100, false)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 99, XpRules.xpForLevel(99) + 1, false)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 5, -1, false)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.egg("SHINY", 10)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.egg("RARE", -5)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.egg("RARE", Rarity.RARE.eggTiles() + 1)), BOB, "Ann"));

		assertEquals("", bob.offerFrom(ANN));
		assertTrue(fromBob.isEmpty());
		assertTrue(bobTold.isEmpty());
	}

	@Test
	public void aMessageMeantForSomeoneElseIsIgnored()
	{
		ann.offer(BOB, "Bob", annImp, null);

		assertFalse(bob.receive(last(fromAnn), CAT, "Ann"));
		assertEquals("", bob.offerFrom(ANN));
	}

	@Test
	public void aGiftNeedsRoom()
	{
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			bobGame.data().eggs.add(new Egg(Rarity.COMMON));

		Egg egg = new Egg(Rarity.RARE);
		annGame.data().eggs.add(egg);
		ann.offer(BOB, "Bob", egg, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		int sent = fromBob.size();
		bob.answer(ANN, "Ann", true);

		assertEquals("You cannot carry another egg", last(bobTold));
		assertEquals("nothing is sent, so Bob can make room and accept again", sent, fromBob.size());
		assertFalse(bob.offerFrom(ANN).isEmpty());
	}

	@Test
	public void anEggForAnEggFitsEvenWithFullHands()
	{
		Egg mine = new Egg(Rarity.RARE);
		annGame.data().eggs.add(mine);
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			bobGame.data().eggs.add(new Egg(Rarity.COMMON));

		ann.offer(BOB, "Bob", mine, Item.egg("COMMON", 0));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		Egg got = only(annGame.data().eggs);
		assertEquals(Rarity.COMMON, got.rarityTier());
		assertEquals(SaveData.MAX_EGGS, bobGame.data().eggs.size());
	}

	@Test
	public void onlyOneOfferWaitsAtATime()
	{
		ann.offer(BOB, "Bob", annImp, null);
		int sent = fromAnn.size();
		ann.offer(BOB, "Bob", annImp, null);

		assertEquals(sent, fromAnn.size());
		assertEquals("Still waiting for an answer to your last offer", last(annTold));
	}

	@Test
	public void somethingNoLongerOwnedCannotBeOffered()
	{
		ann.offer(BOB, "Bob", new Creature("imp", 50, now), null);

		assertTrue(fromAnn.isEmpty());
		assertEquals("You no longer have that", last(annTold));
	}

	@Test
	public void anOfferNobodyAnswersRunsOut()
	{
		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		assertFalse(ann.expire());
		assertFalse(bob.expire());

		now += FriendTrades.OFFER_TIMEOUT_MS + 1;
		assertEquals("", bob.offerFrom(ANN));
		assertTrue(ann.expire());
		assertEquals("Your trade offer got no answer", last(annTold));
		assertTrue(bob.expire());
		assertFalse(ann.expire());
		bob.answer(ANN, "Ann", true);
		assertTrue("an offer that ran out cannot be accepted", fromBob.isEmpty());
	}

	@Test
	public void aFriendWhoLeavesTakesTheirOffersWithThem()
	{
		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.forget(ANN);
		ann.forget(BOB);

		assertEquals("", bob.offerFrom(ANN));
		assertFalse(ann.waitingOn(BOB));
	}

	@Test
	public void crossingOffersCannotBothBeAccepted()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.offer(ANN, "Ann", bobCow, Item.creature("imp", 20, 0, true));
		GielinorGoPartyMessage annOffer = last(fromAnn);
		GielinorGoPartyMessage bobOffer = last(fromBob);
		assertTrue(bob.receive(annOffer, BOB, "Ann"));
		assertTrue(ann.receive(bobOffer, ANN, "Bob"));

		bob.answer(ANN, "Ann", true);
		ann.answer(BOB, "Bob", true);

		assertEquals("Bob sent only his offer", bobOffer, last(fromBob));
		assertEquals("Ann sent only her offer", annOffer, last(fromAnn));
		assertEquals("That is already promised in another trade", last(bobTold));
		assertEquals("That is already promised in another trade", last(annTold));
		assertSame(annImp, only(annGame.data().team));
		assertSame(bobCow, only(bobGame.data().team));
	}

	@Test
	public void twoAgreedOffersCannotAskForTheSameCreature()
	{
		Random random = new Random(5);
		GoGame catGame = new GoGame(table, random, () -> now, notice -> annTold.add(notice.text()));
		List<GielinorGoPartyMessage> fromCat = new ArrayList<>();
		FriendTrades cat = new FriendTrades(table, catGame, random, () -> now,
			message -> fromCat.add(sentBy(message, CAT)), notice -> annTold.add(notice.text()));
		Creature catImp = new Creature("imp", 9, now);
		catGame.data().team.add(catImp);

		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		cat.offer(BOB, "Cat", catImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.receive(last(fromCat), BOB, "Cat");
		bob.answer(ANN, "Ann", true);
		int sent = fromBob.size();
		bob.answer(CAT, "Cat", true);

		assertEquals("no agreement goes to Cat", sent, fromBob.size());
		assertEquals("That is already promised in another trade", last(bobTold));
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(CAT, "Cat", true);

		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromBob).kind());
		assertEquals(1, copies("cow"));
		assertSame(catImp, only(catGame.data().team));
		Creature annGot = only(annGame.data().team);
		assertEquals("cow", annGot.species);
	}

	@Test
	public void somethingPromisedInAnAcceptedTradeCannotBeOfferedElsewhere()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		int sent = fromBob.size();

		bob.offer(CAT, "Cat", bobCow, null);

		assertEquals(sent, fromBob.size());
		assertEquals("That is already promised in a trade you accepted", last(bobTold));
	}

	@Test
	public void theConfirmationChecksTheAskedCreatureIsStillThere()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bobGame.data().team.clear();
		bobGame.takeDirty();

		assertTrue(bob.receive(last(fromAnn), BOB, "Ann"));

		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromBob).kind());
		assertEquals("The trade with Ann fell through", last(bobTold));
		assertTrue("nothing was added", bobGame.data().team.isEmpty());
		assertFalse(bobGame.takeDirty());
		assertFalse(bob.takeTraded());
	}

	@Test
	public void theConfirmationChecksThereIsRoomForWhatComesIn()
	{
		Egg egg = new Egg(Rarity.RARE);
		annGame.data().eggs.add(egg);
		ann.offer(BOB, "Bob", egg, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			bobGame.data().eggs.add(new Egg(Rarity.COMMON));

		bob.receive(last(fromAnn), BOB, "Ann");

		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromBob).kind());
		assertEquals(SaveData.MAX_EGGS, bobGame.data().eggs.size());
		assertEquals("The trade with Ann fell through", last(bobTold));
	}

	@Test
	public void theAgreementChecksThereIsRoomForWhatComesIn()
	{
		bobGame.data().eggs.add(new Egg(Rarity.EPIC));
		ann.offer(BOB, "Bob", annImp, Item.egg("EPIC", 0));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			annGame.data().eggs.add(new Egg(Rarity.COMMON));

		assertTrue(ann.receive(last(fromBob), ANN, "Bob"));

		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromAnn).kind());
		assertSame("Ann keeps her creature", annImp, only(annGame.data().team));
		assertEquals(SaveData.MAX_EGGS, annGame.data().eggs.size());
		assertEquals("The trade with Bob fell through", last(annTold));
	}

	@Test
	public void aCreatureThatGainedXpCallsTheTradeOffAtTheAgreement()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		annImp.xp += 10;

		assertTrue(ann.receive(last(fromBob), ANN, "Bob"));

		assertEquals(GielinorGoPartyMessage.REFUSE, last(fromAnn).kind());
		assertEquals("The trade with Bob fell through", last(annTold));
		assertSame(annImp, only(annGame.data().team));
		assertEquals(1, copies("cow"));
	}

	@Test
	public void aCreatureThatEvolvedBetweenAgreeAndConfirmStillCompletes()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bobCow.species = "imp";

		assertTrue(bob.receive(last(fromAnn), BOB, "Ann"));

		assertEquals(GielinorGoPartyMessage.CONFIRM, last(fromAnn).kind());
		Creature bobGot = only(bobGame.data().team);
		assertEquals("imp", bobGot.species);
		assertTrue(bobGot.gilded);
		assertEquals(1, copies("cow"));
		assertEquals(1, copies("imp"));
		assertTrue(bob.takeTraded());
	}

	@Test
	public void aCreatureThatGainedXpBetweenAgreeAndConfirmStillCompletes()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bobCow.xp += 500;

		assertTrue(bob.receive(last(fromAnn), BOB, "Ann"));

		assertEquals(1, copies("cow"));
		assertEquals(1, copies("imp"));
		Creature annGot = only(annGame.data().team);
		Creature bobGot = only(bobGame.data().team);
		assertEquals("cow", annGot.species);
		assertEquals("imp", bobGot.species);
	}

	@Test
	public void aPromisedCreatureCannotBeReleasedButCanBeMoved()
	{
		annGame.data().team.clear();
		annGame.data().storage.add(annImp);
		ann.offer(BOB, "Bob", annImp, null);

		assertTrue(ann.locked(annImp));
		annGame.release(0);
		assertEquals("Imp is promised in a trade", last(annTold));
		assertSame(annImp, only(annGame.data().storage));

		annGame.moveToTeam(0);
		assertSame("moving does not change who owns it", annImp, only(annGame.data().team));
		annGame.moveToStorage(0);
		assertTrue(ann.locked(annImp));

		now += FriendTrades.OFFER_TIMEOUT_MS + 1;
		ann.expire();
		annGame.release(0);
		assertTrue(annGame.data().storage.isEmpty());
	}

	@Test
	public void theAskedCreatureOfAnAgreedOfferCannotBeReleased()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		assertFalse("an offer not yet accepted promises nothing", bob.locked(bobCow));
		bob.answer(ANN, "Ann", true);
		bobGame.moveToStorage(0);

		assertTrue(bob.locked(bobCow));
		bobGame.release(0);

		assertEquals("Cow is promised in a trade", last(bobTold));
		assertSame(bobCow, only(bobGame.data().storage));
	}

	@Test
	public void aPromisedEggWaitsToHatchUntilTheTradeEnds()
	{
		Egg egg = new Egg(Rarity.RARE);
		egg.tilesLeft = 5;
		annGame.data().eggs.add(egg);
		ann.offer(BOB, "Bob", egg, null);
		assertTrue(ann.locked(egg));

		annGame.addSteps(10);
		assertSame("the egg is still carried", egg, only(annGame.data().eggs));
		assertEquals(0, egg.tilesLeft);

		now += FriendTrades.OFFER_TIMEOUT_MS + 1;
		ann.expire();
		annGame.addSteps(1);
		assertTrue("the egg hatches once the trade ends", annGame.data().eggs.isEmpty());
	}

	@Test
	public void anAcceptedGiftReservesTheSlotItNeeds()
	{
		fill(bobGame, 1);
		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		assertEquals(0, bob.reservedCreatureSlots());
		assertTrue(bobGame.hasRoomForCreature());

		bob.answer(ANN, "Ann", true);

		assertEquals(1, bob.reservedCreatureSlots());
		assertEquals(0, bob.reservedEggSlots());
		assertFalse(bobGame.hasRoomForCreature());
		bobGame.data().addJars(JarType.BRONZE, 5);
		int jars = bobGame.data().jarCount(JarType.BRONZE);
		assertEquals(CatchOutcome.FULL, bobGame.tryCatch(new WildEntry(1, COW, 2, 1)));
		assertEquals("no jar was spent", jars, bobGame.data().jarCount(JarType.BRONZE));
		assertFalse("a trade cannot take the reserved slot twice", bobGame.receive(new Creature("cow", 1, now)));

		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");
		assertEquals(0, bob.reservedCreatureSlots());
		assertEquals(300, bobGame.data().storage.size());
	}

	@Test
	public void anAcceptedEggReservesAnEggSlot()
	{
		bobGame.data().eggs.add(new Egg(Rarity.COMMON));
		bobGame.data().eggs.add(new Egg(Rarity.COMMON));
		Egg egg = new Egg(Rarity.RARE);
		annGame.data().eggs.add(egg);
		ann.offer(BOB, "Bob", egg, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		assertTrue(bobGame.hasRoomForEgg());

		bob.answer(ANN, "Ann", true);

		assertEquals(1, bob.reservedEggSlots());
		assertEquals(0, bob.reservedCreatureSlots());
		assertFalse("an egg drop would be skipped", bobGame.hasRoomForEgg());
	}

	@Test
	public void theOfferersWantedItemReservesRoomToo()
	{
		ann.offer(BOB, "Bob", annImp, Item.egg("EPIC", 0));

		assertEquals(1, ann.reservedEggSlots());
		assertEquals(0, ann.reservedCreatureSlots());
	}

	@Test
	public void aLikeForLikeSwapReservesNothing()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);

		assertEquals(0, ann.reservedCreatureSlots());
		assertEquals(0, bob.reservedCreatureSlots());
		assertEquals(0, bob.reservedEggSlots());
	}

	@Test
	public void locksAndReservationsEndWithTheTrade()
	{
		ann.offer(BOB, "Bob", annImp, Item.egg("EPIC", 0));
		assertTrue(ann.locked(annImp));
		now += FriendTrades.OFFER_TIMEOUT_MS + 1;
		ann.expire();
		assertFalse(ann.locked(annImp));
		assertEquals(0, ann.reservedEggSlots());

		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", false);
		ann.receive(last(fromBob), ANN, "Bob");
		assertFalse("refused", ann.locked(annImp));

		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		assertTrue(bob.locked(bobCow));
		bob.clear();
		assertFalse("cleared", bob.locked(bobCow));
		assertEquals(0, bob.reservedCreatureSlots());
		ann.clear();
		assertFalse(ann.locked(annImp));

		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		bob.forget(ANN);
		assertEquals(0, bob.reservedCreatureSlots());
		ann.forget(BOB);
		assertFalse("forgotten", ann.locked(annImp));
	}

	@Test
	public void anAgreedTradeThatTimesOutUnlocksTheAskedCreature()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		assertTrue(bob.locked(bobCow));

		now += FriendTrades.CONFIRM_TIMEOUT_MS + 1;
		assertTrue(bob.expire());

		assertFalse(bob.locked(bobCow));
	}

	@Test
	public void aPlayerCannotDuplicateByActingBetweenAgreeAndConfirm()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bobGame.moveToStorage(0);

		bobGame.release(0);
		int sent = fromBob.size();
		bob.offer(CAT, "Cat", bobCow, null);
		assertEquals("no second offer for the same creature", sent, fromBob.size());

		assertTrue(bob.receive(last(fromAnn), BOB, "Ann"));
		assertEquals(1, copies("cow"));
		assertEquals(1, copies("imp"));
		assertTrue(annGame.data().team.size() + annGame.data().storage.size() == 1);
		assertTrue(bobGame.data().team.size() + bobGame.data().storage.size() == 1);
	}

	@Test
	public void aWalkedEggIsStillTheSameEgg()
	{
		Egg egg = new Egg(Rarity.RARE);
		annGame.data().eggs.add(egg);
		ann.offer(BOB, "Bob", egg, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		egg.tilesLeft -= 40;
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		assertTrue(annGame.data().eggs.isEmpty());
		assertEquals(1, bobGame.data().eggs.size());
	}

	@Test
	public void aGameRefusesWhatDoesNotFit()
	{
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			assertTrue(annGame.receive(new Egg(Rarity.COMMON)));

		assertFalse(annGame.receive(new Egg(Rarity.RARE)));
		assertEquals(SaveData.MAX_EGGS, annGame.data().eggs.size());

		while (annGame.data().team.size() < SaveData.TEAM_SIZE)
			annGame.data().team.add(new Creature("cow", 1, now));

		while (annGame.data().storage.size() < SaveData.STORAGE_SIZE)
			annGame.data().storage.add(new Creature("cow", 1, now));

		assertFalse(annGame.receive(new Creature("imp", 3, now)));
		assertEquals(SaveData.TEAM_SIZE, annGame.data().team.size());
		assertEquals(SaveData.STORAGE_SIZE, annGame.data().storage.size());
	}

	@Test
	public void aPickStillMatchesUntilWhatWasPickedChanges()
	{
		PanelState state = annGame.snapshot();
		CreatureView shown = state.team().get(0);
		TradePick pick = TradePick.of(Holding.TEAM, 0, shown);
		assertTrue(pick.matches(annGame.snapshot()));

		annImp.hpFraction = 0.5;
		assertTrue("health recovers by itself, so it does not count", pick.matches(annGame.snapshot()));

		annImp.xp += 5;
		assertFalse("more XP is a different creature", pick.matches(annGame.snapshot()));
		annImp.xp -= 5;
		assertTrue(pick.matches(annGame.snapshot()));

		annGame.data().team.add(0, new Creature("cow", 3, now));
		assertFalse("another creature moved into that place", pick.matches(annGame.snapshot()));
		annGame.data().team.clear();
		assertFalse("nothing is there any more", pick.matches(annGame.snapshot()));
	}

	@Test
	public void anEggPickSurvivesWalkingButNotAnotherEgg()
	{
		Egg egg = new Egg(Rarity.RARE);
		annGame.data().eggs.add(egg);
		PanelState state = annGame.snapshot();
		EggView shown = state.eggs().get(0);
		TradePick pick = TradePick.of(0, shown);

		egg.tilesLeft -= 10;
		assertTrue(pick.matches(annGame.snapshot()));

		annGame.data().eggs.set(0, new Egg(Rarity.EPIC));
		assertFalse(pick.matches(annGame.snapshot()));
		annGame.data().eggs.clear();
		assertFalse(pick.matches(annGame.snapshot()));
	}

	@Test
	public void aFriendsItemIsOnlyFoundWhenTheLabelStillMatches()
	{
		List<Item> theirs = Arrays.asList(Item.creature("cow", 5, 0, false), Item.egg("EPIC", 0));

		assertSame(theirs.get(0), ann.friendItem(theirs, 0, "Cow (Lv 5)"));
		assertSame(theirs.get(1), ann.friendItem(theirs, 1, "Epic egg"));
		assertEquals(null, ann.friendItem(theirs, 0, "Imp (Lv 5)"));
		assertEquals(null, ann.friendItem(theirs, 1, "Cow (Lv 5)"));
		assertEquals(null, ann.friendItem(theirs, 2, "Cow (Lv 5)"));
		assertEquals(null, ann.friendItem(theirs, -1, "Cow (Lv 5)"));
	}

	@Test
	public void aClaimedLevelThatDoesNotMatchTheXpIsRejected()
	{
		assertFalse(bob.receive(offerOf(Item.creature("imp", 99, 0, false)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 1, XpRules.xpForLevel(50), false)), BOB, "Ann"));
		assertEquals("", bob.offerFrom(ANN));

		long xp = XpRules.xpForLevel(20);
		assertTrue(bob.receive(offerOf(Item.creature("imp", 20, xp, false)), BOB, "Ann"));
		assertEquals("Imp (Lv 20) as a gift", bob.offerFrom(ANN));
	}

	private static final String MOVES = "{\"moves\":["
		+ "{\"id\":\"a\",\"name\":\"Aa\",\"style\":\"MELEE\",\"power\":1.0,\"accuracy\":1.0},"
		+ "{\"id\":\"b\",\"name\":\"Bb\",\"style\":\"MELEE\",\"power\":1.3,\"accuracy\":0.9},"
		+ "{\"id\":\"c\",\"name\":\"Cc\",\"style\":\"MELEE\",\"power\":1.6,\"accuracy\":0.8},"
		+ "{\"id\":\"d\",\"name\":\"Dd\",\"style\":\"MELEE\",\"power\":2.0,\"accuracy\":0.7}],"
		+ "\"defaults\":{\"MELEE\":\"all\"},"
		+ "\"pools\":{\"all\":{\"styles\":[\"MELEE\"],\"learns\":[[\"a\",1],[\"b\",10],[\"c\",20],[\"d\",40]]}},"
		+ "\"species\":{}}";

	private void giveBothTheMoves()
	{
		MoveTable moves = MoveTable.load(new Gson(), new StringReader(MOVES));
		annGame.moves(moves);
		bobGame.moves(moves);
	}

	@Test
	public void aTradedCreatureKeepsTheMovesItKnew()
	{
		giveBothTheMoves();
		annImp.moves = new ArrayList<>(Arrays.asList("a", "c"));

		swap();

		Creature bobGot = only(bobGame.data().team);
		Creature annGot = only(annGame.data().team);
		assertEquals(Arrays.asList("a", "c"), bobGot.moves);
		assertEquals("the other side's creature arrived with the moves of its level", Arrays.asList("a"),
			annGot.moves);
	}

	@Test
	public void anOfferWithMovesTheCreatureCannotKnowIsIgnored()
	{
		giveBothTheMoves();
		long xp = XpRules.xpForLevel(20);
		String[] ok = {"a", "b", "c"};
		String[] early = {"a", "d"};
		String[] unknown = {"a", "zzz"};
		String[] repeated = {"a", "a"};
		String[] tooMany = {"a", "b", "c", "d", "a"};
		String[] blank = {"a", null};

		assertFalse(bob.receive(offerOf(Item.creature("imp", 20, xp, false, StatRolls.AVERAGE, early)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 20, xp, false, StatRolls.AVERAGE, unknown)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 20, xp, false, StatRolls.AVERAGE, repeated)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 20, xp, false, StatRolls.AVERAGE, tooMany)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 20, xp, false, StatRolls.AVERAGE, blank)), BOB, "Ann"));
		assertEquals("", bob.offerFrom(ANN));

		assertTrue(bob.receive(offerOf(Item.creature("imp", 20, xp, false, StatRolls.AVERAGE, ok)), BOB, "Ann"));
		assertEquals("Imp (Lv 20) as a gift", bob.offerFrom(ANN));
	}

	@Test
	public void aCreatureThatLearnedAMoveBetweenOfferAndAgreementCallsTheTradeOff()
	{
		giveBothTheMoves();
		annImp.moves = new ArrayList<>(Arrays.asList("a", "b"));
		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);

		annImp.moves = new ArrayList<>(Arrays.asList("a", "c"));
		ann.receive(last(fromBob), ANN, "Bob");

		assertEquals("the swap was not made", 1, annGame.data().team.size());
		assertSame(annImp, only(annGame.data().team));
		assertTrue(bobGame.data().team.size() == 1);
	}

	@Test
	public void aNewOfferFromSomeoneWithAnOpenOfferIsIgnored()
	{
		long xp = XpRules.xpForLevel(20);
		assertTrue(bob.receive(offerOf(Item.creature("imp", 20, xp, false)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("cow", 20, xp, false)), BOB, "Ann"));
		assertEquals("Imp (Lv 20) as a gift", bob.offerFrom(ANN));

		bob.answer(ANN, "Ann", false);
		GielinorGoPartyMessage again = offerOf(Item.creature("cow", 20, xp, false));
		assertTrue("after a decline a new offer is welcome", bob.receive(again, BOB, "Ann"));
		assertEquals("Cow (Lv 20) as a gift", bob.offerFrom(ANN));
	}

	@Test
	public void aNewOfferCannotReplaceAnAgreedTrade()
	{
		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);

		assertFalse(bob.receive(offerOf(Item.creature("cow", 20, XpRules.xpForLevel(20), false)), BOB, "Ann"));
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		assertEquals(2, bobGame.data().team.size());
		assertEquals(1, copies("imp"));
	}

	@Test
	public void offerNoticesFromOneSenderAreThrottled()
	{
		long xp = XpRules.xpForLevel(20);
		for (int i = 0; i < 4; i++)
		{
			bob.receive(offerOf(Item.creature("imp", 20, xp, false)), BOB, "Ann");
			bob.answer(ANN, "Ann", false);
			now += 1_000L;
		}

		assertEquals("one notice in four seconds", 1, offerNotices());

		now += FriendTrades.NOTICE_GAP_MS;
		bob.receive(offerOf(Item.creature("imp", 20, xp, false)), BOB, "Ann");
		assertEquals(2, offerNotices());
	}

	@Test
	public void aSenderIsHeldToTenMessagesInTenSeconds()
	{
		MessageRate rate = new MessageRate();
		for (int i = 0; i < MessageRate.LIMIT; i++)
			assertTrue(rate.allow(ANN, now + i));

		assertFalse(rate.allow(ANN, now + 100));
		assertTrue("another sender is not affected", rate.allow(BOB, now + 100));
		assertTrue("the window moves on", rate.allow(ANN, now + MessageRate.WINDOW_MS + 100));
	}

	@Test
	public void onlyAHelloThatTellsSomethingNewChangesWhatThePanelShows()
	{
		FriendBattles battles = new FriendBattles(table, new Random(6), () -> now, message -> { }, notice -> { });
		GielinorGoPartyMessage hello = hello(5);
		assertEquals(null, battles.receive(hello, BOB, "Ann", bobGame.data(), true));
		assertTrue(battles.takeChanged());
		assertFalse(battles.takeChanged());

		battles.receive(hello(5), BOB, "Ann", bobGame.data(), true);
		assertFalse("the same team again is no news", battles.takeChanged());

		battles.receive(hello(6), BOB, "Ann", bobGame.data(), true);
		assertTrue(battles.takeChanged());
		battles.receive(hello(6), BOB, "Ann", bobGame.data(), true);
		assertFalse(battles.takeChanged());
	}

	private GielinorGoPartyMessage hello(int level)
	{
		GielinorGoPartyMessage hello = new GielinorGoPartyMessage(GielinorGoPartyMessage.HELLO, 0, 0, 0, 3,
			new String[] {"imp"}, new int[] {level});
		return sentBy(hello, ANN);
	}

	private long offerNotices()
	{
		return bobTold.stream().filter(text -> text.endsWith("offers a trade. See the Battle tab")).count();
	}

	private int copies(String species)
	{
		int count = 0;
		for (GoGame game : new GoGame[] {annGame, bobGame})
		{
			for (Creature creature : game.data().team)
				count += creature.species.equals(species) ? 1 : 0;

			for (Creature creature : game.data().storage)
				count += creature.species.equals(species) ? 1 : 0;
		}

		return count;
	}

	private void fill(GoGame game, int free)
	{
		while (game.data().team.size() < SaveData.TEAM_SIZE)
			game.data().team.add(new Creature("imp", 1, now));

		while (game.data().storage.size() < SaveData.STORAGE_SIZE - free)
			game.data().storage.add(new Creature("imp", 1, now));
	}

	@Test
	public void rollsAndTemperamentSurviveATrade()
	{
		StatRolls annRolls = new StatRolls(3, -1, 2, 5, Temperament.BRAVE);
		StatRolls bobRolls = new StatRolls(-4, 0, 1, -2, Temperament.STEADY);
		annImp.setRolls(annRolls);
		bobCow.setRolls(bobRolls);

		swap();

		Creature bobGot = only(bobGame.data().team);
		Creature annGot = only(annGame.data().team);
		assertEquals(annRolls, bobGot.rolls());
		assertEquals("BRAVE", bobGot.temperament);
		assertEquals(bobRolls, annGot.rolls());
	}

	@Test
	public void aTradedCreatureWithNoRollsStaysAverage()
	{
		swap();

		Creature bobGot = only(bobGame.data().team);
		assertEquals(StatRolls.AVERAGE, bobGot.rolls());
		assertNull(bobGot.temperament);
	}

	@Test
	public void theOfferNamesTheRatingOfWhatIsGiven()
	{
		annImp.setRolls(new StatRolls(5, 5, 3, 0, Temperament.BRAVE));

		ann.offer(BOB, "Bob", annImp, null);
		bob.receive(last(fromAnn), BOB, "Ann");

		assertEquals("Offered gilded Imp (Lv 20) Strong to Bob", last(annTold));
		assertEquals("gilded Imp (Lv 20) Strong as a gift", bob.offerFrom(ANN));
	}

	@Test
	public void aRollOutOfRangeOrAnUnknownTemperamentIsRefused()
	{
		long xp = XpRules.xpForLevel(20);
		StatRolls tooHigh = new StatRolls(9, 0, 0, 0, Temperament.STEADY);
		StatRolls tooLow = new StatRolls(0, 0, 0, -6, Temperament.STEADY);

		assertFalse(bob.receive(offerOf(Item.creature("imp", 20, xp, false, tooHigh)), BOB, "Ann"));
		assertFalse(bob.receive(offerOf(Item.creature("imp", 20, xp, false, tooLow)), BOB, "Ann"));
		GielinorGoPartyMessage odd = offerOf(Item.creature("imp", 20, xp, false,
			new StatRolls(0, 0, 0, 0, Temperament.BRAVE)));
		Gson gson = new Gson();
		GielinorGoPartyMessage grumpy = gson.fromJson(gson.toJson(odd).replace("BRAVE", "GRUMPY"),
			GielinorGoPartyMessage.class);
		grumpy.setMemberId(ANN);
		assertFalse(bob.receive(grumpy, BOB, "Ann"));

		assertEquals("", bob.offerFrom(ANN));
		assertTrue(fromBob.isEmpty());
		assertTrue(bob.receive(offerOf(Item.creature("imp", 20, xp, false, StatRolls.AVERAGE)), BOB, "Ann"));
	}

	@Test
	public void aFriendSendingNoRollsAtAllMeansAverage()
	{
		annImp.setRolls(new StatRolls(5, 5, 3, 0, Temperament.BRAVE));
		ann.offer(BOB, "Bob", annImp, null);
		Gson gson = new Gson();
		String wire = gson.toJson(last(fromAnn)).replaceAll(",\"rolls\":\\[[^\\]]*\\]", "")
			.replace(",\"temperament\":\"BRAVE\"", "");
		assertFalse("the old layout has no rolls", wire.contains("rolls"));
		GielinorGoPartyMessage old = gson.fromJson(wire, GielinorGoPartyMessage.class);
		old.setMemberId(ANN);

		assertTrue(bob.receive(old, BOB, "Ann"));
		assertEquals("gilded Imp (Lv 20) as a gift", bob.offerFrom(ANN));
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");

		List<Creature> team = bobGame.data().team;
		Creature got = team.get(team.size() - 1);
		assertEquals(StatRolls.AVERAGE, got.rolls());
	}

	private void swap()
	{
		ann.offer(BOB, "Bob", annImp, Item.creature("cow", 5, 0, false));
		bob.receive(last(fromAnn), BOB, "Ann");
		bob.answer(ANN, "Ann", true);
		ann.receive(last(fromBob), ANN, "Bob");
		bob.receive(last(fromAnn), BOB, "Ann");
	}

	private GielinorGoPartyMessage offerOf(Item item)
	{
		GielinorGoPartyMessage offer = new GielinorGoPartyMessage(GielinorGoPartyMessage.OFFER, BOB, 9, 0, 0,
			new String[0], new int[0]);
		return sentBy(offer.giving(item), ANN);
	}

	private static GielinorGoPartyMessage sentBy(GielinorGoPartyMessage message, long memberId)
	{
		message.setMemberId(memberId);
		return message;
	}

	private static <T> T only(List<T> list)
	{
		assertEquals(1, list.size());
		return list.get(0);
	}

	private static <T> T last(List<T> list)
	{
		return list.get(list.size() - 1);
	}
}
