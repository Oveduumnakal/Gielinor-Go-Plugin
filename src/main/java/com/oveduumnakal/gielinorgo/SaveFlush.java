/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Writes a game to the account it was loaded from. The game, the persistence and the account are passed in, never
 * read from the plugin, so a save that is queued or that runs while the account changes still writes the right game
 * to the right place.
 */
final class SaveFlush
{
	private SaveFlush()
	{
	}

	/**
	 * Writes the game when it has unsaved progress.
	 *
	 * @param game the game to write; nothing happens when null
	 * @param persistence where the save goes
	 * @param profileKey the config key of the account the game was loaded from, or null for the current account
	 * @param unsaved whether the caller already knows of unsaved progress
	 * @return whether a save was written
	 */
	static boolean write(GoGame game, GoPersistence persistence, String profileKey, boolean unsaved)
	{
		if (game == null || persistence == null)
			return false;

		boolean pending = unsaved | game.takeDirty();
		if (!pending)
			return false;

		persistence.save(game.data(), profileKey);
		return true;
	}
}
