/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Everything the painted window draws from the game, copied at one moment on the game thread. Paint reads one of these
 * from a volatile field and never touches the live game; the version goes up with every new one, so a cached still
 * layer knows when it is out of date. Immutable. The facts the shell itself needs (can Storage be chosen, is something
 * waiting) are worked out when it is built, so painting does no rule work. Holds no RuneLite types.
 */
final class WindowSnapshot
{
	/** The snapshot before the save is read: nothing loaded, nothing waiting. */
	static final WindowSnapshot EMPTY = new WindowSnapshot(0, null, false, false, false, false, false, 0);

	private final long version;
	private final PanelState state;
	private final boolean loaded;
	private final boolean storageEnabled;
	private final boolean pendingMove;
	private final boolean readyEgg;
	private final boolean firstRun;
	private final int doneTasks;
	private FriendsState friends = FriendsState.none();

	private WindowSnapshot(long version, PanelState state, boolean loaded, boolean storageEnabled,
		boolean pendingMove, boolean readyEgg, boolean firstRun, int doneTasks)
	{
		this.version = version;
		this.state = state;
		this.loaded = loaded;
		this.storageEnabled = storageEnabled;
		this.pendingMove = pendingMove;
		this.readyEgg = readyEgg;
		this.firstRun = firstRun;
		this.doneTasks = doneTasks;
	}

	/**
	 * Builds a snapshot of a loaded save.
	 *
	 * @param version the version, one more than the snapshot it replaces
	 * @param state what the panel shows
	 * @param storageEnabled whether the team may be changed here, which is what lets the Storage tab be chosen
	 * @return the snapshot
	 */
	static WindowSnapshot of(long version, PanelState state, boolean storageEnabled)
	{
		boolean pending = false;
		for (CreatureView creature : state.team())
			pending |= !creature.pending().isEmpty();

		boolean ready = false;
		for (EggView egg : state.eggs())
			ready |= egg.tilesLeft() <= 0;

		int done = 0;
		if (state.tasks() != null)
			for (TaskView.Row row : state.tasks().rows())
				done += row.done() ? 1 : 0;

		return new WindowSnapshot(version, state, true, storageEnabled, pending, ready, !state.starters().isEmpty(),
			done);
	}

	/**
	 * Adds the friends in the party. Call it once while the snapshot is being built, before it is published.
	 *
	 * @param party the friends the Battle page lists
	 * @return this snapshot
	 */
	WindowSnapshot withFriends(FriendsState party)
	{
		friends = party;
		return this;
	}

	/** @return the friends in the party, none outside a party */
	FriendsState friends()
	{
		return friends;
	}

	/** @return the version; a still layer painted for another version is painted again */
	long version()
	{
		return version;
	}

	/** @return what the panel shows, or null before the save is read */
	PanelState state()
	{
		return state;
	}

	/** @return whether the save has been read */
	boolean loaded()
	{
		return loaded;
	}

	/** @return whether the Storage tab can be chosen */
	boolean storageEnabled()
	{
		return storageEnabled;
	}

	/** @return whether a new player has no creature yet and is offered starters */
	boolean firstRun()
	{
		return firstRun;
	}

	/** @return how many creatures are on the team, 0 before the save is read */
	int teamSize()
	{
		return state == null ? 0 : state.team().size();
	}

	/** @return how many creatures are in storage, 0 before the save is read */
	int storedCount()
	{
		return state == null ? 0 : state.storage().size();
	}

	/** @return how many of today's tasks are done */
	int doneTasks()
	{
		return doneTasks;
	}

	/**
	 * Tells whether something waits for the player, which the toggle shows as a pulsing dot.
	 *
	 * @param seenDoneTasks how many done tasks the player had seen when the window was last open
	 * @return true when a creature has a move to decide on, an egg is ready, a new player has starters to choose from,
	 *         or a task was completed since the player last looked
	 */
	boolean waiting(int seenDoneTasks)
	{
		return pendingMove || readyEgg || firstRun || doneTasks > seenDoneTasks;
	}
}
