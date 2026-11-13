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

/** Checks who acts on which game tick of an acted-out battle. */
public class BattleTimelineTest
{
	private static final double CLOSE = 0.0001;

	private static final List<BattleTimeline.Step> TWO_HITS = Arrays.asList(
		step(BattleTimeline.Beat.LEAD_ATTACKS), step(BattleTimeline.Beat.FOE_ATTACKS),
		step(BattleTimeline.Beat.FINISH));

	@Test
	public void linesBecomeBeatsInOrder()
	{
		List<BattleLine> lines = Arrays.asList(
			line(BattleLine.Kind.OPENING),
			line(BattleLine.Kind.LEAD_HIT),
			line(BattleLine.Kind.FOE_HIT),
			line(BattleLine.Kind.OTHER),
			line(BattleLine.Kind.CLOSING));

		assertEquals(Arrays.asList(BattleTimeline.Beat.FACE_OFF, BattleTimeline.Beat.LEAD_ATTACKS,
			BattleTimeline.Beat.FOE_ATTACKS, BattleTimeline.Beat.FINISH), beatsOf(BattleTimeline.stepsOf(lines)));
	}

	@Test
	public void aBreathLineBecomesASpecialBeat()
	{
		List<BattleLine> lines = Arrays.asList(
			new BattleLine("x", Tone.GOOD, false, BattleLine.Kind.LEAD_HIT, true),
			new BattleLine("x", Tone.BAD, true, BattleLine.Kind.FOE_HIT, true),
			line(BattleLine.Kind.FOE_HIT),
			line(BattleLine.Kind.CLOSING));

		assertEquals(Arrays.asList(BattleTimeline.Beat.LEAD_SPECIAL, BattleTimeline.Beat.FOE_SPECIAL,
			BattleTimeline.Beat.FOE_ATTACKS, BattleTimeline.Beat.FINISH), beatsOf(BattleTimeline.stepsOf(lines)));
	}

	@Test
	public void aRealBattleEndsWithTheResult()
	{
		Combatant mine = new Combatant(new Species("Cow", 2, CombatType.STAB), 5, 1.0);
		Combatant foe = new Combatant(new Species("Goblin", 13, CombatType.STAB), 5, 1.0);

		BattleResult result = BattleSim.fight(mine, foe, new Random(7));
		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(result.lines());

		assertEquals(BattleTimeline.Beat.FACE_OFF, steps.get(0).beat());
		assertEquals(BattleTimeline.Beat.FINISH, steps.get(steps.size() - 1).beat());
		assertEquals(result.lines().size(), steps.size());
	}

	@Test
	public void aMissingFinishIsAdded()
	{
		BattleTimeline timeline = new BattleTimeline(Collections.singletonList(step(BattleTimeline.Beat.LEAD_ATTACKS)),
			true);

		assertEquals(Arrays.asList(BattleTimeline.Beat.LEAD_ATTACKS, BattleTimeline.Beat.FINISH),
			beatsOf(timeline.steps()));
		assertEquals(Collections.singletonList(BattleTimeline.Beat.FINISH),
			beatsOf(new BattleTimeline(Collections.emptyList(), true).steps()));
	}

