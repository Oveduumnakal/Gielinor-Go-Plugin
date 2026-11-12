/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

/** One side of a battle: a species at a level, with the stats that follow from the two. */
final class Combatant
{
	private final String name;
	private final CombatType type;
	private final CombatType secondStyle;
	private final double secondChance;
	private final int level;
	private final int maxHp;
	private final int attack;
	private final int defence;
	private final int speed;
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
		this(species, species.name(), level, hpFraction);
	}

	/**
	 * Builds a fighter that goes by another name in the battle lines, such as a nickname.
	 *
	 * @param species the species
	 * @param displayName the name the battle lines use
	 * @param level the creature level, 1 to 99
	 * @param hpFraction the share of full health it starts with, from 0 to 1
	 */
	Combatant(Species species, String displayName, int level, double hpFraction)
	{
		this(species, displayName, level, hpFraction, StatRolls.AVERAGE);
	}

	/**
	 * Builds a fighter with rolls and a temperament, under the species name.
	 *
	 * @param species the species
	 * @param level the creature level, 1 to 99
	 * @param hpFraction the share of full health it starts with, from 0 to 1
	 * @param rolls the creature's rolls and temperament
	 */
	Combatant(Species species, int level, double hpFraction, StatRolls rolls)
	{
		this(species, species.name(), level, hpFraction, rolls);
	}

	/**
	 * Builds a fighter with rolls and a temperament that goes by another name in the battle lines.
	 *
	 * @param species the species
	 * @param displayName the name the battle lines use
	 * @param level the creature level, 1 to 99
	 * @param hpFraction the share of full health it starts with, from 0 to 1
	 * @param rolls the creature's rolls and temperament; {@link StatRolls#AVERAGE} leaves every stat as it was
	 */
	Combatant(Species species, String displayName, int level, double hpFraction, StatRolls rolls)
	{
		double tier = Math.sqrt(Math.max(1, species.level()));
		this.name = displayName;
		this.type = species.style();
		this.secondStyle = species.secondStyle();
		this.secondChance = species.secondChance();
		this.level = level;
		this.maxHp = (int) Math.round((20 + 2.0 * level + tier * level / 2.0) * rolls.factor(Stat.HEALTH));
		this.attack = (int) Math.round((5 + level / 2.0 + tier * level / 8.0) * rolls.factor(Stat.ATTACK));
		this.defence = (int) Math.round((5 + level / 2.0 + tier * level / 10.0) * rolls.factor(Stat.DEFENCE));
		this.speed = (int) Math.round((5 + level / 2) * rolls.factor(Stat.SPEED));
		this.hp = maxHp * Math.max(0.0, Math.min(1.0, hpFraction));
	}

	/** @return the name the battle lines use: the species name, or a nickname */
	String name()
	{
		return name;
	}

	/** @return the combat style */
	CombatType type()
	{
		return type;
	}

	/**
	 * Picks the style of one attack. A creature with a second style rolls it and uses it on that share of its hits;
	 * any other uses its one style and does not touch the random source, so battles between those are unchanged.
	 *
	 * @param random the battle's random source
	 * @return the style of this attack
	 */
	CombatType rollStyle(Random random)
	{
		if (secondStyle == null)
			return type;

		return random.nextDouble() < secondChance ? secondStyle : type;
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

	/** @return the speed stat: the higher one attacks first */
	int speed()
	{
		return speed;
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

	/** Takes the fighter out of the battle, for a duel that ran out of turns. */
	void knockOut()
	{
		hp = 0;
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
