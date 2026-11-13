/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the five statuses on a fighter, one block each, without a battle around them. */
public class StatusTest
{
	private static final Species PLAIN = new Species("Plain", 20, CombatType.STAB);
	private static final Species SPIDER = new Species("Spider", 20, CombatType.STAB, Trait.POISONOUS);
	private static final Species IMP = new Species("Imp", 20, CombatType.STAB, Trait.FIERY);

	private static Combatant plain()
	{
		return new Combatant(PLAIN, 40, 1.0);
	}

	@Test
	public void poisonTakesASixteenthOfFullHealthEachRoundForFourRounds()
	{
		Combatant one = plain();
		assertTrue(one.afflict(Status.POISON, new Random(1)));
		int each = (int) Math.round(one.maxHp() / 16.0);
		for (int round = 0; round < 4; round++)
		{
			assertEquals(Status.POISON, one.status());
			assertEquals(each, one.endOfRound());
		}

		assertNull("it ends when its rounds are up", one.status());
		assertEquals(0, one.endOfRound());
		assertEquals(one.maxHp() - 4 * each, one.hp(), 0.0);
	}

	@Test
	public void venomGrowsEachRound()
	{
		Combatant one = plain();
		one.afflict(Status.VENOM, new Random(1));
		int[] taken = new int[4];
		for (int round = 0; round < 4; round++)
			taken[round] = one.endOfRound();

		assertEquals((int) Math.round(one.maxHp() * 0.05), taken[0]);
		assertEquals((int) Math.round(one.maxHp() * 0.10), taken[1]);
		assertEquals((int) Math.round(one.maxHp() * 0.15), taken[2]);
		assertEquals((int) Math.round(one.maxHp() * 0.20), taken[3]);
		assertNull(one.status());
	}

	@Test
	public void burnTakesHealthAndCutsMeleeHitsButNotMagic()
	{
		Combatant burned = plain();
		Combatant fresh = plain();
		Combatant target = plain();
		burned.afflict(Status.BURN, new Random(1));
		Move melee = Move.basic(CombatType.STAB);
		Move magic = Move.basic(CombatType.MAGIC);

		int cut = BattleSim.damage(burned, melee, target, new Random(7));
		int normal = BattleSim.damage(fresh, melee, target, new Random(7));
		int spell = BattleSim.damage(burned, magic, target, new Random(7));
		int spellNormal = BattleSim.damage(fresh, magic, target, new Random(7));

		assertTrue("a burned melee hit is softer: " + cut + " against " + normal, cut < normal);
		assertEquals("magic is not cut", spellNormal, spell);
		assertEquals((int) Math.round(burned.maxHp() / 16.0), burned.endOfRound());
	}

	@Test
	public void freezeSkipsOneOrTwoTurnsAndThenEnds()
	{
		boolean one = false;
		boolean two = false;
		for (int seed = 0; seed < 50; seed++)
		{
			Random random = new Random(seed * 7919L + 13);
			Combatant frozen = plain();
			frozen.afflict(Status.FREEZE, random);
			int skipped = 0;
			while (frozen.status() != null)
			{
				assertTrue(frozen.skipsTurn(random));
				skipped++;
			}

			assertTrue("skipped " + skipped, skipped == 1 || skipped == 2);
			assertFalse(frozen.skipsTurn(random));
			one |= skipped == 1;
			two |= skipped == 2;
		}

		assertTrue("both lengths come up", one && two);
	}

	@Test
	public void stunSkipsAboutHalfOfItsTurnsAndEndsAfterThree()
	{
		Random random = new Random(11);
		int skipped = 0;
		int total = 0;
		for (int i = 0; i < 1000; i++)
		{
			Combatant stunned = plain();
			stunned.afflict(Status.STUN, random);
			for (int turn = 0; turn < 3; turn++)
			{
				assertEquals(Status.STUN, stunned.status());
				skipped += stunned.skipsTurn(random) ? 1 : 0;
				total++;
			}

			assertNull("three turns and it is over", stunned.status());
		}

		double share = skipped / (double) total;
		assertTrue("about half: " + share, share > 0.45 && share < 0.55);
	}

	@Test
	public void aCreatureHasOneStatusAtATime()
	{
		Combatant one = plain();

		assertTrue(one.afflict(Status.POISON, new Random(1)));
		assertFalse(one.afflict(Status.BURN, new Random(1)));
		assertEquals(Status.POISON, one.status());
		one.clearStatus();
		assertTrue(one.afflict(Status.BURN, new Random(1)));
	}

	@Test
	public void poisonousCreaturesAreImmuneToPoisonAndVenomAndFieryOnesToBurn()
	{
		Combatant spider = new Combatant(SPIDER, 40, 1.0);
		Combatant imp = new Combatant(IMP, 40, 1.0);

		assertFalse(spider.afflict(Status.POISON, new Random(1)));
		assertFalse(spider.afflict(Status.VENOM, new Random(1)));
		assertTrue(spider.afflict(Status.BURN, new Random(1)));
		assertFalse(imp.afflict(Status.BURN, new Random(1)));
		assertTrue(imp.afflict(Status.POISON, new Random(1)));
		assertTrue(Status.POISON.blockedBy(EnumSet.of(Trait.POISONOUS)));
		assertFalse(Status.FREEZE.blockedBy(Collections.singleton(Trait.POISONOUS)));
	}

	@Test
	public void aNameMapsToItsStatusAndAnUnknownOneToNone()
	{
		assertEquals(Status.VENOM, Status.named("venom"));
		assertNull(Status.named("sleep"));
		assertNull(Status.named(null));
	}

	@Test
	public void onlyTheDamagingStatusesAreCuredByAnAntidote()
	{
		assertTrue(Status.POISON.curable() && Status.VENOM.curable() && Status.BURN.curable());
		assertFalse(Status.FREEZE.curable() || Status.STUN.curable());
	}
}
