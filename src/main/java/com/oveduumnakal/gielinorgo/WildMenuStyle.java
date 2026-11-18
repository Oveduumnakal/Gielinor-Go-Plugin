/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Which menu a right click on a wild creature opens; the "Wild creature menu" setting. Holds no RuneLite types.
 */
public enum WildMenuStyle
{
	/** The game's own right-click menu, with Battle and Catch added to it. The default. */
	GAME("Game"),

	/** A menu painted in the window's style: Battle, Catch and its list of jars. Shift-right-click gets the game's. */
	PAINTED("Painted");

	private final String label;

	WildMenuStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
