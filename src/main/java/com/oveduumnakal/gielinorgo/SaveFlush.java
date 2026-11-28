/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

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
	 * Hands work to the thread that owns the game and tells when it has run, so a caller on another thread never reads
	 * the game while that thread changes it.
	 *
	 * @param thread queues work on the owning thread
	 * @param work the work to run there
	 * @return done once the work has run, failed when it threw
	 */
	static Future<Void> handOver(Consumer<Runnable> thread, Runnable work)
	{
		CompletableFuture<Void> done = new CompletableFuture<>();
		thread.accept(() ->
		{
			try
			{
				work.run();
				done.complete(null);
			}
			catch (RuntimeException e)
			{
				done.completeExceptionally(e);
			}
		});
		return done;
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
