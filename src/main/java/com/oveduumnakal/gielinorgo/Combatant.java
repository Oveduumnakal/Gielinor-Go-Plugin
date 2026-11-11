/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** One side of a battle: a species at a level, with the stats that follow from the two. */
final class Combatant
{
	private final String name;
	private final CombatType type;
	private final int level;
	private final int maxHp;
	private final int attack;
	private final int defence;
	private double hp;

	/**
	 * Builds a fighter.
	 *
	 * @param species the species
	 * @param level the creature level, 1 to 99
	 * @param hpFraction the share of full health it starts with, from 0 to 1
	 */
	Combatant(Species species, int level, double hpFraction)
	{
		double tier = Math.sqrt(Math.max(1, species.level()));
		this.name = species.name();
		this.type = species.style();
		this.level = level;
		this.maxHp = (int) Math.round(20 + 2.0 * level + tier * level / 2.0);
		this.attack = (int) Math.round(5 + level / 2.0 + tier * level / 8.0);
		this.defence = (int) Math.round(5 + level / 2.0 + tier * level / 10.0);
		this.hp = maxHp * Math.max(0.0, Math.min(1.0, hpFraction));
	}

	/** @return the species name */
	String name()
	{
		return name;
	}

	/** @return the combat style */
	CombatType type()
	{
		return type;
	}

	/** @return the creature level */
	int level()
	{
		return level;
	}

	/** @return health when fully rested */
	int maxHp()
	{
		return maxHp;
	}

	/** @return the attack stat */
	int attack()
	{
		return attack;
	}

	/** @return the defence stat */
	int defence()
	{
		return defence;
	}

	/** @return health left */
	double hp()
	{
		return hp;
	}

	/** @return the share of full health left, from 0 to 1 */
	double hpFraction()
	{
		return Math.max(0.0, hp / maxHp);
	}

	/** @return whether the fighter is out of health */
	boolean fainted()
	{
		return hp <= 0;
	}

	/**
	 * Takes damage.
	 *
	 * @param damage the health to remove
	 */
	void hit(int damage)
	{
		hp = Math.max(0, hp - damage);
	}
}
