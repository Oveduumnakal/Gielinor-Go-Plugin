/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The order of events when a team battle is acted out on screen: for each duel the creatures that are new fade in,
 * the hits of the duel are played one beat each, the loser goes down and lies there, and fades out. Every number
 * that sets the pace lives here, so the length of a whole battle can be tested and tuned in one place. The result
 * of the battle was decided before the script is made; the script only shows it. Holds no RuneLite types, and
 * is moved on by game ticks.
 */
final class TeamScript
{
	/** Most hits shown for one duel: the first, one from the middle and the last. The rest are not acted out. */
	static final int TEAM_HIT_BEATS = 3;

	/** Game ticks the loser of a duel lies on the ground before it fades out: about two seconds. */
	static final int TEAM_HOLD_TICKS = 3;

	/** Game ticks a creature takes to fade in or out. */
	static final int FADE_TICKS = 1;

	/** Game ticks from the start of a hit's beat to its line: the blow lands then, as in a wild battle. */
	static final int LINE_DELAY_TICKS = 1;

	/** What happens in one entry of the script. */
	enum Kind
	{
		/** The opposing creature of the duel fades in. */
		ENTER_FOE,

		/** The player's creature of the duel fades in. */
		ENTER_MINE,

		/** One hit: an attack by one side and a reaction by the other. */
		HIT,

		/** The loser goes down and stays down. */
		DOWN,

		/** One side fades out. */
		EXIT
	}

	/** One of the two sides. */
	enum Side
	{
		/** The player's creatures. */
		MINE,

		/** The opposing creatures. */
		FOE
	}

	/** One thing that happens, and how many game ticks it lasts. */
	static final class Entry
	{
		private final Kind kind;
		private final Side side;
		private final String species;
		private final boolean gilded;
		private int npcId;
		private final BattleTimeline.Step step;
		private final BattleLine line;
		private final int ticks;

		private Entry(Kind kind, Side side, String species, boolean gilded, BattleTimeline.Step step, BattleLine line,
			int ticks)
		{
			this.kind = kind;
			this.side = side;
			this.species = species;
			this.gilded = gilded;
			this.step = step;
			this.line = line;
			this.ticks = ticks;
		}

		/** @return what happens */
		Kind kind()
		{
			return kind;
		}

		/** @return the side it is about: who fades in, who is hit, who goes down, who fades out; null for a hit */
		Side side()
		{
			return side;
		}

		/** @return the species key of the creature that fades in, or null for any other entry */
		String species()
		{
			return species;
		}

		/** @return whether the creature that fades in is drawn gold */
		boolean gilded()
		{
			return gilded;
		}

		/** @return the NPC id to draw the creature that fades in with, 0 for the species' first */
		int npcId()
		{
			return npcId;
		}

		private Entry withNpcId(int id)
		{
			this.npcId = id;
			return this;
		}

		/** @return the step of a hit, with its style and projectile, or null for any other entry */
		BattleTimeline.Step step()
		{
			return step;
		}

		/** @return the line to float for this entry, or null when it has none */
		BattleLine line()
		{
			return line;
		}

		/** @return how many game ticks it lasts */
		int ticks()
		{
			return ticks;
		}

		private int lineDelay()
		{
			return kind == Kind.HIT ? LINE_DELAY_TICKS : 0;
		}
	}

	/** What one game tick of a run brings. */
	static final class Tick
	{
		private final Entry started;
		private final BattleLine line;
		private final boolean done;
		private final boolean waiting;

		private Tick(Entry started, BattleLine line, boolean done)
		{
			this(started, line, done, false);
		}

		private Tick(Entry started, BattleLine line, boolean done, boolean waiting)
		{
			this.started = started;
			this.line = line;
			this.done = done;
			this.waiting = waiting;
		}

		/** @return whether an open script has played everything it was given and waits for more */
		boolean waiting()
		{
			return waiting;
		}

		/** @return the entry that begins on this tick, or null when the one before carries on */
		Entry started()
		{
			return started;
		}

		/** @return the line that is due on this tick, or null */
		BattleLine line()
		{
			return line;
		}

		/** @return whether the script is over: every entry has run its ticks */
		boolean done()
		{
			return done;
		}
	}

