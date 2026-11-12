/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.WorldView;

/**
 * Acts out the turns of a duel between two bodies, the same way in a battle against a wild creature and in a team
 * battle: a melee attacker steps up, strikes and steps back, a ranged or magic attacker stays put and a
 * {@link BattleProjectile} crosses the gap, and the blow lands at {@link TurnMotion#IMPACT_TICK} for every style.
 * It owns the turn in progress and the projectile; the stage that uses it owns the bodies and everything else.
 * Every method must be called on the game thread.
 */
final class DuelActing
{
	private static final Logger log = LoggerFactory.getLogger(DuelActing.class);

	/** Share of a body's height a projectile leaves from and lands at. */
	private static final double SHOT_HEIGHT = 0.6;

	/** One of the two bodies of a duel. */
	interface Side
	{
		/**
		 * Acts out a move.
		 *
		 * @param act the move
		 */
		void act(CreatureBody.Act act);

		/**
		 * Steps up to the target, strikes and steps back.
		 *
		 * @param distance how far it steps, in tiles
		 * @param act the attack
		 */
		void meleeTurn(double distance, CreatureBody.Act act);

		/** @return the body's world x, in tiles */
		double x();

		/** @return the body's world y, in tiles */
		double y();

		/** @return how tall the body is drawn, in scene units */
		double height();
	}

	/** Makes the lead creature's side of a duel from the follower. */
	private static final class LeadSide implements Side
	{
		private final FollowerService follower;

		private LeadSide(FollowerService follower)
		{
			this.follower = follower;
		}

		@Override
		public void act(CreatureBody.Act act)
		{
			follower.act(act);
		}

		@Override
		public void meleeTurn(double distance, CreatureBody.Act act)
		{
			follower.meleeTurn(distance, act);
		}

		@Override
		public double x()
		{
			return follower.x();
		}

		@Override
		public double y()
		{
			return follower.y();
		}

		@Override
		public double height()
		{
			return follower.tagHeight();
		}
	}

	private final Client client;
	private final ProjectileModels projectileModels;
	private final Side lead;
	private final Side foe;
	private double gap = TurnMotion.GAP_TILES;
	private BattleProjectile projectile;
	private int turnAge = TurnMotion.TURN_TICKS;
	private boolean impactPending;
	private boolean impactOnLead;

	/**
	 * Creates the helper with no turn in progress.
	 *
	 * @param client the game client
	 * @param projectileModels where the models of the projectiles are built
	 * @param lead the lead creature's side
	 * @param foe the other creature's side
	 */
	DuelActing(Client client, ProjectileModels projectileModels, Side lead, Side foe)
	{
		this.client = client;
		this.projectileModels = projectileModels;
		this.lead = lead;
		this.foe = foe;
	}

	/**
	 * Makes the lead creature's side of a duel.
	 *
	 * @param follower the lead creature
	 * @return its side
	 */
	static Side leadOf(FollowerService follower)
	{
		return new LeadSide(follower);
	}

	/**
	 * Sets how far apart the two bodies stand, edge to edge, so a melee attacker knows how far to step.
	 *
	 * @param tiles the gap that worked, in tiles
	 */
	void setGap(double tiles)
	{
		gap = tiles;
	}

	/**
	 * Plays one beat of a duel: the blow of the beat before lands first, if it has not yet.
	 *
	 * @param step the beat
	 * @param leadLost whether the lead is the one that goes down at {@link BattleTimeline.Beat#FINISH}
	 */
	void play(BattleTimeline.Step step, boolean leadLost)
	{
		landBlow();
		switch (step.beat())
		{
			case FACE_OFF:
				break;

			case LEAD_ATTACKS:
				leadStrikes(step, CreatureBody.Act.ATTACK);
				break;

			case FOE_ATTACKS:
				foeStrikes(step, CreatureBody.Act.ATTACK);
				break;

			case LEAD_SPECIAL:
				leadStrikes(step, CreatureBody.Act.SPECIAL);
				break;

			case FOE_SPECIAL:
				foeStrikes(step, CreatureBody.Act.SPECIAL);
				break;

			default:
				if (leadLost)
					lead.act(CreatureBody.Act.DEFEATED);
				else
					foe.act(CreatureBody.Act.DEFEATED);

				break;
		}
	}

