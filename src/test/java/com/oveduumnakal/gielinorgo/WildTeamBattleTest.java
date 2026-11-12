/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Plays automatic wild battles with the whole team: the next creature steps in when one faints. */
public class WildTeamBattleTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.MELEE);
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);

	private final List<Notice> notices = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		game = newGame(5);
	}

	private GoGame newGame(long seed)
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, GOBLIN, DRAGON));
		return new GoGame(table, new EvolutionTable(Arrays.asList(), table), new ChampionTable(Arrays.asList(), table),
			new Random(seed), () -> now, notices::add);
	}

	private static WildEntry wild(Species species, int level)
	{
		return new WildEntry(1, species, level, 1);
	}

	private Creature add(String key, int level)
	{
		Creature creature = new Creature(key, level, now);
		game.data().team.add(creature);
		return creature;
	}

	private static BattleTimeline timelineOf(BattleResult result)
	{
		BattleTimeline timeline = BattleTimeline.open(Arrays.asList(
			new BattleTimeline.Step(BattleTimeline.Beat.FACE_OFF, null)));
		WildScript script = WildScript.of(result);
		timeline.append(script.steps());
		timeline.finishWith(result.won());
		return timeline;
	}

	private static List<BattleTimeline.Beat> beatsPlayed(BattleTimeline timeline)
	{
		List<BattleTimeline.Beat> beats = new ArrayList<>();
		for (int tick = 0; tick < 2000 && timeline.phase() != BattleTimeline.Phase.DONE; tick++)
		{
			BattleTimeline.Step step = timeline.tick(true);
			if (step != null)
				beats.add(step.beat());
		}

		return beats;
	}

	/** @return a battle in which a level 1 lead loses and the strong second creature wins */
	private BattleResult leadLosesSecondWins()
	{
		game = newGame(5);
		add("chicken", 1);
		add("king black dragon", 60);
		return game.battle(wild(GOBLIN, 30));
	}

	@Test
	public void aSecondCreatureThatWinsPaysTheWinOnceAndBothHealthsAreWritten()
	{
		Creature lead = add("chicken", 1);
		Creature second = add("king black dragon", 60);
		WildEntry foe = wild(GOBLIN, 30);

		BattleResult result = game.battle(foe);

		assertTrue(result.won());
		assertEquals(1, result.switches());
		assertEquals(1, game.data().battlesWon);
		assertTrue(foe.battled());
		assertTrue(foe.weakened());
		assertEquals(0.0, lead.hpFraction, 0.0);
		assertEquals(0L, lead.xp);
		assertEquals(now, lead.hpStampMs);
		assertTrue(second.xp > 0L);
		assertTrue(second.hpFraction < 1.0);
		assertTrue(result.log().contains("Go, King Black Dragon!"));
		assertNull("only one battle per wild creature", game.battle(foe));
	}

	@Test
	public void aWholeTeamThatFaintsLosesAndIsWritten()
	{
		Creature first = add("chicken", 1);
		Creature second = add("chicken", 1);
		WildEntry foe = wild(DRAGON, 60);

		BattleResult result = game.battle(foe);

		assertFalse(result.won());
		assertEquals(1, result.switches());
		assertEquals(0, game.data().battlesWon);
		assertTrue(foe.battled());
		assertFalse(foe.weakened());
		assertEquals(0.0, first.hpFraction, 0.0);
		assertEquals(0.0, second.hpFraction, 0.0);
		assertEquals(0L, second.xp);
	}

	@Test
	public void aTiredLeadSitsOutAndTheFirstRestedCreatureOpens()
	{
		Creature lead = add("chicken", 1);
		lead.hpFraction = 0.1;
		lead.hpStampMs = now;
		Creature second = add("king black dragon", 60);

		BattleResult result = game.battle(wild(GOBLIN, 5));

		assertTrue(result.won());
		assertEquals(0, result.switches());
		assertEquals(0.1, lead.hpFraction, 0.0);
		assertEquals(0L, lead.xp);
		assertTrue(second.xp > 0L);
		assertFalse(result.leadOpened());
		assertEquals("King Black Dragon", result.opener().name());
	}

	@Test
	public void aTeamThatIsTooTiredDoesNotFight()
	{
		for (Creature creature : Arrays.asList(add("chicken", 1), add("goblin", 1)))
		{
			creature.hpFraction = 0.1;
			creature.hpStampMs = now;
		}

		WildEntry foe = wild(CHICKEN, 1);
		notices.clear();

		assertNull(game.battle(foe));
		assertFalse(foe.battled());
		assertTrue(notices.stream().anyMatch(notice -> notice.text().equals("Your team is too tired")));
	}

	@Test
	public void aTeamOfOneCreatureFightsExactlyAsTheSingleBattleDid()
	{
		for (long seed = 1; seed <= 12; seed++)
		{
			game = newGame(seed);
			Creature lead = add("goblin", 14);
			Combatant mine = new Combatant(GOBLIN, "Goblin", 14, 1.0, lead.rolls());
			Combatant foe = new Combatant(CHICKEN, 12, 1.0);
			BattleResult before = BattleSim.fight(mine, foe, new Random(seed));

			BattleResult now = game.battle(wild(CHICKEN, 12));

			assertEquals("seed " + seed, before.log(), now.log());
			assertEquals(before.won(), now.won());
			assertEquals(before.hpFractionLeft(), now.hpFractionLeft(), 0.0);
			assertEquals(before.turns(), now.turns());
			assertEquals(0, now.switches());
			List<BattleLine> cut = now.summary(6);
			assertEquals(before.summary(6).size(), cut.size());
			assertEquals(before.hpFractionLeft(), lead.hpFraction, 0.0);
		}
	}

	@Test
	public void theTimelineOfASwitchHasTheFallTheEntryAndTheRightEnd()
	{
		BattleResult result = leadLosesSecondWins();
		assertTrue(result.won());
		BattleTimeline timeline = timelineOf(result);

		List<BattleTimeline.Beat> beats = beatsPlayed(timeline);

		int fell = beats.indexOf(BattleTimeline.Beat.LEAD_FELL);
		assertTrue(fell > 0);
		assertEquals(BattleTimeline.Beat.LEAD_ENTERS, beats.get(fell + 1));
		assertEquals(BattleTimeline.Beat.FINISH, beats.get(beats.size() - 1));
		assertEquals(1, count(beats, BattleTimeline.Beat.FINISH));
		assertFalse(timeline.leadLost());
		assertEquals(BattleTimeline.Phase.DONE, timeline.phase());
	}

	private static int count(List<BattleTimeline.Beat> beats, BattleTimeline.Beat beat)
	{
		return (int) beats.stream().filter(one -> one == beat).count();
	}

	@Test
	public void aLostTeamBattleEndsWithTheLeadDown()
	{
		add("chicken", 1);
		add("chicken", 1);
		BattleResult result = game.battle(wild(DRAGON, 60));
		BattleTimeline timeline = timelineOf(result);

		List<BattleTimeline.Beat> beats = beatsPlayed(timeline);

		assertTrue(timeline.leadLost());
		assertEquals(BattleTimeline.Beat.FINISH, beats.get(beats.size() - 1));
		assertTrue(beats.contains(BattleTimeline.Beat.LEAD_ENTERS));
	}

	@Test
	public void theTextOfASwitchNamesTheFallAndTheNextCreatureAndMatchesTheBeats()
	{
		BattleResult result = leadLosesSecondWins();
		WildScript script = WildScript.of(result);
		List<BattleLine> lines = result.summary(6);

		List<String> texts = new ArrayList<>();
		lines.forEach(line -> texts.add(line.text()));

		assertEquals(script.lines().size(), lines.size());
		assertTrue(texts.contains("Chicken fainted"));
		assertTrue(texts.contains("Go, King Black Dragon!"));
		assertTrue(texts.indexOf("Chicken fainted") + 1 == texts.indexOf("Go, King Black Dragon!"));
		assertEquals(lines.size(), script.steps().size() + 2);
		assertFalse(texts.contains("Choose your next creature"));
	}

	@Test
	public void aLongTeamBattleIsCutDownDuelByDuel()
	{
		BattleResult result = leadLosesSecondWins();
		int blows = 0;
		for (BattleLine line : result.lines())
		{
			if (line.kind() == BattleLine.Kind.LEAD_HIT || line.kind() == BattleLine.Kind.FOE_HIT)
				blows++;
		}

		int shown = 0;
		for (BattleLine line : result.summary(6))
		{
			if (line.kind() == BattleLine.Kind.LEAD_HIT || line.kind() == BattleLine.Kind.FOE_HIT)
				shown++;
		}

		assertTrue(shown <= 2 * TeamScript.TEAM_HIT_BEATS);
		assertTrue(shown <= blows);
	}
}
