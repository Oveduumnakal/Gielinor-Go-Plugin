/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * One line the game wants to tell the player, and what it floats over: a wild creature's body, for news about
 * that creature, or the player's own head, for things the player found, earned or cannot do.
 */
final class Notice
{
	/** The {@link #wildId()} of a notice that belongs over the player's head. */
	static final int PLAYER = -1;

	private final String text;
	private final Tone tone;
	private final int wildId;
	private final Anchor anchor;
	private final JarType jar;
	private final ItemType item;

	/** Where a notice floats: over the player's head, over a wild creature's body, or over a body on a stage. */
	enum Anchor
	{
		/** Over the player's head. */
		HEAD,

		/**
		 * Over the body of the wild creature {@link #wildId()} names; falls back to the player's head while that
		 * body has no place on screen.
		 */
		WILD,

		/**
		 * Over the wild creature's body in a battle on stage, or the opposing creature of a team battle on stage;
		 * falls back to the player's head when no body is drawn.
		 */
		STAGE_FOE,

		/** Over the lead creature's body while a battle is on stage; falls back to the player. */
		STAGE_LEAD
	}

	private Notice(String text, Tone tone, int wildId, Anchor anchor, JarType jar)
	{
		this(text, tone, wildId, anchor, jar, null);
	}

	private Notice(String text, Tone tone, int wildId, Anchor anchor, JarType jar, ItemType item)
	{
		this.text = text;
		this.tone = tone;
		this.wildId = wildId;
		this.anchor = anchor;
		this.jar = jar;
		this.item = item;
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
	 * Makes a notice about a healing item the player found, shown over the player's head with the item's picture
	 * beside it.
	 *
	 * @param text the line
	 * @param tone its mood
	 * @param item the item the line is about
	 * @return the notice
	 */
	static Notice playerItem(String text, Tone tone, ItemType item)
	{
		return new Notice(text, tone, PLAYER, Anchor.HEAD, null, item);
	}

	/**
	 * Makes a notice shown over a wild creature's body.
	 *
	 * @param wildId the wild creature's id
	 * @param text the line
	 * @param tone its mood
	 * @return the notice
	 */
	static Notice creature(int wildId, String text, Tone tone)
	{
		return new Notice(text, tone, wildId, Anchor.WILD, null);
	}

	/**
	 * Makes a notice about a wild creature whose battle is on stage. It floats over the creature's body, as
	 * {@link #creature(int, String, Tone)} would.
	 *
	 * @param wildId the wild creature's id
	 * @param text the line
	 * @param tone its mood
	 * @return the notice
	 */
	static Notice stageFoe(int wildId, String text, Tone tone)
	{
		return new Notice(text, tone, wildId, Anchor.STAGE_FOE, null);
	}

	/**
	 * Makes a notice about the opposing creature of a team battle acted out on screen, which is no monster in the
	 * game. It floats over that creature; once no stage shows it, it floats over the player's head.
	 *
	 * @param text the line
	 * @param tone its mood
	 * @return the notice
	 */
	static Notice stageFoe(String text, Tone tone)
	{
		return new Notice(text, tone, PLAYER, Anchor.STAGE_FOE, null);
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

	/** @return the healing item this line is about, whose picture is drawn beside it, or null */
	ItemType item()
	{
		return item;
	}

	/** @return the id of the wild creature this floats over, or {@link #PLAYER} */
	int wildId()
	{
		return wildId;
	}

	/** @return whether this is about no wild creature, so it floats over the player's head */
	boolean overPlayer()
	{
		return wildId == PLAYER;
	}

	/** @return where this floats: over the player, over a wild body, or over a body on a battle stage */
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
		return anchor == other.anchor && wildId == other.wildId;
	}
}
