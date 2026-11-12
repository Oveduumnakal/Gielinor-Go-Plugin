/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks that the opening line is kept for the scene but takes no text slot, and that the match-up has a tooltip. */
public class OpeningLineHiddenTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);

	private static BattleResult longBattle()
	{
		return BattleSim.fight(new Combatant(GOBLIN, 10, 1.0), new Combatant(GOBLIN, 10, 1.0), new Random(5));
	}

	@Test
	public void theOpeningStaysFirstSoTheSceneCanFaceOff()
	{
		List<BattleLine> shown = longBattle().playbackSummary(6);

		assertEquals(BattleLine.Kind.OPENING, shown.get(0).kind());
		assertEquals(1, shown.stream().filter(line -> line.kind() == BattleLine.Kind.OPENING).count());
	}

	@Test
	public void theOpeningTakesNoneOfTheSixTextSlots()
	{
		BattleResult result = longBattle();
		assertTrue("the test needs a long battle", result.lines().size() > 7);

		List<BattleLine> shown = result.playbackSummary(6);
		List<BattleLine> real = new ArrayList<>(shown.subList(1, shown.size()));
		List<BattleLine> all = result.lines();
		List<BattleLine> body = all.subList(1, all.size());

		assertEquals(6, real.size());
		assertEquals(body.subList(0, 3), real.subList(0, 3));
		assertEquals(body.subList(body.size() - 3, body.size()), real.subList(3, 6));
	}

	@Test
	public void aShortBattleKeepsEveryLine()
	{
		BattleResult result = new BattleResult(true, Arrays.asList(
			new BattleLine("a", Tone.INFO, false, BattleLine.Kind.OPENING),
			new BattleLine("b", Tone.INFO, false, BattleLine.Kind.LEAD_HIT)), 1.0, 1);

		assertEquals(result.lines(), result.playbackSummary(6));
	}

	@Test
	public void aBattleWithoutAnOpeningSummarisesAsBefore()
	{
		List<BattleLine> lines = new ArrayList<>();
		for (int i = 0; i < 10; i++)
			lines.add(new BattleLine("x" + i, Tone.INFO, false, BattleLine.Kind.LEAD_HIT));

		BattleResult result = new BattleResult(true, lines, 1.0, 10);

		assertEquals(result.summary(6), result.playbackSummary(6));
	}

	@Test
	public void theTextWaitsOneBeatForTheTextlessFaceOff()
	{
		BattleTimeline.Step face = new BattleTimeline.Step(BattleTimeline.Beat.FACE_OFF, null);
		BattleTimeline.Step hit = new BattleTimeline.Step(BattleTimeline.Beat.LEAD_ATTACKS, CombatType.MELEE);
		BattleTimeline timeline = new BattleTimeline(Arrays.asList(face, hit), true);

		timeline.tick(true);
		for (int tick = 0; tick <= BattleTimeline.BEAT_TICKS; tick++)
		{
			assertFalse("tick " + tick, timeline.linesDue());
			timeline.tick(true);
		}

		assertTrue(timeline.linesDue());
	}

	@Test
	public void theStyleSentenceSaysWhatItBeatsAndWhatBeatsIt()
	{
		assertEquals("Melee beats Ranged, loses to Magic", CombatType.MELEE.matchup());
		assertEquals("Ranged beats Magic, loses to Melee", CombatType.RANGED.matchup());
		assertEquals("Magic beats Melee, loses to Ranged", CombatType.MAGIC.matchup());
	}
}
