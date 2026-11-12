/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * What is faded, and when, in a battle that is acted out on screen. At the start the real monster's full-size
 * "ghost" fades from solid down to its resting opacity while the stand-in fades in; during the fight the ghost fades
 * in and out as the real monster leaves and comes back to the tile the battle started on; at a normal end the
 * stand-in fades out while the ghost fades up to solid, and then the real monster is shown again. It has no link to
 * the game client, and it is moved on by client ticks, so it can be tested tick by tick.
 */
final class BattleFades
{
	/** Client ticks, 20 ms each, the fade at the start of a battle lasts: 1 game tick of 30 client ticks, 0.6 s. */
	static final int START_FADE_CLIENT_TICKS = 30;

	/**
	 * Client ticks, 20 ms each, the ghost's fade on or off the start tile and the fade at the end of a battle
	 * last: 3 game ticks of 30 client ticks, 1.8 seconds.
	 */
	static final int FADE_CLIENT_TICKS = 3 * 30;

	/** The stage the fades are at. */
	enum Stage
	{
		/** The ghost fades down and the stand-in fades in. */
		STARTING,

		/** The ghost follows whether the real monster is on the start tile. */
		FIGHTING,

		/** The stand-in fades out and the ghost fades up to solid. */
		ENDING,

		/** Over: the real monster can be shown again. */
		OVER
	}

	private final OpacityFade ghost = new OpacityFade(1.0);
	private final OpacityFade standIn = new OpacityFade(0.0);
	private Stage stage = Stage.STARTING;
	private int age;

	/**
	 * Starts the fades of a battle: the ghost solid and the stand-in unseen.
	 *
	 * @param onHome whether the real monster is on the tile the battle started on
	 */
	BattleFades(boolean onHome)
	{
		ghost.moveTo(resting(onHome), START_FADE_CLIENT_TICKS);
		standIn.moveTo(1.0, START_FADE_CLIENT_TICKS);
	}

	/**
	 * Gives the opacity the ghost rests at.
	 *
	 * @param onHome whether the real monster is on the tile the battle started on
	 * @return 0 on that tile, where the stand-in already shows it, else {@link GhostMirror#OPACITY}
	 */
	static double resting(boolean onHome)
	{
		return onHome ? 0.0 : GhostMirror.OPACITY;
	}

	/**
	 * Moves the fades on by one client tick.
	 *
	 * @param onHome whether the real monster is on the tile the battle started on now
	 */
	void tick(boolean onHome)
	{
		if (stage == Stage.OVER)
			return;

		age++;
		int length = stage == Stage.STARTING ? START_FADE_CLIENT_TICKS : FADE_CLIENT_TICKS;
		int left = length - age;
		if (stage == Stage.STARTING)
		{
			ghost.moveTo(resting(onHome), left + 1);
			standIn.moveTo(1.0, left + 1);
		}
		else if (stage == Stage.FIGHTING)
		{
			ghost.moveAt(resting(onHome), GhostMirror.OPACITY / FADE_CLIENT_TICKS);
		}
		else
		{
			ghost.moveTo(1.0, left + 1);
			standIn.moveTo(0.0, left + 1);
		}

		ghost.tick();
		standIn.tick();
		if (stage != Stage.FIGHTING && age >= length)
			next();
	}

	/** Starts the fade at the end of a normal battle: the stand-in out, the ghost up to solid. */
	void beginEnd()
	{
		if (stage == Stage.ENDING || stage == Stage.OVER)
			return;

		stage = Stage.ENDING;
		age = 0;
	}

	/** @return the stage the fades are at */
	Stage stage()
	{
		return stage;
	}

	/** @return whether the end fade has started, or is over */
	boolean ending()
	{
		return stage == Stage.ENDING || stage == Stage.OVER;
	}

	/** @return whether the end fade is over, so the real monster can be shown and the copies removed */
	boolean finished()
	{
		return stage == Stage.OVER;
	}

	/** @return how much of the ghost can be seen now, from 0 to 1 */
	double ghostOpacity()
	{
		return ghost.opacity();
	}

	/** @return how much of the stand-in can be seen now, from 0 to 1 */
	double standInOpacity()
	{
		return standIn.opacity();
	}

	private void next()
	{
		stage = stage == Stage.STARTING ? Stage.FIGHTING : Stage.OVER;
		age = 0;
	}
}
