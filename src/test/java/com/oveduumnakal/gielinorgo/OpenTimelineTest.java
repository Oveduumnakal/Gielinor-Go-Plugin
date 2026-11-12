/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks a battle timeline that is fed turn by turn: waiting, appending, finishing, and no change for the rest. */
public class OpenTimelineTest
{
	private static BattleTimeline.Step step(BattleTimeline.Beat beat)
	{
		return new BattleTimeline.Step(beat, null);
	}

	private static List<BattleTimeline.Beat> trace(BattleTimeline timeline, int ticks)
	{
		List<BattleTimeline.Beat> beats = new ArrayList<>();
		for (int i = 0; i < ticks; i++)
		{
			BattleTimeline.Step played = timeline.tick(true);
			beats.add(played == null ? null : played.beat());
		}

		return beats;
	}

	@Test
	public void anOpenBattlePlaysWhatItHasAndThenWaitsForever()
	{
		BattleTimeline timeline = BattleTimeline.open(Collections.singletonList(step(BattleTimeline.Beat.FACE_OFF)));

		List<BattleTimeline.Beat> beats = trace(timeline, 400);

		assertEquals(BattleTimeline.Beat.FACE_OFF, beats.get(0));
		assertEquals(1, beats.stream().filter(beat -> beat != null).count());
		assertEquals(BattleTimeline.Phase.WAITING, timeline.phase());
		assertTrue(timeline.waiting());
		assertFalse(timeline.sealed());
		assertTrue(timeline.fightStarted());
	}

	@Test
	public void aTurnAppendedWhileWaitingPlaysAtOnceAndTheBattleWaitsAgain()
	{
		BattleTimeline timeline = BattleTimeline.open(Collections.singletonList(step(BattleTimeline.Beat.FACE_OFF)));
		trace(timeline, 20);
		assertTrue(timeline.waiting());

		timeline.append(Arrays.asList(step(BattleTimeline.Beat.LEAD_ATTACKS), step(BattleTimeline.Beat.FOE_ATTACKS)));
		List<BattleTimeline.Beat> beats = trace(timeline, 20);

		assertFalse(timeline.waiting() && beats.get(0) == null);
		assertEquals(BattleTimeline.Beat.LEAD_ATTACKS, beats.get(0));
		assertEquals(BattleTimeline.Beat.FOE_ATTACKS, beats.get(BattleTimeline.BEAT_TICKS));
		assertEquals(BattleTimeline.Phase.WAITING, timeline.phase());
	}

	@Test
	public void aTurnAppendedBeforeTheLastBeatEndsFollowsItAtTheUsualPace()
	{
		BattleTimeline timeline = BattleTimeline.open(Collections.singletonList(step(BattleTimeline.Beat.FACE_OFF)));
		List<BattleTimeline.Beat> beats = new ArrayList<>(trace(timeline, 1));

		timeline.append(Collections.singletonList(step(BattleTimeline.Beat.LEAD_ATTACKS)));
		beats.addAll(trace(timeline, 10));

		assertEquals(BattleTimeline.Beat.FACE_OFF, beats.get(0));
		assertEquals(BattleTimeline.Beat.LEAD_ATTACKS, beats.get(BattleTimeline.BEAT_TICKS));
	}

	@Test
	public void finishingAWonBattleLaysTheFoeDownForTenSecondsThenEnds()
	{
		BattleTimeline timeline = BattleTimeline.open(Collections.singletonList(step(BattleTimeline.Beat.FACE_OFF)));
		trace(timeline, 20);

		timeline.finishWith(true);

		assertTrue(timeline.sealed());
		assertFalse(timeline.waiting());
		assertEquals(BattleTimeline.Beat.FINISH, trace(timeline, 1).get(0));
		assertTrue(timeline.knockedOut());
		assertFalse(timeline.leadLost());
		trace(timeline, BattleTimeline.KNOCKED_OUT_TICKS - 1);
		assertEquals(BattleTimeline.Phase.DOWN, timeline.phase());
		trace(timeline, 1);
		assertEquals(BattleTimeline.Phase.DONE, timeline.phase());
	}

