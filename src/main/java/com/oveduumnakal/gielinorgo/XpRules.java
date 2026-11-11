/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;

/** Level curves and XP sharing for creatures and for the trainer. Pure maths, no game state. */
final class XpRules
{
	/** Highest level a creature or the trainer can reach. */
	static final int MAX_LEVEL = 99;

	/** OSRS XP the player must gain for the team to receive 1 creature XP. */
	static final int OSRS_XP_PER_CREATURE_XP = 10;

	private static final long CREATURE_SCALE = 50L;
	private static final long TRAINER_SCALE = 10L;

	private XpRules()
	{
	}

	/**
	 * Total creature XP needed to be a level.
	 *
	 * @param level the level, clamped to 1 to 99
	 * @return the XP at which that level starts
	 */
	static long xpForLevel(int level)
	{
		return curve(level, CREATURE_SCALE);
	}

	/**
	 * Creature level for a total XP.
	 *
	 * @param xp total creature XP
	 * @return the level, 1 to 99
	 */
	static int levelForXp(long xp)
	{
		return inverse(xp, CREATURE_SCALE);
	}

	/**
	 * Total trainer XP needed to be a trainer level.
	 *
	 * @param level the level, clamped to 1 to 99
	 * @return the XP at which that level starts
	 */
	static long trainerXpForLevel(int level)
	{
		return curve(level, TRAINER_SCALE);
	}

	/**
	 * Trainer level for a total trainer XP.
	 *
	 * @param xp total trainer XP
	 * @return the level, 1 to 99
	 */
	static int trainerLevelForXp(long xp)
	{
		return inverse(xp, TRAINER_SCALE);
	}

	/**
	 * How far a creature is through its current level.
	 *
	 * @param xp total creature XP
	 * @return progress to the next level from 0 to 1, and 1 at the level cap
	 */
	static double progress(long xp)
	{
		return progress(xp, CREATURE_SCALE);
	}

	/**
	 * How far the trainer is through the current trainer level.
	 *
	 * @param xp total trainer XP
	 * @return progress to the next level from 0 to 1, and 1 at the level cap
	 */
	static double trainerProgress(long xp)
	{
		return progress(xp, TRAINER_SCALE);
	}

	/**
	 * Shares creature XP across a team. The lead counts twice, everyone else once, and what does not divide evenly
	 * goes to the lead.
	 *
	 * @param team the team, lead first
	 * @param amount creature XP to hand out
	 */
	static void share(List<Creature> team, long amount)
	{
		if (team.isEmpty() || amount <= 0)
			return;

		long shares = team.size() + 1L;
		long each = amount / shares;
		long leftover = amount - each * shares;
		for (Creature creature : team)
			creature.xp = capped(creature.xp + each);

		Creature lead = team.get(0);
		lead.xp = capped(lead.xp + each + leftover);
	}

	/**
	 * Keeps a creature's XP from running past the level cap.
	 *
	 * @param xp total creature XP
	 * @return the XP, limited to what level 99 needs
	 */
	static long capped(long xp)
	{
		return Math.max(0, Math.min(xp, xpForLevel(MAX_LEVEL)));
	}

	private static long curve(int level, long scale)
	{
		long steps = Math.max(1, Math.min(MAX_LEVEL, level)) - 1L;
		return scale * steps * steps;
	}

	private static int inverse(long xp, long scale)
	{
		if (xp <= 0)
			return 1;

		int level = 1 + (int) Math.floor(Math.sqrt(xp / (double) scale));
		while (level < MAX_LEVEL && curve(level + 1, scale) <= xp)
			level++;

		while (level > 1 && curve(level, scale) > xp)
			level--;

		return Math.min(MAX_LEVEL, level);
	}

	private static double progress(long xp, long scale)
	{
		int level = inverse(xp, scale);
		if (level >= MAX_LEVEL)
			return 1.0;

		long start = curve(level, scale);
		long end = curve(level + 1, scale);
		return Math.max(0.0, Math.min(1.0, (xp - start) / (double) (end - start)));
	}
}
