/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Decides how often each species turns wild today: the featured species, night, and the weekend event. Every answer
 * is a pure function of the UTC date, the local time and the species, so every player on the same plugin version
 * sees the same featured species with no server. Knows nothing about the game client.
 */
final class SpawnCalendar
{
	/** How much more often a featured species is wild. */
	static final double FEATURED_BOOST = 3.0;

	/** How much more often undead and demons are wild at night. */
	static final double NIGHT_BOOST = 2.0;

	/** How much more often the weekend's group is wild. */
	static final double WEEKEND_WILD_BOOST = 2.0;

	/** How much more often the weekend's group is gilded. */
	static final double WEEKEND_GILDED_BOOST = 2.0;

	/** The most all the boosts together can make a species. */
	static final double MAX_BOOST = 6.0;

	/** The hour night starts, on the computer's clock. */
	static final int NIGHT_START_HOUR = 20;

	/** The hour night ends, on the computer's clock. */
	static final int NIGHT_END_HOUR = 6;

	/** Epoch day 0 was a Thursday, so adding 3 makes weeks count from Monday. */
	private static final int MONDAY_OFFSET = 3;

	/** One group of species that is wild and gilded more often on a weekend. */
	enum Theme
	{
		DRAGON("Dragon", Trait.FIERY),
		UNDEAD("Undead", Trait.UNHOLY),
		TIDE("Tide", Trait.AQUATIC),
		GIANT("Giant", Trait.LARGE),
		SLAYER("Slayer", Trait.SLAYER),
		GOBLIN("Goblin", null);

		private final String title;
		private final Trait trait;

		Theme(String title, Trait trait)
		{
			this.title = title;
			this.trait = trait;
		}

		/** @return the event's name, for example "Dragon weekend" */
		String eventName()
		{
			return title + " weekend";
		}

		/**
		 * Tells whether a species belongs to this group.
		 *
		 * @param species the species
		 * @return true when it has the group's trait, or for goblins when its name contains "goblin"
		 */
		boolean covers(Species species)
		{
			if (trait != null)
				return species.traits().contains(trait);

			return species.key().contains("goblin");
		}
	}

	private final Map<Rarity, List<Species>> byRarity = new EnumMap<>(Rarity.class);

	/**
	 * Builds the calendar over a species list.
	 *
	 * @param table every catchable species
	 */
	SpawnCalendar(SpeciesTable table)
	{
		for (Rarity rarity : Rarity.values())
			byRarity.put(rarity, new ArrayList<>());

		for (Species species : table.catchable())
			byRarity.get(species.rarity()).add(species);

		for (List<Species> list : byRarity.values())
			list.sort(Comparator.comparing(Species::key));
	}

	/**
	 * Finds today's featured species: for each rarity, the species at a position fixed by the date, in name order.
	 *
	 * @param date today's date in UTC
	 * @return up to five species, one per rarity that has any, weakest rarity first
	 */
	List<Species> featured(LocalDate date)
	{
		List<Species> picks = new ArrayList<>();
		for (Rarity rarity : Rarity.values())
		{
			List<Species> list = byRarity.get(rarity);
			if (!list.isEmpty())
				picks.add(list.get(DateHash.pick(date.toEpochDay(), rarity.ordinal(), list.size())));
		}

		return Collections.unmodifiableList(picks);
	}

	/**
	 * Tells whether a species is featured today.
	 *
	 * @param species the species
	 * @param date today's date in UTC
	 * @return true when it is one of the day's picks
	 */
	boolean isFeatured(Species species, LocalDate date)
	{
		for (Species pick : featured(date))
		{
			if (pick.key().equals(species.key()))
				return true;
		}

		return false;
	}

	/**
	 * Tells whether it is night.
	 *
	 * @param time the time of day on the computer's clock
	 * @return true from 20:00 up to but not including 06:00
	 */
	static boolean isNight(LocalTime time)
	{
		return time.getHour() >= NIGHT_START_HOUR || time.getHour() < NIGHT_END_HOUR;
	}

	/**
	 * Finds the weekend event, which turns to the next group each week.
	 *
	 * @param date today's date in UTC
	 * @return the group, or null on a weekday
	 */
	static Theme weekendTheme(LocalDate date)
	{
		DayOfWeek day = date.getDayOfWeek();
		if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY)
			return null;

		Theme[] themes = Theme.values();
		long week = Math.floorDiv(date.toEpochDay() + MONDAY_OFFSET, 7L);
		return themes[(int) Math.floorMod(week, (long) themes.length)];
	}

	/**
	 * Works out how many times more often a species is wild right now.
	 *
	 * @param species the species
	 * @param date today's date in UTC
	 * @param localTime the time of day on the computer's clock
	 * @return 1 for an ordinary species, up to {@link #MAX_BOOST}
	 */
	double wildMultiplier(Species species, LocalDate date, LocalTime localTime)
	{
		double boost = 1.0;
		if (isFeatured(species, date))
			boost *= FEATURED_BOOST;

		if (isNight(localTime) && species.traits().contains(Trait.UNHOLY))
			boost *= NIGHT_BOOST;

		Theme theme = weekendTheme(date);
		if (theme != null && theme.covers(species))
			boost *= WEEKEND_WILD_BOOST;

		return Math.min(MAX_BOOST, boost);
	}

	/**
	 * Works out how many times more often a species is gilded.
	 *
	 * @param species the species
	 * @param date today's date in UTC
	 * @return 1, or {@link #WEEKEND_GILDED_BOOST} for the weekend's group
	 */
	double gildedMultiplier(Species species, LocalDate date)
	{
		Theme theme = weekendTheme(date);
		return theme != null && theme.covers(species) ? WEEKEND_GILDED_BOOST : 1.0;
	}

	/**
	 * Works out the chance that one monster turns wild on one roll.
	 *
	 * @param species the species
	 * @param oneIn the setting: an ordinary monster turns wild one time in this many
	 * @param events whether spawn events are on; when off every boost is 1
	 * @param clock the date and time
	 * @return a chance from 0 to 1
	 */
	double chance(Species species, int oneIn, boolean events, SpawnClock clock)
	{
		double boost = events ? wildMultiplier(species, clock.utcDate(), clock.localTime()) : 1.0;
		return Math.min(1.0, boost / Math.max(1, oneIn));
	}

	/**
	 * Works out the gilded odds for a species that turned wild.
	 *
	 * @param species the species
	 * @param events whether spawn events are on; when off the odds are the ordinary ones
	 * @param clock the date and time
	 * @return the species turns gilded one time in this many
	 */
	int gildedOneIn(Species species, boolean events, SpawnClock clock)
	{
		double boost = events ? gildedMultiplier(species, clock.utcDate()) : 1.0;
		return Math.max(1, (int) Math.round(WildTracker.GILDED_ONE_IN / boost));
	}

	/**
	 * Builds what the Today section shows.
	 *
	 * @param events whether spawn events are on; when off there is nothing to show
	 * @param clock the date and time
	 * @return the view
	 */
	TodayView today(boolean events, SpawnClock clock)
	{
		if (!events)
			return TodayView.NONE;

		LocalDate date = clock.utcDate();
		List<TodayView.Pick> picks = new ArrayList<>();
		for (Species species : featured(date))
			picks.add(new TodayView.Pick(species.name(), species.rarity()));

		Theme theme = weekendTheme(date);
		String event = theme == null ? null : theme.eventName() + ": x2 wild, x2 gilded";
		int minutes = TodayView.minutesToMidnight(clock.utcTime().toSecondOfDay());
		return new TodayView(picks, event, isNight(clock.localTime()), minutes, true);
	}
}
