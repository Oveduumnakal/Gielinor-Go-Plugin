/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Everything the Summary page says about one creature, worded once and kept apart from the painting so it is tested
 * without a window. Left side: the nickname as the title with the species under it, the gender, the level, and the
 * style and trait chips. Right side: the four stats from {@link StatRolls} with the rating, the Nature box
 * ({@link Temperament#label()} and {@link Temperament#effect()}), the examine text, the moves (at most four, in two
 * columns), the bond hearts and the evolution hint. A creature whose species is not in the list still has a Summary;
 * its missing parts read as plain words. Immutable. Holds no RuneLite types.
 */
final class SummaryContent
{
	/** The words shown when the species has no examine line. */
	static final String NO_DESCRIPTION = "Nothing is known about this creature yet.";

	/** The words shown when the creature knows no move. */
	static final String NO_MOVES = "No moves yet.";

	/** The words of the evolution hint of a creature that does not evolve. */
	static final String DOES_NOT_EVOLVE = "Does not evolve";

	/** The most moves a creature knows. */
	static final int MAX_MOVES = 4;

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	/** A chip: the creature's style or one of its traits. */
	static final class Chip
	{
		private final String label;
		private final boolean style;

		Chip(String label, boolean style)
		{
			this.label = label;
			this.style = style;
		}

		/** @return the word on the chip */
		String label()
		{
			return label;
		}

		/** @return whether this is the combat style, which stands out */
		boolean style()
		{
			return style;
		}
	}

	/** One line of the Stats box. */
	static final class StatLine
	{
		private final String label;
		private final int roll;
		private final String rollText;
		private final String netText;
		private final String valueText;

		StatLine(String label, int roll, String rollText, String netText)
		{
			this(label, roll, rollText, netText, "");
		}

		StatLine(String label, int roll, String rollText, String netText, String valueText)
		{
			this.label = label;
			this.roll = roll;
			this.rollText = rollText;
			this.netText = netText;
			this.valueText = valueText;
		}

		/** @return the stat itself, such as {@code 84}, or an empty text when the view does not know it */
		String valueText()
		{
			return valueText;
		}

		/** @return the stat's name */
		String label()
		{
			return label;
		}

		/** @return the roll, from {@link StatRolls#MIN} to {@link StatRolls#MAX} */
		int roll()
		{
			return roll;
		}

		/** @return the roll as a signed number, such as {@code +3} or {@code 0} */
		String rollText()
		{
			return rollText;
		}

		/** @return what roll and nature together do to the stat, such as {@code +8%}, or an empty text for nothing */
		String netText()
		{
			return netText;
		}

		/** @return how full the stat's bar is, from 0 for the lowest roll to 1 for the highest */
		double fraction()
		{
			return (roll - StatRolls.MIN) / (double) (StatRolls.MAX - StatRolls.MIN);
		}
	}

	/** One move cell. */
	static final class MoveLine
	{
		private final String name;
		private final String detail;
		private final CombatType style;

		MoveLine(String name, String detail, CombatType style)
		{
			this.name = name;
			this.detail = detail;
			this.style = style;
		}

		/** @return the move's name */
		String name()
		{
			return name;
		}

		/** @return the move's style, power and accuracy */
		String detail()
		{
			return detail;
		}

		/** @return the move's style, for its icon */
		CombatType style()
		{
			return style;
		}
	}

	private final RowModel row;
	private final List<Chip> chips;
	private final List<StatLine> stats;
	private final List<MoveLine> moves;
	private final String pending;
	private final String caught;

	private SummaryContent(RowModel row, List<Chip> chips, List<StatLine> stats, List<MoveLine> moves, String pending,
		String caught)
	{
		this.caught = caught;
		this.row = row;
		this.chips = Collections.unmodifiableList(chips);
		this.stats = Collections.unmodifiableList(stats);
		this.moves = Collections.unmodifiableList(moves);
		this.pending = pending;
	}

	/**
	 * Words the Summary of a creature.
	 *
	 * @param row the creature's row
	 * @return its content
	 */
	static SummaryContent of(RowModel row)
	{
		return of(row, ZoneId.systemDefault());
	}

	/**
	 * Words the Summary of a creature, with the day it was caught in a given time zone.
	 *
	 * @param row the creature's row
	 * @param zone the zone the catch day is worked out in
	 * @return its content
	 */
	static SummaryContent of(RowModel row, ZoneId zone)
	{
		CreatureView view = row.view();
		int[] numbers = view.stats();
		List<Chip> chips = chipsFor(view.type(), view.traits());
		List<StatLine> stats = new ArrayList<>();
		StatRolls rolls = view.rolls();
		for (Stat stat : Stat.values())
		{
			int roll = rolls.roll(stat);
			long net = Math.round((rolls.factor(stat) - 1.0) * 100);
			String netText = net == 0 ? "" : (net > 0 ? "+" : "") + net + "%";
			String value = numbers == null || stat.ordinal() >= numbers.length ? ""
				: String.valueOf(numbers[stat.ordinal()]);
			stats.add(new StatLine(stat.label(), roll, roll > 0 ? "+" + roll : String.valueOf(roll), netText, value));
		}

		List<MoveLine> moves = new ArrayList<>();
		for (Move move : view.moves())
		{
			if (moves.size() < MAX_MOVES)
				moves.add(new MoveLine(move.name() == null ? "Attack" : move.name(), move.describe(), move.style()));
		}

		String pending = view.pending().isEmpty() ? null : "New move: " + view.pending().get(0).name();
		return new SummaryContent(row, chips, stats, moves, pending, caughtText(view.caughtAtMs(), zone));
	}

	/**
	 * Works out the chips of a creature: its combat style first, then each trait besides the one its style implies.
	 *
	 * @param style the creature's combat style
	 * @param traits the creature's traits, in the order they should appear
	 * @return the chips, the style chip first
	 */
	static List<Chip> chipsFor(CombatType style, Collection<Trait> traits)
	{
		List<Chip> chips = new ArrayList<>();
		chips.add(new Chip(style.label(), true));
		for (Trait trait : traits)
		{
			if (trait != Trait.forStyle(style))
				chips.add(new Chip(capitalised(trait.name()), false));
		}

		return chips;
	}

	/**
	 * Words the day a creature was caught.
	 *
	 * @param ms the catch time in epoch milliseconds, or 0 or less when unknown
	 * @param zone the zone the day is worked out in
	 * @return {@code Caught 12 Nov 2026}, or an empty text when the time is unknown
	 */
	static String caughtText(long ms, ZoneId zone)
	{
		if (ms <= 0)
			return "";

		return "Caught " + DAY.withZone(zone).format(Instant.ofEpochMilli(ms));
	}

	private static String capitalised(String name)
	{
		return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
	}

	/** @return the creature's row */
	RowModel row()
	{
		return row;
	}

	/** @return the title: the nickname, or the species when there is none */
	String title()
	{
		return row.name();
	}

	/** @return the species under a nickname, or an empty text when the title already is the species */
	String subtitle()
	{
		return row.nicknamed() ? row.species() : "";
	}

	/** @return the gender, or null when none is known and no icon is drawn */
	Gender gender()
	{
		return row.gender();
	}

	/** @return {@code Lv 12} */
	String level()
	{
		return row.levelText();
	}

	/** @return when the creature was caught, such as {@code Caught 12 Nov 2026}, or an empty text when unknown */
	String caught()
	{
		return caught;
	}

	/**
	 * @return one line per stat with its number, roll and net change, or an empty text when the numbers are unknown
	 */
	String statsTip()
	{
		StringBuilder text = new StringBuilder();
		for (StatLine line : stats)
		{
			if (line.valueText().isEmpty())
				return "";

			if (text.length() > 0)
				text.append('\n');

			text.append(line.label())
				.append(' ')
				.append(line.valueText())
				.append(": roll ")
				.append(line.rollText());
			if (!line.netText().isEmpty())
				text.append(", ").append(line.netText()).append(" with the ").append(row.view().temperamentLabel())
					.append(" nature");
		}

		return text.toString();
	}

	/** @return the style chip first, then each trait the species has besides its style */
	List<Chip> chips()
	{
		return chips;
	}

	/** @return the four stats in the order health, attack, defence, speed */
	List<StatLine> stats()
	{
		return stats;
	}

	/** @return the one-word rating of the rolls, such as {@code Strong} */
	String rating()
	{
		StatRolls.Rating rating = row.view().rating();
		return rating.label();
	}

	/** @return the title of the Nature box, such as {@code Nature: Brave} */
	String natureTitle()
	{
		return "Nature: " + row.view().temperamentLabel();
	}

	/** @return what the nature does, such as {@code +10% Attack, -10% Defence}, or {@code no effect} */
	String natureEffect()
	{
		return row.view().temperamentEffect();
	}

	/** @return the examine line of the species, or {@link #NO_DESCRIPTION} */
	String description()
	{
		String text = row.view().examine();
		return text == null || text.trim().isEmpty() ? NO_DESCRIPTION : text.trim();
	}

	/** @return the known moves, at most {@link #MAX_MOVES}, in the order learned */
	List<MoveLine> moves()
	{
		return moves;
	}

	/** @return the words that mark a move waiting to be learned, or null when none waits */
	String pendingMove()
	{
		return pending;
	}

	/** @return how many bond hearts are filled, 0 to {@link BondRules#MAX_HEARTS} */
	int hearts()
	{
		return row.view().hearts();
	}

	/** @return the words about the bond, for the tooltip and the line beside the hearts */
	String bondText()
	{
		int hearts = row.view().hearts();
		return hearts > 0 ? BondRules.describe(hearts) : "No bond yet. It grows as your lead.";
	}

	/**
	 * Gives the evolution hint.
	 *
	 * @return what it waits for when it has the level but not the condition, else what it grows into, else
	 *         {@link #DOES_NOT_EVOLVE}
	 */
	String evolution()
	{
		CreatureView view = row.view();
		if (view.evolvesWhen() != null)
			return view.evolvesWhen();

		return view.growsInto() == null ? DOES_NOT_EVOLVE : view.growsInto();
	}

	/**
	 * Finds the creature an Up or Down press goes to.
	 *
	 * @param index the place now shown
	 * @param delta 1 for Down, -1 for Up
	 * @param teamSize how many creatures are on the team
	 * @return the next place, wrapping at the ends
	 */
	static int neighbour(int index, int delta, int teamSize)
	{
		return teamSize <= 0 ? 0 : Math.floorMod(index + delta, teamSize);
	}
}
