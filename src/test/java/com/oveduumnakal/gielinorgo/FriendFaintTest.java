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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Friend battles: a fainted creature is not sent, one that reaches 0 health faints, and nothing else is written. */
public class FriendFaintTest
{
	private static final long ANN = 11L;
	private static final long BOB = 22L;
	private static final Species IMP = new Species("Imp", 7, CombatType.STAB);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private final SpeciesTable table = new SpeciesTable(Arrays.asList(IMP, CHICKEN, DRAGON));
	private final List<GielinorGoPartyMessage> fromAnn = new ArrayList<>();
	private final List<GielinorGoPartyMessage> fromBob = new ArrayList<>();
	private final List<String> annTold = new ArrayList<>();
	private final List<String> bobTold = new ArrayList<>();
	private final SaveData annData = SaveData.fresh();
	private final SaveData bobData = SaveData.fresh();
	private long now = 1_000_000L;
	private FriendBattles ann;
	private FriendBattles bob;

	@Before
	public void setUp()
	{
		ann = new FriendBattles(table, new Random(1), () -> now, message -> fromAnn.add(sentBy(message, ANN)),
			notice -> annTold.add(notice.text()));
		bob = new FriendBattles(table, new Random(2), () -> now, message -> fromBob.add(sentBy(message, BOB)),
			notice -> bobTold.add(notice.text()));
	}

	private static GielinorGoPartyMessage sentBy(GielinorGoPartyMessage message, long memberId)
	{
		message.setMemberId(memberId);
		return message;
	}

	private static <T> T last(List<T> list)
	{
		return list.get(list.size() - 1);
	}

	private void meet()
	{
		ann.hello(annData, false);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		bob.hello(bobData, false);
		ann.receive(last(fromBob), ANN, "Bob", annData, true);
		ann.takeHelloWanted();
	}

	private static Creature fainted(String species, int level, long now)
	{
		Creature creature = new Creature(species, level, now);
		creature.faint(now);
		return creature;
	}

	@Test
	public void aFaintedCreatureIsLeftOutOfTheChallengeButTheHelloStillListsEveryone()
	{
		annData.team.add(new Creature("imp", 10, now));
		annData.team.add(fainted("king black dragon", 60, now));
		annData.team.add(new Creature("chicken", 5, now));
		bobData.team.add(new Creature("chicken", 5, now));

		ann.hello(annData, false);
		GielinorGoPartyMessage hello = last(fromAnn);
		assertEquals(3, hello.species().length);

		meet();
		ann.challenge(BOB, "Bob", annData);

		GielinorGoPartyMessage sent = last(fromAnn);
		assertEquals(GielinorGoPartyMessage.CHALLENGE, sent.kind());
		assertEquals(Arrays.asList("imp", "chicken"), Arrays.asList(sent.species()));
	}

	@Test
	public void withNoCreatureFitThePlayerCannotChallenge()
	{
		annData.team.add(fainted("imp", 10, now));
		bobData.team.add(new Creature("chicken", 5, now));
		meet();
		fromAnn.clear();

		ann.challenge(BOB, "Bob", annData);

		assertTrue(fromAnn.isEmpty());
		assertEquals(GoGame.FAINTED_TEAM_TEXT, last(annTold));
	}

	@Test
	public void aPlayerWhoseTeamHasAllFaintedDeclinesAChallenge()
	{
		annData.team.add(new Creature("imp", 10, now));
		bobData.team.add(fainted("chicken", 5, now));
		meet();
		ann.challenge(BOB, "Bob", annData);

		assertNull(bob.receive(last(fromAnn), BOB, "Ann", bobData, true));

		assertEquals(GielinorGoPartyMessage.DECLINE, last(fromBob).kind());
	}

	@Test
	public void theCreaturesThatLostFaintAndTheOnesThatWonAreNotWrittenTo()
	{
		Creature dragon = new Creature("king black dragon", 60, now);
		Creature imp = new Creature("imp", 10, now);
		annData.team.add(imp);
		annData.team.add(fainted("chicken", 5, now));
		annData.team.add(dragon);
		Creature chicken = new Creature("chicken", 5, now);
		bobData.team.add(chicken);
		meet();
		long stamp = dragon.hpStampMs;

		ann.challenge(BOB, "Bob", annData);
		TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

		assertTrue(annSaw.won());
		assertFalse(bobSaw.won());
		assertTrue("Bob's only creature lost", chicken.fainted);
		assertTrue(bobTold.contains("Chicken fainted. Revive it at a bank"));
		assertFalse("the dragon won, so it stands", dragon.fainted);
		assertEquals("a survivor's health is not written", 1.0, dragon.hpFraction, 1e-9);
		assertEquals(stamp, dragon.hpStampMs);
		assertFalse("the imp never fought", imp.fainted);
	}

	@Test
	public void theFighterThatFellMapsToTheRightCreatureWhenAFaintedOneSitsInTheMiddle()
	{
		Creature chicken = new Creature("chicken", 5, now);
		Creature imp = new Creature("imp", 10, now);
		Creature oldFaint = fainted("king black dragon", 60, now);
		annData.team.add(chicken);
		annData.team.add(oldFaint);
		annData.team.add(imp);
		bobData.team.add(new Creature("king black dragon", 60, now));
		meet();
		now += 5_000;

		ann.challenge(BOB, "Bob", annData);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		ann.receive(last(fromBob), ANN, "Bob", annData, true);

		assertTrue(chicken.fainted);
		assertTrue(imp.fainted);
		assertEquals("the old faint was not touched", now - 5_000, oldFaint.hpStampMs);
	}

