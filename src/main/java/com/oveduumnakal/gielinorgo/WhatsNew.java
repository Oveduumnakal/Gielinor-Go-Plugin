/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** Decides when the panel's change log button reads What's New. Pure, so it is easy to test. */
final class WhatsNew
{
	private WhatsNew()
	{
	}

	/**
	 * Tells whether the change log button should read What's New and be highlighted.
	 *
	 * @param lastSeen the version the player last opened the change log for, or null or empty on a first install
	 * @param current the plugin's current version
	 * @param dismissed whether the player already opened the change log for this version
	 * @return true only when an older version was seen before and the player has not opened the change log since
	 */
	static boolean shouldShow(String lastSeen, String current, boolean dismissed)
	{
		if (lastSeen == null || lastSeen.isEmpty() || current == null)
			return false;

		return !lastSeen.equals(current) && !dismissed;
	}

	/**
	 * Tells whether a first install should record the current version without showing What's New.
	 *
	 * @param lastSeen the stored version, or null or empty when none was stored
	 * @param current the plugin's current version
	 * @return true when nothing is stored yet and there is a current version to record
	 */
	static boolean recordSilently(String lastSeen, String current)
	{
		return (lastSeen == null || lastSeen.isEmpty()) && current != null;
	}
}
