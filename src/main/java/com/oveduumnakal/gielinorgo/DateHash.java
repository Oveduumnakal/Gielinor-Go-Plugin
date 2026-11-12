/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * A fixed mix of a day number and a small salt into one 64-bit value. It is plain integer arithmetic written out
 * here, with no {@code String.hashCode}, no {@code Objects.hash} and no {@code Random} seeded from text, so the
 * same day and salt give the same answer on every Java version and every computer. Anything that must be the same
 * for every player on a given day (the featured species, the day's tasks) can pick from it.
 */
final class DateHash
{
	private static final long DAY_MULTIPLIER = 0x9E3779B97F4A7C15L;
	private static final long SALT_MULTIPLIER = 0xC2B2AE3D27D4EB4FL;
	private static final long MIX_ONE = 0xBF58476D1CE4E5B9L;
	private static final long MIX_TWO = 0x94D049BB133111EBL;

	private DateHash()
	{
	}

	/**
	 * Mixes a day and a salt.
	 *
	 * @param epochDay days since 1970-01-01 UTC
	 * @param salt a small number that tells apart different questions asked about the same day
	 * @return a well-spread 64-bit value
	 */
	static long mix(long epochDay, int salt)
	{
		long x = epochDay * DAY_MULTIPLIER + (salt + 1L) * SALT_MULTIPLIER;
		x ^= x >>> 30;
		x *= MIX_ONE;
		x ^= x >>> 27;
		x *= MIX_TWO;
		x ^= x >>> 31;
		return x;
	}

	/**
	 * Picks a position from a day and a salt.
	 *
	 * @param epochDay days since 1970-01-01 UTC
	 * @param salt a small number that tells apart different questions asked about the same day
	 * @param count how many positions there are, at least 1
	 * @return a position from 0 up to but not including count
	 */
	static int pick(long epochDay, int salt, int count)
	{
		return (int) Math.floorMod(mix(epochDay, salt), (long) Math.max(1, count));
	}
}
