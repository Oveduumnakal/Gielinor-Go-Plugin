/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Seeds the experience and level a skill had before the plugin started watching it, so the first experience drop
 * after the plugin is turned on counts. Without it that drop only sets the baseline and nothing is trained.
 */
final class StatBaseline
{
	private StatBaseline()
	{
	}

	/**
	 * Records the current experience and level of every skill that has no baseline yet.
	 *
	 * @param skills the skills to seed
	 * @param xp the baseline experience, by skill
	 * @param levels the baseline level, by skill
	 * @param xpOf reads a skill's current experience
	 * @param levelOf reads a skill's current level
	 * @param <S> the skill type
	 */
	static <S> void seed(Iterable<S> skills, Map<S, Integer> xp, Map<S, Integer> levels, ToIntFunction<S> xpOf,
		ToIntFunction<S> levelOf)
	{
		for (S skill : skills)
		{
			xp.putIfAbsent(skill, xpOf.applyAsInt(skill));
			levels.putIfAbsent(skill, levelOf.applyAsInt(skill));
		}
	}
}