	/** A script being played, moved on one game tick at a time. */
	static final class Run
	{
		private final List<Entry> entries;
		private boolean open;
		private int next;
		private Entry current;
		private int left;
		private int age;

		private Run(List<Entry> entries, boolean open)
		{
			this.entries = entries;
			this.open = open;
		}

		/**
		 * Adds entries to a script that is still open, for the next turn of a battle the player steers.
		 *
		 * @param more the entries, in order
		 */
		void append(List<Entry> more)
		{
			entries.addAll(more);
		}

		/** Closes the script: it is done once the entries it has are played. */
		void close()
		{
			open = false;
		}

		/** @return whether the script can still be added to */
		boolean open()
		{
			return open;
		}

		/** @return whether an open script has played everything it was given and waits for more */
		boolean waiting()
		{
			return open && left == 0 && next >= entries.size();
		}

		/**
		 * Moves the script on by one game tick.
		 *
		 * @return what begins and which line is due on this tick; done once every entry has run its ticks
		 */
		Tick tick()
		{
			Entry started = null;
			if (left == 0)
			{
				if (next >= entries.size())
					return new Tick(null, null, !open, open);

				current = entries.get(next++);
				left = current.ticks;
				age = 0;
				started = current;
			}

			BattleLine due = current.line != null && age == current.lineDelay() ? current.line : null;
			age++;
			left--;
			return new Tick(started, due, false);
		}
	}

	private final List<Entry> entries;

	private TeamScript(List<Entry> entries)
	{
		this.entries = Collections.unmodifiableList(entries);
	}

	/**
	 * Cuts a duel's hits down to the few that are acted out.
	 *
	 * @param hits every hit of the duel, in order
	 * @return all of them when there are {@link #TEAM_HIT_BEATS} or fewer, else the first, the one in the middle and
	 *     the last
	 */
	static List<BattleLine> cut(List<BattleLine> hits)
	{
		if (hits.size() <= TEAM_HIT_BEATS)
			return new ArrayList<>(hits);

		List<BattleLine> kept = new ArrayList<>();
		kept.add(hits.get(0));
		kept.add(hits.get(hits.size() / 2));
		kept.add(hits.get(hits.size() - 1));
		return kept;
	}

	/**
	 * Plans the scene of a team battle.
	 *
	 * @param duels the duels in order
	 * @param leadFirst whether the player's first creature is the lead that already stands beside the player, so it
	 *     does not fade in
	 * @return the script
	 */
	static TeamScript of(List<Duel> duels, boolean leadFirst)
	{
		List<Entry> entries = new ArrayList<>();
		Duel before = null;
		for (Duel duel : duels)
		{
			if (before == null || duel.theirIndex() != before.theirIndex())
				entries.add(new Entry(Kind.ENTER_FOE, Side.FOE, duel.theirSpecies(), false, null, null, FADE_TICKS));

			boolean mineNew = before == null ? !leadFirst : duel.mineIndex() != before.mineIndex();
			if (mineNew)
				entries.add(new Entry(Kind.ENTER_MINE, Side.MINE, duel.mineSpecies(), duel.mineGilded(), null, null,
					FADE_TICKS).withNpcId(duel.mineNpcId()));

			for (BattleLine hit : duel.hits())
			{
				List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(Collections.singletonList(hit));
				if (!steps.isEmpty())
					entries.add(new Entry(Kind.HIT, null, null, false, steps.get(0), hit, BattleTimeline.BEAT_TICKS));
			}

			Side loser = duel.mineWon() ? Side.FOE : Side.MINE;
			entries.add(new Entry(Kind.DOWN, loser, null, false, null, duel.knockOut(), TEAM_HOLD_TICKS));
			entries.add(new Entry(Kind.EXIT, loser, null, false, null, null, FADE_TICKS));
			before = duel;
		}

		if (before != null && !before.mineWon())
			entries.add(new Entry(Kind.EXIT, Side.FOE, null, false, null, null, FADE_TICKS));

		return new TeamScript(entries);
	}

	/**
	 * Makes the notice that floats for a line: a hit floats over the creature that was hit, anything else over the
	 * player.
	 *
	 * @param line the line
	 * @return the notice
	 */
	static Notice noticeOf(BattleLine line)
	{
		if (!line.blow())
			return Notice.player(line.text(), line.tone());

		return line.overPlayer() ? Notice.stageLead(line.text(), line.tone())
			: Notice.stageFoe(line.text(), line.tone());
	}

