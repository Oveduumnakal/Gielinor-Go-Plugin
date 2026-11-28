/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Remembers the OSRS XP the client reported this session, and decides when it may be written to the save. Kept free
 * of RuneLite types so the ordering rules are tested: nothing is stamped before the login was reconciled against the
 * save, and never on a world that keeps its own XP.
 */
final class XpLedger
{
	private static final String OVERALL = "OVERALL";

	private final Map<String, Integer> login = new HashMap<>();
	private final Map<String, Integer> current = new HashMap<>();
	private boolean reconciled;
	private boolean worldOk;

	/**
	 * Records a reading of a skill's XP. The first reading of the session is kept as the login value.
	 *
	 * @param skill the {@code Skill} name
	 * @param xp the skill's OSRS XP
	 */
	void read(String skill, int xp)
	{
		if (OVERALL.equals(skill))
			return;

		login.putIfAbsent(skill, xp);
		current.put(skill, xp);
	}

	/**
	 * Takes the readings of a plugin that was switched on while logged in: both the login and the latest values.
	 *
	 * @param xp the current XP by skill name
	 */
	void seed(Map<String, Integer> xp)
	{
		for (Map.Entry<String, Integer> entry : xp.entrySet())
		{
			if (!OVERALL.equals(entry.getKey()) && entry.getValue() != null)
			{
				login.putIfAbsent(entry.getKey(), entry.getValue());
				current.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Tells whether the login can be compared with the save: it has not been yet, and every skill the save knows has
	 * reported. A skill that reports after the comparison is stamped straight into the save, so its XP from time away
	 * would be lost.
	 *
	 * @param saved the skills in the save's XP snapshot
	 * @return whether to reconcile now
	 */
	boolean reconcileDue(Set<String> saved)
	{
		if (reconciled || login.isEmpty())
			return false;

		for (String skill : saved)
		{
			if (!OVERALL.equals(skill) && !login.containsKey(skill))
				return false;
		}

		return true;
	}

	/** @return a copy of each skill's first reading this session */
	Map<String, Integer> login()
	{
		return new HashMap<>(login);
	}

	/** Records that the login was compared with the save. */
	void markReconciled()
	{
		reconciled = true;
	}

	/** Records that a different save was loaded, so the login must be compared with it first. */
	void markUnreconciled()
	{
		reconciled = false;
	}

	/**
	 * Records whether the current world's XP is the account's own.
	 *
	 * @param ok false on seasonal, deadman and similar worlds
	 */
	void worldOk(boolean ok)
	{
		worldOk = ok;
	}

	/** @return whether the last world seen counts */
	boolean worldOk()
	{
		return worldOk;
	}

	/**
	 * The XP to write into the save's snapshot.
	 *
	 * @return a copy of the latest readings, or null when the login is not reconciled yet or the world does not count
	 */
	Map<String, Integer> stamp()
	{
		return reconciled && worldOk ? new HashMap<>(current) : null;
	}

	/** Forgets everything, for the login screen and for plugin stop. */
	void reset()
	{
		login.clear();
		current.clear();
		reconciled = false;
		worldOk = false;
	}
}
