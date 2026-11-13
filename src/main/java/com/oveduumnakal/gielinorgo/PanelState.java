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
	private MilestoneView milestones = MilestoneView.NONE;
	private EliteView elite = EliteView.NONE;
	private RivalView rival = RivalView.NONE;
	private String resetSummary = "";
	private List<StarterView> starters = Collections.emptyList();
	private final Map<ItemType, Integer> items;

	PanelState(int trainerLevel, double trainerProgress, int battlesWon, Map<JarType, Integer> jars,
		JarType selectedJar, int stepProgress, List<CreatureView> team, List<CreatureView> storage,
		List<LogEntry> log, int caughtSpecies, int seenSpecies, List<EggView> eggs, List<ChampionView> champions,
		TodayView today, TaskView tasks)
	{
		this(trainerLevel, trainerProgress, battlesWon, jars, selectedJar, stepProgress, team, storage, log,
			caughtSpecies, seenSpecies, eggs, champions, today, tasks, Collections.emptyMap());
	}

	/**
	 * Builds a snapshot with the healing items owned and no battle waiting.
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
	 * @param items healing items owned
	 */
	PanelState(int trainerLevel, double trainerProgress, int battlesWon, Map<JarType, Integer> jars,
		JarType selectedJar, int stepProgress, List<CreatureView> team, List<CreatureView> storage,
		List<LogEntry> log, int caughtSpecies, int seenSpecies, List<EggView> eggs, List<ChampionView> champions,
		TodayView today, TaskView tasks, Map<ItemType, Integer> items)
	{
		this(trainerLevel, trainerProgress, battlesWon, jars, selectedJar, stepProgress, team, storage, log,
			caughtSpecies, seenSpecies, eggs, champions, today, tasks, items, null);
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
	 * @param items healing items owned
	 * @param battle the battle that waits for a choice, or null when none does
	 */
	private PanelState(int trainerLevel, double trainerProgress, int battlesWon, Map<JarType, Integer> jars,
		JarType selectedJar, int stepProgress, List<CreatureView> team, List<CreatureView> storage,
		List<LogEntry> log, int caughtSpecies, int seenSpecies, List<EggView> eggs, List<ChampionView> champions,
		TodayView today, TaskView tasks, Map<ItemType, Integer> items, BattleView battle)
	{
		this.battle = battle;
		Map<ItemType, Integer> copy = new EnumMap<>(ItemType.class);
		copy.putAll(items);
		this.items = Collections.unmodifiableMap(copy);
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
		PanelState copy = new PanelState(trainerLevel, trainerProgress, battlesWon, jars, selectedJar, stepProgress,
			team, storage, log, caughtSpecies, seenSpecies, eggs, champions, today, tasks, items, waiting);
		copy.resetSummary = resetSummary;
		copy.starters = starters;
		copy.rival = rival;
		return copy.withGoals(milestones, elite);
	}

	/**
	 * Adds the line the reset confirm dialog shows, to a state that is still being built.
	 *
	 * @param summary what a reset would lose, in words
	 * @return this state
	 */
	PanelState withResetSummary(String summary)
	{
		this.resetSummary = summary;
		return this;
	}

	/**
	 * Adds the starters on offer, to a state that is still being built.
	 *
	 * @param offer the starters, or an empty list when none is offered
	 * @return this state
	 */
	PanelState withStarters(List<StarterView> offer)
	{
		this.starters = Collections.unmodifiableList(new ArrayList<>(offer));
		return this;
	}

	/**
	 * Adds the rival card, to a state that is still being built.
	 *
	 * @param card what the rival card shows
	 * @return this state
	 */
	PanelState withRival(RivalView card)
	{
		this.rival = card;
		return this;
	}

	/** @return what the rival card on the Battle tab shows */
	RivalView rival()
	{
		return rival;
	}

	/** @return the starter creatures on offer to a new player; empty when none is */
	List<StarterView> starters()
	{
		return starters;
	}

	/** @return what a reset would lose, in words, for the confirm dialog */
	String resetSummary()
	{
		return resetSummary;
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

	/**
	 * Adds the end-game goals to a state that is still being built. Call it before the state is shared.
	 *
	 * @param milestoneView the Creature Log milestones
	 * @param eliteView the Elite challenge card
	 * @return this state
	 */
	PanelState withGoals(MilestoneView milestoneView, EliteView eliteView)
	{
		this.milestones = milestoneView;
		this.elite = eliteView;
		return this;
	}

	/** @return the Elite challenge card for the Battle tab */
	EliteView elite()
	{
		return elite;
	}

	/** @return the next Creature Log milestone and the full list, for the Log tab */
	MilestoneView milestones()
	{
		return milestones;
	}

	/** @return healing items owned; every item has an entry once the game has built the state */
	Map<ItemType, Integer> items()
	{
		return items;
	}

	/** @return how many species can be caught: the Log entries that are not retired quest-only species */
	int totalSpecies()
	{
		int total = 0;
		for (LogEntry entry : log)
		{
			if (!entry.retired())
				total++;
		}

		return total;
	}
}
