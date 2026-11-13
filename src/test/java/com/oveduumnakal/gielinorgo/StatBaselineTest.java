/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks that skills start with a baseline, so the first experience drop is counted. */
public class StatBaselineTest
{
	@Test
	public void everySkillGetsItsCurrentExperienceAndLevel()
	{
		Map<String, Integer> xp = new HashMap<>();
		Map<String, Integer> levels = new HashMap<>();

		StatBaseline.seed(Arrays.asList("fishing", "mining"), xp, levels, skill -> skill.length() * 100,
			skill -> skill.length());

		assertEquals(Integer.valueOf(700), xp.get("fishing"));
		assertEquals(Integer.valueOf(600), xp.get("mining"));
		assertEquals(Integer.valueOf(7), levels.get("fishing"));
	}

	@Test
	public void aBaselineAlreadyThereIsKept()
	{
		Map<String, Integer> xp = new HashMap<>();
		Map<String, Integer> levels = new HashMap<>();
		xp.put("fishing", 50);
		levels.put("fishing", 2);

		StatBaseline.seed(Arrays.asList("fishing"), xp, levels, skill -> 999, skill -> 99);

		assertEquals(Integer.valueOf(50), xp.get("fishing"));
		assertEquals(Integer.valueOf(2), levels.get("fishing"));
	}
}
