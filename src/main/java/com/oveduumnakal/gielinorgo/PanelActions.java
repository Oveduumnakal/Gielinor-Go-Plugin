/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * What the player can do from the side panel. The panel calls these on the UI thread; the plugin carries each one
 * over to the game thread and then sends the panel a fresh {@link PanelState}.
 */
interface PanelActions
{
	/**
	 * Makes a team member the lead.
	 *
	 * @param teamIndex position in {@link PanelState#team()}
	 */
	void setLead(int teamIndex);

	/**
	 * Moves a team member into storage.
	 *
	 * @param teamIndex position in {@link PanelState#team()}
	 */
	void moveToStorage(int teamIndex);

	/**
	 * Moves a stored creature onto the team, when the team has room.
	 *
	 * @param storageIndex position in {@link PanelState#storage()}
	 */
	void moveToTeam(int storageIndex);

	/**
	 * Releases a stored creature for good.
	 *
	 * @param storageIndex position in {@link PanelState#storage()}
	 */
	void release(int storageIndex);

	/**
	 * Challenges a City Champion. It only starts a battle while the player stands in that city.
	 *
	 * @param championId the champion's {@link ChampionView#id()}
	 */
	void challenge(String championId);

	/** Starts the Elite challenge and fights its first battle. It only starts at the Champions' Guild. */
	void eliteChallenge();

	/** Fights the next battle of the Elite run that is open. */
	void eliteContinue();

	/** Gives up the Elite run that is open. */
	void eliteForfeit();

	/**
	 * Challenges a friend in the player's RuneLite party to a battle between the two teams.
	 *
	 * @param memberId the friend's {@link FriendView#memberId()}
	 */
	void challengeFriend(long memberId);

	/**
	 * Offers a friend one of the player's creatures or eggs, for one of theirs or as a gift. Both picks are checked
	 * again on the game thread; an offer whose picks no longer match is not sent.
	 *
	 * @param memberId the friend's {@link FriendView#memberId()}
	 * @param give what the player picked to give
	 * @param wantIndex the place in {@link FriendView#tradeable()} of what to ask for, or -1 for nothing
	 * @param wantLabel the label shown at that place, or null for nothing
	 */
	void offerTrade(long memberId, TradePick give, int wantIndex, String wantLabel);

	/**
	 * Answers the trade a friend is offering.
	 *
	 * @param memberId the friend's {@link FriendView#memberId()}
	 * @param accept true to take the trade, false to turn it down
	 */
	void answerTrade(long memberId, boolean accept);

	/**
	 * Gives a team member or stored creature a nickname. The pick is checked again on the game thread, and a
	 * pick that no longer matches changes nothing. The nickname stays on this computer.
	 *
	 * @param pick the creature, as the panel showed it
	 * @param name the typed name; an empty name goes back to the species name
	 */
	void rename(TradePick pick, String name);

	/**
	 * Uses a healing item on a team member or stored creature outside battle. The pick is checked again on the game
	 * thread, and a pick that no longer matches uses nothing.
	 *
	 * @param pick the creature, as the panel showed it
	 * @param type the item to use
	 */
	void useItem(TradePick pick, ItemType type);

	/**
	 * Chooses the jar thrown by the Catch option.
	 *
	 * @param type the jar type
	 */
	void selectJar(JarType type);

	/**
	 * Makes the choice of a battle that waits for one: attack, switch, throw a jar or run. It is checked again on the
	 * game thread, and a choice made when none is open changes nothing.
	 *
	 * @param choice what the player chose
	 */
	void battleChoice(BattleChoice choice);
}
