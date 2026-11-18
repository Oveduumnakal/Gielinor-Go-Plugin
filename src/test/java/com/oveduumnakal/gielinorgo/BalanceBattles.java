/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Sections 6 and 7 of the balance report: seeded battles and the moves. Fighters are built as the game builds a wild
 * foe, with the moves {@link MoveTable#movesAt} gives for their level and no stat rolls, and fight through
 * {@link BattleSim#fight} and {@link BattleSim#teamFight}, so the battle maths is the real one. Nothing is changed.
 */
final class BalanceBattles
{
	/** The creature level of the equal-level tables. */
	static final int LEVEL = 30;

	private static final long SEED = 20261118L;
	private static final int[] GAPS = {-10, -5, -2, -1, 0, 1, 2, 5, 10};
	private static final int[] WILD_LEVELS = {3, 5, 8, 12};
	private static final int[] TEAM_LEVELS = {30, 40, 50, 60, 70};
	private static final int NEUTRAL_LEVEL = 60;
	private static final int DAMAGE_SAMPLES = 400;

	private BalanceBattles()
	{
	}

	/**
	 * Builds a fighter the way the game builds a wild foe.
	 *
	 * @param data the measured data
	 * @param species the species
	 * @param level the creature level
	 * @return the fighter at full health, with the moves its level has unlocked
	 */
	static Combatant fighter(BalanceData data, Species species, int level)
	{
		return new Combatant(species, level, 1.0).moves(data.moves().movesAt(species, level));
	}

	/** The outcome of repeated fights between two creatures. */
	static final class Rate
	{
		private final double wins;
		private final double turns;

		Rate(double wins, double turns)
		{
			this.wins = wins;
			this.turns = turns;
		}

		/** @return the share of fights the first creature won */
		double wins()
		{
			return wins;
		}

		/** @return the mean number of attacks in a fight */
		double turns()
		{
			return turns;
		}
	}

	/**
	 * Fights two creatures many times with seeded randomness.
	 *
	 * @param data the measured data
	 * @param mine the first creature's species
	 * @param myLevel its level
	 * @param foe the second creature's species
	 * @param foeLevel its level
	 * @return the win share and mean length
	 */
	static Rate duel(BalanceData data, Species mine, int myLevel, Species foe, int foeLevel)
	{
		int won = 0;
		long turns = 0;
		Random seeds = new Random(SEED);
		for (int i = 0; i < data.fights(); i++)
		{
			BattleResult result = BattleSim.fight(fighter(data, mine, myLevel), fighter(data, foe, foeLevel),
				new Random(seeds.nextLong()));
			if (result.won())
				won++;

			turns += result.turns();
		}

		return new Rate(won / (double) data.fights(), turns / (double) data.fights());
	}

	private static Species neutral(CombatType style)
	{
		return new Species("Neutral " + style.label(), NEUTRAL_LEVEL, style);
	}

	/**
	 * Writes section 6.
	 *
	 * @param data the measured data
	 * @return the Markdown
	 */
	static String battles(BalanceData data)
	{
		StringBuilder out = new StringBuilder("## 6. Battles\n\n");
		out.append("Each cell is ").append(BalanceTable.cell(data.fights())).append(" seeded fights through ")
			.append("`BattleSim.fight` (one creature each, the game's auto attack choice, no items). ")
			.append("Fighters have no stat rolls and the moves their level unlocks. A cell is the win share of the ")
			.append("row creature. Mirror matches are about 50%.\n\n");
		out.append(triangle(data, true));
		out.append(triangle(data, false));
		out.append(gaps(data));
		out.append(starters(data));
		out.append(tiers(data));
		out.append(team(data));
		out.append(styleStrength(data, Rarity.UNCOMMON));
		out.append(styleStrength(data, Rarity.RARE));
		return out.toString();
	}

	private static String triangle(BalanceData data, boolean neutral)
	{
		StringBuilder out = new StringBuilder(neutral ? "### 6a. Styles against each other, neutral species\n\n"
			: "### 6b. Styles against each other, real species of combat level near " + NEUTRAL_LEVEL + "\n\n");
		out.append(neutral ? "A made-up species of combat level " + NEUTRAL_LEVEL + " per style with no stat factors, "
			: "The catchable species of each style nearest to combat level " + NEUTRAL_LEVEL
				+ ", with its own stat factors and moves, ")
			.append("both at creature level ").append(LEVEL).append(". A style is strong against two others (x")
			.append(CombatType.STRONG)
			.append(") and weak against two (x" + CombatType.WEAK + ").\n\n");
		String[] head = new String[CombatType.values().length + 2];
		head[0] = "Row beats column";
		for (CombatType style : CombatType.values())
			head[style.ordinal() + 1] = style.label();

		head[head.length - 1] = "Mean over the other four";
		BalanceTable table = new BalanceTable(head);
		for (CombatType row : CombatType.values())
		{
			Object[] cells = new Object[head.length];
			Species shown = data.nearest(row, NEUTRAL_LEVEL);
			cells[0] = row.label() + (neutral ? "" : " (" + shown.name() + ", Lv " + shown.level() + ")");
			double sum = 0;
			for (CombatType column : CombatType.values())
			{
				Species mine = neutral ? neutral(row) : data.nearest(row, NEUTRAL_LEVEL);
				Species foe = neutral ? neutral(column) : data.nearest(column, NEUTRAL_LEVEL);
				double wins = duel(data, mine, LEVEL, foe, LEVEL).wins();
				cells[column.ordinal() + 1] = BalanceTable.pct(wins);
				if (row != column)
					sum += wins;
			}

			cells[cells.length - 1] = BalanceTable.pct(sum / (CombatType.values().length - 1));
			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String gaps(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 6c. Level gaps, same species\n\n");
		out.append("A neutral Crush species against itself, the row creature at level ").append(LEVEL)
			.append(" plus the gap against a level " + LEVEL + " one.\n\n");
		BalanceTable table = new BalanceTable("Level gap", "Win share", "Mean attacks");
		Species crush = neutral(CombatType.CRUSH);
		for (int gap : GAPS)
		{
			Rate rate = duel(data, crush, LEVEL + gap, crush, LEVEL);
			table.row((gap > 0 ? "+" : "") + gap, BalanceTable.pct(rate.wins()), rate.turns());
		}

		return out.append(table).toString();
	}

	private static String starters(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 6d. The starters against wild Common creatures\n\n");
		out.append("Each starter at level " + StarterRules.LEVEL + " against the Common species of each style whose ")
			.append("combat level makes it appear at about the level in the second column (combat level = (level / 3) ")
			.append("squared). Target in the plan: about 65 to 80% against a style it beats, 20 to 35% against one ")
			.append("that beats it.\n\n");
		String[] head = new String[CombatType.values().length + 2];
		head[0] = "Starter";
		head[1] = "Wild level";
		for (CombatType style : CombatType.values())
			head[style.ordinal() + 2] = "vs " + style.label() + " (combat level)";

		BalanceTable table = new BalanceTable(head);
		for (String name : StarterRules.STARTERS)
		{
			Species starter = data.species().find(name);
			if (starter == null)
				continue;

			for (int level : WILD_LEVELS)
			{
				Object[] cells = new Object[head.length];
				cells[0] = starter.name() + " (" + starter.style().label() + ")";
				cells[1] = level;
				for (CombatType style : CombatType.values())
				{
					Species foe = data.nearest(style, (int) Math.round(level * level / 9.0));
					cells[style.ordinal() + 2] = BalanceTable.pct(duel(data, starter, StarterRules.LEVEL, foe,
						level).wins()) + " (" + foe.level() + ")";
				}

				table.row(cells);
			}
		}

		return out.append(table).toString();
	}

	private static String tiers(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 6e. A lead of one tier against a wild creature of another\n\n");
		out.append("The middle Crush species of each tier, both at the typical wild level of the foe's tier, so style ")
			.append("is held level and the tier alone speaks. Row is the lead's tier, column the wild creature's ")
			.append("tier.\n\n");
		String[] head = new String[Rarity.values().length + 1];
		head[0] = "Lead tier";
		Random random = new Random(SEED);
		int[] levels = new int[Rarity.values().length];
		for (Rarity rarity : Rarity.values())
		{
			levels[rarity.ordinal()] = BalanceProgress.typicalLevel(data, rarity, random);
			head[rarity.ordinal() + 1] = rarity.label() + " foe (Lv " + levels[rarity.ordinal()] + ")";
		}

		BalanceTable table = new BalanceTable(head);
		for (Rarity lead : Rarity.values())
		{
			Object[] cells = new Object[head.length];
			cells[0] = lead.label();
			for (Rarity foe : Rarity.values())
				cells[foe.ordinal() + 1] = BalanceTable.pct(duel(data, data.median(lead, CombatType.CRUSH),
					levels[foe.ordinal()], data.median(foe, CombatType.CRUSH), levels[foe.ordinal()]).wins());

			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String styleStrength(BalanceData data, Rarity tier)
	{
		int pairs = Math.max(10, data.fights() / 5);
		String name = tier.label();
		StringBuilder out = new StringBuilder("### 6g. Style strength with real species, " + name + " tier\n\n");
		out.append("Random pairs of real " + name + " species, " + pairs + " per cell, one fight each, both at ")
			.append("creature level " + LEVEL + ", with their own stat factors and moves. This is the check that no ")
			.append("style is better or worse than the others by more than about 10 points.\n\n");
		String[] head = new String[CombatType.values().length + 2];
		head[0] = "Row beats column";
		for (CombatType style : CombatType.values())
			head[style.ordinal() + 1] = style.label() + " (" + data.count(tier, style) + " species)";

		head[head.length - 1] = "Mean over the other four";
		BalanceTable table = new BalanceTable(head);
		for (CombatType row : CombatType.values())
		{
			Object[] cells = new Object[head.length];
			cells[0] = row.label();
			double sum = 0;
			int counted = 0;
			for (CombatType column : CombatType.values())
			{
				List<Species> mine = of(data, tier, row);
				List<Species> foes = of(data, tier, column);
				if (mine.isEmpty() || foes.isEmpty())
				{
					cells[column.ordinal() + 1] = "n/a";
					continue;
				}

				Random random = new Random(SEED);
				int won = 0;
				for (int i = 0; i < pairs; i++)
				{
					Species a = mine.get(random.nextInt(mine.size()));
					Species b = foes.get(random.nextInt(foes.size()));
					if (BattleSim.fight(fighter(data, a, LEVEL), fighter(data, b, LEVEL), new Random(random.nextLong()))
						.won())
						won++;
				}

				double share = won / (double) pairs;
				cells[column.ordinal() + 1] = BalanceTable.pct(share);
				if (row != column)
				{
					sum += share;
					counted++;
				}
			}

			cells[cells.length - 1] = counted == 0 ? "n/a" : BalanceTable.pct(sum / counted);
			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static List<Species> of(BalanceData data, Rarity tier, CombatType style)
	{
		List<Species> list = new ArrayList<>();
		for (Species each : data.tier(tier))
		{
			if (each.style() == style)
				list.add(each);
		}

		return list;
	}

	private static String team(BalanceData data)
	{
		Species boss = data.median(Rarity.LEGENDARY, CombatType.CRUSH);
		StringBuilder out = new StringBuilder("### 6f. A team of 6 against a boss-like creature\n\n");
		out.append("Six middle Rare Crush creatures fight one ").append(boss.name()).append(" (the middle Legendary ")
			.append("Crush species, combat level ").append(boss.level()).append(") at creature level 60, through ")
			.append("`BattleSim.teamFight`.\n\n");
		BalanceTable table = new BalanceTable("Team level", "Win share");
		Species member = data.median(Rarity.RARE, CombatType.CRUSH);
		for (int level : TEAM_LEVELS)
		{
			int won = 0;
			Random seeds = new Random(SEED);
			for (int i = 0; i < data.fights(); i++)
			{
				List<Combatant> mine = new ArrayList<>();
				for (int slot = 0; slot < SaveData.TEAM_SIZE; slot++)
					mine.add(fighter(data, member, level));

				if (BattleSim.teamFight(mine, Collections.singletonList(fighter(data, boss, 60)),
					new Random(seeds.nextLong())).won())
					won++;
			}

			table.row(level, BalanceTable.pct(won / (double) data.fights()));
		}

		return out.append(table).toString();
	}

	/**
	 * Works out the damage a move does each round against an equal-level creature of the same style, so the combat
	 * triangle is out of the way.
	 *
	 * @param move the move
	 * @return the mean damage times the accuracy
	 */
	static double expectedDamage(Move move)
	{
		Species attacker = neutral(move.style());
		Combatant mine = new Combatant(attacker, LEVEL, 1.0);
		Combatant foe = new Combatant(attacker, LEVEL, 1.0);
		Random random = new Random(SEED);
		double total = 0;
		for (int i = 0; i < DAMAGE_SAMPLES; i++)
			total += BattleSim.damage(mine, move, foe, random);

		return total / DAMAGE_SAMPLES * move.accuracy();
	}

	/**
	 * Tells whether a later move is pointless next to an earlier one: the same style, no more power, no more accuracy
	 * and no better effect.
	 *
	 * @param earlier the move learned first
	 * @param later the move learned later
	 * @return true when {@code later} is strictly worse in some way and better in none
	 */
	static boolean dominated(Move earlier, Move later)
	{
		if (earlier.style() != later.style() || earlier.id().equals(later.id()))
			return false;

		boolean noWorse = earlier.power() >= later.power() && earlier.accuracy() >= later.accuracy();
		boolean strictly = earlier.power() > later.power() || earlier.accuracy() > later.accuracy();
		Move.Effect have = later.effect();
		if (have != null && have.status() != null)
		{
			Move.Effect other = earlier.effect();
			boolean covered = other != null && have.status().equals(other.status()) && other.chance() >= have.chance();
			noWorse &= covered;
			strictly |= covered && other.chance() > have.chance();
		}

		return noWorse && strictly;
	}

	/**
	 * Writes section 7.
	 *
	 * @param data the measured data
	 * @return the Markdown
	 */
	static String moves(BalanceData data)
	{
		StringBuilder out = new StringBuilder("## 7. Moves\n\n");
		MoveTable table = data.moves();
		Map<String, Move> all = new LinkedHashMap<>();
		List<String> flagged = new ArrayList<>();
		for (String pool : table.poolNames())
		{
			List<Move> learned = new ArrayList<>();
			List<Integer> levels = new ArrayList<>();
			for (String entry : table.describePool(pool))
			{
				Move move = table.find(entry.substring(0, entry.indexOf('@')));
				if (move != null)
				{
					all.putIfAbsent(move.id(), move);
					learned.add(move);
					levels.add(Integer.parseInt(entry.substring(entry.indexOf('@') + 1)));
				}
			}

			for (int later = 0; later < learned.size(); later++)
			{
				for (int earlier = 0; earlier < later; earlier++)
				{
					if (levels.get(earlier) < levels.get(later) && dominated(learned.get(earlier), learned.get(later)))
					{
						flagged.add("`" + pool + "`: " + learned.get(later).name() + " (Lv " + levels.get(later)
							+ ") is worse than " + learned.get(earlier).name() + " (Lv " + levels.get(earlier) + ")");
						break;
					}
				}
			}
		}

		out.append("Expected damage per round of each move for a level ").append(LEVEL).append(" creature against an ")
			.append("equal-level creature of the same style (so no triangle), as the mean of ").append(DAMAGE_SAMPLES)
			.append(" real `BattleSim.damage` rolls times the move's accuracy. ").append(all.size())
			.append(" moves in ").append(table.poolCount()).append(" pools. The effect column is the chance a hit ")
			.append("causes a status.\n\n");
		List<Move> ordered = new ArrayList<>(all.values());
		ordered.sort(Comparator.comparingDouble(BalanceBattles::expectedDamage).reversed());
		BalanceTable moves = new BalanceTable("Move", "Style", "Power", "Accuracy", "Damage per round", "Effect");
		for (Move move : ordered)
		{
			Move.Effect effect = move.effect();
			String text = effect == null || effect.status() == null ? "-"
				: BalanceTable.pct(effect.chance()) + " " + effect.status().toLowerCase(Locale.ROOT);
			moves.row(move.name(), move.style().label(), move.power(), BalanceTable.pct(move.accuracy()),
				expectedDamage(move), text);
		}

		out.append(moves);
		out.append("### 7b. Moves worse than one learned earlier in the same pool\n\n");
		out.append("A later move of the same style with no more power, no more accuracy and no better effect than an ")
			.append("earlier one. " + flagged.size() + " found.\n\n");
		for (String line : flagged)
			out.append("- " + line + "\n");

		return out.append('\n').toString();
	}
}
