/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The words and numbers of the abbreviated card that opens beside a wild creature's bubbles, worked out once from the
 * {@link WildEntry} and kept apart from the painting so a test can read them. Left out on purpose: portrait, moves,
 * nature, bond and buttons. Immutable. Holds no RuneLite types.
 */
final class WildCardContent
{
	/** The line a knocked-out creature adds. */
	static final String KNOCKED_OUT_LINE = "Knocked out: twice as easy to catch";

	/** The odds line when the player has no jar. */
	static final String NO_JARS = "No jars";

	/** The lowest stat factor, which fills no part of the bar. */
	private static final double LOW = 0.75;

	/** The width of the factor range, so the highest factor fills the bar. */
	private static final double SPAN = 0.5;

	/** One of the four stat rows: the stat's name, how full its bar is and the percent beside it. */
	static final class StatRow
	{
		private final String label;
		private final double fraction;
		private final String percentText;

		StatRow(String label, double fraction, String percentText)
		{
			this.label = label;
			this.fraction = fraction;
			this.percentText = percentText;
		}

		/** @return the stat's name */
		String label()
		{
			return label;
		}

		/** @return how full the bar is, from 0 for a factor of 0.75 to 1 for a factor of 1.25 */
		double fraction()
		{
			return fraction;
		}

		/** @return what the species does to the stat, such as {@code +12%}, {@code -5%} or {@code 0%} */
		String percentText()
		{
			return percentText;
		}
	}

	private final String name;
	private final int nameRgb;
	private final String level;
	private final List<SummaryContent.Chip> chips;
	private final String recordLabel;
	private final int recordRgb;
	private final List<StatRow> stats;
	private final String words;
	private final String odds;
	private final String knockedOutLine;

	private WildCardContent(String name, int nameRgb, String level, List<SummaryContent.Chip> chips,
		String recordLabel, int recordRgb, List<StatRow> stats, String words, String odds, String knockedOutLine)
	{
		this.name = name;
		this.nameRgb = nameRgb;
		this.level = level;
		this.chips = Collections.unmodifiableList(chips);
		this.recordLabel = recordLabel;
		this.recordRgb = recordRgb;
		this.stats = Collections.unmodifiableList(stats);
		this.words = words;
		this.odds = odds;
		this.knockedOutLine = knockedOutLine;
	}

	/**
	 * Words the card of one wild creature.
	 *
	 * @param entry the creature
	 * @param shown the jar a Catch would throw with its odds against this creature, or null when the player has none
	 * @param knockedOut whether the creature lost its one battle
	 * @return the content
	 */
	static WildCardContent of(WildEntry entry, JarSuggest.JarChoice shown, boolean knockedOut)
	{
		Species species = entry.species();
		Rarity rarity = species.rarity();
		int rgb = (entry.gilded() ? WildTip.GILDED_COLOR : rarity.color()).getRGB() & 0xFFFFFF;
		List<SummaryContent.Chip> chips = new ArrayList<>(SummaryContent.chipsFor(species.style(), entry.traits()));
		chips.add(new SummaryContent.Chip(rarity.label(), false));
		List<StatRow> rows = new ArrayList<>();
		for (Stat stat : Stat.values())
		{
			double factor = species.factor(stat);
			long percent = Math.round((factor - 1.0) * 100);
			double fraction = Math.max(0.0, Math.min(1.0, (factor - LOW) / SPAN));
			rows.add(new StatRow(stat.label(), fraction, (percent > 0 ? "+" : "") + percent + "%"));
		}

		WildRecord record = entry.record();
		String odds = shown == null ? NO_JARS : shown.jar().label() + " jar " + shown.pct() + "%";
		return new WildCardContent(entry.prefix() + species.name(), rgb, "Lv " + entry.level(), chips,
			record == null ? null : record.label(), record == null ? 0 : record.rgb(), rows,
			String.join(" · ", species.words(2)), odds, knockedOut ? KNOCKED_OUT_LINE : null);
	}

	/** @return the creature's name with any Gilded or Roaming prefix */
	String name()
	{
		return name;
	}

	/** @return the colour of the name as 0xRRGGBB: gold when gilded, else the rarity's colour */
	int nameRgb()
	{
		return nameRgb;
	}

	/** @return the level, such as {@code Lv 14} */
	String level()
	{
		return level;
	}

	/** @return the style chip, then the trait chips, then the rarity chip */
	List<SummaryContent.Chip> chips()
	{
		return chips;
	}

	/** @return the species record word (New, Seen or Caught), or null when the record is not known */
	String recordLabel()
	{
		return recordLabel;
	}

	/** @return the colour of the record word as 0xRRGGBB; 0 when there is none */
	int recordRgb()
	{
		return recordRgb;
	}

	/** @return the four stat rows in the order health, attack, defence, speed */
	List<StatRow> stats()
	{
		return stats;
	}

	/** @return the species' good and bad points in words joined by a middle dot; empty for a plain species */
	String words()
	{
		return words;
	}

	/** @return the odds line, such as {@code Bronze jar 45%}, or {@link #NO_JARS} */
	String odds()
	{
		return odds;
	}

	/** @return {@link #KNOCKED_OUT_LINE} for a knocked-out creature, else null */
	String knockedOutLine()
	{
		return knockedOutLine;
	}
}
