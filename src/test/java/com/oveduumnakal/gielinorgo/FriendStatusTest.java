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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Friend battles full of statuses: both players see the same battle, down to every status line. */
public class FriendStatusTest
{
	private static final long ANN = 11L;
	private static final long BOB = 22L;
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);
	private static final Species SPIDER = new Species("Spider", 40, CombatType.MELEE, Trait.POISONOUS);
	private static final String MOVES = "{\"moves\":["
		+ "{\"id\":\"bite\",\"name\":\"Bite\",\"style\":\"MELEE\",\"power\":1.0,\"accuracy\":1.0},"
		+ "{\"id\":\"venom\",\"name\":\"Venom\",\"style\":\"MELEE\",\"power\":1.2,\"accuracy\":0.9,"
		+ "\"effect\":{\"status\":\"VENOM\",\"chance\":0.5}},"
		+ "{\"id\":\"ice\",\"name\":\"Ice\",\"style\":\"MAGIC\",\"power\":1.4,\"accuracy\":0.9,\"slot\":\"SECOND\","
		+ "\"effect\":{\"status\":\"FREEZE\",\"chance\":0.4}},"
		+ "{\"id\":\"bind\",\"name\":\"Bind\",\"style\":\"MAGIC\",\"power\":0.8,\"accuracy\":1.0,"
		+ "\"effect\":{\"status\":\"STUN\",\"chance\":0.5}}],"
		+ "\"defaults\":{\"MELEE\":\"all\"},"
		+ "\"pools\":{\"all\":{\"styles\":[\"MELEE\"],"
		+ "\"learns\":[[\"bite\",1],[\"venom\",5],[\"ice\",10],[\"bind\",15]]}},"
		+ "\"species\":{}}";

	private final SpeciesTable table = new SpeciesTable(Arrays.asList(DRAGON, SPIDER));
	private final MoveTable moves = MoveTable.load(new Gson(), new StringReader(MOVES));

	private static <T> T last(List<T> list)
	{
		return list.get(list.size() - 1);
	}

	@Test
	public void bothPlayersSeeTheSameLinesOfABattleFullOfStatuses()
	{
		int statusLines = 0;
		for (int seed = 1; seed <= 20; seed++)
		{
			List<GielinorGoPartyMessage> fromAnn = new ArrayList<>();
			List<GielinorGoPartyMessage> fromBob = new ArrayList<>();
			SaveData annData = SaveData.fresh();
			SaveData bobData = SaveData.fresh();
			long now = 1_000_000L;
			FriendBattles ann = new FriendBattles(table, new Random(seed), () -> now, message ->
			{
				message.setMemberId(ANN);
				fromAnn.add(message);
			}, notice ->
			{
			}).withMoves(moves);
			FriendBattles bob = new FriendBattles(table, new Random(seed + 500), () -> now, message ->
			{
				message.setMemberId(BOB);
				fromBob.add(message);
			}, notice ->
			{
			}).withMoves(moves);
			annData.team.add(new Creature("king black dragon", 45, now));
			annData.team.add(new Creature("spider", 30, now));
			bobData.team.add(new Creature("spider", 45, now));
			bobData.team.add(new Creature("king black dragon", 30, now));
			ann.hello(annData, false);
			bob.receive(last(fromAnn), BOB, "Ann", bobData, true);
			bob.hello(bobData, false);
			ann.receive(last(fromBob), ANN, "Bob", annData, true);

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
				List<BattleLine> one = annDuels.get(i).hits();
				List<BattleLine> other = bobDuels.get(i).hits();
				assertEquals(one.size(), other.size());
				for (int j = 0; j < one.size(); j++)
				{
					BattleLine mineLine = one.get(j);
					assertEquals(mineLine.text(), other.get(j).text());
					statusLines += mineLine.kind() == BattleLine.Kind.STATUS ? 1 : 0;
				}
			}
		}

		assertTrue("statuses were in play: " + statusLines, statusLines > 0);
	}
}
