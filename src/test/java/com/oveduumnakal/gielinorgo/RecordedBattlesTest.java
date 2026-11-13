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

/**
 * Pins the results of team and friend battles, recorded before the battles were acted out on screen. They were
 * re-recorded when moves replaced the dragons' second style (issue 21): the verdict lines say the same, the health
 * left differs.
 */
public class RecordedBattlesTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final Species WIZARD = new Species("Wizard", 13, CombatType.MAGIC);
	private static final Species GREEN = new Species("Green dragon", 88, CombatType.MELEE);
	private static final Species IMP = new Species("Imp", 7, CombatType.MELEE);
	private static final Species KBD = new Species("King Black Dragon", 276, CombatType.MELEE);
	private static final long ANN = 11L;
	private static final long BOB = 22L;

	private static final String[] TEAM_BATTLES =
	{
		"LOST mine 0.0 0.0 0.0 theirs 0.0 0.0 286.0 134.0 lost / Gob beat Archer (Lv 16)"
		+ " / Gob lost to Wizard (Lv 25) / Green dragon beat Wizard (Lv 25)"
		+ " / Green dragon lost to Green dragon (Lv 45) / Wizard lost to Green dragon (Lv 45)",
		"WON mine 0.0 28.80000000000001 126.0 theirs 0.0 0.0 0.0 won / Gob beat Archer (Lv 16)"
		+ " / Gob lost to Wizard (Lv 25) / Green dragon beat Wizard (Lv 25)"
		+ " / Green dragon beat Green dragon (Lv 45)",
		"WON mine 0.0 28.80000000000001 149.0 theirs 0.0 0.0 0.0 0.0 won"
		+ " / Gob beat Archer (Lv 16) / Gob lost to Wizard (Lv 25)"
		+ " / Green dragon beat Wizard (Lv 25) / Green dragon beat Green dragon (Lv 45)"
		+ " / Green dragon beat Goblin (Lv 32)",
		"WON mine 0.0 337.6 172.0 theirs 0.0 0.0 0.0 won / Gob beat Archer (Lv 16)"
		+ " / Gob lost to Wizard (Lv 25) / Green dragon beat Wizard (Lv 25)"
		+ " / Green dragon beat Green dragon (Lv 45)",
		"WON mine 0.0 297.6 195.0 theirs 0.0 0.0 0.0 0.0 won / Gob beat Archer (Lv 16)"
		+ " / Gob beat Wizard (Lv 25) / Gob lost to Green dragon (Lv 45)"
		+ " / Green dragon beat Green dragon (Lv 45) / Green dragon beat Goblin (Lv 34)",
		"WON mine 0.0 410.6 218.0 theirs 0.0 0.0 0.0 won / Gob beat Archer (Lv 16)"
		+ " / Gob beat Wizard (Lv 25) / Gob lost to Green dragon (Lv 45)"
		+ " / Green dragon beat Green dragon (Lv 45)",
		"WON mine 0.0 304.40000000000003 241.0 theirs 0.0 0.0 0.0 0.0 won"
		+ " / Gob beat Archer (Lv 16) / Gob beat Wizard (Lv 25)"
		+ " / Gob lost to Green dragon (Lv 45) / Green dragon beat Green dragon (Lv 45)"
		+ " / Green dragon beat Goblin (Lv 36)"
	};

	private static final String[] FRIEND_BATTLES =
	{
		"ANN lost / Battle with Bob! / Your Green dragon beat Bob's Goblin (Lv 20)"
		+ " / Your Green dragon lost to Bob's King Black Dragon (Lv 35)"
		+ " / Your Goblin lost to Bob's King Black Dragon (Lv 35)"
		+ " / Your Imp lost to Bob's King Black Dragon (Lv 35) / Bob won this one BOB won"
		+ " / Battle with Ann! / Your Goblin lost to Ann's Green dragon (Lv 35)"
		+ " / Your King Black Dragon beat Ann's Green dragon (Lv 35)"
		+ " / Your King Black Dragon beat Ann's Goblin (Lv 12)"
		+ " / Your King Black Dragon beat Ann's Imp (Lv 9) / You beat Ann!",
		"ANN won / Battle with Bob! / Your Green dragon beat Bob's Goblin (Lv 20)"
		+ " / Your Green dragon beat Bob's King Black Dragon (Lv 35)"
		+ " / Your Green dragon beat Bob's Imp (Lv 15) / You beat Bob! BOB lost"
		+ " / Battle with Ann! / Your Goblin lost to Ann's Green dragon (Lv 55)"
		+ " / Your King Black Dragon lost to Ann's Green dragon (Lv 55)"
		+ " / Your Imp lost to Ann's Green dragon (Lv 55) / Ann won this one",
		"ANN won / Battle with Bob! / Your Green dragon beat Bob's Goblin (Lv 20)"
		+ " / Your Green dragon beat Bob's King Black Dragon (Lv 35)"
		+ " / Your Green dragon beat Bob's Imp (Lv 15) / You beat Bob! BOB lost"
		+ " / Battle with Ann! / Your Goblin lost to Ann's Green dragon (Lv 75)"
		+ " / Your King Black Dragon lost to Ann's Green dragon (Lv 75)"
		+ " / Your Imp lost to Ann's Green dragon (Lv 75) / Ann won this one",
		"ANN won / Battle with Bob! / Your Green dragon beat Bob's Goblin (Lv 20)"
		+ " / Your Green dragon beat Bob's King Black Dragon (Lv 35)"
		+ " / Your Green dragon beat Bob's Imp (Lv 15) / You beat Bob! BOB lost"
		+ " / Battle with Ann! / Your Goblin lost to Ann's Green dragon (Lv 95)"
		+ " / Your King Black Dragon lost to Ann's Green dragon (Lv 95)"
		+ " / Your Imp lost to Ann's Green dragon (Lv 95) / Ann won this one"
	};

	@Test
	public void teamFightsGiveTheRecordedResults()
	{
		for (int seed = 0; seed < TEAM_BATTLES.length; seed++)
			assertEquals("team seed " + seed, TEAM_BATTLES[seed], team(seed));
	}

	@Test
	public void friendBattlesGiveTheRecordedResults()
	{
		for (int seed = 1; seed <= FRIEND_BATTLES.length; seed++)
			assertEquals("friend seed " + seed, FRIEND_BATTLES[seed - 1], friends(seed));
	}

	/**
	 * Fights a team battle and describes it.
	 *
	 * @param seed the seed
	 * @return the winner, the health left on every creature and the lines
	 */
	static String team(long seed)
	{
		List<Combatant> mine = new ArrayList<>(Arrays.asList(new Combatant(GOBLIN, "Gob", 14 + 5 * (int) seed, 1.0),
			new Combatant(GREEN, 40 + 8 * (int) seed, 0.8, StatRolls.of(new int[]{3, -2, 5, 1}, "RECKLESS"))
				.moves(TestMoves.dragon()),
			new Combatant(WIZARD, 22 + 6 * (int) seed, 1.0)));
		List<Combatant> theirs = new ArrayList<>(Arrays.asList(new Combatant(ARCHER, 16, 1.0),
			new Combatant(WIZARD, 25, 1.0), new Combatant(GREEN, 45, 1.0).moves(TestMoves.dragon())));
		if (seed % 2 == 0)
			theirs.add(new Combatant(GOBLIN, 30 + (int) seed, 1.0));

		TeamBattleResult result = BattleSim.teamFight(mine, theirs, new Random(seed));
		return describe(result, mine, theirs);
	}

	/**
	 * Plays a friend battle from both sides and describes what the challenger saw and what the other saw.
	 *
	 * @param seed the seed
	 * @return the description
	 */
	static String friends(long seed)
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(IMP, GOBLIN, GREEN, KBD));
		List<GielinorGoPartyMessage> fromAnn = new ArrayList<>();
		List<GielinorGoPartyMessage> fromBob = new ArrayList<>();
		long now = 1_000_000L;
		FriendBattles ann = new FriendBattles(table, new Random(seed), () -> now, message ->
		{
			message.setMemberId(ANN);
			fromAnn.add(message);
		}, notice ->
		{
		});
		FriendBattles bob = new FriendBattles(table, new Random(seed + 100), () -> now, message ->
		{
			message.setMemberId(BOB);
			fromBob.add(message);
		}, notice ->
		{
		});
		SaveData annData = SaveData.fresh();
		SaveData bobData = SaveData.fresh();
		annData.team.add(new Creature("green dragon", 15 + 20 * (int) seed, now));
		annData.team.add(new Creature("goblin", 12, now));
		annData.team.add(new Creature("imp", 9, now));
		Creature rolled = new Creature("goblin", 20, now);
		rolled.setRolls(StatRolls.of(new int[]{4, 4, -3, 2}, "BRAVE"));
		bobData.team.add(rolled);
		bobData.team.add(new Creature("king black dragon", 35, now));
		bobData.team.add(new Creature("imp", 15, now));
		ann.hello(annData, false);
		bob.receive(fromAnn.get(fromAnn.size() - 1), BOB, "Ann", bobData, true);
		bob.hello(bobData, false);
		ann.receive(fromBob.get(fromBob.size() - 1), ANN, "Bob", annData, true);
		ann.challenge(BOB, "Bob", annData);
		TeamBattleResult bobSaw = bob.receive(fromAnn.get(fromAnn.size() - 1), BOB, "Ann", bobData, true);
		TeamBattleResult annSaw = ann.receive(fromBob.get(fromBob.size() - 1), ANN, "Bob", annData, true);
		return "ANN " + lines(annSaw) + " BOB " + lines(bobSaw);
	}

	private static String describe(TeamBattleResult result, List<Combatant> mine, List<Combatant> theirs)
	{
		StringBuilder text = new StringBuilder(result.won() ? "WON" : "LOST");
		text.append(" mine");
		for (Combatant one : mine)
			text.append(' ').append(one.hp());

		text.append(" theirs");
		for (Combatant one : theirs)
			text.append(' ').append(one.hp());

		return text + " " + lines(result);
	}

	private static String lines(TeamBattleResult result)
	{
		StringBuilder text = new StringBuilder(result.won() ? "won" : "lost");
		for (BattleLine line : result.lines())
			text.append(" / ").append(line.text());

		return text.toString();
	}
}
