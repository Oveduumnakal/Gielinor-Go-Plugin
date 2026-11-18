/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The numbers and checks behind fainting: where a fainted creature may be revived for free, what health it comes back
 * with, and when a passing healer stands the first one up because the player has nothing living and no way to revive.
 * No game types, so tests are plain.
 */
final class ReviveRules
{
	/** The share of full health a bank revive gives, the same as a Revive draught, see {@link ItemRules#REVIVE_TO}. */
	static final double BANK_TO = ItemRules.REVIVE_TO;

	/** How long everything the player owns must have been fainted before a passing healer helps, in milliseconds. */
	static final long RESCUE_MS = 10 * 60 * 1000L;

	/** The share of full health the passing healer gives. */
	static final double RESCUE_TO = GoGame.MIN_BATTLE_HP;

	private ReviveRules()
	{
	}

	/**
	 * Decides whether a creature may be revived for free here. It is the same rule as changing the team,
	 * {@link TeamSwapRule}: a city or team town, near a bank, or with the bank open.
	 *
	 * @param inCity whether the player is inside a City Champion's area or a team-change town
	 * @param nearBank whether a banker, bank booth or bank chest is within {@link BankSpots#NEAR_TILES} tiles
	 * @param bankOpen whether the bank interface is open
	 * @return true when the place allows a free revive
	 */
	static boolean mayReviveHere(boolean inCity, boolean nearBank, boolean bankOpen)
	{
		return TeamSwapRule.allowed(inCity, nearBank, bankOpen);
	}

	/**
	 * Words why a free revive is refused here.
	 *
	 * @param name the creature's name
	 * @return the line to show
	 */
	static String placeRefusal(String name)
	{
		return "Go to a bank or a city to revive " + name;
	}

	/**
	 * Tells whether the passing healer stands a creature up.
	 *
	 * @param nowMs the current wall-clock time in milliseconds
	 * @param latestFaintMs when the newest of the fainted creatures fainted
	 * @param anyAlive whether the player owns a creature, in the team or in storage, that has not fainted
	 * @param hasCreatures whether the player owns any creature at all
	 * @return true when the player owns creatures, every one of them has fainted, and the newest fainted at least
	 *     {@link #RESCUE_MS} ago
	 */
	static boolean rescueDue(long nowMs, long latestFaintMs, boolean anyAlive, boolean hasCreatures)
	{
		return hasCreatures && !anyAlive && nowMs - latestFaintMs >= RESCUE_MS;
	}
}
