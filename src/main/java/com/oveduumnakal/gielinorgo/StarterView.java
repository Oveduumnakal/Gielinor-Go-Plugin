/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** One starter creature as the Team tab offers it: the creature and a line about what it grows into. Immutable. */
final class StarterView
{
	private final CreatureView creature;
	private final String growsInto;

	StarterView(CreatureView creature, String growsInto)
	{
		this.creature = creature;
		this.growsInto = growsInto;
	}

	/** @return the creature, as a card would show it */
	CreatureView creature()
	{
		return creature;
	}

	/** @return what it grows into, such as {@code Lesser demon (20), Greater demon (40)} */
	String growsInto()
	{
		return growsInto;
	}
}
