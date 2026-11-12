/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Where the side panel gets creature pictures from. A seam, so the panel never touches the game client. */
interface PortraitSource
{
	/**
	 * Asks for a species' picture. Called on the UI thread.
	 *
	 * @param speciesName the species name, as shown on a creature card
	 * @param onReady receives the picture on the UI thread: straight away when it is already drawn, later when
	 *     it has to be drawn first, and never when the species has no model to draw
	 */
	void request(String speciesName, Consumer<BufferedImage> onReady);

	/**
	 * Asks for the picture of a creature of a species that was caught as one NPC variant of it. A source that does
	 * not tell variants apart draws the species' picture.
	 *
	 * @param speciesName the species name, as shown on a creature card
	 * @param npcId the NPC id to draw it as, or 0 for the species' first
	 * @param onReady receives the picture on the UI thread, see {@link #request(String, Consumer)}
	 */
	default void request(String speciesName, int npcId, Consumer<BufferedImage> onReady)
	{
		request(speciesName, onReady);
	}

	/**
	 * Names a picture, so two variants of one species never share one.
	 *
	 * @param speciesName the species name or key
	 * @param npcId the NPC id the picture is drawn from, or 0 for the species' first
	 * @return the key the picture is cached under
	 */
	static String cacheKey(String speciesName, int npcId)
	{
		String key = Species.keyFor(speciesName);
		return npcId > 0 ? key + "#" + npcId : key;
	}
}
