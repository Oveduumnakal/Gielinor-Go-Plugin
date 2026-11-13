/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.List;

/**
 * A City Champion: a fixed team the player can challenge from the side panel while standing inside the city. One
 * entry of the bundled {@code champions.json}.
 */
final class Champion
{
	/** One creature on a champion's team, as written in the data file. */
	static final class Member
	{
		private String species;
		private int level;

		/** Used by Gson. */
		Member()
		{
		}

		Member(String species, int level)
		{
			this.species = species;
			this.level = level;
		}

		/** @return the species name */
		String species()
		{
			return species;
		}

		/** @return the creature level, 1 to 99 */
		int level()
		{
			return Math.max(1, Math.min(XpRules.MAX_LEVEL, level));
		}
	}

	private String id;
	private String city;
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	private List<Member> team;
	private String jar;
	private int jars;
	private int rivalBattle;

	/** Used by Gson. */
	Champion()
	{
	}

	Champion(String id, String city, int minX, int minY, int maxX, int maxY, List<Member> team, JarType jar,
		int jars)
	{
		this.id = id;
		this.city = city;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.team = team;
		this.jar = jar.name();
		this.jars = jars;
	}

	/**
	 * Makes the rival's team for one battle, fought like a champion's.
	 *
	 * @param name the rival's name
	 * @param battle which of the rival's battles this is, counted from 1
	 * @param team the rival's creatures, in order
	 * @param jar the jar a first win pays
	 * @param jars how many of them
	 * @return the rival as a champion with no city
	 */
	static Champion rival(String name, int battle, List<Member> team, JarType jar, int jars)
	{
		Champion rival = new Champion(RivalRules.ID, name, 0, 0, 0, 0, team, jar, jars);
		rival.rivalBattle = battle;
		return rival;
	}

	/** @return whether this is the rival rather than a City Champion */
	boolean rival()
	{
		return rivalBattle > 0;
	}

	/** @return which of the rival's battles this is, counted from 1, or 0 for any other champion */
	int rivalBattle()
	{
		return rivalBattle;
	}

	/** @return the key this champion is saved under */
	String id()
	{
		return id;
	}

	/** @return the city's name */
	String city()
	{
		return city;
	}

	/** @return the champion's title, such as {@code Champion of Lumbridge}; the rival's name for the rival */
	String title()
	{
		return rival() ? city : "Champion of " + city;
	}

	/** @return the creatures the champion sends out, in order */
	List<Member> team()
	{
		return team == null ? Collections.emptyList() : team;
	}

	/**
	 * Makes the same champion with another team, which is how a rematch is fought: same city, id and rewards.
	 *
	 * @param other the creatures the champion sends out instead
	 * @return a new champion
	 */
	Champion withTeam(List<Member> other)
	{
		Champion copy = new Champion(id, city, minX, minY, maxX, maxY, other, rewardJar(), jars);
		copy.jar = jar;
		return copy;
	}

	/** @return the level of the champion's strongest creature */
	int topLevel()
	{
		int top = 1;
		for (Member member : team())
			top = Math.max(top, member.level());

		return top;
	}

	/** @return the jar type given for the first win, Iron when the data file names none the game knows */
	JarType rewardJar()
	{
		for (JarType type : JarType.values())
		{
			if (!type.specialty() && type.name().equals(jar))
				return type;
		}

		return JarType.IRON;
	}

	/** @return how many reward jars the first win gives */
	int rewardJars()
	{
		return Math.max(1, jars);
	}

	/**
	 * Checks whether a surface tile is inside the city.
	 *
	 * @param x world x
	 * @param y world y
	 * @return true when the tile is within the city's rectangle
	 */
	boolean contains(int x, int y)
	{
		return x >= minX && x <= maxX && y >= minY && y <= maxY;
	}

	/**
	 * Checks whether this city's rectangle shares any tile with another's.
	 *
	 * @param other the other champion
	 * @return true when the rectangles overlap
	 */
	boolean overlaps(Champion other)
	{
		return minX <= other.maxX && other.minX <= maxX && minY <= other.maxY && other.minY <= maxY;
	}
}
