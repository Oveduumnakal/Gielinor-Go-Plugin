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
	private final JarType rewardJar;
	private final String rematchTeam;
	private final boolean gold;
	private final String rematchNote;

	ChampionView(String id, String city, String team, String reward, boolean beaten, boolean here)
	{
		this(id, city, team, reward, beaten, here, null);
	}

	ChampionView(String id, String city, String team, String reward, boolean beaten, boolean here,
		JarType rewardJar)
	{
		this.id = id;
		this.city = city;
		this.team = team;
		this.reward = reward;
		this.beaten = beaten;
		this.here = here;
		this.rewardJar = rewardJar;
		this.rematchTeam = null;
		this.gold = false;
		this.rematchNote = null;
	}

	private ChampionView(ChampionView base, String rematchTeam, boolean gold, String rematchNote)
	{
		this.id = base.id;
		this.city = base.city;
		this.team = base.team;
		this.reward = base.reward;
		this.beaten = base.beaten;
		this.here = base.here;
		this.rewardJar = base.rewardJar;
		this.rematchTeam = rematchTeam;
		this.gold = gold;
		this.rematchNote = rematchNote;
	}

	/**
	 * Makes the same card with the rematch line, shown once the Elite challenge has been cleared.
	 *
	 * @param rematchTeam the rematch creatures in one line
	 * @param gold whether the rematch has been won, which turns the sigil gold
	 * @param rematchNote when the daily reward is ready, such as {@code Reward ready}
	 * @return a new view
	 */
	ChampionView withRematch(String rematchTeam, boolean gold, String rematchNote)
	{
		return new ChampionView(this, rematchTeam, gold, rematchNote);
	}

	/** @return whether the card offers a rematch, which needs a cleared Elite challenge */
	boolean rematchOpen()
	{
		return rematchTeam != null;
	}

	/** @return the rematch creatures in one line, or null before the Elite challenge is cleared */
	String rematchTeam()
	{
		return rematchTeam;
	}

	/** @return whether the rematch has been won, so the city's sigil is gold */
	boolean gold()
	{
		return gold;
	}

	/** @return when the daily rematch reward is ready, or null before the Elite challenge is cleared */
	String rematchNote()
	{
		return rematchNote;
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

	/** @return the jar the first-win reward is paid in, whose picture the card shows, or null */
	JarType rewardJar()
	{
		return rewardJar;
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
			&& rewardJar == that.rewardJar
			&& gold == that.gold
			&& Objects.equals(rematchTeam, that.rematchTeam)
			&& Objects.equals(rematchNote, that.rematchNote)
			&& Objects.equals(id, that.id)
			&& Objects.equals(city, that.city)
			&& Objects.equals(team, that.team)
			&& Objects.equals(reward, that.reward);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, city, team, reward, beaten, here, rewardJar, rematchTeam, gold, rematchNote);
	}
}
