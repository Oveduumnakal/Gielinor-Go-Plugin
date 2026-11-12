/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Plays friend battles between two players whose messages are handed to each other by the test. */
public class FriendBattlesTest
{
	private static final long ANN = 11L;
	private static final long BOB = 22L;
	private static final long CAT = 33L;
	private static final Species IMP = new Species("Imp", 7, CombatType.MELEE);
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.MELEE);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);

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
		annData.team.add(new Creature("king black dragon", 60, now));
		annData.team.add(new Creature("imp", 10, now));
		bobData.team.add(new Creature("chicken", 5, now));
		bobData.team.add(new Creature("imp", 12, now));
	}

	@Test
	public void aHelloIntroducesAPlayerAndIsOwedOneBack()
	{
		ann.hello(annData, false);
		assertNull(bob.receive(last(fromAnn), BOB, "Ann", bobData, true));

		List<FriendView> friends = bob.views(names());
		assertEquals(1, friends.size());
		assertEquals(ANN, friends.get(0).memberId());
		assertEquals("Ann", friends.get(0).name());
		assertEquals("King Black Dragon 60, Imp 10", friends.get(0).team());
		assertTrue(friends.get(0).challengeable());
		assertFalse(friends.get(0).waiting());
		assertTrue(bob.takeHelloWanted());
		assertFalse(bob.takeHelloWanted());
	}

	@Test
	public void aSecondHelloIsOnlyAnsweredWhenItAsks()
	{
		meet();

		ann.hello(annData, false);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		assertFalse(bob.takeHelloWanted());

		ann.hello(annData, true);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		assertTrue(bob.takeHelloWanted());
	}

	@Test
	public void aFriendWhoLeftThePartyIsNotListed()
	{
		meet();

		assertTrue(bob.views(Collections.emptyMap()).isEmpty());
		bob.forget(ANN);
		assertTrue(bob.views(names()).isEmpty());
	}

	@Test
	public void bothPlayersSeeTheSameBattleFromTheirOwnSide()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		assertEquals("Challenged Bob!", last(annTold));
		assertTrue(annWaitsForBob());

		TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		assertNotNull(bobSaw);
		assertEquals(GielinorGoPartyMessage.ACCEPT, last(fromBob).kind());
		TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);
		assertNotNull(annSaw);

		assertTrue("a level 60 dragon should beat a chicken and an imp", annSaw.won());
		assertFalse(bobSaw.won());
		List<String> annRead = texts(annSaw);
		List<String> bobRead = texts(bobSaw);
		assertEquals(annRead.size(), bobRead.size());
		assertEquals("Battle with Bob!", annRead.get(0));
		assertEquals("Battle with Ann!", bobRead.get(0));
		assertEquals("Your King Black Dragon beat Bob's Chicken (Lv 5)", annRead.get(1));
		assertEquals("Your Chicken lost to Ann's King Black Dragon (Lv 60)", bobRead.get(1));
		assertEquals("You beat Bob!", last(annRead));
		assertEquals("Ann won this one", last(bobRead));
		assertFalse(annWaitsForBob());
	}

	@Test
	public void bothSidesGetTheSameDuelsSeenFromTheirOwnSide()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

		List<Duel> annDuels = annSaw.duels();
		List<Duel> bobDuels = bobSaw.duels();

		assertFalse(annDuels.isEmpty());
		assertEquals(annDuels.size(), bobDuels.size());
		assertTrue("both players' first creature is their lead", annSaw.leadFirst() && bobSaw.leadFirst());
		for (int i = 0; i < annDuels.size(); i++)
		{
			Duel mine = annDuels.get(i);
			Duel theirs = bobDuels.get(i);

			assertEquals(mine.mineIndex(), theirs.theirIndex());
			assertEquals(mine.theirIndex(), theirs.mineIndex());
			assertEquals(mine.mineSpecies(), theirs.theirSpecies());
			assertEquals(mine.theirSpecies(), theirs.mineSpecies());
			assertEquals(mine.mineLevel(), theirs.theirLevel());
			assertEquals(mine.theirLevel(), theirs.mineLevel());
			assertNotEquals(mine.mineWon(), theirs.mineWon());
			assertTrue(annSaw.lines().contains(mine.knockOut()));
			assertTrue(bobSaw.lines().contains(theirs.knockOut()));
			List<BattleLine> mineHits = mine.hits();
			List<BattleLine> theirHits = theirs.hits();

			assertEquals(mineHits.size(), theirHits.size());
			for (int j = 0; j < mineHits.size(); j++)
			{
				BattleLine one = mineHits.get(j);
				BattleLine other = theirHits.get(j);

				assertEquals(mirrored(one.kind()), other.kind());
				assertEquals(one.style(), other.style());
				assertEquals(one.overPlayer(), !other.overPlayer());
				assertTrue(one.text() + " and " + other.text() + " tell the same hit",
					one.text().endsWith(other.text()) || other.text().endsWith(one.text()));
			}
		}
	}

	@Test
	public void aLeadLeftOutOfTheTeamMeansTheFirstFighterIsNotTheLead()
	{
		annData.team.add(0, new Creature("kraken", 5, now));
		meet();
		ann.challenge(BOB, "Bob", annData);
		TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

		assertFalse("a creature this version does not know is left out, so the lead does not fight",
			annSaw.leadFirst());
		assertTrue(bobSaw.leadFirst());
	}

	@Test
	public void bothPlayersSeeTheSameBattleWhenADragonBreathes()
	{
		Species green = new Species("Green dragon", 88, CombatType.MELEE);
		SpeciesTable dragons = new SpeciesTable(Arrays.asList(IMP, CHICKEN, green));
		annData.team.clear();
		bobData.team.clear();
		annData.team.add(new Creature("green dragon", 50, now));
		bobData.team.add(new Creature("green dragon", 50, now));
		int annWins = 0;

		for (int seed = 1; seed <= 40; seed++)
		{
			fromAnn.clear();
			fromBob.clear();
			ann = new FriendBattles(dragons, new Random(seed), () -> now, message -> fromAnn.add(sentBy(message, ANN)),
				notice -> annTold.add(notice.text()));
			bob = new FriendBattles(dragons, new Random(seed + 1000), () -> now,
				message -> fromBob.add(sentBy(message, BOB)), notice -> bobTold.add(notice.text()));
			meet();

			ann.challenge(BOB, "Bob", annData);
			TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
			TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

			assertNotNull(bobSaw);
			assertNotNull(annSaw);
			assertEquals("seed " + seed, annSaw.won(), !bobSaw.won());
			assertEquals("seed " + seed, annSaw.lines().size(), bobSaw.lines().size());
			List<BattleLine> annLines = annSaw.lines();
			List<BattleLine> bobLines = bobSaw.lines();
			String annLine = annLines.get(1).text();
			String bobLine = bobLines.get(1).text();
			String annVerdict = annSaw.won() ? "beat" : "lost to";
			String bobVerdict = annSaw.won() ? "lost to" : "beat";

			assertEquals("Your Green dragon " + annVerdict + " Bob's Green dragon (Lv 50)", annLine);
			assertEquals("Your Green dragon " + bobVerdict + " Ann's Green dragon (Lv 50)", bobLine);
			annWins += annSaw.won() ? 1 : 0;
		}

		assertTrue("equal dragons should each win some of 40 battles, Ann won " + annWins,
			annWins > 0 && annWins < 40);
	}

	@Test
	public void evenTeamsStillAgreeOnTheWinner()
	{
		annData.team.clear();
		bobData.team.clear();
		for (int i = 0; i < SaveData.TEAM_SIZE; i++)
		{
			annData.team.add(new Creature("imp", 20 + i, now));
			bobData.team.add(new Creature("imp", 25 - i, now));
		}

		meet();
		for (int round = 0; round < 20; round++)
		{
			now += FriendBattles.REST_MS;
			ann.challenge(BOB, "Bob", annData);
			TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
			TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

			assertNotEquals("round " + round, annSaw.won(), bobSaw.won());
			assertEquals("round " + round, texts(annSaw).size(), texts(bobSaw).size());
		}
	}

	@Test
	public void aFriendBattleChangesNothingInTheSave()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		ann.receive(last(fromBob), ANN, "Bob", annData, true);

		assertEquals(0, annData.battlesWon);
		assertEquals(0, bobData.battlesWon);
		assertEquals(1.0, bobData.team.get(0).hpFraction, 0.0);
		assertEquals(0, annData.trainerXp);
	}

	@Test
	public void aPlayerWhoDoesNotTakeBattlesDeclines()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);

		assertNull(bob.receive(last(fromAnn), BOB, "Ann", bobData, false));
		assertEquals(GielinorGoPartyMessage.DECLINE, last(fromBob).kind());
		assertNull(ann.receive(last(fromBob), ANN, "Bob", annData, true));
		assertEquals("Bob is not battling right now", last(annTold));
		assertFalse(annWaitsForBob());
	}

	@Test
	public void aSecondChallengeRightAfterABattleIsDeclined()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		ann.receive(last(fromBob), ANN, "Bob", annData, true);

		now += FriendBattles.REST_MS - 1;
		ann.challenge(BOB, "Bob", annData);
		assertNull(bob.receive(last(fromAnn), BOB, "Ann", bobData, true));
		assertEquals(GielinorGoPartyMessage.DECLINE, last(fromBob).kind());
	}

	@Test
	public void aChallengeMeantForSomeoneElseIsIgnored()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		int sentBefore = fromBob.size();

		assertNull(bob.receive(last(fromAnn), CAT, "Ann", bobData, true));
		assertEquals(sentBefore, fromBob.size());
	}

	@Test
	public void aPlayersOwnMessageIsIgnored()
	{
		ann.hello(annData, true);

		assertNull(ann.receive(last(fromAnn), ANN, "Ann", annData, true));
		assertTrue(ann.views(names()).isEmpty());
		assertFalse(ann.takeHelloWanted());
	}

	@Test
	public void anAnswerNobodyAskedForStartsNoBattle()
	{
		meet();
		GielinorGoPartyMessage forged = sentBy(new GielinorGoPartyMessage(GielinorGoPartyMessage.ACCEPT, ANN, 5, 5, 1,
			new String[]{"imp"}, new int[]{99}), BOB);

		assertNull(ann.receive(forged, ANN, "Bob", annData, true));
	}

	@Test
	public void anAnswerFromTheWrongPlayerStartsNoBattle()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		GielinorGoPartyMessage challenge = last(fromAnn);
		GielinorGoPartyMessage forged = sentBy(new GielinorGoPartyMessage(GielinorGoPartyMessage.ACCEPT, ANN,
			challenge.battle(), 5, 1, new String[]{"imp"}, new int[]{99}), CAT);

		assertNull(ann.receive(forged, ANN, "Cat", annData, true));
		assertTrue("the real answer must still count", annWaitsForBob());
	}

	@Test
	public void aTeamThatMakesNoSenseIsTurnedAway()
	{
		assertNull(bob.receive(challengeWith(new String[]{"imp"}, new int[]{100}), BOB, "Ann", bobData, true));
		assertNull(bob.receive(challengeWith(new String[]{"imp"}, new int[]{0}), BOB, "Ann", bobData, true));
		assertNull(bob.receive(challengeWith(new String[]{"dragon of doom"}, new int[]{5}), BOB, "Ann", bobData, true));
		assertNull(bob.receive(challengeWith(new String[]{"imp", "imp"}, new int[]{5}), BOB, "Ann", bobData, true));
		assertNull(bob.receive(challengeWith(new String[]{null}, new int[]{5}), BOB, "Ann", bobData, true));
		assertNull(bob.receive(challengeWith(null, null), BOB, "Ann", bobData, true));
		assertNull(bob.receive(challengeWith(new String[0], new int[0]), BOB, "Ann", bobData, true));
		String[] tooMany = new String[SaveData.TEAM_SIZE + 1];
		Arrays.fill(tooMany, "imp");
		assertNull(bob.receive(challengeWith(tooMany, new int[SaveData.TEAM_SIZE + 1]), BOB, "Ann", bobData, true));

		assertTrue("nothing may be sent back for a message that makes no sense", fromBob.isEmpty());
		assertTrue(bob.views(names()).isEmpty());
	}

	@Test
	public void aMessageWithNoKindIsIgnored()
	{
		GielinorGoPartyMessage empty = sentBy(new GielinorGoPartyMessage(), ANN);

		assertNull(bob.receive(empty, BOB, "Ann", bobData, true));
		assertNull(bob.receive(sentBy(new GielinorGoPartyMessage("DANCE", BOB, 0, 0, 1, null, null), ANN), BOB, "Ann",
			bobData, true));
	}

	@Test
	public void aChallengeNeedsATeamOnBothSides()
	{
		meet();
		SaveData nobody = SaveData.fresh();
		ann.challenge(BOB, "Bob", nobody);
		assertEquals("Catch a creature first", last(annTold));

		ann.challenge(CAT, "Cat", annData);
		assertEquals("Cat has no team to battle", last(annTold));
	}

	@Test
	public void onlyOneChallengeWaitsAtATime()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		int sent = fromAnn.size();
		ann.challenge(BOB, "Bob", annData);

		assertEquals(sent, fromAnn.size());
		assertEquals("Still waiting for an answer", last(annTold));
	}

	@Test
	public void aChallengeNobodyAnswersRunsOut()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		GielinorGoPartyMessage challenge = last(fromAnn);
		assertFalse(ann.expire());

		now += FriendBattles.CHALLENGE_TIMEOUT_MS + 1;
		assertTrue(ann.expire());
		assertEquals("Your challenge got no answer", last(annTold));
		assertFalse(ann.expire());

		bob.receive(challenge, BOB, "Ann", bobData, true);
		assertNull("an answer that comes too late starts no battle", ann.receive(last(fromBob), ANN, "Bob", annData,
			true));
	}

	@Test
	public void aCreatureOfAnUnknownSpeciesIsLeftOutOfTheTeamSent()
	{
		annData.team.add(0, new Creature("creature from a newer version", 50, now));
		ann.hello(annData, false);

		String[] sent = last(fromAnn).species();
		assertEquals(2, sent.length);
		assertEquals("king black dragon", sent[0]);
	}

	@Test
	public void aMessageSurvivesTheTripThroughTheParty()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		ann.hello(annData, true);
		Gson gson = new Gson();
		GielinorGoPartyMessage hello = last(fromAnn);
		GielinorGoPartyMessage challenge = fromAnn.get(fromAnn.size() - 2);

		String wire = gson.toJson(hello);
		GielinorGoPartyMessage arrived = gson.fromJson(wire, GielinorGoPartyMessage.class);
		assertFalse("the member id is filled in by the party service, never sent", wire.contains("memberId"));
		assertEquals(GielinorGoPartyMessage.HELLO, arrived.kind());
		assertTrue(arrived.askBack());
		assertEquals(1, arrived.trainer());
		assertArrayEquals(hello.species(), arrived.species());
		assertArrayEquals(hello.levels(), arrived.levels());

		GielinorGoPartyMessage copy = gson.fromJson(gson.toJson(challenge), GielinorGoPartyMessage.class);
		assertEquals(BOB, copy.to());
		assertEquals(challenge.battle(), copy.battle());
		assertEquals(challenge.seed(), copy.seed());
	}

	@Test
	public void rolledTeamsStillGiveBothPlayersTheSameWinner()
	{
		annData.team.clear();
		bobData.team.clear();
		Random dealer = new Random(99);
		for (int i = 0; i < SaveData.TEAM_SIZE; i++)
		{
			Creature mine = new Creature("imp", 20 + i, now);
			mine.setRolls(StatRolls.roll(dealer));
			annData.team.add(mine);
			Creature theirs = new Creature("imp", 20 + i, now);
			theirs.setRolls(StatRolls.roll(dealer));
			bobData.team.add(theirs);
		}

		meet();
		int annWins = 0;
		for (int round = 0; round < 30; round++)
		{
			now += FriendBattles.REST_MS;
			ann.challenge(BOB, "Bob", annData);
			TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
			TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

			assertNotNull("round " + round, bobSaw);
			assertNotNull("round " + round, annSaw);
			assertNotEquals("round " + round, annSaw.won(), bobSaw.won());
			assertEquals("round " + round, texts(annSaw).size(), texts(bobSaw).size());
			annWins += annSaw.won() ? 1 : 0;
		}

		assertTrue("both sides should win some rounds, Ann won " + annWins, annWins > 0 && annWins < 30);
	}

	@Test
	public void theRollsDecideAnOtherwiseEvenBattle()
	{
		annData.team.clear();
		bobData.team.clear();
		Creature strong = new Creature("imp", 30, now);
		strong.setRolls(new StatRolls(5, 5, 5, 5, Temperament.STEADY));
		Creature weak = new Creature("imp", 30, now);
		weak.setRolls(new StatRolls(-5, -5, -5, -5, Temperament.STEADY));
		annData.team.add(strong);
		bobData.team.add(weak);
		meet();

		for (int round = 0; round < 10; round++)
		{
			now += FriendBattles.REST_MS;
			ann.challenge(BOB, "Bob", annData);
			TeamBattleResult bobSaw = bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
			TeamBattleResult annSaw = ann.receive(last(fromBob), ANN, "Bob", annData, true);

			assertTrue("round " + round, annSaw.won());
			assertFalse("round " + round, bobSaw.won());
		}
	}

	@Test
	public void rollsTravelInTheMessageAndNicknamesDoNot()
	{
		Creature rolled = annData.team.get(1);
		rolled.setRolls(new StatRolls(5, 5, 3, 1, Temperament.RECKLESS));
		rolled.nickname = "Sparky";

		ann.hello(annData, false);
		GielinorGoPartyMessage hello = last(fromAnn);
		String wire = new Gson().toJson(hello);
		GielinorGoPartyMessage arrived = new Gson().fromJson(wire, GielinorGoPartyMessage.class);

		assertFalse(wire.contains("Sparky"));
		assertEquals(GielinorGoPartyMessage.PROTOCOL, arrived.version());
		assertArrayEquals(new int[] {5, 5, 3, 1}, arrived.rolls()[1]);
		assertArrayEquals(new int[] {0, 0, 0, 0}, arrived.rolls()[0]);
		assertEquals("RECKLESS", arrived.temperaments()[1]);
		assertNull(arrived.temperaments()[0]);

		arrived.setMemberId(ANN);
		bob.receive(arrived, BOB, "Ann", bobData, true);
		FriendView seen = bob.views(names()).get(0);
		assertEquals("King Black Dragon 60, Imp 10 (Exceptional)", seen.team());
	}

	@Test
	public void aTeamWithNoRollsAtAllFightsAtAverage()
	{
		String[] species = {"imp"};
		int[] levels = {20};
		GielinorGoPartyMessage plain = challengeWith(species, levels);
		assertNull(plain.rolls());
		assertNull(plain.temperaments());

		TeamBattleResult bobSaw = bob.receive(plain, BOB, "Ann", bobData, true);

		assertNotNull(bobSaw);
		assertEquals(GielinorGoPartyMessage.ACCEPT, last(fromBob).kind());
	}

	@Test
	public void aBadRollArrayDropsTheMessage()
	{
		rejects(new int[][] {{9, 0, 0, 0}}, null);
		rejects(new int[][] {{0, 0, 0, -6}}, null);
		rejects(new int[][] {{0, 0, 0}}, null);
		rejects(new int[][] {null}, null);
		rejects(new int[][] {{0, 0, 0, 0}, {0, 0, 0, 0}}, null);
		rejects(null, new String[] {"GRUMPY"});
		rejects(null, new String[] {"STEADY", "BRAVE"});

		assertTrue("nothing may be sent back for a message that makes no sense", fromBob.isEmpty());
		assertTrue(bob.views(names()).isEmpty());
		GielinorGoPartyMessage good = withRolls(new String[] {"imp"}, new int[] {20}, new int[][] {{5, -5, 0, 1}},
			new String[] {"BRAVE"});
		assertNotNull(bob.receive(good, BOB, "Ann", bobData, true));
	}

	@Test
	public void aChallengeFromAnotherVersionIsTurnedAway()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		GielinorGoPartyMessage old = otherVersion(last(fromAnn), 1);

		assertNull(bob.receive(old, BOB, "Ann", bobData, true));

		assertEquals("Ann runs a different version of Gielinor Go", last(bobTold));
		assertTrue("nothing is sent back to a different version", fromBob.size() == 1);
		assertNull(bob.receive(otherVersion(last(fromAnn), 0), BOB, "Ann", bobData, true));
	}

	@Test
	public void anAnswerFromAnotherVersionIsTurnedAway()
	{
		meet();
		ann.challenge(BOB, "Bob", annData);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);

		assertNull(ann.receive(otherVersion(last(fromBob), 3), ANN, "Bob", annData, true));

		assertEquals("Bob runs a different version of Gielinor Go", last(annTold));
	}

	@Test
	public void aHelloFromAnotherVersionStillIntroducesThePlayer()
	{
		ann.hello(annData, false);

		assertNull(bob.receive(otherVersion(last(fromAnn), 1), BOB, "Ann", bobData, true));

		assertEquals(1, bob.views(names()).size());
		assertTrue(bobTold.isEmpty());
	}

	private void rejects(int[][] rolls, String[] temperaments)
	{
		GielinorGoPartyMessage bad = withRolls(new String[] {"imp"}, new int[] {20}, rolls, temperaments);
		assertNull(bob.receive(bad, BOB, "Ann", bobData, true));
	}

	private GielinorGoPartyMessage withRolls(String[] species, int[] levels, int[][] rolls, String[] temperaments)
	{
		return challengeWith(species, levels).rolled(rolls, temperaments);
	}

	private static GielinorGoPartyMessage otherVersion(GielinorGoPartyMessage message, int version)
	{
		Gson gson = new Gson();
		String wire = gson.toJson(message).replace("\"version\":" + GielinorGoPartyMessage.PROTOCOL,
			"\"version\":" + version);
		GielinorGoPartyMessage copy = gson.fromJson(wire, GielinorGoPartyMessage.class);
		copy.setMemberId(message.getMemberId());
		return copy;
	}

	/** Lets the two players learn of each other, as two hellos through the party would. */
	private void meet()
	{
		ann.hello(annData, false);
		bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
		bob.hello(bobData, false);
		ann.receive(last(fromBob), ANN, "Bob", annData, true);
		ann.takeHelloWanted();
	}

	private boolean annWaitsForBob()
	{
		List<FriendView> friends = ann.views(names());
		return friends.get(0).waiting();
	}

	private static BattleLine.Kind mirrored(BattleLine.Kind kind)
	{
		if (kind == BattleLine.Kind.LEAD_HIT)
			return BattleLine.Kind.FOE_HIT;

		return kind == BattleLine.Kind.FOE_HIT ? BattleLine.Kind.LEAD_HIT : kind;
	}

	private static List<String> texts(TeamBattleResult result)
	{
		List<String> texts = new ArrayList<>();
		for (BattleLine line : result.lines())
			texts.add(line.text());

		return texts;
	}

	private GielinorGoPartyMessage challengeWith(String[] species, int[] levels)
	{
		return sentBy(new GielinorGoPartyMessage(GielinorGoPartyMessage.CHALLENGE, BOB, 1, 2, 3, species, levels), ANN);
	}

	private static GielinorGoPartyMessage sentBy(GielinorGoPartyMessage message, long memberId)
	{
		message.setMemberId(memberId);
		return message;
	}

	private static Map<Long, String> names()
	{
		Map<Long, String> names = new HashMap<>();
		names.put(ANN, "Ann");
		names.put(BOB, "Bob");
		return names;
	}

	private static <T> T last(List<T> list)
	{
		return list.get(list.size() - 1);
	}
}
