/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Where the team may be changed: moving a creature between the team and storage is only allowed inside a city (any
 * City Champion's area, beaten or not, or a town from {@link TeamTowns}), near a bank, or with the bank open.
 */
final class TeamSwapRule
{
	/** What the player is told when a swap is refused. */
	static final String REFUSAL = "Go to a city or a bank to change your team";

	private TeamSwapRule()
	{
	}

	/**
	 * Decides whether the team may be changed here.
	 *
	 * @param inCity whether the player is inside a City Champion's area or a team-change town
	 * @param nearBank whether a banker, bank booth or bank chest is within {@link BankSpots#NEAR_TILES} tiles
	 * @param bankOpen whether the bank interface is open
	 * @return true when the team may be changed
	 */
	static boolean allowed(boolean inCity, boolean nearBank, boolean bankOpen)
	{
		return inCity || nearBank || bankOpen;
	}
}
