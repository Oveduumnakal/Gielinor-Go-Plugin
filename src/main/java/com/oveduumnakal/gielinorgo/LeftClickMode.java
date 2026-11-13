/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * When a plain left click on a wild creature belongs to this plugin. Holds no RuneLite types.
 */
public enum LeftClickMode
{
	/** The wild creature always answers a left click. */
	ALWAYS("Always"),

	/** The wild creature answers a left click except while the player is in combat with a real monster. */
	OUT_OF_COMBAT("Not while in combat"),

	/** The wild creature never answers a left click; its entries are in the right-click menu only. */
	NEVER("Never");

	private final String label;

	LeftClickMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
