/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.List;

/**
 * One pairing of a team battle, seen from the player's side: which creature of each team fought, the few hits that
 * are acted out, the line that tells who was knocked out, and who won. It is only a record of what the dice
 * already decided, so keeping it changes no result.
 */
final class Duel
{
	private final int mineIndex;
	private final int theirIndex;
	private final String mineSpecies;
	private final String theirSpecies;
	private final boolean mineGilded;
	private final int mineLevel;
	private final int theirLevel;
	private final List<BattleLine> hits;
	private final BattleLine knockOut;
	private final boolean mineWon;

	/**
	 * Records a duel.
	 *
	 * @param mine the player's creature, which also tells the index
	 * @param mineIndex where the player's creature stands in its team
	 * @param theirs the opposing creature
	 * @param theirIndex where the opposing creature stands in its team
	 * @param hits every hit of the duel in order, seen from the player's side; cut down here to the few that are shown
	 * @param knockOut the line that tells who was knocked out
	 * @param mineWon whether the player's creature won
	 */
	Duel(Combatant mine, int mineIndex, Combatant theirs, int theirIndex, List<BattleLine> hits, BattleLine knockOut,
		boolean mineWon)
	{
		this.mineIndex = mineIndex;
		this.theirIndex = theirIndex;
		this.mineSpecies = mine.speciesKey();
		this.theirSpecies = theirs.speciesKey();
		this.mineGilded = mine.isGilded();
		this.mineLevel = mine.level();
		this.theirLevel = theirs.level();
		this.hits = Collections.unmodifiableList(TeamScript.cut(hits));
		this.knockOut = knockOut;
		this.mineWon = mineWon;
	}

	/** @return where the player's creature stands in its team */
	int mineIndex()
	{
		return mineIndex;
	}

	/** @return where the opposing creature stands in its team */
	int theirIndex()
	{
		return theirIndex;
	}

	/** @return the species key of the player's creature */
	String mineSpecies()
	{
		return mineSpecies;
	}

	/** @return the species key of the opposing creature */
	String theirSpecies()
	{
		return theirSpecies;
	}

	/** @return whether the player's creature is gilded */
	boolean mineGilded()
	{
		return mineGilded;
	}

	/** @return the level of the player's creature */
	int mineLevel()
	{
		return mineLevel;
	}

	/** @return the level of the opposing creature */
	int theirLevel()
	{
		return theirLevel;
	}

	/** @return the hits that are acted out: the first, one from the middle and the last, or all when there are few */
	List<BattleLine> hits()
	{
		return hits;
	}

	/** @return the line that tells who was knocked out */
	BattleLine knockOut()
	{
		return knockOut;
	}

	/** @return whether the player's creature won this duel */
	boolean mineWon()
	{
		return mineWon;
	}
}
