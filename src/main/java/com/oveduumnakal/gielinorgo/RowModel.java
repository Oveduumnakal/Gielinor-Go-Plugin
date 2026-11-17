/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Everything one creature row of the painted window shows, worked out once from a {@link CreatureView}: the name, the
 * state, the badges, the stripe and frame, the tooltip. The Party row and the Summary header both read it, so each
 * badge is defined in one place. Every badge the side panel's {@code CreatureCard} shows is carried: the lead, a move
 * waiting to be learned, the species waiting to evolve and the rating; the painted window adds Resting, Fainted,
 * the rarity stripe and the gilded frame. Colour is never the only signal: each state also has its own word, and the
 * gender its own icon. Immutable, and holds no RuneLite types, so it is tested without a window.
 */
final class RowModel
{
	/** The share of health under which a creature is resting. */
	static final double RESTING_BELOW = 0.25;

	/** How much of its colour a resting row keeps. */
	static final double RESTING_DIM = 0.70;

	/** How much of its colour a fainted row keeps. */
	static final double FAINTED_DIM = 0.55;

	/** The longest a name is drawn at full size before it shrinks one step, as a share of the room it has. */
	static final double SHRINK_TO = 0.85;

	/** What shape a creature is in, which decides how its row is dressed. */
	enum State
	{
		/** An ordinary row. */
		NORMAL,

		/** Below a quarter of its health: the row is dimmed and the portrait washed out. */
		RESTING,

		/** Out of the fight: a greyed row and an empty health bar. Dormant until fainting exists. */
		FAINTED
	}

	/** A small tag or mark on a row. */
	enum Badge
	{
		/** The lead creature. */
		LEAD("Lead"),

		/** A move waits to be learned: a pulsing gold star. */
		NEW_MOVE("New move"),

		/** Under a quarter of its health. */
		RESTING("Resting"),

		/** Out of the fight. */
		FAINTED("Fainted"),

		/** It has the level to evolve but not the condition. */
		WAITING("Waiting"),

		/** A rare gilded creature: a gold frame. */
		GILDED("Gilded");

		private final String label;

		Badge(String label)
		{
			this.label = label;
		}

		/** @return the word on the tag */
		String label()
		{
			return label;
		}
	}

	private final int index;
	private final CreatureView view;
	private final boolean lead;
	private final State state;
	private final List<Badge> badges;

	private RowModel(int index, CreatureView view, boolean lead, State state, List<Badge> badges)
	{
		this.index = index;
		this.view = view;
		this.lead = lead;
		this.state = state;
		this.badges = Collections.unmodifiableList(badges);
	}

	/**
	 * Works out the row of a creature on the team.
	 *
	 * @param view the creature
	 * @param index its place on the team; 0 is the lead
	 * @return the row
	 */
	static RowModel of(CreatureView view, int index)
	{
		boolean lead = index == 0;
		State state = stateOf(view);
		List<Badge> badges = new ArrayList<>();
		if (lead)
			badges.add(Badge.LEAD);

		if (!view.pending().isEmpty())
			badges.add(Badge.NEW_MOVE);

		if (state == State.RESTING)
			badges.add(Badge.RESTING);

		if (state == State.FAINTED)
			badges.add(Badge.FAINTED);

		if (view.evolvesWhen() != null)
			badges.add(Badge.WAITING);

		if (view.gilded())
			badges.add(Badge.GILDED);

		return new RowModel(index, view, lead, state, badges);
	}

	/**
	 * Gives the state of a creature.
	 *
	 * @param view the creature
	 * @return fainted when it is fainted, resting under a quarter of its health, otherwise normal
	 */
	static State stateOf(CreatureView view)
	{
		if (view.fainted())
			return State.FAINTED;

		return view.hpFraction() < RESTING_BELOW ? State.RESTING : State.NORMAL;
	}

	/** @return the creature's place on the team; 0 is the lead */
	int index()
	{
		return index;
	}

	/** @return the creature this row shows */
	CreatureView view()
	{
		return view;
	}

	/** @return whether this is the lead, which has an arrow and an outline */
	boolean lead()
	{
		return lead;
	}

	/** @return how the row is dressed */
	State state()
	{
		return state;
	}

