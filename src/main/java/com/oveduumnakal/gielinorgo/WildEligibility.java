/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Decides whether a monster in the world may turn wild, and what level the wild creature is measured by. Pure rules
 * with no game types, so they are tested without a client.
 *
 * <p>An ordinary monster needs a combat level of at least 1. A Hunter creature has none, so it is accepted by NPC id:
 * many NPCs with no combat level share a name with a species (pets, decor), and a name alone must never turn one wild.
 */
final class WildEligibility
{
	private WildEligibility()
	{
	}

	/**
	 * Tells whether a monster may turn wild.
	 *
	 * @param species the species the monster's name belongs to, or null when it has none
	 * @param npcId the monster's NPC id
	 * @param npcCombatLevel the monster's combat level, 0 when the game gives it none
	 * @return false for no species or a quest-only one; for a Hunter species true only for one of its own NPC ids;
	 *     for any other species true when the monster has a combat level of at least 1
	 */
	static boolean eligible(Species species, int npcId, int npcCombatLevel)
	{
		if (species == null || species.questOnly())
			return false;

		if (species.hunter())
			return species.hasNpcId(npcId);

		return npcCombatLevel >= 1;
	}

	/**
	 * The level the wild creature's catch odds and own level are worked out from.
	 *
	 * @param species the species of the monster
	 * @param npcCombatLevel the monster's combat level
	 * @return the Hunter level of a Hunter species, otherwise the monster's combat level
	 */
	static int combatLevel(Species species, int npcCombatLevel)
	{
		return species != null && species.hunter() ? species.level() : npcCombatLevel;
	}
}
