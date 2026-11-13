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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Friend battles with moves: the moves cross intact, both sides see the same battle, a bad move is refused. */
public class FriendMovesTest
{
	private static final long ANN = 11L;
	private static final long BOB = 22L;
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);
	private static final Species IMP = new Species("Imp", 7, CombatType.MELEE);
	private static final String MOVES = "{\"moves\":["
		+ "{\"id\":\"bite\",\"name\":\"Bite\",\"style\":\"MELEE\",\"power\":1.0,\"accuracy\":1.0},"
		+ "{\"id\":\"claw\",\"name\":\"Claw\",\"style\":\"MELEE\",\"power\":1.3,\"accuracy\":0.9},"
		+ "{\"id\":\"fire\",\"name\":\"Fire\",\"style\":\"MAGIC\",\"power\":1.6,\"accuracy\":0.8,\"slot\":\"SECOND\"},"
		+ "{\"id\":\"inferno\",\"name\":\"Inferno\",\"style\":\"MAGIC\",\"power\":2.2,\"accuracy\":0.7,"
		+ "\"slot\":\"SECOND\"}],"
		+ "\"defaults\":{\"MELEE\":\"all\"},"
		+ "\"pools\":{\"all\":{\"styles\":[\"MELEE\"],"
		+ "\"learns\":[[\"bite\",1],[\"claw\",10],[\"fire\",20],[\"inferno\",40]]}},"
		+ "\"species\":{}}";

	private final SpeciesTable table = new SpeciesTable(Arrays.asList(DRAGON, IMP));
	private final MoveTable moves = MoveTable.load(new Gson(), new StringReader(MOVES));
	private final List<GielinorGoPartyMessage> fromAnn = new ArrayList<>();
	private final List<GielinorGoPartyMessage> fromBob = new ArrayList<>();
	private final SaveData annData = SaveData.fresh();
	private final SaveData bobData = SaveData.fresh();
	private long now = 1_000_000L;
	private FriendBattles ann;
	private FriendBattles bob;

	@Before
	public void setUp()
	{
		ann = new FriendBattles(table, new Random(1), () -> now, message -> fromAnn.add(sentBy(message, ANN)),
			notice ->
			{
			}).withMoves(moves);
		bob = new FriendBattles(table, new Random(2), () -> now, message -> fromBob.add(sentBy(message, BOB)),
			notice ->
			{
			}).withMoves(moves);
		Creature dragon = new Creature("king black dragon", 45, now);
		dragon.moves = new ArrayList<>(Arrays.asList("bite", "fire", "claw"));
		annData.team.add(dragon);
		bobData.team.add(new Creature("imp", 45, now));
	}

	private static GielinorGoPartyMessage sentBy(GielinorGoPartyMessage message, long id)
	{
		message.setMemberId(id);
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
	}

	private GielinorGoPartyMessage challengeWith(String[][] sentMoves)
	{
		GielinorGoPartyMessage message = new GielinorGoPartyMessage(GielinorGoPartyMessage.CHALLENGE, BOB, 1, 2, 3,
			new String[] {"imp"}, new int[] {20});
		return sentBy(message.moved(sentMoves), ANN);
	}

	@Test
	public void aTeamMessageCarriesTheMovesEachCreatureKnows()
	{
		ann.hello(annData, false);

		GielinorGoPartyMessage hello = last(fromAnn);

		assertEquals(1, hello.moves().length);
		assertArrayEquals(new String[] {"bite", "fire", "claw"}, hello.moves()[0]);
		assertEquals(GielinorGoPartyMessage.PROTOCOL, hello.version());
		assertEquals(3, GielinorGoPartyMessage.PROTOCOL);
	}

	@Test
	public void aFriendsMovesReachTheTradeList()
	{
		meet();

		List<GielinorGoPartyMessage.Item> offered = bob.tradeable(ANN);

		assertArrayEquals(new String[] {"bite", "fire", "claw"}, offered.get(0).moves());
	}

	@Test
	public void aCreatureWithMovesItsSpeciesCannotKnowSendsOnlyTheRightOnes()
	{
		annData.team.get(0).moves = new ArrayList<>(Arrays.asList("bite", "nonsense", "inferno"));

		ann.hello(annData, false);

		assertArrayEquals("inferno is learned at 40 and the dragon is 45, nonsense is unknown",
			new String[] {"bite", "inferno"}, last(fromAnn).moves()[0]);

		annData.team.get(0).moves = new ArrayList<>(Arrays.asList("inferno"));
		annData.team.get(0).xp = XpRules.xpForLevel(30);
		ann.hello(annData, false);

		assertArrayEquals("a move the level has not unlocked is not sent, so the level decides",
			new String[0], last(fromAnn).moves()[0]);
	}

	@Test
	public void bothPlayersSeeTheSameBattleWithMovesAndTheLinesNameThem()
	{
		int named = 0;
		for (int seed = 1; seed <= 20; seed++)
		{
			fromAnn.clear();
			fromBob.clear();
			ann = new FriendBattles(table, new Random(seed), () -> now, message -> fromAnn.add(sentBy(message, ANN)),
				notice ->
				{
				}).withMoves(moves);
			bob = new FriendBattles(table, new Random(seed + 500), () -> now,
				message -> fromBob.add(sentBy(message, BOB)), notice ->
				{
				}).withMoves(moves);
			meet();

			ann.challenge(BOB, "Bob", annData);
			TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
			TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

			assertNotNull(bobSaw);
			assertNotNull(annSaw);
			assertEquals("seed " + seed, annSaw.won(), !bobSaw.won());
			List<Duel> annDuels = annSaw.duels();
			List<Duel> bobDuels = bobSaw.duels();
			assertEquals(annDuels.size(), bobDuels.size());
			for (int i = 0; i < annDuels.size(); i++)
			{
				List<BattleLine> annHits = annDuels.get(i).hits();
				List<BattleLine> bobHits = bobDuels.get(i).hits();
				assertEquals(annHits.size(), bobHits.size());
				for (int j = 0; j < annHits.size(); j++)
				{
					BattleLine one = annHits.get(j);
					BattleLine other = bobHits.get(j);
					assertEquals(one.text(), other.text());
					assertEquals(one.miss(), other.miss());
					assertEquals(one.special(), other.special());
					named += one.text().contains("King Black Dragon uses ") ? 1 : 0;
				}
			}

			now += FriendBattles.REST_MS;
		}

		assertTrue("the dragon's named moves show in the lines: " + named, named > 0);
	}

	@Test
	public void movesTheCreatureCannotKnowTurnTheChallengeAway()
	{
		meet();

		assertNotNull("the same message with moves that are fine", receiveChallenge(new String[][] {{"bite", "fire"}},
			20));
		now += FriendBattles.REST_MS;
		assertNull("too early: fire needs level 20", receiveChallenge(new String[][] {{"bite", "fire"}}, 10));
		assertNull("unknown", receiveChallenge(new String[][] {{"bite", "zzz"}}, 20));
		assertNull("repeated", receiveChallenge(new String[][] {{"bite", "bite"}}, 20));
		assertNull("five", receiveChallenge(new String[][] {{"bite", "claw", "fire", "inferno", "bite"}}, 50));
		assertNull("a blank", receiveChallenge(new String[][] {{"bite", null}}, 20));
		assertNull("one list too few", receiveChallenge(new String[][] {{"bite"}, {"claw"}}, 20));
	}

	@Test
	public void aCreatureWithNoListOfMovesUsesTheMovesItsLevelUnlocked()
	{
		meet();

		assertNotNull(receiveChallenge(new String[][] {null}, 20));
		now += FriendBattles.REST_MS;
		assertNotNull(receiveChallenge(new String[][] {{}}, 20));
	}

	private TeamBattleResult receiveChallenge(String[][] sentMoves, int level)
	{
		GielinorGoPartyMessage message = new GielinorGoPartyMessage(GielinorGoPartyMessage.CHALLENGE, BOB, 1, 2, 3,
			new String[] {"imp"}, new int[] {level});
		return bob.receive(sentBy(message.moved(sentMoves), ANN), BOB, "Ann", bobData, true);
	}

	@Test
	public void aTeamThatNamesNoMovesFightsWithTheMovesItsLevelUnlocked()
	{
		meet();

		TeamBattleResult plain = bob.receive(challengeWith(null), BOB, "Ann", bobData, true);
		assertNotNull("no moves sent means the level decides", plain);
		assertFalse(plain.lines().isEmpty());
	}

	@Test
	public void aBattleWithoutAMoveTableTurnsAwayAMessageThatNamesMoves()
	{
		FriendBattles bare = new FriendBattles(table, new Random(3), () -> now, message ->
		{
		}, notice ->
		{
		});

		GielinorGoPartyMessage message = challengeWith(new String[][] {{"bite"}});

		assertNull(bare.receive(message, BOB, "Ann", bobData, true));
	}
}
