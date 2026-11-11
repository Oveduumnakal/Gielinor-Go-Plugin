/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * A see-through copy of a monster that is hidden for a battle. It is built from a private copy of the monster's
 * model, so the model the game shares between monsters of that type is never touched, and it follows the real
 * monster every client tick: place, facing and animation. Every method must be called on the game thread.
 */
final class NpcGhost extends RuneLiteObject
{
	private final Client client;
	private final AnimationController controller;
	private final ModelData source;
	private final Model[] levels = new Model[OpacityFade.SOLID + 1];
	private int level = OpacityFade.HIDDEN;
	private int shown = GhostMirror.NO_ANIMATION;

	private NpcGhost(Client client, ModelData source)
	{
		super(client);
		this.client = client;
		this.source = source;
		this.controller = new AnimationController(client, (Animation) null);
		this.controller.setOnFinished(NpcGhost::restart);
		setAnimationController(controller);
	}

	/**
	 * Builds the copy. It is not in the scene until {@link #mirror(NPC, double)} is called with some opacity. The
	 * models for each opacity are built the first time that opacity is shown.
	 *
	 * @param client the game client
	 * @param parts the monster's model, loaded for this copy alone
	 * @return the copy
	 */
	static NpcGhost create(Client client, NpcModels.Parts parts)
	{
		return new NpcGhost(client, parts.scaled());
	}

	/**
	 * Puts the copy where the monster is, facing the way it faces, showing what it shows, at the given opacity.
	 * The copy leaves the scene while the monster has no place in it, or while the opacity is 0. Opacity 1 draws
	 * the plain model; anything between draws a private see-through copy, built once per level.
	 *
	 * @param npc the real monster
	 * @param opacity how much of it can be seen, from 0 to 1
	 * @throws IllegalStateException when the model for that opacity cannot be built
	 */
	void mirror(NPC npc, double opacity)
	{
		LocalPoint at = npc.getLocalLocation();
		WorldView view = npc.getWorldView();
		int wanted = OpacityFade.levelOf(opacity);
		if (wanted == OpacityFade.HIDDEN || at == null || view == null)
		{
			if (isActive())
				setActive(false);

			return;
		}

		if (wanted != level)
		{
			setModel(modelAt(wanted));
			level = wanted;
		}

		GhostMirror.Pick pick = GhostMirror.pick(npc.getAnimation(), npc.getAnimationFrame(),
			npc.getPoseAnimation(), npc.getPoseAnimationFrame());
		if (GhostMirror.animationChanged(shown, controller.getAnimation() != null, pick.animation()))
		{
			shown = pick.animation();
			controller.setAnimation(pick.animation() == GhostMirror.NO_ANIMATION
				? null : client.loadAnimation(pick.animation()));
		}

		if (controller.getAnimation() != null && GhostMirror.drifted(controller.getFrame(), pick.frame()))
			controller.setFrame(pick.frame());

		setLocation(at, view.getPlane());
		setOrientation(npc.getCurrentOrientation());
		if (!isActive())
			setActive(true);
	}

	private Model modelAt(int wanted)
	{
		if (levels[wanted] == null)
		{
			ModelData data = source.shallowCopy();
			if (wanted != OpacityFade.SOLID)
			{
				data.cloneTransparencies(true);
				byte[] faces = data.getFaceTransparencies();
				if (faces == null)
					throw new IllegalStateException("no transparencies for level " + wanted);

				GhostMirror.fadeAll(faces, OpacityFade.opacityOf(wanted));
			}

			levels[wanted] = data.light();
		}

		if (levels[wanted] == null)
			throw new IllegalStateException("no model for level " + wanted);

		return levels[wanted];
	}

	/** Takes the copy out of the scene. */
	void remove()
	{
		if (isActive())
			setActive(false);
	}

	/**
	 * Called when the copy's animation runs out. It starts over, and the next {@link #mirror(NPC, double)} puts it
	 * back in step with the monster.
	 *
	 * @param finished the controller whose animation just ended
	 */
	static void restart(AnimationController finished)
	{
		finished.reset();
	}
}
