/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** What the save remembers about the rival: which starter the rival took and how many rival battles are won. */
final class RivalProgress
{
	/** The species key of the rival's starter, fixed the first time the rival is needed. */
	String starter;

	/** How many of the rival's five battles the player has won, 0 to {@link RivalRules#BATTLES}. */
	int won;

	/** Used by Gson. */
	RivalProgress()
	{
	}

	RivalProgress(String starter)
	{
		this.starter = starter;
	}
}
