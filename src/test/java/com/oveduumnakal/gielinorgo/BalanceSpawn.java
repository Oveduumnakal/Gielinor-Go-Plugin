/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Section 1 of the balance report: how often a creature turns wild, from monsters in view and from skilling. Every
 * chance comes from {@link SpawnCalendar#monsterChance} and {@link SkillingRules#chance}, and the waits use
 * {@link WildTracker#MEMORY_MS} and {@link WildTracker#COOLDOWN_MS}, so a tuning change shows in the next run.
 */
final class BalanceSpawn
{
	/** The "Wild creature rarity" settings the report tries. */
	static final int[] ONE_IN = {2, 4, 8, 16, 50};

	/** How many monsters are in view in each table. */
	static final int[] MONSTERS = {1, 3, 10};

	/** The OSRS XP per hour the skilling tables try. */
	static final int[] XP_PER_HOUR = {20_000, 50_000, 100_000, 300_000};

	/** The size of one XP drop the skilling tables assume, in OSRS XP. */
	static final int DROP_XP = 100;

	private static final long SEED = 20261118L;
	private static final int SAMPLES = 20_000;
	private static final double MS_PER_MINUTE = 60_000.0;

	/** A date and times fixed by the report, so nothing reads a clock. */
	private static final class Fixed implements SpawnClock
	{
		private final LocalDate date;
		private final LocalTime local;

		Fixed(LocalDate date, LocalTime local)
		{
			this.date = date;
			this.local = local;
		}

		@Override
		public LocalDate utcDate()
		{
			return date;
		}

		@Override
		public LocalTime utcTime()
		{
			return local;
		}

		@Override
		public LocalTime localTime()
		{
			return local;
		}
	}

	private BalanceSpawn()
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
		SpawnCalendar calendar = new SpawnCalendar(data.species());
		LocalDate weekday = firstDate(LocalDate.of(2026, 11, 16), null);
		LocalDate undeadWeekend = firstDate(LocalDate.of(2026, 11, 14), SpawnCalendar.Theme.UNDEAD);
		SpawnClock day = new Fixed(weekday, LocalTime.NOON);
		StringBuilder out = new StringBuilder("## 1. Spawn\n\n");
		out.append("Constants read: `WildTracker.MAX_WILD` ").append(WildTracker.MAX_WILD)
			.append(", `COOLDOWN_MS` ").append(BalanceTable.cell((int) WildTracker.COOLDOWN_MS))
			.append(", `MEMORY_MS` ").append(BalanceTable.cell((int) WildTracker.MEMORY_MS))
			.append(", `GILDED_ONE_IN` ").append(WildTracker.GILDED_ONE_IN)
			.append(", `ROAMING_ONE_IN` ").append(WildTracker.ROAMING_ONE_IN)
			.append(", `SpawnCalendar.FEATURED_BOOST` ").append(SpawnCalendar.FEATURED_BOOST)
			.append(", `NIGHT_BOOST` ").append(SpawnCalendar.NIGHT_BOOST)
			.append(", `WEEKEND_WILD_BOOST` ").append(SpawnCalendar.WEEKEND_WILD_BOOST)
			.append(", `MAX_BOOST` ").append(SpawnCalendar.MAX_BOOST)
			.append(". Tier factors (`Rarity.wildFactor`): ");
		for (Rarity rarity : Rarity.values())
			out.append(rarity.label() + " " + rarity.wildFactor() + (rarity == Rarity.LEGENDARY ? "" : ", "));

		out.append(".\n\n");
		out.append(chanceTable(data, calendar, day));
		out.append(boostTable(data, calendar, weekday, undeadWeekend));
		for (int monsters : MONSTERS)
			out.append(waitTable(data, calendar, day, monsters));

		out.append(perHourTable(data, calendar, day));
		out.append(skillingTable());
		out.append(skillingMix(data));
		return out.toString();
	}

	private static LocalDate firstDate(LocalDate from, SpawnCalendar.Theme theme)
	{
		LocalDate date = from;
		for (int i = 0; i < 60; i++)
		{
			SpawnCalendar.Theme found = SpawnCalendar.weekendTheme(date);
			if (theme == null ? found == null : found == theme)
				return date;

			date = date.plusDays(1);
		}

		return from;
	}

	private static String chanceTable(BalanceData data, SpawnCalendar calendar, SpawnClock clock)
	{
		StringBuilder out = new StringBuilder("### 1a. Chance that one monster rolls wild, per roll\n\n");
		out.append("A monster rolls once, then again every `MEMORY_MS` while it stays in view. No events, so no ")
			.append("boost. Species: the middle species of each tier.\n\n");
		BalanceTable table = new BalanceTable(heading("Tier"));
		for (Rarity rarity : Rarity.values())
		{
			Object[] cells = new Object[ONE_IN.length + 1];
			cells[0] = rarity.label();
			for (int i = 0; i < ONE_IN.length; i++)
				cells[i + 1] = BalanceTable.pct(calendar.monsterChance(data.median(rarity), ONE_IN[i], false, clock));

			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String[] heading(String first)
	{
		String[] head = new String[ONE_IN.length + 1];
		head[0] = first;
		for (int i = 0; i < ONE_IN.length; i++)
			head[i + 1] = "wildChance " + ONE_IN[i] + (ONE_IN[i] == 8 ? " (default)" : "");

		return head;
	}

	private static String boostTable(BalanceData data, SpawnCalendar calendar, LocalDate weekday,
		LocalDate undeadWeekend)
	{
		Species common = data.median(Rarity.COMMON);
		Species ghoul = new Species("Night ghoul", common.level(), CombatType.CRUSH, Trait.UNHOLY);
		List<Species> picks = calendar.featured(weekday);
		Species featured = picks.isEmpty() ? common : picks.get(0);
		StringBuilder out = new StringBuilder("### 1b. What the event boosts do to a Common monster\n\n");
		out.append("Same chance as 1a with events on. `" + featured.name() + "` is the featured Common species ")
			.append("on the sample weekday; the ghoul is a made-up undead of the same level.\n\n");
		BalanceTable table = new BalanceTable(heading("Scenario"));
		table.row(scenario("No boost (events off)", calendar, common, false, new Fixed(weekday, LocalTime.NOON)));
		table.row(scenario("Featured species (x3)", calendar, featured, true, new Fixed(weekday, LocalTime.NOON)));
		table.row(scenario("Undead at night (x2)", calendar, ghoul, true, new Fixed(weekday, LocalTime.of(22, 0))));
		table.row(scenario("Undead at night on the Undead weekend (x4)", calendar, ghoul, true,
			new Fixed(undeadWeekend, LocalTime.of(22, 0))));
		return out.append(table).toString();
	}

	private static Object[] scenario(String name, SpawnCalendar calendar, Species species, boolean events,
		SpawnClock clock)
	{
		Object[] cells = new Object[ONE_IN.length + 1];
		cells[0] = name;
		for (int i = 0; i < ONE_IN.length; i++)
			cells[i + 1] = BalanceTable.pct(calendar.monsterChance(species, ONE_IN[i], events, clock));

		return cells;
	}

	/**
	 * Works out the minutes until the first wild creature, for monsters that are all in view when the wait starts.
	 * Each monster rolls at once and again every {@link WildTracker#MEMORY_MS}, so the number of whole rounds that
	 * fail before one succeeds is geometric. {@code BalanceReportTest} checks this against the real
	 * {@link WildTracker}.
	 *
	 * @param chance the chance one roll turns wild
	 * @param monsters the monsters in view
	 * @return the expected minutes, infinite when the chance is zero
	 */
	static double minutesToWild(double chance, int monsters)
	{
		double any = 1.0 - Math.pow(1.0 - chance, monsters);
		if (any <= 0.0)
			return Double.POSITIVE_INFINITY;

		return WildTracker.MEMORY_MS / MS_PER_MINUTE * (1.0 - any) / any;
	}

	/**
	 * Plays the real {@link WildTracker} until the first creature turns wild, many times, and averages the minutes.
	 *
	 * @param chance the chance one roll turns wild
	 * @param monsters the monsters in view
	 * @param trials how many waits to play
	 * @return the mean minutes
	 */
	static double simulatedMinutes(double chance, int monsters, int trials)
	{
		Species species = new Species("Probe", 10, CombatType.CRUSH);
		double total = 0;
		Random seeds = new Random(SEED);
		for (int trial = 0; trial < trials; trial++)
		{
			WildTracker tracker = new WildTracker(new Random(seeds.nextLong()));
			long now = 0;
			boolean found = false;
			while (!found)
			{
				for (int monster = 0; monster < monsters && !found; monster++)
					found = tracker.offer(monster, 1, species, 10, false, now, chance, WildTracker.GILDED_ONE_IN, false)
						!= null;

				if (!found)
					now += 600;
			}

			total += now / MS_PER_MINUTE;
		}

		return total / trials;
	}

	private static String waitTable(BalanceData data, SpawnCalendar calendar, SpawnClock clock, int monsters)
	{
		StringBuilder out = new StringBuilder("### 1c. Minutes until the next wild creature, ").append(monsters)
			.append(monsters == 1 ? " monster" : " monsters").append(" in view\n\n");
		if (monsters == MONSTERS[0])
			out.append("The mean wait from the moment the monsters come into view: each rolls at once, then every ")
				.append("3 minutes. In the game a monster rolls when it spawns into view and then one random monster ")
				.append("that may roll does so every 25 game ticks (15 seconds, `GielinorGoPlugin.REROLL_TICKS`, ")
				.append("private), so at most 4 rolls a minute whatever the number of monsters. The model is exact ")
				.append("up to about 12 monsters and at most 15 seconds optimistic. Waits over 2 hours are shown ")
				.append("in hours. The monsters must be within 20 tiles (`SPAWN_RANGE`, private).\n\n");

		BalanceTable table = new BalanceTable(heading("Tier"));
		for (Rarity rarity : Rarity.values())
		{
			Object[] cells = new Object[ONE_IN.length + 1];
			cells[0] = rarity.label();
			for (int i = 0; i < ONE_IN.length; i++)
			{
				double chance = calendar.monsterChance(data.median(rarity), ONE_IN[i], false, clock);
				cells[i + 1] = BalanceTable.minutes(minutesToWild(chance, monsters));
			}

			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String perHourTable(BalanceData data, SpawnCalendar calendar, SpawnClock clock)
	{
		StringBuilder out = new StringBuilder("### 1d. Wild creatures per hour of standing among 3 monsters\n\n");
		out.append("60 minutes divided by the wait in 1c plus the ").append(WildTracker.COOLDOWN_MS / 1000)
			.append(" second cooldown. Monsters that turned wild are replaced by fresh ones. ")
			.append("Ignores the cap of " + WildTracker.MAX_WILD + " at once.\n\n");
		BalanceTable table = new BalanceTable(heading("Tier"));
		for (Rarity rarity : Rarity.values())
		{
			Object[] cells = new Object[ONE_IN.length + 1];
			cells[0] = rarity.label();
			for (int i = 0; i < ONE_IN.length; i++)
			{
				double chance = calendar.monsterChance(data.median(rarity), ONE_IN[i], false, clock);
				cells[i + 1] = BalanceTable.number(60.0 / (minutesToWild(chance, 3)
					+ WildTracker.COOLDOWN_MS / MS_PER_MINUTE));
			}

			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String skillingTable()
	{
		StringBuilder out = new StringBuilder("### 1e. Wild creatures from skilling (no rarity weighting)\n\n");
		out.append("`SkillingRules.XP_PER_ENCOUNTER` ").append(SkillingRules.XP_PER_ENCOUNTER)
			.append(", `REFERENCE_ONE_IN` ").append(SkillingRules.REFERENCE_ONE_IN)
			.append(", `COOLDOWN_MS` ").append(SkillingRules.COOLDOWN_MS)
			.append(". XP drops of ").append(DROP_XP).append(" OSRS XP each; the cell is the mean minutes between ")
			.append("encounters (the cooldown plus the wait for a roll), then creatures per hour. ")
			.append("The setting scales the chance the same way as for monsters.\n\n");
		BalanceTable table = new BalanceTable(heading("OSRS XP per hour"));
		for (int xp : XP_PER_HOUR)
		{
			Object[] cells = new Object[ONE_IN.length + 1];
			cells[0] = xp;
			for (int i = 0; i < ONE_IN.length; i++)
			{
				double dropsPerMinute = xp / 60.0 / DROP_XP;
				double perMinute = dropsPerMinute * SkillingRules.chance(DROP_XP, ONE_IN[i]);
				double minutes = SkillingRules.COOLDOWN_MS / MS_PER_MINUTE + 1.0 / perMinute;
				cells[i + 1] = BalanceTable.number(minutes) + " min (" + BalanceTable.number(60.0 / minutes) + "/h)";
			}

			table.row(cells);
		}

		return out.append(table).toString();
	}

	private static String skillingMix(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 1f. Which tier a skilling encounter is\n\n");
		out.append("Sampled ").append(BalanceTable.cell(SAMPLES)).append(" picks of the real `SkillingTable.pick` per ")
			.append("skill and skill level. Skilling ignores `Rarity.wildFactor`, so the mix is the table's own.\n\n");
		String[] skills = {"FISHING", "WOODCUTTING", "MINING", "FARMING", "HUNTER"};
		int[] levels = {1, 40, 99};
		String[] head = new String[Rarity.values().length + 2];
		head[0] = "Skill at level";
		for (Rarity rarity : Rarity.values())
			head[rarity.ordinal() + 1] = rarity.label();

		head[head.length - 1] = "no pick";
		BalanceTable table = new BalanceTable(head);
		for (String skill : skills)
		{
			for (int level : levels)
			{
				Map<Rarity, Integer> tally = new EnumMap<>(Rarity.class);
				Random random = new Random(SEED);
				int none = 0;
				for (int i = 0; i < SAMPLES; i++)
				{
					Species picked = data.skilling().pick(skill, level, random);
					if (picked == null)
						none++;
					else
						tally.merge(picked.rarity(), 1, Integer::sum);
				}

				Object[] cells = new Object[head.length];
				cells[0] = skill.charAt(0) + skill.substring(1).toLowerCase(Locale.ROOT) + " " + level;
				for (Rarity rarity : Rarity.values())
					cells[rarity.ordinal() + 1] = BalanceTable.pct(tally.getOrDefault(rarity, 0) / (double) SAMPLES);

				cells[cells.length - 1] = BalanceTable.pct(none / (double) SAMPLES);
				table.row(cells);
			}
		}

		return out.append(table).toString();
	}
}
