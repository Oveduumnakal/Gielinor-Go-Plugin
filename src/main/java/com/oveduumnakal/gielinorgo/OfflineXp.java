/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Decides what XP gained while the plugin was not watching (on mobile, in another client, or with the plugin off) is
 * worth. It compares the XP the save remembers with the XP the game reports at login. Pure rules: no clock, no
 * network, a seeded {@link Random} for the jar and item rolls.
 */
final class OfflineXp
{
	/** Most OSRS XP credited for one skill in one login. */
	static final int MAX_SKILL_XP = 1_000_000;

	/** Most OSRS XP credited across all skills in one login. */
	static final int MAX_TOTAL_XP = 3_000_000;

	/** World types that keep their own XP, so their numbers are not the account's. */
	private static final String[] OWN_XP_WORLDS = {"SEASONAL", "DEADMAN", "TOURNAMENT_WORLD", "FRESH_START_WORLD",
		"QUEST_SPEEDRUNNING", "NOSAVE_MODE", "BETA_WORLD"};

	private static final String OVERALL = "OVERALL";

	private OfflineXp()
	{
	}

	/**
	 * Works out what the XP gained while away is worth.
	 *
	 * @param snapshot the OSRS XP of each skill (by {@code Skill} name) when the plugin last saw it
	 * @param login the OSRS XP of each skill as the game reports it now
	 * @param random the random source for the fractional jars and items
	 * @return what is credited, empty when nothing is
	 */
	static Result compute(Map<String, Integer> snapshot, Map<String, Integer> login, Random random)
	{
		Result result = new Result();
		for (Map.Entry<String, Integer> entry : new TreeMap<>(login).entrySet())
		{
			String skill = entry.getKey();
			Integer before = snapshot.get(skill);
			Integer now = entry.getValue();
			if (OVERALL.equals(skill) || before == null || now == null || now <= before)
				continue;

			long raw = (long) now - before;
			int gain = (int) Math.min(raw, Math.min(MAX_SKILL_XP, MAX_TOTAL_XP - result.totalXp));
			if (gain < raw)
				result.capped = true;

			if (gain <= 0)
				continue;

			result.totalXp += gain;
			result.xpBySkill.put(skill, gain);
			result.levelUps += OsrsLevels.levelForXp(before + gain) - OsrsLevels.levelForXp(before);
			JarType jar = JarType.forSkill(skill);
			if (jar != null)
				add(result.jars, jar, count(gain, CatchRules.XP_PER_SPECIALTY_JAR, random));

			for (ItemType item : ItemType.forSkill(skill))
				add(result.items, item, count(gain, ItemRules.XP_PER_ITEM, random));
		}

		return result;
	}

	/**
	 * Tells whether XP gained on a world counts as the account's own.
	 *
	 * @param worldTypeNames the {@code WorldType} names of the current world
	 * @return false on seasonal, deadman, tournament and similar worlds that keep their own XP
	 */
	static boolean countsWorld(Collection<String> worldTypeNames)
	{
		for (String other : OWN_XP_WORLDS)
		{
			if (worldTypeNames.contains(other))
				return false;
		}

		return true;
	}

	private static int count(int gain, double perOne, Random random)
	{
		int whole = (int) (gain / perOne);
		double rest = gain / perOne - whole;
		return rest > 0 && random.nextDouble() < rest ? whole + 1 : whole;
	}

	private static <K> void add(Map<K, Integer> into, K key, int count)
	{
		if (count > 0)
			into.merge(key, count, Integer::sum);
	}

	/** What a login's offline XP is worth. */
	static final class Result
	{
		private final Map<String, Integer> xpBySkill = new TreeMap<>();
		private final Map<JarType, Integer> jars = new EnumMap<>(JarType.class);
		private final Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
		private int totalXp;
		private int levelUps;
		private boolean capped;

		/** @return the OSRS XP credited across all skills */
		int totalXp()
		{
			return totalXp;
		}

		/** @return the credited OSRS XP by skill, in name order */
		Map<String, Integer> xpBySkill()
		{
			return Collections.unmodifiableMap(xpBySkill);
		}

		/** @return OSRS levels gained across all skills */
		int levelUps()
		{
			return levelUps;
		}

		/** @return the specialty jars found, by type */
		Map<JarType, Integer> jars()
		{
			return Collections.unmodifiableMap(jars);
		}

		/** @return the healing items found, by type */
		Map<ItemType, Integer> items()
		{
			return Collections.unmodifiableMap(items);
		}

		/** @return whether a cap cut some of the gain */
		boolean capped()
		{
			return capped;
		}

		/** @return whether nothing at all is credited */
		boolean isEmpty()
		{
			return totalXp == 0;
		}
	}
}
