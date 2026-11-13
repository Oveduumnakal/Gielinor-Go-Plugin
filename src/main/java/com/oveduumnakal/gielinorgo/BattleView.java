/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the battle box shows while a battle the player steers waits for a choice: both creatures, the time left, and
 * which choices are open. Immutable, so it can be handed to the UI thread.
 */
final class BattleView
{
	/** One creature of the team that could step in. */
	static final class Option
	{
		private final int teamIndex;
		private final String name;
		private final int level;
		private final double hpFraction;

		/**
		 * Describes a creature that could step in.
		 *
		 * @param teamIndex its place in the battle's team, which {@link BattleChoice#switchTo(int)} takes
		 * @param name its name
		 * @param level its level
		 * @param hpFraction the share of full health it has left
		 */
		Option(int teamIndex, String name, int level, double hpFraction)
		{
			this.teamIndex = teamIndex;
			this.name = name;
			this.level = level;
			this.hpFraction = hpFraction;
		}

		/** @return its place in the battle's team */
		int teamIndex()
		{
			return teamIndex;
		}

		/** @return its name */
		String name()
		{
			return name;
		}

		/** @return its level */
		int level()
		{
			return level;
		}

		/** @return the share of full health it has left, from 0 to 1 */
		double hpFraction()
		{
			return hpFraction;
		}
	}

	private final String mineName;
	private final int mineLevel;
	private final double mineHp;
	private final String foeName;
	private final int foeLevel;
	private final double foeHp;
	private final Status mineStatus;
	private final Status foeStatus;
	private final boolean forcedSwitch;
	private final boolean canCatch;
	private final String jarLabel;
	private final int jarCount;
	private final int secondsLeft;
	private final double timeLeft;
	private final List<Option> options;
	private final List<Move> moves;

	/**
	 * Describes the battle at one moment.
	 *
	 * @param mine the player's creature in the fight
	 * @param foe the opposing creature in the fight
	 * @param forcedSwitch whether the creature in the fight fainted and the next one must be picked
	 * @param canCatch whether a jar can be thrown: a wild creature and at least one jar
	 * @param jar the jar a throw uses, or null when there is none
	 * @param jarCount how many of that jar are left
	 * @param secondsLeft the whole seconds left to choose
	 * @param timeLeft the share of the time left, from 0 to 1
	 * @param options the creatures that could step in
	 */
	BattleView(Combatant mine, Combatant foe, boolean forcedSwitch, boolean canCatch, JarType jar, int jarCount,
		int secondsLeft, double timeLeft, List<Option> options)
	{
		this.mineName = mine.name();
		this.mineLevel = mine.level();
		this.mineHp = mine.hpFraction();
		this.foeName = foe.name();
		this.foeLevel = foe.level();
		this.foeHp = foe.hpFraction();
		this.mineStatus = mine.status();
		this.foeStatus = foe.status();
		this.forcedSwitch = forcedSwitch;
		this.canCatch = canCatch;
		this.jarLabel = jar == null ? null : jar.label();
		this.jarCount = jarCount;
		this.secondsLeft = secondsLeft;
		this.timeLeft = timeLeft;
		this.options = Collections.unmodifiableList(new ArrayList<>(options));
		this.moves = mine.moves();
	}

	/** @return the moves the player's creature in the fight can use, in the order it knows them; never empty */
	List<Move> moves()
	{
		return moves;
	}

	/** @return whether there is a real choice of move: the creature knows more than one */
	boolean canChooseMove()
	{
		return moves.size() > 1;
	}

	/** @return the name of the player's creature in the fight */
	String mineName()
	{
		return mineName;
	}

	/** @return the level of the player's creature in the fight */
	int mineLevel()
	{
		return mineLevel;
	}

	/** @return the share of full health the player's creature has left */
	double mineHp()
	{
		return mineHp;
	}

	/** @return the status of the player's creature in the fight, or null for none */
	Status mineStatus()
	{
		return mineStatus;
	}

	/** @return the status of the opposing creature in the fight, or null for none */
	Status foeStatus()
	{
		return foeStatus;
	}

	/**
	 * Words a creature's status as a tag to put after its name.
	 *
	 * @param status the status, or null
	 * @return a space and the tag in square brackets, or an empty text for none
	 */
	static String tagText(Status status)
	{
		return status == null ? "" : " [" + status.tag() + "]";
	}

	/** @return the name of the opposing creature in the fight */
	String foeName()
	{
		return foeName;
	}

	/** @return the level of the opposing creature in the fight */
	int foeLevel()
	{
		return foeLevel;
	}

	/** @return the share of full health the opposing creature has left */
	double foeHp()
	{
		return foeHp;
	}

	/** @return whether the next creature must be picked, because the one in the fight fainted */
	boolean forcedSwitch()
	{
		return forcedSwitch;
	}

	/** @return whether a jar can be thrown */
	boolean canCatch()
	{
		return canCatch;
	}

	/** @return the name of the jar a throw uses, or null when there is none */
	String jarLabel()
	{
		return jarLabel;
	}

	/** @return how many of that jar are left */
	int jarCount()
	{
		return jarCount;
	}

	/** @return the whole seconds left to choose */
	int secondsLeft()
	{
		return secondsLeft;
	}

	/** @return the share of the time left, from 0 to 1 */
	double timeLeft()
	{
		return timeLeft;
	}

	/** @return the creatures that could step in, with their places in the battle's team */
	List<Option> options()
	{
		return options;
	}

	/** @return whether anyone could step in */
	boolean canSwitch()
	{
		return !options.isEmpty();
	}
}
