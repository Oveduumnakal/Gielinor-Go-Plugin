/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** One line of a battle, and whether it is about the player's creature or the wild one. */
final class BattleLine
{
	private final String text;
	private final Tone tone;
	private final boolean overPlayer;

	BattleLine(String text, Tone tone, boolean overPlayer)
	{
		this.text = text;
		this.tone = tone;
		this.overPlayer = overPlayer;
	}

	/** @return the line */
	String text()
	{
		return text;
	}

	/** @return the mood, from the player's side */
	Tone tone()
	{
		return tone;
	}

	/** @return true when the line is about the player's creature, false when it is about the wild one */
	boolean overPlayer()
	{
		return overPlayer;
	}
}
