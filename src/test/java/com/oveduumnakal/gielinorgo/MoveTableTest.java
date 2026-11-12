/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;
import java.util.List;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks how a pool is read: the newest four moves, what a level range unlocks and what a creature may know. */
public class MoveTableTest
{
	private static final String JSON = "{\"moves\":["
		+ "{\"id\":\"a\",\"name\":\"A\",\"style\":\"MELEE\",\"power\":1.0,\"accuracy\":1.0},"
		+ "{\"id\":\"b\",\"name\":\"B\",\"style\":\"MELEE\",\"power\":1.2,\"accuracy\":0.9},"
		+ "{\"id\":\"c\",\"name\":\"C\",\"style\":\"MAGIC\",\"power\":1.5,\"accuracy\":0.8,\"slot\":\"SECOND\"},"
		+ "{\"id\":\"d\",\"name\":\"D\",\"style\":\"MELEE\",\"power\":1.8,\"accuracy\":0.7},"
		+ "{\"id\":\"e\",\"name\":\"E\",\"style\":\"MELEE\",\"power\":2.0,\"accuracy\":0.6},"
		+ "{\"id\":\"f\",\"name\":\"F\",\"style\":\"MAGIC\",\"power\":1.0,\"accuracy\":1.0}],"
		+ "\"defaults\":{\"MELEE\":\"plain\",\"MAGIC\":\"spell\"},"
		+ "\"pools\":{\"plain\":{\"styles\":[\"MELEE\"],"
		+ "\"learns\":[[\"a\",1],[\"b\",5],[\"c\",10],[\"d\",20],[\"e\",30]]},"
		+ "\"spell\":{\"styles\":[\"MAGIC\"],\"learns\":[[\"f\",1]]}},"
		+ "\"species\":{\"special\":\"spell\"}}";

	private static final Species GOBLIN = new Species("Goblin", 5, CombatType.MELEE);
	private static final Species WIZARD = new Species("Wizard", 5, CombatType.MAGIC);

	private final MoveTable table = MoveTable.load(new Gson(), new StringReader(JSON));

	private static String ids(List<Move> moves)
	{
		StringBuilder text = new StringBuilder();
		for (Move move : moves)
			text.append(move.id());

		return text.toString();
	}

	@Test
	public void aCreatureKnowsTheNewestFourMovesItsLevelHasUnlocked()
	{
		assertEquals("a", ids(table.movesAt(GOBLIN, 1)));
		assertEquals("ab", ids(table.movesAt(GOBLIN, 7)));
		assertEquals("abcd", ids(table.movesAt(GOBLIN, 20)));
		assertEquals("bcde", ids(table.movesAt(GOBLIN, 99)));
	}

	@Test
	public void aJumpOverLevelsUnlocksEveryMoveInBetween()
	{
		assertEquals("bc", ids(table.unlockedBetween(GOBLIN, 4, 10)));
		assertEquals("", ids(table.unlockedBetween(GOBLIN, 5, 9)));
		assertEquals("abcde", ids(table.unlockedBetween(GOBLIN, 0, 99)));
		assertEquals("a move at the level you were at is not new", "d", ids(table.unlockedBetween(GOBLIN, 10, 20)));
	}

	@Test
	public void aMoveIsKnownOnlyFromItsPoolAndAtItsLevel()
	{
		assertTrue(table.knows(GOBLIN, 10, "c"));
		assertFalse("too early", table.knows(GOBLIN, 9, "c"));
		assertFalse("not in the pool", table.knows(GOBLIN, 99, "f"));
		assertFalse("unknown", table.knows(GOBLIN, 99, "zzz"));
		assertTrue(table.knows(WIZARD, 1, "f"));
	}

	@Test
	public void aSpeciesOfTheTableWithAnOwnPoolUsesIt()
	{
		Species special = new Species("Special", 5, CombatType.MAGIC);
		assertEquals("spell", table.poolOf(special));
		assertEquals("f", ids(table.movesAt(special, 50)));
	}

	@Test
	public void anEmptyOrBrokenTableKnowsNothing()
	{
		assertEquals("", ids(MoveTable.empty().movesAt(GOBLIN, 50)));
		assertEquals(0, MoveTable.load(new Gson(), new StringReader("{broken")).size());
		assertEquals(0, MoveTable.load(new Gson(), new StringReader("")).size());
		assertNull(MoveTable.empty().poolOf(GOBLIN));
		assertFalse(MoveTable.empty().knows(GOBLIN, 99, "a"));
	}

	@Test
	public void aBreathPlaysTheSecondAnimationAndTheBasicAttackReadsAsBefore()
	{
		assertTrue(table.find("c").second());
		assertFalse(table.find("a").second());
		assertTrue(Move.basic(CombatType.MELEE).plain());
		assertEquals(1.0, Move.basic(CombatType.RANGED).power(), 0.0);
		assertEquals(CombatType.RANGED, Move.basic(CombatType.RANGED).style());
		assertEquals("Magic, power 1.5, accuracy 80%", table.find("c").describe());
	}
}
