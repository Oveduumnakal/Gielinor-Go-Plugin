/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests the odds of running from a wild creature. */
public class FleeRulesTest
{
	/** A source that always rolls the same number. */
	private static final class Always extends Random
	{
		private static final long serialVersionUID = 1L;
		private final double roll;

		private Always(double roll)
		{
			this.roll = roll;
		}

		@Override
		public double nextDouble()
		{
			return roll;
		}
	}

	@Test
	public void theChancesMatchTheTableOfTheIssue()
	{
		assertEquals(0.80, FleeRules.chance(30, 20, false), 1e-9);
		assertEquals(0.95, FleeRules.chance(30, 20, true), 1e-9);
		assertEquals(0.50, FleeRules.chance(20, 20, false), 1e-9);
		assertEquals(0.70, FleeRules.chance(20, 20, true), 1e-9);
		assertEquals(0.20, FleeRules.chance(10, 20, false), 1e-9);
		assertEquals(0.40, FleeRules.chance(10, 20, true), 1e-9);
	}

	@Test
	public void theChanceStaysBetweenTenAndNinetyFivePercent()
	{
		assertEquals(0.10, FleeRules.chance(1, 99, false), 1e-9);
		assertEquals(0.95, FleeRules.chance(99, 1, true), 1e-9);
	}

	@Test
	public void aRollUnderTheChanceGetsAway()
	{
		assertTrue(FleeRules.escapes(20, 20, false, new Always(0.49)));
		assertFalse(FleeRules.escapes(20, 20, false, new Always(0.50)));
	}

	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private static BattleState battle(Combatant mine, Combatant foe, double roll, BattleState.Mode mode)
	{
		return new BattleState(Collections.singletonList(mine), Collections.singletonList(foe), new Always(roll), mode);
	}

	@Test
	public void aFailedTryCostsTheTurnAndTheOpponentAttacks()
	{
		Combatant mine = new Combatant(GOBLIN, 20, 1.0);
		BattleState state = battle(mine, new Combatant(GOBLIN, 20, 1.0), 0.6, BattleState.Mode.WILD);

		TurnResult turn = state.step(BattleChoice.run());

		assertEquals(TurnResult.Status.ONGOING, turn.status());
		assertTrue(turn.turnUsed());
		List<BattleLine> lines = turn.lines();
		assertEquals("You could not get away!", lines.get(0).text());
		assertTrue(mine.hp() < mine.maxHp());
	}

	@Test
	public void aSuccessfulTryEndsTheBattle()
	{
		Combatant mine = new Combatant(GOBLIN, 20, 1.0);
		BattleState state = battle(mine, new Combatant(GOBLIN, 20, 1.0), 0.4, BattleState.Mode.WILD);

		assertEquals(TurnResult.Status.RAN, state.step(BattleChoice.run()).status());
		assertEquals(mine.maxHp(), mine.hp(), 0.0);
	}

	@Test
	public void walkingAwayAndOtherBattlesAlwaysGetAway()
	{
		BattleState left = battle(new Combatant(GOBLIN, 1, 1.0), new Combatant(GOBLIN, 99, 1.0), 0.999,
			BattleState.Mode.WILD);
		BattleState champion = battle(new Combatant(GOBLIN, 1, 1.0), new Combatant(GOBLIN, 99, 1.0), 0.999,
			BattleState.Mode.CHAMPION);

		assertEquals(TurnResult.Status.RAN, left.step(BattleChoice.leave()).status());
		assertEquals(1.0, champion.fleeChance(), 0.0);
		assertEquals(TurnResult.Status.RAN, champion.step(BattleChoice.run()).status());
		assertFalse(BattleChoice.run().sure());
		assertTrue(BattleChoice.leave().sure());
		assertEquals(BattleChoice.run(), BattleChoice.leave());
	}

	@Test
	public void theRunButtonSaysTheOdds()
	{
		assertEquals("", BattleControlsModel.runTip(1.0));
		assertEquals("70% chance to get away. A failed try costs your turn.", BattleControlsModel.runTip(0.7));
	}
}
