/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.function.BooleanSupplier;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;

/**
 * The player's side of a catch: the character makes a throwing motion, and reacts to the result once the jar has
 * landed. What the creature does is played on its own body ({@link WildBody}, {@link BodyCatch}). All of it is drawn
 * on the player's own screen only; nothing is sent to the game. Every method must be called on the game thread.
 */
final class CatchEffects
{
	/** The animation id that means the player is not doing anything. */
	private static final int NO_ANIMATION = -1;

	/** The one reaction that is waiting for its moment. */
	private static final class Reaction
	{
		private final int animation;
		private final WorldPoint tile;
		private int age;

		private Reaction(int animation, WorldPoint tile)
		{
			this.animation = animation;
			this.tile = tile;
		}
	}

	private final Client client;
	private final BooleanSupplier enabled;
	private WorldPoint thrownFrom;
	private Reaction reaction;

	/**
	 * Creates the effects with nothing playing.
	 *
	 * @param client the game client
	 * @param enabled whether the "Catch effects" setting is on; switching it off ends the waiting reaction at once
	 */
	CatchEffects(Client client, BooleanSupplier enabled)
	{
		this.client = client;
		this.enabled = enabled;
	}

	/**
	 * Makes the player's character throw, and remembers the tile it threw from. The motion is left out while the
	 * character is in the middle of something else, such as fishing or fighting, so a real animation is never cut
	 * short on screen.
	 *
	 * @return whether the throw was played
	 */
	boolean throwJar()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getAnimation() != NO_ANIMATION)
			return false;

		thrownFrom = player.getWorldLocation();
		player.setAnimation(AnimationID.HUMAN_THROW);
		player.setAnimationFrame(0);
		return true;
	}

	/**
	 * Lines up the character's reaction to a throw that was played. It starts once the throw is over, and only if
	 * the character is then idle and still on the tile it threw from. A newer result replaces a waiting one.
	 *
	 * @param outcome what the throw led to
	 */
	void react(CatchOutcome outcome)
	{
		int animation = CatchScene.reactionFor(outcome);
		reaction = animation == CatchScene.NONE ? null : new Reaction(animation, thrownFrom);
	}

	/** Moves the waiting reaction on by one client tick. */
	void clientTick()
	{
		if (!enabled.getAsBoolean())
		{
			clear();
			return;
		}

		tickReaction();
	}

	/** Ages the waiting reaction and plays it when its moment comes. */
	private void tickReaction()
	{
		if (reaction == null)
			return;

		reaction.age++;
		if (reaction.age < CatchScene.REACT_TICK)
			return;

		Reaction due = reaction;
		reaction = null;
		Player player = client.getLocalPlayer();
		if (player == null || player.getAnimation() != NO_ANIMATION || due.tile == null
			|| !due.tile.equals(player.getWorldLocation()))
			return;

		player.setAnimation(due.animation);
		player.setAnimationFrame(0);
	}

	/** Ends the waiting reaction at once, for a logout, a world hop or the plugin stopping. */
	void clear()
	{
		reaction = null;
	}
}
