/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.Set;

/**
 * Section 2 of the balance report: how likely a throw is to catch, how many jars and how much walking a catch costs,
 * and where the jars come from. Every chance is a call to {@link CatchRules}, every jar bonus is read from
 * {@link JarType}, and the walking uses {@link GoGame#TILES_PER_JAR}.
 */
final class BalanceCatch
{
	/** The tiles per hour the report tries: casual with pauses, walking, running. */
	static final int[] TILES_PER_HOUR = {3_000, 6_000, 12_000};

	/** The jars the tables compare. A specialty jar is shown against a creature that has its trait. */
	static final JarType[] JARS = {JarType.BRONZE, JarType.IRON, JarType.RUNE, JarType.DRAGON, JarType.TIDE};

	private static final Set<Trait> WITH_TRAIT = Collections.singleton(Trait.AQUATIC);
	private static final Set<Trait> NO_TRAITS = Collections.emptySet();

	private BalanceCatch()
	{
	}

	/**
	 * Writes the section.
	 *
	 * @param data the measured data
	 * @return the Markdown
	 */
	static String section(BalanceData data)
	{
		StringBuilder out = new StringBuilder("## 2. Catch\n\n");
		out.append("Constants read: `CatchRules.WEAKENED_BONUS` ").append(CatchRules.WEAKENED_BONUS)
			.append(", `KNOCKED_OUT_BONUS` ").append(CatchRules.KNOCKED_OUT_BONUS)
			.append(", `BATTLE_BONUS` ").append(CatchRules.BATTLE_BONUS)
			.append(", `FLEE_CHANCE` ").append(CatchRules.FLEE_CHANCE)
			.append(", chance limits ").append(CatchRules.MIN_CHANCE).append(" to ").append(CatchRules.MAX_CHANCE)
			.append(". Jar bonuses:");
		for (JarType jar : JARS)
			out.append(" " + jar.label() + " " + jar.traitBonus() + ";");

		out.append(" jar unlocks (trainer level):");
		for (JarType jar : JARS)
		{
			if (!jar.specialty())
				out.append(" " + jar.label() + " " + jar.trainerLevel() + ";");
		}

		out.append("\n\nA specialty jar (the Tide jar stands for all 12) is shown against a creature with its trait; ")
			.append("against any other creature it is a Bronze jar. A creature that breaks out of a jar runs away ")
			.append("with the flee chance, so the jars per catch below are 1 divided by the throw chance, and the ")
			.append("chance to keep the creature is lower.\n\n");
		out.append(chanceTable(data, "2a. Chance per throw, standing", Kind.STANDING));
		out.append(chanceTable(data, "2b. Chance per throw, just lost a battle (weakened)", Kind.WEAKENED));
		out.append(chanceTable(data, "2c. Chance per throw, knocked out", Kind.KNOCKED_OUT));
		out.append(chanceTable(data, "2d. Chance per throw, in battle at 50% health", Kind.IN_BATTLE));
		out.append(rangeTable(data));
		out.append(costTable(data));
		out.append(keepTable(data));
		out.append(incomeTable(data));
		out.append(otherSources());
		out.append(dropMix());
		return out.toString();
	}

	/** The state a creature is in when a jar is thrown. */
	enum Kind
	{
		STANDING,
		WEAKENED,
		KNOCKED_OUT,
		IN_BATTLE
	}

	/**
	 * Gives the chance of one throw.
	 *
	 * @param jar the jar
	 * @param level the monster's combat level
	 * @param kind the state of the creature
	 * @return the chance
	 */
	static double chance(JarType jar, int level, Kind kind)
	{
		Set<Trait> traits = jar.specialty() ? WITH_TRAIT : NO_TRAITS;
		switch (kind)
		{
			case WEAKENED:
				return CatchRules.chance(jar, level, true, traits);
			case KNOCKED_OUT:
				return CatchRules.chance(jar, level, true, traits, true);
			case IN_BATTLE:
				return CatchRules.inBattle(jar, level, traits, 0.5);
			default:
				return CatchRules.chance(jar, level, false, traits);
		}
	}

	/**
	 * Gives the chance of keeping a creature that is thrown at until it is caught or flees.
	 *
	 * @param throwChance the chance of one throw
	 * @param throwsAllowed how many throws the player makes at most
	 * @return the chance it is caught within that many throws
	 */
	static double caughtWithin(double throwChance, int throwsAllowed)
	{
		double stays = (1.0 - throwChance) * (1.0 - CatchRules.FLEE_CHANCE);
		double total = 0;
		double reach = 1.0;
		for (int i = 0; i < throwsAllowed; i++)
		{
			total += reach * throwChance;
			reach *= stays;
		}

		return total;
	}

