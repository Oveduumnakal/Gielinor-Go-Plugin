/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins whole battles, line by line, recorded from the battle code as it was before the turn engine was introduced:
 * plain fights, dragons that breathe, level and speed ties, a fight that runs into the turn limit, tired starts, team
 * fights and the single duels friend battles are made of. The recording lives in the test resource
 * {@code battle-goldens.txt}: a failure here means the battle rules changed. When moves replaced the second style
 * (issue 21) the 49 cases that have a dragon, a wyvern or the always-casting mage in them were re-recorded on
 * purpose; the 40 others are exactly as first recorded. For issue 85 (Melee split into Stab, Slash and Crush)
 * the whole file was re-recorded with {@code MELEE} and {@code Melee} turned into {@code STAB} and {@code Stab} and nothing else: every roll, hit and
 * verdict is as before.
 */
public class BattleGoldensTest
{
	private static final String RESOURCE = "/battle-goldens.txt";
	private static final String CASE = "== ";

	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species ARCHER = new Species("Archer", 13, CombatType.RANGED);
	private static final Species WIZARD = new Species("Wizard", 13, CombatType.MAGIC);
	private static final Species GREEN = new Species("Green dragon", 88, CombatType.STAB);
	private static final Species WYVERN = new Species("Spitting Wyvern", 139, CombatType.STAB);
	private static final Species KBD = new Species("King Black Dragon", 276, CombatType.STAB);
	private static final Species TANK_MAGE = new Species("Tank mage", 1, CombatType.STAB);
	private static final StatRolls HARDY = StatRolls.of(new int[]{5, -5, 5, 0}, "HARDY");
	private static final Species TANK_ARCHER = new Species("Tank archer", 1, CombatType.RANGED);

	@Test
	public void everyRecordedBattleComesOutTheSame() throws IOException
	{
		Map<String, String> recorded = read();
		Map<String, String> now = record();

		assertEquals(recorded.keySet(), now.keySet());
		for (Map.Entry<String, String> one : recorded.entrySet())
			assertEquals(one.getKey(), one.getValue(), now.get(one.getKey()));
	}

	@Test
	public void theRecordingCoversTheTurnLimitTiesAndBreath() throws IOException
	{
		Map<String, String> recorded = read();
		boolean capped = false;
		boolean breathed = false;
		boolean iced = false;
		for (String body : recorded.values())
		{
			capped |= body.contains("turns=" + BattleSim.MAX_TURNS + " ");
			breathed |= body.contains("uses Dragonfire");
			iced |= body.contains("uses Icy breath");
		}

		assertTrue("a battle that reaches the turn limit is recorded", capped);
		assertTrue("fire breath is recorded", breathed);
		assertTrue("ice breath is recorded", iced);
		assertTrue(recorded.size() >= 40);
	}

	private static Map<String, String> read() throws IOException
	{
		Map<String, String> cases = new LinkedHashMap<>();
		try (InputStream in = BattleGoldensTest.class.getResourceAsStream(RESOURCE))
		{
			assertNotNull(RESOURCE + " is missing", in);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String name = null;
			StringBuilder body = new StringBuilder();
			for (String line = reader.readLine(); line != null; line = reader.readLine())
			{
				if (line.startsWith(CASE))
				{
					if (name != null)
						cases.put(name, body.toString());

					name = line.substring(CASE.length());
					body = new StringBuilder();
				}
				else
				{
					body.append(line).append('\n');
				}
			}

			if (name != null)
				cases.put(name, body.toString());
		}

		return cases;
	}

	/**
	 * Fights every case and describes it.
	 *
	 * @return the description of each case, by name, in a fixed order
	 */
	static Map<String, String> record()
	{
		Map<String, String> cases = new LinkedHashMap<>();
		recordPlain(cases);
		recordDragons(cases);
		recordTies(cases);
		recordLimit(cases);
		recordTired(cases);
		recordTeams(cases);
		recordDuels(cases);
		return cases;
	}

