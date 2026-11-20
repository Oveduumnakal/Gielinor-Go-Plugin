/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks that a view carries the time its creature was caught through every copy, and that it counts as identity. */
public class CreatureViewCaughtTest
{
	private static CreatureView base()
	{
		return new CreatureView("Goblin", 10, 0.5, 1.0, Rarity.COMMON, CombatType.CRUSH, false, null);
	}

	@Test
	public void aViewBuiltWithoutItHasZero()
	{
		assertEquals(0L, base().caughtAtMs());
	}

	@Test
	public void theTimeSurvivesEveryCopy()
	{
		CreatureView caught = base().withCaught(12_345L);

		assertEquals(12_345L, caught.withGender(Gender.MALE).caughtAtMs());
		assertEquals(12_345L, caught.withDetails("A goblin.", null).caughtAtMs());
		assertEquals(12_345L, caught.withSpecies(null).caughtAtMs());
		assertEquals(12_345L, caught.withFainted(true).caughtAtMs());
		assertEquals(12_345L, base().withGender(Gender.FEMALE).withCaught(12_345L)
			.withDetails("A goblin.", null).caughtAtMs());
	}

	@Test
	public void copiesKeepTheOtherExtras()
	{
		CreatureView copy = base().withGender(Gender.FEMALE)
			.withDetails("Examine.", "Grows.")
			.withCaught(7L);

		assertEquals(Gender.FEMALE, copy.gender());
		assertEquals("Examine.", copy.examine());
		assertEquals("Grows.", copy.growsInto());
	}

	@Test
	public void theTimeIsPartOfTheCreatureIdentity()
	{
		CreatureView five = base().withCaught(5L);
		CreatureView six = base().withCaught(6L);

		assertEquals(five, base().withCaught(5L));
		assertFalse(five.equals(six));
		assertFalse(five.sameCreature(six));
		assertTrue(five.sameCreature(base().withCaught(5L)));
	}

	@Test
	public void trainingAndRenamingDoNotMakeAnotherCreature()
	{
		CreatureView before = base().withCaught(5L);
		CreatureView after = new CreatureView("Goblin", 13, 0.9, 0.3, Rarity.COMMON, CombatType.CRUSH, false, "Gobbo")
			.withCaught(5L);

		assertFalse(before.equals(after));
		assertTrue(before.sameCreature(after));
		assertTrue(after.sameCreature(before));
	}

	@Test
	public void theGameFillsItInFromTheSavedCreature()
	{
		GoGame game = new GoGame(new SpeciesTable(Arrays.asList(new Species("Cub", 5, CombatType.STAB))),
			new Random(1), () -> 1L, notice ->
			{
			});
		Creature cub = new Creature("cub", 6, 4_242L);
		SaveData save = SaveData.fresh();
		save.storage.add(cub);
		game.load(save);

		List<CreatureView> stored = game.snapshot().storage();

		assertEquals(4_242L, stored.get(0).caughtAtMs());
	}
}
