/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Locale;

/**
 * One move a creature can use in a battle: a style, a power, an accuracy and sometimes an effect. The moves come from
 * the bundled {@code moves.json} (see {@link MoveTable}); a creature that has no moves of its own fights with
 * {@link #basic(CombatType)}, which is exactly the one attack every creature had before moves existed.
 */
final class Move
{
	/** The slot of a move that plays the creature's plain attack animation. */
	static final String ATTACK = "ATTACK";

	/** The slot of a move that plays the creature's second attack animation, such as a breath. */
	static final String SECOND = "SECOND";

	/** The least accuracy a move can have. */
	static final double MIN_ACCURACY = 0.5;

	/** What a move can do besides damage, rolled after a hit. It is read by the status effects of issue 22. */
	static final class Effect
	{
		private String status;
		private double chance;

		/** Used by Gson. */
		Effect()
		{
		}

		/** @return the name of the status the move can cause */
		String status()
		{
			return status;
		}

		/** @return the chance, from 0 to 1, that a hit causes it */
		double chance()
		{
			return Math.max(0.0, Math.min(1.0, chance));
		}
	}

	private String id;
	private String name;
	private CombatType style;
	private double power;
	private double accuracy;
	private String slot;
	private boolean icy;
	private Effect effect;

	/** Used by Gson. */
	Move()
	{
	}

	/**
	 * Makes a move, for tests and for building moves in code.
	 *
	 * @param id the id, as saved and sent to friends
	 * @param name the name the player sees
	 * @param style the style, which decides the combat triangle
	 * @param power the damage factor, 1.0 being a plain attack
	 * @param accuracy the chance to hit, from 0.5 to 1
	 */
	Move(String id, String name, CombatType style, double power, double accuracy)
	{
		this.id = id;
		this.name = name;
		this.style = style;
		this.power = power;
		this.accuracy = accuracy;
	}

	/**
	 * The plain attack of a creature that has no moves: power 1, accuracy 1, no name. A battle fought with it reads
	 * and rolls exactly as battles did before moves existed.
	 *
	 * @param style the creature's own style
	 * @return the move
	 */
	static Move basic(CombatType style)
	{
		return new Move(null, null, style, 1.0, 1.0);
	}

	/**
	 * Gives the move a slot, for tests and for building moves in code.
	 *
	 * @param newSlot {@link #ATTACK} or {@link #SECOND}
	 * @param isIcy whether a breath is shown with the icy projectile
	 * @return this move
	 */
	Move inSlot(String newSlot, boolean isIcy)
	{
		this.slot = newSlot;
		this.icy = isIcy;
		return this;
	}

	/** @return the id, or null for the basic attack */
	String id()
	{
		return id;
	}

	/** @return the name the player sees, or null for the basic attack */
	String name()
	{
		return name;
	}

	/** @return whether this is the unnamed basic attack, whose battle lines keep the old wording */
	boolean plain()
	{
		return id == null;
	}

	/** @return the style, melee when the data leaves it out */
	CombatType style()
	{
		return style == null ? CombatType.MELEE : style;
	}

	/** @return the damage factor, 1.0 being a plain attack; at least 0.1 */
	double power()
	{
		return power <= 0 ? 1.0 : Math.max(0.1, power);
	}

	/** @return the chance to hit, from {@link #MIN_ACCURACY} to 1; 1 when the data leaves it out */
	double accuracy()
	{
		return accuracy <= 0 ? 1.0 : Math.max(MIN_ACCURACY, Math.min(1.0, accuracy));
	}

	/** @return whether the move plays the second attack animation of the creature, such as a breath */
	boolean second()
	{
		return SECOND.equals(slot);
	}

	/** @return whether a breath is shown with the icy projectile */
	boolean icy()
	{
		return icy;
	}

	/** @return what the move can do besides damage, or null for nothing */
	Effect effect()
	{
		return effect;
	}

	/**
	 * Describes the move for a tooltip.
	 *
	 * @return the style, power and accuracy, and the effect when there is one
	 */
	String describe()
	{
		String text = style().label() + ", power " + trim(power()) + ", accuracy " + Math.round(accuracy() * 100) + "%";
		if (effect != null && effect.status() != null)
			text += ", " + Math.round(effect.chance() * 100) + "% " + effect.status().toLowerCase(Locale.ROOT);

		return text;
	}

	private static String trim(double value)
	{
		return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
	}
}
