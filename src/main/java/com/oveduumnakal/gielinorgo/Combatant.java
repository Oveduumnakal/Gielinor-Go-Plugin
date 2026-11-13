/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** One side of a battle: a species at a level, with the stats that follow from the two. */
final class Combatant
{
	private final String name;
	private final String speciesKey;
	private final CombatType type;
	private final int level;
	private final int maxHp;
	private final int attack;
	private final int defence;
	private final int speed;
	private double hp;
	private boolean gilded;
	private final Species species;
	private int npcId;
	private List<Move> moves;
	private Status status;
	private int statusTurns;
	private int venomStep;

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
		this.species = species;
		this.speciesKey = species.key();
		this.type = species.style();
		this.moves = Collections.singletonList(Move.basic(species.style()));
		this.level = level;
		this.maxHp = scaled(20 + 2.0 * level + tier * level / 2.0, Stat.HEALTH, rolls);
		this.attack = scaled(5 + level / 2.0 + tier * level / 8.0, Stat.ATTACK, rolls);
		this.defence = scaled(5 + level / 2.0 + tier * level / 10.0, Stat.DEFENCE, rolls);
		this.speed = scaled(5 + level / 2, Stat.SPEED, rolls);
		this.hp = maxHp * Math.max(0.0, Math.min(1.0, hpFraction));
	}

	/**
	 * Applies the species' factor, then the creature's rolls and temperament, to a stat the level formula gave.
	 *
	 * @param base the stat from the species level and the creature level
	 * @param stat which stat it is
	 * @param rolls the creature's rolls and temperament
	 * @return the stat, rounded
	 */
	private int scaled(double base, Stat stat, StatRolls rolls)
	{
		return (int) Math.round(base * species.factor(stat) * rolls.factor(stat));
	}

	/** @return the name the battle lines use: the species name, or a nickname */
	String name()
	{
		return name;
	}

	/** @return the key of the species, so a battle that is acted out on screen can draw it */
	String speciesKey()
	{
		return speciesKey;
	}

	/**
	 * Notes that this fighter is a gilded creature. It only changes how the fighter is drawn, never a stat.
	 *
	 * @param shining whether the creature is gilded
	 * @return this fighter
	 */
	Combatant gilded(boolean shining)
	{
		this.gilded = shining;
		return this;
	}

	/**
	 * Notes which NPC the creature was caught as, so it is drawn like the variant that was caught. It only changes
	 * how the fighter is drawn, never a stat.
	 *
	 * @param saved the creature's saved NPC id, 0 when unknown
	 * @return this fighter
	 */
	Combatant npc(int saved)
	{
		this.npcId = species.npcIdFor(saved);
		return this;
	}

	/** @return the NPC id to draw the creature with, 0 when it has none: a creature with no source NPC is the first */
	int npcId()
	{
		return npcId;
	}

	/** @return whether the creature is gilded, so it is drawn gold */
	boolean isGilded()
	{
		return gilded;
	}

	/** @return the combat style */
	CombatType type()
	{
		return type;
	}

	/**
	 * Gives the fighter its moves. A fighter that is never given any fights with one basic attack of its own style,
	 * which is the one attack every creature had before moves existed.
	 *
	 * @param known the moves, in the order the creature knows them; null or empty keeps the basic attack
	 * @return this fighter
	 */
	Combatant moves(List<Move> known)
	{
		if (known != null && !known.isEmpty())
			this.moves = Collections.unmodifiableList(new ArrayList<>(known));

		return this;
	}

	/** @return the moves the fighter can use, in the order it knows them; never empty */
	List<Move> moves()
	{
		return moves;
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

	/** @return the status the fighter has, or null for none */
	Status status()
	{
		return status;
	}

	/** @return the traits of the fighter's species, in a set the caller may change */
	Set<Trait> traits()
	{
		return species.traits();
	}

	/**
	 * Tells whether a status could be put on the fighter now: it has none and its traits do not block it.
	 *
	 * @param wanted the status
	 * @return true when {@link #afflict} would put it on
	 */
	boolean canAfflict(Status wanted)
	{
		return status == null && !wanted.blockedBy(species.traits());
	}

	/**
	 * Puts a status on the fighter. A fighter that already has one keeps it, and one that is immune stays free. A
	 * freeze draws once, to roll whether it lasts one turn or two.
	 *
	 * @param wanted the status
	 * @param random the source of the freeze length
	 * @return whether the status was put on
	 */
	boolean afflict(Status wanted, Random random)
	{
		if (!canAfflict(wanted))
			return false;

		status = wanted;
		venomStep = 0;
		statusTurns = wanted == Status.FREEZE ? 1 + random.nextInt(2) : wanted.turns();
		return true;
	}

	/** Takes the status away, as a switch, a cure or the end of a pairing does. */
	void clearStatus()
	{
		status = null;
		statusTurns = 0;
		venomStep = 0;
	}

	/**
	 * Ends a round for a fighter: a damaging status takes its health and runs down by one round, and ends when its
	 * rounds are up. A fighter with no such status takes nothing.
	 *
	 * @return the health taken, 0 when none
	 */
	int endOfRound()
	{
		if (status == null || !status.damaging())
			return 0;

		int damage = status.roundDamage(maxHp, venomStep);
		venomStep++;
		hit(damage);
		if (--statusTurns <= 0)
			clearStatus();

		return damage;
	}

	/**
	 * Asks whether the fighter loses its turn. A freeze always does, and a stun does half the time with one draw;
	 * either runs down by one turn and ends when its turns are up. Any other status, or none, never does and draws
	 * nothing.
	 *
	 * @param random the source of the stun roll
	 * @return true when the fighter cannot move this turn
	 */
	boolean skipsTurn(Random random)
	{
		if (status == null || !status.skipping())
			return false;

		boolean skip = status == Status.FREEZE || random.nextDouble() < Status.STUN_SKIP_CHANCE;
		if (--statusTurns <= 0)
			clearStatus();

		return skip;
	}

	/**
	 * Restores health, as a salve does. Health never goes above full.
	 *
	 * @param share the share of full health to restore, from 0 to 1
	 * @return the health restored, a whole number of points
	 */
	int heal(double share)
	{
		double before = hp;
		hp = Math.min(maxHp, hp + share * maxHp);
		return (int) Math.round(hp - before);
	}

	/**
	 * Brings a fainted fighter back, as a Revive draught does.
	 *
	 * @param share the share of full health it comes back with
	 */
	void revive(double share)
	{
		hp = Math.max(hp, maxHp * Math.max(0.0, Math.min(1.0, share)));
	}
}
