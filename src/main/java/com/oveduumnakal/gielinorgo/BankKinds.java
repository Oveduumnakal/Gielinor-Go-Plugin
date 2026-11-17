/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Decides from a name and a list of right-click actions whether a banker, booth or chest opens a bank. Holds no client
 * types, so it is tested without a client.
 */
final class BankKinds
{
	private static final Pattern BANK_WORD = Pattern.compile("\\bbank\\b");

	private static final String[] CHEST_ACTIONS = {"Use", "Open", "Collect"};

	private BankKinds()
	{
	}

	/**
	 * Tells whether a monster opens a bank: a banker has a {@code Bank} action.
	 *
	 * @param actions the monster's right-click actions, which may hold nulls
	 * @return true when the monster offers a bank
	 */
	static boolean npcOffersBank(String[] actions)
	{
		return hasAction(actions, "Bank");
	}

	/**
	 * Tells whether an object opens a bank. A booth has a {@code Bank} action. A bank chest often has only
	 * {@code Use}, so an object whose name has the word bank counts too when it can be used, opened or collected
	 * from. A deposit box does not count, and neither does an object that only has bank in a longer word.
	 *
	 * @param name the object's name, which may be null
	 * @param actions the object's right-click actions, which may hold nulls
	 * @return true when the object offers a bank
	 */
	static boolean objectOffersBank(String name, String[] actions)
	{
		if (hasAction(actions, "Bank"))
			return true;

		if (name == null)
			return false;

		String lower = name.toLowerCase(Locale.ROOT);
		if (!BANK_WORD.matcher(lower).find() || lower.contains("deposit"))
			return false;

		for (String action : CHEST_ACTIONS)
		{
			if (hasAction(actions, action))
				return true;
		}

		return false;
	}

	private static boolean hasAction(String[] actions, String wanted)
	{
		if (actions == null)
			return false;

		for (String action : actions)
		{
			if (action != null && wanted.equalsIgnoreCase(action.trim()))
				return true;
		}

		return false;
	}
}
