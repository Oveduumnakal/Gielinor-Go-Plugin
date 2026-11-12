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
	private final Anchor anchor;
	private final JarType jar;

	/** Where a notice floats: over a head as usual, or over a body that is acted out on a battle stage. */
	enum Anchor
	{
		/** Over the monster {@link #npcIndex()} names, or over the player when that is {@link #PLAYER}. */
		HEAD,

		/** Over the stand-in of the wild creature on stage; falls back to the monster or the player. */
		STAGE_FOE,

		/** Over the lead creature's body while a battle is on stage; falls back to the player. */
		STAGE_LEAD
	}

	private Notice(String text, Tone tone, int npcIndex, Anchor anchor, JarType jar)
	{
		this.text = text;
		this.tone = tone;
		this.npcIndex = npcIndex;
		this.anchor = anchor;
		this.jar = jar;
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
		return new Notice(text, tone, PLAYER, Anchor.HEAD, null);
	}

	/**
	 * Makes a notice about a jar the player found, shown over the player's head with the jar's picture beside it.
	 *
	 * @param text the line
	 * @param tone its mood
	 * @param jar the jar the line is about
	 * @return the notice
	 */
	static Notice player(String text, Tone tone, JarType jar)
	{
		return new Notice(text, tone, PLAYER, Anchor.HEAD, jar);
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
		return new Notice(text, tone, npcIndex, Anchor.HEAD, null);
	}

	/**
	 * Makes a notice about a wild creature whose battle is on stage. It floats over the creature's stand-in; once
	 * no stage shows it, it floats over the monster, as {@link #creature(int, String, Tone)} would.
	 *
	 * @param npcIndex the game's index for the monster, kept for the fallback
	 * @param text the line
	 * @param tone its mood
	 * @return the notice
	 */
	static Notice stageFoe(int npcIndex, String text, Tone tone)
	{
		return new Notice(text, tone, npcIndex, Anchor.STAGE_FOE, null);
	}

	/**
	 * Makes a notice about the lead creature while a battle is on stage. It floats over the lead's body; once no
	 * stage shows it, it floats over the player's head.
	 *
	 * @param text the line
	 * @param tone its mood
	 * @return the notice
	 */
	static Notice stageLead(String text, Tone tone)
	{
		return new Notice(text, tone, PLAYER, Anchor.STAGE_LEAD, null);
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

	/** @return the jar this line is about, whose picture is drawn beside it, or null */
	JarType jar()
	{
		return jar;
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

	/** @return where this floats: over a head, or over a body on a battle stage */
	Anchor anchor()
	{
		return anchor;
	}

	/**
	 * @param other another notice
	 * @return whether both float over the same place, so they share one stack of lines
	 */
	boolean sameAnchor(Notice other)
	{
		return anchor == other.anchor && npcIndex == other.npcIndex;
	}
}