	/**
	 * The lead attacks: a melee attacker steps up to the other body, any other stays put and shoots. The other body
	 * reacts when the blow lands ({@link #advanceTurn(WorldView, int)}).
	 *
	 * @param step the step
	 * @param act the lead's attack
	 */
	private void leadStrikes(BattleTimeline.Step step, CreatureBody.Act act)
	{
		if (step.melee())
			lead.meleeTurn(gap - TurnMotion.CONTACT_TILES, act);
		else
			lead.act(act);

		beginTurn(step, false);
	}

	/**
	 * The other creature attacks, the same way. The lead reacts when the blow lands.
	 *
	 * @param step the step
	 * @param act the other creature's attack
	 */
	private void foeStrikes(BattleTimeline.Step step, CreatureBody.Act act)
	{
		if (step.melee())
			foe.meleeTurn(gap - TurnMotion.CONTACT_TILES, act);
		else
			foe.act(act);

		beginTurn(step, true);
	}

	/**
	 * Starts counting a turn and, for a shot, sends the projectile on its way.
	 *
	 * @param step the step
	 * @param leadIsHit whether the lead is the one that is hit
	 */
	private void beginTurn(BattleTimeline.Step step, boolean leadIsHit)
	{
		turnAge = 0;
		impactPending = true;
		impactOnLead = leadIsHit;
		if (step.melee() || step.look() == null)
			return;

		try
		{
			Side from = leadIsHit ? foe : lead;
			Side to = leadIsHit ? lead : foe;
			ProjectilePath route = new ProjectilePath(from.x(), from.y(), from.height() * SHOT_HEIGHT, to.x(), to.y(),
				to.height() * SHOT_HEIGHT);
			if (projectile == null)
				projectile = new BattleProjectile(client, projectileModels);

			projectile.launch(step.look(), route);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not launch a projectile", e);
			dropProjectile();
		}
	}

	/**
	 * Moves the turn on by one client tick: the blow lands at {@link TurnMotion#IMPACT_TICK}, and a projectile that
	 * is in the air is placed. A failure costs the projectile, not the battle.
	 *
	 * @param view the top level world view
	 * @param plane the plane of the battle
	 */
	void advanceTurn(WorldView view, int plane)
	{
		if (turnAge >= TurnMotion.TURN_TICKS)
			return;

		turnAge++;
		if (impactPending && turnAge >= TurnMotion.IMPACT_TICK)
			landBlow();

		if (projectile == null)
			return;

		try
		{
			projectile.place(turnAge, view, plane);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not move the projectile", e);
			dropProjectile();
		}
	}

	/** Makes the target of the turn react, if the blow has not landed yet, and takes the projectile away. */
	private void landBlow()
	{
		if (!impactPending)
			return;

		impactPending = false;
		if (impactOnLead)
			lead.act(CreatureBody.Act.HIT);
		else
			foe.act(CreatureBody.Act.HIT);

		if (projectile != null)
			projectile.hide();
	}

	/** Ends a turn that is cut short: no blow lands and the projectile goes. */
	void settle()
	{
		impactPending = false;
		turnAge = TurnMotion.TURN_TICKS;
		if (projectile != null)
			projectile.hide();
	}

	/** Ends the turn and takes the projectile away for good, for when the scene is over. Safe at any time. */
	void release()
	{
		impactPending = false;
		turnAge = TurnMotion.TURN_TICKS;
		dropProjectile();
	}

	/** Takes the projectile away for good: after a failure, it is not tried again in this scene. */
	private void dropProjectile()
	{
		BattleProjectile gone = projectile;
		projectile = null;
		if (gone == null)
			return;

		try
		{
			gone.remove();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not remove the projectile", e);
		}
	}
}
