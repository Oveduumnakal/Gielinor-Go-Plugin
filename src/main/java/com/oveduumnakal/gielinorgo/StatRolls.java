/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * The hidden make-up of one creature: a roll from {@link #MIN} to {@link #MAX} for health, attack, defence and
 * speed, and a {@link Temperament}. Zero is average, so a creature with no rolls and the steady temperament, which
 * is what every creature saved before rolls existed has, keeps exactly the strength it always had. Immutable.
 */
final class StatRolls
{
	/** Lowest roll for one stat. */
	static final int MIN = -5;

	/** Highest roll for one stat. */
	static final int MAX = 5;

	/** Share of a stat one roll point is worth. */
	static final double PER_POINT = 0.015;

	/** How many stats are rolled. */
	static final int COUNT = 4;

	/** The make-up of a creature with no rolls: every stat exactly as the species and level give it. */
	static final StatRolls AVERAGE = new StatRolls(0, 0, 0, 0, Temperament.STEADY);

	/** The one-word summary of how good a creature's rolls are. */
	enum Rating
	{
		WEAK("Weak"),
		AVERAGE("Average"),
		STRONG("Strong"),
		EXCEPTIONAL("Exceptional");

		private final String label;

		Rating(String label)
		{
			this.label = label;
		}

		/** @return the word shown to the player */
		String label()
		{
			return label;
		}
	}

	private final int hp;
	private final int attack;
	private final int defence;
	private final int speed;
	private final Temperament temperament;

	/**
	 * Makes a set of rolls. The values are not clamped, so {@link #valid()} can tell a made-up one.
	 *
	 * @param hp the health roll
	 * @param attack the attack roll
	 * @param defence the defence roll
	 * @param speed the speed roll
	 * @param temperament the temperament, never null
	 */
	StatRolls(int hp, int attack, int defence, int speed, Temperament temperament)
	{
		this.hp = hp;
		this.attack = attack;
		this.defence = defence;
		this.speed = speed;
		this.temperament = temperament;
	}

	/**
	 * Rolls a new creature's make-up: four rolls, then a temperament.
	 *
	 * @param random the game's random source
	 * @return the rolls
	 */
	static StatRolls roll(Random random)
	{
		int span = MAX - MIN + 1;
		int hp = MIN + random.nextInt(span);
		int attack = MIN + random.nextInt(span);
		int defence = MIN + random.nextInt(span);
		int speed = MIN + random.nextInt(span);
		Temperament[] all = Temperament.values();
		return new StatRolls(hp, attack, defence, speed, all[random.nextInt(all.length)]);
	}

	/**
	 * Reads rolls that came from a save or a message. A missing array or temperament means average.
	 *
	 * @param values health, attack, defence and speed rolls, or null for all zero
	 * @param temperament a {@link Temperament#name()}, or null for steady
	 * @return the rolls, or null when the array is the wrong size, a roll is out of range or the temperament is
	 *     unknown
	 */
	static StatRolls of(int[] values, String temperament)
	{
		Temperament parsed = Temperament.named(temperament);
		if (parsed == null || (values != null && values.length != COUNT))
			return null;

		StatRolls rolls = values == null ? new StatRolls(0, 0, 0, 0, parsed)
			: new StatRolls(values[0], values[1], values[2], values[3], parsed);
		return rolls.valid() ? rolls : null;
	}

	/**
	 * Pulls a roll into range.
	 *
	 * @param roll any number
	 * @return the number, at least {@link #MIN} and at most {@link #MAX}
	 */
	static int clamp(int roll)
	{
		return Math.max(MIN, Math.min(MAX, roll));
	}

	/** @return whether every roll is inside {@link #MIN} to {@link #MAX} */
	boolean valid()
	{
		return inRange(hp) && inRange(attack) && inRange(defence) && inRange(speed);
	}

	/** @return the four rolls in the order health, attack, defence, speed, as a new array */
	int[] values()
	{
		return new int[] {hp, attack, defence, speed};
	}

	/** @return the temperament */
	Temperament temperament()
	{
		return temperament;
	}

	/**
	 * Gives the roll of one stat.
	 *
	 * @param stat the stat
	 * @return its roll
	 */
	int roll(Stat stat)
	{
		switch (stat)
		{
			case HEALTH:
				return hp;

			case ATTACK:
				return attack;

			case DEFENCE:
				return defence;

			default:
				return speed;
		}
	}

	/** @return the four rolls added up */
	int total()
	{
		return hp + attack + defence + speed;
	}

	/** @return the one-word summary: weak at -8 or less, strong at 8 or more, exceptional at 14 or more */
	Rating rating()
	{
		int total = total();
		if (total <= -8)
			return Rating.WEAK;

		if (total >= 14)
			return Rating.EXCEPTIONAL;

		return total >= 8 ? Rating.STRONG : Rating.AVERAGE;
	}

	/**
	 * Tells how much these rolls and the temperament scale a stat.
	 *
	 * @param stat the stat
	 * @return {@code (1 + 0.015 * roll) * temperament factor}; exactly 1.0 for {@link #AVERAGE}
	 */
	double factor(Stat stat)
	{
		return (1.0 + PER_POINT * roll(stat)) * temperament.factor(stat);
	}

	/**
	 * Writes the rolls as the panel shows them.
	 *
	 * @return for example {@code Health +3 · Attack -1 · Defence 0 · Speed +5}
	 */
	String describe()
	{
		StringBuilder text = new StringBuilder();
		for (Stat stat : Stat.values())
		{
			if (text.length() > 0)
				text.append(" · ");

			int roll = roll(stat);
			text.append(stat.label()).append(' ').append(roll > 0 ? "+" + roll : String.valueOf(roll));
		}

		return text.toString();
	}

	private static boolean inRange(int roll)
	{
		return roll >= MIN && roll <= MAX;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof StatRolls))
			return false;

		StatRolls that = (StatRolls) other;
		return Arrays.equals(values(), that.values()) && temperament == that.temperament;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(Arrays.hashCode(values()), temperament);
	}
}