	private static String[] heading(String first)
	{
		String[] head = new String[JARS.length + 1];
		head[0] = first;
		for (int i = 0; i < JARS.length; i++)
			head[i + 1] = JARS[i].specialty() ? "Specialty (on trait)" : JARS[i].label();

		return head;
	}

	private static String tierLabel(BalanceData data, Rarity rarity)
	{
		return rarity.label() + " (Lv " + Math.round(data.meanLevel(rarity)) + ")";
	}

	private static String chanceTable(BalanceData data, String title, Kind kind)
	{
		StringBuilder out = new StringBuilder("### " + title + "\n\nAt the mean combat level of the tier.\n\n");
		BalanceTable table = new BalanceTable(heading("Tier"));
		for (Rarity rarity : Rarity.values())
		{
			Object[] cells = new Object[JARS.length + 1];
			cells[0] = tierLabel(data, rarity);
			for (int i = 0; i < JARS.length; i++)
				cells[i + 1] = BalanceTable.pct(chance(JARS[i], (int) Math.round(data.meanLevel(rarity)), kind));

			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String rangeTable(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 2e. Spread inside a tier, standing\n\n");
		out.append("The weakest, mean and strongest species of each tier, thrown at with a Bronze and a Dragon ")
			.append("jar.\n\n");
		BalanceTable table = new BalanceTable("Tier", "Lv min / mean / max", "Bronze min / mean / max",
			"Dragon min / mean / max");
		for (Rarity rarity : Rarity.values())
		{
			int low = data.minLevel(rarity);
			int mid = (int) Math.round(data.meanLevel(rarity));
			int high = data.maxLevel(rarity);
			table.row(rarity.label(), low + " / " + mid + " / " + high, triple(JarType.BRONZE, low, mid, high),
				triple(JarType.DRAGON, low, mid, high));
		}

		return out.append(table).toString();
	}

	private static String triple(JarType jar, int low, int mid, int high)
	{
		return BalanceTable.pct(chance(jar, low, Kind.STANDING)) + " / "
			+ BalanceTable.pct(chance(jar, mid, Kind.STANDING)) + " / "
			+ BalanceTable.pct(chance(jar, high, Kind.STANDING));
	}

	private static String costTable(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 2f. Jars and walking per catch\n\n");
		out.append("Jars per catch is 1 divided by the throw chance. Tiles per catch is that many jars at ")
			.append(GoGame.TILES_PER_JAR).append(" tiles per jar, if every jar came from walking.\n\n");
		BalanceTable table = new BalanceTable("Tier", "Bronze jars standing", "Bronze jars knocked out",
			"Dragon jars standing", "Dragon jars knocked out", "Tiles per catch, Bronze standing",
			"Tiles per catch, Bronze knocked out");
		for (Rarity rarity : Rarity.values())
		{
			int level = (int) Math.round(data.meanLevel(rarity));
			double standing = 1.0 / chance(JarType.BRONZE, level, Kind.STANDING);
			double knocked = 1.0 / chance(JarType.BRONZE, level, Kind.KNOCKED_OUT);
			table.row(tierLabel(data, rarity), standing, knocked, 1.0 / chance(JarType.DRAGON, level, Kind.STANDING),
				1.0 / chance(JarType.DRAGON, level, Kind.KNOCKED_OUT), standing * GoGame.TILES_PER_JAR,
				knocked * GoGame.TILES_PER_JAR);
		}

		return out.append(table).toString();
	}

	private static String keepTable(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 2g. Keeping the creature\n\n");
		out.append("The chance to catch it within 3 throws, counting that a miss lets it run away ")
			.append(BalanceTable.pct(CatchRules.FLEE_CHANCE)).append(" of the time.\n\n");
		BalanceTable table = new BalanceTable("Tier", "Bronze standing", "Bronze knocked out", "Iron standing",
			"Iron knocked out", "Dragon standing", "Dragon knocked out");
		for (Rarity rarity : Rarity.values())
		{
			int level = (int) Math.round(data.meanLevel(rarity));
			table.row(tierLabel(data, rarity),
				BalanceTable.pct(caughtWithin(chance(JarType.BRONZE, level, Kind.STANDING), 3)),
				BalanceTable.pct(caughtWithin(chance(JarType.BRONZE, level, Kind.KNOCKED_OUT), 3)),
				BalanceTable.pct(caughtWithin(chance(JarType.IRON, level, Kind.STANDING), 3)),
				BalanceTable.pct(caughtWithin(chance(JarType.IRON, level, Kind.KNOCKED_OUT), 3)),
				BalanceTable.pct(caughtWithin(chance(JarType.DRAGON, level, Kind.STANDING), 3)),
				BalanceTable.pct(caughtWithin(chance(JarType.DRAGON, level, Kind.KNOCKED_OUT), 3)));
		}

		return out.append(table).toString();
	}

	private static String incomeTable(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 2h. Bronze jars from walking and the standing catches they buy\n\n");
		out.append("One tile per game tick is 6,000 tiles an hour, running 12,000, 3,000 is casual play with pauses. ")
			.append("Catches per hour use the standing Bronze chance of 2a, with no other jar source.\n\n");
		String[] head = new String[Rarity.values().length + 2];
		head[0] = "Tiles per hour";
		head[1] = "Jars per hour";
		for (Rarity rarity : Rarity.values())
			head[rarity.ordinal() + 2] = rarity.label() + " catches per hour";

		BalanceTable table = new BalanceTable(head);
		for (int tiles : TILES_PER_HOUR)
		{
			double jars = tiles / (double) GoGame.TILES_PER_JAR;
			Object[] cells = new Object[head.length];
			cells[0] = tiles;
			cells[1] = jars;
			for (Rarity rarity : Rarity.values())
				cells[rarity.ordinal() + 2] = jars
					* chance(JarType.BRONZE, (int) Math.round(data.meanLevel(rarity)), Kind.STANDING);

			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String otherSources()
	{
		StringBuilder out = new StringBuilder("### 2i. Other jar sources\n\n");
		BalanceTable table = new BalanceTable("Source", "Amount", "Constant");
		table.row("Walking", "1 Bronze jar per " + GoGame.TILES_PER_JAR + " tiles", "`GoGame.TILES_PER_JAR`");
		table.row("OSRS level-up", GoGame.JARS_PER_LEVEL_UP + " Bronze jars", "`GoGame.JARS_PER_LEVEL_UP`");
		table.row("Battle win", BalanceTable.pct(GoGame.JAR_DROP_CHANCE) + " for one tier jar, see 2j",
			"`GoGame.JAR_DROP_CHANCE`");
		table.row("First catch of a UTC day", GoGame.STREAK_JARS + " Bronze jars", "`GoGame.STREAK_JARS`");
		table.row("Daily task (jar reward)", TaskCalendar.DAILY_JARS + " jars", "`TaskCalendar.DAILY_JARS`");
		table.row("Weekly task", TaskCalendar.WEEKLY_JARS + " jars and a " + TaskCalendar.WEEKLY_EGG.label() + " egg",
			"`TaskCalendar.WEEKLY_JARS`");
		table.row("City Champion rematch", GoGame.REMATCH_FIRST_JARS + " Dragon jars the first time, then "
			+ GoGame.REMATCH_DAILY_JARS + " a day", "`GoGame.REMATCH_FIRST_JARS`, `REMATCH_DAILY_JARS`");
		table.row("Task or streak egg with no free slot", GoGame.EGG_FALLBACK_JARS + " jars",
			"`GoGame.EGG_FALLBACK_JARS`");
		out.append(table);
		out.append("Specialty jars from skilling, per hour of training one skill (drops of " + BalanceSpawn.DROP_XP
			+ " XP, `CatchRules.XP_PER_SPECIALTY_JAR` " + (int) CatchRules.XP_PER_SPECIALTY_JAR + "):\n\n");
		BalanceTable skilling = new BalanceTable("OSRS XP per hour", "Specialty jars per hour");
		for (int xp : BalanceSpawn.XP_PER_HOUR)
			skilling.row(xp, xp / (double) BalanceSpawn.DROP_XP * CatchRules.specialtyDropChance(BalanceSpawn.DROP_XP));

		return out.append(skilling).toString();
	}

	private static String dropMix()
	{
		StringBuilder out = new StringBuilder("### 2j. Which tier jar a battle win drops\n\n");
		out.append("The drop picks two jars among the unlocked tier jars and keeps the lower, so the weaker jars drop ")
			.append("more. This mirrors `GoGame.randomUnlockedJar`, which is private.\n\n");
		BalanceTable table = new BalanceTable("Trainer level", "Bronze", "Iron", "Rune", "Dragon");
		int[] levels = {1, 10, 25, 50};
		for (int level : levels)
		{
			int unlocked = 0;
			for (JarType jar : JARS)
			{
				if (!jar.specialty() && level >= jar.trainerLevel())
					unlocked++;
			}

			Object[] cells = new Object[5];
			cells[0] = level;
			for (int i = 0; i < 4; i++)
				cells[i + 1] = i < unlocked ? BalanceTable.pct((2.0 * (unlocked - i) - 1.0)
					/ (unlocked * (double) unlocked)) : "-";

			table.row(cells);
		}

		return out.append(table).toString();
	}
}
