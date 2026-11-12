/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** What the Today section of the Log tab shows: the featured species, the event, night, and the time to reset. */
final class TodayView
{
	/** A view with nothing to show, used while spawn events are off. */
	static final TodayView NONE = new TodayView(Collections.emptyList(), null, false, 0, false);

	/** One featured species. */
	static final class Pick
	{
		private final String name;
		private final Rarity rarity;

		Pick(String name, Rarity rarity)
		{
			this.name = name;
			this.rarity = rarity;
		}

		/** @return the species name */
		String name()
		{
			return name;
		}

		/** @return the species' rarity */
		Rarity rarity()
		{
			return rarity;
		}
	}

	private final List<Pick> featured;
	private final String event;
	private final boolean night;
	private final int resetMinutes;
	private final boolean active;
	private final Set<String> keys = new TreeSet<>();

	TodayView(List<Pick> featured, String event, boolean night, int resetMinutes, boolean active)
	{
		this.featured = Collections.unmodifiableList(new ArrayList<>(featured));
		this.event = event;
		this.night = night;
		this.resetMinutes = resetMinutes;
		this.active = active;
		for (Pick pick : featured)
			keys.add(Species.keyFor(pick.name()));
	}

	/** @return whether there is anything to show */
	boolean active()
	{
		return active;
	}

	/** @return the featured species, one per rarity, weakest rarity first */
	List<Pick> featured()
	{
		return featured;
	}

	/** @return the weekend event line, for example "Dragon weekend: x2 wild, x2 gilded", or null when none is on */
	String event()
	{
		return event;
	}

	/** @return whether it is night, when undead and demons are wild more often */
	boolean night()
	{
		return night;
	}

	/** @return the night line, shown while it is night */
	String nightLine()
	{
		return "Night: undead and demons x2 wild";
	}

	/**
	 * Tells whether a species is featured today.
	 *
	 * @param name a species name
	 * @return true when it is one of today's featured species
	 */
	boolean isFeatured(String name)
	{
		return keys.contains(Species.keyFor(name));
	}

	/** @return the featured species names, for noticing when the day's picks change */
	Set<String> featuredKeys()
	{
		return Collections.unmodifiableSet(keys);
	}

	/** @return the time to the next UTC midnight, like "5h 12m" or "40m" */
	String resetText()
	{
		int hours = resetMinutes / 60;
		int minutes = resetMinutes % 60;
		return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
	}

	/**
	 * Builds the number of minutes left until the next UTC midnight.
	 *
	 * @param secondOfDay the UTC time of day in seconds, 0 to 86,399
	 * @return the minutes left, rounded up, at least 1
	 */
	static int minutesToMidnight(int secondOfDay)
	{
		int left = 86_400 - secondOfDay;
		return Math.max(1, (left + 59) / 60);
	}
}
