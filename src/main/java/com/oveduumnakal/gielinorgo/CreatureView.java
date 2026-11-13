/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One owned creature as the side panel shows it. Immutable, so it can cross from the game thread to the UI. */
final class CreatureView
{
	private final String name;
	private final int level;
	private final double xpProgress;
	private final double hpFraction;
	private final Rarity rarity;
	private final CombatType type;
	private final boolean gilded;
	private final String nickname;
	private final StatRolls rolls;
	private final int npcId;
	private final List<Move> moves;
	private final List<Move> pending;
	private final int hearts;

	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded, String nickname)
	{
		this(name, level, xpProgress, hpFraction, rarity, type, gilded, nickname, StatRolls.AVERAGE);
	}

	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded, String nickname, StatRolls rolls)
	{
		this(name, level, xpProgress, hpFraction, rarity, type, gilded, nickname, rolls, 0);
	}

	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded, String nickname, StatRolls rolls, int npcId)
	{
		this(name, level, xpProgress, hpFraction, rarity, type, gilded, nickname, rolls, npcId,
			Collections.emptyList(), Collections.emptyList());
	}

	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded, String nickname, StatRolls rolls, int npcId, List<Move> moves, List<Move> pending)
	{
		this(name, level, xpProgress, hpFraction, rarity, type, gilded, nickname, rolls, npcId, moves, pending, 0);
	}

	/**
	 * Describes a creature with its moves.
	 *
	 * @param name the species name
	 * @param level the creature level
	 * @param xpProgress progress to the next level, from 0 to 1
	 * @param hpFraction the share of full health
	 * @param rarity the species' rarity
	 * @param type the species' combat style
	 * @param gilded whether it is gilded
	 * @param nickname its nickname, or null
	 * @param rolls its rolls and temperament
	 * @param npcId the NPC id its portrait is drawn from, or 0
	 * @param moves the moves it knows, in the order learned
	 * @param pending the moves that wait for a choice, oldest first
	 * @param hearts its bond hearts, 0 to {@value BondRules#MAX_HEARTS}
	 */
	CreatureView(String name, int level, double xpProgress, double hpFraction, Rarity rarity, CombatType type,
		boolean gilded, String nickname, StatRolls rolls, int npcId, List<Move> moves, List<Move> pending,
		int hearts)
	{
		this.hearts = hearts;
		this.moves = Collections.unmodifiableList(new ArrayList<>(moves));
		this.pending = Collections.unmodifiableList(new ArrayList<>(pending));
		this.npcId = npcId;
		this.rolls = rolls;
		this.gilded = gilded;
		this.nickname = nickname;
		this.name = name;
		this.level = level;
		this.xpProgress = xpProgress;
		this.hpFraction = hpFraction;
		this.rarity = rarity;
		this.type = type;
	}

	/** @return its bond hearts, 0 to {@value BondRules#MAX_HEARTS} */
	int hearts()
	{
		return hearts;
	}

	/** @return the species name, which stays the species name even when there is a nickname */
	String name()
	{
		return name;
	}

	/** @return the NPC id its portrait is drawn from, or 0 for the species' first */
	int npcId()
	{
		return npcId;
	}

	/** @return the moves it knows, in the order learned; empty when the game has no moves */
	List<Move> moves()
	{
		return moves;
	}

	/** @return the moves that wait for the player to forget one or skip, oldest first */
	List<Move> pending()
	{
		return pending;
	}

	/** @return the nickname the player gave it, or null when it has none */
	String nickname()
	{
		return nickname;
	}

	/** @return the creature's rolls and temperament */
	StatRolls rolls()
	{
		return rolls;
	}

	/** @return the one-word rating of its rolls */
	StatRolls.Rating rating()
	{
		return rolls.rating();
	}

	/** @return the name of its temperament, such as Brave */
	String temperamentLabel()
	{
		return rolls.temperament().label();
	}

	/** @return what its temperament does, such as {@code +10% Speed, -10% Health}, or {@code no effect} */
	String temperamentEffect()
	{
		return rolls.temperament().effect();
	}

	/** @return its rolls with the net percentage each stat gets, as two HTML lines */
	String rollsNetLines()
	{
		return rolls.describeNet();
	}

	/** @return its four rolls as a line, such as {@code Health +3 · Attack -1 · Defence 0 · Speed +5} */
	String rollsLine()
	{
		return rolls.describe();
	}

	/** @return the creature level, 1 to 99 */
	int level()
	{
		return level;
	}

	/** @return progress to the next level, from 0 to 1 */
	double xpProgress()
	{
		return xpProgress;
	}

	/** @return the share of full health, from 0 to 1 */
	double hpFraction()
	{
		return hpFraction;
	}

	/** @return the species' rarity tier */
	Rarity rarity()
	{
		return rarity;
	}

	/** @return the species' combat style */
	CombatType type()
	{
		return type;
	}

	/** @return whether this is a rare gilded creature */
	boolean gilded()
	{
		return gilded;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof CreatureView))
			return false;

		CreatureView that = (CreatureView) other;
		return level == that.level
			&& gilded == that.gilded
			&& Double.compare(xpProgress, that.xpProgress) == 0
			&& Double.compare(hpFraction, that.hpFraction) == 0
			&& rarity == that.rarity
			&& type == that.type
			&& Objects.equals(name, that.name)
			&& Objects.equals(nickname, that.nickname)
			&& npcId == that.npcId
			&& hearts == that.hearts
			&& moves.equals(that.moves)
			&& pending.equals(that.pending)
			&& rolls.equals(that.rolls);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, level, xpProgress, hpFraction, rarity, type, gilded, nickname, rolls, npcId, moves,
			pending, hearts);
	}
}
