/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The jars a creature is caught in. Tier jars (Bronze to Dragon) raise the catch chance on everything and drop from
 * battles. Specialty jars are found while training a skill, and are only better than Bronze on creatures with the
 * jar's {@link Trait}.
 */
enum JarType
{
	BRONZE("Bronze", 1.0, 1),
	IRON("Iron", 1.5, 10),
	RUNE("Rune", 2.0, 25),
	DRAGON("Dragon", 3.0, 50),
	CAVE("Cave", 2.5, Trait.CAVE, "MINING"),
	SWIFT("Swift", 2.5, Trait.FAST, "AGILITY", "THIEVING", "HUNTER"),
	BLESSED("Blessed", 2.5, Trait.UNHOLY, "PRAYER"),
	SLAYER("Slayer", 2.5, Trait.SLAYER, "SLAYER"),
	VENOM("Venom", 2.5, Trait.POISONOUS, "HERBLORE", "FARMING"),
	EMBER("Ember", 2.5, Trait.FIERY, "FIREMAKING", "SMITHING", "COOKING"),
	GIANT("Giant", 2.5, Trait.LARGE, "WOODCUTTING", "CONSTRUCTION"),
	TIDE("Tide", 2.5, Trait.AQUATIC, "FISHING", "SAILING"),
	WARRIOR("Warrior", 1.75, Trait.MELEE, "ATTACK", "STRENGTH", "DEFENCE"),
	ARCHER("Archer", 2.5, Trait.RANGED, "RANGED", "FLETCHING"),
	MYSTIC("Mystic", 2.5, Trait.MAGIC, "MAGIC", "RUNECRAFT", "CRAFTING");

	private final String label;
	private final double bonus;
	private final double traitBonus;
	private final int trainerLevel;
	private final Trait trait;
	private final List<String> skills;

	JarType(String label, double bonus, int trainerLevel)
	{
		this.label = label;
		this.bonus = bonus;
		this.traitBonus = bonus;
		this.trainerLevel = trainerLevel;
		this.trait = null;
		this.skills = Collections.emptyList();
	}

	JarType(String label, double traitBonus, Trait trait, String... skills)
	{
		this.label = label;
		this.bonus = 1.0;
		this.traitBonus = traitBonus;
		this.trainerLevel = 0;
		this.trait = trait;
		this.skills = Collections.unmodifiableList(Arrays.asList(skills));
	}

	/** @return the name shown to the player, without the word "jar" */
	String label()
	{
		return label;
	}

	/** @return the multiplier this jar applies to the base catch chance on a creature without its trait */
	double bonus()
	{
		return bonus;
	}

	/** @return the multiplier on a creature that has this jar's trait; the same as {@link #bonus()} for a tier jar */
	double traitBonus()
	{
		return traitBonus;
	}

	/** @return the trainer level from which battles can drop this jar; 0 for a specialty jar, which never drops */
	int trainerLevel()
	{
		return trainerLevel;
	}

	/** @return whether this jar is found by training a skill and tied to a trait */
	boolean specialty()
	{
		return trait != null;
	}

	/** @return the trait this jar is good at, or null for a tier jar */
	Trait trait()
	{
		return trait;
	}

	/** @return the skills that can turn up this jar, as {@code Skill} enum names; empty for a tier jar */
	List<String> skills()
	{
		return skills;
	}

	/**
	 * Multiplier this jar applies to the base catch chance on one creature.
	 *
	 * @param traits the creature's traits
	 * @return {@link #traitBonus()} when the jar's trait is among them, otherwise {@link #bonus()}
	 */
	double bonusAgainst(Set<Trait> traits)
	{
		return trait != null && traits.contains(trait) ? traitBonus : bonus;
	}

	/**
	 * Finds the specialty jar a skill can turn up.
	 *
	 * @param skill the skill's enum name, in any letter case
	 * @return the jar, or null when training that skill finds none
	 */
	static JarType forSkill(String skill)
	{
		if (skill == null)
			return null;

		String wanted = skill.toUpperCase(Locale.ROOT);
		for (JarType type : values())
		{
			if (type.skills.contains(wanted))
				return type;
		}

		return null;
	}
}
