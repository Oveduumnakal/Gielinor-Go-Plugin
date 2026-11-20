/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Gives a blurred picture of the game scene behind a card, for the frosted-glass Info card (#294). With
 * {@link #NONE} the glass shows the scene itself through its tint. Holds no RuneLite types.
 */
interface CardBackdrop
{
	/** No backdrop: the glass shows the scene as it is. */
	CardBackdrop NONE = screenBox -> null;

	/**
	 * Gives the blurred scene behind a part of the screen.
	 *
	 * @param screenBox the card's footprint on the screen (its body and the shadow below it), in canvas pixels
	 * @return a small blurred picture of what is behind it, to be stretched over the card, or null when there is none
	 */
	BufferedImage behind(Rectangle screenBox);
}
