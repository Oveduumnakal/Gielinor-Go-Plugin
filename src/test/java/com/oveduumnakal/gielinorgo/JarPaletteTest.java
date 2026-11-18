/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks that every jar has its own bubble colour, apart from the Battle and Info bubbles. */
public class JarPaletteTest
{
	@Test
	public void everyJarHasAColourOfItsOwn()
	{
		Set<Integer> seen = new HashSet<>();
		for (JarType jar : JarType.values())
		{
			Color colour = JarPalette.of(jar);
			assertNotNull(jar.name(), colour);
			assertTrue(jar.name() + " repeats a colour", seen.add(colour.getRGB()));
		}

		assertEquals(JarType.values().length, seen.size());
	}

	@Test
	public void noJarUsesABattleOrInfoBase()
	{
		for (JarType jar : JarType.values())
		{
			assertNotEquals(jar.name(), JarPalette.BATTLE_BASE, JarPalette.of(jar));
			assertNotEquals(jar.name(), JarPalette.INFO_BASE, JarPalette.of(jar));
		}

		assertNotEquals(JarPalette.BATTLE_BASE, JarPalette.INFO_BASE);
	}

	@Test
	public void noJarGivesAGrey()
	{
		Color grey = JarPalette.of(null);

		assertNotNull(grey);
		assertEquals(grey.getRed(), grey.getGreen());
		assertEquals(grey.getGreen(), grey.getBlue());
	}
}
