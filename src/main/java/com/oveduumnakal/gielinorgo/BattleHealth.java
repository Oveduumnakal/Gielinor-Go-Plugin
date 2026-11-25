/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The health both creatures of a battle have around one line: just before it happens and once it has. The scene shows
 * it as bars under the names and the pinned party box follows it. All shares are of full health, from 0 to 1.
 */
final class BattleHealth
{
	private final int minePlace;
	private final double leadBefore;
	private final double foeBefore;
	private final double lead;
	private final double foe;

	/**
	 * @param minePlace the player's creature's place in the battle team, or -1 when it is not the player's team
	 * @param leadBefore the share the creature on the player's side of the scene had
	 * @param foeBefore the share the other creature had
	 * @param lead the share the creature on the player's side has once the line has happened
	 * @param foe the share the other creature has then
	 */
	BattleHealth(int minePlace, double leadBefore, double foeBefore, double lead, double foe)
	{
		this.minePlace = minePlace;
		this.leadBefore = leadBefore;
		this.foeBefore = foeBefore;
		this.lead = lead;
		this.foe = foe;
	}

	int minePlace()
	{
		return minePlace;
	}

	double leadBefore()
	{
		return leadBefore;
	}

	double foeBefore()
	{
		return foeBefore;
	}

	double lead()
	{
		return lead;
	}

	double foe()
	{
		return foe;
	}
}
