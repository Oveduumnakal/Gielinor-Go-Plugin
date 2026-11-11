/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

/** The mood of a line of floating text, which decides its colour. */
enum Tone
{
	/** Plain news, such as a wild creature appearing. */
	INFO(Color.WHITE),
	/** Something went the player's way: a catch, a hit landed, a level gained. */
	GOOD(new Color(0x5CFF7A)),
	/** Something went against the player: a creature broke free, a hit taken, nothing to throw. */
	BAD(new Color(0xFF6B5C)),
	/** The player found or earned an item. */
	FIND(new Color(0xFFD23F));

	private final Color color;

	Tone(Color color)
	{
		this.color = color;
	}

	/** @return the text colour for this tone */
	Color color()
	{
		return color;
	}
}
