/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The texts of the pop-up notice, in one place so the wording is the same on screen and in the chat box. Holds no
 * RuneLite types.
 */
enum NoticeMessage
{
	/** The window closed because real combat started. */
	CLOSED_COMBAT("Gielinor Go closed: you are in combat."),

	/** The window closed because a Gielinor Go battle started. */
	CLOSED_BATTLE("Gielinor Go closed: a battle started."),

	/** The player tried to open the window during real combat. */
	BLOCKED_COMBAT("Can't open during combat."),

	/** The player tried to open the window during a Gielinor Go battle. */
	BLOCKED_BATTLE("Can't open during a battle."),

	/** The player tried to open the window before the save was read. */
	LOADING("Still loading your save."),

	/** A new player has no creature yet and is told where to click. */
	FIRST_RUN("Click the jar to choose your first creature."),

	/** The player clicked the Storage tab away from a bank and a city. */
	STORAGE_LOCKED("Go to a city or a bank to open Storage.");

	private final String text;

	NoticeMessage(String text)
	{
		this.text = text;
	}

	/** @return the words to show */
	String text()
	{
		return text;
	}
}
