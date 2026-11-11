/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** How a team battle against a City Champion went: who won, and one line per creature knocked out. */
final class TeamBattleResult
{
	private final boolean won;
	private final List<BattleLine> lines;

	TeamBattleResult(boolean won, List<BattleLine> lines)
	{
		this.won = won;
		this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
	}

	/** @return whether the player's team beat the whole opposing team */
	boolean won()
	{
		return won;
	}

	/** @return the lines of the battle, in order */
	List<BattleLine> lines()
	{
		return lines;
	}
}