	/** @return the entries, in order */
	List<Entry> entries()
	{
		return entries;
	}

	/** @return how many game ticks the whole script lasts */
	int totalTicks()
	{
		int total = 0;
		for (Entry entry : entries)
			total += entry.ticks;

		return total;
	}

	/** @return a fresh run of the script, at its start */
	Run run()
	{
		return new Run(new ArrayList<>(entries), false);
	}

	/**
	 * Starts the script of a battle the player steers: it has no entries yet and no end, and waits whenever it has
	 * played what it was given.
	 *
	 * @return an open run
	 */
	static Run openRun()
	{
		return new Run(new ArrayList<>(), true);
	}

	/**
	 * Makes the entry that brings the opposing creature in.
	 *
	 * @param species the species key
	 * @return the entry
	 */
	static Entry enterFoe(String species)
	{
		return new Entry(Kind.ENTER_FOE, Side.FOE, species, false, null, null, FADE_TICKS);
	}

	/**
	 * Makes the entry that brings one of the player's creatures in.
	 *
	 * @param species the species key
	 * @param gilded whether it is drawn gold
	 * @return the entry
	 */
	static Entry enterMine(String species, boolean gilded)
	{
		return enterMine(species, gilded, 0);
	}

	/**
	 * Makes the entry that brings one of the player's creatures in, drawn as the NPC it was caught as.
	 *
	 * @param species the species key
	 * @param gilded whether it is drawn gold
	 * @param npcId the NPC id to draw it with, 0 for the species' first
	 * @return the entry
	 */
	static Entry enterMine(String species, boolean gilded, int npcId)
	{
		return new Entry(Kind.ENTER_MINE, Side.MINE, species, gilded, null, null, FADE_TICKS).withNpcId(npcId);
	}

	/**
	 * Turns one turn of a battle the player steers into the entries that act it out: a creature that comes back and
	 * one that steps in, the blows, and whoever falls, fades out and is replaced.
	 *
	 * @param turn the turn
	 * @return the entries, in order
	 */
	static List<Entry> entriesOf(TurnResult turn)
	{
		List<Entry> out = new ArrayList<>();
		if (turn.voluntarySwitch())
			out.add(new Entry(Kind.EXIT, Side.MINE, null, false, null, null, FADE_TICKS));

		Combatant entered = turn.enteredMine();
		if (entered != null)
			out.add(enterMine(entered.speciesKey(), entered.isGilded(), entered.npcId()));

		for (BattleLine hit : turn.hits())
		{
			List<BattleTimeline.Step> steps = BattleTimeline.stepsOf(Collections.singletonList(hit));
			if (!steps.isEmpty())
				out.add(new Entry(Kind.HIT, null, null, false, steps.get(0), hit, BattleTimeline.BEAT_TICKS));
		}

		Duel duel = turn.duel();
		BattleLine knockOut = duel == null ? null : duel.knockOut();
		if (turn.mineFainted())
		{
			out.add(new Entry(Kind.DOWN, Side.MINE, null, false, null, knockOut, TEAM_HOLD_TICKS));
			out.add(new Entry(Kind.EXIT, Side.MINE, null, false, null, null, FADE_TICKS));
		}

		if (turn.foeFainted())
		{
			out.add(new Entry(Kind.DOWN, Side.FOE, null, false, null, knockOut, TEAM_HOLD_TICKS));
			out.add(new Entry(Kind.EXIT, Side.FOE, null, false, null, null, FADE_TICKS));
		}

		Combatant next = turn.enteredFoe();
		if (next != null)
			out.add(enterFoe(next.speciesKey()));

		return out;
	}

	/**
	 * Makes the last entries of a battle the player steers: the opposing creature that won fades out, as it does in
	 * a battle played in one go.
	 *
	 * @param won whether the player's team won
	 * @return the entries
	 */
	static List<Entry> endEntries(boolean won)
	{
		List<Entry> out = new ArrayList<>();
		if (!won)
			out.add(new Entry(Kind.EXIT, Side.FOE, null, false, null, null, FADE_TICKS));

		return out;
	}
}