	@Test
	public void finishingALostBattleLaysTheLeadDown()
	{
		BattleTimeline timeline = BattleTimeline.open(Collections.singletonList(step(BattleTimeline.Beat.FACE_OFF)));
		trace(timeline, 20);

		timeline.finishWith(false);
		trace(timeline, 1);

		assertTrue(timeline.leadLost());
		assertFalse(timeline.knockedOut());
		assertEquals(BattleTimeline.HOLD_TICKS, timeline.holdTicks());
	}

	@Test
	public void finishingWhileBeatsAreStillQueuedPlaysThemFirst()
	{
		BattleTimeline timeline = BattleTimeline.open(Arrays.asList(step(BattleTimeline.Beat.FACE_OFF),
			step(BattleTimeline.Beat.LEAD_ATTACKS)));

		timeline.finishWith(true);
		List<BattleTimeline.Beat> played = new ArrayList<>();
		for (BattleTimeline.Beat beat : trace(timeline, 40))
		{
			if (beat != null)
				played.add(beat);
		}

		assertEquals(Arrays.asList(BattleTimeline.Beat.FACE_OFF, BattleTimeline.Beat.LEAD_ATTACKS,
			BattleTimeline.Beat.FINISH), played);
	}

	@Test
	public void aSealedBattleIgnoresMoreBeatsAndASecondEnd()
	{
		BattleTimeline timeline = BattleTimeline.open(Collections.singletonList(step(BattleTimeline.Beat.FACE_OFF)));
		timeline.finishWith(true);

		timeline.append(Collections.singletonList(step(BattleTimeline.Beat.FOE_ATTACKS)));
		timeline.finishWith(false);

		assertEquals(Arrays.asList(BattleTimeline.Beat.FACE_OFF, BattleTimeline.Beat.FINISH),
			steps(timeline));
		assertFalse(timeline.leadLost());
	}

	private static List<BattleTimeline.Beat> steps(BattleTimeline timeline)
	{
		List<BattleTimeline.Beat> beats = new ArrayList<>();
		for (BattleTimeline.Step one : timeline.steps())
			beats.add(one.beat());

		return beats;
	}

	@Test
	public void aFallenLeadDoesNotEndTheBattle()
	{
		BattleTimeline timeline = BattleTimeline.open(Arrays.asList(step(BattleTimeline.Beat.FACE_OFF),
			step(BattleTimeline.Beat.FOE_ATTACKS), step(BattleTimeline.Beat.LEAD_FELL)));

		List<BattleTimeline.Beat> beats = trace(timeline, 60);

		assertTrue(beats.contains(BattleTimeline.Beat.LEAD_FELL));
		assertEquals(BattleTimeline.Phase.WAITING, timeline.phase());
		assertFalse(timeline.knockedOut());
	}

	@Test
	public void givingEveryBeatAtOnceIsTheSameAsAnAutomaticBattle()
	{
		List<BattleTimeline.Step> all = Arrays.asList(step(BattleTimeline.Beat.FACE_OFF),
			step(BattleTimeline.Beat.LEAD_ATTACKS), step(BattleTimeline.Beat.FOE_SPECIAL),
			step(BattleTimeline.Beat.LEAD_ATTACKS), step(BattleTimeline.Beat.FINISH));
		for (boolean won : new boolean[]{true, false})
		{
			BattleTimeline auto = new BattleTimeline(all, won);
			BattleTimeline fed = BattleTimeline.open(all.subList(0, 4));
			fed.finishWith(won);

			for (int tick = 0; tick < 120; tick++)
			{
				BattleTimeline.Step one = auto.tick(tick > 2);
				BattleTimeline.Step two = fed.tick(tick > 2);

				assertEquals("tick " + tick, one == null ? null : one.beat(), two == null ? null : two.beat());
				assertEquals("tick " + tick, auto.phase(), fed.phase());
				assertEquals("tick " + tick, auto.linesDue(), fed.linesDue());
				assertEquals("tick " + tick, auto.knockedOut(), fed.knockedOut());
			}

			assertEquals(BattleTimeline.Phase.DONE, auto.phase());
			assertNull(auto.tick(true));
		}
	}

	@Test
	public void anAutomaticBattleIsSealedAndNeverWaits()
	{
		BattleTimeline auto = new BattleTimeline(Collections.singletonList(step(BattleTimeline.Beat.FACE_OFF)), true);

		trace(auto, 200);

		assertTrue(auto.sealed());
		assertFalse(auto.waiting());
		assertEquals(BattleTimeline.Phase.DONE, auto.phase());
	}
}
