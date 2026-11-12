/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Runs a battle the player steers: it keeps the session and the choice timer, opens the prompt when the scene has
 * played the last turn, turns a choice into a turn of the game, hands the turn to the scene and to whoever shows the
 * text, and ends the battle when the player leaves it. It holds no game client types: the scene and the listener are
 * small interfaces the plugin fills in, so every rule can be tested with fakes. Every method must be called on the
 * game thread.
 */
final class BattleDirector
{
	/** What acts the battle out on screen. A battle with no scene is played in text only. */
	interface Scene
	{
		/** @return whether the scene is still on screen; false once it ended by itself */
		boolean active();

		/** @return whether the scene has played every turn it was given and waits for the next */
		boolean waiting();

		/**
		 * Adds a turn.
		 *
		 * @param turn the turn
		 */
		void append(TurnResult turn);

		/**
		 * Gives the battle its end: what is left plays, and the scene ends by itself.
		 *
		 * @param won whether the player won
		 */
		void finishWith(boolean won);

		/**
		 * Shows another creature of the team as the lead.
		 *
		 * @param entered the creature that steps in
		 */
		void showLead(Combatant entered);

		/** Ends the scene at once, with a fade, because the player ran. */
		void runAway();

		/** Ends the scene at once, for a battle that cannot go on. */
		void end();
	}

	/** Hears what happens, to show it. */
	interface Listener
	{
		/**
		 * A turn was played.
		 *
		 * @param session the battle
		 * @param turn the turn
		 * @param closing the lines that close the battle when this turn ended it, else none
		 * @param hasScene whether a scene acts the turn out, so it shows the blows itself
		 */
		void turn(BattleSession session, TurnResult turn, List<BattleLine> closing, boolean hasScene);

		/**
		 * A jar was thrown. Called before the turn is shown. For a catch or a creature that ran off the battle is over
		 * and the scene must be handed to the catch effects, which end it.
		 *
		 * @param session the battle
		 * @param jar the jar thrown
		 * @param outcome what it did
		 */
		void jarThrown(BattleSession session, JarType jar, CatchOutcome outcome);
	}

	private final GoGame game;
	private final IntSupplier turnSeconds;
	private final Listener listener;
	private TurnClock clock;
	private BattleSession session;
	private Scene scene;
	private boolean promptOpen;

	/**
	 * Makes a director with no battle.
	 *
	 * @param game the game
	 * @param turnSeconds gives the seconds a player has to choose, read each time a prompt opens
	 * @param listener who shows what happens
	 */
	BattleDirector(GoGame game, IntSupplier turnSeconds, Listener listener)
	{
		this.game = game;
		this.turnSeconds = turnSeconds;
		this.clock = new TurnClock(turnSeconds.getAsInt());
		this.listener = listener;
	}

	/** @return whether a battle is on */
	boolean active()
	{
		return session != null;
	}

	/** @return the battle, or null when none is on */
	BattleSession session()
	{
		return session;
	}

	/** @return whether the battle waits for the player's choice right now */
	boolean promptOpen()
	{
		return session != null && promptOpen;
	}

	/**
	 * Starts a battle. It waits for a choice at once when it has no scene, and as soon as the scene waits when it
	 * has one.
	 *
	 * @param battle the battle
	 * @param stage what acts it out, or null for text only
	 */
	void begin(BattleSession battle, Scene stage)
	{
		end();
		session = battle;
		scene = stage;
		promptOpen = false;
		if (stage == null)
			open();
	}

	/**
	 * Describes the battle for the panel and the prompt.
	 *
	 * @return the view, or null when no choice is open
	 */
	BattleView view()
	{
		if (!promptOpen())
			return null;

		return game.battleView(session, clock.secondsLeft(), clock.fractionLeft());
	}

	/**
	 * Moves on by one game tick: opens the prompt when the scene is ready, counts the time to choose, and ends the
	 * battle as running when it cannot go on.
	 *
	 * @param forfeit whether the player left the battle: logged out, hopped, walked away or the creature is gone
	 */
	void gameTick(boolean forfeit)
	{
		if (session == null)
			return;

		if (forfeit || (scene != null && !scene.active()))
		{
			giveUp();
			return;
		}

		if (!promptOpen)
		{
			if (scene == null || scene.waiting())
				open();

			return;
		}

		if (clock.tick())
			choose(autoChoice());
	}

	/**
	 * Chooses for the player when the time is up: the next creature after a faint, otherwise an attack.
	 *
	 * @return the choice
	 */
	BattleChoice autoChoice()
	{
		BattleState state = session.state();
		return state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive()) : BattleChoice.attack();
	}

	/**
	 * Plays the player's choice.
	 *
	 * @param choice what the player chose
	 * @return true when the choice was played; false when no choice is open or the battle does not accept it
	 */
	boolean choose(BattleChoice choice)
	{
		if (session == null || !promptOpen)
			return false;

		BattleSession played = session;
		JarType jar = choice.kind() == BattleChoice.Kind.CATCH && choice.jar() == null ? game.jarToThrow()
			: choice.jar();
		TurnResult turn = game.act(played, choice);
		if (turn == null)
			return false;

		if (!turn.turnUsed())
		{
			listener.turn(played, turn, Collections.emptyList(), scene != null);
			return true;
		}

		promptOpen = false;
		clock.close();
		Scene stage = scene;
		TurnResult.Status status = turn.status();
		List<BattleLine> closing = game.closingLines(played, turn);
		if (turn.thrown() != null)
			listener.jarThrown(played, jar, turn.thrown());

		if (stage != null)
			playOnScene(stage, turn, status);

		listener.turn(played, turn, closing, stage != null);
		if (status.over())
		{
			session = null;
			scene = null;
			return true;
		}

		if (stage == null)
			open();

		return true;
	}

	private void playOnScene(Scene stage, TurnResult turn, TurnResult.Status status)
	{
		Combatant entered = turn.enteredMine();
		if (entered != null && stage.active())
			stage.showLead(entered);

		if (status == TurnResult.Status.RAN)
		{
			stage.runAway();
			return;
		}

		if (status == TurnResult.Status.CAUGHT || status == TurnResult.Status.FOE_FLED)
			return;

		stage.append(turn);
		if (status == TurnResult.Status.WON)
			stage.finishWith(true);
		else if (status == TurnResult.Status.LOST)
			stage.finishWith(false);
	}

	/** Ends the battle as running: the player left it. Nothing is won, and the scene ends. */
	void giveUp()
	{
		BattleSession leaving = session;
		Scene stage = scene;
		session = null;
		scene = null;
		promptOpen = false;
		clock.close();
		if (leaving != null)
			game.forfeit(leaving);

		if (stage != null)
			stage.end();
	}

	/** Drops the battle with no ruling, for when the plugin stops. Whatever was won or lost stays as it is. */
	void end()
	{
		if (session != null)
			giveUp();
	}

	private void open()
	{
		promptOpen = true;
		clock = new TurnClock(turnSeconds.getAsInt());
		clock.open();
	}
}
