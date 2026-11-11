/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * One line the game wants to tell the player, and whose head it belongs over: a wild creature's, for news about
 * that creature, or the player's own, for things the player found, earned or cannot do.
 */
final class Notice
{
	/** The {@link #npcIndex()} of a notice that belongs over the player's head. */
	static final int PLAYER = -1;

	private final String text;
	private final Tone tone;
	private final int npcIndex;

	private Notice(String text, Tone tone, int npcIndex)
	{
		this.text = text;
		this.tone = tone;
		this.npcIndex = npcIndex;
	}

	/**
	 * Makes a notice shown over the player's head.
	 *
	 * @param text the line
	 * @param tone its mood
	 * @return the notice
	 */
	static Notice player(String text, Tone tone)
	{
		return new Notice(text, tone, PLAYER);
	}

	/**
	 * Makes a notice shown over a monster's head.
	 *
	 * @param npcIndex the game's index for the monster
	 * @param text the line
	 * @param tone its mood
	 * @return the notice
	 */
	static Notice creature(int npcIndex, String text, Tone tone)
	{
		return new Notice(text, tone, npcIndex);
	}

	/** @return the line */
	String text()
	{
		return text;
	}

	/** @return the mood, which decides the colour */
	Tone tone()
	{
		return tone;
	}

	/** @return the game's index for the monster this floats over, or {@link #PLAYER} */
	int npcIndex()
	{
		return npcIndex;
	}

	/** @return whether this floats over the player's head */
	boolean overPlayer()
	{
		return npcIndex == PLAYER;
	}
}
