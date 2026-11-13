/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import net.runelite.api.gameval.SpotanimID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the four projectile looks and which attack gets which. */
public class ProjectileLookTest
{
	@Test
	public void eachLookNamesTheSpotAnimationItWasReadFrom()
	{
		assertEquals(SpotanimID.IRON_ARROW_TRAVEL, ProjectileLook.ARROW.spotAnimId());
		assertEquals(SpotanimID.FIRESTRIKE_TRAVEL, ProjectileLook.SPELL.spotAnimId());
		assertEquals(SpotanimID.FIREBREATH_TRAVEL, ProjectileLook.FIRE.spotAnimId());
		assertEquals(SpotanimID.WYVERN_SKELETON_TRAVEL_BREATH, ProjectileLook.ICE.spotAnimId());
	}

	@Test
	public void theValuesAreTheOnesReadFromTheCache()
	{
		assertEquals(3136, ProjectileLook.ARROW.modelId());
		assertFalse(ProjectileLook.ARROW.animated());
		assertEquals(3081, ProjectileLook.SPELL.modelId());
		assertEquals(659, ProjectileLook.SPELL.animationId());
		assertEquals(3154, ProjectileLook.FIRE.modelId());
		assertEquals(373, ProjectileLook.FIRE.animationId());
		assertEquals(10361, ProjectileLook.ICE.modelId());
		assertEquals(2992, ProjectileLook.ICE.animationId());
	}

	@Test
	public void recoloursComeInPairs()
	{
		for (ProjectileLook look : ProjectileLook.values())
		{
			short[] from = look.recolorFrom();
			short[] to = look.recolorTo();
			assertEquals(look.name(), from.length, to.length);
			assertTrue(look.size() > 0);
		}
	}

	@Test
	public void aMeleeAttackHasNoProjectile()
	{
		assertNull(ProjectileLook.of(CombatType.STAB, false, false));
		assertNull(ProjectileLook.of(CombatType.STAB, true, false));
		assertNull(ProjectileLook.of(null, false, false));
	}

	@Test
	public void rangedShootsAnArrowAndMagicCastsABolt()
	{
		assertEquals(ProjectileLook.ARROW, ProjectileLook.of(CombatType.RANGED, false, false));
		assertEquals(ProjectileLook.SPELL, ProjectileLook.of(CombatType.MAGIC, false, false));
	}

	@Test
	public void aBreathIsFireOrIce()
	{
		assertEquals(ProjectileLook.FIRE, ProjectileLook.of(CombatType.MAGIC, true, false));
		assertEquals(ProjectileLook.ICE, ProjectileLook.of(CombatType.MAGIC, true, true));
	}
}
