/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Objects;

/**
 * What the player does on one turn of a battle: attack (with a move), switch creature, throw a jar, run, or (later)
 * use an item.
 */
final class BattleChoice
{
	/** The kinds of choice. */
	enum Kind
	{
		/** The creature attacks. */
		ATTACK,

		/** Another creature of the team steps in. It costs the turn, unless the first one fainted. */
		SWITCH,

		/** A jar is thrown at the wild creature. */
		CATCH,

		/** The battle ends and nothing is won. */
		RUN,

		/** Reserved for healing items. It cannot be chosen yet. */
		ITEM
	}

	private static final BattleChoice ATTACK = new BattleChoice(Kind.ATTACK, -1, null, -1);
	private static final BattleChoice RUN = new BattleChoice(Kind.RUN, -1, null, -1);
	private static final BattleChoice ITEM = new BattleChoice(Kind.ITEM, -1, null, -1);

	private final Kind kind;
	private final int teamIndex;
	private final JarType jar;
	private final int moveIndex;

	private BattleChoice(Kind kind, int teamIndex, JarType jar, int moveIndex)
	{
		this.kind = kind;
		this.teamIndex = teamIndex;
		this.jar = jar;
		this.moveIndex = moveIndex;
	}

	/** @return the choice to attack and let the creature pick its own move, as an automatic battle does */
	static BattleChoice attack()
	{
		return ATTACK;
	}

	/**
	 * Makes the choice to attack with one of the creature's moves.
	 *
	 * @param moveIndex the place of the move in the creature's moves, from 0
	 * @return the choice
	 */
	static BattleChoice attack(int moveIndex)
	{
		return new BattleChoice(Kind.ATTACK, -1, null, moveIndex);
	}

	/** @return the choice to run away */
	static BattleChoice run()
	{
		return RUN;
	}

	/** @return the reserved choice to use an item */
	static BattleChoice item()
	{
		return ITEM;
	}

	/**
	 * Makes the choice to switch.
	 *
	 * @param teamIndex the place of the creature that steps in, in the battle's own team list
	 * @return the choice
	 */
	static BattleChoice switchTo(int teamIndex)
	{
		return new BattleChoice(Kind.SWITCH, teamIndex, null, -1);
	}

	/**
	 * Makes the choice to throw a jar.
	 *
	 * @param jar the jar to throw, or null for the one the game picks
	 * @return the choice
	 */
	static BattleChoice catching(JarType jar)
	{
		return new BattleChoice(Kind.CATCH, -1, jar, -1);
	}

	/** @return which kind of choice this is */
	Kind kind()
	{
		return kind;
	}

	/** @return the place of the creature that steps in, or -1 when the choice is not a switch */
	int teamIndex()
	{
		return teamIndex;
	}

	/** @return the place of the move to use, or -1 when the creature picks its own or the choice is not an attack */
	int moveIndex()
	{
		return moveIndex;
	}

	/** @return the jar to throw, or null when the game picks or the choice is not a catch */
	JarType jar()
	{
		return jar;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof BattleChoice))
			return false;

		BattleChoice that = (BattleChoice) other;
		return kind == that.kind && teamIndex == that.teamIndex && jar == that.jar && moveIndex == that.moveIndex;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(kind, teamIndex, jar, moveIndex);
	}

	@Override
	public String toString()
	{
		return kind + (teamIndex >= 0 ? "(" + teamIndex + ")" : "") + (jar != null ? "(" + jar + ")" : "")
			+ (moveIndex >= 0 ? "[move " + moveIndex + "]" : "");
	}
}
