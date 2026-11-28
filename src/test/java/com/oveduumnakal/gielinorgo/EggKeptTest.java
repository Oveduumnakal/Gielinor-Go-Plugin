/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks that a ready egg is kept when no species can hatch from it, as when the species file failed to load. */
public class EggKeptTest
{
	@Test
	public void anEggWithNothingToHatchIsNotUsedUp()
	{
		List<String> told = new ArrayList<>();
		GoGame game = new GoGame(new SpeciesTable(Collections.emptyList()), new Random(5), () -> 0L,
			notice -> told.add(notice.text()));
		Egg egg = new Egg(Rarity.COMMON);
		egg.tilesLeft = 1;
		game.data().eggs.add(egg);

		game.addSteps(5);

		assertEquals(Collections.singletonList(egg), game.data().eggs);
		assertEquals(0, egg.tilesLeft);
		assertTrue(told.isEmpty());
	}
}
