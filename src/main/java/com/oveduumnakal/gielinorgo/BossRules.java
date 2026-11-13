/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rules for boss eggs: which chat lines count as a kill and how often a kill drops an egg. Pure rules with no
 * RuneLite types. Nothing about loot is read: only the game's kill-count line.
 */
final class BossRules
{
	/** The chance that a kill after the first one drops the boss's egg: 1 in 20. */
	static final double LATER_CHANCE = 0.05;

	private static final Pattern KILL = Pattern.compile("^Your (.+?) kill count is: ([0-9,]+)\\.?$");
	private static final Pattern RAID = Pattern.compile("^Your completed (.+?) count is: ([0-9,]+)\\.?$");

	private BossRules()
	{
	}

	/**
	 * Reads a game message that tells a kill count.
	 *
	 * @param message the message text, with or without colour tags
	 * @return the boss or raid name as the game wrote it, or null when the line is not a kill-count line
	 */
	static String killedName(String message)
	{
		if (message == null)
			return null;

		String plain = message.replaceAll("<[^>]*>", "")
			.replace('\u00a0', ' ')
			.trim();
		Matcher kill = KILL.matcher(plain);
		if (kill.matches())
			return kill.group(1).trim();

		Matcher raid = RAID.matcher(plain);
		if (raid.matches())
			return raid.group(1).trim();

		return null;
	}

	/**
	 * Rolls whether a kill drops an egg.
	 *
	 * @param firstKill true when the boss has not paid its sure egg yet
	 * @param random the source of the roll
	 * @return true for an egg
	 */
	static boolean eggDrops(boolean firstKill, Random random)
	{
		return firstKill || random.nextDouble() < LATER_CHANCE;
	}
}
