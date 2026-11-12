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
import static org.junit.Assert.assertNull;
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
	public void hitLinesCarryTheStyleTheyWereRolledIn()
	{
		boolean sawBite = false;
		boolean sawBreath = false;
		for (int seed = 0; seed < 40; seed++)
		{
			for (BattleLine line : fight(FIRE_DRAGON, 40, GOBLIN, 70, seed).lines())
			{
				if (line.kind() != BattleLine.Kind.LEAD_HIT && line.kind() != BattleLine.Kind.FOE_HIT)
				{
					assertNull(line.style());
					continue;
				}

				assertEquals(line.text(), line.special() ? CombatType.MAGIC : CombatType.MELEE, line.style());
				sawBite |= !line.special();
				sawBreath |= line.special();
			}
		}

		assertTrue(sawBite);
		assertTrue(sawBreath);
	}

	@Test
	public void plainFightersCarryTheirOwnStyleAndAWyvernBreathIsIcy()
	{
		for (BattleLine line : fight(ARCHER, 40, WIZARD, 40, 5).lines())
		{
			if (line.kind() == BattleLine.Kind.LEAD_HIT)
				assertEquals(CombatType.RANGED, line.style());

			if (line.kind() == BattleLine.Kind.FOE_HIT)
				assertEquals(CombatType.MAGIC, line.style());
		}

		for (BattleLine line : fight(ICE_WYVERN, 40, GOBLIN, 70, 1).lines())
		{
			if (line.special())
				assertTrue(line.icy());
		}

		for (BattleLine line : fight(SURE_BREATH, 40, GOBLIN, 70, 1).lines())
			assertFalse(line.icy());
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

	@Test
	public void aCreatureWithNoRollsHasExactlyTheOldStats()
	{
		Combatant plain = new Combatant(GOBLIN, 20, 1.0);
		Combatant average = new Combatant(GOBLIN, "Goblin", 20, 1.0, StatRolls.AVERAGE);

		assertEquals(Math.round(20 + 2.0 * 20 + Math.sqrt(13) * 20 / 2.0), plain.maxHp());
		assertEquals(plain.maxHp(), average.maxHp());
		assertEquals(plain.attack(), average.attack());
		assertEquals(plain.defence(), average.defence());
		assertEquals(5 + 20 / 2, plain.speed());
		assertEquals(plain.speed(), average.speed());
	}

	@Test
	public void statsScaleWithRollsAndTemperament()
	{
		Combatant plain = new Combatant(GOBLIN, 40, 1.0);
		Combatant strong = new Combatant(GOBLIN, 40, 1.0, new StatRolls(5, 5, 5, 5, Temperament.STEADY));
		Combatant weak = new Combatant(GOBLIN, 40, 1.0, new StatRolls(-5, -5, -5, -5, Temperament.STEADY));
		Combatant brave = new Combatant(GOBLIN, 40, 1.0, new StatRolls(0, 0, 0, 0, Temperament.BRAVE));

		assertTrue(strong.maxHp() > plain.maxHp() && plain.maxHp() > weak.maxHp());
		assertTrue(strong.attack() > plain.attack() && plain.attack() > weak.attack());
		assertTrue(strong.defence() > plain.defence() && plain.defence() > weak.defence());
		assertTrue(strong.speed() > plain.speed() && plain.speed() > weak.speed());
		assertEquals(Math.round(plain.maxHp() * 1.075), strong.maxHp());
		assertTrue(brave.attack() > plain.attack());
		assertTrue(brave.defence() < plain.defence());
		assertEquals(plain.maxHp(), brave.maxHp());
	}

	@Test
	public void aFasterLowerLevelCreatureAttacksFirst()
	{
		StatRolls quickRolls = new StatRolls(0, 0, 0, 5, Temperament.NIMBLE);
		StatRolls slowRolls = new StatRolls(0, 0, 0, -5, Temperament.STEADY);
		Combatant quick = new Combatant(GOBLIN, 20, 1.0, quickRolls);
		Combatant slow = new Combatant(GOBLIN, 21, 1.0, slowRolls);
		assertTrue(quick.speed() > slow.speed());

		for (int seed = 0; seed < 20; seed++)
		{
			BattleResult result = BattleSim.fight(new Combatant(GOBLIN, 20, 1.0, quickRolls),
				new Combatant(GOBLIN, 21, 1.0, slowRolls), new Random(seed));

			BattleLine first = result.lines().get(1);
			assertEquals("seed " + seed, BattleLine.Kind.LEAD_HIT, first.kind());
		}
	}

	@Test
	public void aTieOnSpeedFallsBackToLevelThenTheCoin()
	{
		StatRolls rolls = new StatRolls(0, 0, 0, 0, Temperament.STEADY);
		int leadFirst = 0;
		for (int seed = 0; seed < 40; seed++)
		{
			BattleResult higher = BattleSim.fight(new Combatant(GOBLIN, 5, 1.0, rolls),
				new Combatant(GOBLIN, 4, 1.0, rolls), new Random(seed));
			BattleLine opened = higher.lines().get(1);
			assertEquals(BattleLine.Kind.LEAD_HIT, opened.kind());

			BattleResult even = BattleSim.fight(new Combatant(GOBLIN, 9, 1.0, rolls),
				new Combatant(GOBLIN, 9, 1.0, rolls), new Random(seed * 7919L));
			BattleLine toss = even.lines().get(1);
			leadFirst += toss.kind() == BattleLine.Kind.LEAD_HIT ? 1 : 0;
		}

		assertTrue("equal creatures toss a coin: " + leadFirst, leadFirst > 0 && leadFirst < 40);
	}

	@Test
	public void twoUnrolledCreaturesFightExactlyAsTheyAlwaysDid()
	{
		for (int seed = 0; seed < 50; seed++)
		{
			Random pick = new Random(seed);
			int mine = 1 + pick.nextInt(99);
			int foe = 1 + pick.nextInt(99);
			BattleResult plain = fight(GOBLIN, mine, DRAGON, foe, seed);
			BattleResult average = BattleSim.fight(new Combatant(GOBLIN, "Goblin", mine, 1.0, StatRolls.AVERAGE),
				new Combatant(DRAGON, "King Black Dragon", foe, 1.0, StatRolls.AVERAGE), new Random(seed));

			assertEquals(plain.log(), average.log());
			assertEquals(plain.won(), average.won());
			assertTrue(plain.turns() > 0);
		}
	}

	/**
	 * Battles recorded before rolls existed: seed, species, levels, winner, turns and the hash of the log. An
	 * unrolled battle must still come out exactly the same.
	 */
	@Test
	public void unrolledBattlesMatchTheOnesRecordedBeforeRollsExisted()
	{
		Species[] species = {GOBLIN, DRAGON, WIZARD};
		int[][] recorded = {
			{0, 0, 8, -1277317425},
			{1, 1, 5, 266527838},
			{2, 0, 5, 1740425804},
			{3, 1, 15, -104490119},
			{4, 1, 12, -1868845961},
			{5, 1, 6, -1565569272},
			{6, 1, 5, 104386038},
			{7, 0, 10, -60745422},
			{8, 1, 13, 145099186},
			{9, 0, 5, 2032157480},
			{10, 0, 15, -129741735},
			{11, 1, 12, -92214220}
		};
		for (int[] row : recorded)
		{
			Random pick = new Random(row[0] * 31L + 5);
			Species mine = species[pick.nextInt(3)];
			Species foe = species[pick.nextInt(3)];
			int myLevel = 1 + pick.nextInt(60);
			int foeLevel = pick.nextBoolean() ? myLevel : 1 + pick.nextInt(60);
			BattleResult result = fight(mine, myLevel, foe, foeLevel, row[0] * 7919L);

			assertEquals("seed " + row[0], row[1] == 1, result.won());
			assertEquals("seed " + row[0], row[2], result.turns());
			assertEquals("seed " + row[0], row[3], result.log().hashCode());
		}
	}

	private static BattleResult fight(Species mine, int myLevel, Species foe, int foeLevel, long seed)
	{
		return BattleSim.fight(new Combatant(mine, myLevel, 1.0), new Combatant(foe, foeLevel, 1.0),
			new Random(seed));
	}
}
