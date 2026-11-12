/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the side panel draws, copied out of the game at one moment. Immutable, so the game thread can hand it
 * to the UI thread without either one seeing a half-finished change.
 */
final class PanelState
{
	private final int trainerLevel;
	private final double trainerProgress;
	private final int battlesWon;
	private final Map<JarType, Integer> jars;
	private final JarType selectedJar;
	private final int stepProgress;
	private final List<CreatureView> team;
	private final List<CreatureView> storage;
	private final List<LogEntry> log;
	private final int caughtSpecies;
	private final int seenSpecies;
	private final List<EggView> eggs;
	private final List<ChampionView> champions;
	private final TodayView today;
	private final TaskView tasks;
	private final BattleView battle;

	PanelState(int trainerLevel, double trainerProgress, int battlesWon, Map<JarType, Integer> jars,
		JarType selectedJar, int stepProgress, List<CreatureView> team, List<CreatureView> storage,
		List<LogEntry> log, int caughtSpecies, int seenSpecies, List<EggView> eggs, List<ChampionView> champions,
		TodayView today, TaskView tasks)
	{
		this(trainerLevel, trainerProgress, battlesWon, jars, selectedJar, stepProgress, team, storage, log,
			caughtSpecies, seenSpecies, eggs, champions, today, tasks, null);
	}

	/**
	 * Builds a snapshot that also carries the battle waiting for a choice.
	 *
	 * @param trainerLevel the trainer level
	 * @param trainerProgress progress to the next trainer level
	 * @param battlesWon battles won so far
	 * @param jars jars owned
	 * @param selectedJar the jar the player chose to throw
	 * @param stepProgress tiles walked towards the next jar
	 * @param team the team, lead first
	 * @param storage the stored creatures
	 * @param log the Creature Log
	 * @param caughtSpecies how many species were caught
	 * @param seenSpecies how many species were seen
	 * @param eggs the eggs carried
	 * @param champions every City Champion
	 * @param today the Today section
	 * @param tasks the Tasks section
	 * @param battle the battle that waits for a choice, or null when none does
	 */
	PanelState(int trainerLevel, double trainerProgress, int battlesWon, Map<JarType, Integer> jars,
		JarType selectedJar, int stepProgress, List<CreatureView> team, List<CreatureView> storage,
		List<LogEntry> log, int caughtSpecies, int seenSpecies, List<EggView> eggs, List<ChampionView> champions,
		TodayView today, TaskView tasks, BattleView battle)
	{
		this.battle = battle;
		this.today = today;
		this.tasks = tasks;
		this.eggs = Collections.unmodifiableList(new ArrayList<>(eggs));
		this.champions = Collections.unmodifiableList(new ArrayList<>(champions));
		this.trainerLevel = trainerLevel;
		this.trainerProgress = trainerProgress;
		this.battlesWon = battlesWon;
		this.jars = Collections.unmodifiableMap(new EnumMap<>(jars));
		this.selectedJar = selectedJar;
		this.stepProgress = stepProgress;
		this.team = Collections.unmodifiableList(new ArrayList<>(team));
		this.storage = Collections.unmodifiableList(new ArrayList<>(storage));
		this.log = log;
		this.caughtSpecies = caughtSpecies;
		this.seenSpecies = seenSpecies;
	}

	/**
	 * Copies the snapshot with the battle that waits for a choice, so the panel can show the battle box.
	 *
	 * @param waiting the battle, or null when none waits
	 * @return the new snapshot
	 */
	PanelState withBattle(BattleView waiting)
	{
		return new PanelState(trainerLevel, trainerProgress, battlesWon, jars, selectedJar, stepProgress, team,
			storage, log, caughtSpecies, seenSpecies, eggs, champions, today, tasks, waiting);
	}

	/** @return the battle that waits for the player's choice, or null when none does */
	BattleView battle()
	{
		return battle;
	}

	/** @return the trainer level, 1 to 99 */
	int trainerLevel()
	{
		return trainerLevel;
	}

	/** @return progress to the next trainer level, from 0 to 1 */
	double trainerProgress()
	{
		return trainerProgress;
	}

	/** @return battles won so far */
	int battlesWon()
	{
		return battlesWon;
	}

	/** @return jars owned, with an entry for every jar type */
	Map<JarType, Integer> jars()
	{
		return jars;
	}

	/** @return the jar the player chose to throw */
	JarType selectedJar()
	{
		return selectedJar;
	}

	/** @return tiles walked towards the next jar, from 0 up to {@link GoGame#TILES_PER_JAR} */
	int stepProgress()
	{
		return stepProgress;
	}

	/** @return the team, lead first, at most {@link SaveData#TEAM_SIZE} */
	List<CreatureView> team()
	{
		return team;
	}

	/** @return stored creatures, in the order they were stored */
	List<CreatureView> storage()
	{
		return storage;
	}

	/**
	 * The Creature Log, weakest species first. The same list instance is reused until a catch or a sighting changes
	 * it, so the panel can compare by identity to skip redrawing.
	 *
	 * @return one entry per species
	 */
	List<LogEntry> log()
	{
		return log;
	}

	/** @return how many different species have been caught */
	int caughtSpecies()
	{
		return caughtSpecies;
	}

	/** @return how many different species have been seen wild */
	int seenSpecies()
	{
		return seenSpecies;
	}

	/** @return the eggs being carried, at most {@link SaveData#MAX_EGGS} */
	List<EggView> eggs()
	{
		return eggs;
	}

	/** @return every City Champion, easiest first, with whether it is beaten and whether the player is there */
	List<ChampionView> champions()
	{
		return champions;
	}

	/** @return what the Today section shows: the featured species, the event and night */
	TodayView today()
	{
		return today;
	}

	/** @return what the Tasks section shows: the daily and weekly tasks, the streak and the reset countdown */
	TaskView tasks()
	{
		return tasks;
	}

	/** @return how many species exist */
	int totalSpecies()
	{
		return log.size();
	}
}
