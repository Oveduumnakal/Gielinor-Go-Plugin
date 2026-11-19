/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the pinned party box shows and when it shows: one {@link RowModel} for each creature on the team, in team order,
 * so the box and the Party page agree on the name, the level and the health of every creature. It has no RuneLite
 * types.
 */
final class PartyBoxModel
{
	/** The most creatures the box lists. */
	static final int MAX_ROWS = SaveData.TEAM_SIZE;

	private PartyBoxModel()
	{
	}

	/**
	 * Works out the rows of the box.
	 *
	 * @param team the team, lead first; null counts as no team
	 * @return one row for each creature, at most {@link #MAX_ROWS}
	 */
	static List<RowModel> rows(List<CreatureView> team)
	{
		if (team == null)
			return Collections.emptyList();

		List<RowModel> rows = new ArrayList<>();
		for (int i = 0; i < Math.min(MAX_ROWS, team.size()); i++)
			rows.add(RowModel.of(team.get(i), i));

		return Collections.unmodifiableList(rows);
	}

	/**
	 * Decides whether the box is drawn at all. Real combat does not hide it, only a Go battle does.
	 *
	 * @param enabled the "Pin Party to UI" setting
	 * @param battleOn whether a Go battle is on
	 * @param loaded whether the save is loaded and live
	 * @param creatures how many creatures there are to show
	 * @return true when the box shows
	 */
	static boolean visible(boolean enabled, boolean battleOn, boolean loaded, int creatures)
	{
		return enabled && !battleOn && loaded && creatures > 0;
	}
}
