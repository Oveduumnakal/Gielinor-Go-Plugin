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
	 * Chooses the jar thrown by the Catch option.
	 *
	 * @param type the jar type
	 */
	void selectJar(JarType type);
}