	private static void recordPlain(Map<String, String> cases)
	{
		Species[] pool = {GOBLIN, ARCHER, WIZARD, KBD};
		for (int seed = 0; seed < 10; seed++)
		{
			Random pick = new Random(seed * 17L + 3);
			Species mine = pool[pick.nextInt(pool.length)];
			Species foe = pool[pick.nextInt(pool.length)];
			int myLevel = 1 + pick.nextInt(70);
			int foeLevel = 1 + pick.nextInt(70);
			Combatant one = new Combatant(mine, myLevel, 1.0);
			Combatant two = new Combatant(foe, foeLevel, 1.0);
			cases.put("plain " + seed, fight(one, two, seed));
		}
	}

	private static void recordDragons(Map<String, String> cases)
	{
		for (int seed = 0; seed < 6; seed++)
		{
			Combatant dragon = green(30 + 4 * seed, 1.0);
			Combatant goblin = new Combatant(GOBLIN, 40, 1.0);
			cases.put("dragon is mine " + seed, fight(dragon, goblin, seed));
			cases.put("dragon is theirs " + seed,
				fight(new Combatant(WIZARD, 35, 1.0), green(35 + seed, 1.0), seed));
		}

		for (int seed = 0; seed < 4; seed++)
		{
			cases.put("wyvern " + seed,
				fight(wyvern(50, 1.0), new Combatant(ARCHER, 55, 1.0), seed));
			cases.put("two dragons " + seed,
				fight(green(45, 1.0), wyvern(45, 1.0), seed));
		}
	}

	private static void recordTies(Map<String, String> cases)
	{
		for (int seed = 0; seed < 8; seed++)
		{
			cases.put("same species same level " + seed,
				fight(new Combatant(GOBLIN, 20, 1.0), new Combatant(GOBLIN, 20, 1.0), seed));
			cases.put("same level other species " + seed,
				fight(new Combatant(GOBLIN, 25, 1.0), new Combatant(ARCHER, 25, 1.0), seed));
		}

		Combatant quick = new Combatant(GOBLIN, 18, 1.0, StatRolls.of(new int[]{0, 0, 0, 5}, "HASTY"));
		Combatant slow = new Combatant(GOBLIN, 20, 1.0);
		cases.put("faster but lower level", fight(quick, slow, 4));
		Combatant rolled = new Combatant(KBD, 30, 1.0, StatRolls.of(new int[]{3, -2, 5, 1}, "RECKLESS"));
		cases.put("rolled against plain", fight(rolled, new Combatant(WIZARD, 44, 1.0), 9));
	}

	private static void recordLimit(Map<String, String> cases)
	{
		for (int seed = 0; seed < 6; seed++)
		{
			Combatant mine = mage();
			Combatant foe = new Combatant(TANK_ARCHER, "Archer", 2, 1.0, HARDY);
			cases.put("turn limit " + seed, fight(mine, foe, seed));
		}
	}

	private static void recordTired(Map<String, String> cases)
	{
		for (int seed = 0; seed < 4; seed++)
		{
			Combatant tired = new Combatant(GOBLIN, 30, 0.3 + 0.1 * seed);
			cases.put("tired start " + seed, fight(tired, new Combatant(ARCHER, 28, 1.0), seed));
		}
	}

	private static void recordTeams(Map<String, String> cases)
	{
		for (int seed = 0; seed < 8; seed++)
		{
			List<Combatant> mine = new ArrayList<>(Arrays.asList(
				new Combatant(GOBLIN, "Gob", 14 + 4 * seed, 1.0),
				green(38 + 3 * seed, 0.9, StatRolls.of(new int[]{3, -2, 5, 1}, "RECKLESS")),
				wyvern(30 + seed, 0.7),
				new Combatant(WIZARD, 22 + 5 * seed, 1.0)));
			List<Combatant> theirs = new ArrayList<>(Arrays.asList(
				new Combatant(ARCHER, 20, 1.0),
				new Combatant(WIZARD, 25 + seed, 1.0),
				green(45, 1.0)));
			if (seed % 2 == 1)
				theirs.add(new Combatant(KBD, 28 + seed, 1.0));

			cases.put("team " + seed, team(mine, theirs, seed));
		}

		for (int seed = 0; seed < 3; seed++)
		{
			List<Combatant> mine = new ArrayList<>(Arrays.asList(
				mage(),
				new Combatant(GOBLIN, 12, 1.0)));
			List<Combatant> theirs = new ArrayList<>(Arrays.asList(
				new Combatant(TANK_ARCHER, "Archer", 2, 1.0, HARDY),
				new Combatant(GOBLIN, 12, 1.0)));
			cases.put("team with a turn limit " + seed, team(mine, theirs, seed));
		}

		for (int seed = 0; seed < 4; seed++)
		{
			List<Combatant> mine = new ArrayList<>(Arrays.asList(new Combatant(GOBLIN, 20, 1.0),
				new Combatant(GOBLIN, 20, 1.0)));
			List<Combatant> theirs = new ArrayList<>(Arrays.asList(new Combatant(GOBLIN, 20, 1.0),
				new Combatant(ARCHER, 20, 1.0)));
			cases.put("team of equals " + seed, team(mine, theirs, seed));
		}
	}

