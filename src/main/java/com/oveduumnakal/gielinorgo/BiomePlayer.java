/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays one background loop at a time. Painting asks for the picture of this moment and never waits: loading and
 * decoding happen on the executor, one frame ahead, and until the first frame is ready there is no picture. A loop
 * nobody has asked about for a while is let go ({@link #sweep(long)}), so nothing is decoded while the page that shows
 * it is closed. Holds no RuneLite types.
 */
final class BiomePlayer
{
	/** How long without being asked before the loop is let go, in milliseconds. */
	static final long IDLE_MS = 2_000;

	private static final Logger log = LoggerFactory.getLogger(BiomePlayer.class);

	/** Reads one bundled loop. */
	interface Loader
	{
		/**
		 * @param biome the loop's folder name
		 * @return the loop
		 * @throws IOException when it cannot be read
		 */
		BiomeLoop load(String biome) throws IOException;
	}

	/** What is being played. A new one replaces it when the biome changes, so late work is simply dropped. */
	private static final class Playing
	{
		private final String biome;
		private volatile BiomeLoop loop;
		private volatile BufferedImage picture;
		private volatile int index = -1;
		private volatile boolean busy;

		private Playing(String biome)
		{
			this.biome = biome;
		}
	}

	private final Executor executor;
	private final Loader loader;
	private final Set<String> broken = ConcurrentHashMap.newKeySet();
	private volatile Playing playing;
	private volatile long askedAt;

	/**
	 * Creates a player.
	 *
	 * @param executor where files are read and frames decoded; not the game thread
	 * @param loader reads a loop
	 */
	BiomePlayer(Executor executor, Loader loader)
	{
		this.executor = executor;
		this.loader = loader;
	}

	/**
	 * Creates a player of the bundled loops.
	 *
	 * @param executor where the work happens
	 * @param gson the Gson to read manifests with
	 * @return the player
	 */
	static BiomePlayer bundled(Executor executor, Gson gson)
	{
		return new BiomePlayer(executor, biome -> BiomeLoop.load(gson, biome));
	}

	/**
	 * Gives the picture of a biome for this moment.
	 *
	 * @param biome the loop's folder name
	 * @param nowMs the time in milliseconds
	 * @param moving whether the loop runs; when not, its first frame is held
	 * @return the picture, or null while it is not ready or when the loop cannot be read
	 */
	BufferedImage frame(String biome, long nowMs, boolean moving)
	{
		askedAt = nowMs;
		if (broken.contains(biome))
			return null;

		Playing current = playing;
		if (current == null || !current.biome.equals(biome))
		{
			current = new Playing(biome);
			playing = current;
		}

		BiomeLoop loop = current.loop;
		int wanted = loop == null || !moving ? 0 : loop.frameAt(nowMs);
		if (!current.busy && current.index != wanted)
		{
			current.busy = true;
			Playing asked = current;
			executor.execute(() -> decode(asked, wanted));
		}

		return current.picture;
	}

	private void decode(Playing asked, int index)
	{
		try
		{
			if (playing != asked)
				return;

			if (asked.loop == null)
				asked.loop = loader.load(asked.biome);

			asked.picture = asked.loop.decode(Math.min(index, asked.loop.frames() - 1));
			asked.index = index;
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Gielinor Go: could not play the {} background", asked.biome, e);
			broken.add(asked.biome);
		}
		finally
		{
			asked.busy = false;
		}
	}

	/**
	 * Lets the loop go when nobody has asked for it lately.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void sweep(long nowMs)
	{
		if (nowMs - askedAt > IDLE_MS)
			playing = null;
	}

	/** Lets everything go, for when the plugin stops. */
	void clear()
	{
		playing = null;
		broken.clear();
	}

	/** @return whether a loop is held */
	boolean holding()
	{
		return playing != null;
	}
}
