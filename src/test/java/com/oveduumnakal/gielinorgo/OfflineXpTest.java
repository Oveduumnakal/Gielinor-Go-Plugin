/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks what XP gained while the plugin was away is credited. */
public class OfflineXpTest
{
	private static Map<String, Integer> xp(Object... pairs)
	{
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
			map.put((String) pairs[i], (Integer) pairs[i + 1]);

		return map;
	}

	private static OfflineXp.Result compute(Map<String, Integer> snapshot, Map<String, Integer> login)
	{
		return OfflineXp.compute(snapshot, login, new Random(5));
	}

	@Test
	public void noSnapshotMeansNoCredit()
	{
		OfflineXp.Result result = compute(new HashMap<>(), xp("MINING", 50_000, "ATTACK", 9_000));

		assertTrue(result.isEmpty());
		assertEquals(0, result.totalXp());
	}

	@Test
	public void aSkillWithNoSnapshotEntryIsBaselinedNotCredited()
	{
		OfflineXp.Result result = compute(xp("MINING", 1_000), xp("MINING", 1_500, "ATTACK", 90_000));

		assertEquals(500, result.totalXp());
		assertEquals(Integer.valueOf(500), result.xpBySkill().get("MINING"));
		assertNull(result.xpBySkill().get("ATTACK"));
	}

	@Test
	public void theGainIsLoginMinusSnapshot()
	{
		OfflineXp.Result result = compute(xp("MINING", 1_000, "FISHING", 200), xp("MINING", 4_000, "FISHING", 700));

		assertEquals(3_500, result.totalXp());
		assertEquals(Integer.valueOf(3_000), result.xpBySkill().get("MINING"));
		assertEquals(Integer.valueOf(500), result.xpBySkill().get("FISHING"));
		assertFalse(result.isEmpty());
	}

	@Test
	public void xpThatDidNotGrowIsIgnored()
	{
		OfflineXp.Result result = compute(xp("MINING", 5_000, "FISHING", 700), xp("MINING", 4_000, "FISHING", 700));

		assertTrue(result.isEmpty());
		assertTrue(result.xpBySkill().isEmpty());
	}

	@Test
	public void overallIsNeverCounted()
	{
		OfflineXp.Result result = compute(xp("OVERALL", 100, "MINING", 100), xp("OVERALL", 9_999, "MINING", 150));

		assertEquals(50, result.totalXp());
		assertNull(result.xpBySkill().get("OVERALL"));
	}

	@Test
	public void eachSkillIsCapped()
	{
		OfflineXp.Result result = compute(xp("MINING", 100), xp("MINING", 100 + 5_000_000));

		assertEquals(OfflineXp.MAX_SKILL_XP, result.totalXp());
		assertTrue(result.capped());
	}

	@Test
	public void theTotalIsCappedAndTheExcessIsDropped()
	{
		Map<String, Integer> snapshot = xp("AGILITY", 0, "ATTACK", 0, "COOKING", 0, "FISHING", 0, "MINING", 0);
		Map<String, Integer> login = xp("AGILITY", 900_000, "ATTACK", 900_000, "COOKING", 900_000, "FISHING", 900_000,
			"MINING", 900_000);

		OfflineXp.Result result = compute(snapshot, login);

		assertEquals(OfflineXp.MAX_TOTAL_XP, result.totalXp());
		assertTrue(result.capped());
		assertEquals(Integer.valueOf(900_000), result.xpBySkill().get("AGILITY"));
		assertEquals(Integer.valueOf(900_000), result.xpBySkill().get("COOKING"));
		assertEquals(Integer.valueOf(300_000), result.xpBySkill().get("FISHING"));
		assertNull("the skill after the cap got nothing", result.xpBySkill().get("MINING"));
	}

	@Test
	public void aGainUnderTheCapsIsNotMarkedCapped()
	{
		assertFalse(compute(xp("MINING", 100), xp("MINING", 200_000)).capped());
	}

