/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** The four stats a creature's rolls and temperament can change. */
enum Stat
{
	HEALTH("Health"),
	ATTACK("Attack"),
	DEFENCE("Defence"),
	SPEED("Speed");

	private final String label;

	Stat(String label)
	{
		this.label = label;
	}

	/** @return the name shown to the player */
	String label()
	{
		return label;
	}
}
