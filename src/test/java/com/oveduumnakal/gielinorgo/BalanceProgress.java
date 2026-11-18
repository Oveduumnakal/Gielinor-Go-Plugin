/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sections 3 and 4 of the balance report: how fast creatures and the trainer level up, and how eggs drop and hatch.
 * The XP splits are measured by calling {@link XpRules#share} and {@link XpRules#shareEqually} on a real team, so the
 * online rule (the lead counts twice) and the offline rule (everyone equally) are both read from the code.
 */
final class BalanceProgress
{
	/** The creature levels the tables reach for. */
	static final int[] LEVELS = {10, 30, 50, 70, 99};

	/** The OSRS XP per hour the level tables try. */
	static final int[] XP_PER_HOUR = {50_000, 100_000, 300_000};

	/** Battle wins per hour the egg and XP tables try. */
	static final int[] WINS_PER_HOUR = {10, 30, 60};

	private static final long SEED = 20261118L;
	private static final long PROBE = 420_000L;
	private static final int[] TEAMS = {1, 3, 6};
	private static final int[] TRAINER_LEVELS = {10, 25, 50, 75, 99};

	private BalanceProgress()
	{
	}

	/**
	 * Writes section 3.
	 *
	 * @param data the measured data
	 * @return the Markdown
	 */
	static String levels(BalanceData data)
	{
		StringBuilder out = new StringBuilder("## 3. Levels\n\n");
		out.append("Constants read: `XpRules.OSRS_XP_PER_CREATURE_XP` ").append(XpRules.OSRS_XP_PER_CREATURE_XP)
			.append(" (so 10 OSRS XP is 1 creature XP), creature XP for level L is 50 x (L-1)^2 and trainer XP is ")
			.append("10 x (L-1)^2 (`XpRules.xpForLevel`, `trainerXpForLevel`), `BondRules` bonus at 5 hearts ")
			.append(BondRules.xpBonus(BondRules.MAX_HEARTS)).append("%.\n\n");
		out.append(shares());
		for (int xp : XP_PER_HOUR)
			out.append(hoursTable(xp));

		out.append(battleXp(data));
		out.append(trainer(data));
		return out.toString();
	}

	/**
	 * Measures the share of a hand-out one creature of a team gets.
	 *
	 * @param size the team size
	 * @param place the creature's place in the team, 0 for the lead
	 * @param equally true for the equal split, false for the online split where the lead counts twice
	 * @return the share, from 0 to 1
	 */
	static double shareOf(int size, int place, boolean equally)
	{
		List<Creature> team = new ArrayList<>();
		for (int i = 0; i < size; i++)
			team.add(new Creature("probe" + i, 1, 0L));

		long[] before = new long[size];
		for (int i = 0; i < size; i++)
			before[i] = team.get(i).xp;

		if (equally)
			XpRules.shareEqually(team, PROBE, new Random(SEED));
		else
			XpRules.share(team, PROBE);

		return (team.get(place).xp - before[place]) / (double) PROBE;
	}

	private static String shares()
	{
		StringBuilder out = new StringBuilder("### 3a. Share of XP, online and offline\n\n");
		out.append("Online (`XpRules.share`) the lead counts twice. XP gained while away ")
			.append("(`XpRules.shareEqually`, #219) is split equally. The difference matters for a big team.\n\n");
		BalanceTable table = new BalanceTable("Team size", "Online, lead", "Online, each other", "Equal split, each",
			"Lead: equal divided by online");
		for (int size : TEAMS)
		{
			double lead = shareOf(size, 0, false);
			double other = size == 1 ? 0.0 : shareOf(size, 1, false);
			double equal = shareOf(size, 0, true);
			table.row(size, BalanceTable.pct(lead), size == 1 ? "-" : BalanceTable.pct(other), BalanceTable.pct(equal),
				BalanceTable.number(equal / lead));
		}

		return out.append(table).toString();
	}

	private static String hoursTable(int osrsXpPerHour)
	{
		double creatureXp = osrsXpPerHour / (double) XpRules.OSRS_XP_PER_CREATURE_XP;
		StringBuilder out = new StringBuilder("### 3b. Hours to a creature level at " + BalanceTable.cell(osrsXpPerHour)
			+ " OSRS XP per hour\n\n");
		out.append("Hours of OSRS play (no battle XP, no bond bonus) for a creature to reach the level, by team size ")
			.append("and the creature's place. The last column is the offline split of a team of 6.\n\n");
		BalanceTable table = new BalanceTable("Level", "Creature XP", "Team 1 lead", "Team 3 lead", "Team 3 other",
			"Team 6 lead", "Team 6 other", "Offline, team 6");
		for (int level : LEVELS)
		{
			double need = XpRules.xpForLevel(level);
			table.row(level, (int) need, hours(need, creatureXp, shareOf(1, 0, false)),
				hours(need, creatureXp, shareOf(3, 0, false)), hours(need, creatureXp, shareOf(3, 1, false)),
				hours(need, creatureXp, shareOf(6, 0, false)), hours(need, creatureXp, shareOf(6, 1, false)),
				hours(need, creatureXp, shareOf(6, 0, true)));
		}

		return out.append(table).toString();
	}

	private static String hours(double need, double creatureXpPerHour, double share)
	{
		return BalanceTable.minutes(need / (creatureXpPerHour * share) * 60.0);
	}

	private static String battleXp(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 3c. XP from battle wins, and how it compares\n\n");
		out.append("A win over a wild creature of level L pays the creature that finished the fight 20 + 5 x L ")
			.append("creature XP (literals in `GoGame.rewardWin`, which is private), and it is not shared. The ")
			.append("right-hand columns ")
			.append("turn that into the OSRS XP per hour that would pay the same to a solo lead.\n\n");
		BalanceTable table = new BalanceTable("Foe level", "XP per win", "10 wins/h", "30 wins/h", "60 wins/h",
			"OSRS XP/h equal to 30 wins/h");
		int[] foeLevels = {10, 20, 30, 50, 70};
		for (int level : foeLevels)
		{
			long perWin = 20L + 5L * level;
			table.row(level, (int) perWin, perWin * WINS_PER_HOUR[0], perWin * WINS_PER_HOUR[1],
				perWin * WINS_PER_HOUR[2], perWin * WINS_PER_HOUR[1] * XpRules.OSRS_XP_PER_CREATURE_XP);
		}

		out.append(table);
		out.append("Hours for a solo lead to reach level 30 and 50 at 100,000 OSRS XP per hour, with and without ")
			.append("battle wins at foe level 30:\n\n");
		BalanceTable mix = new BalanceTable("Wins per hour", "Level 30", "Level 50");
		long perWin = 20L + 5L * 30;
		for (int wins : new int[] {0, 10, 30, 60})
		{
			double perHour = 100_000.0 / XpRules.OSRS_XP_PER_CREATURE_XP + wins * (double) perWin;
			mix.row(wins, BalanceTable.minutes(XpRules.xpForLevel(30) / perHour * 60.0),
				BalanceTable.minutes(XpRules.xpForLevel(50) / perHour * 60.0));
		}

		return out.append(mix).toString();
	}

	private static String trainer(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 3d. Trainer level\n\n");
		out.append("A catch pays `CatchRules.catchXp` trainer XP (20 + the creature's level), a win 5 + the foe's ")
			.append("level (literal in `GoGame.rewardWin`), a daily task " + TaskCalendar.DAILY_TRAINER_XP)
			.append(". Columns: catches alone needed to reach the level, with the wild level of a Common, a Rare ")
			.append("and a Legendary creature (the mean `CatchRules.wildLevel` of the tier's mean combat level).\n\n");
		Random random = new Random(SEED);
		int common = typicalLevel(data, Rarity.COMMON, random);
		int rare = typicalLevel(data, Rarity.RARE, random);
		int legendary = typicalLevel(data, Rarity.LEGENDARY, random);
		BalanceTable table = new BalanceTable("Trainer level", "Trainer XP", "Common (Lv " + common + ")",
			"Rare (Lv " + rare + ")", "Legendary (Lv " + legendary + ")", "Common, a win and a catch each");
		for (int level : TRAINER_LEVELS)
		{
			double need = XpRules.trainerXpForLevel(level);
			table.row(level, (int) need, catches(need, common), catches(need, rare), catches(need, legendary),
				need / (CatchRules.catchXp(common) + 5.0 + common));
		}

		return out.append(table).toString();
	}

	private static double catches(double need, int level)
	{
		return need / CatchRules.catchXp(level);
	}

	/**
	 * Finds the mean wild level of a tier.
	 *
	 * @param data the measured data
	 * @param rarity the tier
	 * @param random the source of the level spread
	 * @return the mean level of {@link CatchRules#wildLevel} at the tier's mean combat level, rounded
	 */
	static int typicalLevel(BalanceData data, Rarity rarity, Random random)
	{
		int combat = (int) Math.round(data.meanLevel(rarity));
		long total = 0;
		int samples = 400;
		for (int i = 0; i < samples; i++)
			total += CatchRules.wildLevel(combat, random);

		return (int) Math.round(total / (double) samples);
	}

	/**
	 * Writes section 4.
	 *
	 * @param data the measured data
	 * @return the Markdown
	 */
	static String eggs(BalanceData data)
	{
		StringBuilder out = new StringBuilder("## 4. Eggs\n\n");
		out.append("Constants read: `GoGame.EGG_DROP_CHANCE` ").append(GoGame.EGG_DROP_CHANCE)
			.append(", `ROAMING_EGG_CHANCE` ").append(GoGame.ROAMING_EGG_CHANCE)
			.append(", `WildTracker.ROAMING_ONE_IN` ").append(WildTracker.ROAMING_ONE_IN)
			.append(", `BossRules.LATER_CHANCE` ").append(BossRules.LATER_CHANCE)
			.append(", `SaveData.MAX_EGGS` ").append(SaveData.MAX_EGGS)
			.append(" eggs carried (every carried egg loses the same tiles, so they hatch side by side). ")
			.append("An egg hatches at the level of its tier (`Rarity.hatchLevel`) into a random species of ")
			.append("its tier.\n\n");
		out.append(hatchTable());
		out.append(dropTable());
		out.append(rateTable(data));
		out.append(worthTable(data));
		return out.toString();
	}

	private static String hatchTable()
	{
		StringBuilder out = new StringBuilder("### 4a. Tiles and time to hatch\n\n");
		BalanceTable table = new BalanceTable("Tier", "Tiles", "At 3,000 tiles/h", "At 6,000 tiles/h",
			"At 12,000 tiles/h");
		for (Rarity rarity : Rarity.values())
		{
			double tiles = rarity.eggTiles();
			table.row(rarity.label(), rarity.eggTiles(),
				BalanceTable.minutes(tiles / BalanceCatch.TILES_PER_HOUR[0] * 60),
				BalanceTable.minutes(tiles / BalanceCatch.TILES_PER_HOUR[1] * 60),
				BalanceTable.minutes(tiles / BalanceCatch.TILES_PER_HOUR[2] * 60));
		}

		return out.append(table).toString();
	}

	/**
	 * Gives the chance that one battle win drops an egg, counting roaming creatures.
	 *
	 * @param events whether spawn events are on, which is what lets a creature be roaming
	 * @return the chance
	 */
	static double eggPerWin(boolean events)
	{
		double roaming = events ? 1.0 / WildTracker.ROAMING_ONE_IN : 0.0;
		return (1.0 - roaming) * GoGame.EGG_DROP_CHANCE + roaming * GoGame.ROAMING_EGG_CHANCE;
	}

	private static String dropTable()
	{
		StringBuilder out = new StringBuilder("### 4b. Egg chance per battle win\n\n");
		out.append("The egg is of the beaten creature's tier, or one tier up when the creature was roaming. The ")
			.append("chance does not depend on the tier. Roaming creatures need spawn events on.\n\n");
		BalanceTable table = new BalanceTable("Beaten tier", "Plain egg", "Roaming egg", "Per win, events off",
			"Per win, events on", "Wins per egg, events on");
		for (Rarity rarity : Rarity.values())
			table.row(rarity.label(), rarity.label() + " " + BalanceTable.pct(GoGame.EGG_DROP_CHANCE),
				rarity.next().label() + " " + BalanceTable.pct(GoGame.ROAMING_EGG_CHANCE),
				BalanceTable.pct(eggPerWin(false)), BalanceTable.pct(eggPerWin(true)), 1.0 / eggPerWin(true));

		return out.append(table).toString();
	}

	/**
	 * Works out which tier of wild creature a fight is against, if the player met every species equally often before
	 * the tier factor. It is an illustration: the real mix depends on where the player plays.
	 *
	 * @param data the measured data
	 * @param rarity the tier
	 * @return the share of fights against that tier
	 */
	static double illustrativeShare(BalanceData data, Rarity rarity)
	{
		double total = 0;
		for (Rarity each : Rarity.values())
			total += data.tier(each).size() * each.wildFactor();

		return total <= 0 ? 0.0 : data.tier(rarity).size() * rarity.wildFactor() / total;
	}

	private static String rateTable(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 4c. Eggs per hour, by tier of egg\n\n");
		out.append("Illustration only: it assumes the fights are split across tiers as the species counts times ")
			.append("`Rarity.wildFactor` say, with events on (roaming creatures). Real play depends on the area. ")
			.append("Each cell is the mean hours until one egg of that tier.\n\n");
		String[] head = new String[Rarity.values().length + 2];
		head[0] = "Wins per hour";
		head[1] = "Eggs per hour, any tier";
		for (Rarity rarity : Rarity.values())
			head[rarity.ordinal() + 2] = rarity.label() + " egg every";

		BalanceTable table = new BalanceTable(head);
		double roaming = 1.0 / WildTracker.ROAMING_ONE_IN;
		for (int wins : WINS_PER_HOUR)
		{
			Object[] cells = new Object[head.length];
			cells[0] = wins;
			cells[1] = wins * eggPerWin(true);
			for (Rarity egg : Rarity.values())
			{
				double perWin = 0;
				for (Rarity beaten : Rarity.values())
				{
					double share = illustrativeShare(data, beaten);
					if (beaten == egg)
						perWin += share * (1.0 - roaming) * GoGame.EGG_DROP_CHANCE;

					if (beaten.next() == egg)
						perWin += share * roaming * GoGame.ROAMING_EGG_CHANCE;
				}

				cells[egg.ordinal() + 2] = BalanceTable.minutes(1.0 / (perWin * wins) * 60.0);
			}

			table.row(cells);
		}

		out.append(table);
		out.append("Share of fights by tier in that illustration:");
		for (Rarity rarity : Rarity.values())
			out.append(' ').append(rarity.label()).append(' ').append(BalanceTable.pct(illustrativeShare(data, rarity)))
				.append(';');

		return out.append("\n\n").toString();
	}

	private static String worthTable(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 4d. What a hatch is worth\n\n");
		out.append("A hatch starts at the level of its tier (`Rarity.hatchLevel`); a wild creature of the same tier ")
			.append("is caught at its wild level. Hours are at 100,000 OSRS XP per hour for a solo lead (team of 1), ")
			.append("counted from the hatch level.\n\n");
		BalanceTable table = new BalanceTable("Tier", "Hatch level", "Creature XP at hatch", "Training it saves",
			"Species", "Evolve at least once", "Mean first evolution level", "Hours to that level",
			"Hours to level 30", "Typical wild level", "Hours to reach the wild level");
		Random random = new Random(SEED);
		double perHour = 100_000.0 / XpRules.OSRS_XP_PER_CREATURE_XP;
		for (Rarity rarity : Rarity.values())
		{
			int evolving = 0;
			double levels = 0;
			for (Species each : data.tier(rarity))
			{
				EvolutionTable.Step step = data.evolutions().next(each.key());
				if (step != null)
				{
					evolving++;
					levels += step.level();
				}
			}

			double first = evolving == 0 ? Double.NaN : levels / evolving;
			int wild = typicalLevel(data, rarity, random);
			long start = XpRules.xpForLevel(rarity.hatchLevel());
			table.row(rarity.label(), rarity.hatchLevel(), start, BalanceTable.minutes(start / perHour * 60.0),
				data.tier(rarity).size(), BalanceTable.pct(evolving / (double) Math.max(1,
				data.tier(rarity).size())), first,
				Double.isNaN(first) ? "n/a"
					: BalanceTable.minutes(Math.max(0, XpRules.xpForLevel((int) Math.round(first)) - start)
						/ perHour * 60.0),
				BalanceTable.minutes(Math.max(0, XpRules.xpForLevel(30) - start) / perHour * 60.0), wild,
				BalanceTable.minutes(Math.max(0, XpRules.xpForLevel(wild) - start) / perHour * 60.0));
		}

		return out.append(table).toString();
	}
}
