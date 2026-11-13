/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final Species WIZARD = new Species("Wizard", 13, CombatType.MAGIC);
	private static final Species FIRE_DRAGON = new Species("Green dragon", 88, CombatType.STAB);
	private static final Species SURE_BREATH = new Species("Test dragon", 88, CombatType.STAB);
	private static final Species ICE_WYVERN = new Species("Spitting Wyvern", 139, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

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
	public void aDragonUsesBothItsMovesAndTheLinesNameThem()
	{
		int hits = 0;
		int breaths = 0;
		int bites = 0;
		for (int seed = 0; seed < 300; seed++)
		{
			for (BattleLine line : fight(FIRE_DRAGON, 40, GOBLIN, 70, seed).lines())
			{
				if (line.kind() != BattleLine.Kind.LEAD_HIT)
					continue;

				hits++;
				boolean missed = line.miss();
				if (line.special())
				{
					breaths++;
					String start = missed ? "Green dragon's Dragonfire missed" : "Green dragon uses Dragonfire: -";
					assertTrue(line.text(), line.text().startsWith(start));
				}
				else
				{
					bites++;
					assertTrue(line.text(), line.text().startsWith("Green dragon uses Bite: -"));
				}
			}
		}

		assertTrue("both moves come up: " + breaths + " breaths, " + bites + " bites", breaths > 0 && bites > 0);
		assertTrue("the stronger move comes up more often", breaths + bites == hits);
	}

	@Test
	public void aWyvernBreathesIce()
	{
		boolean sawBreath = false;
		for (BattleLine line : fight(ICE_WYVERN, 40, GOBLIN, 70, 1).lines())
		{
			if (line.special() && !line.miss())
			{
				sawBreath = true;
				assertTrue(line.text(), line.text().startsWith("Spitting Wyvern uses Icy breath: -"));
			}
		}

		assertTrue(sawBreath);
	}

	@Test
	public void hitLinesCarryTheStyleOfTheMoveThatMadeThem()
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

				if (line.overPlayer())
					continue;

				assertEquals(line.text(), line.special() ? CombatType.MAGIC : CombatType.STAB, line.style());
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
	public void aMoveOverAPlayerReadsTheSameAsOneOverTheFoe()
	{
		boolean sawBreath = false;
		for (int seed = 0; seed < 50 && !sawBreath; seed++)
		{
			for (BattleLine line : fight(GOBLIN, 70, FIRE_DRAGON, 40, seed).lines())
			{
				if (line.kind() == BattleLine.Kind.FOE_HIT && line.special() && !line.miss())
				{
					sawBreath = true;
					assertTrue(line.overPlayer());
					assertTrue(line.text(), line.text().startsWith("Green dragon uses Dragonfire: -"));
				}
				else if (line.kind() == BattleLine.Kind.LEAD_HIT)
				{
					assertTrue(line.text(), line.text().startsWith("-"));
				}
			}
		}

		assertTrue(sawBreath);
	}

	@Test
	public void aMissIsAnAttackLineThatSaysSoAndDoesNoDamage()
	{
		boolean sawMiss = false;
		for (int seed = 0; seed < 40; seed++)
		{
			Combatant dragon = new Combatant(FIRE_DRAGON, 40, 1.0).moves(TestMoves.dragon());
			Combatant goblin = new Combatant(GOBLIN, 70, 1.0);
			BattleResult result = BattleSim.fight(dragon, goblin, new Random(seed));
			for (BattleLine line : result.lines())
			{
				if (!line.miss())
					continue;

				sawMiss = true;
				assertEquals("a miss is still a blow", BattleLine.Kind.LEAD_HIT, line.kind());
				assertEquals("Green dragon's Dragonfire missed", line.text());
				assertFalse(line.overPlayer());
				assertEquals(Tone.INFO, line.tone());
			}
		}

		assertTrue(sawMiss);
	}

	@Test
	public void aBattleWithMovesIsTheSameForTheSameSeed()
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
	public void theTriangleUsesTheStyleOfTheMove()
	{
		Combatant dragon = new Combatant(FIRE_DRAGON, 50, 1.0);
		Combatant archer = new Combatant(ARCHER, 50, 1.0);
		Combatant goblin = new Combatant(GOBLIN, 50, 1.0);

		int bite = BattleSim.damage(dragon, CombatType.STAB, archer, new Random(3));
		int breath = BattleSim.damage(dragon, CombatType.MAGIC, archer, new Random(3));
		assertTrue("melee beats ranged, magic loses to it", bite > breath);

		int bitten = BattleSim.damage(dragon, CombatType.STAB, goblin, new Random(3));
		int breathed = BattleSim.damage(dragon, CombatType.MAGIC, goblin, new Random(3));
		assertTrue("magic beats melee", breathed > bitten);
		assertEquals(bite, BattleSim.damage(dragon, archer, new Random(3)));
	}

	@Test
	public void powerScalesTheDamageAndPowerOneChangesNothing()
	{
		Combatant attacker = new Combatant(GOBLIN, 50, 1.0);
		Combatant target = new Combatant(GOBLIN, 50, 1.0);
		Move plain = new Move("a", "A", CombatType.STAB, 1.0, 1.0);
		Move strong = new Move("b", "B", CombatType.STAB, 2.0, 1.0);

		assertEquals(BattleSim.damage(attacker, target, new Random(3)), BattleSim.damage(attacker, plain, target,
			new Random(3)));
		assertTrue(BattleSim.damage(attacker, strong, target, new Random(3))
			> BattleSim.damage(attacker, plain, target, new Random(3)));
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
	 * unrolled battle must still come out exactly the same. Re-recorded for issue 85: the logs name a style, and
	 * Melee is now Stab, so seven of the twelve hashes changed; hashing the new logs with "Stab" turned back into
	 * "Melee" gives the old values, so nothing but that word moved.
	 */
	@Test
	public void unrolledBattlesMatchTheOnesRecordedBeforeRollsExisted()
	{
		Species[] species = {GOBLIN, DRAGON, WIZARD};
		int[][] recorded = {
			{0, 0, 8, 1618297563},
			{1, 1, 5, 266527838},
			{2, 0, 5, -690367402},
			{3, 1, 15, -104490119},
			{4, 1, 12, -69464885},
			{5, 1, 6, -1892739012},
			{6, 1, 5, -189094218},
			{7, 0, 10, -490844800},
			{8, 1, 13, 145099186},
			{9, 0, 5, 2032157480},
			{10, 0, 15, -129741735},
			{11, 1, 12, -1721383970}
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
		return BattleSim.fight(fighter(mine, myLevel), fighter(foe, foeLevel), new Random(seed));
	}

	/** Builds a fighter; the test dragons and the wyvern get the moves their names suggest. */
	private static Combatant fighter(Species species, int level)
	{
		Combatant one = new Combatant(species, level, 1.0);
		if (species == FIRE_DRAGON)
			return one.moves(TestMoves.dragon());

		if (species == SURE_BREATH)
			return one.moves(Collections.singletonList(TestMoves.DRAGONFIRE));

		if (species == ICE_WYVERN)
			return one.moves(Collections.singletonList(TestMoves.ICY_BREATH));

		return one;
	}

	@Test
	public void teamFightKeepsADuelForEveryKnockOut()
	{
		List<Combatant> mine = new ArrayList<>(Arrays.asList(new Combatant(GOBLIN, 12, 1.0),
			new Combatant(ARCHER, 30, 1.0)));
		List<Combatant> theirs = new ArrayList<>(Arrays.asList(new Combatant(WIZARD, 20, 1.0),
			new Combatant(DRAGON, 40, 1.0)));

		TeamBattleResult result = BattleSim.teamFight(mine, theirs, new Random(3));

		List<Duel> duels = result.duels();
		List<BattleLine> lines = result.lines();

		assertEquals(lines.size(), duels.size());
		for (int i = 0; i < duels.size(); i++)
		{
			Duel duel = duels.get(i);
			assertEquals(lines.get(i), duel.knockOut());
			assertFalse(duel.hits().isEmpty());
			for (BattleLine hit : duel.hits())
				assertTrue(hit.kind() == BattleLine.Kind.LEAD_HIT || hit.kind() == BattleLine.Kind.FOE_HIT);
		}

		Duel first = duels.get(0);

		assertEquals("goblin", first.mineSpecies());
		assertEquals("wizard", first.theirSpecies());
		assertEquals(0, first.mineIndex());
		assertEquals(0, first.theirIndex());
	}

	@Test
	public void aHitAndAMissCarryTheIdOfTheMove()
	{
		Combatant attacker = new Combatant(GOBLIN, 50, 1.0);
		Combatant target = new Combatant(GOBLIN, 50, 1.0);
		Move wind = new Move("wind_strike", "Wind strike", CombatType.MAGIC, 1.0, 1.0);

		assertEquals("wind_strike", BattleSim.strike(attacker, target, 3, true, wind).move());
		assertEquals("wind_strike", BattleSim.strike(attacker, target, 3, false, wind).move());
		assertEquals("wind_strike", BattleSim.miss(attacker, true, wind).move());
	}
}
