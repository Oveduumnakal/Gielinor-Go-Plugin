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
 * other; at the last line the loser goes down and stays down for a few seconds. An automatic battle is planned in
 * full when it starts. A battle the player steers is fed beat by beat ({@link #append}) and waits in
 * {@link Phase#WAITING} whenever it has played everything it was given, until the next turn is appended or the
 * battle is ended with {@link #finishWith(boolean)}. It has no link to the game client, so the order of events can
 * be tested tick by tick.
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

		/** A battle the player steers has played every beat it was given and waits for the next turn. */
		WAITING,

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
		FINISH,

		/** The lead creature goes down, but the battle goes on: another creature of the team must step in. */
		LEAD_FELL,

		/** Another creature of the team steps in as the lead, in a battle that played its switches by itself. */
		LEAD_ENTERS
	}

	/** One beat of the fight, with how the attack of that beat is made. */
	static final class Step
	{
		private final Beat beat;
		private final CombatType style;
		private final ProjectileLook look;
		private final Combatant entered;

		/**
		 * Describes the step in which a creature of the team steps in as the lead.
		 *
		 * @param entered the creature that steps in
		 */
		Step(Combatant entered)
		{
			this.beat = Beat.LEAD_ENTERS;
			this.style = null;
			this.look = null;
			this.entered = entered;
		}

		/**
		 * Describes a step with no projectile.
		 *
		 * @param beat what happens
		 * @param style the style of the attack, or null when the beat is not an attack
		 */
		Step(Beat beat, CombatType style)
		{
			this(beat, style, null);
		}

		/**
		 * Describes a step.
		 *
		 * @param beat what happens
		 * @param style the style of the attack, or null when the beat is not an attack
		 * @param look what the projectile looks like, or null when the attack has none
		 */
		Step(Beat beat, CombatType style, ProjectileLook look)
		{
			this.beat = beat;
			this.style = style;
			this.look = look;
			this.entered = null;
		}

		/** @return the creature that steps in at a {@link Beat#LEAD_ENTERS} beat, else null */
		Combatant entered()
		{
			return entered;
		}

		/** @return what happens */
		Beat beat()
		{
			return beat;
		}

		/** @return the style of the attack, or null when the beat is not an attack */
		CombatType style()
		{
			return style;
		}

		/** @return what the projectile looks like, or null when the attack is made by stepping in */
		ProjectileLook look()
		{
			return look;
		}

		/** @return whether the attacker steps up to the target: a melee attack, or a beat with no style given */
		boolean melee()
		{
			return style == null || style == CombatType.MELEE;
		}
	}

	private final List<Step> steps;
	private boolean leadWon;
	private boolean sealed;
	private Phase phase = Phase.WALKING;
	private int walked;
	private int next;
	private int held;
	private int wait;
	private int sinceFirst = -1;

	/**
	 * Plans a battle.
	 *
	 * @param steps what happens on each beat of the fight, in order; a missing {@link Beat#FINISH} is added
	 * @param leadWon whether the player's creature won, so the other one is the loser when it is false
	 */
	BattleTimeline(List<Step> steps, boolean leadWon)
	{
		this(steps, leadWon, true);
	}

	private BattleTimeline(List<Step> steps, boolean leadWon, boolean sealed)
	{
		this.steps = new ArrayList<>(steps);
		if (sealed && (this.steps.isEmpty() || this.steps.get(this.steps.size() - 1).beat() != Beat.FINISH))
			this.steps.add(new Step(Beat.FINISH, null));

		this.leadWon = leadWon;
		this.sealed = sealed;
	}

	/**
	 * Plans the start of a battle the player steers: the beats known so far, and no end yet.
	 *
	 * @param steps what happens on the first beats, usually the face-off
	 * @return a timeline that waits for more beats once it has played these
	 */
	static BattleTimeline open(List<Step> steps)
	{
		return new BattleTimeline(steps, false, false);
	}

	/**
	 * Adds the beats of the next turn of a battle the player steers. A battle that is waiting goes on with them.
	 * Beats added to a battle that already has its end are ignored.
	 *
	 * @param more the beats, in order
	 */
	void append(List<Step> more)
	{
		if (sealed)
			return;

		steps.addAll(more);
		if (phase == Phase.WAITING && next < steps.size())
			phase = Phase.FIGHTING;
	}

	/**
	 * Gives a battle the player steers its end: the beats that are left play, and then the loser goes down. Does
	 * nothing for a battle that already has its end.
	 *
	 * @param won whether the player's creature won
	 */
	void finishWith(boolean won)
	{
		if (sealed)
			return;

		sealed = true;
		leadWon = won;
		steps.add(new Step(Beat.FINISH, null));
		if (phase == Phase.WAITING)
			phase = Phase.FIGHTING;
	}

	/** @return whether the battle has its end, so nothing more can be added */
	boolean sealed()
	{
		return sealed;
	}

	/** @return whether a battle the player steers has played everything it was given and waits for the next turn */
	boolean waiting()
	{
		return phase == Phase.WAITING;
	}

	/**
	 * Turns battle lines into steps. The opening line is a beat where nobody strikes ({@link Beat#FACE_OFF}), so text
	 * and blows stay one to one; a line that is only text makes no step. A hit keeps the style it was rolled in.
	 *
	 * @param lines the lines of the battle, in order
	 * @return one step per line that is something to see
	 */
	static List<Step> stepsOf(List<BattleLine> lines)
	{
		List<Step> steps = new ArrayList<>();
		for (BattleLine line : lines)
		{
			switch (line.kind())
			{
				case OPENING:
					steps.add(new Step(Beat.FACE_OFF, null));
					break;

				case LEAD_HIT:
					steps.add(new Step(line.special() ? Beat.LEAD_SPECIAL : Beat.LEAD_ATTACKS, line.style(),
						ProjectileLook.of(line.style(), line.special(), line.icy())));
					break;

				case FOE_HIT:
					steps.add(new Step(line.special() ? Beat.FOE_SPECIAL : Beat.FOE_ATTACKS, line.style(),
						ProjectileLook.of(line.style(), line.special(), line.icy())));
					break;

				case CLOSING:
					steps.add(new Step(Beat.FINISH, null));
					break;

				default:
					break;
			}
		}

		return steps;
	}

	/**
	 * Moves the battle on by one game tick.
	 *
	 * @param arrived whether the lead has reached its place beside the other creature
	 * @return what happens on this tick of the fight, or null when nothing does (still walking, or already down)
	 */
	Step tick(boolean arrived)
	{
		if (sinceFirst >= 0)
			sinceFirst++;

		if (phase == Phase.WALKING)
		{
			walked++;
			if (!arrived && walked < MAX_WALK_TICKS)
				return null;

			phase = Phase.FIGHTING;
		}

		if (phase == Phase.WAITING && next < steps.size())
			phase = Phase.FIGHTING;

		if (phase == Phase.FIGHTING)
		{
			if (wait > 0)
			{
				wait--;
				return null;
			}

			if (next >= steps.size())
			{
				phase = Phase.WAITING;
				return null;
			}

			wait = BEAT_TICKS - 1;
			Step step = steps.get(next++);
			if (sinceFirst < 0)
				sinceFirst = 0;

			if (step.beat() == Beat.FINISH)
			{
				phase = Phase.DOWN;
				held = 0;
			}

			return step;
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

	/**
	 * Tells whether the battle lines may start showing: from one game tick after the first beat, so each line
	 * appears as its blow lands ({@link TurnMotion#IMPACT_TICK} client ticks, one game tick, after its beat starts).
	 *
	 * @return true from one game tick after the first beat
	 */
	boolean linesDue()
	{
		return sinceFirst >= 1;
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

	/** @return the steps, for tests and logging */
	List<Step> steps()
	{
		return Collections.unmodifiableList(steps);
	}
}
