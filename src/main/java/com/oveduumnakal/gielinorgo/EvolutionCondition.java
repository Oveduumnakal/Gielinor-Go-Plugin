/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** A condition an evolution step can ask for on top of its level. Every one can be met by a player alone. */
enum EvolutionCondition
{
	NIGHT("at night")
	{
		@Override
		boolean holds(Conditions now)
		{
			return now.night();
		}
	},
	DAY("by day")
	{
		@Override
		boolean holds(Conditions now)
		{
			return !now.night();
		}
	},
	UNDERGROUND("underground")
	{
		@Override
		boolean holds(Conditions now)
		{
			return now.underground();
		}
	},
	SURFACE("on the surface")
	{
		@Override
		boolean holds(Conditions now)
		{
			return !now.underground();
		}
	};

	private final String phrase;

	EvolutionCondition(String phrase)
	{
		this.phrase = phrase;
	}

	/**
	 * Tells whether the condition is met.
	 *
	 * @param now the world right now
	 * @return true when a creature may evolve
	 */
	abstract boolean holds(Conditions now);

	/** @return the condition as it ends a sentence, such as {@code at night} */
	String phrase()
	{
		return phrase;
	}

	/** @return what a waiting creature's card says, such as {@code Evolves at night} */
	String waitingText()
	{
		return "Evolves " + phrase;
	}

	/**
	 * Finds the condition that covers the cases this one leaves out.
	 *
	 * @return {@code DAY} for {@code NIGHT} and so on
	 */
	EvolutionCondition opposite()
	{
		switch (this)
		{
			case NIGHT:
				return DAY;
			case DAY:
				return NIGHT;
			case UNDERGROUND:
				return SURFACE;
			default:
				return UNDERGROUND;
		}
	}
}