	/** @return the badges in the order they are drawn */
	List<Badge> badges()
	{
		return badges;
	}

	/**
	 * @param badge a badge
	 * @return whether the row has it
	 */
	boolean has(Badge badge)
	{
		return badges.contains(badge);
	}

	/** @return the name the row shows: the nickname, or the species name when there is none */
	String name()
	{
		return view.nickname() == null ? view.name() : view.nickname();
	}

	/** @return the species name, which the Summary shows under a nickname and the tooltip always names */
	String species()
	{
		return view.name();
	}

	/** @return whether the creature has a nickname, so the species is a second line on the Summary */
	boolean nicknamed()
	{
		return view.nickname() != null;
	}

	/** @return {@code Lv 12} */
	String levelText()
	{
		return "Lv " + view.level();
	}

	/** @return the share of health the bar shows: empty for a fainted creature */
	double hp()
	{
		return state == State.FAINTED ? 0.0 : view.hpFraction();
	}

	/** @return the whole number the health bar's text shows, such as {@code 45%} */
	String hpText()
	{
		return Math.round(hp() * 100) + "%";
	}

	/** @return the share of the way to the next level */
	double xp()
	{
		return view.xpProgress();
	}

	/** @return how much of its colour the row keeps, from 0 to 1 */
	double dim()
	{
		switch (state)
		{
			case RESTING:
				return RESTING_DIM;
			case FAINTED:
				return FAINTED_DIM;
			default:
				return 1.0;
		}
	}

	/** @return whether the portrait is drawn washed out */
	boolean desaturated()
	{
		return state != State.NORMAL;
	}

	/** @return whether the row has the gold frame */
	boolean gilded()
	{
		return view.gilded();
	}

	/** @return the species' rarity, whose colour is the stripe on the portrait's edge */
	Rarity rarity()
	{
		return view.rarity();
	}

	/** @return the gender, or null when the view has none, in which case no gender icon is drawn */
	Gender gender()
	{
		return view.gender();
	}

	/** @return the combat style, drawn as the style icon */
	CombatType style()
	{
		return view.type();
	}

	/** @return whether a move waits to be learned */
	boolean newMove()
	{
		return has(Badge.NEW_MOVE);
	}

	/**
	 * Gives the tag words the row can fit, most important first: the state words, then Waiting.
	 *
	 * @return the words of the badges that are tags, in order; the star and the frame are not words
	 */
	List<String> tags()
	{
		List<String> words = new ArrayList<>();
		for (Badge badge : badges)
		{
			if (badge == Badge.LEAD || badge == Badge.RESTING || badge == Badge.FAINTED)
				words.add(badge.label());
			else if (badge == Badge.WAITING)
				words.add(view.evolvesWhen());
		}

		return words;
	}

	private String pendingName()
	{
		Move waiting = view.pending().get(0);
		return waiting.name();
	}

	/**
	 * Words the tooltip of the row: the full name, which is also where a name too long for the row is read in full,
	 * the species, rarity and style, the health and the way to the next level, then the one-line facts of each badge.
	 *
	 * @return the tooltip, with line breaks
	 */
	String tooltip()
	{
		StringBuilder text = new StringBuilder(name());
		if (nicknamed())
			text.append(" (")
				.append(species())
				.append(')');

		text.append('\n').append(view.rarity().label()).append(' ').append(view.type().label().toLowerCase(Locale.ROOT))
			.append(" creature, ").append(levelText());
		text.append('\n').append(state == State.FAINTED ? "Fainted" : "Health " + hpText());
		if (view.evolvesWhen() != null)
			text.append(", ").append(view.evolvesWhen());
		else
			text.append(", ").append(Math.round(xp() * 100)).append("% to level ")
				.append(Math.min(XpRules.MAX_LEVEL, view.level() + 1));

		if (view.gilded())
			text.append("\nGilded");

		if (lead)
			text.append("\nYour lead: fights battles and trains twice as fast");

		if (newMove())
			text.append("\nWants to learn " + pendingName());

		if (state == State.RESTING)
			text.append("\nResting: it is too tired to be at its best");

		text.append("\nClick for details. Right-click for more.");
		return text.toString();
	}
}
