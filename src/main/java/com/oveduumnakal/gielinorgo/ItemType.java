/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The healing items. Salves restore health, the Revive draught lifts a creature that is too tired to battle, and the
 * Antidote cures a status effect in battle. Items are found, never bought, and cannot be traded.
 */
enum ItemType
{
	SMALL_SALVE("Small salve", 0.25, false, false, "HERBLORE", "COOKING"),
	LARGE_SALVE("Large salve", 0.60, false, false),
	REVIVE("Revive draught", 0.0, true, false),
	ANTIDOTE("Antidote", 0.0, false, true, "HERBLORE");

	private final String label;
	private final double heal;
	private final boolean revives;
	private final boolean cures;
	private final List<String> skills;

	ItemType(String label, double heal, boolean revives, boolean cures, String... skills)
	{
		this.label = label;
		this.heal = heal;
		this.revives = revives;
		this.cures = cures;
		this.skills = Collections.unmodifiableList(Arrays.asList(skills));
	}

	/** @return the name shown to the player */
	String label()
	{
		return label;
	}

	/** @return the share of full health a salve restores, from 0 to 1; 0 for an item that does not heal by an amount */
	double heal()
	{
		return heal;
	}

	/** @return whether this item brings back a creature that is too tired or has fainted */
	boolean revives()
	{
		return revives;
	}

	/** @return whether this item cures poison, venom and burn; it has an effect in battle only */
	boolean cures()
	{
		return cures;
	}

	/** @return the skills whose XP drops can turn this item up, as {@code Skill} enum names; empty when none do */
	List<String> skills()
	{
		return skills;
	}

	/**
	 * Finds the items a skill's XP drops can turn up.
	 *
	 * @param skill the skill's enum name, in any letter case
	 * @return the items in enum order; empty when training that skill finds none
	 */
	static List<ItemType> forSkill(String skill)
	{
		List<ItemType> found = new ArrayList<>();
		if (skill == null)
			return found;

		String wanted = skill.toUpperCase(Locale.ROOT);
		for (ItemType type : values())
		{
			if (type.skills.contains(wanted))
				found.add(type);
		}

		return found;
	}
}
