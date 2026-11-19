/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Locale;
import java.util.Set;

/**
 * Names of the random event NPCs, which walk up to the player and interact with them without being a fight. They do
 * not count as combat. Names shared with real monsters (zombie, shade, rock golem, river troll, tree spirit) are left
 * out on purpose so real combat is never excluded. The rule is pure and has no RuneLite types.
 */
final class RandomEvents
{
	private static final Set<String> NAMES = Set.of(
		"genie", "drunken dwarf", "dr jekyll", "evil bob", "sandwich lady", "mysterious old man", "beekeeper",
		"certer", "count check", "quiz master", "capt' arnav", "freaky forester", "mr. mordaut", "leo",
		"rick turpentine", "niles", "miles", "giles", "flippa", "postie pete", "prison pete", "molly",
		"sergeant damien", "frog", "frog prince", "frog princess", "pillory guard", "strange plant",
		"lost sheep farmer", "bob");

	private RandomEvents()
	{
	}

	/**
	 * @param name an NPC's name, possibly null, with or without colour tags
	 * @return whether the NPC is a random event NPC
	 */
	static boolean isRandomEvent(String name)
	{
		if (name == null)
			return false;

		String plain = name.replaceAll("<[^>]*>", "")
			.trim()
			.toLowerCase(Locale.ROOT);
		return NAMES.contains(plain);
	}
}
