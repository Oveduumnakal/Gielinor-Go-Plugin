/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** What happened when the player threw a jar at a wild creature. */
enum CatchOutcome
{
	/** The creature was caught and added to the team or to storage. */
	CAUGHT,
	/** The creature broke out and is still wild. */
	BROKE_FREE,
	/** The creature broke out and ran away. */
	FLED,
	/** The player has no jars, so nothing was thrown. */
	NO_JARS,
	/** The team and storage are both full, so nothing was thrown. */
	FULL
}
