/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The health the pinned party box shows while a battle that played itself is still being shown. Its result is not
 * written to the save until later, so each line that appears hands over the health it leaves. It is display only and
 * free of RuneLite types.
 */
final class LiveHealth
{
	private final Map<Integer, Double> shown = new HashMap<>();
	private Object battle;

	/**
	 * Starts following a battle and forgets any other.
	 *
	 * @param followed the battle, or null to follow none
	 */
	void follow(Object followed)
	{
		battle = followed;
		shown.clear();
	}

	/**
	 * Takes the health a shown line leaves. A line of a battle that is no longer followed changes nothing.
	 *
	 * @param step the health, or null when the line carries none
	 * @return whether the health to show changed
	 */
	boolean show(Step step)
	{
		if (step == null || step.battle != battle)
			return false;

		Double before = shown.put(step.teamPlace, step.health);
		return before == null || before != step.health;
	}

	/**
	 * Lays the followed health over the saved team.
	 *
	 * @param team the team as saved, in team order
	 * @return the team as it should be shown
	 */
	List<CreatureView> over(List<CreatureView> team)
	{
		if (shown.isEmpty())
			return team;

		List<CreatureView> views = new ArrayList<>(team);
		for (Map.Entry<Integer, Double> entry : shown.entrySet())
		{
			int place = entry.getKey();
			if (place >= 0 && place < views.size())
				views.set(place, views.get(place).withHealthNow(entry.getValue()));
		}

		return views;
	}

	/** The health one line of one battle leaves one team member with. */
	static final class Step
	{
		private final Object battle;
		private final int teamPlace;
		private final double health;

		/**
		 * @param battle the battle the line belongs to
		 * @param teamPlace the creature's place in the saved team
		 * @param health the share of full health left, from 0 to 1
		 */
		Step(Object battle, int teamPlace, double health)
		{
			this.battle = battle;
			this.teamPlace = teamPlace;
			this.health = health;
		}
	}
}
