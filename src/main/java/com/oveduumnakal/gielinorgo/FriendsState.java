/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The friends the Battle tab lists, copied out at one moment. Immutable, so it can cross to the UI thread. */
final class FriendsState
{
	private final boolean inParty;
	private final List<FriendView> friends;

	FriendsState(boolean inParty, List<FriendView> friends)
	{
		this.inParty = inParty;
		this.friends = Collections.unmodifiableList(new ArrayList<>(friends));
	}

	/** @return a state for a player who is in no party */
	static FriendsState none()
	{
		return new FriendsState(false, Collections.emptyList());
	}

	/** @return whether the player is in a RuneLite party */
	boolean inParty()
	{
		return inParty;
	}

	/** @return the party members who also play, in the order they were met */
	List<FriendView> friends()
	{
		return friends;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof FriendsState))
			return false;

		FriendsState that = (FriendsState) other;
		return inParty == that.inParty && friends.equals(that.friends);
	}

	@Override
	public int hashCode()
	{
		return friends.hashCode() * 31 + (inParty ? 1 : 0);
	}
}
