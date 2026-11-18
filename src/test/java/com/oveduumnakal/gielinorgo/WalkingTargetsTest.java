/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pins how many tiles a jar, an egg and the walking tasks ask for (issue 206 doubled them). */
public class WalkingTargetsTest
{
	@Test
	public void aBronzeJarTakes300Tiles()
	{
		assertEquals(300, GoGame.TILES_PER_JAR);
	}

	@Test
	public void eggsTake1000To4000Tiles()
	{
		assertEquals(1000, Rarity.COMMON.eggTiles());
		assertEquals(1600, Rarity.UNCOMMON.eggTiles());
		assertEquals(2400, Rarity.RARE.eggTiles());
		assertEquals(3200, Rarity.EPIC.eggTiles());
		assertEquals(4000, Rarity.LEGENDARY.eggTiles());
	}

	@Test
	public void aHigherTierEggNeedsMoreTiles()
	{
		Rarity[] tiers = Rarity.values();
		for (int i = 1; i < tiers.length; i++)
			assertTrue(tiers[i].name(), tiers[i].eggTiles() > tiers[i - 1].eggTiles());
	}

	@Test
	public void theWalkingTasksAsk1200AndTenThousandTiles()
	{
		assertEquals(1200, TaskKind.WALK.dailyGoal());
		assertEquals(10_000, TaskKind.WALK.weeklyGoal());
	}
}
