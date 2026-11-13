/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One attempt at the Elite challenge: four Elite trainers and the Grand Champion, back to back. Pure rules with no
 * RuneLite types. The run holds which battle is next; the team's carried health lives on the creatures, which the
 * game freezes while the run is open so nothing rests. A lost battle or a forfeit ends the run, and the next
 * attempt starts at the first trainer. The run is never saved: logging out forfeits it.
 *
 * <p>Each battle is played by a {@link Fight}. The default is the automatic {@link BattleSim#teamFight}; a manual
 * battle session can be plugged in by giving the run another one.
 */
final class EliteRun
{
	/** Sigils the player needs before the challenge unlocks: one for every City Champion. */
	static final int SIGILS_NEEDED = 8;

	/** Why a challenge cannot start. */
	static final String NEEDS_EGG_SLOT = "Free an egg slot first";

	/** Plays one battle of the run: the player's team against one Elite team. */
	interface Fight
	{
		/**
		 * Plays the next battle.
		 *
		 * @param mine the player's creatures, in the order they fight; their health is changed by the battle
		 * @param theirs the opposing creatures, in the order they fight
		 * @param random the source of every roll
		 * @return the outcome
		 */
		TeamBattleResult play(List<Combatant> mine, List<Combatant> theirs, Random random);
	}

	/** Where a run stands. */
	enum State
	{
		/** A battle is next. */
		OPEN,

		/** The Grand Champion was beaten. */
		CLEARED,

		/** A battle was lost. */
		LOST,

		/** The player gave up, or logged out. */
		FORFEITED
	}

	private final EliteTable table;
	private final SpeciesTable species;
	private final Fight fighter;
	private int stage;
	private State state = State.OPEN;
	private MoveTable moves = MoveTable.empty();

	/**
	 * Opens a run that plays its battles automatically.
	 *
	 * @param table the Elite teams
	 * @param species every catchable species
	 */
	EliteRun(EliteTable table, SpeciesTable species)
	{
		this(table, species, BattleSim::teamFight);
	}

	/**
	 * Opens a run with a chosen way of playing its battles.
	 *
	 * @param table the Elite teams
	 * @param species every catchable species
	 * @param fight plays each battle
	 */
	EliteRun(EliteTable table, SpeciesTable species, Fight fight)
	{
		this.table = table;
		this.species = species;
		this.fighter = fight;
	}

	/**
	 * Gives the opposing creatures their moves. Without this call they fight with one basic attack.
	 *
	 * @param table the moves
	 * @return this run
	 */
	EliteRun withMoves(MoveTable table)
	{
		this.moves = table == null ? MoveTable.empty() : table;
		return this;
	}

	/**
	 * Says why a challenge cannot start now.
	 *
	 * @param table the Elite teams
	 * @param sigils how many City Champion sigils the player holds
	 * @param here whether the player stands in the Champions' Guild rectangle
	 * @param eggRoom whether the player can carry one more egg
	 * @param fit whether at least one creature on the team is healthy enough to fight
	 * @return the reason in words for the player, or null when the run may start
	 */
	static String refusal(EliteTable table, int sigils, boolean here, boolean eggRoom, boolean fit)
	{
		if (!table.available())
			return "The Elite challenge is not available";

		if (sigils < SIGILS_NEEDED)
			return "Earn all " + SIGILS_NEEDED + " sigils first (" + sigils + " / " + SIGILS_NEEDED + ")";

		if (!here)
			return "Go to the Champions' Guild to start the Elite challenge";

		if (!eggRoom)
			return NEEDS_EGG_SLOT;

		return fit ? null : "Your team is too tired";
	}

	/** @return how the run stands */
	State state()
	{
		return state;
	}

	/** @return whether a battle is next */
	boolean open()
	{
		return state == State.OPEN;
	}

	/** @return the number of the next battle, counted from 0 */
	int stage()
	{
		return stage;
	}

	/** @return the name of the opponent of the next battle, or of the last one once the run is over */
	String opponent()
	{
		return current().city();
	}

	private Champion current()
	{
		List<Champion> stages = table.stages();
		return stages.get(Math.min(stage, stages.size() - 1));
	}

	/** @return the opposing team of the next battle, as a line such as {@code Red dragon 70, Frost dragon 71} */
	String opponentTeam()
	{
		List<String> names = new ArrayList<>();
		for (Champion.Member member : current().team())
			names.add(species.find(member.species()).name() + " " + member.level());

		return String.join(", ", names);
	}

	/**
	 * Plays the next battle. Winning moves the run on, and winning the last one clears it. Losing ends it and sets
	 * the next attempt back to the first trainer.
	 *
	 * @param mine the player's creatures, in the order they fight, carrying their health from the last battle
	 * @param random the source of every roll
	 * @return the battle, or null when the run is already over
	 */
	TeamBattleResult fight(List<Combatant> mine, Random random)
	{
		if (!open())
			return null;

		TeamBattleResult fought = fighter.play(mine, opponents(), random);
		record(fought.won());
		return fought;
	}

	/**
	 * Builds the opposing creatures of the next battle, with their moves.
	 *
	 * @return the creatures in the order they fight
	 */
	List<Combatant> opponents()
	{
		List<Combatant> theirs = new ArrayList<>();
		for (Champion.Member member : table.stages().get(stage).team())
		{
			Species found = species.find(member.species());
			theirs.add(new Combatant(found, member.level(), 1.0).moves(moves.movesAt(found, member.level())));
		}

		return theirs;
	}

	/** @return the Elite team of the next battle */
	Champion foe()
	{
		return table.stages().get(stage);
	}

	/**
	 * Notes how a battle played outside {@link #fight} ended, such as one the player steered. Winning moves the run
	 * on, and winning the last one clears it. Losing, or running, ends it and sets the next attempt back to the
	 * first trainer. Nothing happens when the run is already over.
	 *
	 * @param won whether the player beat the Elite team
	 */
	void record(boolean won)
	{
		if (!open())
			return;

		if (!won)
		{
			state = State.LOST;
			stage = 0;
		}
		else if (stage + 1 >= table.stages().size())
		{
			state = State.CLEARED;
		}
		else
		{
			stage++;
		}
	}

	/** Gives the run up. The next attempt starts at the first trainer. */
	void forfeit()
	{
		if (open())
		{
			state = State.FORFEITED;
			stage = 0;
		}
	}
}
