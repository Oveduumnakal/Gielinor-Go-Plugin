/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.runelite.api.WorldView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks when a duel is settled, so the next beat of a battle may start. */
public class DuelActingTest
{
	/** A side whose melee turn is on or off. */
	private static final class FakeSide implements DuelActing.Side
	{
		private final List<CreatureBody.Act> acts = new ArrayList<>();
		private boolean turning;

		@Override
		public void act(CreatureBody.Act act)
		{
			acts.add(act);
		}

		@Override
		public void meleeTurn(double distance, CreatureBody.Act act)
		{
		}

		@Override
		public boolean turning()
		{
			return turning;
		}

		@Override
		public double x()
		{
			return 0;
		}

		@Override
		public double y()
		{
			return 0;
		}

		@Override
		public double height()
		{
			return 0;
		}
	}

	@Test
	public void aDuelIsSettledOnlyWhenNeitherSideIsInAMeleeTurn()
	{
		FakeSide lead = new FakeSide();
		FakeSide foe = new FakeSide();
		DuelActing acting = new DuelActing(null, null, lead, foe);

		assertTrue(acting.settled());

		lead.turning = true;
		assertFalse(acting.settled());

		lead.turning = false;
		foe.turning = true;
		assertFalse(acting.settled());

		foe.turning = false;
		assertTrue(acting.settled());
	}

	/** Counts how many projectiles a duel launches. */
	private static final class FakeShot implements DuelActing.Shot
	{
		private final List<ProjectileLook> launched = new ArrayList<>();

		@Override
		public boolean launch(ProjectileLook flight, ProjectilePath route, double casterScale)
		{
			launched.add(flight);
			return true;
		}

		@Override
		public void place(int age, WorldView view, int plane)
		{
		}

		@Override
		public void hide()
		{
		}

		@Override
		public void remove()
		{
		}
	}

	private static BattleTimeline.Step spell(BattleTimeline.Beat beat)
	{
		return new BattleTimeline.Step(beat, CombatType.MAGIC, ProjectileLook.SPELL);
	}

	@Test
	public void oneMagicAttackCastsOnceAndLaunchesOneProjectile()
	{
		FakeSide lead = new FakeSide();
		FakeSide foe = new FakeSide();
		FakeShot shot = new FakeShot();
		DuelActing acting = new DuelActing(() -> shot, lead, foe);

		acting.play(spell(BattleTimeline.Beat.LEAD_ATTACKS), false);

		assertEquals(Arrays.asList(CreatureBody.Act.ATTACK), lead.acts);
		assertEquals(Arrays.asList(ProjectileLook.SPELL), shot.launched);
		assertTrue(foe.acts.isEmpty());
	}

	@Test
	public void aBeatThatIsPlayedAgainDoesNotCastAgain()
	{
		FakeSide lead = new FakeSide();
		FakeShot shot = new FakeShot();
		DuelActing acting = new DuelActing(() -> shot, lead, new FakeSide());
		BattleTimeline.Step step = spell(BattleTimeline.Beat.LEAD_ATTACKS);

		acting.play(step, false);
		acting.play(step, false);

		assertEquals(1, lead.acts.size());
		assertEquals(1, shot.launched.size());
	}

	@Test
	public void twoMagicAttacksInARowCastTwiceAndTheBlowOfTheFirstLandsOnce()
	{
		FakeSide lead = new FakeSide();
		FakeSide foe = new FakeSide();
		FakeShot shot = new FakeShot();
		DuelActing acting = new DuelActing(() -> shot, lead, foe);

		acting.play(spell(BattleTimeline.Beat.LEAD_ATTACKS), false);
		acting.play(spell(BattleTimeline.Beat.FOE_ATTACKS), false);

		assertEquals(Arrays.asList(CreatureBody.Act.ATTACK), lead.acts);
		assertEquals(Arrays.asList(CreatureBody.Act.HIT, CreatureBody.Act.ATTACK), foe.acts);
		assertEquals(2, shot.launched.size());
	}

	@Test
	public void aSecondAttackBreathCastsOnceToo()
	{
		FakeSide foe = new FakeSide();
		FakeShot shot = new FakeShot();
		DuelActing acting = new DuelActing(() -> shot, new FakeSide(), foe);

		acting.play(new BattleTimeline.Step(BattleTimeline.Beat.FOE_SPECIAL, CombatType.MAGIC, ProjectileLook.FIRE),
			false);

		assertEquals(Arrays.asList(CreatureBody.Act.SPECIAL), foe.acts);
		assertEquals(1, shot.launched.size());
	}
}