	@Test
	public void nothingHappensWhileTheLeadWalks()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, true);

		for (int tick = 1; tick < BattleTimeline.MAX_WALK_TICKS; tick++)
		{
			assertNull(timeline.tick(false));
			assertEquals(BattleTimeline.Phase.WALKING, timeline.phase());
			assertFalse(timeline.fightStarted());
		}
	}

	@Test
	public void theFirstBlowComesOnTheTickTheLeadArrives()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, true);
		timeline.tick(false);
		timeline.tick(false);

		assertEquals(BattleTimeline.Beat.LEAD_ATTACKS, beatOf(timeline.tick(true)));
		assertTrue(timeline.fightStarted());
		assertEquals(BattleTimeline.Phase.FIGHTING, timeline.phase());
	}

	@Test
	public void aLeadThatNeverArrivesIsGivenUpOn()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, true);
		for (int tick = 1; tick < BattleTimeline.MAX_WALK_TICKS; tick++)
			timeline.tick(false);

		assertEquals(BattleTimeline.Beat.LEAD_ATTACKS, beatOf(timeline.tick(false)));
	}

	@Test
	public void beatsComeBeatTicksApart()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, true);

		assertEquals(BattleTimeline.Beat.LEAD_ATTACKS, beatOf(timeline.tick(true)));
		assertEquals(BattleTimeline.Beat.FOE_ATTACKS, beatOf(afterPause(timeline)));
		assertEquals(BattleTimeline.Phase.FIGHTING, timeline.phase());
		assertEquals(BattleTimeline.Beat.FINISH, beatOf(afterPause(timeline)));
		assertEquals(BattleTimeline.Phase.DOWN, timeline.phase());
	}

	private static BattleTimeline.Step afterPause(BattleTimeline timeline)
	{
		for (int tick = 1; tick < BattleTimeline.BEAT_TICKS; tick++)
			assertNull(timeline.tick(true));

		return timeline.tick(true);
	}

	@Test
	public void theLoserStaysDownThenEverythingIsOver()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, false);
		timeline.tick(true);
		afterPause(timeline);
		afterPause(timeline);

		for (int tick = 1; tick < BattleTimeline.HOLD_TICKS; tick++)
		{
			assertNull(timeline.tick(true));
			assertEquals(BattleTimeline.Phase.DOWN, timeline.phase());
		}

		assertNull(timeline.tick(true));
		assertEquals(BattleTimeline.Phase.DONE, timeline.phase());
		assertNull(timeline.tick(true));
		assertEquals(BattleTimeline.Phase.DONE, timeline.phase());
	}

	@Test
	public void aBeatenWildCreatureStaysDownTenSecondsKnockedOut()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, true);
		assertFalse(timeline.knockedOut());
		timeline.tick(true);
		afterPause(timeline);
		afterPause(timeline);

		assertEquals(17, BattleTimeline.KNOCKED_OUT_TICKS);
		assertEquals(BattleTimeline.KNOCKED_OUT_TICKS, timeline.holdTicks());
		for (int tick = 1; tick < BattleTimeline.KNOCKED_OUT_TICKS; tick++)
		{
			assertNull(timeline.tick(true));
			assertEquals(BattleTimeline.Phase.DOWN, timeline.phase());
			assertTrue(timeline.knockedOut());
		}

		assertNull(timeline.tick(true));
		assertEquals(BattleTimeline.Phase.DONE, timeline.phase());
		assertFalse(timeline.knockedOut());
	}

	@Test
	public void theLeadStaysDownTheShortTimeWhenItLostAndIsNeverKnockedOut()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, false);
		timeline.tick(true);
		afterPause(timeline);
		afterPause(timeline);

		assertEquals(BattleTimeline.HOLD_TICKS, timeline.holdTicks());
		assertFalse(timeline.knockedOut());
		for (int tick = 1; tick < BattleTimeline.HOLD_TICKS; tick++)
			assertNull(timeline.tick(true));

		assertEquals(BattleTimeline.Phase.DOWN, timeline.phase());
		assertNull(timeline.tick(true));
		assertEquals(BattleTimeline.Phase.DONE, timeline.phase());
	}

	@Test
	public void stepsKeepTheStyleAndTheLook()
	{
		List<BattleLine> lines = Arrays.asList(
			new BattleLine("x", Tone.GOOD, false, BattleLine.Kind.LEAD_HIT, false, CombatType.RANGED, false),
			new BattleLine("x", Tone.BAD, true, BattleLine.Kind.FOE_HIT, true, CombatType.MAGIC, true),
			new BattleLine("x", Tone.BAD, true, BattleLine.Kind.FOE_HIT, false, CombatType.STAB, false),
			line(BattleLine.Kind.CLOSING));

		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(lines);

		assertEquals(CombatType.RANGED, steps.get(0).style());
		assertEquals(ProjectileLook.ARROW, steps.get(0).look());
		assertFalse(steps.get(0).melee());
		assertEquals(CombatType.MAGIC, steps.get(1).style());
		assertEquals(ProjectileLook.ICE, steps.get(1).look());
		assertEquals(CombatType.STAB, steps.get(2).style());
		assertNull(steps.get(2).look());
		assertTrue(steps.get(2).melee());
		assertNull(steps.get(3).style());
	}

	@Test
	public void theLinesAreDueOneGameTickAfterTheFirstBeat()
	{
		BattleTimeline timeline = new BattleTimeline(TWO_HITS, true);
		assertFalse(timeline.linesDue());
		timeline.tick(false);
		assertFalse(timeline.linesDue());

		timeline.tick(true);
		assertTrue(timeline.fightStarted());
		assertFalse(timeline.linesDue());

		timeline.tick(true);
		assertTrue(timeline.linesDue());
		timeline.tick(true);
		assertTrue(timeline.linesDue());
	}

	@Test
	public void theLoserIsTheLeadOnlyWhenTheLeadLost()
	{
		assertTrue(new BattleTimeline(TWO_HITS, false).leadLost());
		assertFalse(new BattleTimeline(TWO_HITS, true).leadLost());
	}

	@Test
	public void aLungeGoesOutAndBackAndEndsAtRest()
	{
		assertEquals(0.0, BattleTimeline.lunge(0, 0.4), CLOSE);
		assertEquals(0.4, BattleTimeline.lunge(BattleTimeline.LUNGE_TICKS / 2, 0.4), CLOSE);
		assertEquals(0.0, BattleTimeline.lunge(BattleTimeline.LUNGE_TICKS, 0.4), CLOSE);
		assertEquals(0.0, BattleTimeline.lunge(BattleTimeline.LUNGE_TICKS + 50, 0.4), CLOSE);
		assertEquals(0.0, BattleTimeline.lunge(-1, 0.4), CLOSE);
	}

	@Test
	public void aRecoilGoesTheOtherWay()
	{
		assertTrue(BattleTimeline.lunge(BattleTimeline.LUNGE_TICKS / 2, -0.15) < 0);
	}

	private static BattleTimeline.Step step(BattleTimeline.Beat beat)
	{
		return new BattleTimeline.Step(beat, null);
	}

	private static BattleTimeline.Beat beatOf(BattleTimeline.Step step)
	{
		return step == null ? null : step.beat();
	}

	private static List<BattleTimeline.Beat> beatsOf(List<BattleTimeline.Step> steps)
	{
		List<BattleTimeline.Beat> beats = new ArrayList<>();
		for (BattleTimeline.Step step : steps)
			beats.add(step.beat());

		return beats;
	}

	private static BattleLine line(BattleLine.Kind kind)
	{
		return new BattleLine("x", Tone.INFO, false, kind);
	}
}
