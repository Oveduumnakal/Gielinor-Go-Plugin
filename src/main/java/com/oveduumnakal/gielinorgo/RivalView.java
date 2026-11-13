/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Objects;

/** The rival card as the side panel shows it. Immutable, so it can cross from the game thread to the UI. */
final class RivalView
{
	/** What the card offers. */
	enum Mode
	{
		/** The card is not shown: there is no rival. */
		HIDDEN,

		/** The next battle is not open yet. */
		LOCKED,

		/** The next battle is open and can be fought anywhere. */
		READY,

		/** All five battles are won. */
		BEATEN
	}

	/** A view for a game with no rival. */
	static final RivalView NONE = new RivalView(Mode.HIDDEN, null, 0, null, null, null, null, null);

	private final Mode mode;
	private final String name;
	private final int battle;
	private final String line;
	private final String team;
	private final String reward;
	private final String status;
	private final JarType rewardJar;

	RivalView(Mode mode, String name, int battle, String line, String team, String reward, String status,
		JarType rewardJar)
	{
		this.mode = mode;
		this.name = name;
		this.battle = battle;
		this.line = line;
		this.team = team;
		this.reward = reward;
		this.status = status;
		this.rewardJar = rewardJar;
	}

	/** @return what the card offers */
	Mode mode()
	{
		return mode;
	}

	/** @return the rival's name */
	String name()
	{
		return name;
	}

	/** @return the battle the card is about, counted from 1; 5 once all are won */
	int battle()
	{
		return battle;
	}

	/** @return what the rival says, or null when there is nothing to say yet */
	String line()
	{
		return line;
	}

	/** @return the team for the open battle in one line, or null when none is open */
	String team()
	{
		return team;
	}

	/** @return what the next win pays in a few words, or null when nothing is left to win */
	String reward()
	{
		return reward;
	}

	/** @return a short sentence on where things stand, such as {@code Earn 4 sigils to meet Bryn again} */
	String status()
	{
		return status;
	}

	/** @return the jar the reward is paid in, whose picture the card shows, or null */
	JarType rewardJar()
	{
		return rewardJar;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof RivalView))
			return false;

		RivalView that = (RivalView) other;
		return mode == that.mode
			&& battle == that.battle
			&& rewardJar == that.rewardJar
			&& Objects.equals(name, that.name)
			&& Objects.equals(line, that.line)
			&& Objects.equals(team, that.team)
			&& Objects.equals(reward, that.reward)
			&& Objects.equals(status, that.status);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(mode, name, battle, line, team, reward, status, rewardJar);
	}
}
