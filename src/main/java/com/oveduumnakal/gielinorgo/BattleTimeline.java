/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Who acts when in a battle that is acted out on screen. The lead creature first walks up to the other one; then
 * each battle line is one beat, {@link #BEAT_TICKS} game ticks apart, an attack by one side and a reaction by the
 * other; at the last line the loser goes down and stays down for a few seconds. It has no link to the game client,
 * so the order of events can be tested tick by tick.
 */
final class BattleTimeline
{
	/** Most game ticks to wait for the lead to arrive. When they are up the lead is put in place. */
	static final int MAX_WALK_TICKS = 8;

	/** Game ticks from one attack to the next, the same pace as the battle lines ({@link NoticeBoard#LINE_TICKS}). */
	static final int BEAT_TICKS = NoticeBoard.LINE_TICKS;

	/** Game ticks the lead stays down when it loses: about five seconds. */
	static final int HOLD_TICKS = 8;

	/** Game ticks a beaten wild creature stays down, knocked out and open to a jar: about ten seconds. */
	static final int KNOCKED_OUT_TICKS = 17;

	/** Client ticks, 20 ms each, an attack's lunge or a recoil lasts. Shorter than a game tick (30). */
	static final int LUNGE_TICKS = 18;

	/** How far a creature with no attack animation lunges at the other, in tiles. */
	static final double LUNGE_TILES = 0.4;

	/** How far a creature with no reaction animation is knocked back, in tiles. */
	static final double RECOIL_TILES = 0.15;

	/** The stage a battle is at. */
	enum Phase
	{
		/** The lead is walking up to the other creature. */
		WALKING,

		/** The battle lines are being played, one every {@link #BEAT_TICKS} game ticks. */
		FIGHTING,

		/** The loser is down. */
		DOWN,

		/** Over: everyone is back to normal. */
		DONE
	}

	/** What happens on one game tick of the fight. */
	enum Beat
	{
		/** The opening line: both creatures face each other and nobody strikes. */
		FACE_OFF,

		/** The lead creature attacks and the other one reacts. */
		LEAD_ATTACKS,

		/** The other creature attacks and the lead reacts. */
		FOE_ATTACKS,

		/** The lead creature uses its second attack, such as a breath, and the other one reacts. */
		LEAD_SPECIAL,

		/** The other creature uses its second attack and the lead reacts. */
		FOE_SPECIAL,

		/** The result is out and the loser goes down. */
		FINISH
	}

	private final List<Beat> beats;
	private final boolean leadWon;
	private Phase phase = Phase.WALKING;
	private int walked;
	private int next;
	private int held;
	private int wait;

	/**
	 * Plans a battle.
	 *
	 * @param beats what happens on each game tick of the fight, in order; a missing {@link Beat#FINISH} is added
	 * @param leadWon whether the player's creature won, so the other one is the loser when it is false
	 */
	BattleTimeline(List<Beat> beats, boolean leadWon)
	{
		this.beats = new ArrayList<>(beats);
		if (this.beats.isEmpty() || this.beats.get(this.beats.size() - 1) != Beat.FINISH)
			this.beats.add(Beat.FINISH);

		this.leadWon = leadWon;
	}

	/**
	 * Turns battle lines into beats. The opening line is a beat where nobody strikes ({@link Beat#FACE_OFF}), so text
	 * and blows stay one to one; a line that is only text makes no beat.
	 *
	 * @param lines the lines of the battle, in order
	 * @return one beat per line that is something to see
	 */
	static List<Beat> beatsOf(List<BattleLine> lines)
	{
		List<Beat> beats = new ArrayList<>();
		for (BattleLine line : lines)
		{
			switch (line.kind())
			{
				case OPENING:
					beats.add(Beat.FACE_OFF);
					break;

				case LEAD_HIT:
					beats.add(line.special() ? Beat.LEAD_SPECIAL : Beat.LEAD_ATTACKS);
					break;

				case FOE_HIT:
					beats.add(line.special() ? Beat.FOE_SPECIAL : Beat.FOE_ATTACKS);
					break;

				case CLOSING:
					beats.add(Beat.FINISH);
					break;

				default:
					break;
			}
		}

		return beats;
	}

	/**
	 * Moves the battle on by one game tick.
	 *
	 * @param arrived whether the lead has reached its place beside the other creature
	 * @return what happens on this tick of the fight, or null when nothing does (still walking, or already down)
	 */
	Beat tick(boolean arrived)
	{
		if (phase == Phase.WALKING)
		{
			walked++;
			if (!arrived && walked < MAX_WALK_TICKS)
				return null;

			phase = Phase.FIGHTING;
		}

		if (phase == Phase.FIGHTING)
		{
			if (wait > 0)
			{
				wait--;
				return null;
			}

			wait = BEAT_TICKS - 1;
			Beat beat = beats.get(next++);
			if (beat == Beat.FINISH)
			{
				phase = Phase.DOWN;
				held = 0;
			}

			return beat;
		}

		if (phase == Phase.DOWN && ++held >= holdTicks())
			phase = Phase.DONE;

		return null;
	}

	/** @return how many game ticks the loser stays down: {@link #KNOCKED_OUT_TICKS} for a wild loser */
	int holdTicks()
	{
		return leadWon ? KNOCKED_OUT_TICKS : HOLD_TICKS;
	}

	/** @return whether the wild creature lies knocked out: the lead won and the loser is down */
	boolean knockedOut()
	{
		return leadWon && phase == Phase.DOWN;
	}

	/** @return the stage the battle is at */
	Phase phase()
	{
		return phase;
	}

	/** @return whether the battle lines have started: true from the tick the first beat plays */
	boolean fightStarted()
	{
		return phase != Phase.WALKING;
	}

	/** @return whether the lead is the one that goes down */
	boolean leadLost()
	{
		return !leadWon;
	}

	/**
	 * Works out how far a creature is pushed along its facing at one moment of a lunge or a recoil: out and back
	 * again, smoothly.
	 *
	 * @param age client ticks since the beat began
	 * @param tiles the furthest it goes, in tiles; negative for backwards
	 * @return the distance along the facing, 0 at the start and again once {@link #LUNGE_TICKS} are up
	 */
	static double lunge(int age, double tiles)
	{
		if (age < 0 || age >= LUNGE_TICKS)
			return 0.0;

		return tiles * Math.sin(Math.PI * age / LUNGE_TICKS);
	}

	/** @return the beats, for tests and logging */
	List<Beat> beats()
	{
		return Collections.unmodifiableList(beats);
	}
}
