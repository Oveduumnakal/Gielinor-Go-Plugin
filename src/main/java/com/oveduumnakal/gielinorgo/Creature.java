/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;

/**
 * One creature the player owns. Saved to the profile as JSON, so every field here is part of the persisted schema.
 */
final class Creature
{
	/** How long a creature takes to go from no health back to full, in milliseconds. */
	static final long FULL_REST_MS = 5 * 60 * 1000L;

	/** Species key, see {@link Species#keyFor(String)}. */
	String species;

	/** Total creature XP. The level is derived from it by {@link XpRules#levelForXp(long)}. */
	long xp;

	/** Share of full health the creature had at {@link #hpStampMs}, from 0 to 1. */
	double hpFraction = 1.0;

	/** Wall-clock time {@link #hpFraction} was recorded, in milliseconds. */
	long hpStampMs;

	/** Wall-clock time the creature was caught, in milliseconds. */
	long caughtAtMs;

	/** Whether this is a rare gilded creature. Only its looks differ. */
	boolean gilded;

	/** The name the player gave this creature, or null for none. Local only: it is never sent to other players. */
	String nickname;

	/** Health roll, {@value StatRolls#MIN} to {@value StatRolls#MAX}. 0 is average, and what an older save loads as. */
	int rollHp;

	/** Attack roll, {@value StatRolls#MIN} to {@value StatRolls#MAX}. 0 is average. */
	int rollAttack;

	/** Defence roll, {@value StatRolls#MIN} to {@value StatRolls#MAX}. 0 is average. */
	int rollDefence;

	/** Speed roll, {@value StatRolls#MIN} to {@value StatRolls#MAX}. 0 is average. */
	int rollSpeed;

	/** A {@link Temperament#name()}, or null for the steady temperament, which changes nothing. */
	String temperament;

	/**
	 * The id of the NPC this creature was caught as, so it looks like the variant that was caught. 0 means unknown:
	 * an older save, or a creature that was hatched, evolved or received in a trade. It is shown as the species' first
	 * NPC id then, see {@link Species#npcIdFor(int)}.
	 */
	int npcId;

	/**
	 * The ids of the moves the creature knows, at most {@value MoveTable#MAX_KNOWN}, in the order learned. Null in an
	 * older save: {@link GoGame#load(SaveData)} then gives it the newest moves its level has unlocked.
	 */
	List<String> moves;

	/**
	 * The ids of moves the creature could learn but has no room for, oldest first. Each waits until the player picks a
	 * move to forget or skips it. Null or empty when nothing waits.
	 */
	List<String> pendingMoves;

	/**
	 * Bond points earned while this creature was the lead, see {@link BondRules}. 0 is no bond, and what an older save
	 * loads as. Local only: it is never sent to other players, so a traded creature starts again at 0.
	 */
	int bond;

	/** Used by Gson. */
	Creature()
	{
	}

	Creature(String species, int level, long nowMs)
	{
		this.species = species;
		this.xp = XpRules.xpForLevel(level);
		this.caughtAtMs = nowMs;
		this.hpStampMs = nowMs;
	}

	/** @return the creature's rolls and temperament; a creature that never rolled gets {@link StatRolls#AVERAGE} */
	StatRolls rolls()
	{
		Temperament parsed = Temperament.named(temperament);
		return new StatRolls(rollHp, rollAttack, rollDefence, rollSpeed, parsed == null ? Temperament.STEADY : parsed);
	}

	/**
	 * Gives the creature its rolls and temperament.
	 *
	 * @param rolls the rolls; steady is stored as null
	 */
	void setRolls(StatRolls rolls)
	{
		int[] values = rolls.values();
		rollHp = values[0];
		rollAttack = values[1];
		rollDefence = values[2];
		rollSpeed = values[3];
		temperament = rolls.temperament() == Temperament.STEADY ? null : rolls.temperament().name();
	}

	/** @return the creature's level, 1 to 99 */
	int level()
	{
		return XpRules.levelForXp(xp);
	}

	/**
	 * Health right now, counting the rest since the last battle.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 * @return the share of full health, from 0 to 1
	 */
	double hpFractionAt(long nowMs)
	{
		double rested = Math.max(0, nowMs - hpStampMs) / (double) FULL_REST_MS;
		return Math.max(0.0, Math.min(1.0, hpFraction + rested));
	}
}
