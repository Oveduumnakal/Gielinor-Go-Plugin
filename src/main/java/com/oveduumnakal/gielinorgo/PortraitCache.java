/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * What the painted window knows of creature pictures. Painting asks for a picture every frame and must never wait, so
 * this holds the pictures that have arrived and asks the {@link PortraitSource} once for each that has not; the source
 * calls back whenever the picture is ready, on whatever thread it likes, and the next frame draws it. A picture that
 * arrives late fades in over {@link WindowTimes#PORTRAIT_FADE}; one that was already drawn is there at once, without a
 * fade. Species without a model never get a picture, so the window draws its backdrop alone. Holds no RuneLite types.
 */
final class PortraitCache
{
	private final PortraitSource source;
	private final LongSupplier clock;
	private final Map<String, BufferedImage> images = new ConcurrentHashMap<>();
	private final Map<String, Long> arrived = new ConcurrentHashMap<>();
	private final Set<String> asked = ConcurrentHashMap.newKeySet();
	private volatile Thread requester;

	/**
	 * Creates the cache.
	 *
	 * @param source where pictures come from
	 * @param clock the time in milliseconds, to time the fade
	 */
	PortraitCache(PortraitSource source, LongSupplier clock)
	{
		this.source = source;
		this.clock = clock;
	}

	/**
	 * Gives the picture of a creature, asking for it the first time.
	 *
	 * @param species the species name
	 * @param npcId the NPC id the creature looks like, or 0
	 * @return the picture, or null while it has not arrived
	 */
	BufferedImage image(String species, int npcId)
	{
		String key = PortraitSource.cacheKey(species, npcId);
		BufferedImage have = images.get(key);
		if (have != null)
			return have;

		if (asked.add(key))
		{
			requester = Thread.currentThread();
			try
			{
				source.request(species, npcId, picture ->
				{
					images.put(key, picture);
					arrived.put(key, Thread.currentThread() == requester ? -1L : clock.getAsLong());
				});
			}
			finally
			{
				requester = null;
			}
		}

		return images.get(key);
	}

	/**
	 * Gives how much of a picture shows while it fades in.
	 *
	 * @param species the species name
	 * @param npcId the NPC id the creature looks like, or 0
	 * @param nowMs the time in milliseconds
	 * @param mode the motion mode; a mode without fades shows the picture at once
	 * @return 0 before it arrived, 1 once it has faded in or when it needed no fade
	 */
	double alpha(String species, int npcId, long nowMs, MotionMode mode)
	{
		String key = PortraitSource.cacheKey(species, npcId);
		if (!images.containsKey(key))
			return 0.0;

		Long at = arrived.get(key);
		long ms = mode.fadeMs(WindowTimes.PORTRAIT_FADE);
		if (at == null || at < 0 || ms <= 0)
			return 1.0;

		return Math.max(0.0, Math.min(1.0, (double) (nowMs - at) / ms));
	}

	/** Forgets every picture, for when the plugin stops. */
	void clear()
	{
		images.clear();
		arrived.clear();
		asked.clear();
	}
}
