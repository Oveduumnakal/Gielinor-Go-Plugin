/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of learning moves, applied to a {@link Creature}'s saved lists. A creature knows at most
 * {@link MoveTable#MAX_KNOWN} moves. While it knows fewer, a move its level unlocks is learned at once; with a full
 * set, the new move waits in {@link Creature#pendingMoves} until the player picks a move to forget or skips it. It
 * holds no game client types.
 */
final class MoveLearning
{
	/** What a level-up did to a creature's moves. */
	static final class Result
	{
		private final List<Move> learned = new ArrayList<>();
		private final List<Move> queued = new ArrayList<>();

		/** @return the moves learned at once, in the order unlocked */
		List<Move> learned()
		{
			return learned;
		}

		/** @return the moves that now wait for the player's choice, in the order unlocked */
		List<Move> queued()
		{
			return queued;
		}
	}

	/** How a pending move was settled. */
	static final class Resolution
	{
		private final Move learned;
		private final Move forgotten;

		private Resolution(Move learned, Move forgotten)
		{
			this.learned = learned;
			this.forgotten = forgotten;
		}

		/** @return the move that was waiting */
		Move move()
		{
			return learned;
		}

		/** @return the move that was forgotten to make room, or null when the pending move was skipped */
		Move forgotten()
		{
			return forgotten;
		}

		/** @return whether the pending move was learned */
		boolean wasLearned()
		{
			return forgotten != null;
		}
	}

	private MoveLearning()
	{
	}

	/**
	 * Gives a creature the moves its level has unlocked, newest four, and clears anything pending. For a catch, a
	 * hatch and an old creature that has none.
	 *
	 * @param creature the creature
	 * @param species its species
	 * @param table the moves
	 */
	static void start(Creature creature, Species species, MoveTable table)
	{
		if (table.size() == 0)
			return;

		creature.moves = new ArrayList<>();
		creature.pendingMoves = new ArrayList<>();
		for (Move move : table.movesAt(species, creature.level()))
			creature.moves.add(move.id());
	}

	/**
	 * Makes a loaded creature's moves right: moves the table does not allow at this species and level are dropped
	 * (an unknown id, or a move the species' pool does not teach, or not yet), and a creature left short of moves is
	 * topped up with the newest ones its level has unlocked. With a table that knows no moves nothing is touched.
	 *
	 * @param creature the creature
	 * @param species its species
	 * @param table the moves
	 */
	static void settle(Creature creature, Species species, MoveTable table)
	{
		if (table.size() == 0)
			return;

		int level = creature.level();
		List<String> kept = new ArrayList<>();
		for (String id : creature.moves == null ? new ArrayList<String>() : creature.moves)
		{
			if (id != null && !kept.contains(id) && kept.size() < MoveTable.MAX_KNOWN
				&& table.knows(species, level, id))
				kept.add(id);
		}

		List<String> waiting = new ArrayList<>();
		for (String id : creature.pendingMoves == null ? new ArrayList<String>() : creature.pendingMoves)
		{
			if (id != null && !kept.contains(id) && !waiting.contains(id) && table.knows(species, level, id))
				waiting.add(id);
		}

		creature.moves = kept;
		creature.pendingMoves = waiting;
		if (waiting.isEmpty())
			topUp(creature, species, table);
	}

	private static void topUp(Creature creature, Species species, MoveTable table)
	{
		List<Move> fresh = new ArrayList<>();
		for (Move move : table.movesAt(species, creature.level()))
		{
			if (!creature.moves.contains(move.id()))
				fresh.add(move);
		}

		int room = MoveTable.MAX_KNOWN - creature.moves.size();
		for (int i = Math.max(0, fresh.size() - room); i < fresh.size(); i++)
			creature.moves.add(fresh.get(i).id());
	}

	/**
	 * Applies a level-up: every move the species unlocks between the two levels is learned while there is room, and
	 * queued once the creature knows a full set.
	 *
	 * @param creature the creature
	 * @param species its species now
	 * @param table the moves
	 * @param fromLevel the level it had
	 * @param toLevel the level it has now
	 * @return what was learned and what is waiting
	 */
	static Result levelUp(Creature creature, Species species, MoveTable table, int fromLevel, int toLevel)
	{
		Result result = new Result();
		ensureLists(creature);
		for (Move move : table.unlockedBetween(species, fromLevel, toLevel))
		{
			if (creature.moves.contains(move.id()) || creature.pendingMoves.contains(move.id()))
				continue;

			if (creature.moves.size() < MoveTable.MAX_KNOWN)
			{
				creature.moves.add(move.id());
				result.learned.add(move);
			}
			else
			{
				creature.pendingMoves.add(move.id());
				result.queued.add(move);
			}
		}

		return result;
	}

	/**
	 * Fits a creature's moves to a new species after an evolution. Moves the new species' pool still teaches at this
	 * level stay; the others go, and a creature left with fewer than four is topped up from what the level unlocks.
	 *
	 * @param creature the creature, already of its new species
	 * @param species the new species
	 * @param table the moves
	 */
	static void evolved(Creature creature, Species species, MoveTable table)
	{
		if (table.size() == 0)
			return;

		ensureLists(creature);
		int level = creature.level();
		creature.moves.removeIf(id -> !table.knows(species, level, id));
		creature.pendingMoves.removeIf(id -> !table.knows(species, level, id) || creature.moves.contains(id));
		if (creature.pendingMoves.isEmpty())
			topUp(creature, species, table);
	}

	/**
	 * Settles the oldest pending move.
	 *
	 * @param creature the creature
	 * @param table the moves
	 * @param forgetIndex the place in the known moves of the one to forget, or -1 to skip the pending move
	 * @return how it was settled, or null when nothing was waiting or the place is not one of the known moves
	 */
	static Resolution resolve(Creature creature, MoveTable table, int forgetIndex)
	{
		ensureLists(creature);
		if (creature.pendingMoves.isEmpty() || forgetIndex < -1 || forgetIndex >= creature.moves.size())
			return null;

		Move waiting = table.find(creature.pendingMoves.remove(0));
		if (waiting == null)
			return null;

		if (forgetIndex < 0)
			return new Resolution(waiting, null);

		Move forgotten = table.find(creature.moves.get(forgetIndex));
		creature.moves.set(forgetIndex, waiting.id());
		return new Resolution(waiting, forgotten);
	}

	/**
	 * Lists the moves a creature fights with. A creature that has none of its own, such as one a trade just brought
	 * in, fights with what its level has unlocked.
	 *
	 * @param creature the creature
	 * @param species its species
	 * @param table the moves
	 * @return the moves in the order the creature knows them; empty when the table has none for the species
	 */
	static List<Move> known(Creature creature, Species species, MoveTable table)
	{
		List<Move> moves = new ArrayList<>();
		if (creature.moves != null)
		{
			for (String id : creature.moves)
			{
				Move move = table.find(id);
				if (move != null)
					moves.add(move);
			}
		}

		return moves.isEmpty() ? table.movesAt(species, creature.level()) : moves;
	}

	/**
	 * Lists the ids of the moves a creature shows to a friend or sends in a trade. Only moves its species' pool allows
	 * at its level are listed, so the other side never turns it away.
	 *
	 * @param creature the creature
	 * @param species its species
	 * @param table the moves
	 * @return the ids, in the order the creature knows the moves; empty when the table has none for the species
	 */
	static String[] sendable(Creature creature, Species species, MoveTable table)
	{
		List<String> ids = new ArrayList<>();
		for (Move move : known(creature, species, table))
		{
			if (table.knows(species, creature.level(), move.id()))
				ids.add(move.id());
		}

		return ids.toArray(new String[0]);
	}

	/**
	 * Lists the moves waiting for a choice.
	 *
	 * @param creature the creature
	 * @param table the moves
	 * @return the pending moves, oldest first
	 */
	static List<Move> pending(Creature creature, MoveTable table)
	{
		List<Move> moves = new ArrayList<>();
		if (creature.pendingMoves != null)
		{
			for (String id : creature.pendingMoves)
			{
				Move move = table.find(id);
				if (move != null)
					moves.add(move);
			}
		}

		return moves;
	}

	private static void ensureLists(Creature creature)
	{
		if (creature.moves == null)
			creature.moves = new ArrayList<>();

		if (creature.pendingMoves == null)
			creature.pendingMoves = new ArrayList<>();
	}
}
