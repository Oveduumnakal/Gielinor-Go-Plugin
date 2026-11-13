/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Plays two {@link FriendTurnBattle} instances against each other, handing each the other's messages. */
public class FriendTurnBattleTest
{
	private static final long ANN = 1L;
	private static final long BOB = 2L;
	private static final long BATTLE = 77L;
	private static final long SEED = 12345L;
	private static final Species IMP = new Species("Imp", 7, CombatType.STAB);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private final List<GielinorGoPartyMessage> fromAnn = new ArrayList<>();
	private final List<GielinorGoPartyMessage> fromBob = new ArrayList<>();
	private FriendTurnBattle ann;
	private FriendTurnBattle bob;

	@Before
	public void setUp()
	{
		ann = new FriendTurnBattle(BATTLE, BOB, "Bob", true, annTeam(), bobTeam(), SEED, fromAnn::add);
		bob = new FriendTurnBattle(BATTLE, ANN, "Ann", false, bobTeam(), annTeam(), SEED, fromBob::add);
	}

	private static List<Combatant> annTeam()
	{
		return new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 40, 1.0), new Combatant(IMP, 10, 1.0)));
	}

	private static List<Combatant> bobTeam()
	{
		return new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 5, 1.0), new Combatant(IMP, 12, 1.0)));
	}

	private void deliver()
	{
		for (GielinorGoPartyMessage message : fromAnn)
		{
			message.setMemberId(ANN);
			bob.remote(message, 0);
		}

		for (GielinorGoPartyMessage message : fromBob)
		{
			message.setMemberId(BOB);
			ann.remote(message, 0);
		}

		fromAnn.clear();
		fromBob.clear();
	}

	private static GielinorGoPartyMessage from(long member, GielinorGoPartyMessage message)
	{
		message.setMemberId(member);
		return message;
	}

	private void playOut()
	{
		ann.opened(0);
		bob.opened(0);
		for (int turns = 0; turns < 300 && !ann.over(); turns++)
		{
			if (ann.mustChoose())
				ann.local(ann.automatic(), 0);

			if (bob.mustChoose())
				bob.local(bob.automatic(), 0);

			deliver();
			ann.opened(0);
			bob.opened(0);
		}
	}

	@Test
	public void theSameChoicesGiveTheSameBattleOnBothSides()
	{
		playOut();

		assertTrue(ann.over());
		assertTrue(bob.over());
		assertNull(ann.ended());
		assertNull(bob.ended());
		assertEquals(TurnResult.Status.WON, ann.state().ended());
		assertEquals(TurnResult.Status.LOST, bob.state().ended());
		assertEquals(ann.state().stateHash(), bob.state().stateHash());
		FriendTurnBattle.Played one = ann.poll();
		FriendTurnBattle.Played other = bob.poll();
		int turns = 0;
		while (one != null)
		{
			assertNotNull(other);
			assertEquals(one.result().hits().size(), other.result().hits().size());
			assertEquals(one.result().status().over(), other.result().status().over());
			turns++;
			one = ann.poll();
			other = bob.poll();
		}

		assertNull(other);
		assertTrue(turns > 1);
	}

	@Test
	public void aTurnIsOnlyPlayedOnceBothChoicesAreIn()
	{
		ann.opened(0);
		assertTrue(ann.local(BattleChoice.attack(), 0));

		assertNull(ann.poll());
		assertTrue(ann.waitingForFriend());
		assertFalse(ann.mustChoose());
		assertEquals(1, ann.turn());
		assertEquals(1, fromAnn.size());
		assertEquals(GielinorGoPartyMessage.TURN, fromAnn.get(0).kind());

		bob.opened(0);
		bob.local(BattleChoice.attack(), 0);
		deliver();

		assertNotNull(ann.poll());
		assertNotNull(bob.poll());
		assertEquals(2, ann.turn());
		assertEquals(2, bob.turn());
	}

	@Test
	public void aLateChoiceBecomesTheAutomaticOne()
	{
		ann.opened(0);

		ann.timeout(FriendTurnBattle.TURN_MS - 1);
		assertTrue(fromAnn.isEmpty());

		ann.timeout(FriendTurnBattle.TURN_MS);
		assertEquals(1, fromAnn.size());
		assertEquals("A", fromAnn.get(0).choice());
		assertFalse(ann.mustChoose());
		assertNull(ann.ended());
	}

	@Test
	public void aMissingChoiceEndsTheBattleAfterTheGraceTime()
	{
		ann.opened(0);
		ann.local(BattleChoice.attack(), 0);

		ann.timeout(FriendTurnBattle.TURN_MS + FriendTurnBattle.GRACE_MS - 1);
		assertNull(ann.ended());

		ann.timeout(FriendTurnBattle.TURN_MS + FriendTurnBattle.GRACE_MS);
		assertEquals(FriendTurnBattle.End.SILENT, ann.ended());
		assertTrue(ann.over());
		assertEquals("Your friend stopped answering", ann.ended().words("Bob"));
	}

	@Test
	public void aWrongTurnNumberEndsTheBattle()
	{
		int hash = ann.state().stateHash();

		bob.remote(from(ANN, GielinorGoPartyMessage.turn(BOB, BATTLE, 5, "A", hash)), 0);

		assertEquals(FriendTurnBattle.End.OUT_OF_STEP, bob.ended());
	}

	@Test
	public void anUnknownMoveEndsTheBattle()
	{
		int hash = ann.state().stateHash();

		bob.remote(from(ANN, GielinorGoPartyMessage.turn(BOB, BATTLE, 1, "A9", hash)), 0);

		assertEquals(FriendTurnBattle.End.OUT_OF_STEP, bob.ended());
	}

	@Test
	public void aStateHashThatDoesNotMatchEndsTheBattle()
	{
		int hash = ann.state().stateHash();

		bob.remote(from(ANN, GielinorGoPartyMessage.turn(BOB, BATTLE, 1, "A", hash + 1)), 0);

		assertEquals(FriendTurnBattle.End.OUT_OF_STEP, bob.ended());
	}

	@Test
	public void aChoiceThatIsNotOneEndsTheBattle()
	{
		int hash = ann.state().stateHash();

		bob.remote(from(ANN, GielinorGoPartyMessage.turn(BOB, BATTLE, 1, "X!", hash)), 0);

		assertEquals(FriendTurnBattle.End.OUT_OF_STEP, bob.ended());
	}

	@Test
	public void aSecondChoiceForTheSameTurnEndsTheBattle()
	{
		int hash = ann.state().stateHash();
		bob.remote(from(ANN, GielinorGoPartyMessage.turn(BOB, BATTLE, 1, "A", hash)), 0);
		assertNull(bob.ended());

		bob.remote(from(ANN, GielinorGoPartyMessage.turn(BOB, BATTLE, 1, "A", hash)), 0);

		assertEquals(FriendTurnBattle.End.OUT_OF_STEP, bob.ended());
	}

	@Test
	public void aMessageForAnotherBattleOrFromAnotherPlayerIsIgnored()
	{
		int hash = ann.state().stateHash();

		bob.remote(from(ANN, GielinorGoPartyMessage.turn(BOB, BATTLE + 1, 5, "A", hash)), 0);
		bob.remote(from(99L, GielinorGoPartyMessage.turn(BOB, BATTLE, 5, "A", hash)), 0);

		assertNull(bob.ended());
		assertFalse(bob.over());
	}

	@Test
	public void theFriendLeavingEndsTheBattleWithNoResult()
	{
		bob.remote(from(ANN, GielinorGoPartyMessage.leave(BOB, BATTLE)), 0);

		assertEquals(FriendTurnBattle.End.LEFT, bob.ended());
		assertEquals("Ann left the battle", bob.ended().words("Ann"));
		assertNull(bob.state().ended());
	}

	@Test
	public void leavingTellsTheFriendAndEndsTheBattle()
	{
		ann.leave();

		assertEquals(FriendTurnBattle.End.MINE, ann.ended());
		assertEquals(1, fromAnn.size());
		assertEquals(GielinorGoPartyMessage.LEAVE, fromAnn.get(0).kind());

		deliver();

		assertEquals(FriendTurnBattle.End.LEFT, bob.ended());
	}

	@Test
	public void aChoiceThatCannotBeMadeNowChangesNothing()
	{
		assertFalse(ann.local(BattleChoice.attack(99), 0));
		assertFalse(ann.local(BattleChoice.switchTo(0), 0));
		assertFalse(ann.local(BattleChoice.item(ItemType.SMALL_SALVE, 0), 0));
		assertFalse(ann.local(null, 0));

		assertTrue(fromAnn.isEmpty());
		assertTrue(ann.mustChoose());
	}

	@Test
	public void aChoiceIsWrittenAndReadBack()
	{
		BattleChoice[] choices = {BattleChoice.attack(), BattleChoice.attack(1), BattleChoice.switchTo(2),
			BattleChoice.item(ItemType.LARGE_SALVE, 1)};

		for (BattleChoice choice : choices)
		{
			BattleChoice back = FriendTurnBattle.decode(FriendTurnBattle.encode(choice));

			assertNotNull(back);
			assertEquals(choice.kind(), back.kind());
			assertEquals(choice.moveIndex(), back.moveIndex());
			assertEquals(choice.teamIndex(), back.teamIndex());
			assertEquals(choice.itemType(), back.itemType());
		}
	}

	@Test
	public void textThatIsNotAChoiceReadsAsNothing()
	{
		for (String text : new String[] {null, "", "Z", "A-1", "A99999", "S", "IWRONG:0", "ISMALL_SALVE", "I:1",
			"ISMALL_SALVE:x"})
			assertNull(text, FriendTurnBattle.decode(text));
	}

	@Test
	public void anItemIsSpentOnTheRealCreatureOnBothSides()
	{
		List<Combatant> hurt = new ArrayList<>(Arrays.asList(new Combatant(IMP, 10, 0.5),
			new Combatant(CHICKEN, 5, 1.0)));
		FriendTurnBattle hurtAnn = new FriendTurnBattle(BATTLE, BOB, "Bob", true, hurt, bobTeam(), SEED,
			fromAnn::add);
		FriendTurnBattle seenByBob = new FriendTurnBattle(BATTLE, ANN, "Ann", false, bobTeam(),
			new ArrayList<>(Arrays.asList(new Combatant(IMP, 10, 0.5), new Combatant(CHICKEN, 5, 1.0))), SEED,
			fromBob::add);

		hurtAnn.opened(0);
		seenByBob.opened(0);
		assertTrue(hurtAnn.local(BattleChoice.item(ItemType.LARGE_SALVE, 0), 0));
		assertTrue(seenByBob.local(BattleChoice.attack(), 0));
		for (GielinorGoPartyMessage message : fromAnn)
			seenByBob.remote(from(ANN, message), 0);

		for (GielinorGoPartyMessage message : fromBob)
			hurtAnn.remote(from(BOB, message), 0);

		assertNull(hurtAnn.ended());
		assertNull(seenByBob.ended());
		FriendTurnBattle.Played played = hurtAnn.poll();
		assertNotNull(played);
		assertEquals(ItemType.LARGE_SALVE, played.mine().itemType());
		assertEquals(hurtAnn.state().stateHash(), seenByBob.state().stateHash());
	}
}