	@Test
	public void levelUpsComeFromTheXpDifference()
	{
		OfflineXp.Result result = compute(xp("MINING", 82, "FISHING", 1_000), xp("MINING", 1_154, "FISHING", 1_100));

		assertEquals("mining 1 to 10 is nine levels, fishing stays level 9", 9, result.levelUps());
	}

	@Test
	public void jarsFollowTheSkillsJarType()
	{
		OfflineXp.Result result = compute(xp("MINING", 0, "ATTACK", 0, "SMITHING", 0, "SAILING", 0, "OVERALL", 0),
			xp("MINING", 40_000, "ATTACK", 60_000, "SAILING", 40_000, "SMITHING", 0, "OVERALL", 500_000));

		assertEquals(Integer.valueOf(2), result.jars().get(JarType.CAVE));
		assertEquals(Integer.valueOf(3), result.jars().get(JarType.STAB));
		assertEquals(Integer.valueOf(2), result.jars().get(JarType.TIDE));
		assertNull(result.jars().get(JarType.EMBER));
		assertNull("no jar is paid for XP alone as a Bronze jar", result.jars().get(JarType.BRONZE));
	}

	@Test
	public void aSkillWithNoJarPaysNone()
	{
		OfflineXp.Result result = compute(xp("HITPOINTS", 0), xp("HITPOINTS", 500_000));

		assertTrue(result.jars().isEmpty());
		assertEquals(500_000, result.totalXp());
	}

	@Test
	public void wholeJarsAreCertainAndTheRemainderIsASeededRoll()
	{
		Map<String, Integer> snapshot = xp("MINING", 0);
		int gain = 50_000;
		int certain = 2;
		int rolled = 0;
		Random seeds = new Random(1);
		for (int trial = 0; trial < 400; trial++)
		{
			OfflineXp.Result result = OfflineXp.compute(snapshot, xp("MINING", gain), new Random(seeds.nextLong()));
			int jars = result.jars().get(JarType.CAVE);
			assertTrue(jars == certain || jars == certain + 1);
			rolled += jars - certain;
		}

		assertTrue("half a jar of remainder pays about half the time: " + rolled, rolled > 140 && rolled < 260);
	}

	@Test
	public void itemsFollowItemTypeForSkill()
	{
		OfflineXp.Result result = compute(xp("HERBLORE", 0, "COOKING", 0, "MINING", 0),
			xp("HERBLORE", 60_000, "COOKING", 30_000, "MINING", 90_000));

		assertEquals(Integer.valueOf(3), result.items().get(ItemType.SMALL_SALVE));
		assertEquals(Integer.valueOf(2), result.items().get(ItemType.ANTIDOTE));
		assertNull(result.items().get(ItemType.REVIVE));
	}

	@Test
	public void sameSeedSameResult()
	{
		Map<String, Integer> snapshot = xp("MINING", 0, "HERBLORE", 0, "FIREMAKING", 0);
		Map<String, Integer> login = xp("MINING", 33_333, "HERBLORE", 17_000, "FIREMAKING", 9_000);

		OfflineXp.Result first = OfflineXp.compute(snapshot, login, new Random(77));
		OfflineXp.Result second = OfflineXp.compute(snapshot, login, new Random(77));

		assertEquals(first.jars(), second.jars());
		assertEquals(first.items(), second.items());
		assertEquals(first.xpBySkill(), second.xpBySkill());
	}

	@Test
	public void worldsWithTheirOwnXpAreNotCounted()
	{
		assertTrue(OfflineXp.countsWorld(Collections.<String>emptyList()));
		assertTrue(OfflineXp.countsWorld(Arrays.asList("MEMBERS", "PVP")));
		for (String other : Arrays.asList("SEASONAL", "DEADMAN", "TOURNAMENT_WORLD", "FRESH_START_WORLD",
			"QUEST_SPEEDRUNNING", "NOSAVE_MODE", "BETA_WORLD"))
			assertFalse(other, OfflineXp.countsWorld(Arrays.asList("MEMBERS", other)));
	}
}
