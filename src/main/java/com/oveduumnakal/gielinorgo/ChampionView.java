/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Objects;

/** One City Champion as the side panel shows it. Immutable, so it can cross from the game thread to the UI. */
final class ChampionView
{
	private final String id;
	private final String city;
	private final String team;
	private final String reward;
	private final boolean beaten;
	private final boolean here;

	ChampionView(String id, String city, String team, String reward, boolean beaten, boolean here)
	{
		this.id = id;
		this.city = city;
		this.team = team;
		this.reward = reward;
		this.beaten = beaten;
		this.here = here;
	}

	/** @return the id the panel passes back when the player challenges this champion */
	String id()
	{
		return id;
	}

	/** @return the city's name */
	String city()
	{
		return city;
	}

	/** @return the champion's creatures in one line, such as {@code Goblin 4, Cow 5, Giant rat 7} */
	String team()
	{
		return team;
	}

	/** @return the first-win reward in a few words, such as {@code 3 Iron jars} */
	String reward()
	{
		return reward;
	}

	/** @return whether the player has beaten this champion and holds the city's sigil */
	boolean beaten()
	{
		return beaten;
	}

	/** @return whether the player is standing in this city right now, so the champion can be challenged */
	boolean here()
	{
		return here;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof ChampionView))
			return false;

		ChampionView that = (ChampionView) other;
		return beaten == that.beaten
			&& here == that.here
			&& Objects.equals(id, that.id)
			&& Objects.equals(city, that.city)
			&& Objects.equals(team, that.team)
			&& Objects.equals(reward, that.reward);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, city, team, reward, beaten, here);
	}
}
