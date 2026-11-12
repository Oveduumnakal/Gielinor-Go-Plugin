/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * A creature's temperament: {@link #STEADY} changes nothing, every other one gives 10% more of one stat and 10%
 * less of another.
 */
enum Temperament
{
	STEADY("Steady", null, null),
	BRAVE("Brave", Stat.ATTACK, Stat.DEFENCE),
	RECKLESS("Reckless", Stat.ATTACK, Stat.HEALTH),
	FIERCE("Fierce", Stat.ATTACK, Stat.SPEED),
	GUARDED("Guarded", Stat.DEFENCE, Stat.ATTACK),
	STUBBORN("Stubborn", Stat.DEFENCE, Stat.SPEED),
	HARDY("Hardy", Stat.HEALTH, Stat.ATTACK),
	STOUT("Stout", Stat.HEALTH, Stat.SPEED),
	HASTY("Hasty", Stat.SPEED, Stat.DEFENCE),
	NIMBLE("Nimble", Stat.SPEED, Stat.HEALTH);

	/** Share of a stat a temperament adds or takes away. */
	static final double SHIFT = 0.10;

	private final String label;
	private final Stat up;
	private final Stat down;

	Temperament(String label, Stat up, Stat down)
	{
		this.label = label;
		this.up = up;
		this.down = down;
	}

	/** @return the name shown to the player */
	String label()
	{
		return label;
	}

	/** @return what it does, such as {@code +10% Speed, -10% Health}, or {@code no effect} for steady */
	String effect()
	{
		if (up == null)
			return "no effect";

		long percent = Math.round(SHIFT * 100);
		return "+" + percent + "% " + up.label() + ", -" + percent + "% " + down.label();
	}

	/**
	 * Finds a temperament by the name saved or sent for it.
	 *
	 * @param name a {@link #name()}, as stored or sent; null stands for {@link #STEADY}
	 * @return the temperament, or null when the name is not one
	 */
	static Temperament named(String name)
	{
		if (name == null)
			return STEADY;

		for (Temperament temperament : values())
		{
			if (temperament.name().equals(name))
				return temperament;
		}

		return null;
	}

	/**
	 * Tells how this temperament scales a stat.
	 *
	 * @param stat the stat
	 * @return 1.1 for the stat it raises, 0.9 for the one it lowers, otherwise exactly 1.0
	 */
	double factor(Stat stat)
	{
		if (stat == up)
			return 1.0 + SHIFT;

		if (stat == down)
			return 1.0 - SHIFT;

		return 1.0;
	}
}
