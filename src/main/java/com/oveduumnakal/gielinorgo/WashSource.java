/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Gives a glass surface the soft wash of colours from the game scene just outside it (#324). Each surface asks by a
 * name of its own, so every surface keeps its own wash. Holds no RuneLite types.
 */
interface WashSource
{
	/** No wash: every surface is tinted solid. */
	WashSource NONE = (surface, screenBox) -> null;

	/**
	 * Gives the wash of a surface, and records that the surface is on screen.
	 *
	 * @param surface the surface's name, such as {@code window} or {@code card}; one wash is kept per name
	 * @param screenBox the surface's footprint on the screen (its body and its shadow), in canvas pixels
	 * @return a small picture of the scene round the surface, to be stretched over it, or null when there is none yet
	 */
	BufferedImage wash(String surface, Rectangle screenBox);
}
