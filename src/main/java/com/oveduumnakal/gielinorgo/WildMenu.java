/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * What the menu offers over a wild creature's body, and in which order. The first entry is the one a plain left
 * click does. Which action that is lives in one constant, {@link #LEFT_CLICK_CATCHES}. Holds no RuneLite types.
 */
final class WildMenu
{
	/**
	 * Whether a plain left click on a wild creature throws the selected jar. When false (the owner's choice of
	 * 2026-11-12), a left click starts a battle instead, except on a knocked-out creature
	 * ({@link #leftClickCatches(boolean, boolean)}). Flip it here.
	 */
	static final boolean LEFT_CLICK_CATCHES = false;

	/**
	 * Whether Catch becomes a list of jars when the player owns two or more. When false the menu is the flat one
	 * of {@link #topFirst}: one Catch with the selected jar, and one per owned specialty jar that fits the creature.
	 * The owner's switch if the submenu misbehaves in the game, like {@link #LEFT_CLICK_CATCHES}.
	 */
	static final boolean SUBMENU = true;

	/** The words of the inert row between the suggested jars and the others; the plugin greys it. */
	static final String SEPARATOR = "--------";

	/** The words of a catch entry. */
	static final String CATCH = "Catch";

	/** The words of a battle entry. */
	static final String BATTLE = "Battle";

	/** One menu entry. */
	static final class Item
	{
		private final String option;
		private final JarType jar;

		private Item(String option, JarType jar)
		{
			this.option = option;
			this.jar = jar;
		}

		/** @return the words of the entry: {@link WildMenu#CATCH} or {@link WildMenu#BATTLE} */
		String option()
		{
			return option;
		}

		/** @return the jar a catch entry throws, or null for a battle entry or when the player has no jar */
		JarType jar()
		{
			return jar;
		}

		/** @return whether this is a catch entry */
		boolean isCatch()
		{
			return CATCH.equals(option);
		}
	}

	private WildMenu()
	{
	}

	/**
	 * Works out what a plain left click does for a wild creature in some state: the default
	 * ({@link #LEFT_CLICK_CATCHES}) while it stands, a catch once it lies knocked out, and a catch whenever a
	 * battle would be refused (no team, or no rested lead).
	 *
	 * @param knockedOut whether the creature lies knocked out after losing a battle
	 * @param canBattle whether a battle could start now
	 * @return true when the left click throws the selected jar, false when it starts a battle
	 */
	static boolean leftClickCatches(boolean knockedOut, boolean canBattle)
	{
		return knockedOut || !canBattle || LEFT_CLICK_CATCHES;
	}

	/**
	 * Tells whether a wild creature's entries may take the plain left click under a setting.
	 *
	 * @param mode the player's setting
	 * @param inCombat whether the player is fighting a real monster now
	 * @return true when the entries go on top of the menu, false when they go below the game's own entries
	 */
	static boolean leftClickAllowed(LeftClickMode mode, boolean inCombat)
	{
		switch (mode)
		{
			case ALWAYS:
				return true;
			case NEVER:
				return false;
			default:
				return !inCombat;
		}
	}

	/**
	 * Builds the target text of a wild creature's menu entries: the coloured name and the level, with the record
	 * words (New, Seen or Caught) after the level when the record is known.
	 *
	 * @param prefix the words in front of the name, such as "Gilded ", or an empty string
	 * @param name the species name
	 * @param nameRgb the colour of the name as 0xRRGGBB
	 * @param level the creature level
	 * @param record what the Creature Log knows of the species, or null when it is not known yet
	 * @return the text, such as {@code Gilded <col=4aa3ff>Chicken</col> (Lv 3 - New)}
	 */
	static String target(String prefix, String name, int nameRgb, int level, WildRecord record)
	{
		String colour = String.format("%06x", nameRgb & 0xFFFFFF);
		String suffix = record == null ? "" : " - " + record.label();
		return prefix + "<col=" + colour + ">" + name + "</col> (Lv " + level + suffix + ")";
	}

	/**
	 * Builds the plain heading of the painted menu: the name and the level, with the record words after the level.
	 *
	 * @param prefix the words in front of the name, such as "Gilded ", or an empty string
	 * @param name the species name
	 * @param level the creature level
	 * @param record what the Creature Log knows of the species, or null when it is not known yet
	 * @return the text, such as {@code Gilded Chicken (Lv 3 - New)}
	 */
	static String title(String prefix, String name, int level, WildRecord record)
	{
		String suffix = record == null ? "" : " - " + record.label();
		return prefix + name + " (Lv " + level + suffix + ")";
	}

	/**
	 * Tells whether a menu entry's words are one of this plugin's wild creature entries.
	 *
	 * @param option the entry's words
	 * @return true for a catch or battle entry
	 */
	static boolean isOurs(String option)
	{
		return CATCH.equals(option) || BATTLE.equals(option);
	}

	/**
	 * Lists the entries for one wild creature, the left-click one first.
	 *
	 * @param standard the jar the player has selected, or null when they have none
	 * @param specialty the specialty jars that are good against this creature
	 * @param withBattle whether a battle may be started now
	 * @param leftClickCatches whether the catch entries come before the battle entry
	 * @return the entries, top of the menu first
	 */
	static List<Item> topFirst(JarType standard, List<JarType> specialty, boolean withBattle,
		boolean leftClickCatches)
	{
		List<Item> catches = new ArrayList<>();
		catches.add(new Item(CATCH, standard));
		for (JarType jar : specialty)
			catches.add(new Item(CATCH, jar));

		List<Item> items = new ArrayList<>();
		Item battle = withBattle ? new Item(BATTLE, null) : null;
		if (battle != null && !leftClickCatches)
			items.add(battle);

		items.addAll(catches);
		if (battle != null && leftClickCatches)
			items.add(battle);

		return items;
	}

	/** What kind of top-level entry a {@link Top} is. */
	enum TopKind
	{
		/** Start a battle. */
		BATTLE,
		/** Throw one jar, the one the entry names. */
		CATCH_PLAIN,
		/** Open the list of jars. */
		CATCH_SUBMENU
	}

	/** What kind of row a {@link Row} is. */
	enum RowKind
	{
		/** A jar to throw. */
		JAR,
		/** The inert line between the suggested jars and the others. */
		SEPARATOR
	}

	/** One entry of the menu itself: Battle, a plain Catch, or the Catch list. */
	static final class Top
	{
		private final TopKind kind;
		private final JarType jar;
		private final List<Row> rows;

		private Top(TopKind kind, JarType jar, List<Row> rows)
		{
			this.kind = kind;
			this.jar = jar;
			this.rows = Collections.unmodifiableList(rows);
		}

		/** @return which entry this is */
		TopKind kind()
		{
			return kind;
		}

		/** @return the jar a plain Catch throws, or null for any other entry or when the player has no jar */
		JarType jar()
		{
			return jar;
		}

		/** @return the rows of the Catch list, top first; empty for any other entry */
		List<Row> rows()
		{
			return rows;
		}
	}

	/** One row of the Catch list. */
	static final class Row
	{
		private final RowKind kind;
		private final JarSuggest.JarChoice choice;
		private final boolean suggested;

		private Row(RowKind kind, JarSuggest.JarChoice choice, boolean suggested)
		{
			this.kind = kind;
			this.choice = choice;
			this.suggested = suggested;
		}

		/** @return which kind of row this is */
		RowKind kind()
		{
			return kind;
		}

		/** @return the jar of a {@link RowKind#JAR} row, or null for the separator */
		JarSuggest.JarChoice choice()
		{
			return choice;
		}

		/** @return whether the jar is one of the suggested ones */
		boolean suggested()
		{
			return suggested;
		}
	}

	/**
	 * Lays the whole menu out, top of the menu first. With fewer than two owned jars, or with
	 * {@code submenu} false, it is the flat menu of {@link #topFirst}. Otherwise, with Battle on top
	 * ({@code withBattle && !leftClickCatches}) it is Battle and the Catch list; with Catch on top it is a plain Catch
	 * with the standard jar (what a left click throws), the Catch list, and Battle last when it is offered.
	 *
	 * @param standard the jar a plain left click throws, or null when the player has none
	 * @param owned every owned jar with its odds
	 * @param withBattle whether a battle may be started now
	 * @param leftClickCatches whether the catch entries come before the battle entry
	 * @param submenu whether the list may be used, normally {@link #SUBMENU}
	 * @return the entries, top first
	 */
	static List<Top> layout(JarType standard, List<JarSuggest.JarChoice> owned, boolean withBattle,
		boolean leftClickCatches, boolean submenu)
	{
		List<Top> tops = new ArrayList<>();
		if (!submenu || owned.size() < 2)
		{
			List<JarSuggest.JarChoice> specialty = new ArrayList<>();
			for (JarSuggest.JarChoice choice : owned)
				if (choice.matching() && choice.jar() != standard)
					specialty.add(choice);

			specialty.sort(Comparator.comparingInt(c -> c.jar().ordinal()));
			List<JarType> jars = new ArrayList<>();
			for (JarSuggest.JarChoice choice : specialty)
				jars.add(choice.jar());

			for (Item item : topFirst(standard, jars, withBattle, leftClickCatches))
				tops.add(item.isCatch() ? new Top(TopKind.CATCH_PLAIN, item.jar(), Collections.emptyList())
					: new Top(TopKind.BATTLE, null, Collections.emptyList()));

			return tops;
		}

		Top list = new Top(TopKind.CATCH_SUBMENU, null, rowsOf(owned));
		boolean battleFirst = withBattle && !leftClickCatches;
		if (battleFirst)
			tops.add(new Top(TopKind.BATTLE, null, Collections.emptyList()));
		else
			tops.add(new Top(TopKind.CATCH_PLAIN, standard, Collections.emptyList()));

		tops.add(list);
		if (withBattle && !battleFirst)
			tops.add(new Top(TopKind.BATTLE, null, Collections.emptyList()));

		return tops;
	}

	private static List<Row> rowsOf(List<JarSuggest.JarChoice> owned)
	{
		JarSuggest.Split split = JarSuggest.split(owned);
		List<Row> rows = new ArrayList<>();
		for (JarSuggest.JarChoice choice : split.suggested())
			rows.add(new Row(RowKind.JAR, choice, true));

		if (!split.suggested().isEmpty() && !split.others().isEmpty())
			rows.add(new Row(RowKind.SEPARATOR, null, false));

		for (JarSuggest.JarChoice choice : split.others())
			rows.add(new Row(RowKind.JAR, choice, false));

		return rows;
	}

	/**
	 * @param choice a jar in the Catch list
	 * @return its words in the list, such as {@code Dragon jar}
	 */
	static String jarOption(JarSuggest.JarChoice choice)
	{
		return choice.jar().label() + " jar";
	}

	/**
	 * @param choice a jar in the Catch list
	 * @return its odds and count, such as {@code 92% (x3)}
	 */
	static String jarTarget(JarSuggest.JarChoice choice)
	{
		return choice.pct() + "% (x" + choice.owned() + ")";
	}
}
