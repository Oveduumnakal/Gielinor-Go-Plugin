/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Builds the balance report of issue 220 as Markdown. Every number is read from the real game classes
 * ({@link CatchRules}, {@link XpRules}, {@link Rarity}, {@link SpawnCalendar}, {@link WildTracker},
 * {@link SkillingRules}, {@link BattleSim}, {@link StarterRules}), never copied, so after a tuning change a new run
 * shows the new numbers. Seeded randomness only and no clock, so two runs give the same text. It changes no
 * behaviour. {@link BalanceReportTest} runs it when the {@code BALANCE_REPORT} environment variable is set.
 */
final class BalanceReport
{
	private BalanceReport()
	{
	}

	/**
	 * Builds the whole report.
	 *
	 * @param data the species, evolutions, moves and skilling the sections measure
	 * @return the Markdown text
	 */
	static String build(BalanceData data)
	{
		StringBuilder out = new StringBuilder("# Gielinor Go balance report\n\n");
		out.append("Measures the constants in the code as they are. ").append(data.species().catchableSize())
			.append(" catchable species. Seeded and deterministic: two runs give the same text. ")
			.append("Run: `BALANCE_REPORT=1 ./gradlew test --no-daemon -q --tests ")
			.append("'com.oveduumnakal.gielinorgo.BalanceReportTest'`.\n\n");
		out.append(BalanceSpawn.section(data));
		out.append(BalanceCatch.section(data));
		out.append(BalanceProgress.levels(data));
		out.append(BalanceProgress.eggs(data));
		out.append(BalanceSpecies.section(data));
		out.append(BalanceBattles.battles(data));
		out.append(BalanceBattles.moves(data));
		out.append(BalanceFaint.section(data));
		return out.toString();
	}
}
