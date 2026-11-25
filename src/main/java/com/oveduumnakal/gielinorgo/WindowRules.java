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
	 * Tells whether a Gielinor Go battle is on: a battle that waits for choices, or a scene that is still being acted
	 * out, such as a battle that plays itself.
	 *
	 * @param directed whether a battle that the player steers is on
	 * @param wildScene whether a battle against a wild creature is being acted out
	 * @param arenaScene whether a team battle is being acted out
	 * @return true when any of them is on
	 */
	static boolean battleOn(boolean directed, boolean wildScene, boolean arenaScene)
	{
		return directed || wildScene || arenaScene;
	}

	/**
	 * Tells whether the result of a battle that played itself is to be written now.
	 *
	 * @param age game ticks the result has waited
	 * @param limit game ticks after which it is written whatever the scene does
	 * @param sceneOn whether a battle scene is still acted out
	 * @param linesShown whether every line of the battle has been shown
	 * @param wildDown whether the wild creature lies knocked out on the scene, which is when it can be caught
	 * @return true once the wild creature is down, the scene and the lines are done, or the wait has passed the limit
	 */
	static boolean deferredDue(int age, int limit, boolean sceneOn, boolean linesShown, boolean wildDown)
	{
		return wildDown || age > limit || !sceneOn && linesShown;
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
