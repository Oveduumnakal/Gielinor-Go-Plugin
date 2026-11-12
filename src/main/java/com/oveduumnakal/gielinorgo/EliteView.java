/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Objects;

/** The Elite challenge card as the side panel shows it. Immutable. */
final class EliteView
{
	/** What the card offers. */
	enum Mode
	{
		/** The card is not shown: the challenge does not exist. */
		HIDDEN,

		/** Fewer than 8 sigils. */
		LOCKED,

		/** All sigils, but the player is away from the Champions' Guild. */
		AWAY,

		/** At the guild, ready to start. */
		HERE,

		/** A run is open: Continue or Forfeit. */
		RUNNING
	}

	/** A view for a game with no Elite challenge. */
	static final EliteView NONE = new EliteView(Mode.HIDDEN, 0, 0, 0, null, null, null, false);

	private final Mode mode;
	private final int sigils;
	private final int stage;
	private final int stages;
	private final String opponent;
	private final String team;
	private final String reward;
	private final boolean titled;

	EliteView(Mode mode, int sigils, int stage, int stages, String opponent, String team, String reward,
		boolean titled)
	{
		this.mode = mode;
		this.sigils = sigils;
		this.stage = stage;
		this.stages = stages;
		this.opponent = opponent;
		this.team = team;
		this.reward = reward;
		this.titled = titled;
	}

	/** @return what the card offers */
	Mode mode()
	{
		return mode;
	}

	/** @return the City Champion sigils held */
	int sigils()
	{
		return sigils;
	}

	/** @return the battle that is next, counted from 1, 0 when no run is open */
	int stage()
	{
		return stage;
	}

	/** @return how many battles a run has */
	int stages()
	{
		return stages;
	}

	/** @return the name of the next opponent, or null when no run is open */
	String opponent()
	{
		return opponent;
	}

	/** @return the next opposing team in one line, or null when no run is open */
	String team()
	{
		return team;
	}

	/** @return the reward for the next clear in a few words */
	String reward()
	{
		return reward;
	}

	/** @return whether the player has cleared the challenge and holds the title Grand Champion */
	boolean titled()
	{
		return titled;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof EliteView))
			return false;

		EliteView that = (EliteView) other;
		return mode == that.mode && sigils == that.sigils && stage == that.stage && stages == that.stages
			&& titled == that.titled && Objects.equals(opponent, that.opponent) && Objects.equals(team, that.team)
			&& Objects.equals(reward, that.reward);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(mode, sigils, stage, stages, opponent, team, reward, titled);
	}
}
