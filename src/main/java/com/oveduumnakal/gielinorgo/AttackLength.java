/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * How long one beat of an acted-out battle lasts for the attack in it: the normal {@link BattleTimeline#BEAT_TICKS},
 * or longer when the attacker's animation or the target's hit reaction runs past it, so the next attack never starts
 * while the last one still plays. A projectile needs no term of its own: it always lands on
 * {@link TurnMotion#IMPACT_TICK}, inside the normal beat. Holds no RuneLite types: the animation lengths are passed in.
 */
final class AttackLength
{
	/** Most game ticks one beat may last, so a odd animation cannot stall a battle: about five seconds. */
	static final int MAX_BEAT_TICKS = 8;

	private AttackLength()
	{
	}

	/**
	 * Adds up the frame lengths of an animation.
	 *
	 * @param frameLengths the length of each frame in client ticks (20 ms each), or null
	 * @return the animation's length in client ticks, 0 for none
	 */
	static int clientTicks(int[] frameLengths)
	{
		int total = 0;
		if (frameLengths == null)
			return total;

		for (int length : frameLengths)
		{
			if (length > 0)
				total += length;
		}

		return total;
	}

	/**
	 * Works out how many game ticks a beat with an attack lasts.
	 *
	 * @param melee whether the attacker steps up first, so its animation starts at {@link TurnMotion#STRIKE_TICK}
	 *     rather than at {@link TurnMotion#SHOT_TICK}
	 * @param attackTicks the attack animation's length in client ticks, 0 when it has none
	 * @param reactTicks the target's hit reaction in client ticks, 0 when it has none
	 * @param reacts whether the target reacts: false for a miss
	 * @return the longer of {@link BattleTimeline#BEAT_TICKS} and the time the attack and the reaction take, never
	 *     more than {@link #MAX_BEAT_TICKS}
	 */
	static int beatTicks(boolean melee, int attackTicks, int reactTicks, boolean reacts)
	{
		int start = melee ? TurnMotion.STRIKE_TICK : TurnMotion.SHOT_TICK;
		int end = start + attackTicks;
		if (reacts)
			end = Math.max(end, TurnMotion.IMPACT_TICK + reactTicks);

		return gameTicks(end);
	}

	/**
	 * Works out how many game ticks a beat lasts in which a creature only flinches, from poison or a lost turn.
	 *
	 * @param reactTicks the hit reaction's length in client ticks, 0 when it has none
	 * @return the longer of {@link BattleTimeline#BEAT_TICKS} and the reaction, never more than {@link #MAX_BEAT_TICKS}
	 */
	static int flinchTicks(int reactTicks)
	{
		return gameTicks(reactTicks);
	}

	private static int gameTicks(int clientTicks)
	{
		int perTick = TurnMotion.CLIENT_TICKS_PER_GAME_TICK;
		int needed = (clientTicks + perTick - 1) / perTick;
		return Math.min(MAX_BEAT_TICKS, Math.max(BattleTimeline.BEAT_TICKS, needed));
	}
}
