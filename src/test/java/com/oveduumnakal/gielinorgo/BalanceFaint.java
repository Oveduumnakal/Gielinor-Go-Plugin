/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Section 8 of the balance report: what fainting (issue 221) costs and how fast the ways back work. Reads
 * {@link ReviveRules}, {@link ItemRules} and {@link Creature#FULL_REST_MS}; changes nothing.
 */
final class BalanceFaint
{
	private static final double MINUTE = 60_000.0;

	private BalanceFaint()
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
		double rest = Creature.FULL_REST_MS / MINUTE;
		StringBuilder out = new StringBuilder("## 8. Faint and revive\n\n");
		out.append("A creature that reaches 0 health faints and does not rest back up on its own. A tired creature ")
			.append("that did not faint rests to full in " + BalanceTable.number(rest) + " minutes ")
			.append("(`Creature.FULL_REST_MS`).\n\n");
		BalanceTable ways = new BalanceTable("Way back", "Health it comes back with", "Wait or place",
			"Minutes until full health after it", "Constant");
		ways.row("Revive at a bank or in a city", BalanceTable.pct(ReviveRules.BANK_TO), "a bank, city or team town",
			(1.0 - ReviveRules.BANK_TO) * rest, "`ReviveRules.BANK_TO`");
		ways.row("Revive draught", BalanceTable.pct(ItemRules.REVIVE_TO), "anywhere, one item",
			(1.0 - ItemRules.REVIVE_TO) * rest, "`ItemRules.REVIVE_TO`");
		ways.row("Passing healer (only when every creature owned has fainted)", BalanceTable.pct(ReviveRules.RESCUE_TO),
			BalanceTable.number(ReviveRules.RESCUE_MS / MINUTE) + " minutes after the newest faint",
			(1.0 - ReviveRules.RESCUE_TO) * rest, "`ReviveRules.RESCUE_MS`, `RESCUE_TO`");
		out.append(ways);
		out.append("The health a creature needs to start a battle is `GoGame.MIN_BATTLE_HP` "
			+ BalanceTable.pct(GoGame.MIN_BATTLE_HP) + ", so the healer's " + BalanceTable.pct(ReviveRules.RESCUE_TO)
			+ " is exactly enough to fight.\n\n");
		out.append("Where else revive items come from: a battle win drops a Revive draught with chance "
			+ BalanceTable.pct(ItemRules.WIN_REVIVE_CHANCE) + " (`ItemRules.WIN_REVIVE_CHANCE`), the first win over a ")
			.append("City Champion pays " + ItemRules.CHAMPION_REVIVES + " (`CHAMPION_REVIVES`).\n\n");
		BalanceTable wins = new BalanceTable("Battle wins per hour", "Revive draughts per hour", "Hours per draught");
		for (int perHour : BalanceProgress.WINS_PER_HOUR)
			wins.row(perHour, perHour * ItemRules.WIN_REVIVE_CHANCE, 1.0 / (perHour * ItemRules.WIN_REVIVE_CHANCE));

		out.append(wins);
		out.append("How often a battle ends in a faint depends on the level gap (see 6c): at equal level the lead ")
			.append("loses about half of the fights, and a loss faints it.\n\n");
		return out.toString();
	}
}
