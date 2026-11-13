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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks the script of a team battle the player steers: it waits, takes turns, and turns each one into entries. */
public class OpenScriptTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	private static List<TeamScript.Kind> kinds(List<TeamScript.Entry> entries)
	{
		List<TeamScript.Kind> kinds = new ArrayList<>();
		for (TeamScript.Entry entry : entries)
			kinds.add(entry.kind());

		return kinds;
	}

	@Test
	public void anOpenRunWaitsWhenItHasPlayedEverythingAndNeverEnds()
	{
		TeamScript.Run run = TeamScript.openRun();
		run.append(Collections.singletonList(TeamScript.enterFoe("goblin")));

		TeamScript.Tick first = run.tick();
		TeamScript.Tick second = run.tick();

		assertEquals("goblin", first.started().species());
		assertFalse(first.waiting());
		assertTrue(second.waiting() || run.waiting());
		for (int i = 0; i < 300; i++)
		{
			TeamScript.Tick idle = run.tick();
			assertFalse(idle.done());
			assertNull(idle.started());
		}

		assertTrue(run.waiting());
	}

	@Test
	public void entriesAppendedWhileWaitingPlayAtOnce()
	{
		TeamScript.Run run = TeamScript.openRun();
		run.tick();
		assertTrue(run.waiting());

		run.append(Collections.singletonList(TeamScript.enterMine("archer", true)));
		TeamScript.Tick tick = run.tick();

		assertEquals(TeamScript.Kind.ENTER_MINE, tick.started().kind());
		assertTrue(tick.started().gilded());
		assertFalse(run.waiting() && tick.started() == null);
	}

	@Test
	public void aClosedRunEndsAfterItsLastEntry()
	{
		TeamScript.Run run = TeamScript.openRun();
		run.append(Collections.singletonList(TeamScript.enterFoe("goblin")));
		run.close();

		assertFalse(run.open());
		assertFalse(run.tick().done());
		assertTrue(run.tick().done());
	}

	@Test
	public void anAutomaticRunIsClosedAndEndsAsBefore()
	{
		Combatant mine = new Combatant(DRAGON, 40, 1.0);
		TeamBattleResult result = BattleSim.teamFight(new ArrayList<>(Collections.singletonList(mine)),
			new ArrayList<>(Collections.singletonList(new Combatant(GOBLIN, 5, 1.0))), new Random(1));
		TeamScript.Run run = TeamScript.of(result.duels(), true).run();

		assertFalse(run.open());
		int ticks = 0;
		while (!run.tick().done())
			ticks++;

		assertEquals(TeamScript.of(result.duels(), true).totalTicks(), ticks);
	}

	@Test
	public void aBlowTurnIsTheBlowsAndNothingElse()
	{
		BattleState state = new BattleState(new ArrayList<>(Collections.singletonList(new Combatant(GOBLIN, 20, 1.0))),
			new ArrayList<>(Collections.singletonList(new Combatant(GOBLIN, 20, 1.0))), new Random(1),
			BattleState.Mode.CHAMPION);

		TurnResult turn = state.step(BattleChoice.attack());
		List<TeamScript.Entry> entries = TeamScript.entriesOf(turn);

		assertEquals(Arrays.asList(TeamScript.Kind.HIT, TeamScript.Kind.HIT), kinds(entries));
		List<BattleLine> hits = turn.hits();
		assertSame(hits.get(0), entries.get(0).line());
		assertEquals(BattleTimeline.BEAT_TICKS, entries.get(0).ticks());
	}

	@Test
	public void aVoluntarySwitchFadesTheOldCreatureOutAndTheNewOneIn()
	{
		BattleState state = new BattleState(new ArrayList<>(Arrays.asList(new Combatant(GOBLIN, 20, 1.0),
			new Combatant(ARCHER, 20, 1.0).gilded(true))),
			new ArrayList<>(Collections.singletonList(new Combatant(GOBLIN, 20, 1.0))), new Random(1),
			BattleState.Mode.CHAMPION);

		List<TeamScript.Entry> entries = TeamScript.entriesOf(state.step(BattleChoice.switchTo(1)));

		assertEquals(Arrays.asList(TeamScript.Kind.EXIT, TeamScript.Kind.ENTER_MINE, TeamScript.Kind.HIT),
			kinds(entries));
		assertEquals(TeamScript.Side.MINE, entries.get(0).side());
		assertEquals("archer", entries.get(1).species());
		assertTrue(entries.get(1).gilded());
	}

	@Test
	public void aFaintedCreatureGoesDownFadesOutAndIsReplacedForFree()
	{
		BattleState state = new BattleState(new ArrayList<>(Arrays.asList(new Combatant(GOBLIN, 2, 1.0),
			new Combatant(ARCHER, 20, 1.0))),
			new ArrayList<>(Collections.singletonList(new Combatant(DRAGON, 60, 1.0))), new Random(1),
			BattleState.Mode.CHAMPION);

		TurnResult fall = state.step(BattleChoice.attack());
		List<TeamScript.Entry> down = TeamScript.entriesOf(fall);
		List<TeamScript.Entry> free = TeamScript.entriesOf(state.step(BattleChoice.switchTo(1)));

		assertEquals(Arrays.asList(TeamScript.Kind.HIT, TeamScript.Kind.DOWN, TeamScript.Kind.EXIT), kinds(down));
		assertEquals(TeamScript.Side.MINE, down.get(1).side());
		Duel duel = fall.duel();
		assertSame(duel.knockOut(), down.get(1).line());
		assertEquals(Collections.singletonList(TeamScript.Kind.ENTER_MINE), kinds(free));
	}

	@Test
	public void aBeatenOpponentFadesOutAndTheNextOneComesIn()
	{
		BattleState state = new BattleState(new ArrayList<>(Collections.singletonList(new Combatant(DRAGON, 60, 1.0))),
			new ArrayList<>(Arrays.asList(new Combatant(GOBLIN, 5, 1.0), new Combatant(ARCHER, 5, 1.0))),
			new Random(1), BattleState.Mode.CHAMPION);

		List<TeamScript.Entry> entries = TeamScript.entriesOf(state.step(BattleChoice.attack()));

		assertEquals(Arrays.asList(TeamScript.Kind.HIT, TeamScript.Kind.DOWN, TeamScript.Kind.EXIT,
			TeamScript.Kind.ENTER_FOE), kinds(entries));
		assertEquals(TeamScript.Side.FOE, entries.get(1).side());
		assertEquals("archer", entries.get(3).species());
	}

	@Test
	public void theEndOfALostBattleFadesTheWinnerOut()
	{
		List<TeamScript.Entry> lost = TeamScript.endEntries(false);
		assertEquals(Collections.singletonList(TeamScript.Kind.EXIT), kinds(lost));
		assertEquals(TeamScript.Side.FOE, lost.get(0).side());
		assertTrue(TeamScript.endEntries(true).isEmpty());
	}
}