	private static void recordDuels(Map<String, String> cases)
	{
		for (int seed = 0; seed < 6; seed++)
		{
			for (boolean firstIsMine : new boolean[]{true, false})
			{
				Combatant first = green(40 + seed, 1.0);
				Combatant second = new Combatant(GOBLIN, 44, 1.0);
				List<BattleLine> hits = new ArrayList<>();
				boolean won = BattleSim.duelWon(first, second, new Random(seed), hits, firstIsMine);
				StringBuilder text = new StringBuilder("first won=" + won + " hp " + first.hp() + " " + second.hp()
					+ "\n");
				for (BattleLine hit : hits)
					text.append(line(hit));

				cases.put("duel " + seed + (firstIsMine ? " mine" : " theirs"), text.toString());
			}
		}

		for (int seed = 0; seed < 4; seed++)
		{
			Combatant first = new Combatant(ARCHER, 33, 1.0);
			Combatant second = new Combatant(ARCHER, 33, 1.0);
			boolean won = BattleSim.duelWon(first, second, new Random(seed));
			cases.put("duel without lines " + seed,
				"first won=" + won + " hp " + first.hp() + " " + second.hp() + "\n");
		}
	}

	private static Combatant green(int level, double health)
	{
		return new Combatant(GREEN, level, health).moves(TestMoves.dragon());
	}

	private static Combatant green(int level, double health, StatRolls rolls)
	{
		return new Combatant(GREEN, level, health, rolls).moves(TestMoves.dragon());
	}

	private static Combatant wyvern(int level, double health)
	{
		return new Combatant(WYVERN, level, health).moves(TestMoves.wyvern());
	}

	private static Combatant mage()
	{
		return new Combatant(TANK_MAGE, "Mage", 2, 1.0, HARDY).moves(TestMoves.alwaysMagic());
	}

	private static String fight(Combatant mine, Combatant foe, long seed)
	{
		BattleResult result = BattleSim.fight(mine, foe, new Random(seed));
		StringBuilder text = new StringBuilder("won=" + result.won() + " turns=" + result.turns() + " hpLeft="
			+ result.hpFractionLeft() + " hp " + mine.hp() + " " + foe.hp() + "\n");
		for (BattleLine line : result.lines())
			text.append(line(line));

		return text.toString();
	}

	private static String team(List<Combatant> mine, List<Combatant> theirs, long seed)
	{
		TeamBattleResult result = BattleSim.teamFight(mine, theirs, new Random(seed));
		StringBuilder text = new StringBuilder("won=" + result.won() + " mine");
		for (Combatant one : mine)
			text.append(' ').append(one.hp());

		text.append(" theirs");
		for (Combatant one : theirs)
			text.append(' ').append(one.hp());

		text.append('\n');
		for (BattleLine line : result.lines())
			text.append(line(line));

		for (Duel duel : result.duels())
		{
			text.append("duel ").append(duel.mineIndex()).append(' ').append(duel.theirIndex()).append(' ')
				.append(duel.mineWon()).append(' ').append(duel.mineSpecies()).append(' ')
				.append(duel.theirSpecies()).append(' ')
				.append(duel.mineGilded()).append('\n');
			for (BattleLine hit : duel.hits())
				text.append("  ").append(line(hit));

			text.append("  out ").append(line(duel.knockOut()));
		}

		return text.toString();
	}

	private static String line(BattleLine line)
	{
		return line.text() + " | " + line.tone() + " " + line.overPlayer() + " " + line.kind() + " " + line.special()
			+ " " + line.style() + " " + line.icy() + "\n";
	}
}
