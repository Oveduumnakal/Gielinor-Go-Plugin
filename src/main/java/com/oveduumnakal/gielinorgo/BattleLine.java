/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** One line of a battle, and whether it is about the player's creature or the wild one. */
final class BattleLine
{
	/** What a line is, so a battle can be acted out in step with the text. */
	enum Kind
	{
		/** The player's creature steps up and attacks. */
		OPENING,

		/** The player's creature hit the other one. */
		LEAD_HIT,

		/** The other creature hit the player's creature. */
		FOE_HIT,

		/** The result: the loser goes down. */
		CLOSING,

		/** Any other line, which is only text. */
		OTHER
	}

	private final String text;
	private final Tone tone;
	private final boolean overPlayer;
	private final Kind kind;
	private final boolean special;
	private final CombatType style;
	private final boolean icy;
	private final boolean miss;

	BattleLine(String text, Tone tone, boolean overPlayer)
	{
		this(text, tone, overPlayer, Kind.OTHER);
	}

	BattleLine(String text, Tone tone, boolean overPlayer, Kind kind)
	{
		this(text, tone, overPlayer, kind, false);
	}

	/**
	 * Builds a line.
	 *
	 * @param text the line
	 * @param tone the mood, from the player's side
	 * @param overPlayer whether the line is about the player's creature
	 * @param kind what the line is
	 * @param special whether the attack was the attacker's second kind, such as a dragon's fire breath
	 */
	BattleLine(String text, Tone tone, boolean overPlayer, Kind kind, boolean special)
	{
		this(text, tone, overPlayer, kind, special, null, false);
	}

	/**
	 * Builds a line for a hit, with the style it was made in.
	 *
	 * @param text the line
	 * @param tone the mood, from the player's side
	 * @param overPlayer whether the line is about the player's creature
	 * @param kind what the line is
	 * @param special whether the attack was the attacker's second kind, such as a dragon's fire breath
	 * @param style the style of the attack, or null for a line that is not a hit
	 * @param icy whether a breath is icy, as a wyvern's is; false for anything that is not a breath
	 */
	BattleLine(String text, Tone tone, boolean overPlayer, Kind kind, boolean special, CombatType style,
		boolean icy)
	{
		this(text, tone, overPlayer, kind, special, style, icy, false);
	}

	/**
	 * Builds a line for an attack that may have missed.
	 *
	 * @param text the line
	 * @param tone the mood, from the player's side
	 * @param overPlayer whether the line is about the player's creature
	 * @param kind what the line is
	 * @param special whether the attack used the attacker's second animation, such as a breath
	 * @param style the style of the attack, or null for a line that is not an attack
	 * @param icy whether a breath is icy, as a wyvern's is; false for anything that is not a breath
	 * @param miss whether the attack missed, so the target does not react
	 */
	BattleLine(String text, Tone tone, boolean overPlayer, Kind kind, boolean special, CombatType style,
		boolean icy, boolean miss)
	{
		this.miss = miss;
		this.style = style;
		this.icy = icy;
		this.special = special;
		this.text = text;
		this.tone = tone;
		this.overPlayer = overPlayer;
		this.kind = kind;
	}

	/** @return true when the attack missed: it is still acted out, but the target does not react */
	boolean miss()
	{
		return miss;
	}

	/** @return true when the attack was a move of the second slot (a breath), so it plays its second animation */
	boolean special()
	{
		return special;
	}

	/** @return the style the attack was made in, or null for a line that is not a hit */
	CombatType style()
	{
		return style;
	}

	/** @return true when the attack was an icy breath, so it is shown with the icy projectile */
	boolean icy()
	{
		return icy;
	}

	/** @return what the line is, {@link Kind#OTHER} when it is only text */
	Kind kind()
	{
		return kind;
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
