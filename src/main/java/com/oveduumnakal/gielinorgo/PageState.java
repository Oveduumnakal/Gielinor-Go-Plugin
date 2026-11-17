/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Which page of the Party tab is shown and which creature the Summary is on. The tab has two pages: the Party list and
 * the Summary of one creature. Esc, Backspace and the Back button go from Summary to the list (the window itself
 * closes after that, which is the shell's rule); Up and Down on the Summary go to the previous and the next creature,
 * wrapping at the ends; a team that shrinks while the Summary is open (a creature moved to storage) keeps the Summary
 * on a creature that exists or goes back to the list. Plain state, owned by the game thread. Holds no RuneLite types.
 */
final class PageState
{
	/** The pages of the tab. */
	enum Page
	{
		/** The six rows. */
		PARTY,

		/** One creature in detail. */
		SUMMARY
	}

	private Page page = Page.PARTY;
	private int index;
	private int cameFrom = -1;
	private long openedMs;

	/** @return the page shown */
	Page page()
	{
		return page;
	}

	/** @return the creature the Summary shows, as a place on the team; meaningless on the list */
	int index()
	{
		return index;
	}

	/** @return the row the Summary grew out of, or -1 when it did not come from a row */
	int cameFrom()
	{
		return cameFrom;
	}

	/** @return when the Summary was opened, in milliseconds */
	long openedMs()
	{
		return openedMs;
	}

	/** @return how many pages deep the tab is: 0 on the list, 1 on the Summary */
	int depth()
	{
		return page == Page.SUMMARY ? 1 : 0;
	}

	/**
	 * Opens the Summary of a creature.
	 *
	 * @param place the creature's place on the team
	 * @param teamSize how many creatures are on the team
	 * @param nowMs the time in milliseconds
	 * @return whether the Summary opened; false when there is no such creature
	 */
	boolean open(int place, int teamSize, long nowMs)
	{
		if (place < 0 || place >= teamSize)
			return false;

		page = Page.SUMMARY;
		index = place;
		cameFrom = place;
		openedMs = nowMs;
		return true;
	}

	/** Goes back to the list. Nothing happens on the list itself. */
	void back()
	{
		page = Page.PARTY;
		cameFrom = -1;
	}

	/**
	 * Moves the Summary to the previous or the next creature, wrapping around.
	 *
	 * @param delta 1 for the next creature, -1 for the previous
	 * @param teamSize how many creatures are on the team
	 * @param nowMs the time in milliseconds, so the Summary's entrance plays again for the new creature
	 * @return the creature's place now shown; unchanged when the Summary is not open or the team is empty
	 */
	int step(int delta, int teamSize, long nowMs)
	{
		if (page != Page.SUMMARY || teamSize <= 0)
			return index;

		index = Math.floorMod(index + delta, teamSize);
		cameFrom = -1;
		openedMs = nowMs;
		return index;
	}

	/**
	 * Keeps the page valid after the team changed.
	 *
	 * @param teamSize how many creatures are on the team now
	 */
	void clamp(int teamSize)
	{
		if (page != Page.SUMMARY)
			return;

		if (teamSize <= 0)
			back();
		else if (index >= teamSize)
			index = teamSize - 1;
	}
}
