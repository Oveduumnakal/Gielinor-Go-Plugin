/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * When the painted window may open and when it must close. Real combat, a Gielinor Go battle and an unread save all
 * keep it shut; the Storage tab follows the team-swap rule. Pure rules over plain values, so they are tested like
 * {@link WildHiding}. Holds no RuneLite types.
 */
final class WindowRules
{
	private WindowRules()
	{
	}

	/**
	 * Decides whether the window may be open.
	 *
	 * @param realCombat whether the player is fighting a real monster, or did within the last few seconds
	 * @param goBattle whether a Gielinor Go battle is on
	 * @param loaded whether the save has been read
	 * @return true when the window may open
	 */
	static boolean mayOpen(boolean realCombat, boolean goBattle, boolean loaded)
	{
		return loaded && !realCombat && !goBattle;
	}

	/**
	 * Gives the words for why the window cannot open now.
	 *
	 * @param realCombat whether the player is in combat
	 * @param goBattle whether a Gielinor Go battle is on
	 * @param loaded whether the save has been read
	 * @return the notice to show when the player tries to open it, or null when it may open; loading is named before
	 *         a battle and a battle before combat
	 */
	static NoticeMessage blockedNotice(boolean realCombat, boolean goBattle, boolean loaded)
	{
		if (!loaded)
			return NoticeMessage.LOADING;

		if (goBattle)
			return NoticeMessage.BLOCKED_BATTLE;

		return realCombat ? NoticeMessage.BLOCKED_COMBAT : null;
	}

	/**
	 * Gives the words for why an open window has to close.
	 *
	 * @param realCombat whether the player is in combat
	 * @param goBattle whether a Gielinor Go battle is on
	 * @return the notice to show, or null when the window may stay; a battle is named before combat
	 */
	static NoticeMessage closedNotice(boolean realCombat, boolean goBattle)
	{
		if (goBattle)
			return NoticeMessage.CLOSED_BATTLE;

		return realCombat ? NoticeMessage.CLOSED_COMBAT : null;
	}

	/**
	 * Decides whether the Storage tab can be chosen: the same rule as moving a creature between the team and
	 * storage.
	 *
	 * @param inCity whether the player is in a City Champion's area or a team-change town
	 * @param nearBank whether a bank is near or open
	 * @return true when Storage can be chosen
	 */
	static boolean storageEnabled(boolean inCity, boolean nearBank)
	{
		return TeamSwapRule.allowed(inCity, nearBank, false);
	}
}
