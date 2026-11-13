/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * A moment that plays a short game sound. The sound ids are the game's own sound effects (the values of
 * RuneLite's {@code SoundEffectID}); the owner picks them by ear, so they live in this one file. A higher priority
 * wins when several cues come from one event.
 */
enum Cue
{
	/** A creature was caught: the Grand Exchange offer "ding-a-ling" (3925). */
	CATCH(3925, 3),
	/** A jar failed and the creature broke free or ran: the magic splash "boing" (227). */
	BROKE_FREE(227, 1),
	/** A creature gained a level: the Grand Exchange collect "bloop" (3928). */
	LEVEL_UP(3928, 2),
	/** A creature evolved: the teleport "vwoop" (200). */
	EVOLVE(200, 5),
	/** An egg hatched: the pick-a-plant "bloop" (2581). */
	HATCH(2581, 4),
	/** A gilded creature appeared: the town crier's bell "ding" (3813). */
	GILDED(3813, 7),
	/** A City Champion's sigil was earned: the town crier's bell "dong" (3817). */
	SIGIL(3817, 6),
	/** A Creature Log milestone was paid: the Grand Exchange coin "tinkle" (3924). */
	MILESTONE(3924, 4);

	private final int soundId;
	private final int priority;

	Cue(int soundId, int priority)
	{
		this.soundId = soundId;
		this.priority = priority;
	}

	/** @return the game sound effect id to play */
	int soundId()
	{
		return soundId;
	}

	/** @return how important this cue is next to others from the same event; higher wins */
	int priority()
	{
		return priority;
	}

	/**
	 * Picks the more important of two cues.
	 *
	 * @param first a cue, or null
	 * @param second a cue, or null
	 * @return the one with the higher priority, the other when one is null, null when both are
	 */
	static Cue stronger(Cue first, Cue second)
	{
		if (first == null)
			return second;

		if (second == null)
			return first;

		return second.priority > first.priority ? second : first;
	}
}
