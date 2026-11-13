/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What one turn of a battle did: the lines it made, who fell or stepped in, and where the battle stands now. */
final class TurnResult
{
	/** Where a battle stands after a turn. */
	enum Status
	{
		/** Nobody has won; the player chooses again. */
		ONGOING,

		/** The player's creature fainted and others can fight: the next choice must be a free switch. */
		NEEDS_SWITCH,

		/** The opposing side is out of creatures. */
		WON,

		/** The player's whole team is down. */
		LOST,

		/** The wild creature was caught. */
		CAUGHT,

		/** The wild creature broke out of a jar and ran away. */
		FOE_FLED,

		/** The player ran away. */
		RAN;

		/** @return whether the battle is over */
		boolean over()
		{
			return this != ONGOING && this != NEEDS_SWITCH;
		}
	}

	private final Status status;
	private final List<BattleLine> lines;
	private final List<BattleLine> hits;
	private final Duel duel;
	private final boolean turnUsed;
	private final Combatant enteredMine;
	private final Combatant enteredFoe;
	private final boolean mineFainted;
	private final boolean foeFainted;
	private final boolean voluntarySwitch;
	private final CatchOutcome thrown;
	private final boolean voluntaryFoeSwitch;

	/**
	 * Builds a result.
	 *
	 * @param status where the battle stands
	 * @param lines every line of the turn, in order
	 * @param hits the lines that are blows, in order
	 * @param duel the finished pairing when this turn ended one in a team battle, else null
	 * @param turnUsed whether the turn was used; false when a jar could not be thrown
	 * @param enteredMine the player's creature that stepped in during this turn, or null
	 * @param enteredFoe the opposing creature that stepped in during this turn, or null
	 * @param mineFainted whether the player's creature in the fight fell this turn
	 * @param foeFainted whether the opposing creature in the fight fell this turn
	 * @param voluntarySwitch whether the player switched by choice, so the creature before it came back
	 */
	TurnResult(Status status, List<BattleLine> lines, List<BattleLine> hits, Duel duel, boolean turnUsed,
		Combatant enteredMine, Combatant enteredFoe, boolean mineFainted, boolean foeFainted,
		boolean voluntarySwitch)
	{
		this(status, lines, hits, duel, turnUsed, enteredMine, enteredFoe, mineFainted, foeFainted, voluntarySwitch,
			null);
	}

	/**
	 * Builds a result for a turn in which a jar was thrown.
	 *
	 * @param status where the battle stands
	 * @param lines every line of the turn, in order
	 * @param hits the lines that are blows, in order
	 * @param duel the finished pairing when this turn ended one in a team battle, else null
	 * @param turnUsed whether the turn was used; false when a jar could not be thrown
	 * @param enteredMine the player's creature that stepped in during this turn, or null
	 * @param enteredFoe the opposing creature that stepped in during this turn, or null
	 * @param mineFainted whether the player's creature in the fight fell this turn
	 * @param foeFainted whether the opposing creature in the fight fell this turn
	 * @param voluntarySwitch whether the player switched by choice, so the creature before it came back
	 * @param thrown what the thrown jar did, or null when no jar was thrown this turn
	 */
	TurnResult(Status status, List<BattleLine> lines, List<BattleLine> hits, Duel duel, boolean turnUsed,
		Combatant enteredMine, Combatant enteredFoe, boolean mineFainted, boolean foeFainted,
		boolean voluntarySwitch, CatchOutcome thrown)
	{
		this(status, lines, hits, duel, turnUsed, enteredMine, enteredFoe, mineFainted, foeFainted, voluntarySwitch,
			thrown, false);
	}

	/**
	 * Builds a result for a turn of a battle in which the opposing side makes its own choices.
	 *
	 * @param status where the battle stands
	 * @param lines every line of the turn, in order
	 * @param hits the lines that are blows, in order
	 * @param duel the finished pairing when this turn ended one in a team battle, else null
	 * @param turnUsed whether the turn was used
	 * @param enteredMine the player's creature that stepped in during this turn, or null
	 * @param enteredFoe the opposing creature that stepped in during this turn, or null
	 * @param mineFainted whether the player's creature in the fight fell this turn
	 * @param foeFainted whether the opposing creature in the fight fell this turn
	 * @param voluntarySwitch whether the player switched by choice, so the creature before it came back
	 * @param thrown what the thrown jar did, or null when no jar was thrown this turn
	 * @param voluntaryFoeSwitch whether the opposing side switched by choice, so its creature before came back
	 */
	TurnResult(Status status, List<BattleLine> lines, List<BattleLine> hits, Duel duel, boolean turnUsed,
		Combatant enteredMine, Combatant enteredFoe, boolean mineFainted, boolean foeFainted,
		boolean voluntarySwitch, CatchOutcome thrown, boolean voluntaryFoeSwitch)
	{
		this.voluntaryFoeSwitch = voluntaryFoeSwitch;
		this.thrown = thrown;
		this.status = status;
		this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
		this.hits = Collections.unmodifiableList(new ArrayList<>(hits));
		this.duel = duel;
		this.turnUsed = turnUsed;
		this.enteredMine = enteredMine;
		this.enteredFoe = enteredFoe;
		this.mineFainted = mineFainted;
		this.foeFainted = foeFainted;
		this.voluntarySwitch = voluntarySwitch;
	}

	/** @return whether the opposing side switched by choice this turn, so the creature before it came back */
	boolean voluntaryFoeSwitch()
	{
		return voluntaryFoeSwitch;
	}

	/** @return what the jar thrown this turn did, or null when no jar was thrown */
	CatchOutcome thrown()
	{
		return thrown;
	}

	/** @return where the battle stands */
	Status status()
	{
		return status;
	}

	/** @return every line of the turn, in order */
	List<BattleLine> lines()
	{
		return lines;
	}

	/** @return the lines of the turn that are blows, in order */
	List<BattleLine> hits()
	{
		return hits;
	}

	/** @return the pairing this turn finished, in a battle that records them, else null */
	Duel duel()
	{
		return duel;
	}

	/** @return false when the choice used no turn, as a jar thrown with none left */
	boolean turnUsed()
	{
		return turnUsed;
	}

	/** @return the player's creature that stepped in during this turn, or null */
	Combatant enteredMine()
	{
		return enteredMine;
	}

	/** @return the opposing creature that stepped in during this turn, or null */
	Combatant enteredFoe()
	{
		return enteredFoe;
	}

	/** @return whether the player's creature in the fight fell this turn */
	boolean mineFainted()
	{
		return mineFainted;
	}

	/** @return whether the opposing creature in the fight fell this turn */
	boolean foeFainted()
	{
		return foeFainted;
	}

	/** @return whether the player switched by choice during this turn, so the creature before it came back */
	boolean voluntarySwitch()
	{
		return voluntarySwitch;
	}
}
