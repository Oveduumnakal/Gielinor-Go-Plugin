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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks the order and the length of a team battle's scene, and the duels it is made from. */
public class TeamScriptTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final int PINNED_3V3_TICKS = 71;
	private static final int PINNED_6V6_TICKS = 155;

	@Test
	public void aDuelIsCutDownToItsFirstMiddleAndLastHit()
	{
		List<BattleLine> hits = hits(7);

		List<BattleLine> shown = TeamScript.cut(hits);

		assertEquals(TeamScript.TEAM_HIT_BEATS, shown.size());
		assertSame(hits.get(0), shown.get(0));
		assertSame(hits.get(3), shown.get(1));
		assertSame(hits.get(6), shown.get(2));
		assertEquals(3, TeamScript.cut(hits(3)).size());
		assertEquals(1, TeamScript.cut(hits(1)).size());
		assertTrue(TeamScript.cut(hits(0)).isEmpty());
	}

	@Test
	public void aKeptBlowShowsTheTagsAsTheyAreAfterTheDroppedLinesThatChangedThem()
	{
		List<BattleLine> hits = hits(7);
		hits.set(1, hits.get(1).withTags(null, Status.POISON));
		hits.set(2, new BattleLine("poison wears off", Tone.INFO, false, BattleLine.Kind.STATUS)
			.withTags(null, null));

		List<BattleLine> shown = TeamScript.cut(hits);
		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(shown);

		assertEquals(3, steps.size());
		assertTrue(!steps.get(0).tagged());
		assertTrue(steps.get(1).tagged());
		assertEquals(null, steps.get(1).foeStatus());
		assertTrue(steps.get(2).tagged());
		assertEquals(null, steps.get(2).foeStatus());
	}

	@Test
	public void aKeptBlowShowsAStatusPutOnByADroppedLine()
	{
		List<BattleLine> hits = hits(7);
		hits.set(1, new BattleLine("poisoned", Tone.INFO, false, BattleLine.Kind.STATUS)
			.withTags(null, Status.POISON));

		List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(TeamScript.cut(hits));

		assertEquals(Status.POISON, steps.get(1).foeStatus());
		assertEquals(Status.POISON, steps.get(2).foeStatus());
	}

	@Test
	public void aSingleDuelPlaysEnterHitsDownAndExit()
	{
		TeamScript script = TeamScript.of(Arrays.asList(duel(0, 0, true, 5)), false);

		assertKinds(script, TeamScript.Kind.ENTER_FOE, TeamScript.Kind.ENTER_MINE, TeamScript.Kind.HIT,
			TeamScript.Kind.HIT, TeamScript.Kind.HIT, TeamScript.Kind.DOWN, TeamScript.Kind.EXIT);
		assertEquals(TeamScript.Side.FOE, script.entries().get(5).side());
		assertEquals(TeamScript.Side.FOE, script.entries().get(6).side());
		assertEquals("goblin", script.entries().get(0).species());
		assertEquals("archer", script.entries().get(1).species());
	}

	@Test
	public void theLeadThatIsAlreadyBesideThePlayerDoesNotFadeIn()
	{
		TeamScript script = TeamScript.of(Arrays.asList(duel(0, 0, true, 2)), true);

		assertKinds(script, TeamScript.Kind.ENTER_FOE, TeamScript.Kind.HIT, TeamScript.Kind.HIT,
			TeamScript.Kind.DOWN, TeamScript.Kind.EXIT);
	}

	@Test
	public void onlyTheSideThatLostIsSwappedAndTheWinnerCarriesOn()
	{
		List<Duel> duels = Arrays.asList(duel(0, 0, true, 1), duel(0, 1, false, 1), duel(1, 1, true, 1));

		TeamScript script = TeamScript.of(duels, true);

		assertKinds(script,
			TeamScript.Kind.ENTER_FOE, TeamScript.Kind.HIT, TeamScript.Kind.DOWN, TeamScript.Kind.EXIT,
			TeamScript.Kind.ENTER_FOE, TeamScript.Kind.HIT, TeamScript.Kind.DOWN, TeamScript.Kind.EXIT,
			TeamScript.Kind.ENTER_MINE, TeamScript.Kind.HIT, TeamScript.Kind.DOWN, TeamScript.Kind.EXIT);
		assertEquals(TeamScript.Side.FOE, script.entries().get(2).side());
		assertEquals(TeamScript.Side.MINE, script.entries().get(6).side());
		assertEquals(TeamScript.Side.FOE, script.entries().get(10).side());
	}

	@Test
	public void aLostBattleEndsWithTheWinningOpponentFadingOut()
	{
		TeamScript script = TeamScript.of(Arrays.asList(duel(0, 0, false, 1)), true);

		assertKinds(script, TeamScript.Kind.ENTER_FOE, TeamScript.Kind.HIT, TeamScript.Kind.DOWN,
			TeamScript.Kind.EXIT, TeamScript.Kind.EXIT);
		assertEquals(TeamScript.Side.MINE, script.entries().get(2).side());
		assertEquals(TeamScript.Side.MINE, script.entries().get(3).side());
		assertEquals(TeamScript.Side.FOE, script.entries().get(4).side());
	}

	@Test
	public void aHitCarriesItsStyleAndTheLineThatFloatsForIt()
	{
		BattleLine hit = new BattleLine("-9 (20/30)", Tone.GOOD, false, BattleLine.Kind.LEAD_HIT, false,
			CombatType.RANGED, false);
		Duel duel = new Duel(new Combatant(GOBLIN, 5, 1.0), 0, new Combatant(ARCHER, 5, 1.0), 0,
			Arrays.asList(hit), knockOut(), true);

		TeamScript.Entry entry = TeamScript.of(Arrays.asList(duel), true).entries().get(1);

		assertEquals(TeamScript.Kind.HIT, entry.kind());
		assertSame(hit, entry.line());
		assertEquals(BattleTimeline.Beat.LEAD_ATTACKS, entry.step().beat());
		assertEquals(CombatType.RANGED, entry.step().style());
		assertEquals(BattleTimeline.BEAT_TICKS, entry.ticks());
	}

	@Test
	public void aThreeVersusThreeBattleIsFiveDuelsAndAboutFortyFiveSeconds()
	{
		TeamScript script = TeamScript.of(duelsOfLongestBattle(3), false);

		assertEquals(5, countKind(script, TeamScript.Kind.DOWN));
		assertEquals(5 * TeamScript.TEAM_HIT_BEATS, countKind(script, TeamScript.Kind.HIT));
		assertEquals(PINNED_3V3_TICKS, script.totalTicks());
		assertTrue("about 43 seconds at 0.6 seconds a tick, near the plan's 45", script.totalTicks() * 0.6 <= 50.0);
	}

	@Test
	public void aSixVersusSixBattleIsElevenDuelsAndAboutNinetyFiveSeconds()
	{
		TeamScript script = TeamScript.of(duelsOfLongestBattle(6), false);

		assertEquals(11, countKind(script, TeamScript.Kind.DOWN));
		assertEquals(PINNED_6V6_TICKS, script.totalTicks());
		assertTrue("about 93 seconds at 0.6 seconds a tick, near the plan's 95", script.totalTicks() * 0.6 <= 105.0);
	}

	@Test
	public void aRunStartsEachEntryOnceAndFloatsTheLineWhenTheBlowLands()
	{
		Duel duel = duel(0, 0, true, 2);
		TeamScript script = TeamScript.of(Arrays.asList(duel), true);
		TeamScript.Run run = script.run();
		List<String> events = new ArrayList<>();
		int ticks = 0;
		TeamScript.Tick tick = run.tick();
		while (!tick.done())
		{
			ticks++;
			if (tick.started() != null)
				events.add(ticks + ":" + tick.started().kind());

			if (tick.line() != null)
				events.add(ticks + ":line " + tick.line().text());

			tick = run.tick();
		}

		assertEquals(script.totalTicks(), ticks);
		int beat = BattleTimeline.BEAT_TICKS;
		int firstHit = 1 + TeamScript.FADE_TICKS;
		assertEquals(Arrays.asList(
			"1:ENTER_FOE",
			firstHit + ":HIT",
			(firstHit + TeamScript.LINE_DELAY_TICKS) + ":line " + duel.hits().get(0).text(),
			(firstHit + beat) + ":HIT",
			(firstHit + beat + TeamScript.LINE_DELAY_TICKS) + ":line " + duel.hits().get(1).text(),
			(firstHit + 2 * beat) + ":DOWN",
			(firstHit + 2 * beat) + ":line " + duel.knockOut().text(),
			(firstHit + 2 * beat + TeamScript.TEAM_HOLD_TICKS) + ":EXIT"), events);
		assertTrue("a finished run stays finished", run.tick().done());
	}

	@Test
	public void hitsFloatOverTheCreatureThatWasHitAndKnockOutsOverThePlayer()
	{
		BattleLine mine = new BattleLine("-9", Tone.GOOD, false, BattleLine.Kind.LEAD_HIT);
		BattleLine theirs = new BattleLine("Gob -4", Tone.BAD, true, BattleLine.Kind.FOE_HIT);

		assertEquals(Notice.Anchor.STAGE_FOE, TeamScript.noticeOf(mine).anchor());
		assertEquals(Notice.Anchor.STAGE_LEAD, TeamScript.noticeOf(theirs).anchor());
		assertEquals(Notice.Anchor.HEAD, TeamScript.noticeOf(knockOut()).anchor());
		assertTrue(TeamScript.noticeOf(knockOut()).overPlayer());
		assertTrue("an arena foe has no monster, so it falls back to the player", TeamScript.noticeOf(mine)
			.overPlayer());
	}

	@Test
	public void aRealBattleHasADuelPerKnockOutAndTheRightLoser()
	{
		for (int seed = 0; seed < 60; seed++)
		{
			List<Combatant> mine = team(seed, 3);
			List<Combatant> theirs = team(seed + 500, 3);
			TeamBattleResult result = BattleSim.teamFight(mine, theirs, new Random(seed));
			List<Duel> duels = result.duels();
			int myLosses = 0;
			int theirLosses = 0;

			assertTrue(duels.size() >= 3 && duels.size() <= 5);
			for (Duel duel : duels)
			{
				assertTrue(result.lines().contains(duel.knockOut()));
				assertTrue(duel.hits().size() <= TeamScript.TEAM_HIT_BEATS);
				assertEquals(duel.mineWon(), duel.knockOut().text().contains(" beat "));
				myLosses += duel.mineWon() ? 0 : 1;
				theirLosses += duel.mineWon() ? 1 : 0;
			}

			assertEquals(result.won(), duels.get(duels.size() - 1).mineWon());
			assertEquals(result.won() ? 3 : myLosses, result.won() ? theirLosses : 3);
			assertEquals(duels.size(), result.lines().size());
		}
	}

	@Test
	public void theLinesAroundTheKnockOutsAreSplitForTheScene()
	{
		BattleLine opening = new BattleLine("Challenging!", Tone.INFO, true);
		BattleLine verdict = new BattleLine("You beat them!", Tone.GOOD, true);
		Duel duel = duel(0, 0, true, 1);
		TeamBattleResult result = new TeamBattleResult(true, Arrays.asList(opening, duel.knockOut(), verdict),
			Arrays.asList(duel));

		assertEquals(Arrays.asList(opening), result.leadIn());
		assertEquals(Arrays.asList(verdict), result.leadOut());
		assertTrue(result.withLeadFirst(true).leadFirst());
		assertTrue(!result.leadFirst());
		assertEquals(Arrays.asList(opening, duel.knockOut(), verdict), new TeamBattleResult(true,
			Arrays.asList(opening, duel.knockOut(), verdict)).leadIn());
		assertTrue(new TeamBattleResult(true, Arrays.asList(opening)).duels().isEmpty());
	}

	private static List<Combatant> team(long seed, int size)
	{
		Random random = new Random(seed);
		List<Combatant> team = new ArrayList<>();
		for (int i = 0; i < size; i++)
			team.add(new Combatant(random.nextBoolean() ? GOBLIN : ARCHER, 5 + random.nextInt(40), 1.0));

		return team;
	}

	private static List<Duel> duelsOfLongestBattle(int size)
	{
		List<Duel> duels = new ArrayList<>();
		int mine = 0;
		int theirs = 0;
		for (int i = 0; i < 2 * size - 1; i++)
		{
			boolean won = i % 2 == 0;
			duels.add(duel(mine, theirs, won, 5));
			if (won)
				theirs++;
			else
				mine++;
		}

		return duels;
	}

	private static Duel duel(int mine, int theirs, boolean mineWon, int hits)
	{
		return new Duel(new Combatant(ARCHER, 9, 1.0), mine, new Combatant(GOBLIN, 9, 1.0), theirs, hits(hits),
			knockOut(), mineWon);
	}

	private static BattleLine knockOut()
	{
		return new BattleLine("Archer beat Goblin (Lv 9)", Tone.GOOD, true);
	}

	private static List<BattleLine> hits(int count)
	{
		List<BattleLine> hits = new ArrayList<>();
		for (int i = 0; i < count; i++)
		{
			boolean mine = i % 2 == 0;
			hits.add(new BattleLine("hit " + i, mine ? Tone.GOOD : Tone.BAD, !mine,
				mine ? BattleLine.Kind.LEAD_HIT : BattleLine.Kind.FOE_HIT, false, CombatType.STAB, false));
		}

		return hits;
	}

	private static int countKind(TeamScript script, TeamScript.Kind kind)
	{
		int count = 0;
		for (TeamScript.Entry entry : script.entries())
		{
			if (entry.kind() == kind)
				count++;
		}

		return count;
	}

	private static void assertKinds(TeamScript script, TeamScript.Kind... kinds)
	{
		List<TeamScript.Kind> actual = new ArrayList<>();
		for (TeamScript.Entry entry : script.entries())
			actual.add(entry.kind());

		assertEquals(Arrays.asList(kinds), actual);
	}
}
