/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The six tabs of the painted window, in the order they sit in the header. Holds no RuneLite types.
 */
enum WindowTab
{
	/** The team. */
	PARTY("Party", "Your team of six will show here."),

	/** Jars, healing items, eggs and the trainer level. */
	BAG("Bag", "Your jars, items and eggs will show here."),

	/** Stored creatures. Only reachable where the team may be changed. */
	STORAGE("Storage", "Your stored creatures will show here."),

	/** The Creature Log. */
	COLLECTION("Collection", "The Creature Log will show here."),

	/** Daily and weekly tasks and milestones. */
	TASKS("Tasks", "Your tasks and milestones will show here."),

	/** City Champions, the Elite challenge, the Rival and friends. */
	BATTLE("Battle", "Champions, the Elite challenge and friends will show here.");

	private final String label;
	private final String placeholder;

	WindowTab(String label, String placeholder)
	{
		this.label = label;
		this.placeholder = placeholder;
	}

	/** @return the word on the tab */
	String label()
	{
		return label;
	}

	/** @return what the placeholder page says until the real page is built */
	String placeholder()
	{
		return placeholder;
	}

	/** @return whether the tab can only be chosen where the team may be changed */
	boolean gated()
	{
		return this == STORAGE;
	}

	/** @return the id the tab's {@link Hit} has */
	String hitId()
	{
		return "tab:" + name();
	}

	/**
	 * @param hitId a hit id
	 * @return the tab it names, or null when it is not a tab's id
	 */
	static WindowTab fromHitId(String hitId)
	{
		for (WindowTab tab : values())
			if (tab.hitId().equals(hitId))
				return tab;

		return null;
	}
}