	@Test
	public void theChallengedSideFaintsToo()
	{
		annData.team.add(new Creature("king black dragon", 60, now));
		Creature imp = new Creature("imp", 10, now);
		bobData.team.add(imp);
		meet();

		ann.challenge(BOB, "Bob", annData);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);

		assertTrue(imp.fainted);
	}

	@Test
	public void theTurnBattleFaintsAFighterThatFellAndWritesNothingElse()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(IMP, CHICKEN, DRAGON));
		GoGame game = new GoGame(species, new Random(5), () -> now, notice ->
		{
		});
		Creature weak = new Creature("imp", 3, now);
		Creature old = fainted("chicken", 5, now);
		long oldStamp = old.hpStampMs;
		game.data().team.add(weak);
		game.data().team.add(old);
		List<GielinorGoPartyMessage> fromMe = new ArrayList<>();
		List<GielinorGoPartyMessage> fromFriend = new ArrayList<>();
		FriendTurnBattle mine = new FriendTurnBattle(5L, 2L, "Bob", true,
			new ArrayList<>(Arrays.asList(new Combatant(IMP, 3, 1.0))),
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 1.0))), 99L, fromMe::add);
		FriendTurnBattle friend = new FriendTurnBattle(5L, 1L, "Ann", false,
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 1.0))),
			new ArrayList<>(Arrays.asList(new Combatant(IMP, 3, 1.0))), 99L, fromFriend::add);
		BattleSession session = game.startFriendBattle(mine);
		assertEquals("the fainted creature is not a fighter", 1, session.fighters().size());

		for (int i = 0; i < 100 && !mine.over(); i++)
		{
			mine.opened(now);
			friend.opened(now);
			if (mine.mustChoose())
				mine.local(mine.automatic(), now);

			if (friend.mustChoose())
				friend.local(friend.automatic(), now);

			for (GielinorGoPartyMessage message : fromMe)
			{
				message.setMemberId(1L);
				friend.remote(message, now);
			}

			for (GielinorGoPartyMessage message : fromFriend)
			{
				message.setMemberId(2L);
				mine.remote(message, now);
			}

			fromMe.clear();
			fromFriend.clear();
			TurnResult turn = game.friendTurn(session);
			while (turn != null)
				turn = game.friendTurn(session);
		}

		assertTrue(mine.over());
		assertNotNull(mine.state().ended());
		assertTrue(weak.fainted);
		assertEquals(oldStamp, old.hpStampMs);
		assertEquals(0, game.data().battlesWon);
	}

	@Test
	public void aSurvivorOfATurnBattleKeepsItsHealthInTheSave()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(IMP, CHICKEN, DRAGON));
		GoGame game = new GoGame(species, new Random(5), () -> now, notice ->
		{
		});
		Creature strong = new Creature("king black dragon", 60, now);
		game.data().team.add(strong);
		FriendTurnBattle mine = new FriendTurnBattle(5L, 2L, "Bob", true,
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 0.5))),
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))), 99L, message ->
			{
			});
		BattleSession session = game.startFriendBattle(mine);

		game.forfeit(session);

		assertFalse(strong.fainted);
		assertEquals(1.0, strong.hpFraction, 1e-9);
	}

	@Test
	public void crossingChallengesAreBothDeclinedAndNoBattleIsFought()
	{
		annData.team.add(new Creature("imp", 10, now));
		bobData.team.add(new Creature("chicken", 5, now));
		meet();
		ann.challenge(BOB, "Bob", annData);
		bob.challenge(ANN, "Ann", bobData);
		GielinorGoPartyMessage fromAnnFirst = last(fromAnn);
		GielinorGoPartyMessage fromBobFirst = last(fromBob);

		assertNull(bob.receive(fromAnnFirst, BOB, "Ann", bobData, true));
		assertNull(ann.receive(fromBobFirst, ANN, "Bob", annData, true));

		assertEquals(GielinorGoPartyMessage.DECLINE, last(fromAnn).kind());
		assertEquals(GielinorGoPartyMessage.DECLINE, last(fromBob).kind());
	}

	@Test
	public void anAnswerThatArrivesWhileBusyStartsNoBattleEvenWhenNotSteered()
	{
		Creature imp = new Creature("imp", 3, now);
		annData.team.add(imp);
		bobData.team.add(new Creature("king black dragon", 60, now));
		meet();
		ann.challenge(BOB, "Bob", annData);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);

		assertNull(ann.receive(last(fromBob), ANN, "Bob", annData, false, false, false));

		assertFalse(imp.fainted);
		assertEquals(GielinorGoPartyMessage.LEAVE, last(fromAnn).kind());
	}

	@Test
	public void aTurnBattleKeepsTheCreaturesThatWereChallengedAfterAReorder()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(IMP, CHICKEN, DRAGON));
		GoGame game = new GoGame(species, new Random(5), () -> now, notice ->
		{
		});
		Creature first = new Creature("imp", 3, now);
		Creature second = new Creature("imp", 4, now);
		game.data().team.add(first);
		game.data().team.add(second);
		bobData.team.add(new Creature("chicken", 5, now));
		meet();
		ann.challenge(BOB, "Bob", game.data(), true);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true, true);
		Collections.swap(game.data().team, 0, 1);

		ann.receive(last(fromBob), ANN, "Bob", game.data(), true, true);
		BattleSession session = game.startFriendBattle(ann.takeStarted());

		assertTrue(session.fighters().get(0) == first);
		assertTrue(session.fighters().get(1) == second);
	}
}
