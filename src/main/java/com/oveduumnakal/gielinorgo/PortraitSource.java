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
}
