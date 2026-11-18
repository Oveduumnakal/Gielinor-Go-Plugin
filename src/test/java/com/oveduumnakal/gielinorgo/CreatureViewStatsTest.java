/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Checks that a view carries the real stat numbers through every copy, and that the game computes them. */
public class CreatureViewStatsTest
{
	private static CreatureView base()
	{
		return new CreatureView("Goblin", 10, 0.5, 1.0, Rarity.COMMON, CombatType.CRUSH, false, null);
	}

	@Test
	public void aViewBuiltWithoutThemHasNone()
	{
		assertNull(base().stats());
	}

	@Test
	public void theNumbersAreHealthAttackDefenceSpeed()
	{
		assertArrayEquals(new int[] {84, 31, 22, 40}, base().withStats(84, 31, 22, 40).stats());
	}

	@Test
	public void theNumbersSurviveEveryCopy()
	{
		CreatureView with = base().withStats(84, 31, 22, 40);

		assertArrayEquals(new int[] {84, 31, 22, 40}, with.withGender(Gender.MALE).stats());
		assertArrayEquals(new int[] {84, 31, 22, 40}, with.withDetails("A goblin.", null).stats());
		assertArrayEquals(new int[] {84, 31, 22, 40}, with.withSpecies(null).stats());
		assertArrayEquals(new int[] {84, 31, 22, 40}, with.withFainted(true).stats());
		assertArrayEquals(new int[] {84, 31, 22, 40}, with.withCaught(9L).stats());
	}

	@Test
	public void theOtherExtrasSurviveWithStats()
	{
		CreatureView copy = base().withGender(Gender.FEMALE).withCaught(7L).withDetails("Examine.", "Grows.")
			.withStats(1, 2, 3, 4);

		assertEquals(Gender.FEMALE, copy.gender());
		assertEquals(7L, copy.caughtAtMs());
		assertEquals("Examine.", copy.examine());
		assertEquals("Grows.", copy.growsInto());
	}

	@Test
	public void theCallerCannotChangeTheNumbersAfterwards()
	{
		CreatureView with = base().withStats(1, 2, 3, 4);
		with.stats()[0] = 99;

		assertEquals(1, with.stats()[0]);
	}

	@Test
	public void theGameFillsThemInFromTheCombatantTheBattleUses()
	{
		Species cub = new Species("Cub", 5, CombatType.STAB);
		GoGame game = new GoGame(new SpeciesTable(Arrays.asList(cub)), new Random(1), () -> 1L, notice ->
		{
		});
		Creature creature = new Creature("cub", 6, 4_242L);
		SaveData save = SaveData.fresh();
		save.storage.add(creature);
		game.load(save);
		Combatant expected = new Combatant(cub, creature.level(), 1.0, creature.rolls());

		List<CreatureView> stored = game.snapshot().storage();

		assertArrayEquals(new int[] {expected.maxHp(), expected.attack(), expected.defence(), expected.speed()},
			stored.get(0).stats());
	}

	@Test
	public void aSpeciesTheGameDoesNotKnowHasNoNumbers()
	{
		GoGame game = new GoGame(new SpeciesTable(Arrays.asList(new Species("Cub", 5, CombatType.STAB))),
			new Random(1), () -> 1L, notice ->
			{
			});
		SaveData save = SaveData.fresh();
		save.storage.add(new Creature("gone", 6, 1L));
		game.load(save);

		List<CreatureView> stored = game.snapshot().storage();

		assertNull(stored.get(0).stats());
	}
}
