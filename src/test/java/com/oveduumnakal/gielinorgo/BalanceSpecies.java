/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Section 5 of the balance report: how the species spread over the five tiers and the five styles, and which level a
 * wild creature of each tier appears at. Counts come from the loaded {@link SpeciesTable}, levels from
 * {@link CatchRules#wildLevel}.
 */
final class BalanceSpecies
{
	private static final long SEED = 20261118L;
	private static final int PER_SPECIES = 100;

	private BalanceSpecies()
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
		StringBuilder out = new StringBuilder("## 5. Rarity and species\n\n");
		int hunters = 0;
		int quest = 0;
		for (Species each : data.species().all())
		{
			if (each.questOnly())
				quest++;
			else if (each.hunter())
				hunters++;
		}

		out.append("Species in the table: " + data.species().size() + ", catchable " + data.species().catchableSize()
			+ " (" + quest + " are quest-only and cannot be met; " + hunters
			+ " of the catchable ones are Hunter species). Tier by combat level: ");
		for (Rarity rarity : Rarity.values())
			out.append(rarity.label() + " from " + rarity.minLevel() + (rarity == Rarity.LEGENDARY ? "" : ", "));

		out.append(". Nothing in the code gates a species by trainer level: every catchable species can turn wild ")
			.append("from the first minute, and only the jars are gated (see 2).\n\n");
		out.append(counts(data));
		out.append(levels(data));
		return out.toString();
	}

	private static String counts(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 5a. Catchable species by tier and style\n\n");
		String[] head = new String[CombatType.values().length + 3];
		head[0] = "Tier";
		for (CombatType style : CombatType.values())
			head[style.ordinal() + 1] = style.label();

		head[head.length - 2] = "Total";
		head[head.length - 1] = "Share of all";
		BalanceTable table = new BalanceTable(head);
		int all = Math.max(1, data.species().catchableSize());
		for (Rarity rarity : Rarity.values())
		{
			Object[] cells = new Object[head.length];
			cells[0] = rarity.label();
			for (CombatType style : CombatType.values())
				cells[style.ordinal() + 1] = data.count(rarity, style);

			cells[head.length - 2] = data.tier(rarity).size();
			cells[head.length - 1] = BalanceTable.pct(data.tier(rarity).size() / (double) all);
			table.row(cells);
		}

		Object[] totals = new Object[head.length];
		totals[0] = "All";
		for (CombatType style : CombatType.values())
		{
			int count = 0;
			for (Rarity rarity : Rarity.values())
				count += data.count(rarity, style);

			totals[style.ordinal() + 1] = count + " (" + BalanceTable.pct(count / (double) all) + ")";
		}

		totals[head.length - 2] = data.species().catchableSize();
		totals[head.length - 1] = BalanceTable.pct(1.0);
		table.row(totals);
		return out.append(table).toString();
	}

	private static String levels(BalanceData data)
	{
		StringBuilder out = new StringBuilder("### 5b. The level a wild creature appears at\n\n");
		out.append("`CatchRules.wildLevel` of every species of the tier, " + PER_SPECIES + " draws each. A ")
			.append("roaming creature is `WildTracker.ROAMING_LEVELS` (" + WildTracker.ROAMING_LEVELS)
			.append(") levels higher, capped at " + XpRules.MAX_LEVEL + ".\n\n");
		BalanceTable table = new BalanceTable("Tier", "Combat level min / mean / max", "Wild level mean",
			"Wild level min", "10th percentile", "90th percentile", "Wild level max");
		Random random = new Random(SEED);
		for (Rarity rarity : Rarity.values())
		{
			List<Integer> drawn = new ArrayList<>();
			for (Species each : data.tier(rarity))
			{
				for (int i = 0; i < PER_SPECIES; i++)
					drawn.add(CatchRules.wildLevel(each.level(), random));
			}

			Collections.sort(drawn);
			if (drawn.isEmpty())
			{
				table.row(rarity.label(), "none", "n/a", "n/a", "n/a", "n/a", "n/a");
				continue;
			}

			double sum = 0;
			for (int level : drawn)
				sum += level;

			table.row(rarity.label(), data.minLevel(rarity) + " / " + Math.round(data.meanLevel(rarity)) + " / "
				+ data.maxLevel(rarity), sum / drawn.size(), drawn.get(0), drawn.get(drawn.size() / 10),
				drawn.get(drawn.size() * 9 / 10), drawn.get(drawn.size() - 1));
		}

		return out.append(table).toString();
	}
}
