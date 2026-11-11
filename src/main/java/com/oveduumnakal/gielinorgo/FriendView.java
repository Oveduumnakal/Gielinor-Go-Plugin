/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One party member who also plays, as the side panel shows them. Immutable, so it can cross to the UI thread. */
final class FriendView
{
	private final long memberId;
	private final String name;
	private final int trainerLevel;
	private final String team;
	private final boolean challengeable;
	private final boolean waiting;
	private final List<String> tradeable;
	private final String offer;
	private final boolean offered;

	FriendView(long memberId, String name, int trainerLevel, String team, boolean challengeable, boolean waiting,
		List<String> tradeable)
	{
		this(memberId, name, trainerLevel, team, challengeable, waiting, tradeable, "", false);
	}

	private FriendView(long memberId, String name, int trainerLevel, String team, boolean challengeable,
		boolean waiting, List<String> tradeable, String offer, boolean offered)
	{
		this.memberId = memberId;
		this.name = name;
		this.trainerLevel = trainerLevel;
		this.team = team;
		this.challengeable = challengeable;
		this.waiting = waiting;
		this.tradeable = Collections.unmodifiableList(new ArrayList<>(tradeable));
		this.offer = offer;
		this.offered = offered;
	}

	/**
	 * Adds where trading with this friend stands.
	 *
	 * @param theirOffer the trade this friend is offering the player, in words, or empty when there is none
	 * @param mineWaits whether the player has offered this friend a trade that is not answered yet
	 * @return a copy of this view with the trade details
	 */
	FriendView withTrade(String theirOffer, boolean mineWaits)
	{
		return new FriendView(memberId, name, trainerLevel, team, challengeable, waiting, tradeable, theirOffer,
			mineWaits);
	}

	/** @return the id the panel passes back when the player challenges this friend */
	long memberId()
	{
		return memberId;
	}

	/** @return the friend's name in the game */
	String name()
	{
		return name;
	}

	/** @return the friend's trainer level */
	int trainerLevel()
	{
		return trainerLevel;
	}

	/** @return the friend's creatures in one line, such as {@code Goblin 4, Cow 5}; empty when they have none */
	String team()
	{
		return team;
	}

	/** @return whether the friend has a team to battle */
	boolean challengeable()
	{
		return challengeable;
	}

	/** @return whether the player has challenged this friend and no answer has come yet */
	boolean waiting()
	{
		return waiting;
	}

	/** @return what can be asked of this friend in a trade, in words: their team's creatures, then their eggs */
	List<String> tradeable()
	{
		return tradeable;
	}

	/** @return the trade this friend is offering the player, in words; empty when there is none */
	String offer()
	{
		return offer;
	}

	/** @return whether the player has offered this friend a trade that is not answered yet */
	boolean offered()
	{
		return offered;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof FriendView))
			return false;

		FriendView that = (FriendView) other;
		return memberId == that.memberId
			&& trainerLevel == that.trainerLevel
			&& challengeable == that.challengeable
			&& waiting == that.waiting
			&& offered == that.offered
			&& Objects.equals(name, that.name)
			&& Objects.equals(team, that.team)
			&& Objects.equals(tradeable, that.tradeable)
			&& Objects.equals(offer, that.offer);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(memberId, name, trainerLevel, team, challengeable, waiting, tradeable, offer, offered);
	}
}
