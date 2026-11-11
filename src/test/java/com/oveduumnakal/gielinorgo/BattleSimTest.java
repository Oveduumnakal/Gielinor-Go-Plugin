/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks that battles are repeatable, always finish, and respect the type triangle. */
public class BattleSimTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final Species WIZARD = new Species("Wizard", 13, CombatType.MAGIC);
	private static final Species FIRE_DRAGON = new Species("Green dragon", 88, CombatType.MELEE)
		.withSecondStyle(CombatType.MAGIC, 0.25);
	private static final Species SURE_BREATH = new Species("Test dragon", 88, CombatType.MELEE)
		.withSecondStyle(CombatType.MAGIC, 1.0);
	private static final Species ICE_WYVERN = new Species("Spitting Wyvern", 139, CombatType.MELEE)
		.withSecondStyle(CombatType.MAGIC, 1.0);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);

	@Test
	public void sameSeedGivesTheSameBattle()
	{
		BattleResult first = fight(GOBLIN, 10, ARCHER, 10, 42);
		BattleResult second = fight(GOBLIN, 10, ARCHER, 10, 42);

		assertEquals(first.log(), second.log());
		assertEquals(first.won(), second.won());
		assertEquals(first.hpFractionLeft(), second.hpFractionLeft(), 0);
	}

	@Test
	public void everyBattleEndsWithinTheTurnLimit()
	{
		Species[] all = {GOBLIN, ARCHER, WIZARD, DRAGON};
		for (int seed = 0; seed < 200; seed++)
		{
			Random pick = new Random(seed);
			Species mine = all[pick.nextInt(all.length)];
			Species foe = all[pick.nextInt(all.length)];
			BattleResult result = fight(mine, 1 + pick.nextInt(99), foe, 1 + pick.nextInt(99), seed);

			assertTrue(result.turns() <= BattleSim.MAX_TURNS);
			assertTrue(result.hpFractionLeft() >= 0 && result.hpFractionLeft() <= 1);
			assertEquals(result.turns() + 2, result.log().size());
		}
	}

	@Test
	public void typeAdvantageDecidesAnEvenMatch()
	{
		int meleeWins = 0;
		int rangedWins = 0;
		for (int seed = 0; seed < 200; seed++)
		{
			if (fight(GOBLIN, 20, ARCHER, 20, seed).won())
				meleeWins++;

			if (fight(ARCHER, 20, GOBLIN, 20, seed).won())
				rangedWins++;
		}

		assertTrue("melee should beat ranged most of the time, won " + meleeWins, meleeWins > 150);
		assertTrue("ranged should lose to melee most of the time, won " + rangedWins, rangedWins < 50);
	}

	@Test
	public void attackMultiplierShowsInTheDamage()
	{
		Combatant melee = new Combatant(GOBLIN, 50, 1.0);
		Combatant ranged = new Combatant(ARCHER, 50, 1.0);
		Combatant magic = new Combatant(WIZARD, 50, 1.0);

		int strong = BattleSim.damage(melee, ranged, new Random(3));
		int weak = BattleSim.damage(melee, magic, new Random(3));

		assertTrue(strong > weak);
	}

	@Test
	public void aStrongerSpeciesWinsAtTheSameLevel()
	{
		for (int seed = 0; seed < 50; seed++)
			assertTrue(fight(DRAGON, 30, GOBLIN, 30, seed).won());
	}

	@Test
	public void aTiredCreatureStartsWithLessHealth()
	{
		Combatant rested = new Combatant(GOBLIN, 10, 1.0);
		Combatant tired = new Combatant(GOBLIN, 10, 0.5);

		assertEquals(rested.maxHp(), tired.maxHp());
		assertEquals(rested.hp() / 2, tired.hp(), 0.001);
		assertFalse(tired.fainted());
	}

	@Test
	public void summaryKeepsTheOpeningAndTheResult()
	{
		BattleResult result = fight(GOBLIN, 10, GOBLIN, 10, 5);
		assertTrue("the test needs a long battle", result.log().size() > 6);

		List<BattleLine> full = result.lines();
		List<BattleLine> summary = result.summary(6);
		assertEquals(6, summary.size());
		assertEquals(full.subList(0, 3), summary.subList(0, 3));
		assertEquals(full.subList(full.size() - 3, full.size()), summary.subList(3, 6));
		assertEquals(full, result.summary(1000));
	}

	@Test
	public void hitsFloatOverWhoeverWasHit()
	{
		BattleResult result = fight(GOBLIN, 10, GOBLIN, 10, 5);
		List<BattleLine> lines = result.lines();

		assertFalse("the opening is about the wild creature", lines.get(0).overPlayer());
		assertFalse("the result is about the wild creature", lines.get(lines.size() - 1).overPlayer());
		int overPlayer = 0;
		for (BattleLine line : lines.subList(1, lines.size() - 1))
		{
			assertEquals(line.overPlayer() ? Tone.BAD : Tone.GOOD, line.tone());
			assertEquals(line.overPlayer(), line.text().startsWith("Goblin -"));
			if (line.overPlayer())
				overPlayer++;
		}

		assertEquals("hits alternate between the two sides", result.turns() / 2.0, overPlayer, 0.5);
	}

	@Test
	public void everyLineKnowsWhatItIs()
	{
		BattleResult result = fight(GOBLIN, 10, ARCHER, 10, 3);
		List<BattleLine> lines = result.lines();

		assertEquals(BattleLine.Kind.OPENING, lines.get(0).kind());
		assertEquals(BattleLine.Kind.CLOSING, lines.get(lines.size() - 1).kind());
		for (BattleLine line : lines.subList(1, lines.size() - 1))
		{
			BattleLine.Kind expected = line.overPlayer() ? BattleLine.Kind.FOE_HIT : BattleLine.Kind.LEAD_HIT;
			assertEquals(expected, line.kind());
		}
	}

	@Test
	public void aDragonBreathesFireOnAboutOneHitInFour()
	{
		int hits = 0;
		int breaths = 0;
		for (int seed = 0; seed < 300; seed++)
		{
			for (BattleLine line : fight(FIRE_DRAGON, 40, GOBLIN, 70, seed).lines())
			{
				if (line.kind() != BattleLine.Kind.LEAD_HIT)
					continue;

				hits++;
				if (line.special())
				{
					breaths++;
					assertTrue(line.text(), line.text().startsWith("Green dragon breathes fire for "));
				}
				else
				{
					assertTrue(line.text(), line.text().startsWith("-"));
				}
			}
		}

		double share = breaths / (double) hits;
		assertTrue("expected about 0.25 of " + hits + " hits to be breath, got " + share, share > 0.2 && share < 0.3);
	}

	@Test
	public void aWyvernBreathesIce()
	{
		for (BattleLine line : fight(ICE_WYVERN, 40, GOBLIN, 70, 1).lines())
		{
			if (line.special())
				assertTrue(line.text(), line.text().startsWith("Spitting Wyvern breathes ice for "));
		}
	}

	@Test
	public void aBreathOverAPlayerReadsTheSameAndTheOthersAreUnchanged()
	{
		boolean sawBreath = false;
		for (int seed = 0; seed < 50 && !sawBreath; seed++)
		{
			for (BattleLine line : fight(GOBLIN, 70, FIRE_DRAGON, 40, seed).lines())
			{
				if (line.kind() == BattleLine.Kind.FOE_HIT && line.special())
				{
					sawBreath = true;
					assertTrue(line.overPlayer());
					assertTrue(line.text(), line.text().startsWith("Green dragon breathes fire for "));
				}
				else if (line.kind() == BattleLine.Kind.FOE_HIT)
				{
					assertTrue(line.text(), line.text().startsWith("Goblin -"));
				}
			}
		}

		assertTrue(sawBreath);
	}

	@Test
	public void aBattleWithBreathIsTheSameForTheSameSeed()
	{
		for (int seed = 0; seed < 20; seed++)
		{
			BattleResult first = fight(FIRE_DRAGON, 30, ARCHER, 30, seed);
			BattleResult second = fight(FIRE_DRAGON, 30, ARCHER, 30, seed);

			assertEquals(first.log(), second.log());
			assertEquals(first.won(), second.won());
			assertEquals(first.hpFractionLeft(), second.hpFractionLeft(), 0);
		}
	}

	@Test
	public void theTriangleUsesTheStyleRolledForTheHit()
	{
		Combatant dragon = new Combatant(FIRE_DRAGON, 50, 1.0);
		Combatant archer = new Combatant(ARCHER, 50, 1.0);
		Combatant goblin = new Combatant(GOBLIN, 50, 1.0);

		int bite = BattleSim.damage(dragon, CombatType.MELEE, archer, new Random(3));
		int breath = BattleSim.damage(dragon, CombatType.MAGIC, archer, new Random(3));
		assertTrue("melee beats ranged, magic loses to it", bite > breath);

		int bitten = BattleSim.damage(dragon, CombatType.MELEE, goblin, new Random(3));
		int breathed = BattleSim.damage(dragon, CombatType.MAGIC, goblin, new Random(3));
		assertTrue("magic beats melee", breathed > bitten);
		assertEquals(bite, BattleSim.damage(dragon, archer, new Random(3)));
	}

	@Test
	public void aCreatureWithOneStyleLeavesTheRandomSourceAlone()
	{
		Combatant goblin = new Combatant(GOBLIN, 10, 1.0);

		for (long seed = 0; seed < 100; seed++)
		{
			Random used = new Random(seed);
			Random untouched = new Random(seed);
			for (int i = 0; i < 20; i++)
				assertEquals(CombatType.MELEE, goblin.rollStyle(used));

			assertEquals("seed " + seed, untouched.nextLong(), used.nextLong());
		}
	}

	@Test
	public void aCreatureWithTwoStylesDrawsExactlyOneValuePerAttack()
	{
		Combatant dragon = new Combatant(FIRE_DRAGON, 10, 1.0);
		boolean breathed = false;
		boolean bit = false;

		for (long seed = 0; seed < 100; seed++)
		{
			Random used = new Random(seed);
			Random mirror = new Random(seed);
			for (int i = 0; i < 20; i++)
			{
				CombatType expected = mirror.nextDouble() < FIRE_DRAGON.secondChance() ? CombatType.MAGIC
					: CombatType.MELEE;
				CombatType style = dragon.rollStyle(used);

				assertEquals("seed " + seed + " attack " + i, expected, style);
				breathed |= style == CombatType.MAGIC;
				bit |= style == CombatType.MELEE;
			}

			assertEquals("seed " + seed, mirror.nextLong(), used.nextLong());
		}

		assertTrue(breathed && bit);
	}

	@Test
	public void aTwoStyleFightDrawsTheSameOnBothSides()
	{
		for (long seed = 0; seed < 50; seed++)
		{
			Random first = new Random(seed);
			Random second = new Random(seed);

			BattleResult mine = BattleSim.fight(new Combatant(FIRE_DRAGON, 40, 1.0), new Combatant(GOBLIN, 70, 1.0),
				first);
			BattleResult theirs = BattleSim.fight(new Combatant(FIRE_DRAGON, 40, 1.0), new Combatant(GOBLIN, 70, 1.0),
				second);

			List<BattleLine> mineLines = mine.lines();
			List<BattleLine> theirLines = theirs.lines();
			assertEquals(mineLines.size(), theirLines.size());
			for (int i = 0; i < mineLines.size(); i++)
			{
				BattleLine a = mineLines.get(i);
				BattleLine b = theirLines.get(i);

				assertEquals(a.text(), b.text());
				assertEquals(a.special(), b.special());
			}

			assertEquals("seed " + seed, first.nextLong(), second.nextLong());
		}
	}

	private static BattleResult fight(Species mine, int myLevel, Species foe, int foeLevel, long seed)
	{
		return BattleSim.fight(new Combatant(mine, myLevel, 1.0), new Combatant(foe, foeLevel, 1.0),
			new Random(seed));
	}
}
