/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules for the list of notices the plugin holds back while a battle is resolved, so they show after the
 * battle's own lines. The list is open from the moment a battle starts until its lines are played back.
 */
final class HeldNotices
{
	private HeldNotices()
	{
	}

	/**
	 * Opens the list for a battle, keeping what was already held so those notices are still announced.
	 *
	 * @param held the list now held, or null when none is open
	 * @return the list to hold from now on, never null
	 */
	static List<Notice> open(List<Notice> held)
	{
		return held == null ? new ArrayList<>() : held;
	}

	/**
	 * Closes the list when the battle is played back.
	 *
	 * @param held the list now held, or null when it was already closed
	 * @return what was held, or an empty list when it was already closed
	 */
	static List<Notice> close(List<Notice> held)
	{
		return held == null ? new ArrayList<>() : held;
	}
}
