/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** The rules for a creature's nickname. Nicknames stay on the player's computer and are never sent to anyone. */
final class Nickname
{
	/** Most characters a nickname can have. */
	static final int MAX_LENGTH = 12;

	private Nickname()
	{
	}

	/**
	 * Cleans up a typed nickname: trims it and collapses runs of spaces into one.
	 *
	 * @param raw what the player typed, or null
	 * @return the nickname, or null when it is empty, longer than {@link #MAX_LENGTH} or holds anything but
	 *         letters, numbers, spaces and hyphens
	 */
	static String clean(String raw)
	{
		if (raw == null)
			return null;

		String text = raw.trim().replaceAll(" {2,}", " ");
		if (text.isEmpty() || text.length() > MAX_LENGTH)
			return null;

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			boolean letter = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
			boolean digit = c >= '0' && c <= '9';
			if (!letter && !digit && c != ' ' && c != '-')
				return null;
		}

		return text;
	}
}
